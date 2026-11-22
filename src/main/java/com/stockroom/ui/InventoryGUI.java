package com.stockroom.ui;

import com.stockroom.exception.DuplicateItemException;
import com.stockroom.exception.InvalidItemException;
import com.stockroom.exception.ItemNotFoundException;
import com.stockroom.model.Item;
import com.stockroom.model.StockStatus;
import com.stockroom.service.InputValidator;
import com.stockroom.service.InventoryManager;
import com.stockroom.ui.components.DashedRule;
import com.stockroom.ui.components.EmptyStatePanel;
import com.stockroom.ui.components.LedgerButton;
import com.stockroom.ui.components.LedgerCellRenderer;
import com.stockroom.ui.components.LedgerHeaderRenderer;
import com.stockroom.ui.components.LedgerScrollBarUI;
import com.stockroom.ui.components.LedgerTable;
import com.stockroom.ui.components.LevelBarRenderer;
import com.stockroom.ui.components.SegmentedToggle;
import com.stockroom.ui.components.SlipDialog;
import com.stockroom.ui.components.SlipField;
import com.stockroom.ui.components.SlipPanel;
import com.stockroom.ui.components.StampBadge;
import com.stockroom.ui.components.Toast;
import com.stockroom.ui.components.UnderlineField;
import com.stockroom.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

