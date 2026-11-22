package com.stockroom.persistence;

import java.io.IOException;

/**
 * Storage for the inventory, so it survives between runs.
 *
 * <p>The inventory manager depends only on this interface, so the storage
 * format (CSV today) can change without touching the business logic.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public interface InventoryRepository {

    /**
     * Loads the stored inventory. If nothing has been saved yet, returns an
     * empty inventory whose next ID is 1.
     *
     * @return the stored items and next ID
     * @throws IOException if the storage exists but cannot be read
     */
    InventoryData load() throws IOException;

    /**
     * Replaces the stored inventory with the given data.
     *
     * @param data the items and next ID to store
     * @throws IOException if the data cannot be written
     */
    void save(InventoryData data) throws IOException;
}
