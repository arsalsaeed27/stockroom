package com.stockroom.ui.theme;

import com.stockroom.model.StockStatus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * The "Stockroom Ledger" design system: every colour, font and shared
 * painting helper used by the user interface.
 *
 * <p>The look is a physical stockroom ledger: warm paper, black ink, a single
 * signal-orange accent and rubber-stamp status colours. No component
 * hard-codes a colour or font; they all read the constants and factory
 * methods here.</p>
 *
 * <p>Fonts are the bundled OFL fonts Space Grotesk (headings) and IBM Plex
 * Mono (data, labels and numbers), loaded from {@code /fonts} on the
 * classpath and registered with the {@link GraphicsEnvironment}. If a font
 * file is missing or unreadable the matching logical font ({@code SansSerif}
 * or {@code Monospaced}) is used instead, so the application still runs.</p>
 *
 * <p>Call {@link #install()} once at start-up, before any Swing component is
 * created, so that text antialiasing and the Swing defaults take effect.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public final class Theme {

    /** Page background: warm ledger paper, {@code #F3EEE3}. */
    public static final Color PAPER = new Color(0xF3EEE3);

    /** Raised surface such as the entry slip and selected rows, {@code #FBF8F1}. */
    public static final Color SURFACE = new Color(0xFBF8F1);

    /** Primary text and solid button colour: black ink, {@code #1B1A17}. */
    public static final Color INK = new Color(0x1B1A17);

    /** Ruled lines and borders, {@code #D9D0BE}. */
    public static final Color RULE = new Color(0xD9D0BE);

    /** Secondary, muted text such as labels and hints, {@code #7A7263}. */
    public static final Color MUTED = new Color(0x7A7263);

    /** The one and only accent colour: signal orange, {@code #E8590C}. */
    public static final Color ACCENT = new Color(0xE8590C);

    /** The accent at low opacity, used for text selection and row flashes. */
    public static final Color ACCENT_TINT = new Color(0xE8, 0x59, 0x0C, 56);

    /** Status colour for {@link StockStatus#IN_STOCK}, {@code #2F6B3A}. */
    public static final Color STATUS_IN = new Color(0x2F6B3A);

    /** Status colour for {@link StockStatus#LOW_STOCK}, {@code #B7791F}. */
    public static final Color STATUS_LOW = new Color(0xB7791F);

    /** Status colour for {@link StockStatus#OUT_OF_STOCK}, also used for errors, {@code #A8322D}. */
    public static final Color STATUS_OUT = new Color(0xA8322D);

    /** Letter-spacing for small-caps mono labels, as a fraction of the font size. */
    public static final float TRACKING_LABEL = 0.12f;

    /** Tight letter-spacing for large display headings such as the wordmark. */
    public static final float TRACKING_TIGHT = -0.03f;

    /** Classpath folder that holds the bundled font files. */
    private static final String FONT_DIR = "/fonts/";

    /** Dash length, in pixels, of the perforated edge on slips. */
    private static final float PERFORATION_DASH = 6f;

    /** Gap length, in pixels, between dashes of the perforated edge. */
    private static final float PERFORATION_GAP = 4f;

    /** Line width, in pixels, of the perforated edge. */
    private static final float PERFORATION_WIDTH = 1.5f;

    /**
     * Private constructor; this class only holds constants and static
     * helpers and is never instantiated.
     */
    private Theme() {
    }

    /**
     * Prepares Swing for the Stockroom Ledger look. Turns on text
     * antialiasing, loads and registers the bundled fonts and installs
     * ledger colours and fonts as Swing defaults.
     *
     * <p>Call this once on start-up, before any component is created.</p>
     */
    public static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("swing.boldMetal", "false");
        FontHolder.ensureLoaded();
        installUiDefaults();
    }

    /**
     * Writes ledger colours and fonts into the Swing defaults table, so that
     * standard components (labels, text fields, tables, tooltips) match the
     * custom-painted ones instead of showing the default Metal look.
     */
    private static void installUiDefaults() {
        ColorUIResource paper = new ColorUIResource(PAPER);
        ColorUIResource surface = new ColorUIResource(SURFACE);
        ColorUIResource ink = new ColorUIResource(INK);
        ColorUIResource muted = new ColorUIResource(MUTED);
        ColorUIResource rule = new ColorUIResource(RULE);
        FontUIResource body = new FontUIResource(mono(13f));
        FontUIResource small = new FontUIResource(mono(11f));

        UIManager.put("swing.boldMetal", Boolean.FALSE);
        UIManager.put("Panel.background", paper);
        UIManager.put("Viewport.background", paper);
        UIManager.put("ScrollPane.background", paper);
        UIManager.put("Label.font", body);
        UIManager.put("Label.foreground", ink);
        UIManager.put("TextField.font", body);
        UIManager.put("TextField.foreground", ink);
        UIManager.put("TextField.caretForeground", ink);
        UIManager.put("TextField.selectionBackground", new ColorUIResource(ACCENT_TINT));
        UIManager.put("TextField.selectionForeground", ink);
        UIManager.put("Table.font", body);
        UIManager.put("Table.background", paper);
        UIManager.put("Table.foreground", ink);
        UIManager.put("Table.gridColor", rule);
        UIManager.put("Table.selectionBackground", surface);
        UIManager.put("Table.selectionForeground", ink);
        UIManager.put("TableHeader.font", small);
        UIManager.put("TableHeader.background", paper);
        UIManager.put("TableHeader.foreground", muted);
        UIManager.put("ToolTip.font", small);
        UIManager.put("ToolTip.background", surface);
        UIManager.put("ToolTip.foreground", ink);
        UIManager.put("ToolTip.border", new BorderUIResource(BorderFactory.createLineBorder(INK)));
    }

    /**
     * Returns the Space Grotesk display font, bold, for headings and the
     * wordmark.
     *
     * @param size point size of the font
     * @return the display font at the given size
     */
    public static Font display(float size) {
        return FontHolder.DISPLAY.deriveFont(Font.BOLD, size);
    }

    /**
     * Returns IBM Plex Mono Regular, for data and body text.
     *
     * @param size point size of the font
     * @return the regular mono font at the given size
     */
    public static Font mono(float size) {
        return FontHolder.MONO_REGULAR.deriveFont(size);
    }

    /**
     * Returns IBM Plex Mono Medium, for numerals that need a little weight.
     *
     * @param size point size of the font
     * @return the medium mono font at the given size
     */
    public static Font monoMedium(float size) {
        return FontHolder.MONO_MEDIUM.deriveFont(size);
    }

    /**
     * Returns IBM Plex Mono SemiBold, for button labels and small caps.
     *
     * @param size point size of the font
     * @return the semi-bold mono font at the given size
     */
    public static Font monoSemiBold(float size) {
        return FontHolder.MONO_SEMIBOLD.deriveFont(size);
    }

    /**
     * Returns IBM Plex Mono Bold, for stamps and strong numerals.
     *
     * @param size point size of the font
     * @return the bold mono font at the given size
     */
    public static Font monoBold(float size) {
        return FontHolder.MONO_BOLD.deriveFont(size);
    }

    /**
     * Returns a copy of a font with letter-spacing applied.
     *
     * @param font     the font to space out
     * @param tracking letter-spacing as a fraction of the font size; positive
     *                 values spread letters apart, negative values tighten them
     * @return a new font with the given tracking
     */
    public static Font tracked(Font font, float tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return font.deriveFont(attributes);
    }

    /**
     * Returns the colour that belongs to a stock status.
     *
     * @param status the stock status
     * @return the matching status colour
     */
    public static Color statusColor(StockStatus status) {
        return switch (status) {
            case IN_STOCK -> STATUS_IN;
            case LOW_STOCK -> STATUS_LOW;
            case OUT_OF_STOCK -> STATUS_OUT;
        };
    }

    /**
     * Turns on antialiasing, text antialiasing and fractional font metrics
     * on a graphics context. Every custom-painted component calls this at
     * the start of painting.
     *
     * @param graphics the graphics context to configure, which must be a
     *                 {@link Graphics2D}
     * @return the same context, cast to {@link Graphics2D}
     */
    public static Graphics2D applyRenderingHints(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * Creates a square-ended dashed stroke.
     *
     * @param width line width in pixels
     * @param dash  length of each dash in pixels
     * @param gap   length of each gap in pixels
     * @return the dashed stroke
     */
    public static Stroke dashed(float width, float dash, float gap) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[] {dash, gap}, 0f);
    }

    /**
     * Paints a horizontal perforated (dashed) edge, like the tear-off line
     * on a paper slip.
     *
     * @param g2    the graphics context to paint on
     * @param x     left end of the line
     * @param y     vertical position of the line
     * @param width length of the line in pixels
     */
    public static void paintPerforation(Graphics2D g2, int x, int y, int width) {
        Stroke oldStroke = g2.getStroke();
        g2.setColor(MUTED);
        g2.setStroke(dashed(PERFORATION_WIDTH, PERFORATION_DASH, PERFORATION_GAP));
        g2.draw(new Line2D.Float(x, y, (float) x + width, y));
        g2.setStroke(oldStroke);
    }

    /**
     * Loads one bundled font file and registers it with the graphics
     * environment.
     *
     * @param fileName       name of the font file inside {@code /fonts}
     * @param fallbackFamily logical font family to use if loading fails
     * @param fallbackStyle  style of the fallback font, such as {@link Font#BOLD}
     * @return the loaded font at size 1, or the fallback font if the file
     *         is missing or unreadable
     */
    private static Font loadFont(String fileName, String fallbackFamily, int fallbackStyle) {
        try (InputStream in = Theme.class.getResourceAsStream(FONT_DIR + fileName)) {
            if (in == null) {
                return new Font(fallbackFamily, fallbackStyle, 1);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (FontFormatException | IOException e) {
            return new Font(fallbackFamily, fallbackStyle, 1);
        }
    }

    /**
     * Holds the loaded base fonts. Kept in a nested class so the font files
     * are read only on first use (lazy holder idiom), after
     * {@link Theme#install()} has set the antialiasing properties.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private static final class FontHolder {

        /** Space Grotesk, the heading font. */
        private static final Font DISPLAY =
                loadFont("SpaceGrotesk-Variable.ttf", Font.SANS_SERIF, Font.PLAIN);

        /** IBM Plex Mono Regular. */
        private static final Font MONO_REGULAR =
                loadFont("IBMPlexMono-Regular.ttf", Font.MONOSPACED, Font.PLAIN);

        /** IBM Plex Mono Medium. */
        private static final Font MONO_MEDIUM =
                loadFont("IBMPlexMono-Medium.ttf", Font.MONOSPACED, Font.PLAIN);

        /** IBM Plex Mono SemiBold. */
        private static final Font MONO_SEMIBOLD =
                loadFont("IBMPlexMono-SemiBold.ttf", Font.MONOSPACED, Font.BOLD);

        /** IBM Plex Mono Bold. */
        private static final Font MONO_BOLD =
                loadFont("IBMPlexMono-Bold.ttf", Font.MONOSPACED, Font.BOLD);

        /**
         * Private constructor; this holder is never instantiated.
         */
        private FontHolder() {
        }

        /**
         * Does nothing by itself; calling it forces this class to load,
         * which reads and registers every bundled font.
         */
        private static void ensureLoaded() {
            // Class initialisation has already loaded the fonts by the time this runs.
        }
    }
}
