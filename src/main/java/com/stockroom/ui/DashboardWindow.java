package com.stockroom.ui;

import com.stockroom.model.StockStatus;
import com.stockroom.service.InventoryListener;
import com.stockroom.service.InventoryManager;
import com.stockroom.service.StatusBreakdown;
import com.stockroom.ui.components.LedgerButton;
import com.stockroom.ui.components.StatusPieChart;
import com.stockroom.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * Separate, non-modal window with an animated donut chart of the inventory
 * by stock status.
 *
 * <p>The window has the ledger look and its own chrome: it is undecorated,
 * so it has square corners and no system shadow or gradient, with a 1px ink
 * frame, a manifest-style header ("STOCK BY STATUS" and today's date) that
 * can be dragged to move the window, a CLOSE button, and a status bar with
 * hints. Esc also closes it.</p>
 *
 * <p>The window observes the {@link InventoryManager} through an
 * {@link InventoryListener}, so the chart animates to the new counts after
 * every add, update or delete. Clicking a slice or legend row asks the main
 * window to filter by that status; clicking the centre asks it to show all
 * statuses. The listener is removed when the window is disposed.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class DashboardWindow extends JFrame {

    /** Title shown in the taskbar and Alt+Tab. */
    private static final String WINDOW_TITLE = "Stockroom · Dashboard";

    /** Window width, in pixels. */
    private static final int WIDTH = 640;

    /** Window height, in pixels. */
    private static final int HEIGHT = 520;

    /** Horizontal page margin, in pixels. */
    private static final int PAGE_PAD = 26;

    /** Format of the date on the manifest line, for example "23 NOV 2026". */
    private static final DateTimeFormatter MANIFEST_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    /** Hints shown in the status bar. */
    private static final String HINTS = "Click a slice to filter the ledger · centre shows all · Esc closes";

    /** The inventory the chart shows. */
    private final InventoryManager manager;

    /** The animated status chart. */
    private final StatusPieChart chart;

    /** Listener registered with {@link #manager}; kept so it can be removed. */
    private final InventoryListener listener;

    /** Whether the opening sweep has been played. */
    private boolean introPlayed;

    /**
     * Builds the dashboard window. It is not shown until
     * {@link #showDashboard()} is called.
     *
     * @param manager       the inventory to chart
     * @param filterHandler called with a status when a slice or legend row is
     *                      clicked, or with {@code null} when the centre is
     *                      clicked to show all statuses
     * @param icons         window icons, shared with the main window
     * @throws NullPointerException if {@code manager} or {@code filterHandler} is {@code null}
     */
    public DashboardWindow(InventoryManager manager, Consumer<StockStatus> filterHandler, List<Image> icons) {
        super(WINDOW_TITLE);
        this.manager = Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(filterHandler, "filterHandler");

        chart = new StatusPieChart();
        chart.setBreakdown(StatusBreakdown.of(manager));
        chart.setOnStatusSelected(filterHandler);
        chart.setOnCentreSelected(() -> filterHandler.accept(null));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.PAPER);
        root.setBorder(BorderFactory.createLineBorder(Theme.INK, 1));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(chart, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        listener = this::onInventoryChanged;
        manager.addListener(listener);

        getRootPane().registerKeyboardAction(this::onClose,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setUndecorated(true);
        setIconImages(icons);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setResizable(false);
    }

    /**
     * Shows the window, or brings it to the front if it is already open
     * (restoring it if minimised). The first time, the chart sweeps in.
     */
    public void showDashboard() {
        if ((getExtendedState() & Frame.ICONIFIED) != 0) {
            setExtendedState(getExtendedState() & ~Frame.ICONIFIED);
        }
        setVisible(true);
        toFront();
        requestFocus();
        if (!introPlayed) {
            introPlayed = true;
            chart.playIntro();
        }
    }

    /**
     * Returns the chart shown in this window.
     *
     * @return the status chart
     */
    StatusPieChart getChart() {
        return chart;
    }

    /**
     * Stops listening to the inventory and stops the chart's animation, then
     * releases the window.
     */
    @Override
    public void dispose() {
        manager.removeListener(listener);
        chart.stopAnimation();
        super.dispose();
    }

    /**
     * Builds the manifest-style header: "STOCK BY STATUS" above the date line
     * on the left, CLOSE on the right, a 2px ink rule underneath. Dragging the
     * header moves the window.
     *
     * @return the header panel
     */
    private JPanel buildHeader() {
        JLabel title = new JLabel("STOCK BY STATUS");
        title.setFont(Theme.tracked(Theme.display(26f), Theme.TRACKING_TIGHT));
        title.setForeground(Theme.INK);

        String date = LocalDate.now().format(MANIFEST_DATE).toUpperCase(Locale.ROOT);
        JLabel manifestLine = new JLabel("DASHBOARD · " + date);
        manifestLine.setFont(Theme.tracked(Theme.mono(11f), Theme.TRACKING_LABEL));
        manifestLine.setForeground(Theme.MUTED);

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.add(title);
        brand.add(Box.createVerticalStrut(2));
        brand.add(manifestLine);

        LedgerButton closeButton = new LedgerButton("Close", LedgerButton.Variant.SECONDARY);
        closeButton.addActionListener(this::onClose);
        JPanel closeWrap = new JPanel(new GridBagLayout());
        closeWrap.setOpaque(false);
        closeWrap.add(closeButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.INK),
                new EmptyBorder(18, PAGE_PAD, 14, PAGE_PAD)));
        header.add(brand, BorderLayout.WEST);
        header.add(closeWrap, BorderLayout.EAST);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        HeaderDragger dragger = new HeaderDragger();
        header.addMouseListener(dragger);
        header.addMouseMotionListener(dragger);
        return header;
    }

    /**
     * Builds the status bar: hints in small muted mono text under a thin rule.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        JLabel hints = new JLabel(HINTS);
        hints.setFont(Theme.mono(11f));
        hints.setForeground(Theme.MUTED);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.RULE),
                new EmptyBorder(8, PAGE_PAD, 9, PAGE_PAD)));
        bar.add(hints, BorderLayout.WEST);
        return bar;
    }

    /**
     * Sends the new counts to the chart after the inventory changes;
     * registered with the inventory manager.
     */
    private void onInventoryChanged() {
        chart.setBreakdown(StatusBreakdown.of(manager));
    }

    /**
     * Handles CLOSE and Esc: closes and releases the window.
     *
     * @param e the button or key event (not used)
     */
    private void onClose(ActionEvent e) {
        dispose();
    }

    /**
     * Moves the undecorated window when its header is dragged.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private final class HeaderDragger extends MouseAdapter {

        /** Mouse position inside the window when the drag started, or {@code null}. */
        private Point grabOffset;

        /**
         * Creates the drag handler.
         */
        private HeaderDragger() {
        }

        /**
         * Remembers where in the window the header was grabbed.
         *
         * @param e the mouse event
         */
        @Override
        public void mousePressed(MouseEvent e) {
            Point onScreen = e.getLocationOnScreen();
            Point window = getLocationOnScreen();
            grabOffset = new Point(onScreen.x - window.x, onScreen.y - window.y);
        }

        /**
         * Moves the window so the grabbed point follows the mouse.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            if (grabOffset != null) {
                Point onScreen = e.getLocationOnScreen();
                setLocation(onScreen.x - grabOffset.x, onScreen.y - grabOffset.y);
            }
        }

        /**
         * Ends the drag.
         *
         * @param e the mouse event (not used)
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            grabOffset = null;
        }
    }
}
