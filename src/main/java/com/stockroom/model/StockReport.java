package com.stockroom.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * An immutable stock summary of the whole inventory at one moment.
 *
 * <p>A report holds overall totals, the items grouped into one
 * {@link Section} per {@link StockStatus} (always in the order
 * {@link #SECTION_ORDER OUT OF STOCK, LOW STOCK, IN STOCK}), and a reorder
 * list of every item that is low or out of stock. It copies the values it
 * needs from the items, so later changes to the inventory never change a
 * report that has already been built.</p>
 *
 * <p>Reports are built by {@code com.stockroom.service.ReportGenerator} and
 * written to disk by {@code com.stockroom.persistence.HtmlReportWriter}.</p>
 *
 * @param generatedAt  when the report was built
 * @param totals       overall counts and units
 * @param sections     one section per status, in {@link #SECTION_ORDER};
 *                     copied into an unmodifiable list
 * @param reorderList  every low or out-of-stock item with a suggested
 *                     reorder quantity; copied into an unmodifiable list
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public record StockReport(LocalDateTime generatedAt, Totals totals,
                          List<Section> sections, List<ReorderLine> reorderList) {

    /** Order of the sections in every report: most urgent first. */
    public static final List<StockStatus> SECTION_ORDER =
            List.of(StockStatus.OUT_OF_STOCK, StockStatus.LOW_STOCK, StockStatus.IN_STOCK);

    /**
     * Creates a report, checking for {@code null} values and taking
     * unmodifiable copies of the lists.
     *
     * @param generatedAt when the report was built
     * @param totals      overall counts and units
     * @param sections    one section per status, in {@link #SECTION_ORDER}
     * @param reorderList every low or out-of-stock item to reorder
     * @throws NullPointerException if any argument or list element is {@code null}
     */
    public StockReport {
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(totals, "totals");
        sections = List.copyOf(sections);
        reorderList = List.copyOf(reorderList);
    }

    /**
     * Tells whether the report covers no items at all.
     *
     * @return {@code true} if the inventory was empty when the report was built
     */
    public boolean isEmpty() {
        return totals.itemCount() == 0;
    }

    /**
     * Overall figures for the inventory.
     *
     * @param itemCount  number of items
     * @param totalUnits sum of every item's quantity
     * @param inStock    number of items that are {@link StockStatus#IN_STOCK}
     * @param lowStock   number of items that are {@link StockStatus#LOW_STOCK}
     * @param outOfStock number of items that are {@link StockStatus#OUT_OF_STOCK}
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public record Totals(int itemCount, long totalUnits, int inStock, int lowStock, int outOfStock) {

        /**
         * Returns the number of items with a given status.
         *
         * @param status the status to count
         * @return the number of items with this status
         * @throws NullPointerException if {@code status} is {@code null}
         */
        public int count(StockStatus status) {
            return switch (Objects.requireNonNull(status, "status")) {
                case IN_STOCK -> inStock;
                case LOW_STOCK -> lowStock;
                case OUT_OF_STOCK -> outOfStock;
            };
        }

        /**
         * Returns the share of items with a given status, as a percentage.
         *
         * @param status the status to measure
         * @return a value from 0 to 100; 0 when there are no items
         * @throws NullPointerException if {@code status} is {@code null}
         */
        public double percent(StockStatus status) {
            if (itemCount == 0) {
                return 0.0;
            }
            return count(status) * 100.0 / itemCount;
        }
    }

    /**
     * The items that share one stock status, sorted by name.
     *
     * @param status        the status every row in this section has
     * @param rows          the rows, sorted by name ignoring case; copied
     *                      into an unmodifiable list
     * @param subtotalUnits sum of the quantities in this section
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public record Section(StockStatus status, List<Row> rows, long subtotalUnits) {

        /**
         * Creates a section, checking the status and taking an unmodifiable
         * copy of the rows.
         *
         * @param status        the status every row has
         * @param rows          the rows, sorted by name
         * @param subtotalUnits sum of the quantities in this section
         * @throws NullPointerException if {@code status}, {@code rows} or any row is {@code null}
         */
        public Section {
            Objects.requireNonNull(status, "status");
            rows = List.copyOf(rows);
        }
    }

    /**
     * One item as it appears in a section.
     *
     * @param id        the item ID
     * @param name      the item name
     * @param quantity  units on hand
     * @param threshold low-stock threshold
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public record Row(int id, String name, int quantity, int threshold) {
    }

    /**
     * One item on the reorder list, with the quantity suggested to restock it.
     *
     * @param id                the item ID
     * @param name              the item name
     * @param status            the item's status, low or out of stock
     * @param quantity          units on hand
     * @param threshold         low-stock threshold
     * @param suggestedQuantity units to reorder: {@code (threshold * 2) - quantity}
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public record ReorderLine(int id, String name, StockStatus status, int quantity,
                              int threshold, int suggestedQuantity) {
    }
}
