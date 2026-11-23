package com.stockroom.service;

import com.stockroom.model.Item;
import com.stockroom.model.StockReport;
import com.stockroom.model.StockStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link StockReport} from the current contents of an
 * {@link InventoryManager}.
 *
 * <p>The report only reads existing data: it never changes the inventory.
 * Items are grouped by {@link StockStatus} in the order given by
 * {@link StockReport#SECTION_ORDER} (OUT OF STOCK, LOW STOCK, IN STOCK),
 * sorted by name inside each section, and every low or out-of-stock item is
 * put on the reorder list with a suggested quantity of
 * {@code (threshold * 2) - quantity}.</p>
 *
 * <p>The time stamp comes from a {@link Clock}, so tests can fix it.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class ReportGenerator {

    /** Order of rows inside a section and on the reorder list: by name ignoring case, then by ID. */
    private static final Comparator<Item> BY_NAME =
            Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(Item::getId);

    /** The inventory the reports describe. */
    private final InventoryManager manager;

    /** Source of the report time stamp. */
    private final Clock clock;

    /**
     * Creates a generator that stamps reports with the system clock.
     *
     * @param manager the inventory to report on
     * @throws NullPointerException if {@code manager} is {@code null}
     */
    public ReportGenerator(InventoryManager manager) {
        this(manager, Clock.systemDefaultZone());
    }

    /**
     * Creates a generator that stamps reports with the given clock.
     *
     * @param manager the inventory to report on
     * @param clock   source of the report time stamp
     * @throws NullPointerException if either argument is {@code null}
     */
    public ReportGenerator(InventoryManager manager, Clock clock) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Builds a report of the inventory as it is now. An empty inventory gives
     * a report with zero totals, three empty sections and an empty reorder
     * list; see {@link StockReport#isEmpty()}.
     *
     * @return the new, immutable report
     */
    public StockReport generate() {
        List<Item> items = new ArrayList<>(manager.getAll());
        items.sort(BY_NAME);

        List<StockReport.Section> sections = new ArrayList<>();
        List<StockReport.ReorderLine> reorderList = new ArrayList<>();
        for (StockStatus status : StockReport.SECTION_ORDER) {
            List<StockReport.Row> rows = new ArrayList<>();
            long subtotal = 0;
            for (Item item : items) {
                if (item.getStatus() != status) {
                    continue;
                }
                rows.add(new StockReport.Row(item.getId(), item.getName(), item.getQuantity(), item.getThreshold()));
                subtotal += item.getQuantity();
                if (needsReorder(status)) {
                    reorderList.add(new StockReport.ReorderLine(item.getId(), item.getName(), status,
                            item.getQuantity(), item.getThreshold(), suggestedReorder(item)));
                }
            }
            sections.add(new StockReport.Section(status, rows, subtotal));
        }

        return new StockReport(LocalDateTime.now(clock), buildTotals(items), sections, reorderList);
    }

    /**
     * Works out the quantity suggested to restock an item.
     *
     * @param item the item to restock
     * @return {@code (threshold * 2) - quantity}
     */
    static int suggestedReorder(Item item) {
        return item.getThreshold() * 2 - item.getQuantity();
    }

    /**
     * Tells whether items with a given status belong on the reorder list.
     *
     * @param status the status to check
     * @return {@code true} for LOW STOCK and OUT OF STOCK
     */
    private static boolean needsReorder(StockStatus status) {
        return status == StockStatus.LOW_STOCK || status == StockStatus.OUT_OF_STOCK;
    }

    /**
     * Counts the items and units, overall and per status.
     *
     * @param items every item in the inventory
     * @return the overall totals
     */
    private static StockReport.Totals buildTotals(List<Item> items) {
        long units = 0;
        int in = 0;
        int low = 0;
        int out = 0;
        for (Item item : items) {
            units += item.getQuantity();
            switch (item.getStatus()) {
                case IN_STOCK -> in++;
                case LOW_STOCK -> low++;
                case OUT_OF_STOCK -> out++;
            }
        }
        return new StockReport.Totals(items.size(), units, in, low, out);
    }
}
