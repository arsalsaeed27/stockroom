package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * Panel painted as a paper slip: a surface-coloured sheet with a
 * perforated, dashed tear-off edge near the top.
 *
 * <p>A <em>framed</em> slip (used by {@link SlipDialog}) has a 2px ink frame;
 * an unframed slip (used for the Entry Slip beside the ledger) has a 1px
 * rule-coloured outline. Content should leave at least
 * {@link #PERFORATION_CLEARANCE} pixels of top inset so it sits below the
 * perforation.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class SlipPanel extends JPanel {

    /** Top inset, in pixels, that content needs to clear the perforated edge. */
    public static final int PERFORATION_CLEARANCE = 22;

    /** Distance, in pixels, from the top of the slip to the perforated edge. */
    private static final int PERFORATION_Y = 11;

    /** Horizontal inset, in pixels, of the perforated edge from the sides. */
    private static final int PERFORATION_INSET = 10;

    /** Width, in pixels, of the ink frame on framed slips. */
    private static final int FRAME = 2;

    /** Whether the slip has a heavy ink frame instead of a light outline. */
    private final boolean framed;

    /**
     * Creates a slip panel.
     *
     * @param framed {@code true} for a 2px ink frame, {@code false} for a 1px rule outline
     */
    public SlipPanel(boolean framed) {
        this.framed = framed;
        setOpaque(true);
        setBackground(Theme.SURFACE);
    }

    /**
     * Paints the slip background, the perforated edge and the frame or
     * outline.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            int width = getWidth();
            int height = getHeight();
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, width, height);
            Theme.paintPerforation(g2, PERFORATION_INSET, PERFORATION_Y, width - PERFORATION_INSET * 2);

            int line = framed ? FRAME : 1;
            g2.setColor(framed ? Theme.INK : Theme.RULE);
            g2.fillRect(0, 0, width, line);
            g2.fillRect(0, height - line, width, line);
            g2.fillRect(0, 0, line, height);
            g2.fillRect(width - line, 0, line, height);
        } finally {
            g2.dispose();
        }
    }
}
