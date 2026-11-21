package com.stockroom.exception;

/**
 * Thrown when an operation refers to an item ID that is not in the inventory.
 *
 * <p>This happens when the user updates, loads or deletes an ID that never
 * existed or was already deleted, for example through the "Update by ID"
 * form or a stale table selection.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class ItemNotFoundException extends InventoryException {

    /** Version number for serialization. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an item-not-found exception with a user-facing message.
     *
     * @param message the message naming the item that could not be found
     */
    public ItemNotFoundException(String message) {
        super(message);
    }
}
