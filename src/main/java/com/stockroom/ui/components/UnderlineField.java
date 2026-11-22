package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Transparent text input that draws only an underline, like a blank line on
 * a paper form.
 *
 * <p>The underline is 1px ink at rest, 2px orange while the field has
 * focus and 2px status red while it shows an error. An optional placeholder
 * is drawn in muted grey while the field is empty, for example
 * "FIND IN LEDGER&hellip;".</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class UnderlineField extends JTextField {

    /** Thickness, in pixels, of the underline at rest. */
    private static final int LINE_REST = 1;

    /** Thickness, in pixels, of the underline with focus or an error. */
    private static final int LINE_ACTIVE = 2;

    /** Whether the field is showing an error. */
    private boolean error;

    /** Hint drawn while the field is empty; empty for none. */
    private String placeholder = "";

    /**
     * Creates an underline field using the regular mono font.
     *
     * @param fontSize point size of the typed text
     */
    public UnderlineField(float fontSize) {
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 8, 0));
        setFont(Theme.mono(fontSize));
        setForeground(Theme.INK);
        setCaretColor(Theme.INK);
        setSelectionColor(Theme.ACCENT_TINT);
        setSelectedTextColor(Theme.INK);
        setDisabledTextColor(Theme.MUTED);
    }

    /**
     * Switches the red error underline on or off.
     *
     * @param error {@code true} to show the error underline
     */
    public void setError(boolean error) {
        this.error = error;
        repaint();
    }

    /**
     * Reports whether the error underline is shown.
     *
     * @return {@code true} if the field is in the error state
     */
    public boolean hasError() {
        return error;
    }

    /**
     * Sets the hint drawn while the field is empty.
     *
     * @param placeholder the hint text; {@code null} or empty for none
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        repaint();
    }

    /**
     * Returns the hint drawn while the field is empty.
     *
     * @return the placeholder text, or an empty string if there is none
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Handles focus changes as usual, then repaints so the underline colour
     * follows the focus.
     *
     * @param e the focus event
     */
    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        repaint();
    }

    /**
     * Paints the text, the placeholder when empty, and the underline in the
     * colour for the current state.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Theme.applyRenderingHints(g);
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (getDocument().getLength() == 0 && !placeholder.isEmpty()) {
                paintPlaceholder(g2);
            }
            boolean focused = isFocusOwner();
            if (error) {
                g2.setColor(Theme.STATUS_OUT);
            } else if (focused) {
                g2.setColor(Theme.ACCENT);
            } else {
                g2.setColor(isEnabled() ? Theme.INK : Theme.RULE);
            }
            int thickness = (error || focused) ? LINE_ACTIVE : LINE_REST;
            g2.fillRect(0, getHeight() - thickness, getWidth(), thickness);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Draws the placeholder text on the same baseline as typed text.
     *
     * @param g2 the graphics context to paint on
     */
    private void paintPlaceholder(Graphics2D g2) {
        g2.setFont(getFont());
        g2.setColor(Theme.MUTED);
        FontMetrics fm = g2.getFontMetrics();
        Insets insets = getInsets();
        int baseline = getBaseline(getWidth(), getHeight());
        if (baseline < 0) {
            baseline = insets.top + fm.getAscent();
        }
        g2.drawString(placeholder, insets.left, baseline);
    }
}
