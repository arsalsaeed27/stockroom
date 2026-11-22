package com.stockroom.persistence;

import com.stockroom.model.Item;

import java.util.List;

/**
 * Everything needed to save and restore an inventory: the items and the ID
 * to give the next new item.
 *
 * <p>The next ID is stored so that IDs of deleted items are never reused,
 * even after a restart.</p>
 *
 * @param items  the items, in ledger order; copied into an unmodifiable list
 * @param nextId the ID to give the next item that is added
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public record InventoryData(List<Item> items, int nextId) {

    /**
     * Creates the data, taking an unmodifiable copy of the item list.
     *
     * @param items  the items, in ledger order
     * @param nextId the ID to give the next item that is added
     * @throws NullPointerException if {@code items} or any item is {@code null}
     */
    public InventoryData {
        items = List.copyOf(items);
    }
}
