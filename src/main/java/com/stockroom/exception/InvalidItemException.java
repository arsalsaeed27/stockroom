package com.stockroom.exception;

/**
 * Thrown when item input fails validation.
 *
 * <p>Examples: a blank or too-long name, a quantity that is not a whole
 * number, or a quantity or threshold outside the allowed range. The message
 * is the exact text shown to the user, such as
 * "Quantity cannot be negative."</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class InvalidItemException extends InventoryException {

    /** Version number for serialization. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an invalid-item exception with a user-facing message.
     *
     * @param message the validation message explaining which input is wrong
     */
    public InvalidItemException(String message) {
        super(message);
    }
}
