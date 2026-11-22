package com.stockroom.ui.components;

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
 * Plain-text table cell renderer for the ledger: line numbers, IDs, names
 * and numbers, each in its own {@link Kind style}.
 *
 * <p>Every cell paints the paper background, or the surface colour when its
 * row is selected. The {@link Kind#LINE_NUMBER} cell, the first column,
 * also paints a 3px orange bar on the left edge of a selected row. Text that
 * does not fit is cut off with an ellipsis, and the full text is shown as a
 * tooltip.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LedgerCellRenderer extends JComponent implements TableCellRenderer {

    /** Width, in pixels, of the orange selection bar. */
    public static final int SELECTION_BAR_WIDTH = 3;

    /** Padding, in pixels, before left-aligned text. */
    private static final int PAD_LEFT = 14;

    /** Padding, in pixels, after right-aligned text. */
    private static final int PAD_RIGHT = 18;

    /** Ellipsis added to text that is cut off. */
    private static final String ELLIPSIS = "…";

    /** Style of cell this renderer draws. */
    private final Kind kind;

    /** Font used for the cell text. */
    private final Font cellFont;

    /** Text to draw in the current cell. */
    private String text = "";

    /** Whether the row being rendered is selected. */
    private boolean selected;

    /**
     * Creates a renderer for one style of cell.
     *
     * @param kind the cell style
     */
    public LedgerCellRenderer(Kind kind) {
        this.kind = kind;
        this.cellFont = switch (kind) {
            case LINE_NUMBER, ID -> Theme.mono(12f);
            case TEXT -> Theme.monoMedium(14f);
            case NUMBER -> Theme.mono(14f);
        };
        setOpaque(true);
    }

    /**
     * Prepares this component to paint one cell.
     *
     * @param table      the table being painted
     * @param value      the cell value
     * @param isSelected whether the cell's row is selected
     * @param hasFocus   whether the cell has keyboard focus (not used)
     * @param row        view row index of the cell; line numbers use it so
     *                   they always count 1, 2, 3 from the top
     * @param column     view column index of the cell
     * @return this component, configured for the cell
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        selected = isSelected;
        text = format(value, row);
        setToolTipText(kind == Kind.TEXT && !text.isEmpty() ? text : null);
        return this;
    }

    /**
     * Turns a cell value into the text shown for this renderer's style.
     *
     * @param value the cell value
     * @param row   view row index of the cell
     * @return the text to draw
     */
    private String format(Object value, int row) {
        return switch (kind) {
            case LINE_NUMBER -> String.format(Locale.ROOT, "%03d", row + 1);
            case ID -> value instanceof Number id ? String.format(Locale.ROOT, "#%04d", id.intValue()) : "";
            case NUMBER -> value instanceof Number n ? String.format(Locale.ROOT, "%,d", n.longValue()) : "";
            case TEXT -> value == null ? "" : value.toString();
        };
    }

    /**
     * Paints the background, the selection bar for line numbers, and the
     * text.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            int width = getWidth();
            int height = getHeight();
            g2.setColor(selected ? Theme.SURFACE : Theme.PAPER);
            g2.fillRect(0, 0, width, height);
            if (selected && kind == Kind.LINE_NUMBER) {
                g2.setColor(Theme.ACCENT);
                g2.fillRect(0, 0, SELECTION_BAR_WIDTH, height);
            }

            g2.setFont(cellFont);
            FontMetrics fm = g2.getFontMetrics();
            boolean rightAligned = kind == Kind.NUMBER;
            String shown = fit(text, fm, width - PAD_LEFT - PAD_RIGHT);
            int capHeight = fm.getAscent() - fm.getDescent();
            int baseline = (height + capHeight) / 2;
            int x = rightAligned ? width - PAD_RIGHT - fm.stringWidth(shown) : PAD_LEFT;
            g2.setColor(textColor());
            g2.drawString(shown, x, baseline);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Returns the text colour for this renderer's style: muted for line
     * numbers and IDs, ink for names and numbers.
     *
     * @return the text colour
     */
    private Color textColor() {
        return (kind == Kind.LINE_NUMBER || kind == Kind.ID) ? Theme.MUTED : Theme.INK;
    }

    /**
     * Shortens text with an ellipsis until it fits the available width.
     *
     * @param value     the full text
     * @param fm        metrics of the cell font
     * @param available width available, in pixels
     * @return the text, shortened if needed
     */
    private static String fit(String value, FontMetrics fm, int available) {
        if (fm.stringWidth(value) <= available) {
            return value;
        }
        int end = value.length();
        while (end > 0 && fm.stringWidth(value.substring(0, end) + ELLIPSIS) > available) {
            end--;
        }
        return value.substring(0, end) + ELLIPSIS;
    }

    /**
     * Cell styles drawn by {@link LedgerCellRenderer}.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public enum Kind {

        /** Ledger line number such as "007", counted from the top of the view; draws the selection bar. */
        LINE_NUMBER,

        /** Item ID, zero-padded with a hash, such as "#0007". */
        ID,

        /** Left-aligned text, such as the item name. */
        TEXT,

        /** Right-aligned whole number with thousands separators. */
        NUMBER
    }
}
