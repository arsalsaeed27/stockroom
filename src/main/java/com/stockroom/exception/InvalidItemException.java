package com.stockroom.exception;

/**
 * Thrown when item input fails validation.
 *
 * <p>Examples: a blank or too-long name, a quantity that is not a whole
 * number, or a quantity or threshold outside the allowed range. The message
 * is the exact text shown to the user, such as
 * "Quantity cannot be negative."</p>
 *
 * <p>Each exception also names the {@link Field} that failed, so the user
 * interface can show the message under the right input without parsing
 * the text.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class InvalidItemException extends InventoryException {

    /** Version number for serialization. */
    private static final long serialVersionUID = 1L;

    /** The input field that failed validation. */
    private final Field field;

    /**
     * Creates an invalid-item exception for one field with a user-facing
     * message.
     *
     * @param field   the input field that failed validation
     * @param message the validation message explaining what is wrong
     */
    public InvalidItemException(Field field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the input field that failed validation.
     *
     * @return the failing field
     */
    public Field getField() {
        return field;
    }

    /**
     * The item input fields that can fail validation.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    public enum Field {

        /** The item name. */
        NAME,

        /** The quantity on hand. */
        QUANTITY,

        /** The low-stock threshold. */
        THRESHOLD,

        /** An item ID typed in to look an item up. */
        ID
    }
}
