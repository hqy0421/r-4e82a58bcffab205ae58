package technology.tabula.templates;

import technology.tabula.Rectangle;

/**
 * Specification of a single template column: the x coordinate of its anchor,
 * which block edge is measured against that anchor and the maximum deviation
 * (in points) allowed for a block to be assigned to the column.
 */
public class ColumnSpec {

    private final String name;
    private final float anchor;
    private final ColumnAnchor anchorEdge;
    private final float maxDeviation;

    public ColumnSpec(String name, float anchor, ColumnAnchor anchorEdge, float maxDeviation) {
        if (!Float.isFinite(anchor)) {
            throw new IllegalArgumentException("column anchor must be a finite number");
        }
        if (!Float.isFinite(maxDeviation) || maxDeviation < 0) {
            throw new IllegalArgumentException("column max deviation must be a finite, non-negative number");
        }
        this.name = name;
        this.anchor = anchor;
        this.anchorEdge = anchorEdge;
        this.maxDeviation = maxDeviation;
    }

    public String getName() {
        return name;
    }

    public float getAnchor() {
        return anchor;
    }

    public ColumnAnchor getAnchorEdge() {
        return anchorEdge;
    }

    public float getMaxDeviation() {
        return maxDeviation;
    }

    /**
     * Returns the x coordinate of the block edge that is measured against this
     * column's anchor: the left edge for a left-aligned column, the right edge
     * for a right-aligned column.
     */
    public float measuredEdge(Rectangle block) {
        return anchorEdge == ColumnAnchor.LEFT ? block.getLeft() : block.getRight();
    }

    /**
     * Returns the absolute distance between the block's relevant edge and the
     * anchor, measured in hundredths of a point (0.01 pt).
     */
    public long deviationHundredths(Rectangle block) {
        float edge = measuredEdge(block);
        return Math.round(Math.abs(edge - anchor) * 100f);
    }

    /**
     * A block may be placed in this column only if the deviation of its
     * relevant edge from the anchor does not exceed the configured maximum.
     */
    public boolean accepts(Rectangle block) {
        float edge = measuredEdge(block);
        return Math.abs(edge - anchor) <= maxDeviation;
    }
}
