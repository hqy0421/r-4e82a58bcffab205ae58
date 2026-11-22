package technology.tabula.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;

import technology.tabula.Page;
import technology.tabula.PageDims;
import technology.tabula.Table;
import technology.tabula.TextElement;
import technology.tabula.extractors.TemplateExtractionAlgorithm;

public class TestTemplateExtractionAlgorithm {

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float FONT_SIZE = 10f;
    private static final float SPACE_WIDTH = 2.5f;
    private static final float CHAR_HEIGHT = 10f;

    private TextElement word(float top, float left, String text) {
        // rough width: 6pt per character, enough to keep words separated
        return new TextElement(top, left, text.length() * 6f, CHAR_HEIGHT,
                FONT, FONT_SIZE, text, SPACE_WIDTH);
    }

    private Page pageWith(List<TextElement> elements) {
        PDPage pdPage = new PDPage();
        PDDocument doc = new PDDocument();
        return Page.Builder.newInstance()
                .withPageDims(PageDims.of(0f, 0f, 600f, 800f))
                .withRotation(0)
                .withNumber(7)
                .withPdPage(pdPage)
                .withPdDocument(doc)
                .withTextElements(elements)
                .withRulings(new ArrayList<technology.tabula.Ruling>())
                .withMinCharWidth(1f)
                .withMinCharHeight(1f)
                .build();
    }

    /**
     * Settlement-detail layout:
     *   c0 account name  left-aligned anchor 36
     *   c1 debit amount  right-aligned anchor 200
     *   c2 credit amount right-aligned anchor 300
     */
    private ColumnTemplate template(float tolerance) {
        return new ColumnTemplate(tolerance, Arrays.asList(
                new ColumnSpec("account", 36f, ColumnAnchor.LEFT, tolerance),
                new ColumnSpec("debit", 200f, ColumnAnchor.RIGHT, tolerance),
                new ColumnSpec("credit", 300f, ColumnAnchor.RIGHT, tolerance)));
    }

    @Test
    public void missingMiddleAmountDoesNotShiftLaterValue() {
        List<TextElement> elements = new ArrayList<>();
        // line 1: account, debit 100.00 (right edge 200), credit 200.00 (right edge 300)
        elements.add(word(100f, 36f, "CASH"));
        elements.add(word(100f, 164f, "100.00")); // width 36 -> right 200
        elements.add(word(100f, 264f, "200.00")); // width 36 -> right 300
        // line 2: account and credit only, debit missing -> credit must stay in column 2
        elements.add(word(120f, 36f, "RENT"));
        elements.add(word(120f, 264f, "50.00"));  // width 30 -> right 294, dev 6? adjust below
        // re-place the credit exactly on anchor: 6 chars * 6 = 36 wide, left 264 -> right 300
        elements.set(elements.size() - 1, word(120f, 264f, "500.00"));

        Table table = new TemplateExtractionAlgorithm(template(2f)).extract(pageWith(elements)).get(0);

        assertEquals(3, table.getColCount());
        assertEquals(2, table.getRowCount());
        assertEquals("CASH", table.getCell(0, 0).getText());
        assertEquals("100.00", table.getCell(0, 1).getText());
        assertEquals("200.00", table.getCell(0, 2).getText());

        assertEquals("RENT", table.getCell(1, 0).getText());
        assertEquals("", table.getCell(1, 1).getText()); // skipped empty column kept
        assertEquals("500.00", table.getCell(1, 2).getText()); // not shifted into column 1
    }

    @Test
    public void trailingEmptyColumnsArePreserved() {
        List<TextElement> elements = new ArrayList<>();
        elements.add(word(100f, 36f, "ONLY")); // account column only
        Table table = new TemplateExtractionAlgorithm(template(2f)).extract(pageWith(elements)).get(0);

        assertEquals(3, table.getColCount());
        List<technology.tabula.RectangularTextContainer> row = table.getRows().get(0);
        assertEquals(3, row.size());
        assertEquals("ONLY", row.get(0).getText());
        assertEquals("", row.get(1).getText());
        assertEquals("", row.get(2).getText());
    }

    @Test
    public void blockOutsideToleranceRaisesErrorWithPageAndLine() {
        List<TextElement> elements = new ArrayList<>();
        elements.add(word(100f, 36f, "CASH"));
        elements.add(word(100f, 164f, "100.00"));
        elements.add(word(100f, 264f, "200.00"));
        // line 2: value 10pt away from every right-aligned anchor
        elements.add(word(120f, 36f, "RENT"));
        elements.add(word(120f, 180f, "999.99")); // right edge 216, dev 16 from 200

        try {
            new TemplateExtractionAlgorithm(template(2f)).extract(pageWith(elements));
            fail("expected TemplateAlignmentException");
        } catch (TemplateAlignmentException e) {
            assertEquals(7, e.getPageNumber());
            assertEquals(2, e.getLineNumber());
            org.junit.Assert.assertTrue(e.getMessage().contains("page 7, line 2"));
        }
    }

    @Test
    public void tiedOptimumRaisesErrorWithPageAndLine() {
        ColumnTemplate t = new ColumnTemplate(10f, Arrays.asList(
                new ColumnSpec("a", 36f, ColumnAnchor.LEFT, 10f),
                new ColumnSpec("b", 200f, ColumnAnchor.RIGHT, 10f),
                new ColumnSpec("c", 200f, ColumnAnchor.RIGHT, 10f)));
        List<TextElement> elements = new ArrayList<>();
        elements.add(word(100f, 164f, "100.00")); // right 200: fits b and c equally

        try {
            new TemplateExtractionAlgorithm(t).extract(pageWith(elements));
            fail("expected TemplateAlignmentException");
        } catch (TemplateAlignmentException e) {
            assertEquals(7, e.getPageNumber());
            assertEquals(1, e.getLineNumber());
        }
    }

    @Test
    public void emptyPageReturnsEmptyTable() {
        Table table = new TemplateExtractionAlgorithm(template(2f))
                .extract(pageWith(new ArrayList<TextElement>())).get(0);
        assertEquals(0, table.getRowCount());
    }
}
