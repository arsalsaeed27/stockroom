package com.stockroom.exception;

/**
 * Thrown when an item name is already used by another item.
 *
 * <p>Names are compared ignoring case, so "Bolts" and "bolts" clash.
 * Renaming an item to its own name is allowed and does not raise this
 * exception.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class DuplicateItemException extends InventoryException {

    /** Version number for serialization. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a duplicate-item exception with a user-facing message.
     *
     * @param message the message naming the duplicate item,
     *                such as "An item named 'X' already exists."
     */
    public DuplicateItemException(String message) {
        super(message);
    }
}
