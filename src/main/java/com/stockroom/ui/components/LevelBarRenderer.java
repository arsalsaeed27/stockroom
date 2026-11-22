package com.stockroom.ui.components;

import com.stockroom.model.Item;
import com.stockroom.ui.theme.Theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Table cell renderer for the quantity column: a right-aligned mono
 * numeral with a thin level bar beneath it.
 *
 * <p>The bar compares the quantity with the item's low-stock threshold.
 * A full bar means twice the threshold or more, so the threshold always sits
 * at the middle of the bar, where a small ink tick marks it. The filled part
 * uses the item's status colour.</p>
 *
 * <p>The cell value should be the row's {@link Item}, so the renderer can
 * read both quantity and threshold. A plain {@link Number} is shown as a
 * numeral without a bar; any other value leaves the cell blank.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LevelBarRenderer extends JComponent implements TableCellRenderer {

    /** Width, in pixels, of the level bar. */
    private static final int BAR_WIDTH = 64;

    /** Height, in pixels, of the level bar. */
    private static final int BAR_HEIGHT = 3;

    /** Vertical gap, in pixels, between the numeral and the bar. */
    private static final int BAR_GAP = 6;

    /** How far, in pixels, the threshold tick sticks out above and below the bar. */
    private static final int TICK_OVERHANG = 2;

    /** Space, in pixels, between the content and the right edge of the cell. */
    private static final int CELL_PADDING_RIGHT = 18;

    /** Font size, in points, of the numeral. */
    private static final float FONT_SIZE = 14f;

    /** Font used for the numeral. */
    private final Font numberFont;

    /** Formatted quantity to draw, or an empty string for a blank cell. */
    private String text = "";

    /** Item being rendered, or {@code null} when there is no bar to draw. */
    private Item item;

    /** Whether the row being rendered is selected. */
    private boolean selected;

    /**
     * Creates a level bar renderer.
     */
    public LevelBarRenderer() {
        numberFont = Theme.monoMedium(FONT_SIZE);
        setOpaque(true);
    }

    /**
     * Prepares this component to paint one quantity cell.
     *
     * @param table      the table being painted
     * @param value      the cell value, expected to be an {@link Item} or a {@link Number}
     * @param isSelected whether the cell's row is selected
     * @param hasFocus   whether the cell has keyboard focus (not used)
     * @param row        view row index of the cell
     * @param column     view column index of the cell
     * @return this component, configured for the cell
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        selected = isSelected;
        if (value instanceof Item rowItem) {
            item = rowItem;
            text = format(rowItem.getQuantity());
            setToolTipText(text + " on hand, low at " + format(rowItem.getThreshold()));
        } else if (value instanceof Number number) {
            item = null;
            text = format(number.longValue());
            setToolTipText(null);
        } else {
            item = null;
            text = "";
            setToolTipText(null);
        }
        return this;
    }

    /**
     * Paints the row background, the numeral and, for items, the level bar.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            g2.setColor(selected ? Theme.SURFACE : Theme.PAPER);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(numberFont);
            FontMetrics fm = g2.getFontMetrics();
            int capHeight = fm.getAscent() - fm.getDescent();
            int blockHeight = item == null ? capHeight : capHeight + BAR_GAP + BAR_HEIGHT;
            int baseline = (getHeight() - blockHeight) / 2 + capHeight;
            int rightEdge = getWidth() - CELL_PADDING_RIGHT;

            g2.setColor(Theme.INK);
            g2.drawString(text, rightEdge - fm.stringWidth(text), baseline);

            if (item != null) {
                paintBar(g2, rightEdge - BAR_WIDTH, baseline + BAR_GAP);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the level bar track, its filled part and the threshold tick.
     *
     * @param g2 the graphics context to paint on
     * @param x  left edge of the bar
     * @param y  top edge of the bar
     */
    private void paintBar(Graphics2D g2, int x, int y) {
        int quantity = item.getQuantity();
        int threshold = item.getThreshold();
        double scale = Math.max(threshold * 2.0, 1.0);

        g2.setColor(Theme.RULE);
        g2.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        int filled = (int) Math.round(BAR_WIDTH * Math.min(1.0, quantity / scale));
        Color fill = Theme.statusColor(item.getStatus());
        g2.setColor(fill);
        g2.fillRect(x, y, filled, BAR_HEIGHT);

        if (threshold > 0) {
            int tickX = x + (int) Math.round(BAR_WIDTH * (threshold / scale));
            g2.setColor(Theme.INK);
            g2.fillRect(tickX, y - TICK_OVERHANG, 1, BAR_HEIGHT + TICK_OVERHANG * 2);
        }
    }

    /**
     * Formats a number with thousands separators, as in a printed ledger.
     *
     * @param value the number to format
     * @return the formatted number, for example {@code "1,250"}
     */
    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
