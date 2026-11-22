package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.Timer;

/**
 * Short-lived notification styled as a printed receipt slip.
 *
 * <p>The toast slides up and fades in at the bottom-right of its window,
 * stays for about 2.5 seconds, then fades out. It looks like a till receipt:
 * a surface-coloured slip with a torn, zig-zag bottom edge, a small header
 * line with the time, a dashed rule, and the message beside a hand-drawn
 * mark (a green tick for success, a red cross for an error, an ink dash for
 * a neutral notice). Error toasts also have a red edge on the left.</p>
 *
 * <p>One toast object is created per window and reused. Showing a new
 * message while one is on screen replaces it and restarts the animation.
 * Animation runs on a {@link Timer}, so all calls must be made on the event
 * dispatch thread.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class Toast extends JComponent {

    /** Length, in milliseconds, of the slide-in animation. */
    private static final long ENTER_MS = 220;

    /** Time, in milliseconds, the toast stays fully visible. */
    private static final long HOLD_MS = 2500;

    /** Length, in milliseconds, of the fade-out animation. */
    private static final long EXIT_MS = 260;

    /** Delay, in milliseconds, between animation frames. */
    private static final int FRAME_MS = 15;

    /** Distance, in pixels, the toast slides up while appearing. */
    private static final int SLIDE_DISTANCE = 28;

    /** Gap, in pixels, between the toast and the right edge of the window. */
    private static final int MARGIN_RIGHT = 24;

    /** Gap, in pixels, between the toast and the bottom edge of the window (clears the status bar). */
    private static final int MARGIN_BOTTOM = 44;

    /** Horizontal padding, in pixels, inside the slip. */
    private static final int PAD_X = 18;

    /** Padding, in pixels, above the header line. */
    private static final int PAD_TOP = 12;

    /** Padding, in pixels, below the last message line. */
    private static final int PAD_BOTTOM = 12;

    /** Space, in pixels, above and below the dashed rule. */
    private static final int RULE_GAP = 9;

    /** Extra space, in pixels, between wrapped message lines. */
    private static final int LINE_GAP = 3;

    /** Width, in pixels, of one tooth of the torn bottom edge. */
    private static final int TOOTH_WIDTH = 10;

    /** Height, in pixels, of one tooth of the torn bottom edge. */
    private static final int TOOTH_HEIGHT = 6;

    /** Width, in pixels, of the red edge on error toasts. */
    private static final int ERROR_EDGE = 4;

    /** Size, in pixels, of the tick or cross mark. */
    private static final int MARK_SIZE = 10;

    /** Gap, in pixels, between the mark and the message text. */
    private static final int MARK_GAP = 10;

    /** Smallest width, in pixels, of the slip. */
    private static final int MIN_WIDTH = 260;

    /** Widest a message line may be, in pixels, before it wraps. */
    private static final int MAX_TEXT_WIDTH = 340;

    /** Smallest gap, in pixels, between the header title and the time. */
    private static final int HEADER_MIN_GAP = 24;

    /** Header title on success toasts. */
    private static final String SUCCESS_TITLE = "LEDGER RECEIPT";

    /** Header title on error and neutral toasts. */
    private static final String NOTICE_TITLE = "LEDGER NOTICE";

    /** Format of the time printed in the header. */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Layered pane of the window the toast floats over. */
    private final JLayeredPane layer;

    /** Timer that drives the animation frames. */
    private final Timer timer;

    /** Small mono font of the header line. */
    private final Font headerFont;

    /** Mono font of the message text. */
    private final Font messageFont;

    /** Message, already wrapped into lines that fit the slip. */
    private List<String> lines = new ArrayList<>();

    /** Kind of the toast currently shown. */
    private Kind kind = Kind.SUCCESS;

    /** Time printed in the header, captured when the toast was shown. */
    private String timeStamp = "";

    /** {@link System#nanoTime()} reading taken when the current toast was shown. */
    private long startNanos;

    /** Current opacity, from 0 (invisible) to 1 (opaque). */
    private float alpha;

    /** Current downward offset, in pixels, from the resting position. */
    private int slideOffset;

    /**
     * Creates a toast for a window. Nothing is shown until
     * {@link #showSuccess(String)} or {@link #showError(String)} is called.
     *
     * @param rootPane root pane of the window the toast should appear in
     */
    public Toast(JRootPane rootPane) {
        layer = rootPane.getLayeredPane();
        timer = new Timer(FRAME_MS, this::onTick);
        headerFont = Theme.tracked(Theme.mono(10f), Theme.TRACKING_LABEL);
        messageFont = Theme.monoSemiBold(13f);
        setOpaque(false);
        setVisible(false);
    }

    /**
     * Shows a success receipt, for example {@code "ENTRY #0007 ADDED"}.
     * A tick is drawn before the message.
     *
     * @param message the message to show
     */
    public void showSuccess(String message) {
        present(message, Kind.SUCCESS);
    }

    /**
     * Shows an error notice with a red edge. A cross is drawn before the
     * message.
     *
     * @param message the error message to show
     */
    public void showError(String message) {
        present(message, Kind.ERROR);
    }

    /**
     * Shows a neutral notice, for example to say the ledger is empty. A short
     * ink dash is drawn before the message.
     *
     * @param message the message to show
     */
    public void showInfo(String message) {
        present(message, Kind.INFO);
    }

    /**
     * Hides the toast at once, without the fade-out.
     */
    public void dismiss() {
        timer.stop();
        if (getParent() == layer) {
            Rectangle bounds = getBounds();
            layer.remove(this);
            layer.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        setVisible(false);
    }

    /**
     * Returns the slip size needed for the current message.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics header = getFontMetrics(headerFont);
        FontMetrics body = getFontMetrics(messageFont);

        int textWidth = header.stringWidth(title())
                + HEADER_MIN_GAP + header.stringWidth(timeStamp);
        for (String line : lines) {
            textWidth = Math.max(textWidth, MARK_SIZE + MARK_GAP + body.stringWidth(line));
        }
        int width = Math.max(MIN_WIDTH, textWidth + PAD_X * 2 + (kind == Kind.ERROR ? ERROR_EDGE : 0));
        width = ((width + TOOTH_WIDTH - 1) / TOOTH_WIDTH) * TOOTH_WIDTH;

        int lineCount = Math.max(1, lines.size());
        int height = PAD_TOP + header.getHeight() + RULE_GAP * 2
                + lineCount * body.getHeight() + (lineCount - 1) * LINE_GAP
                + PAD_BOTTOM + TOOTH_HEIGHT;
        return new Dimension(width, height);
    }

    /**
     * Starts showing a message, replacing any toast already on screen.
     *
     * @param message   the message to show
     * @param toastKind whether this is a success, error or neutral toast
     */
    private void present(String message, Kind toastKind) {
        kind = toastKind;
        timeStamp = LocalTime.now().format(TIME_FORMAT);
        lines = wrap(message == null ? "" : message);

        if (getParent() != layer) {
            layer.add(this, JLayeredPane.POPUP_LAYER);
        }
        setSize(getPreferredSize());
        startNanos = System.nanoTime();
        updateFrame(0);
        setVisible(true);
        timer.restart();
    }

    /**
     * Advances the animation by one frame; called by the timer.
     *
     * @param e the timer event (not used)
     */
    private void onTick(ActionEvent e) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000L;
        if (elapsed >= ENTER_MS + HOLD_MS + EXIT_MS) {
            dismiss();
        } else {
            updateFrame(elapsed);
        }
    }

    /**
     * Sets opacity and position for a moment in the animation: slide in and
     * fade in, hold, then fade out while drifting down slightly.
     *
     * @param elapsed milliseconds since the toast was shown
     */
    private void updateFrame(long elapsed) {
        if (elapsed < ENTER_MS) {
            double t = easeOut((double) elapsed / ENTER_MS);
            alpha = (float) t;
            slideOffset = (int) Math.round((1 - t) * SLIDE_DISTANCE);
        } else if (elapsed < ENTER_MS + HOLD_MS) {
            alpha = 1f;
            slideOffset = 0;
        } else {
            double t = (double) (elapsed - ENTER_MS - HOLD_MS) / EXIT_MS;
            alpha = (float) Math.max(0, 1 - t);
            slideOffset = (int) Math.round(t * SLIDE_DISTANCE / 2.0);
        }
        setLocation(layer.getWidth() - getWidth() - MARGIN_RIGHT,
                layer.getHeight() - getHeight() - MARGIN_BOTTOM + slideOffset);
        repaint();
    }

    /**
     * Cubic ease-out curve: starts fast and settles gently.
     *
     * @param t progress from 0 to 1
     * @return eased progress from 0 to 1
     */
    private static double easeOut(double t) {
        double inverse = 1 - Math.min(1, Math.max(0, t));
        return 1 - inverse * inverse * inverse;
    }

    /**
     * Splits a message into lines no wider than {@link #MAX_TEXT_WIDTH},
     * breaking at spaces. A single word longer than the limit keeps its own
     * line.
     *
     * @param message the message to wrap
     * @return the wrapped lines, at least one
     */
    private List<String> wrap(String message) {
        FontMetrics fm = getFontMetrics(messageFont);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : message.trim().split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && fm.stringWidth(candidate) > MAX_TEXT_WIDTH) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        result.add(current.toString());
        return result;
    }

    /**
     * Paints the receipt slip at the current opacity.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, Math.max(0f, alpha))));
            int width = getWidth();
            int bodyBottom = getHeight() - TOOTH_HEIGHT;

            Path2D slip = slipOutline(width, bodyBottom);
            g2.setColor(Theme.SURFACE);
            g2.fill(slip);
            g2.setColor(Theme.RULE);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(slip);

            int left = PAD_X;
            if (kind == Kind.ERROR) {
                g2.setColor(Theme.STATUS_OUT);
                g2.fillRect(0, 0, ERROR_EDGE, bodyBottom);
                left += ERROR_EDGE;
            }
            paintContent(g2, left, width - PAD_X);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Builds the outline of the slip: a rectangle whose bottom edge is a row
     * of torn zig-zag teeth. Coordinates are inset by half a pixel so the
     * outline stroke is not clipped.
     *
     * @param width      width of the slip
     * @param bodyBottom y position where the teeth start
     * @return the slip outline
     */
    private static Path2D slipOutline(int width, int bodyBottom) {
        float left = 0.5f;
        float right = width - 0.5f;
        Path2D path = new Path2D.Float();
        path.moveTo(left, 0.5f);
        path.lineTo(right, 0.5f);
        path.lineTo(right, bodyBottom);
        for (float x = right; x > left; x -= TOOTH_WIDTH) {
            path.lineTo(Math.max(left, x - TOOTH_WIDTH / 2f), bodyBottom + TOOTH_HEIGHT - 0.5f);
            path.lineTo(Math.max(left, x - TOOTH_WIDTH), bodyBottom);
        }
        path.closePath();
        return path;
    }

    /**
     * Paints the header line, the dashed rule, the mark and the message.
     *
     * @param g2    the graphics context to paint on
     * @param left  x position where content starts
     * @param right x position where content ends
     */
    private void paintContent(Graphics2D g2, int left, int right) {
        g2.setFont(headerFont);
        FontMetrics header = g2.getFontMetrics();
        int y = PAD_TOP + header.getAscent();
        g2.setColor(Theme.MUTED);
        g2.drawString(title(), left, y);
        g2.drawString(timeStamp, right - header.stringWidth(timeStamp), y);

        y += header.getDescent() + RULE_GAP;
        g2.setStroke(Theme.dashed(1f, 3f, 3f));
        g2.setColor(Theme.RULE);
        g2.draw(new Line2D.Float(left, y + 0.5f, right, y + 0.5f));
        y += RULE_GAP;

        g2.setFont(messageFont);
        FontMetrics body = g2.getFontMetrics();
        int textLeft = left + MARK_SIZE + MARK_GAP;
        int capHeight = body.getAscent() - body.getDescent();
        for (int i = 0; i < lines.size(); i++) {
            int baseline = y + body.getAscent();
            if (i == 0) {
                paintMark(g2, left, baseline - capHeight / 2 - MARK_SIZE / 2);
            }
            g2.setColor(Theme.INK);
            g2.drawString(lines.get(i), textLeft, baseline);
            y += body.getHeight() + LINE_GAP;
        }
    }

    /**
     * Returns the header title for the current kind of toast.
     *
     * @return "LEDGER RECEIPT" for success, "LEDGER NOTICE" otherwise
     */
    private String title() {
        return kind == Kind.SUCCESS ? SUCCESS_TITLE : NOTICE_TITLE;
    }

    /**
     * Draws the success tick, the error cross or the neutral dash as simple
     * strokes.
     *
     * @param g2 the graphics context to paint on
     * @param x  left edge of the mark
     * @param y  top edge of the mark
     */
    private void paintMark(Graphics2D g2, int x, int y) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        Path2D mark = new Path2D.Float();
        if (kind == Kind.INFO) {
            g2.setColor(Theme.INK);
            mark.moveTo(x + 1, y + MARK_SIZE / 2f);
            mark.lineTo(x + MARK_SIZE - 1, y + MARK_SIZE / 2f);
        } else if (kind == Kind.ERROR) {
            g2.setColor(Theme.STATUS_OUT);
            mark.moveTo(x + 1, y + 1);
            mark.lineTo(x + MARK_SIZE - 1, y + MARK_SIZE - 1);
            mark.moveTo(x + MARK_SIZE - 1, y + 1);
            mark.lineTo(x + 1, y + MARK_SIZE - 1);
        } else {
            g2.setColor(Theme.STATUS_IN);
            mark.moveTo(x, y + MARK_SIZE * 0.55f);
            mark.lineTo(x + MARK_SIZE * 0.38f, y + MARK_SIZE * 0.9f);
            mark.lineTo(x + MARK_SIZE, y + MARK_SIZE * 0.1f);
        }
        g2.draw(mark);
    }

    /**
     * Kinds of toast, each with its own mark and edge.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private enum Kind {

        /** Success receipt with a green tick. */
        SUCCESS,

        /** Error notice with a red cross and a red left edge. */
        ERROR,

        /** Neutral notice with an ink dash. */
        INFO
    }
}
