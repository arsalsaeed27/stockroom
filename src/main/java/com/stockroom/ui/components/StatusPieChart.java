package com.stockroom.ui.components;

import com.stockroom.model.StockStatus;
import com.stockroom.service.StatusBreakdown;
import com.stockroom.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Animated donut chart of the inventory's IN / LOW / OUT item counts, with a
 * ledger-style legend on the right.
 *
 * <p>The chart only draws: the counts come in as a {@link StatusBreakdown}
 * through {@link #setBreakdown(StatusBreakdown)}. Slices run clockwise from
 * 12 o'clock in {@link StatusBreakdown#ORDER}, in the warm dashboard colours
 * from {@link Theme#chartColor(StockStatus)}, separated by thin paper-coloured
 * gaps. The centre shows the total item count; an empty inventory shows a
 * dashed grey ring and "NO ENTRIES".</p>
 *
 * <p>Everything moves on one {@link Timer} at about 60 frames a second, which
 * stops as soon as nothing is moving:</p>
 * <ul>
 *   <li>{@link #playIntro()} sweeps the slices in clockwise over
 *       {@value #INTRO_MS} ms;</li>
 *   <li>a new breakdown moves the slice boundaries from their old to their
 *       new positions over {@value #TRANSITION_MS} ms while the centre number
 *       counts up or down;</li>
 *   <li>the slice under the mouse pops outward and brightens, its legend row
 *       is highlighted, and a receipt-style tooltip describes it.</li>
 * </ul>
 *
 * <p>Clicking a slice or a legend row calls the
 * {@linkplain #setOnStatusSelected(Consumer) status handler}; clicking the
 * centre calls the {@linkplain #setOnCentreSelected(Runnable) centre
 * handler}.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class StatusPieChart extends JComponent {

    /** Delay between animation frames, in milliseconds (about 60 fps). */
    private static final int FRAME_MS = 16;

    /** Length of the opening sweep, in milliseconds. */
    private static final int INTRO_MS = 700;

    /** Length of the move from old to new values, in milliseconds. */
    private static final int TRANSITION_MS = 520;

    /** How far, in pixels, a hovered slice moves outward. */
    private static final double POP_DISTANCE = 10.0;

    /** Share of the remaining distance the hover pop covers each frame (exponential ease-out). */
    private static final double POP_EASING = 0.24;

    /** Pop progress below which the pop counts as settled. */
    private static final double POP_EPSILON = 0.004;

    /** How much a fully popped slice is blended towards white. */
    private static final float HIGHLIGHT_MIX = 0.16f;

    /** Ring thickness as a share of the outer radius. */
    private static final double RING_SHARE = 0.38;

    /** Largest outer radius of the ring, in pixels. */
    private static final int MAX_RADIUS = 150;

    /** Width, in pixels, of the paper-coloured gap drawn around each slice. */
    private static final float SEPARATOR_WIDTH = 3f;

    /** Space, in pixels, kept around the chart and legend. */
    private static final int PAD = 24;

    /** Width, in pixels, of the legend on the right. */
    private static final int LEGEND_WIDTH = 236;

    /** Height, in pixels, of one legend row. */
    private static final int LEGEND_ROW = 40;

    /** Size, in pixels, of a legend colour square. */
    private static final int SWATCH = 12;

    /** Horizontal padding, in pixels, inside the tooltip. */
    private static final int TIP_PAD_X = 12;

    /** Vertical padding, in pixels, inside the tooltip above the text. */
    private static final int TIP_PAD_Y = 9;

    /** Width, in pixels, of one torn tooth along the tooltip's bottom edge. */
    private static final int TOOTH_WIDTH = 8;

    /** Height, in pixels, of the torn teeth along the tooltip's bottom edge. */
    private static final int TOOTH_HEIGHT = 4;

    /** Offset, in pixels, of the tooltip from the mouse pointer. */
    private static final int TIP_OFFSET = 16;

    /** The breakdown being shown (the target of any running transition). */
    private StatusBreakdown breakdown = StatusBreakdown.EMPTY;

    /** Slice fractions, in {@link StatusBreakdown#ORDER}, at the start of the running transition. */
    private final double[] fromFractions = new double[3];

    /** Slice fractions, in {@link StatusBreakdown#ORDER}, at the end of the running transition. */
    private final double[] toFractions = new double[3];

    /** Slice fractions, in {@link StatusBreakdown#ORDER}, as drawn in the current frame. */
    private final double[] shownFractions = new double[3];

    /** Hover pop of each slice, from 0 (resting) to 1 (fully popped). */
    private final double[] pops = new double[3];

    /** Centre number at the start of the running transition. */
    private double fromTotal;

    /** Centre number as drawn in the current frame. */
    private double shownTotal;

    /** {@link System#nanoTime()} when the running transition began, or -1 if none. */
    private long transitionStart = -1;

    /** {@link System#nanoTime()} when the opening sweep began, or -1 if none is running. */
    private long introStart = -1;

    /** How much of the ring the opening sweep has revealed, from 0 to 1. */
    private double sweep = 1.0;

    /** Index in {@link StatusBreakdown#ORDER} of the hovered slice or legend row, or -1. */
    private int hovered = -1;

    /** Whether the mouse is over the centre of the ring. */
    private boolean centreHovered;

    /** Last mouse position inside the component, used to place the tooltip; {@code null} when outside. */
    private Point mouse;

    /** Called with a status when a slice or legend row is clicked; may be {@code null}. */
    private Consumer<StockStatus> onStatusSelected;

    /** Called when the centre of the ring is clicked; may be {@code null}. */
    private Runnable onCentreSelected;

    /** Drives every animation; runs only while something is moving. */
    private final Timer timer;

    /** Large mono font of the centre numeral. */
    private final Font numeralFont;

    /** Small tracked caption font, for "ITEMS" and the legend header. */
    private final Font captionFont;

    /** Font of the legend labels. */
    private final Font legendFont;

    /** Font of the legend numbers. */
    private final Font legendNumberFont;

    /** Font of the tooltip text. */
    private final Font tipFont;

    /**
     * Creates an empty chart. Call {@link #setBreakdown(StatusBreakdown)} to
     * give it data and {@link #playIntro()} to sweep it in.
     */
    public StatusPieChart() {
        timer = new Timer(FRAME_MS, this::onTick);
        numeralFont = Theme.monoMedium(46f);
        captionFont = Theme.tracked(Theme.monoSemiBold(10f), Theme.TRACKING_LABEL);
        legendFont = Theme.tracked(Theme.monoSemiBold(11f), Theme.TRACKING_LABEL);
        legendNumberFont = Theme.monoMedium(13f);
        tipFont = Theme.tracked(Theme.monoSemiBold(11f), 0.06f);
        setOpaque(true);
        setBackground(Theme.PAPER);

        ChartMouseHandler mouseHandler = new ChartMouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /**
     * Sets the counts to show. The slices move smoothly from where they are
     * now to the new shares, and the centre number counts to the new total.
     *
     * @param newBreakdown the new counts
     * @throws NullPointerException if {@code newBreakdown} is {@code null}
     */
    public void setBreakdown(StatusBreakdown newBreakdown) {
        Objects.requireNonNull(newBreakdown, "newBreakdown");
        if (newBreakdown.equals(breakdown)) {
            return;
        }
        breakdown = newBreakdown;
        System.arraycopy(shownFractions, 0, fromFractions, 0, 3);
        for (int i = 0; i < 3; i++) {
            toFractions[i] = newBreakdown.fraction(StatusBreakdown.ORDER.get(i));
        }
        fromTotal = shownTotal;
        transitionStart = System.nanoTime();
        if (hovered >= 0 && newBreakdown.count(StatusBreakdown.ORDER.get(hovered)) == 0) {
            hovered = -1;
        }
        startTimer();
    }

    /**
     * Returns the counts being shown.
     *
     * @return the current breakdown
     */
    public StatusBreakdown getBreakdown() {
        return breakdown;
    }

    /**
     * Sweeps the slices in clockwise from 12 o'clock while the centre number
     * counts up from zero. Any value transition is finished at once first.
     */
    public void playIntro() {
        finishTransition();
        sweep = 0;
        introStart = System.nanoTime();
        startTimer();
    }

    /**
     * Sets what happens when a slice or legend row is clicked.
     *
     * @param handler called with the clicked status, or {@code null} for nothing
     */
    public void setOnStatusSelected(Consumer<StockStatus> handler) {
        this.onStatusSelected = handler;
    }

    /**
     * Sets what happens when the centre of the ring is clicked.
     *
     * @param handler called on a centre click, or {@code null} for nothing
     */
    public void setOnCentreSelected(Runnable handler) {
        this.onCentreSelected = handler;
    }

    /**
     * Stops the animation timer, for example when the window closes.
     */
    public void stopAnimation() {
        timer.stop();
    }

    /**
     * Returns a size that fits the ring and the legend side by side.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(MAX_RADIUS * 2 + LEGEND_WIDTH + PAD * 4, MAX_RADIUS * 2 + PAD * 2);
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    /**
     * Starts the frame timer if it is not already running.
     */
    private void startTimer() {
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    /**
     * Jumps the running value transition to its end values.
     */
    private void finishTransition() {
        System.arraycopy(toFractions, 0, shownFractions, 0, 3);
        shownTotal = breakdown.total();
        transitionStart = -1;
    }

    /**
     * Advances every animation by one frame and stops the timer when
     * nothing is moving any more.
     *
     * @param e the timer event (not used)
     */
    private void onTick(ActionEvent e) {
        long now = System.nanoTime();
        boolean moving = false;

        if (introStart >= 0) {
            double t = (now - introStart) / 1_000_000.0 / INTRO_MS;
            sweep = easeOut(t);
            if (t >= 1) {
                sweep = 1;
                introStart = -1;
            } else {
                moving = true;
            }
        }

        if (transitionStart >= 0) {
            double t = (now - transitionStart) / 1_000_000.0 / TRANSITION_MS;
            if (t >= 1) {
                finishTransition();
            } else {
                double eased = easeOut(t);
                for (int i = 0; i < 3; i++) {
                    shownFractions[i] = fromFractions[i] + (toFractions[i] - fromFractions[i]) * eased;
                }
                shownTotal = fromTotal + (breakdown.total() - fromTotal) * eased;
                moving = true;
            }
        }

        for (int i = 0; i < 3; i++) {
            double target = i == hovered ? 1.0 : 0.0;
            double diff = target - pops[i];
            if (Math.abs(diff) < POP_EPSILON) {
                pops[i] = target;
            } else {
                pops[i] += diff * POP_EASING;
                moving = true;
            }
        }

        repaint();
        if (!moving) {
            timer.stop();
        }
    }

    /**
     * Cubic ease-out curve: starts fast and settles gently.
     *
     * @param t progress from 0 to 1; values outside are clamped
     * @return eased progress from 0 to 1
     */
    private static double easeOut(double t) {
        double inverse = 1 - Math.min(1, Math.max(0, t));
        return 1 - inverse * inverse * inverse;
    }

    // ------------------------------------------------------------------
    // Geometry and hit testing
    // ------------------------------------------------------------------

    /**
     * Returns the outer radius of the ring for the current size.
     *
     * @return the outer radius, in pixels
     */
    private double outerRadius() {
        double byWidth = (getWidth() - LEGEND_WIDTH - PAD * 3) / 2.0 - POP_DISTANCE;
        double byHeight = getHeight() / 2.0 - PAD - POP_DISTANCE;
        return Math.max(40, Math.min(MAX_RADIUS, Math.min(byWidth, byHeight)));
    }

    /**
     * Returns the inner radius of the ring.
     *
     * @param outer the outer radius
     * @return the inner radius, in pixels
     */
    private static double innerRadius(double outer) {
        return outer * (1 - RING_SHARE);
    }

    /**
     * Returns the x position of the ring's centre.
     *
     * @return the centre x, in pixels
     */
    private double centreX() {
        double chartWidth = getWidth() - LEGEND_WIDTH - PAD;
        return chartWidth / 2.0;
    }

    /**
     * Returns the y position of the ring's centre.
     *
     * @return the centre y, in pixels
     */
    private double centreY() {
        return getHeight() / 2.0;
    }

    /**
     * Returns the left edge of the legend.
     *
     * @return the legend x, in pixels
     */
    private int legendX() {
        return getWidth() - LEGEND_WIDTH - PAD;
    }

    /**
     * Returns the top of the first legend row; the legend (header, three rows
     * and total) is centred vertically.
     *
     * @return the y of the first row, in pixels
     */
    private int legendRowsTop() {
        int legendHeight = LEGEND_ROW * 5;
        return (getHeight() - legendHeight) / 2 + LEGEND_ROW;
    }

    /**
     * Returns the area of one legend row, used for painting and clicks.
     *
     * @param index position of the row in {@link StatusBreakdown#ORDER}
     * @return the row's bounds
     */
    private Rectangle legendRow(int index) {
        return new Rectangle(legendX(), legendRowsTop() + index * LEGEND_ROW, LEGEND_WIDTH, LEGEND_ROW);
    }

    /**
     * Returns the start of a slice as a fraction of the full turn, clockwise
     * from 12 o'clock.
     *
     * @param index position of the slice in {@link StatusBreakdown#ORDER}
     * @return the start, from 0 to 1
     */
    private double sliceStart(int index) {
        double start = 0;
        for (int i = 0; i < index; i++) {
            start += shownFractions[i];
        }
        return start;
    }

    /**
     * Finds the slice under a point.
     *
     * @param p the point, in component coordinates
     * @return the slice's index in {@link StatusBreakdown#ORDER}, or -1 if the
     *         point is not on a visible slice
     */
    private int sliceAt(Point p) {
        double dx = p.x - centreX();
        double dy = p.y - centreY();
        double distance = Math.hypot(dx, dy);
        double outer = outerRadius();
        if (distance < innerRadius(outer) || distance > outer + POP_DISTANCE) {
            return -1;
        }
        double angle = Math.atan2(dx, -dy);
        double turn = (angle < 0 ? angle + 2 * Math.PI : angle) / (2 * Math.PI);
        if (turn > sweep) {
            return -1;
        }
        double start = 0;
        for (int i = 0; i < 3; i++) {
            double end = start + shownFractions[i];
            if (shownFractions[i] > 0 && turn >= start && turn < end) {
                return i;
            }
            start = end;
        }
        return -1;
    }

    /**
     * Tells whether a point is inside the hole of the ring.
     *
     * @param p the point, in component coordinates
     * @return {@code true} if the point is in the centre
     */
    private boolean isInCentre(Point p) {
        return Math.hypot(p.x - centreX(), p.y - centreY()) < innerRadius(outerRadius());
    }

    /**
     * Finds the legend row under a point. Rows of statuses with no items do
     * not count.
     *
     * @param p the point, in component coordinates
     * @return the row's index in {@link StatusBreakdown#ORDER}, or -1
     */
    private int legendRowAt(Point p) {
        for (int i = 0; i < 3; i++) {
            if (legendRow(i).contains(p) && breakdown.count(StatusBreakdown.ORDER.get(i)) > 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Updates the hovered slice, centre, cursor and tooltip for a new mouse
     * position.
     *
     * @param p the mouse position, or {@code null} when the mouse has left
     */
    private void updateHover(Point p) {
        mouse = p;
        int newHovered = -1;
        boolean newCentre = false;
        if (p != null) {
            newHovered = sliceAt(p);
            if (newHovered < 0) {
                newHovered = legendRowAt(p);
            }
            newCentre = newHovered < 0 && !breakdown.isEmpty() && isInCentre(p);
        }
        boolean clickable = newHovered >= 0 || newCentre;
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        if (newHovered != hovered || newCentre != centreHovered) {
            hovered = newHovered;
            centreHovered = newCentre;
            startTimer();
        } else {
            repaint();
        }
    }

    /**
     * Handles a click: a slice or legend row selects its status, the centre
     * resets to all statuses.
     *
     * @param p where the click happened
     */
    private void handleClick(Point p) {
        int index = sliceAt(p);
        if (index < 0) {
            index = legendRowAt(p);
        }
        if (index >= 0) {
            if (onStatusSelected != null) {
                onStatusSelected.accept(StatusBreakdown.ORDER.get(index));
            }
        } else if (isInCentre(p) && onCentreSelected != null) {
            onCentreSelected.run();
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Paints the ring, the centre, the legend and, when hovering a slice,
     * the tooltip.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.applyRenderingHints(g.create());
        try {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            paintRing(g2);
            paintCentre(g2);
            paintLegend(g2);
            paintTooltip(g2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the dashed track for any part of the ring not covered by a
     * slice, then each visible slice with its pop, highlight and
     * paper-coloured separator.
     *
     * @param g2 the graphics context to paint on
     */
    private void paintRing(Graphics2D g2) {
        double cx = centreX();
        double cy = centreY();
        double outer = outerRadius();
        double inner = innerRadius(outer);

        double covered = 0;
        for (double fraction : shownFractions) {
            covered += fraction;
        }
        double visibleEnd = Math.min(covered, sweep);
        if (visibleEnd < 1 - 1e-6) {
            double mid = (outer + inner) / 2;
            g2.setColor(Theme.RULE);
            g2.setStroke(new BasicStroke((float) (outer - inner), BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10f, new float[] {2.5f, 5f}, 0f));
            double startDeg = 90 - visibleEnd * 360;
            g2.draw(new Arc2D.Double(cx - mid, cy - mid, mid * 2, mid * 2,
                    startDeg, -(1 - visibleEnd) * 360, Arc2D.OPEN));
        }

        for (int i = 0; i < 3; i++) {
            double start = sliceStart(i);
            double end = Math.min(start + shownFractions[i], sweep);
            if (end - start <= 1e-6) {
                continue;
            }
            Shape slice = sliceShape(cx, cy, outer, inner, start, end);
            double midTurn = (start + end) / 2;
            double radians = Math.toRadians(90 - midTurn * 360);
            double offset = pops[i] * POP_DISTANCE;
            Shape placed = AffineTransform.getTranslateInstance(
                    Math.cos(radians) * offset, -Math.sin(radians) * offset).createTransformedShape(slice);

            Color base = Theme.chartColor(StatusBreakdown.ORDER.get(i));
            g2.setColor(mix(base, Color.WHITE, (float) (pops[i] * HIGHLIGHT_MIX)));
            g2.fill(placed);
            boolean fullCircle = end - start >= 1 - 1e-6;
            if (!fullCircle) {
                g2.setColor(Theme.PAPER);
                g2.setStroke(new BasicStroke(SEPARATOR_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g2.draw(placed);
            }
        }
    }

    /**
     * Builds the shape of one slice of the ring.
     *
     * @param cx    centre x
     * @param cy    centre y
     * @param outer outer radius
     * @param inner inner radius
     * @param start start of the slice, as a fraction of a turn clockwise from 12 o'clock
     * @param end   end of the slice, as a fraction of a turn clockwise from 12 o'clock
     * @return the slice outline
     */
    private static Shape sliceShape(double cx, double cy, double outer, double inner, double start, double end) {
        double startDeg = 90 - start * 360;
        double extent = -(end - start) * 360;
        Area area = new Area(new Arc2D.Double(cx - outer, cy - outer, outer * 2, outer * 2,
                startDeg, extent, Arc2D.PIE));
        area.subtract(new Area(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2)));
        return area;
    }

    /**
     * Paints the centre: the total item count with an "ITEMS" caption, or
     * "NO ENTRIES" for an empty inventory. While the centre is hovered, a
     * small "SHOW ALL" hint replaces the caption.
     *
     * @param g2 the graphics context to paint on
     */
    private void paintCentre(Graphics2D g2) {
        int cx = (int) Math.round(centreX());
        int cy = (int) Math.round(centreY());
        boolean empty = breakdown.isEmpty() && Math.round(shownTotal * sweep) == 0;

        if (empty) {
            g2.setFont(Theme.tracked(Theme.monoSemiBold(12f), Theme.TRACKING_LABEL));
            g2.setColor(Theme.MUTED);
            drawCentred(g2, "NO ENTRIES", cx, cy + g2.getFontMetrics().getAscent() / 2 - 1);
            return;
        }

        long number = Math.round(shownTotal * sweep);
        g2.setFont(numeralFont);
        FontMetrics numeral = g2.getFontMetrics();
        g2.setFont(captionFont);
        FontMetrics caption = g2.getFontMetrics();
        int blockHeight = numeral.getAscent() - numeral.getDescent() + 8 + caption.getAscent();
        int top = cy - blockHeight / 2;

        g2.setFont(numeralFont);
        g2.setColor(Theme.INK);
        drawCentred(g2, String.valueOf(number), cx, top + numeral.getAscent() - numeral.getDescent());

        g2.setFont(captionFont);
        g2.setColor(centreHovered ? Theme.ACCENT : Theme.MUTED);
        drawCentred(g2, centreHovered ? "SHOW ALL" : "ITEMS", cx, top + blockHeight);
    }

    /**
     * Paints the legend: a header row, one ruled row per status (colour
     * square, label, count and whole percentage), and a total row under a
     * heavy rule. The hovered row is shaded with a coloured edge.
     *
     * @param g2 the graphics context to paint on
     */
    private void paintLegend(Graphics2D g2) {
        int x = legendX();
        int right = x + LEGEND_WIDTH;
        int countRight = right - 64;
        int top = legendRowsTop();

        g2.setFont(captionFont);
        FontMetrics caption = g2.getFontMetrics();
        int headerBaseline = top - 12;
        g2.setColor(Theme.MUTED);
        g2.drawString("STATUS", x, headerBaseline);
        drawRight(g2, "ITEMS", countRight, headerBaseline);
        drawRight(g2, "SHARE", right, headerBaseline);
        g2.setColor(Theme.INK);
        g2.fillRect(x, top - 2, LEGEND_WIDTH, 2);

        List<StockStatus> order = StatusBreakdown.ORDER;
        for (int i = 0; i < 3; i++) {
            StockStatus status = order.get(i);
            Rectangle row = legendRow(i);
            boolean active = i == hovered;
            if (active) {
                g2.setColor(Theme.SURFACE);
                g2.fillRect(row.x, row.y, row.width, row.height - 1);
                g2.setColor(Theme.chartColor(status));
                g2.fillRect(row.x, row.y, 3, row.height - 1);
            }
            int textBaseline = row.y + (row.height + caption.getAscent()) / 2 - 1;
            int swatchX = row.x + 10;
            g2.setColor(Theme.chartColor(status));
            g2.fillRect(swatchX, row.y + (row.height - SWATCH) / 2, SWATCH, SWATCH);

            g2.setFont(legendFont);
            g2.setColor(breakdown.count(status) == 0 ? Theme.MUTED : Theme.INK);
            g2.drawString(status.getDisplayLabel(), swatchX + SWATCH + 10, textBaseline);

            g2.setFont(legendNumberFont);
            drawRight(g2, String.valueOf(breakdown.count(status)), countRight, textBaseline);
            g2.setColor(Theme.MUTED);
            drawRight(g2, breakdown.wholePercent(status) + "%", right - 4, textBaseline);

            g2.setColor(Theme.RULE);
            g2.fillRect(row.x, row.y + row.height - 1, row.width, 1);
        }

        int totalTop = top + LEGEND_ROW * 3;
        g2.setColor(Theme.INK);
        g2.fillRect(x, totalTop - 1, LEGEND_WIDTH, 2);
        int totalBaseline = totalTop + (LEGEND_ROW + caption.getAscent()) / 2;
        g2.setFont(legendFont);
        g2.drawString("TOTAL", x + 10, totalBaseline);
        g2.setFont(legendNumberFont);
        drawRight(g2, String.valueOf(breakdown.total()), countRight, totalBaseline);
        drawRight(g2, breakdown.isEmpty() ? "—" : "100%", right - 4, totalBaseline);
    }

    /**
     * Paints the receipt-style tooltip next to the mouse while a slice or
     * legend row is hovered, for example "LOW STOCK · 3 ITEMS · 21%". It is
     * kept inside the component.
     *
     * @param g2 the graphics context to paint on
     */
    private void paintTooltip(Graphics2D g2) {
        if (hovered < 0 || mouse == null) {
            return;
        }
        StockStatus status = StatusBreakdown.ORDER.get(hovered);
        int count = breakdown.count(status);
        String text = status.getDisplayLabel() + " · " + count + (count == 1 ? " ITEM" : " ITEMS")
                + " · " + breakdown.wholePercent(status) + "%";

        g2.setFont(tipFont);
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(text) + TIP_PAD_X * 2 + 4;
        width = ((width + TOOTH_WIDTH - 1) / TOOTH_WIDTH) * TOOTH_WIDTH;
        int bodyHeight = TIP_PAD_Y * 2 + fm.getAscent();
        int x = Math.min(mouse.x + TIP_OFFSET, getWidth() - width - 4);
        int y = mouse.y + TIP_OFFSET;
        if (y + bodyHeight + TOOTH_HEIGHT > getHeight() - 4) {
            y = mouse.y - TIP_OFFSET - bodyHeight - TOOTH_HEIGHT;
        }
        x = Math.max(4, x);

        Path2D slip = new Path2D.Float();
        float left = x + 0.5f;
        float rightEdge = x + width - 0.5f;
        float bottom = y + bodyHeight;
        slip.moveTo(left, y + 0.5f);
        slip.lineTo(rightEdge, y + 0.5f);
        slip.lineTo(rightEdge, bottom);
        for (float tx = rightEdge; tx > left; tx -= TOOTH_WIDTH) {
            slip.lineTo(Math.max(left, tx - TOOTH_WIDTH / 2f), bottom + TOOTH_HEIGHT);
            slip.lineTo(Math.max(left, tx - TOOTH_WIDTH), bottom);
        }
        slip.closePath();

        g2.setColor(Theme.SURFACE);
        g2.fill(slip);
        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(slip);
        g2.setColor(Theme.chartColor(status));
        g2.fillRect(x + 1, y + 1, 4, bodyHeight - 1);

        g2.setColor(Theme.INK);
        g2.drawString(text, x + TIP_PAD_X + 2, y + TIP_PAD_Y + fm.getAscent() - 1);
    }

    /**
     * Draws text centred horizontally on a point.
     *
     * @param g2       the graphics context, with the font already set
     * @param text     the text
     * @param cx       horizontal centre
     * @param baseline text baseline
     */
    private static void drawCentred(Graphics2D g2, String text, int cx, int baseline) {
        g2.drawString(text, cx - g2.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    /**
     * Draws text right-aligned to a position.
     *
     * @param g2       the graphics context, with the font already set
     * @param text     the text
     * @param right    x position of the text's right edge
     * @param baseline text baseline
     */
    private static void drawRight(Graphics2D g2, String text, int right, int baseline) {
        g2.drawString(text, right - g2.getFontMetrics().stringWidth(text), baseline);
    }

    /**
     * Blends two colours.
     *
     * @param base   the starting colour
     * @param target the colour to blend towards
     * @param amount how far to blend, from 0 (base) to 1 (target)
     * @return the blended colour
     */
    private static Color mix(Color base, Color target, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        return new Color(
                Math.round(base.getRed() + (target.getRed() - base.getRed()) * a),
                Math.round(base.getGreen() + (target.getGreen() - base.getGreen()) * a),
                Math.round(base.getBlue() + (target.getBlue() - base.getBlue()) * a));
    }

    /**
     * Mouse listener that drives hovering and clicking on the chart.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private final class ChartMouseHandler extends MouseAdapter {

        /**
         * Creates the mouse handler.
         */
        private ChartMouseHandler() {
        }

        /**
         * Updates the hover state as the mouse moves.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            updateHover(e.getPoint());
        }

        /**
         * Updates the hover state while a button is held down.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            updateHover(e.getPoint());
        }

        /**
         * Clears the hover state when the mouse leaves the chart.
         *
         * @param e the mouse event (not used)
         */
        @Override
        public void mouseExited(MouseEvent e) {
            updateHover(null);
        }

        /**
         * Handles a click on a slice, a legend row or the centre.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            handleClick(e.getPoint());
        }
    }
}
