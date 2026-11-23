package com.stockroom.service;

import com.stockroom.exception.InventoryException;
import com.stockroom.model.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StatusBreakdown}: counting from an inventory,
 * exact and whole percentages (which must add up to 100), and an empty
 * inventory.
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class StatusBreakdownTest {

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    StatusBreakdownTest() {
    }

    /**
     * Counts are taken from the inventory's current item statuses, and follow
     * later changes when built again.
     *
     * @throws InventoryException if a valid item is rejected or not found
     */
    @Test
    @DisplayName("Counts items by status from the inventory")
    void countsFromInventory() throws InventoryException {
        InventoryManager manager = new InventoryManager();
        manager.add("Washers", "40", "10");  // IN
        manager.add("Anchors", "12", "5");   // IN
        manager.add("Bolts", "3", "5");      // LOW
        manager.add("Nails", "0", "4");      // OUT

        StatusBreakdown breakdown = StatusBreakdown.of(manager);
        assertEquals(new StatusBreakdown(2, 1, 1), breakdown);
        assertEquals(4, breakdown.total());
        assertEquals(1, breakdown.count(StockStatus.LOW_STOCK));
        assertFalse(breakdown.isEmpty());

        manager.updateItem(3, "Bolts", "0", "5");
        assertEquals(new StatusBreakdown(2, 0, 2), StatusBreakdown.of(manager));
    }

    /**
     * Exact percentages and fractions match the counts.
     */
    @Test
    @DisplayName("Exact percentages")
    void exactPercentages() {
        StatusBreakdown breakdown = new StatusBreakdown(2, 1, 1);
        assertEquals(50.0, breakdown.percent(StockStatus.IN_STOCK), 1e-9);
        assertEquals(25.0, breakdown.percent(StockStatus.LOW_STOCK), 1e-9);
        assertEquals(0.25, breakdown.fraction(StockStatus.OUT_OF_STOCK), 1e-9);
    }

    /**
     * Whole percentages always add up to exactly 100, including thirds and
     * other awkward splits, and are rounded to the nearest share.
     */
    @Test
    @DisplayName("Whole percentages always sum to 100")
    void wholePercentagesSumTo100() {
        for (int in = 0; in <= 12; in++) {
            for (int low = 0; low <= 12; low++) {
                for (int out = 0; out <= 12; out++) {
                    StatusBreakdown breakdown = new StatusBreakdown(in, low, out);
                    if (breakdown.isEmpty()) {
                        continue;
                    }
                    int sum = 0;
                    for (StockStatus status : StatusBreakdown.ORDER) {
                        int whole = breakdown.wholePercent(status);
                        assertTrue(Math.abs(whole - breakdown.percent(status)) < 1.0,
                                () -> breakdown + " " + status + " rounded too far");
                        if (breakdown.count(status) == 0) {
                            assertEquals(0, whole, () -> breakdown + " " + status);
                        }
                        sum += whole;
                    }
                    assertEquals(100, sum, breakdown::toString);
                }
            }
        }

        StatusBreakdown thirds = new StatusBreakdown(1, 1, 1);
        assertEquals(34, thirds.wholePercent(StockStatus.IN_STOCK));
        assertEquals(33, thirds.wholePercent(StockStatus.LOW_STOCK));
        assertEquals(33, thirds.wholePercent(StockStatus.OUT_OF_STOCK));

        StatusBreakdown sevenths = new StatusBreakdown(5, 1, 1);  // 71.43 / 14.29 / 14.29
        assertEquals(72, sevenths.wholePercent(StockStatus.IN_STOCK));
        assertEquals(14, sevenths.wholePercent(StockStatus.LOW_STOCK));
    }

    /**
     * An empty inventory gives zero counts and zero percentages, without
     * dividing by zero.
     */
    @Test
    @DisplayName("Empty inventory")
    void emptyInventory() {
        StatusBreakdown breakdown = StatusBreakdown.of(new InventoryManager());
        assertEquals(StatusBreakdown.EMPTY, breakdown);
        assertTrue(breakdown.isEmpty());
        assertEquals(0, breakdown.total());
        for (StockStatus status : StatusBreakdown.ORDER) {
            assertEquals(0, breakdown.count(status));
            assertEquals(0.0, breakdown.percent(status));
            assertEquals(0, breakdown.wholePercent(status));
        }
    }

    /**
     * Negative counts are rejected.
     */
    @Test
    @DisplayName("Negative counts are rejected")
    void negativeCountsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StatusBreakdown(-1, 0, 0));
    }
}
