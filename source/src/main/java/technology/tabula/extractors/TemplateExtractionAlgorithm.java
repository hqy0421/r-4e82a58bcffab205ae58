package technology.tabula.extractors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import technology.tabula.Line;
import technology.tabula.Page;
import technology.tabula.Table;
import technology.tabula.TextChunk;
import technology.tabula.TextElement;
import technology.tabula.templates.ColumnTemplate;
import technology.tabula.templates.TemplateRowAssigner;

/**
 * Stream-mode extraction driven by an optional {@link ColumnTemplate}.
 *
 * <p>The existing word-merging ({@link TextElement#mergeWords}) and line
 * grouping ({@link TextChunk#groupByLines}) steps are reused unchanged. For
 * every line, the non-empty text blocks are placed into distinct template
 * columns from left to right: each block is scored by the distance from its
 * alignment edge to the column anchor (in 0.01 pt), columns outside the
 * configured maximum deviation are unavailable, and the unique assignment
 * with the smallest total deviation is chosen. Empty columns may be skipped
 * but are kept in the output, including trailing empty columns. Lines without
 * a legal assignment or with a tied optimum raise a
 * {@link technology.tabula.templates.TemplateAlignmentException} carrying the
 * page and line number.</p>
 */
public class TemplateExtractionAlgorithm implements ExtractionAlgorithm {

    private final ColumnTemplate template;

    public TemplateExtractionAlgorithm(ColumnTemplate template) {
        this.template = template;
    }

    public ColumnTemplate getTemplate() {
        return template;
    }

    @Override
    public List<Table> extract(Page page) {
        List<TextElement> textElements = page.getText();

        if (textElements.size() == 0) {
            return Arrays.asList(new Table[] { Table.empty() });
        }

        // reuse the existing word merging and line grouping used by stream mode
        List<TextChunk> textChunks = TextElement.mergeWords(textElements);
        List<Line> lines = TextChunk.groupByLines(textChunks);

        Table table = new Table(this);
        table.setRect(page.getLeft(), page.getTop(), page.getWidth(), page.getHeight());
        table.setPageNumber(page.getPageNumber());

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            Line line = lines.get(lineIndex);
            List<TextChunk> elements = line.getTextElements();

            Collections.sort(elements, new Comparator<TextChunk>() {
                @Override
                public int compare(TextChunk o1, TextChunk o2) {
                    return Float.compare(o1.getLeft(), o2.getLeft());
                }
            });

            List<TextChunk> blocks = new ArrayList<>();
            for (TextChunk tc : elements) {
                if (tc.isSameChar(Line.WHITE_SPACE_CHARS)) {
                    continue;
                }
                blocks.add(tc);
            }

            if (blocks.isEmpty()) {
                continue;
            }

            int lineNumber = lineIndex + 1;
            int[] assignment = TemplateRowAssigner.assign(
                    blocks, template, page.getPageNumber(), lineNumber);

            for (int i = 0; i < blocks.size(); i++) {
                table.add(blocks.get(i), lineIndex, assignment[i]);
            }
        }

        // keep empty trailing columns (and any other skipped columns) in the output
        table.ensureColumnCount(template.size());

        return Arrays.asList(new Table[] { table });
    }

    @Override
    public String toString() {
        return "stream-template";
    }
}
