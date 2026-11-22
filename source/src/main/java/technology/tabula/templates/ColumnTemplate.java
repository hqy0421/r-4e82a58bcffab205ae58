package technology.tabula.templates;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * An optional column template used by stream-mode extraction of borderless
 * tables. Every column declares an anchor x coordinate and whether block left
 * or right edges are measured against it. A default maximum deviation (in
 * points) applies to every column and may be overridden per column.
 *
 * <p>Template JSON shape:</p>
 * <pre>
 * {
 *   "maxDeviation": 2.0,
 *   "columns": [
 *     {"name": "account", "anchor": 36.0,  "align": "left"},
 *     {"name": "amount",  "anchor": 320.0, "align": "right", "maxDeviation": 3.0}
 *   ]
 * }
 * </pre>
 */
public class ColumnTemplate {

    private final float defaultMaxDeviation;
    private final List<ColumnSpec> columns;

    public ColumnTemplate(float defaultMaxDeviation, List<ColumnSpec> columns) {
        if (!Float.isFinite(defaultMaxDeviation) || defaultMaxDeviation < 0) {
            throw new IllegalArgumentException("maxDeviation must be a finite, non-negative number");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("template must declare at least one column");
        }
        this.defaultMaxDeviation = defaultMaxDeviation;
        this.columns = new ArrayList<>(columns);
    }

    public float getDefaultMaxDeviation() {
        return defaultMaxDeviation;
    }

    public List<ColumnSpec> getColumns() {
        return columns;
    }

    public int size() {
        return columns.size();
    }

    public static ColumnTemplate load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    public static ColumnTemplate parse(Reader reader) {
        final JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || parsed.isJsonNull()) {
                throw new IllegalArgumentException("column template is empty");
            }
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("column template root must be a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("invalid column template JSON: " + e.getMessage(), e);
        }

        if (!root.has("maxDeviation") || root.get("maxDeviation").isJsonNull()) {
            throw new IllegalArgumentException("column template requires a top-level \"maxDeviation\" (points)");
        }
        float defaultMaxDeviation = readFloat(root, "maxDeviation");
        if (!Float.isFinite(defaultMaxDeviation) || defaultMaxDeviation < 0) {
            throw new IllegalArgumentException("maxDeviation must be a finite, non-negative number");
        }

        if (!root.has("columns") || !root.get("columns").isJsonArray()) {
            throw new IllegalArgumentException("column template requires a \"columns\" array");
        }
        JsonArray columnsJson = root.getAsJsonArray("columns");
        if (columnsJson.size() == 0) {
            throw new IllegalArgumentException("column template must declare at least one column");
        }

        List<ColumnSpec> columns = new ArrayList<>(columnsJson.size());
        for (int i = 0; i < columnsJson.size(); i++) {
            JsonElement element = columnsJson.get(i);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "column %d must be a JSON object", i + 1));
            }
            JsonObject columnJson = element.getAsJsonObject();

            if (!columnJson.has("anchor") || columnJson.get("anchor").isJsonNull()) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "column %d is missing its \"anchor\"", i + 1));
            }
            float anchor = readFloat(columnJson, "anchor");

            JsonElement alignElement = columnJson.has("align") ? columnJson.get("align")
                    : columnJson.get("alignment");
            if (alignElement == null || alignElement.isJsonNull()) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "column %d is missing its \"align\" (\"left\" or \"right\")", i + 1));
            }
            ColumnAnchor anchorEdge = ColumnAnchor.parse(alignElement.getAsString());

            float maxDeviation = defaultMaxDeviation;
            if (columnJson.has("maxDeviation") && !columnJson.get("maxDeviation").isJsonNull()) {
                maxDeviation = readFloat(columnJson, "maxDeviation");
            }
            if (!Float.isFinite(maxDeviation) || maxDeviation < 0) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "column %d maxDeviation must be a finite, non-negative number", i + 1));
            }

            String name = (columnJson.has("name") && !columnJson.get("name").isJsonNull())
                    ? columnJson.get("name").getAsString()
                    : null;

            columns.add(new ColumnSpec(name, anchor, anchorEdge, maxDeviation));
        }

        return new ColumnTemplate(defaultMaxDeviation, columns);
    }

    private static float readFloat(JsonObject object, String key) {
        try {
            return object.get(key).getAsFloat();
        } catch (NumberFormatException | ClassCastException e) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "\"%s\" must be a number", key));
        }
    }
}
