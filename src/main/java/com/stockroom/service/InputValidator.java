package com.stockroom.service;

import com.stockroom.exception.InvalidItemException;
import com.stockroom.model.Item;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Parses and validates the raw text the user types for an item.
 *
 * <p>All input rules live here, not in the GUI. Each method takes the raw
 * string from a text field and either returns a clean value or throws an
 * {@link InvalidItemException} whose message can be shown to the user
 * as it is.</p>
 *
 * <p>Numbers are parsed with {@link BigInteger}, so very large input such as
 * "99999999999" is reported as too large instead of overflowing an
 * {@code int}.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public final class InputValidator {

    /** Longest item name allowed, in characters. */
    public static final int MAX_NAME_LENGTH = 40;

    /** Largest quantity allowed. */
    public static final int MAX_QUANTITY = 1_000_000;

    /** Largest low-stock threshold allowed. */
    public static final int MAX_THRESHOLD = 10_000;

    /** Matches an optionally negative whole number made of ASCII digits only. */
    private static final Pattern WHOLE_NUMBER = Pattern.compile("-?[0-9]+");

    /**
     * Prevents instantiation; this class only has static methods.
     */
    private InputValidator() {
    }

    /**
     * Trims and validates an item name.
     *
     * @param raw the name exactly as typed; may be {@code null}
     * @return the trimmed name
     * @throws InvalidItemException if the name is blank or longer than
     *                              {@value #MAX_NAME_LENGTH} characters
     */
    public static String validateName(String raw) throws InvalidItemException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new InvalidItemException("Item name cannot be empty.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidItemException("Item name must be 40 characters or fewer.");
        }
        return name;
    }

    /**
     * Parses and validates a quantity.
     *
     * @param raw the quantity exactly as typed; may be {@code null}
     * @return the quantity, between 0 and {@value #MAX_QUANTITY}
     * @throws InvalidItemException if the text is blank, not a whole number,
     *                              negative, or larger than {@value #MAX_QUANTITY}
     */
    public static int parseQuantity(String raw) throws InvalidItemException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw new InvalidItemException("Quantity is required.");
        }
        return parseWholeNumber(text, "Quantity", MAX_QUANTITY, "1,000,000", "25");
    }

    /**
     * Parses and validates a low-stock threshold. Blank input means the
     * default threshold.
     *
     * @param raw the threshold exactly as typed; may be {@code null} or blank
     * @return the threshold, between 0 and {@value #MAX_THRESHOLD}, or
     *         {@link Item#DEFAULT_THRESHOLD} when the input is blank
     * @throws InvalidItemException if the text is not a whole number,
     *                              negative, or larger than {@value #MAX_THRESHOLD}
     */
    public static int parseThreshold(String raw) throws InvalidItemException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return Item.DEFAULT_THRESHOLD;
        }
        return parseWholeNumber(text, "Threshold", MAX_THRESHOLD, "10,000", "5");
    }

    /**
     * Parses non-blank, trimmed text as a whole number from 0 up to a maximum.
     *
     * @param text     the trimmed, non-blank text to parse
     * @param field    the field name used at the start of each message
     * @param max      the largest value allowed
     * @param maxLabel the maximum written for the user, such as "1,000,000"
     * @param example  an example value shown in the not-a-number message
     * @return the parsed value
     * @throws InvalidItemException if the text is not a whole number,
     *                              is negative, or is larger than {@code max}
     */
    private static int parseWholeNumber(String text, String field, int max,
                                        String maxLabel, String example)
            throws InvalidItemException {
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new InvalidItemException(
                    field + " must be a whole number (e.g. " + example + ").");
        }
        BigInteger value = new BigInteger(text);
        if (value.signum() < 0) {
            throw new InvalidItemException(field + " cannot be negative.");
        }
        if (value.compareTo(BigInteger.valueOf(max)) > 0) {
            throw new InvalidItemException(field + " cannot exceed " + maxLabel + ".");
        }
        return value.intValue();
    }
}
