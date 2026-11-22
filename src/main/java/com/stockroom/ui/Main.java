package com.stockroom.ui;

import com.stockroom.service.InventoryManager;
import com.stockroom.ui.theme.Theme;

import javax.swing.SwingUtilities;

/**
 * Application entry point for Stockroom.
 *
 * <p>Installs the Stockroom Ledger theme, then builds and shows the main
 * window on the Swing event dispatch thread.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public final class Main {

    /**
     * Private constructor; this class only holds the static entry point
     * and is never instantiated.
     */
    private Main() {
    }

    /**
     * Starts the application. The theme is installed first, before any
     * Swing component exists, so text antialiasing and the ledger defaults
     * apply everywhere; the window is then created on the event dispatch
     * thread.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    /**
     * Creates the inventory and the main window, shows the window and puts
     * the cursor in the Item name field. Must be called on the event
     * dispatch thread.
     */
    private static void createAndShowWindow() {
        InventoryGUI gui = new InventoryGUI(new InventoryManager());
        gui.setVisible(true);
        gui.focusNewEntry();
    }
}