/**
 * Main window of Stockroom, laid out as a stockroom ledger.
 *
 * <p>The window has four regions:</p>
 * <ul>
 *   <li>a manifest header with the "STOCKROOM" wordmark, the manifest line
 *       with today's date, and live TOTAL / LOW / OUT counters;</li>
 *   <li>on the left (about 68%), a toolbar with the search field, the status
 *       filter and VIEW ALL, above the ledger table. A {@link CardLayout}
 *       swaps the table for an {@link EmptyStatePanel} when there is nothing
 *       to list;</li>
 *   <li>on the right (about 32%), the Entry Slip with the item form, its
 *       buttons and the Update-by-ID mini-form;</li>
 *   <li>a status bar with keyboard hints and the save status.</li>
 * </ul>
 *
 * <p>The window observes the {@link InventoryManager}, so the table,
 * counters and empty state refresh by themselves after every change.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class InventoryGUI extends JFrame {

    /** Title shown in the window's title bar. */
    private static final String WINDOW_TITLE = "Stockroom — Inventory Manifest";

    /** Smallest allowed window width, in pixels. */
    private static final int MIN_WIDTH = 1100;

    /** Smallest allowed window height, in pixels. */
    private static final int MIN_HEIGHT = 700;

    /** Default window width, in pixels. */
    private static final int DEFAULT_WIDTH = 1280;

    /** Default window height, in pixels. */
    private static final int DEFAULT_HEIGHT = 800;

    /** Horizontal page margin, in pixels. */
    private static final int PAGE_PAD = 32;

    /** Gap, in pixels, between the ledger and the Entry Slip. */
    private static final int COLUMN_GAP = 28;

    /** Inner padding, in pixels, of the Entry Slip. */
    private static final int SLIP_PAD = 26;

    /** Height, in pixels, of one ledger row. */
    private static final int ROW_HEIGHT = 40;

    /** Share of the extra width given to the ledger column. */
    private static final double LEDGER_WEIGHT = 0.68;

    /** Share of the extra width given to the Entry Slip column. */
    private static final double SLIP_WEIGHT = 0.32;

    /** Width, in pixels, of the search field above the ledger. */
    private static final int SEARCH_WIDTH = 260;

    /** Pixel sizes of the window icon, for the title bar, taskbar and Alt+Tab. */
    private static final int[] ICON_SIZES = {16, 20, 24, 32, 40, 48, 64};

    /** Card name of the ledger table. */
    private static final String CARD_TABLE = "table";

    /** Card name of the empty-state panel. */
    private static final String CARD_EMPTY = "empty";

    /** Options of the status filter, in display order. */
    private static final String[] FILTER_OPTIONS = {"ALL", "IN", "LOW", "OUT"};

    /** Format of the date on the manifest line, for example "22 NOV 2026". */
    private static final DateTimeFormatter MANIFEST_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    /** Fixed part of the manifest line under the wordmark. */
    private static final String MANIFEST_PREFIX = "INVENTORY MANIFEST · NO. 0001 · ";

    /** Keyboard hints shown on the left of the status bar. */
    private static final String KEY_HINTS = "Ctrl+N new · Ctrl+F find · Del void · Esc clear";

    /** Path of the data file, shown on the right of the status bar. */
    private static final String DATA_FILE = "data/inventory.csv";

    /** Format of the time in the save status, for example "23:41". */
    private static final DateTimeFormatter SAVE_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Helper text under the threshold field, explaining how status is derived. */
    private static final String THRESHOLD_HELPER = "Status → LOW when qty ≤ threshold, OUT at 0";

    /** Title of the empty state when a search or filter matches nothing. */
    private static final String NO_MATCH_TITLE = "NO MATCHES";

    /** Message of the empty state when a search or filter matches nothing. */
    private static final String NO_MATCH_MESSAGE = "No entries match this search or filter. Press VIEW ALL to reset.";

    /** Value of {@link #editingId} while the slip holds a new entry; real IDs start at 1. */
    private static final int NO_ID = 0;

    /** The inventory shown and edited by this window. */
    private final InventoryManager manager;

    /** Model behind the ledger table. */
    private final ItemTableModel tableModel;

    /** Ledger table, which can flash newly added rows. */
    private final LedgerTable table;

    /** Row sorter of the ledger table, also used later for search and filter. */
    private final TableRowSorter<ItemTableModel> sorter;

    /** Layout that swaps between the table and the empty state. */
    private final CardLayout cards;

    /** Panel holding the table card and the empty-state card. */
    private final JPanel cardPanel;

    /** Panel shown instead of the table when there is nothing to list. */
    private final EmptyStatePanel emptyState;

    /** "FIND IN LEDGER&hellip;" search field above the table. */
    private final UnderlineField searchField;

    /** ALL / IN / LOW / OUT status filter. */
    private final SegmentedToggle statusFilter;

    /** Button that clears search and filters and shows every item. */
    private final LedgerButton viewAllButton;

    /** Numeral of the TOTAL counter. */
    private final JLabel totalCount;

    /** Numeral of the LOW counter. */
    private final JLabel lowCount;

    /** Numeral of the OUT counter. */
    private final JLabel outCount;

    /** Mode tag on the Entry Slip, "NEW ENTRY" or "EDITING #0007". */
    private final JLabel modeTag;

    /** Item name field on the Entry Slip. */
    private final SlipField nameField;

    /** Quantity field on the Entry Slip. */
    private final SlipField quantityField;

    /** Low-stock threshold field on the Entry Slip. */
    private final SlipField thresholdField;

    /** ID field of the Update-by-ID mini-form. */
    private final SlipField idField;

    /** Primary slip button: "ADD TO LEDGER" or "SAVE CHANGES". */
    private final LedgerButton primaryButton;

    /** Button that clears the slip. */
    private final LedgerButton clearButton;

    /** Button that deletes the item being edited. */
    private final LedgerButton voidButton;

    /** Button that loads the item whose ID is typed into {@link #idField}. */
    private final LedgerButton loadButton;

    /** Save status on the right of the status bar. */
    private final JLabel saveStatus;

    /** Receipt-style notifications for this window. */
    private final Toast toast;

    /** ID of the item open on the Entry Slip, or {@link #NO_ID} in new-entry mode. */
    private int editingId = NO_ID;

    /**
     * Builds the main window for an inventory. The window is not shown
     * until {@link #setVisible(boolean)} is called.
     *
     * @param manager the inventory to show and edit
     */
    public InventoryGUI(InventoryManager manager) {
        super(WINDOW_TITLE);
        this.manager = manager;

        tableModel = new ItemTableModel();
        table = new LedgerTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        cards = new CardLayout();
        cardPanel = new JPanel(cards);
        emptyState = new EmptyStatePanel();

        searchField = new UnderlineField(13f);
        searchField.setPlaceholder("FIND IN LEDGER…");
        statusFilter = new SegmentedToggle(FILTER_OPTIONS);
        viewAllButton = new LedgerButton("View all", LedgerButton.Variant.SECONDARY);

        totalCount = createCounterNumeral();
        lowCount = createCounterNumeral();
        outCount = createCounterNumeral();

        modeTag = new JLabel();
        nameField = new SlipField("Item name");
        quantityField = new SlipField("Quantity");
        thresholdField = new SlipField("Low-stock threshold", THRESHOLD_HELPER);
        idField = new SlipField("Item ID");
        primaryButton = new LedgerButton("Add to ledger", LedgerButton.Variant.PRIMARY);
        clearButton = new LedgerButton("Clear", LedgerButton.Variant.SECONDARY);
        voidButton = new LedgerButton("Void entry", LedgerButton.Variant.DESTRUCTIVE);
        loadButton = new LedgerButton("Load", LedgerButton.Variant.SECONDARY);
        saveStatus = new JLabel("NOT SAVED · " + DATA_FILE);

        toast = new Toast(getRootPane());

        configureTable();
        setNewEntryMode();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.PAPER);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        toast.setAnchor(cardPanel);
        setContentPane(root);

        wireActions();
        manager.addListener(this::onInventoryChanged);
        refresh();

        setIconImages(createWindowIcons());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Building the layout
    // ------------------------------------------------------------------

    /**
     * Builds the manifest header: wordmark and manifest line on the left,
     * live counters on the right, and a 2px ink rule underneath.
     *
     * @return the header panel
     */
    private JPanel buildHeader() {
        JLabel wordmark = new JLabel("STOCKROOM");
        wordmark.setFont(Theme.tracked(Theme.display(46f), Theme.TRACKING_TIGHT));
        wordmark.setForeground(Theme.INK);

        String date = LocalDate.now().format(MANIFEST_DATE).toUpperCase(Locale.ROOT);
        JLabel manifestLine = new JLabel(MANIFEST_PREFIX + date);
        manifestLine.setFont(Theme.tracked(Theme.mono(11f), Theme.TRACKING_LABEL));
        manifestLine.setForeground(Theme.MUTED);

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.add(wordmark);
        brand.add(Box.createVerticalStrut(2));
        brand.add(manifestLine);

        JPanel counters = new JPanel(new GridLayout(1, 3, 0, 0));
        counters.setOpaque(false);
        counters.add(buildCounter("TOTAL", totalCount, false));
        counters.add(buildCounter("LOW", lowCount, true));
        counters.add(buildCounter("OUT", outCount, true));

        JPanel countersWrap = new JPanel(new GridBagLayout());
        countersWrap.setOpaque(false);
        countersWrap.add(counters);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.INK),
                new EmptyBorder(26, PAGE_PAD, 18, PAGE_PAD)));
        header.add(brand, BorderLayout.WEST);
        header.add(countersWrap, BorderLayout.EAST);
        return header;
    }

    /**
     * Paints the window icon at several sizes, replacing Java's default
     * coffee cup: an ink square holding a paper "S" in Space Grotesk, with
     * an orange accent bar along the bottom, like a ledger stamp.
     *
     * @return the icon images, smallest first
     */
    private static List<Image> createWindowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = Theme.applyRenderingHints(image.createGraphics());
            try {
                g2.setColor(Theme.INK);
                g2.fillRect(0, 0, size, size);
                int bar = Math.max(2, size / 8);
                g2.setColor(Theme.ACCENT);
                g2.fillRect(0, size - bar, size, bar);

                g2.setFont(Theme.display(size * 0.78f));
                FontMetrics fm = g2.getFontMetrics();
                int capHeight = fm.getAscent() - fm.getDescent();
                int x = (size - fm.stringWidth("S")) / 2;
                int y = (size - bar + capHeight) / 2;
                g2.setColor(Theme.PAPER);
                g2.drawString("S", x, y);
            } finally {
                g2.dispose();
            }
            icons.add(image);
        }
        return icons;
    }

    /**
     * Creates the large mono numeral used by a header counter.
     *
     * @return the numeral label, showing "0"
     */
    private static JLabel createCounterNumeral() {
        JLabel numeral = new JLabel("0");
        numeral.setFont(Theme.monoMedium(38f));
        numeral.setForeground(Theme.INK);
        return numeral;
    }

    /**
     * Builds one header counter: a caption above a large numeral, with a
     * thin vertical rule on its left when it is not the first counter.
     *
     * @param caption   small caption, such as "LOW"
     * @param numeral   the numeral label, updated by {@link #refreshCounters()}
     * @param ruleLeft  {@code true} to draw a vertical rule on the left
     * @return the counter panel
     */
    private static JPanel buildCounter(String caption, JLabel numeral, boolean ruleLeft) {
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(Theme.tracked(Theme.monoSemiBold(10f), Theme.TRACKING_LABEL));
        captionLabel.setForeground(Theme.MUTED);

        JPanel counter = new JPanel(new BorderLayout(0, 0));
        counter.setOpaque(false);
        EmptyBorder padding = new EmptyBorder(0, 26, 0, 26);
        counter.setBorder(ruleLeft
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.RULE), padding)
                : padding);
        counter.add(captionLabel, BorderLayout.NORTH);
        counter.add(numeral, BorderLayout.CENTER);
        return counter;
    }

    /**
     * Builds the body: the ledger column on the left and the Entry Slip on
     * the right, splitting extra width about 68 / 32.
     *
     * @return the body panel
     */
    private JPanel buildBody() {
        JPanel ledger = buildLedgerColumn();
        ledger.setPreferredSize(new Dimension(0, 0));
        JPanel slip = buildEntrySlip();
        slip.setPreferredSize(new Dimension(0, 0));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(22, PAGE_PAD, 20, PAGE_PAD));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = LEDGER_WEIGHT;
        c.insets = new Insets(0, 0, 0, COLUMN_GAP);
        body.add(ledger, c);

        c.gridx = 1;
        c.weightx = SLIP_WEIGHT;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(slip, c);
        return body;
    }

    /**
     * Builds the ledger column: the toolbar above the card panel that holds
     * the table and the empty state.
     *
     * @return the ledger column panel
     */
    private JPanel buildLedgerColumn() {
        searchField.setPreferredSize(new Dimension(SEARCH_WIDTH, searchField.getPreferredSize().height));
        searchField.setMinimumSize(searchField.getPreferredSize());

        // Every toolbar control sits on one shared bottom line, like entries on a ruled page.
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 16, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_END;

        c.gridx = 0;
        toolbar.add(searchField, c);

        c.gridx = 1;
        c.insets = new Insets(0, 20, 0, 0);
        toolbar.add(statusFilter, c);

        c.gridx = 2;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        toolbar.add(Box.createHorizontalGlue(), c);

        c.gridx = 3;
        c.weightx = 0;
        toolbar.add(viewAllButton, c);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PAPER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new LedgerScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT / 2);
        JPanel corner = new JPanel();
        corner.setBackground(Theme.PAPER);
        scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);

        cardPanel.setOpaque(false);
        cardPanel.add(scroll, CARD_TABLE);
        cardPanel.add(emptyState, CARD_EMPTY);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(toolbar, BorderLayout.NORTH);
        column.add(cardPanel, BorderLayout.CENTER);
        return column;
    }

    /**
     * Configures the ledger table: row height, ruled lines, renderers,
     * column widths and sorting.
     */
    private void configureTable() {
        table.setRowSorter(sorter);
        sorter.setComparator(ItemTableModel.COL_QTY, Comparator.comparingInt(Item::getQuantity));
        sorter.setSortable(ItemTableModel.COL_LINE, false);

        table.setRowHeight(ROW_HEIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.RULE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.PAPER);
        table.setForeground(Theme.INK);
        table.setSelectionBackground(Theme.SURFACE);
        table.setSelectionForeground(Theme.INK);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(Theme.PAPER);

        TableColumnModel columns = table.getColumnModel();
        configureColumn(columns.getColumn(ItemTableModel.COL_LINE),
                new LedgerCellRenderer(LedgerCellRenderer.Kind.LINE_NUMBER), false, 64, false);
        configureColumn(columns.getColumn(ItemTableModel.COL_ID),
                new LedgerCellRenderer(LedgerCellRenderer.Kind.ID), false, 84, false);
        configureColumn(columns.getColumn(ItemTableModel.COL_NAME),
                new LedgerCellRenderer(LedgerCellRenderer.Kind.TEXT), false, 260, true);
        configureColumn(columns.getColumn(ItemTableModel.COL_QTY),
                new LevelBarRenderer(), true, 130, false);
        configureColumn(columns.getColumn(ItemTableModel.COL_THRESHOLD),
                new LedgerCellRenderer(LedgerCellRenderer.Kind.NUMBER), true, 110, false);
        configureColumn(columns.getColumn(ItemTableModel.COL_STATUS),
                new StampBadge(), false, 170, false);
    }

    /**
     * Sets one column's renderers and width.
     *
     * @param column       the table column
     * @param renderer     the cell renderer
     * @param rightAligned {@code true} to right-align the header, for numeric columns
     * @param width        preferred width, in pixels
     * @param flexible     {@code true} if the column should soak up spare width
     */
    private static void configureColumn(TableColumn column, TableCellRenderer renderer,
                                        boolean rightAligned, int width, boolean flexible) {
        column.setCellRenderer(renderer);
        column.setHeaderRenderer(new LedgerHeaderRenderer(rightAligned));
        column.setPreferredWidth(width);
        if (!flexible) {
            column.setMinWidth(width);
            column.setMaxWidth(width);
        }
    }

    /**
     * Builds the Entry Slip: title and mode tag, the three item fields,
     * the slip buttons, a dashed divider and the Update-by-ID mini-form.
     * The content sits in a borderless scroll pane, so it scrolls instead of
     * being squashed when the window is short.
     *
     * @return the Entry Slip panel
     */
    private JPanel buildEntrySlip() {
        JLabel title = new JLabel("ENTRY SLIP");
        title.setFont(Theme.tracked(Theme.display(22f), Theme.TRACKING_TIGHT));
        title.setForeground(Theme.INK);
        modeTag.setFont(Theme.tracked(Theme.monoSemiBold(10f), Theme.TRACKING_LABEL));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(modeTag, BorderLayout.EAST);

        JPanel secondaryButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        secondaryButtons.setOpaque(false);
        secondaryButtons.add(clearButton);
        secondaryButtons.add(voidButton);

        JLabel updateCaption = new JLabel("UPDATE BY ID");
        updateCaption.setFont(Theme.tracked(Theme.monoSemiBold(10.5f), Theme.TRACKING_LABEL));
        updateCaption.setForeground(Theme.INK);

        JPanel loadWrap = new JPanel(new BorderLayout());
        loadWrap.setOpaque(false);
        loadWrap.setBorder(new EmptyBorder(0, 12, idField.getMessageSpace(), 0));
        loadWrap.add(loadButton, BorderLayout.SOUTH);

        JPanel idRow = new JPanel(new BorderLayout());
        idRow.setOpaque(false);
        idRow.add(idField, BorderLayout.CENTER);
        idRow.add(loadWrap, BorderLayout.EAST);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(SlipPanel.PERFORATION_CLEARANCE + 6, SLIP_PAD, SLIP_PAD, SLIP_PAD));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;

        addRow(content, titleRow, c, 16);
        addRow(content, nameField, c, 4);
        addRow(content, quantityField, c, 4);
        addRow(content, thresholdField, c, 16);
        addRow(content, primaryButton, c, 10);
        addRow(content, secondaryButtons, c, 16);
        addRow(content, new DashedRule(), c, 12);
        addRow(content, updateCaption, c, 8);
        addRow(content, idRow, c, 0);

        c.weighty = 1;
        addRow(content, Box.createVerticalGlue(), c, 0);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new LedgerScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        SlipPanel slip = new SlipPanel(false);
        slip.setLayout(new BorderLayout());
        slip.setBorder(new EmptyBorder(1, 1, 1, 1));
        slip.add(scroll, BorderLayout.CENTER);
        return slip;
    }

    /**
     * Adds one full-width row to a grid-bag column, followed by a gap.
     *
     * @param panel     the panel using a {@link GridBagLayout}
     * @param row       the component to add
     * @param c         shared constraints; only the bottom inset is changed
     * @param gapBelow  space below the row, in pixels
     */
    private static void addRow(JPanel panel, Component row, GridBagConstraints c, int gapBelow) {
        c.insets = new Insets(0, 0, gapBelow, 0);
        panel.add(row, c);
    }

    /**
     * Builds the status bar: keyboard hints on the left, save status on the
     * right, with a thin rule above.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        JLabel hints = new JLabel(KEY_HINTS);
        styleStatusText(hints);
        styleStatusText(saveStatus);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.RULE),
                new EmptyBorder(8, PAGE_PAD, 9, PAGE_PAD)));
        bar.add(hints, BorderLayout.WEST);
        bar.add(saveStatus, BorderLayout.EAST);
        return bar;
    }

    /**
     * Gives a status-bar label its small, muted mono style.
     *
     * @param label the label to style
     */
    private static void styleStatusText(JComponent label) {
        label.setFont(Theme.mono(11f));
        label.setForeground(Theme.MUTED);
    }

    // ------------------------------------------------------------------
    // Slip state
    // ------------------------------------------------------------------

    /**
     * Puts the Entry Slip in "new entry" mode: NEW ENTRY tag, ADD TO LEDGER
     * button, and VOID ENTRY disabled because there is nothing to delete.
     */
    private void setNewEntryMode() {
        editingId = NO_ID;
        setModeTag("NEW ENTRY", Theme.INK);
        primaryButton.setText("Add to ledger");
        voidButton.setEnabled(false);
    }

    /**
     * Puts the Entry Slip in edit mode for an item: fills in its name,
     * quantity and threshold, shows an orange "EDITING #0007" tag, renames
     * the primary button to SAVE CHANGES and enables VOID ENTRY.
     *
     * @param item the item to edit
     */
    private void setEditMode(Item item) {
        editingId = item.getId();
        clearFieldErrors();
        nameField.setText(item.getName());
        quantityField.setText(String.valueOf(item.getQuantity()));
        thresholdField.setText(String.valueOf(item.getThreshold()));
        setModeTag("EDITING " + formatId(item.getId()), Theme.ACCENT);
        primaryButton.setText("Save changes");
        voidButton.setEnabled(true);
    }

    /**
     * Empties the slip, removes every error, deselects the table and returns
     * to new-entry mode.
     */
    private void resetSlip() {
        clearInputs();
        clearFieldErrors();
        idField.setText("");
        setNewEntryMode();
        table.clearSelection();
    }

    /**
     * Empties the name, quantity and threshold fields.
     */
    private void clearInputs() {
        nameField.setText("");
        quantityField.setText("");
        thresholdField.setText("");
    }

    /**
     * Removes the error from every field on the slip.
     */
    private void clearFieldErrors() {
        nameField.clearError();
        quantityField.clearError();
        thresholdField.clearError();
        idField.clearError();
    }

    /**
     * Shows a mode tag on the Entry Slip as small mono text in a square
     * outline.
     *
     * @param text  the tag text
     * @param color colour of the text and outline
     */
    private void setModeTag(String text, Color color) {
        modeTag.setText(text);
        modeTag.setForeground(color);
        modeTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(3, 8, 3, 8)));
    }

    /**
     * Returns the slip field that shows errors for an input field.
     *
     * @param field the field named by a validation error
     * @return the matching slip field
     */
    private SlipField fieldFor(InvalidItemException.Field field) {
        return switch (field) {
            case NAME -> nameField;
            case QUANTITY -> quantityField;
            case THRESHOLD -> thresholdField;
            case ID -> idField;
        };
    }

    /**
     * Shows a validation error inline under the field it belongs to, moves
     * focus there, and shows an error toast.
     *
     * @param ex the validation error, naming the failing field
     */
    private void showFieldError(InvalidItemException ex) {
        SlipField target = fieldFor(ex.getField());
        target.setError(ex.getMessage());
        target.focusField();
        toast.showError(ex.getMessage());
    }

    /**
     * Shows a duplicate-name error inline under the name field and as an
     * error toast.
     *
     * @param ex the duplicate-name error
     */
    private void showDuplicateError(DuplicateItemException ex) {
        nameField.setError(ex.getMessage());
        nameField.focusField();
        toast.showError(ex.getMessage());
    }

    /**
     * Recovers when the item on the slip or the selected row no longer
     * exists: resets the slip, reloads the ledger and explains what
     * happened.
     *
     * @param id the ID that could not be found
     */
    private void handleStaleEntry(int id) {
        resetSlip();
        refresh();
        toast.showError("Entry " + formatId(id) + " no longer exists. The ledger has been refreshed.");
    }

    /**
     * Opens an item on the slip for editing, selecting its row if it is
     * visible in the table.
     *
     * @param item the item to edit
     */
    private void openForEditing(Item item) {
        int viewRow = findViewRow(item.getId());
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        } else {
            table.clearSelection();
        }
        setEditMode(item);
        nameField.focusField();
    }

    /**
     * Finds the table row that shows an item.
     *
     * @param id the item ID
     * @return the view row index, or -1 if the item is not shown
     */
    private int findViewRow(int id) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (tableModel.getItemAt(modelRow).getId() == id) {
                return viewRow;
            }
        }
        return -1;
    }

    /**
     * Formats an ID the way the ledger shows it.
     *
     * @param id the item ID
     * @return the ID with a hash and zero padding, for example "#0007"
     */
    private static String formatId(int id) {
        return String.format(Locale.ROOT, "#%04d", id);
    }

    // ------------------------------------------------------------------
    // Refresh, search and filter
    // ------------------------------------------------------------------

    /**
     * Reloads the table from the inventory, re-applies the search and filter,
     * then updates the counters and chooses between the table and the empty
     * state.
     */
    private void refresh() {
        tableModel.setItems(manager.getAll());
        applyFilters();
        refreshCounters();
    }

    /**
     * Updates the TOTAL, LOW and OUT counters. LOW and OUT turn their status
     * colour when above zero.
     */
    private void refreshCounters() {
        int low = manager.countByStatus(StockStatus.LOW_STOCK);
        int out = manager.countByStatus(StockStatus.OUT_OF_STOCK);
        totalCount.setText(String.valueOf(manager.size()));
        lowCount.setText(String.valueOf(low));
        outCount.setText(String.valueOf(out));
        lowCount.setForeground(low > 0 ? Theme.STATUS_LOW : Theme.INK);
        outCount.setForeground(out > 0 ? Theme.STATUS_OUT : Theme.INK);
    }

    /**
     * Shows the empty state when the inventory is empty or nothing matches
     * the current search and filter; shows the table otherwise.
     */
    private void refreshCard() {
        if (manager.isEmpty()) {
            emptyState.setTitle(EmptyStatePanel.DEFAULT_TITLE);
            emptyState.setMessage(EmptyStatePanel.DEFAULT_MESSAGE);
            cards.show(cardPanel, CARD_EMPTY);
        } else if (table.getRowCount() == 0) {
            emptyState.setTitle(NO_MATCH_TITLE);
            emptyState.setMessage(NO_MATCH_MESSAGE);
            cards.show(cardPanel, CARD_EMPTY);
        } else {
            cards.show(cardPanel, CARD_TABLE);
        }
    }

    /**
     * Applies the search text and the status filter to the table. Name
     * matching is done by {@link InventoryManager#searchByName(String)}, so
     * the search rule lives in the service layer.
     */
    private void applyFilters() {
        String query = searchField.getText().trim();
        StockStatus status = selectedStatus();
        if (query.isEmpty() && status == null) {
            sorter.setRowFilter(null);
        } else {
            Set<Integer> matchingIds = new HashSet<>();
            for (Item item : manager.searchByName(query)) {
                matchingIds.add(item.getId());
            }
            sorter.setRowFilter(new LedgerRowFilter(matchingIds, status));
        }
        refreshCard();
    }

    /**
     * Returns the status chosen in the ALL / IN / LOW / OUT filter.
     *
     * @return the chosen status, or {@code null} for ALL
     */
    private StockStatus selectedStatus() {
        return switch (statusFilter.getSelectedIndex()) {
            case 1 -> StockStatus.IN_STOCK;
            case 2 -> StockStatus.LOW_STOCK;
            case 3 -> StockStatus.OUT_OF_STOCK;
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    /**
     * Connects every button, field, the table selection and the keyboard
     * shortcuts to their handler methods.
     */
    private void wireActions() {
        primaryButton.addActionListener(this::onPrimary);
        nameField.addActionListener(this::onPrimary);
        quantityField.addActionListener(this::onPrimary);
        thresholdField.addActionListener(this::onPrimary);
        clearButton.addActionListener(this::onClear);
        voidButton.addActionListener(this::onVoid);
        loadButton.addActionListener(this::onLoadById);
        idField.addActionListener(this::onLoadById);
        viewAllButton.addActionListener(this::onViewAll);
        statusFilter.addActionListener(this::onFilterChanged);
        searchField.getDocument().addDocumentListener(new SearchListener());
        table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);

        getRootPane().registerKeyboardAction(this::onNewEntry,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(this::onFind,
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(this::onEscape,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        table.registerKeyboardAction(this::onVoid,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                JComponent.WHEN_FOCUSED);
    }

    /**
     * Loads the saved inventory into the window and shows the result in the
     * status bar. If the data file cannot be read, an error toast explains
     * that changes will not be saved this session, so the file is left
     * untouched. Call after the window is shown.
     */
    public void loadInventory() {
        try {
            manager.load();
            setSaveStatus("LOADED " + LocalDateTime.now().format(SAVE_TIME), Theme.MUTED);
        } catch (IOException e) {
            setSaveStatus("LOAD FAILED · NOT SAVING", Theme.STATUS_OUT);
            toast.showError("COULD NOT READ " + DATA_FILE + " · CHANGES WILL NOT BE SAVED");
        }
    }

    /**
     * Updates the save status in the status bar from the manager: SAVED with
     * the time of the last save, or SAVE FAILED in red. Leaves the text alone
     * when no save has happened yet.
     */
    private void refreshSaveStatus() {
        if (manager.getLastSaveError().isPresent()) {
            setSaveStatus("SAVE FAILED · CHANGES KEPT UNTIL NEXT SAVE", Theme.STATUS_OUT);
        } else {
            manager.getLastSavedAt().ifPresent(this::showSavedAt);
        }
    }

    /**
     * Shows "SAVED hh:mm" in the status bar.
     *
     * @param time when the inventory was saved
     */
    private void showSavedAt(LocalDateTime time) {
        setSaveStatus("SAVED " + time.format(SAVE_TIME), Theme.MUTED);
    }

    /**
     * Sets the save status text, followed by the data file path, and its colour.
     *
     * @param state the save state, such as "SAVED 23:41"
     * @param color the text colour
     */
    private void setSaveStatus(String state, Color color) {
        saveStatus.setText(state + " · " + DATA_FILE);
        saveStatus.setForeground(color);
    }

    /**
     * Puts keyboard focus in the Item name field, ready for a new entry.
     * Call after the window is shown.
     */
    public void focusNewEntry() {
        nameField.focusField();
    }

    // ------------------------------------------------------------------
    // Event handlers
    // ------------------------------------------------------------------

    /**
     * Refreshes the window after the inventory changes; registered with the
     * inventory manager as its listener.
     */
    private void onInventoryChanged() {
        refresh();
        refreshSaveStatus();
    }

    /**
     * Handles the primary slip button and Enter in the slip fields: adds a
     * new entry in new-entry mode, or saves changes in edit mode.
     *
     * @param e the button or field event (not used)
     */
    private void onPrimary(ActionEvent e) {
        if (editingId == NO_ID) {
            addEntry();
        } else {
            saveEntry();
        }
    }

    /**
     * Adds the slip's contents as a new item. On success the slip is
     * emptied, the new row flashes orange and a receipt toast appears; on
     * failure the error is shown under the right field.
     */
    private void addEntry() {
        clearFieldErrors();
        try {
            Item item = manager.add(nameField.getText(), quantityField.getText(), thresholdField.getText());
            clearInputs();
            nameField.focusField();
            table.flashRow(findViewRow(item.getId()));
            toast.showSuccess("ENTRY " + formatId(item.getId()) + " ADDED");
        } catch (InvalidItemException ex) {
            showFieldError(ex);
        } catch (DuplicateItemException ex) {
            showDuplicateError(ex);
        }
    }

    /**
     * Saves the slip's contents over the item being edited. On success the
     * slip returns to new-entry mode and a receipt toast appears. If the item
     * has disappeared, the stale entry is handled gracefully.
     */
    private void saveEntry() {
        int id = editingId;
        clearFieldErrors();
        try {
            manager.updateItem(id, nameField.getText(), quantityField.getText(), thresholdField.getText());
            resetSlip();
            toast.showSuccess("ENTRY " + formatId(id) + " UPDATED");
        } catch (ItemNotFoundException ex) {
            handleStaleEntry(id);
        } catch (InvalidItemException ex) {
            showFieldError(ex);
        } catch (DuplicateItemException ex) {
            showDuplicateError(ex);
        }
    }

    /**
     * Handles CLEAR: empties the slip and returns to new-entry mode.
     *
     * @param e the button event (not used)
     */
    private void onClear(ActionEvent e) {
        resetSlip();
    }

    /**
     * Handles the Escape key. While typing in the search field, Escape
     * clears the search; anywhere else it clears the slip, like CLEAR.
     *
     * @param e the key event (not used)
     */
    private void onEscape(ActionEvent e) {
        if (searchField.isFocusOwner() && !searchField.getText().isEmpty()) {
            searchField.setText("");
        } else {
            resetSlip();
        }
    }

    /**
     * Handles Ctrl+N: starts a fresh entry and focuses the name field.
     *
     * @param e the key event (not used)
     */
    private void onNewEntry(ActionEvent e) {
        resetSlip();
        nameField.focusField();
    }

    /**
     * Handles Ctrl+F: focuses the search field and selects its text.
     *
     * @param e the key event (not used)
     */
    private void onFind(ActionEvent e) {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Handles VOID ENTRY and the Delete key on the table: asks for
     * confirmation on a slip dialog, then deletes the item being edited.
     *
     * @param e the button or key event (not used)
     */
    private void onVoid(ActionEvent e) {
        if (editingId == NO_ID) {
            toast.showError("Select an entry to void first.");
            return;
        }
        int id = editingId;
        Optional<Item> item = manager.findById(id);
        if (item.isEmpty()) {
            handleStaleEntry(id);
            return;
        }
        boolean confirmed = SlipDialog.confirm(this, "VOID ENTRY " + formatId(id) + "?",
                item.get().getName(), "Void");
        if (!confirmed) {
            return;
        }
        try {
            manager.deleteItem(id);
            resetSlip();
            toast.showSuccess("ENTRY " + formatId(id) + " VOIDED");
        } catch (ItemNotFoundException ex) {
            handleStaleEntry(id);
        }
    }

    /**
     * Handles LOAD and Enter in the Update-by-ID field: parses the typed ID
     * and opens that item for editing, or shows a not-found message under
     * the ID field.
     *
     * @param e the button or field event (not used)
     */
    private void onLoadById(ActionEvent e) {
        idField.clearError();
        try {
            int id = InputValidator.parseId(idField.getText());
            Item item = manager.getById(id);
            idField.setText("");
            openForEditing(item);
            toast.showSuccess("ENTRY " + formatId(id) + " LOADED");
        } catch (InvalidItemException ex) {
            showFieldError(ex);
        } catch (ItemNotFoundException ex) {
            idField.setError(ex.getMessage());
            idField.focusField();
            toast.showError(ex.getMessage());
        }
    }

    /**
     * Handles a change of table selection: opens the selected item for
     * editing. If the selected row's item no longer exists (a stale
     * selection), it is handled gracefully instead.
     *
     * @param e the selection event
     */
    private void onSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        Item rowItem = tableModel.getItemAt(table.convertRowIndexToModel(viewRow));
        Optional<Item> current = manager.findById(rowItem.getId());
        if (current.isEmpty()) {
            handleStaleEntry(rowItem.getId());
        } else {
            setEditMode(current.get());
        }
    }

    /**
     * Handles a click on the ALL / IN / LOW / OUT filter.
     *
     * @param e the toggle event (not used)
     */
    private void onFilterChanged(ActionEvent e) {
        applyFilters();
    }

    /**
     * Handles typing in the search field by re-applying the filters.
     */
    private void onSearchChanged() {
        applyFilters();
    }

    /**
     * Handles VIEW ALL: clears the search and the status filter and shows
     * every item. If the inventory is empty, shows the empty state and a
     * notice saying so.
     *
     * @param e the button event (not used)
     */
    private void onViewAll(ActionEvent e) {
        searchField.setText("");
        statusFilter.setSelectedIndex(0);
        table.clearSelection();
        refresh();
        if (manager.isEmpty()) {
            toast.showInfo("LEDGER EMPTY · NO ENTRIES ON FILE");
        } else {
            toast.showInfo("SHOWING ALL " + manager.size() + " ENTRIES");
        }
    }

    /**
     * Document listener on the search field that re-filters the ledger on
     * every keystroke.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private final class SearchListener implements DocumentListener {

        /**
         * Creates the search listener.
         */
        private SearchListener() {
        }

        /**
         * Re-filters after text is typed.
         *
         * @param e the document event (not used)
         */
        @Override
        public void insertUpdate(DocumentEvent e) {
            onSearchChanged();
        }

        /**
         * Re-filters after text is deleted.
         *
         * @param e the document event (not used)
         */
        @Override
        public void removeUpdate(DocumentEvent e) {
            onSearchChanged();
        }

        /**
         * Re-filters after an attribute change; plain text fields do not
         * send these, but it is handled for completeness.
         *
         * @param e the document event (not used)
         */
        @Override
        public void changedUpdate(DocumentEvent e) {
            onSearchChanged();
        }
    }

    /**
     * Row filter that keeps rows whose item matched the search and has the
     * chosen status.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private static final class LedgerRowFilter extends RowFilter<ItemTableModel, Integer> {

        /** IDs of items whose names matched the search. */
        private final Set<Integer> matchingIds;

        /** Status to keep, or {@code null} to keep every status. */
        private final StockStatus status;

        /**
         * Creates the filter.
         *
         * @param matchingIds IDs of items whose names matched the search
         * @param status      status to keep, or {@code null} for all
         */
        private LedgerRowFilter(Set<Integer> matchingIds, StockStatus status) {
            this.matchingIds = matchingIds;
            this.status = status;
        }

        /**
         * Decides whether a row stays visible.
         *
         * @param entry the row being tested
         * @return {@code true} if the row's item matched the search and has
         *         the chosen status
         */
        @Override
        public boolean include(Entry<? extends ItemTableModel, ? extends Integer> entry) {
            Item item = entry.getModel().getItemAt(entry.getIdentifier());
            return matchingIds.contains(item.getId())
                    && (status == null || item.getStatus() == status);
        }
    }
}
