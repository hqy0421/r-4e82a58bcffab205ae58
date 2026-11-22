package technology.tabula.templates;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import technology.tabula.Rectangle;

public class TestTemplateRowAssigner {

    private static Rectangle block(float left, float width) {
        // top, left, width, height; right edge == left + width
        return new Rectangle(0f, left, width, 10f);
    }

    /**
     * Four columns:
     *  0: left-aligned  anchor 36
     *  1: right-aligned anchor 200
     *  2: right-aligned anchor 300
     *  3: left-aligned  anchor 320
     * default tolerance 2pt.
     */
    private ColumnTemplate template(float tolerance) {
        return new ColumnTemplate(tolerance, Arrays.asList(
                new ColumnSpec("c0", 36f, ColumnAnchor.LEFT, tolerance),
                new ColumnSpec("c1", 200f, ColumnAnchor.RIGHT, tolerance),
                new ColumnSpec("c2", 300f, ColumnAnchor.RIGHT, tolerance),
                new ColumnSpec("c3", 320f, ColumnAnchor.LEFT, tolerance)));
    }

    @Test
    public void assignsBlocksToDistinctColumnsAllowingSkippedColumns() {
        // blocks: account at 36, amount at right-anchor 300 (middle amount c1 missing)
        List<Rectangle> blocks = Arrays.asList(block(36f, 40f), block(260f, 40f));
        int[] assignment = TemplateRowAssigner.assign(blocks, template(2f), 1, 1);
        assertArrayEquals(new int[] {0, 2}, assignment);
    }

    @Test
    public void fillsAllColumnsWhenEveryBlockPresent() {
        List<Rectangle> blocks = Arrays.asList(
                block(36f, 40f),    // c0 left edge 36
                block(170f, 30f),   // c1 right edge 200
                block(270f, 30f),   // c2 right edge 300
                block(320f, 50f));  // c3 left edge 320
        int[] assignment = TemplateRowAssigner.assign(blocks, template(2f), 1, 1);
        assertArrayEquals(new int[] {0, 1, 2, 3}, assignment);
    }

    @Test
    public void choosesMinimumTotalDeviationAssignment() {
        // block0 fits c0 (dev 1.0) and c1 poorly; make c0 deviation 1, c1's right edge far away
        List<Rectangle> blocks = Arrays.asList(
                block(37f, 40f),    // left 37 -> c0 dev 1.0
                block(261f, 40f));  // right 301 -> c2 dev 1.0
        int[] assignment = TemplateRowAssigner.assign(blocks, template(2f), 3, 7);
        assertArrayEquals(new int[] {0, 2}, assignment);
    }

    @Test
    public void rejectsColumnOutsideMaxDeviation() {
        // second block right edge 305 -> 5pt off c2's anchor, tolerance 2 -> illegal
        List<Rectangle> blocks = Arrays.asList(block(36f, 40f), block(265f, 40f));
        try {
            TemplateRowAssigner.assign(blocks, template(2f), 2, 5);
            fail("expected TemplateAlignmentException");
        } catch (TemplateAlignmentException e) {
            assertEquals(2, e.getPageNumber());
            assertEquals(5, e.getLineNumber());
            assertContains(e.getMessage(), "page 2, line 5");
        }
    }

    @Test
    public void perColumnToleranceOverridesDefault() {
        ColumnTemplate t = new ColumnTemplate(2f, Arrays.asList(
                new ColumnSpec("c0", 36f, ColumnAnchor.LEFT, 2f),
                new ColumnSpec("c1", 300f, ColumnAnchor.RIGHT, 6f)));
        // right edge 305 is 5pt off c1: rejected with default 2pt, allowed with 6pt override
        List<Rectangle> blocks = Arrays.asList(block(36f, 40f), block(265f, 40f));
        int[] assignment = TemplateRowAssigner.assign(blocks, t, 1, 1);
        assertArrayEquals(new int[] {0, 1}, assignment);
    }

    @Test
    public void failsWhenMoreBlocksThanColumns() {
        List<Rectangle> blocks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            blocks.add(block(36f + i * 60f, 10f));
        }
        try {
            TemplateRowAssigner.assign(blocks, template(2f), 1, 2);
            fail("expected TemplateAlignmentException");
        } catch (TemplateAlignmentException e) {
            assertContains(e.getMessage().toLowerCase(Locale.ROOT), "5 non-empty blocks");
        }
    }

    @Test
    public void emptyLineProducesEmptyAssignment() {
        int[] assignment = TemplateRowAssigner.assign(new ArrayList<Rectangle>(), template(2f), 1, 1);
        assertEquals(0, assignment.length);
    }

    @Test
    public void detectsTiedOptima() {
        // Two identical-width columns with the same anchor distance structure so two
        // assignments tie. Columns c1 and c2 share right-anchor deviation for the block:
        ColumnTemplate t = new ColumnTemplate(5f, Arrays.asList(
                new ColumnSpec("c0", 36f, ColumnAnchor.LEFT, 5f),
                new ColumnSpec("c1", 200f, ColumnAnchor.RIGHT, 5f),
                new ColumnSpec("c2", 200f, ColumnAnchor.RIGHT, 5f)));
        // single block whose right edge is 200: placing it in c1 or c2 gives equal score
        List<Rectangle> blocks = Arrays.asList(block(160f, 40f));
        try {
            TemplateRowAssigner.assign(blocks, t, 4, 9);
            fail("expected TemplateAlignmentException for tied optimum");
        } catch (TemplateAlignmentException e) {
            assertEquals(4, e.getPageNumber());
            assertEquals(9, e.getLineNumber());
            assertContains(e.getMessage().toLowerCase(Locale.ROOT), "ambiguous");
        }
    }

    @Test
    public void scoresInHundredthsOfAPoint() {
        ColumnSpec left = new ColumnSpec("c0", 100f, ColumnAnchor.LEFT, 5f);
        Rectangle b = block(100.37f, 10f); // 0.37pt deviation -> 37 hundredths
        assertEquals(37L, left.deviationHundredths(b));
        assertEquals(true, left.accepts(b));

        Rectangle far = block(105.01f, 10f);
        assertEquals(false, left.accepts(far));
    }

    @Test
    public void assignToColumnsKeepsSkippedColumnsAsNull() {
        List<Rectangle> blocks = Arrays.asList(block(36f, 40f), block(260f, 40f));
        List<Rectangle> row = TemplateRowAssigner.assignToColumns(blocks, template(2f), 1, 1);
        assertEquals(4, row.size());
        assertEquals(blocks.get(0), row.get(0));
        assertEquals(null, row.get(1));
        assertEquals(blocks.get(1), row.get(2));
        assertEquals(null, row.get(3));
    }

    private static void assertContains(String haystack, String needle) {
        org.junit.Assert.assertTrue(
                String.format(Locale.ROOT, "expected message to contain \"%s\", got \"%s\"", needle, haystack),
                haystack.contains(needle));
    }
}
