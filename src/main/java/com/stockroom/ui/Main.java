package com.stockroom.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Application entry point for Stockroom.
 *
 * <p>For now this opens an empty placeholder window. The full ledger
 * interface is built in later steps.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public final class Main {

    /** Title shown in the main window's title bar. */
    private static final String WINDOW_TITLE = "Stockroom";

    /** Default window width in pixels. */
    private static final int DEFAULT_WIDTH = 1280;

    /** Default window height in pixels. */
    private static final int DEFAULT_HEIGHT = 800;

    /**
     * Private constructor; this class only holds the static entry point
     * and is never instantiated.
     */
    private Main() {
    }

    /**
     * Starts the application by scheduling the window creation on the
     * Swing event dispatch thread.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    /**
     * Creates the placeholder main window, centres it on screen and makes
     * it visible. Must be called on the event dispatch thread.
     */
    private static void createAndShowWindow() {
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
