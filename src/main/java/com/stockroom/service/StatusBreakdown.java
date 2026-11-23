package com.stockroom.service;

import com.stockroom.model.StockStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How many items have each stock status, with their shares as percentages.
 *
 * <p>This is the data behind the dashboard's status chart. It is built from
 * an {@link InventoryManager} with {@link #of(InventoryManager)} and never
 * changes afterwards, so the chart only has to draw it.</p>
 *
 * <p>{@link #percent(StockStatus)} gives the exact share. For display,
 * {@link #wholePercent(StockStatus)} rounds the shares with the
 * largest-remainder method, so the three whole percentages always add up to
 * exactly 100 (or are all 0 for an empty inventory).</p>
 *
 * @param inStock    number of items that are {@link StockStatus#IN_STOCK}
 * @param lowStock   number of items that are {@link StockStatus#LOW_STOCK}
 * @param outOfStock number of items that are {@link StockStatus#OUT_OF_STOCK}
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public record StatusBreakdown(int inStock, int lowStock, int outOfStock) {

    /** Breakdown of an empty inventory. */
    public static final StatusBreakdown EMPTY = new StatusBreakdown(0, 0, 0);

    /** Order of the statuses in the chart and its legend: healthy first. */
    public static final List<StockStatus> ORDER =
            List.of(StockStatus.IN_STOCK, StockStatus.LOW_STOCK, StockStatus.OUT_OF_STOCK);

    /**
     * Creates a breakdown, checking that no count is negative.
     *
     * @param inStock    number of in-stock items
     * @param lowStock   number of low-stock items
     * @param outOfStock number of out-of-stock items
     * @throws IllegalArgumentException if any count is negative
     */
    public StatusBreakdown {
        if (inStock < 0 || lowStock < 0 || outOfStock < 0) {
            throw new IllegalArgumentException("Counts cannot be negative.");
        }
    }

    /**
     * Counts the items of an inventory by status, as they are now.
     *
     * @param manager the inventory to count
     * @return the breakdown
     * @throws NullPointerException if {@code manager} is {@code null}
     */
    public static StatusBreakdown of(InventoryManager manager) {
        Objects.requireNonNull(manager, "manager");
        return new StatusBreakdown(
                manager.countByStatus(StockStatus.IN_STOCK),
                manager.countByStatus(StockStatus.LOW_STOCK),
                manager.countByStatus(StockStatus.OUT_OF_STOCK));
    }

    /**
     * Returns the total number of items.
     *
     * @return the sum of the three counts
     */
    public int total() {
        return inStock + lowStock + outOfStock;
    }

    /**
     * Tells whether there are no items at all.
     *
     * @return {@code true} if every count is zero
     */
    public boolean isEmpty() {
        return total() == 0;
    }

    /**
     * Returns the number of items with a status.
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
     * Returns a status's share of the items as an exact fraction.
     *
     * @param status the status
     * @return a value from 0 to 1; 0 when there are no items
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public double fraction(StockStatus status) {
        int total = total();
        return total == 0 ? 0.0 : (double) count(status) / total;
    }

    /**
     * Returns a status's share of the items as an exact percentage.
     *
     * @param status the status
     * @return a value from 0 to 100; 0 when there are no items
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public double percent(StockStatus status) {
        return fraction(status) * 100.0;
    }

    /**
     * Returns a status's share as a whole percentage for display. The three
     * whole percentages are rounded together (largest remainder), so they
     * always add up to 100; ties go to the status that comes first in
     * {@link #ORDER}. A status with no items is always 0.
     *
     * @param status the status
     * @return a whole number from 0 to 100; 0 when there are no items
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public int wholePercent(StockStatus status) {
        Objects.requireNonNull(status, "status");
        return wholePercents().get(status);
    }

    /**
     * Rounds all three shares together with the largest-remainder method:
     * each share is first rounded down, then the points still missing from
     * 100 go, one each, to the shares that lost the most.
     *
     * @return the whole percentage of every status
     */
    private Map<StockStatus, Integer> wholePercents() {
        Map<StockStatus, Integer> result = new EnumMap<>(StockStatus.class);
        int total = total();
        if (total == 0) {
            ORDER.forEach(status -> result.put(status, 0));
            return result;
        }
        int assigned = 0;
        for (StockStatus status : ORDER) {
            int floor = count(status) * 100 / total;
            result.put(status, floor);
            assigned += floor;
        }
        for (int missing = 100 - assigned; missing > 0; missing--) {
            StockStatus best = null;
            long bestRemainder = -1;
            for (StockStatus status : ORDER) {
                long remainder = (long) count(status) * 100 % total;
                boolean alreadyRaised = result.get(status) * (long) total > (long) count(status) * 100;
                if (!alreadyRaised && remainder > bestRemainder) {
                    best = status;
                    bestRemainder = remainder;
                }
            }
            result.merge(best, 1, Integer::sum);
        }
        return result;
    }
}
