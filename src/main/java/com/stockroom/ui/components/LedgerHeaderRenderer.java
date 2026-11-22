package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;

/**
 * Column header renderer for the ledger table.
 *
 * <p>Headers are small-caps mono in muted grey with wide letter-spacing, on
 * the paper background, with a 1px ink rule underneath. The column the
 * table is sorted by shows an orange arrow (up for ascending, down for
 * descending).</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LedgerHeaderRenderer extends JComponent implements TableCellRenderer {

    /** Height, in pixels, of the header row. */
    private static final int HEIGHT = 36;

    /** Padding, in pixels, before left-aligned headers. */
    private static final int PAD_LEFT = 14;

    /** Padding, in pixels, after right-aligned headers. */
    private static final int PAD_RIGHT = 18;

    /** Gap, in pixels, between the header text and the sort arrow. */
    private static final int ARROW_GAP = 6;

    /** Letter-spacing of header text. */
    private static final float TRACKING = 0.16f;

    /** Arrow shown on a column sorted in ascending order. */
    private static final String ARROW_UP = "↑";

    /** Arrow shown on a column sorted in descending order. */
    private static final String ARROW_DOWN = "↓";

    /** Whether the header text is right-aligned, to match numeric columns. */
    private final boolean rightAligned;

    /** Font of the header text. */
    private final Font headerFont;

    /** Header text being rendered, in uppercase. */
    private String text = "";

    /** Sort arrow to draw, or an empty string if the column is not sorted. */
    private String arrow = "";

    /**
     * Creates a header renderer.
     *
     * @param rightAligned {@code true} to right-align the text, for numeric columns
     */
    public LedgerHeaderRenderer(boolean rightAligned) {
        this.rightAligned = rightAligned;
        this.headerFont = Theme.tracked(Theme.monoSemiBold(10.5f), TRACKING);
        setOpaque(true);
    }

    /**
     * Prepares this component to paint one column header.
     *
     * @param table      the table whose header is painted
     * @param value      the column name
     * @param isSelected not used for headers
     * @param hasFocus   not used for headers
     * @param row        not used for headers
     * @param column     view column index of the header
     * @return this component, configured for the header
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        text = value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
        arrow = sortArrow(table, column);
        return this;
    }

    /**
     * Returns the preferred size, which fixes the header row height.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(headerFont);
        return new Dimension(fm.stringWidth(text) + PAD_LEFT + PAD_RIGHT, HEIGHT);
    }

    /**
     * Works out which arrow, if any, the column should show.
     *
     * @param table  the table
     * @param column view column index
     * @return the up or down arrow, or an empty string if unsorted
     */
    private static String sortArrow(JTable table, int column) {
        if (table == null || table.getRowSorter() == null || column < 0) {
            return "";
        }
        List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
        if (keys.isEmpty() || keys.get(0).getColumn() != table.convertColumnIndexToModel(column)) {
            return "";
        }
        SortOrder order = keys.get(0).getSortOrder();
        if (order == SortOrder.ASCENDING) {
            return ARROW_UP;
        }
        return order == SortOrder.DESCENDING ? ARROW_DOWN : "";
    }

    /**
     * Paints the background, the ink rule underneath, the text and the
     * sort arrow.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            int width = getWidth();
            int height = getHeight();
            g2.setColor(Theme.PAPER);
            g2.fillRect(0, 0, width, height);
            g2.setColor(Theme.INK);
            g2.fillRect(0, height - 1, width, 1);

            g2.setFont(headerFont);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (height + fm.getAscent() - fm.getDescent()) / 2;
            int textWidth = fm.stringWidth(text);
            int arrowWidth = arrow.isEmpty() ? 0 : fm.stringWidth(arrow) + ARROW_GAP;

            int textX;
            int arrowX;
            if (rightAligned) {
                textX = width - PAD_RIGHT - textWidth;
                arrowX = textX - arrowWidth;
            } else {
                textX = PAD_LEFT;
                arrowX = textX + textWidth + ARROW_GAP;
            }
            g2.setColor(Theme.MUTED);
            g2.drawString(text, textX, baseline);
            if (!arrow.isEmpty()) {
                g2.setColor(Theme.ACCENT);
                g2.drawString(arrow, arrowX, baseline);
            }
        } finally {
            g2.dispose();
        }
    }
}
