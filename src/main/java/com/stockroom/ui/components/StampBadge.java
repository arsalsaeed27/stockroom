package com.stockroom.ui.components;

import com.stockroom.model.StockStatus;
import com.stockroom.ui.theme.Theme;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Table cell renderer that shows a {@link StockStatus} as a rubber stamp.
 *
 * <p>The stamp is the status label in uppercase, letter-spaced mono type,
 * framed by a double border in the status colour. It is rotated by
 * &minus;3 degrees and painted with slightly transparent ink, like a stamp
 * pressed onto a ledger page by hand.</p>
 *
 * <p>Cell values that are not a {@link StockStatus} leave the cell blank.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class StampBadge extends JComponent implements TableCellRenderer {

    /** Rotation of the stamp, in radians (&minus;3 degrees). */
    private static final double ROTATION = Math.toRadians(-3);

    /** Opacity of the stamp ink, so it looks pressed rather than printed. */
    private static final float INK_ALPHA = 0.82f;

    /** Width, in pixels, of the stamp's outer border line. */
    private static final float OUTER_STROKE = 1.5f;

    /** Width, in pixels, of the stamp's inner border line. */
    private static final float INNER_STROKE = 0.75f;

    /** Gap, in pixels, between the outer and inner border lines. */
    private static final double INNER_INSET = 2.5;

    /** Horizontal padding, in pixels, between the outer border and the text. */
    private static final int PAD_X = 10;

    /** Vertical padding, in pixels, between the outer border and the text. */
    private static final int PAD_Y = 6;

    /** Space, in pixels, between the left edge of the cell and the stamp. */
    private static final int CELL_PADDING_LEFT = 14;

    /** Letter-spacing of the stamp text. */
    private static final float TRACKING = 0.14f;

    /** Font size, in points, of the stamp text. */
    private static final float FONT_SIZE = 10.5f;

    /** Font used for the stamp text. */
    private final Font stampFont;

    /** Status currently being rendered, or {@code null} for a blank cell. */
    private StockStatus status;

    /** Whether the row being rendered is selected. */
    private boolean selected;

    /**
     * Creates a stamp badge renderer.
     */
    public StampBadge() {
        stampFont = Theme.tracked(Theme.monoBold(FONT_SIZE), TRACKING);
        setOpaque(true);
    }

    /**
     * Prepares this component to paint one status cell.
     *
     * @param table      the table being painted
     * @param value      the cell value, expected to be a {@link StockStatus}
     * @param isSelected whether the cell's row is selected
     * @param hasFocus   whether the cell has keyboard focus (not used)
     * @param row        view row index of the cell
     * @param column     view column index of the cell
     * @return this component, configured for the cell
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        status = value instanceof StockStatus stockStatus ? stockStatus : null;
        selected = isSelected;
        setToolTipText(status == null ? null : status.getDisplayLabel());
        return this;
    }

    /**
     * Returns the size needed to show the widest stamp.
     *
     * @return the preferred size of the cell content
     */
    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        for (StockStatus each : StockStatus.values()) {
            width = Math.max(width, getFontMetrics(stampFont).stringWidth(each.getDisplayLabel()));
        }
        return new Dimension(width + PAD_X * 2 + CELL_PADDING_LEFT * 2, 40);
    }

    /**
     * Paints the row background and, if a status is set, the stamp.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            g2.setColor(selected ? Theme.SURFACE : Theme.PAPER);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (status != null) {
                paintStamp(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the rotated, double-bordered stamp for the current status.
     *
     * @param g2 the graphics context to paint on, with rendering hints set
     */
    private void paintStamp(Graphics2D g2) {
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(status.getDisplayLabel(), stampFont, frc);
        Rectangle2D textBounds = layout.getBounds();

        double boxWidth = textBounds.getWidth() + PAD_X * 2.0;
        double boxHeight = textBounds.getHeight() + PAD_Y * 2.0;
        double centerX = CELL_PADDING_LEFT + boxWidth / 2.0;
        double centerY = getHeight() / 2.0;

        g2.rotate(ROTATION, centerX, centerY);
        g2.setComposite(AlphaComposite.SrcOver.derive(INK_ALPHA));
        g2.setColor(Theme.statusColor(status));

        Rectangle2D outer = new Rectangle2D.Double(centerX - boxWidth / 2.0,
                centerY - boxHeight / 2.0, boxWidth, boxHeight);
        g2.setStroke(new BasicStroke(OUTER_STROKE));
        g2.draw(outer);

        Rectangle2D inner = new Rectangle2D.Double(outer.getX() + INNER_INSET, outer.getY() + INNER_INSET,
                outer.getWidth() - INNER_INSET * 2, outer.getHeight() - INNER_INSET * 2);
        g2.setStroke(new BasicStroke(INNER_STROKE));
        g2.draw(inner);

        float textX = (float) (centerX - textBounds.getWidth() / 2.0 - textBounds.getX());
        float textY = (float) (centerY - textBounds.getHeight() / 2.0 - textBounds.getY());
        layout.draw(g2, textX, textY);
    }
}
