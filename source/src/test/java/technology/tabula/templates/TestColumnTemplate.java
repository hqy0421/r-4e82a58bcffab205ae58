package technology.tabula.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class TestColumnTemplate {

    @Test
    public void parsesFullTemplateWithPerColumnOverride() {
        String json = "{"
                + "\"maxDeviation\": 2.0,"
                + "\"columns\": ["
                + "  {\"name\": \"account\", \"anchor\": 36.0, \"align\": \"left\"},"
                + "  {\"name\": \"debit\",   \"anchor\": 200.0, \"align\": \"right\", \"maxDeviation\": 3.5},"
                + "  {\"name\": \"credit\",  \"anchor\": 300.0, \"align\": \"R\"}"
                + "]}";
        ColumnTemplate template = ColumnTemplate.parse(new StringReader(json));
        assertEquals(2.0f, template.getDefaultMaxDeviation(), 0f);
        assertEquals(3, template.size());

        List<ColumnSpec> columns = template.getColumns();
        assertEquals("account", columns.get(0).getName());
        assertEquals(ColumnAnchor.LEFT, columns.get(0).getAnchorEdge());
        assertEquals(2.0f, columns.get(0).getMaxDeviation(), 0f);

        assertEquals(3.5f, columns.get(1).getMaxDeviation(), 0f);
        assertEquals(ColumnAnchor.RIGHT, columns.get(1).getAnchorEdge());
        assertEquals(ColumnAnchor.RIGHT, columns.get(2).getAnchorEdge());
    }

    @Test
    public void rejectsMissingTopLevelMaxDeviation() {
        String json = "{\"columns\": [{\"anchor\": 36, \"align\": \"left\"}]}";
        expectError(json, "maxDeviation");
    }

    @Test
    public void rejectsEmptyColumns() {
        String json = "{\"maxDeviation\": 2, \"columns\": []}";
        expectError(json, "at least one column");
    }

    @Test
    public void rejectsMissingAnchor() {
        String json = "{\"maxDeviation\": 2, \"columns\": [{\"align\": \"left\"}]}";
        expectError(json, "anchor");
    }

    @Test
    public void rejectsMissingAlignment() {
        String json = "{\"maxDeviation\": 2, \"columns\": [{\"anchor\": 36}]}";
        expectError(json, "align");
    }

    @Test
    public void rejectsUnknownAlignment() {
        String json = "{\"maxDeviation\": 2, \"columns\": [{\"anchor\": 36, \"align\": \"center\"}]}";
        expectError(json, "alignment");
    }

    @Test
    public void rejectsNegativeTolerance() {
        String json = "{\"maxDeviation\": -1, \"columns\": [{\"anchor\": 36, \"align\": \"left\"}]}";
        expectError(json, "maxDeviation");
    }

    @Test
    public void rejectsMalformedJson() {
        expectError("{not json", "JSON");
    }

    private static void expectError(String json, String expectedFragment) {
        try {
            ColumnTemplate.parse(new StringReader(json));
            fail("expected IllegalArgumentException mentioning " + expectedFragment);
        } catch (IllegalArgumentException e) {
            org.junit.Assert.assertTrue(
                    "expected message to contain \"" + expectedFragment + "\", got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains(expectedFragment.toLowerCase()));
        }
    }
}
