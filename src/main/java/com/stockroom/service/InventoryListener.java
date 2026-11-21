package com.stockroom.service;

/**
 * Observer that is told whenever the inventory changes.
 *
 * <p>The GUI registers a listener with {@link InventoryManager} so the
 * table, counters and saved file refresh on their own after every change.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
@FunctionalInterface
public interface InventoryListener {

    /**
     * Called after the inventory has changed. Implementations should read
     * the new state from the manager.
     */
    void inventoryChanged();
}
