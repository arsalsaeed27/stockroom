package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Square, custom-painted button in the Stockroom Ledger style.
 *
 * <p>Labels are drawn in uppercase, letter-spaced mono type. There are three
 * {@link Variant variants}:</p>
 * <ul>
 *   <li>{@link Variant#PRIMARY}: solid ink with paper text, turning orange on hover;</li>
 *   <li>{@link Variant#SECONDARY}: ink outline, turning orange on hover;</li>
 *   <li>{@link Variant#DESTRUCTIVE}: red outline, filling red on hover.</li>
 * </ul>
 *
 * <p>The button has square corners and no gradient or shadow. A pressed
 * button nudges its label down by one pixel, and keyboard focus is shown as
 * a dashed inner frame.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LedgerButton extends JButton {

    /** Height, in pixels, of every ledger button. */
    private static final int HEIGHT = 40;

    /** Horizontal padding, in pixels, on each side of the label. */
    private static final int PAD_X = 20;

    /** Smallest width, in pixels, a ledger button may have. */
    private static final int MIN_WIDTH = 96;

    /** Width, in pixels, of the outline on outlined variants. */
    private static final float OUTLINE = 1.5f;

    /** Inset, in pixels, of the dashed keyboard-focus frame. */
    private static final int FOCUS_INSET = 4;

    /** Font size, in points, of the label. */
    private static final float FONT_SIZE = 12f;

    /** Visual variant of this button. */
    private final Variant variant;

    /**
     * Creates a ledger button.
     *
     * @param text    the label; it is shown in uppercase
     * @param variant the visual variant
     */
    public LedgerButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(Theme.tracked(Theme.monoSemiBold(FONT_SIZE), Theme.TRACKING_LABEL));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setOpaque(false);
        setBorder(new EmptyBorder(0, PAD_X, 0, PAD_X));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Returns this button's visual variant.
     *
     * @return the variant
     */
    public Variant getVariant() {
        return variant;
    }

    /**
     * Returns the size that fits the label plus padding, at the standard
     * button height.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int width = Math.max(MIN_WIDTH, fm.stringWidth(label()) + PAD_X * 2);
        return new Dimension(width, HEIGHT);
    }

    /**
     * Returns the preferred size, so layouts never squash the label.
     *
     * @return the minimum size
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Paints the whole button: background, outline, label and focus frame.
     * The default look-and-feel painting is skipped entirely.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            ButtonModel model = getModel();
            boolean enabled = isEnabled();
            boolean hover = enabled && model.isRollover();
            boolean pressed = enabled && model.isArmed() && model.isPressed();

            Color fill = fillColor(enabled, hover);
            Color outline = outlineColor(enabled, hover);
            Color text = textColor(enabled, hover);
            int width = getWidth();
            int height = getHeight();

            if (fill != null) {
                g2.setColor(fill);
                g2.fillRect(0, 0, width, height);
            }
            if (outline != null) {
                float half = OUTLINE / 2f;
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(OUTLINE));
                g2.draw(new Rectangle2D.Float(half, half, width - OUTLINE, height - OUTLINE));
            }

            g2.setFont(getFont());
            g2.setColor(text);
            FontMetrics fm = g2.getFontMetrics();
            String label = label();
            int textX = (width - fm.stringWidth(label)) / 2;
            int textY = (height - (fm.getAscent() - fm.getDescent())) / 2
                    + (fm.getAscent() - fm.getDescent()) + (pressed ? 1 : 0);
            g2.drawString(label, textX, textY);

            if (isFocusOwner()) {
                g2.setStroke(Theme.dashed(1f, 2f, 2f));
                g2.draw(new Rectangle2D.Float(FOCUS_INSET + 0.5f, FOCUS_INSET + 0.5f,
                        width - FOCUS_INSET * 2f - 1f, height - FOCUS_INSET * 2f - 1f));
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Returns the label text in uppercase, as it is drawn.
     *
     * @return the uppercase label, or an empty string if there is no text
     */
    private String label() {
        String text = getText();
        return text == null ? "" : text.toUpperCase(Locale.ROOT);
    }

    /**
     * Works out the background fill for the current state.
     *
     * @param enabled whether the button is enabled
     * @param hover   whether the mouse is over the button
     * @return the fill colour, or {@code null} for no fill
     */
    private Color fillColor(boolean enabled, boolean hover) {
        if (!enabled) {
            return variant == Variant.PRIMARY ? Theme.RULE : null;
        }
        return switch (variant) {
            case PRIMARY -> hover ? Theme.ACCENT : Theme.INK;
            case SECONDARY -> null;
            case DESTRUCTIVE -> hover ? Theme.STATUS_OUT : null;
        };
    }

    /**
     * Works out the outline colour for the current state.
     *
     * @param enabled whether the button is enabled
     * @param hover   whether the mouse is over the button
     * @return the outline colour, or {@code null} for no outline
     */
    private Color outlineColor(boolean enabled, boolean hover) {
        if (variant == Variant.PRIMARY) {
            return null;
        }
        if (!enabled) {
            return Theme.RULE;
        }
        return switch (variant) {
            case SECONDARY -> hover ? Theme.ACCENT : Theme.INK;
            default -> Theme.STATUS_OUT;
        };
    }

    /**
     * Works out the label colour for the current state.
     *
     * @param enabled whether the button is enabled
     * @param hover   whether the mouse is over the button
     * @return the label colour
     */
    private Color textColor(boolean enabled, boolean hover) {
        if (!enabled) {
            return variant == Variant.PRIMARY ? Theme.SURFACE : Theme.MUTED;
        }
        return switch (variant) {
            case PRIMARY -> Theme.PAPER;
            case SECONDARY -> hover ? Theme.ACCENT : Theme.INK;
            case DESTRUCTIVE -> hover ? Theme.PAPER : Theme.STATUS_OUT;
        };
    }

    /**
     * Visual variants of a {@link LedgerButton}.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public enum Variant {

        /** Solid ink button with paper text; orange on hover. Used for the main action. */
        PRIMARY,

        /** Ink-outlined button; orange outline and text on hover. Used for secondary actions. */
        SECONDARY,

        /** Red-outlined button; solid red on hover. Used for deleting. */
        DESTRUCTIVE
    }
}
