package technology.tabula.templates;

/**
 * Which edge of a text block is measured against a column anchor.
 * <ul>
 *     <li>{@link #LEFT}: left-aligned column, the block's left edge is scored.</li>
 *     <li>{@link #RIGHT}: right-aligned column, the block's right edge is scored.</li>
 * </ul>
 */
public enum ColumnAnchor {
    LEFT,
    RIGHT;

    public static ColumnAnchor parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("column alignment is required, use \"left\" or \"right\"");
        }
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "left":
            case "l":
                return LEFT;
            case "right":
            case "r":
                return RIGHT;
            default:
                throw new IllegalArgumentException(
                        String.format(java.util.Locale.ROOT,
                                "unknown column alignment \"%s\", use \"left\" or \"right\"", raw));
        }
    }
}
