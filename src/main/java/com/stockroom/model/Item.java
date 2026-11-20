package com.stockroom.model;

/**
 * A single inventory item recorded in the stockroom ledger.
 *
 * <p>An item has an ID, a name, a quantity and a low-stock threshold. The
 * ID is assigned once by the inventory manager and never changes. The stock
 * status is not stored: {@link #getStatus()} works it out from the quantity
 * and threshold.</p>
 *
 * <p>The setters do not validate their arguments. Checking input is the
 * service layer's job, which validates values before passing them in.</p>
 *
 * <p>Two items are equal when they have the same ID.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class Item {

    /** Low-stock threshold used when the user does not enter one. */
    public static final int DEFAULT_THRESHOLD = 5;

    /** Unique ID, assigned by the inventory manager. */
    private final int id;

    /** Item name, already trimmed and validated by the service layer. */
    private String name;

    /** Number of units on hand. */
    private int quantity;

    /** Quantity at or below which the item counts as low on stock. */
    private int threshold;

    /**
     * Creates an item with every field given.
     *
     * @param id        unique ID assigned by the inventory manager
     * @param name      item name
     * @param quantity  number of units on hand
     * @param threshold low-stock threshold
     */
    public Item(int id, String name, int quantity, int threshold) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.threshold = threshold;
    }

    /**
     * Creates an item that uses the {@link #DEFAULT_THRESHOLD default threshold}.
     *
     * @param id       unique ID assigned by the inventory manager
     * @param name     item name
     * @param quantity number of units on hand
     */
    public Item(int id, String name, int quantity) {
        this(id, name, quantity, DEFAULT_THRESHOLD);
    }

    /**
     * Returns the item's unique ID.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the item's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the item's name. The value is not validated here.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of units on hand.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the number of units on hand. The value is not validated here.
     *
     * @param quantity the new quantity
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the low-stock threshold.
     *
     * @return the threshold
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Sets the low-stock threshold. The value is not validated here.
     *
     * @param threshold the new threshold
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * Works out the stock status from the current quantity and threshold.
     * The status is never stored, so it always matches the current values.
     *
     * <ul>
     *   <li>quantity is 0: {@link StockStatus#OUT_OF_STOCK}</li>
     *   <li>quantity is at or below the threshold: {@link StockStatus#LOW_STOCK}</li>
     *   <li>otherwise: {@link StockStatus#IN_STOCK}</li>
     * </ul>
     *
     * @return the stock status
     */
    public StockStatus getStatus() {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantity <= threshold) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    /**
     * Compares this item with another object. Two items are equal when
     * they have the same ID.
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is an {@code Item} with the same ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Item)) {
            return false;
        }
        Item other = (Item) obj;
        return id == other.id;
    }

    /**
     * Returns a hash code based only on the ID, matching {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /**
     * Returns a readable summary of the item, useful for logging and debugging.
     *
     * @return text such as {@code Item{id=7, name='Bolts', quantity=3, threshold=5, status=LOW STOCK}}
     */
    @Override
    public String toString() {
        return "Item{id=" + id
                + ", name='" + name + '\''
                + ", quantity=" + quantity
                + ", threshold=" + threshold
                + ", status=" + getStatus() + '}';
    }
}
