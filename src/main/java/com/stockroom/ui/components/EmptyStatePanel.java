package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

/**
 * Panel shown in place of the ledger table when there is nothing to list.
 *
 * <p>It paints a centred box with a dashed border, holding a large
 * Space Grotesk title ("LEDGER EMPTY" by default) above a muted mono message
 * ("No entries on file. Press Ctrl+N to add your first item." by default).
 * The title and message can be changed, so the same panel can explain an
 * empty search result.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class EmptyStatePanel extends JPanel {

    /** Default title, shown when the inventory has no items. */
    public static final String DEFAULT_TITLE = "LEDGER EMPTY";

    /** Default message, shown when the inventory has no items. */
    public static final String DEFAULT_MESSAGE = "No entries on file. Press Ctrl+N to add your first item.";

    /** Horizontal padding, in pixels, inside the dashed box. */
    private static final int BOX_PAD_X = 56;

    /** Vertical padding, in pixels, inside the dashed box. */
    private static final int BOX_PAD_Y = 44;

    /** Gap, in pixels, between the title and the message. */
    private static final int TEXT_GAP = 12;

    /** Space, in pixels, kept around the box when computing the preferred size. */
    private static final int OUTER_MARGIN = 24;

    /** Line width, in pixels, of the dashed border. */
    private static final float BORDER_WIDTH = 1.5f;

    /** Font of the title. */
    private final Font titleFont;

    /** Font of the message. */
    private final Font messageFont;

    /** Title currently shown. */
    private String title;

    /** Message currently shown. */
    private String message;

    /**
     * Creates an empty-state panel with the default title and message.
     */
    public EmptyStatePanel() {
        this(DEFAULT_TITLE, DEFAULT_MESSAGE);
    }

    /**
     * Creates an empty-state panel with a custom title and message.
     *
     * @param title   large heading inside the box
     * @param message muted line below the heading
     */
    public EmptyStatePanel(String title, String message) {
        this.title = title;
        this.message = message;
        titleFont = Theme.tracked(Theme.display(40f), Theme.TRACKING_TIGHT);
        messageFont = Theme.mono(13f);
        setOpaque(true);
        setBackground(Theme.PAPER);
    }

    /**
     * Returns the title currently shown.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Changes the title and repaints.
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
        revalidate();
        repaint();
    }

    /**
     * Returns the message currently shown.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Changes the message and repaints.
     *
     * @param message the new message
     */
    public void setMessage(String message) {
        this.message = message;
        revalidate();
        repaint();
    }

    /**
     * Returns the size of the box plus a margin around it.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics titleMetrics = getFontMetrics(titleFont);
        FontMetrics messageMetrics = getFontMetrics(messageFont);
        int width = boxWidth(titleMetrics, messageMetrics) + OUTER_MARGIN * 2;
        int height = boxHeight(titleMetrics, messageMetrics) + OUTER_MARGIN * 2;
        return new Dimension(width, height);
    }

    /**
     * Paints the paper background, the dashed box, the title and the
     * message, all centred in the panel.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            g2.setColor(Theme.PAPER);
            g2.fillRect(0, 0, getWidth(), getHeight());

            FontMetrics titleMetrics = g2.getFontMetrics(titleFont);
            FontMetrics messageMetrics = g2.getFontMetrics(messageFont);
            int boxWidth = Math.min(boxWidth(titleMetrics, messageMetrics), getWidth() - 2);
            int boxHeight = boxHeight(titleMetrics, messageMetrics);
            int boxX = (getWidth() - boxWidth) / 2;
            int boxY = (getHeight() - boxHeight) / 2;

            float half = BORDER_WIDTH / 2f;
            g2.setColor(Theme.MUTED);
            g2.setStroke(Theme.dashed(BORDER_WIDTH, 7f, 5f));
            g2.draw(new Rectangle2D.Float(boxX + half, boxY + half,
                    boxWidth - BORDER_WIDTH, boxHeight - BORDER_WIDTH));

            int centerX = getWidth() / 2;
            int titleBaseline = boxY + BOX_PAD_Y + capHeight(titleMetrics);
            g2.setFont(titleFont);
            g2.setColor(Theme.INK);
            g2.drawString(title, centerX - titleMetrics.stringWidth(title) / 2, titleBaseline);

            int messageBaseline = titleBaseline + TEXT_GAP + capHeight(messageMetrics) + messageMetrics.getDescent();
            g2.setFont(messageFont);
            g2.setColor(Theme.MUTED);
            g2.drawString(message, centerX - messageMetrics.stringWidth(message) / 2, messageBaseline);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Works out the width of the dashed box from the wider of the two
     * text lines.
     *
     * @param titleMetrics   metrics of the title font
     * @param messageMetrics metrics of the message font
     * @return the box width in pixels
     */
    private int boxWidth(FontMetrics titleMetrics, FontMetrics messageMetrics) {
        int text = Math.max(titleMetrics.stringWidth(title), messageMetrics.stringWidth(message));
        return text + BOX_PAD_X * 2;
    }

    /**
     * Works out the height of the dashed box from both text lines.
     *
     * @param titleMetrics   metrics of the title font
     * @param messageMetrics metrics of the message font
     * @return the box height in pixels
     */
    private int boxHeight(FontMetrics titleMetrics, FontMetrics messageMetrics) {
        return BOX_PAD_Y * 2 + capHeight(titleMetrics) + TEXT_GAP
                + capHeight(messageMetrics) + messageMetrics.getDescent();
    }

    /**
     * Estimates the height of capital letters for a font, so text is
     * centred by its visible ink rather than by its line height.
     *
     * @param metrics metrics of the font
     * @return the approximate cap height in pixels
     */
    private static int capHeight(FontMetrics metrics) {
        return metrics.getAscent() - metrics.getDescent();
    }
}
