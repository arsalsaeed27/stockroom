package com.stockroom.exception;

/**
 * Base class for every checked exception raised by the inventory system.
 *
 * <p>Callers that do not care about the exact problem can catch this one
 * type. Each subclass names a specific failure: invalid input, a missing
 * item, or a duplicate name. The message is always written for the user
 * and can be shown in the GUI as it is.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class InventoryException extends Exception {

    /** Version number for serialization. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an inventory exception with a user-facing message.
     *
     * @param message the message explaining what went wrong
     */
    public InventoryException(String message) {
        super(message);
    }
}
