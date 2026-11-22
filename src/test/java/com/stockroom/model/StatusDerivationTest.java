package com.stockroom.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link Item#getStatus()}, covering test case TC10.
 *
 * <p>The status is derived, never stored: 0 is OUT_OF_STOCK, at or below
 * the threshold is LOW_STOCK, above it is IN_STOCK.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class StatusDerivationTest {

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    StatusDerivationTest() {
    }

    /**
     * TC10: the status is correct at every boundary of the rule, and it
     * follows changes to quantity and threshold.
     */
    @Test
    @DisplayName("TC10 – Status derivation at the boundaries")
    void tc10StatusAtBoundaries() {
        Item changing = new Item(4, "Washers", 6, 5);
        changing.setQuantity(5);

        assertAll(
                () -> assertEquals(StockStatus.OUT_OF_STOCK, new Item(1, "A", 0, 5).getStatus(),
                        "qty 0 is OUT"),
                () -> assertEquals(StockStatus.LOW_STOCK, new Item(1, "A", 1, 5).getStatus(),
                        "qty 1 with threshold 5 is LOW"),
                () -> assertEquals(StockStatus.LOW_STOCK, new Item(1, "A", 5, 5).getStatus(),
                        "qty equal to threshold is LOW"),
                () -> assertEquals(StockStatus.IN_STOCK, new Item(1, "A", 6, 5).getStatus(),
                        "qty one above threshold is IN"),
                () -> assertEquals(StockStatus.OUT_OF_STOCK, new Item(1, "A", 0, 0).getStatus(),
                        "qty 0 with threshold 0 is OUT, not LOW"),
                () -> assertEquals(StockStatus.IN_STOCK, new Item(1, "A", 1, 0).getStatus(),
                        "qty 1 with threshold 0 is IN"),
                () -> assertEquals(StockStatus.LOW_STOCK, new Item(1, "A", 5).getStatus(),
                        "default threshold is 5, so qty 5 is LOW"),
                () -> assertEquals(StockStatus.LOW_STOCK, changing.getStatus(),
                        "status follows a quantity change from 6 to 5"),
                () -> assertEquals("OUT OF STOCK", StockStatus.OUT_OF_STOCK.getDisplayLabel())
        );
    }
}
