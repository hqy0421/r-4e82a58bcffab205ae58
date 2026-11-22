package technology.tabula.templates;

import java.util.ArrayList;
import java.util.List;

import technology.tabula.Rectangle;

/**
 * Assigns the non-empty text blocks of one line to distinct template columns
 * from left to right.
 *
 * <p>A block may only be placed in a column if the distance between the block
 * edge matching the column's alignment and the column's anchor does not exceed
 * the column's maximum deviation. Distances are accumulated in hundredths of a
 * point (0.01 pt) and the unique assignment with the smallest total deviation
 * is returned. Skipping empty columns is allowed. If no legal assignment
 * exists, or the optimum is shared by two or more distinct assignments, a
 * {@link TemplateAlignmentException} carrying the page and line number is
 * thrown.</p>
 */
public class TemplateRowAssigner {

    private static final long INF = Long.MAX_VALUE / 4;
    /** Path counts are capped at 2: beyond that we only need to know a tie exists. */
    private static final long TIE = 2;

    private TemplateRowAssigner() {
    }

    /**
     * @param blocks     non-empty blocks of the line, ordered left to right
     * @param template   column template
     * @param pageNumber 1-based page number, used in error messages
     * @param lineNumber 1-based line number within the page, used in error messages
     * @return the column index assigned to each block, in the same order as {@code blocks}
     */
    public static int[] assign(List<? extends Rectangle> blocks, ColumnTemplate template,
                               int pageNumber, int lineNumber) {
        int n = blocks.size();
        List<ColumnSpec> columns = template.getColumns();
        int m = columns.size();

        if (n > m) {
            throw new TemplateAlignmentException(
                    String.format(java.util.Locale.ROOT,
                            "line has %d non-empty blocks but the template defines only %d columns", n, m),
                    pageNumber, lineNumber);
        }
        if (n == 0) {
            return new int[0];
        }

        // score[i][j]: deviation in 0.01pt of block i in column j, INF if outside tolerance
        long[][] score = new long[n][m];
        for (int i = 0; i < n; i++) {
            Rectangle block = blocks.get(i);
            for (int j = 0; j < m; j++) {
                ColumnSpec column = columns.get(j);
                score[i][j] = column.accepts(block) ? column.deviationHundredths(block) : INF;
            }
        }

        // dp[i][j]: best total score when block i is placed in column j
        long[][] dp = new long[n][m];
        // ways[i][j]: number of distinct assignments reaching dp[i][j], capped at TIE
        long[][] ways = new long[n][m];

        for (int j = 0; j < m; j++) {
            dp[0][j] = score[0][j];
            ways[0][j] = score[0][j] == INF ? 0 : 1;
        }

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < m; j++) {
                dp[i][j] = INF;
                ways[i][j] = 0;
                if (score[i][j] == INF) {
                    continue;
                }
                long bestPrefix = INF;
                long prefixWays = 0;
                // the previous block must sit in a column strictly to the left
                for (int k = 0; k < j; k++) {
                    if (dp[i - 1][k] == INF) {
                        continue;
                    }
                    if (dp[i - 1][k] < bestPrefix) {
                        bestPrefix = dp[i - 1][k];
                        prefixWays = ways[i - 1][k];
                    } else if (dp[i - 1][k] == bestPrefix) {
                        prefixWays = Math.min(TIE, prefixWays + ways[i - 1][k]);
                    }
                }
                if (bestPrefix == INF) {
                    dp[i][j] = INF;
                    ways[i][j] = 0;
                } else {
                    dp[i][j] = bestPrefix + score[i][j];
                    ways[i][j] = prefixWays;
                }
            }
        }

        long bestTotal = INF;
        long bestWays = 0;
        int lastColumn = -1;
        for (int j = 0; j < m; j++) {
            if (dp[n - 1][j] == INF) {
                continue;
            }
            if (dp[n - 1][j] < bestTotal) {
                bestTotal = dp[n - 1][j];
                bestWays = ways[n - 1][j];
                lastColumn = j;
            } else if (dp[n - 1][j] == bestTotal) {
                bestWays = Math.min(TIE, bestWays + ways[n - 1][j]);
            }
        }

        if (bestTotal == INF) {
            throw new TemplateAlignmentException(
                    "no legal column assignment keeps every block within the configured maximum deviation",
                    pageNumber, lineNumber);
        }
        if (bestWays > 1) {
            throw new TemplateAlignmentException(
                    String.format(java.util.Locale.ROOT,
                            "ambiguous line: %d distinct column assignments share the minimum total deviation %.2f pt",
                            bestWays, bestTotal / 100.0),
                    pageNumber, lineNumber);
        }

        int[] assignment = new int[n];
        assignment[n - 1] = lastColumn;
        for (int i = n - 1; i >= 1; i--) {
            long target = dp[i][assignment[i]] - score[i][assignment[i]];
            int predecessor = -1;
            for (int k = 0; k < assignment[i]; k++) {
                if (dp[i - 1][k] == target) {
                    if (predecessor != -1) {
                        // Defensive: a unique optimum at the last block cannot have two
                        // equal-score predecessors, otherwise it would not be unique.
                        throw new TemplateAlignmentException(
                                "ambiguous line: multiple column assignments share the minimum total deviation",
                                pageNumber, lineNumber);
                    }
                    predecessor = k;
                }
            }
            if (predecessor == -1) {
                throw new TemplateAlignmentException(
                        "internal error while reconstructing the column assignment",
                        pageNumber, lineNumber);
            }
            assignment[i - 1] = predecessor;
        }
        return assignment;
    }

    /**
     * Convenience wrapper returning the assigned column (or empty marker) for
     * every template column of a line: result[j] holds the block placed in
     * column j, or {@code null} when that column is skipped.
     */
    public static List<Rectangle> assignToColumns(List<? extends Rectangle> blocks, ColumnTemplate template,
                                                   int pageNumber, int lineNumber) {
        int[] assignment = assign(blocks, template, pageNumber, lineNumber);
        List<Rectangle> row = new ArrayList<>(template.size());
        for (int j = 0; j < template.size(); j++) {
            row.add(null);
        }
        for (int i = 0; i < assignment.length; i++) {
            row.set(assignment[i], blocks.get(i));
        }
        return row;
    }
}
