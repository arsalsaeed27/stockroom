package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Scroll bar look for the ledger: a slim, square thumb on the paper
 * background with no arrow buttons, replacing the default Metal scroll bar.
 *
 * <p>The thumb is rule-coloured and darkens to muted grey while the mouse is
 * over it or dragging it.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LedgerScrollBarUI extends BasicScrollBarUI {

    /** Thickness, in pixels, of the scroll bar. */
    private static final int THICKNESS = 10;

    /** Gap, in pixels, between the thumb and the edges of the track. */
    private static final int THUMB_INSET = 2;

    /**
     * Creates the ledger scroll bar look.
     */
    public LedgerScrollBarUI() {
        super();
    }

    /**
     * Returns a slim size for the scroll bar.
     *
     * @param c the scroll bar
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == VERTICAL
                ? new Dimension(THICKNESS, 48)
                : new Dimension(48, THICKNESS);
    }

    /**
     * Replaces the "scroll up" arrow with an invisible, zero-size button.
     *
     * @param orientation the arrow direction (not used)
     * @return an empty button
     */
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    /**
     * Replaces the "scroll down" arrow with an invisible, zero-size button.
     *
     * @param orientation the arrow direction (not used)
     * @return an empty button
     */
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    /**
     * Paints the track as plain paper.
     *
     * @param g           the graphics context to paint on
     * @param c           the scroll bar
     * @param trackBounds area of the track
     */
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(Theme.PAPER);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    /**
     * Paints the thumb as a square bar, darker while hovered or dragged.
     *
     * @param g           the graphics context to paint on
     * @param c           the scroll bar
     * @param thumbBounds area of the thumb
     */
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        g.setColor(isThumbRollover() || isDragging ? Theme.MUTED : Theme.RULE);
        g.fillRect(thumbBounds.x + THUMB_INSET, thumbBounds.y + THUMB_INSET,
                thumbBounds.width - THUMB_INSET * 2, thumbBounds.height - THUMB_INSET * 2);
    }

    /**
     * Creates a button that takes up no space, used in place of arrows.
     *
     * @return a zero-size, unfocusable button
     */
    private static JButton emptyButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        button.setFocusable(false);
        return button;
    }
}
