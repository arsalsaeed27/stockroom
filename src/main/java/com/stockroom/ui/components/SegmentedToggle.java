package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * Row of square, equal-width segments of which exactly one is selected,
 * such as the ALL / IN / LOW / OUT status filter.
 *
 * <p>The segments sit inside a 1px ink frame with ink dividers. The selected
 * segment is solid ink with paper text; the others show orange text on
 * hover. Each segment is a real toggle button, so the control works with
 * the keyboard (Tab and Space) as well as the mouse.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class SegmentedToggle extends JPanel {

    /** Height, in pixels, of the control. */
    private static final int SEGMENT_HEIGHT = 32;

    /** Horizontal padding, in pixels, on each side of a segment's label. */
    private static final int PAD_X = 14;

    /** Font size, in points, of the segment labels. */
    private static final float FONT_SIZE = 11f;

    /** The segments, in display order. */
    private final List<Segment> segments = new ArrayList<>();

    /** Group that keeps exactly one segment selected. */
    private final ButtonGroup group = new ButtonGroup();

    /**
     * Creates a toggle with the given options; the first is selected.
     *
     * @param options segment labels, in display order; also used as each
     *                segment's action command
     */
    public SegmentedToggle(String... options) {
        super(new GridLayout(1, options.length, 0, 0));
        setOpaque(false);
        for (int i = 0; i < options.length; i++) {
            Segment segment = new Segment(options[i], i == 0);
            segments.add(segment);
            group.add(segment);
            add(segment);
        }
        setSelectedIndex(0);
    }

    /**
     * Returns the position of the selected segment.
     *
     * @return the selected index, or -1 if there are no segments
     */
    public int getSelectedIndex() {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects a segment without firing action events.
     *
     * @param index position of the segment to select
     * @throws IndexOutOfBoundsException if {@code index} is not a valid position
     */
    public void setSelectedIndex(int index) {
        segments.get(index).setSelected(true);
    }

    /**
     * Returns the label of the selected segment.
     *
     * @return the selected option, or {@code null} if there are no segments
     */
    public String getSelectedOption() {
        int index = getSelectedIndex();
        return index < 0 ? null : segments.get(index).getText();
    }

    /**
     * Registers a listener that runs when the user clicks a segment. The
     * event's action command is the segment's label.
     *
     * @param listener the listener to add
     */
    public void addActionListener(ActionListener listener) {
        for (Segment segment : segments) {
            segment.addActionListener(listener);
        }
    }

    /**
     * One custom-painted segment of the toggle.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private static final class Segment extends JToggleButton {

        /** Whether this is the first segment, which also draws the left frame edge. */
        private final boolean first;

        /**
         * Creates a segment.
         *
         * @param label the segment label
         * @param first {@code true} for the left-most segment
         */
        private Segment(String label, boolean first) {
            super(label);
            this.first = first;
            setFont(Theme.tracked(Theme.monoSemiBold(FONT_SIZE), Theme.TRACKING_LABEL));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /**
         * Returns the size that fits the label plus padding.
         *
         * @return the preferred size
         */
        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + PAD_X * 2, SEGMENT_HEIGHT);
        }

        /**
         * Paints the segment fill, its frame edges, the label and the focus
         * marker.
         *
         * @param g the graphics context to paint on
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.applyRenderingHints(g.create());
            try {
                ButtonModel model = getModel();
                boolean on = isSelected();
                int width = getWidth();
                int height = getHeight();

                g2.setColor(on ? Theme.INK : Theme.PAPER);
                g2.fillRect(0, 0, width, height);

                g2.setColor(Theme.INK);
                g2.fillRect(0, 0, width, 1);
                g2.fillRect(0, height - 1, width, 1);
                g2.fillRect(width - 1, 0, 1, height);
                if (first) {
                    g2.fillRect(0, 0, 1, height);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                if (on) {
                    g2.setColor(Theme.PAPER);
                } else {
                    g2.setColor(model.isRollover() ? Theme.ACCENT : Theme.INK);
                }
                int textX = (width - fm.stringWidth(getText())) / 2;
                int baseline = (height + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, baseline);

                if (isFocusOwner()) {
                    g2.setStroke(Theme.dashed(1f, 2f, 2f));
                    g2.draw(new Rectangle2D.Float(3.5f, 3.5f, width - 8f, height - 8f));
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
