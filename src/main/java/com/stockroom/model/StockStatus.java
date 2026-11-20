package com.stockroom.model;

/**
 * Stock level of an {@link Item}.
 *
 * <p>The status is never stored. {@link Item#getStatus()} works it out
 * from the item's quantity and low-stock threshold every time it is asked:
 * a quantity of zero is {@link #OUT_OF_STOCK}, a quantity at or below the
 * threshold is {@link #LOW_STOCK}, and anything above it is
 * {@link #IN_STOCK}.</p>
 *
 * <p>Each constant has a display label for the user interface and the hex
 * code of its status colour from the Stockroom Ledger design system.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public enum StockStatus {

    /** Quantity is above the low-stock threshold. */
    IN_STOCK("IN STOCK", "#2F6B3A"),

    /** Quantity is above zero but at or below the low-stock threshold. */
    LOW_STOCK("LOW STOCK", "#B7791F"),

    /** Quantity is zero. */
    OUT_OF_STOCK("OUT OF STOCK", "#A8322D");

    /** Human-readable label shown in the user interface, for example "LOW STOCK". */
    private final String displayLabel;

    /** Hex code of the status colour, for example "#B7791F". */
    private final String colorHex;

    /**
     * Creates a status constant with its display label and colour.
     *
     * @param displayLabel label shown in the user interface
     * @param colorHex     status colour as a hex code such as {@code "#2F6B3A"}
     */
    StockStatus(String displayLabel, String colorHex) {
        this.displayLabel = displayLabel;
        this.colorHex = colorHex;
    }

    /**
     * Returns the label shown in the user interface.
     *
     * @return the display label, for example {@code "OUT OF STOCK"}
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    /**
     * Returns the hex code of this status's colour.
     *
     * @return the colour as a hex code, for example {@code "#A8322D"}
     */
    public String getColorHex() {
        return colorHex;
    }

    /**
     * Returns the display label, so the status reads naturally when
     * printed or shown in a component.
     *
     * @return the display label
     */
    @Override
    public String toString() {
        return displayLabel;
    }
}
