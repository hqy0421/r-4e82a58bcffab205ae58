package technology.tabula.templates;

/**
 * Thrown when the blocks of a line cannot be placed into the template columns
 * (no assignment keeps every block within its column's maximum deviation) or
 * when several distinct assignments share the same minimal total deviation.
 * The message always carries the 1-based page number and the 1-based line
 * number within that page.
 */
public class TemplateAlignmentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int pageNumber;
    private final int lineNumber;

    public TemplateAlignmentException(String message, int pageNumber, int lineNumber) {
        super(String.format("page %d, line %d: %s", pageNumber, lineNumber, message));
        this.pageNumber = pageNumber;
        this.lineNumber = lineNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
