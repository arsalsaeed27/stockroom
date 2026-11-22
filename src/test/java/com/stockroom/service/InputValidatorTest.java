package com.stockroom.service;

import com.stockroom.exception.InvalidItemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link InputValidator}, covering the input validation test
 * cases TC2 and TC3.
 *
 * <p>Every assertion checks the exact message the user sees, because the
 * GUI shows the exception message as it is.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class InputValidatorTest {

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    InputValidatorTest() {
    }

    /**
     * TC2: an empty or whitespace-only name is rejected with the
     * "Item name cannot be empty." message.
     */
    @Test
    @DisplayName("TC2 – Empty name shows a validation message")
    void tc2EmptyNameRejected() {
        assertAll(
                () -> assertNameRejected("", "Item name cannot be empty."),
                () -> assertNameRejected("   ", "Item name cannot be empty."),
                () -> assertNameRejected(null, "Item name cannot be empty.")
        );
    }

    /**
     * TC3: a negative, non-numeric, blank or too-large quantity is rejected
     * with the matching message.
     */
    @Test
    @DisplayName("TC3 – Negative or non-numeric quantity shows an error message")
    void tc3InvalidQuantityRejected() {
        assertAll(
                () -> assertQuantityRejected("-5", "Quantity cannot be negative."),
                () -> assertQuantityRejected("abc", "Quantity must be a whole number (e.g. 25)."),
                () -> assertQuantityRejected("5.5", "Quantity must be a whole number (e.g. 25)."),
                () -> assertQuantityRejected("1e3", "Quantity must be a whole number (e.g. 25)."),
                () -> assertQuantityRejected("", "Quantity is required."),
                () -> assertQuantityRejected("1000001", "Quantity cannot exceed 1,000,000."),
                () -> assertQuantityRejected("99999999999", "Quantity cannot exceed 1,000,000.")
        );
    }

    /**
     * Asserts that a name is rejected with an exact message.
     *
     * @param raw      the name as typed
     * @param expected the exact message expected
     */
    private static void assertNameRejected(String raw, String expected) {
        InvalidItemException ex = assertThrows(InvalidItemException.class,
                () -> InputValidator.validateName(raw));
        assertEquals(expected, ex.getMessage());
    }

    /**
     * Asserts that a quantity is rejected with an exact message.
     *
     * @param raw      the quantity as typed
     * @param expected the exact message expected
     */
    private static void assertQuantityRejected(String raw, String expected) {
        InvalidItemException ex = assertThrows(InvalidItemException.class,
                () -> InputValidator.parseQuantity(raw));
        assertEquals(expected, ex.getMessage(), "input: \"" + raw + "\"");
    }
}
