package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import javax.swing.JComponent;

/**
 * Horizontal dashed divider, used to separate sections on a slip.
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class DashedRule extends JComponent {

    /** Preferred height, in pixels, of the divider including breathing room. */
    private static final int HEIGHT = 9;

    /** Line width, in pixels, of the dashes. */
    private static final float LINE_WIDTH = 1f;

    /**
     * Creates a dashed divider.
     */
    public DashedRule() {
        setOpaque(false);
    }

    /**
     * Returns a zero-width size with the divider's height, so layouts
     * stretch it across the available width.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, HEIGHT);
    }

    /**
     * Paints the dashed line across the middle of the component.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            float y = getHeight() / 2f + 0.5f;
            g2.setColor(Theme.MUTED);
            g2.setStroke(Theme.dashed(LINE_WIDTH, 4f, 4f));
            g2.draw(new Line2D.Float(0, y, getWidth(), y));
        } finally {
            g2.dispose();
        }
    }
}
