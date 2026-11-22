package com.stockroom.service;

import com.stockroom.exception.DuplicateItemException;
import com.stockroom.exception.InvalidItemException;
import com.stockroom.exception.ItemNotFoundException;
import com.stockroom.model.Item;
import com.stockroom.model.StockStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds every item in the inventory and carries out operations on them.
 *
 * <p>Items are kept in a {@link LinkedHashMap} keyed by ID, so lookups are
 * fast and items keep the order they were added in. IDs start at 1 and go
 * up by one for each new item; they are never reused.</p>
 *
 * <p>Input is validated with {@link InputValidator} before anything changes.
 * After each change, every registered {@link InventoryListener} is told
 * (Observer pattern).</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class InventoryManager {

    /** Placeholder ID that no item ever has, since IDs start at 1. */
    private static final int NO_ID = 0;

    /** All items, keyed by ID, in the order they were added. */
    private final Map<Integer, Item> items = new LinkedHashMap<>();

    /** Listeners told about every change; safe to modify while notifying. */
    private final List<InventoryListener> listeners = new CopyOnWriteArrayList<>();

    /** ID given to the next item that is added. */
    private int nextId = 1;

    /**
     * Creates an empty inventory manager.
     */
    public InventoryManager() {
    }

    /**
     * Validates raw input and adds a new item with the next free ID.
     *
     * @param rawName      the item name as typed
     * @param rawQuantity  the quantity as typed
     * @param rawThreshold the low-stock threshold as typed; blank means the default
     * @return the newly added item
     * @throws InvalidItemException   if any field fails validation
     * @throws DuplicateItemException if another item already has this name,
     *                                ignoring case
     */
    public Item add(String rawName, String rawQuantity, String rawThreshold)
            throws InvalidItemException, DuplicateItemException {
        String name = InputValidator.validateName(rawName);
        int quantity = InputValidator.parseQuantity(rawQuantity);
        int threshold = InputValidator.parseThreshold(rawThreshold);
        if (isNameTaken(name, NO_ID)) {
            throw new DuplicateItemException("An item named '" + name + "' already exists.");
        }
        Item item = new Item(nextId++, name, quantity, threshold);
        items.put(item.getId(), item);
        notifyListeners();
        return item;
    }

    /**
     * Validates raw input and replaces the name, quantity and threshold of
     * an existing item. Keeping the item's own name, even with different
     * letter case, is allowed.
     *
     * <p>Nothing changes unless every field is valid.</p>
     *
     * @param id           the ID of the item to update
     * @param rawName      the new item name as typed
     * @param rawQuantity  the new quantity as typed
     * @param rawThreshold the new low-stock threshold as typed; blank means the default
     * @return the updated item
     * @throws ItemNotFoundException  if no item has this ID
     * @throws InvalidItemException   if any field fails validation
     * @throws DuplicateItemException if a different item already has this name,
     *                                ignoring case
     */
    public Item updateItem(int id, String rawName, String rawQuantity, String rawThreshold)
            throws ItemNotFoundException, InvalidItemException, DuplicateItemException {
        Item item = getById(id);
        String name = InputValidator.validateName(rawName);
        int quantity = InputValidator.parseQuantity(rawQuantity);
        int threshold = InputValidator.parseThreshold(rawThreshold);
        if (isNameTaken(name, id)) {
            throw new DuplicateItemException("An item named '" + name + "' already exists.");
        }
        item.setName(name);
        item.setQuantity(quantity);
        item.setThreshold(threshold);
        notifyListeners();
        return item;
    }

    /**
     * Removes an item from the inventory. Its ID is not reused.
     *
     * @param id the ID of the item to delete
     * @return the item that was removed
     * @throws ItemNotFoundException if no item has this ID
     */
    public Item deleteItem(int id) throws ItemNotFoundException {
        Item removed = items.remove(id);
        if (removed == null) {
            throw notFound(id);
        }
        notifyListeners();
        return removed;
    }

    /**
     * Returns every item, in the order they were added.
     *
     * @return an unmodifiable snapshot list of all items; empty if there are none
     */
    public List<Item> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }

    /**
     * Looks up an item by ID without throwing.
     *
     * @param id the item ID
     * @return the item, or an empty {@code Optional} if no item has this ID
     */
    public Optional<Item> findById(int id) {
        return Optional.ofNullable(items.get(id));
    }

    /**
     * Looks up an item by ID, throwing if it does not exist.
     *
     * @param id the item ID
     * @return the item with this ID
     * @throws ItemNotFoundException if no item has this ID
     */
    public Item getById(int id) throws ItemNotFoundException {
        Item item = items.get(id);
        if (item == null) {
            throw notFound(id);
        }
        return item;
    }

    /**
     * Finds items whose name contains the query, ignoring case.
     *
     * @param query the text to look for; {@code null} or blank matches every item
     * @return an unmodifiable list of matching items, in the order they were added
     */
    public List<Item> searchByName(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Item> matches = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(item);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Counts the items that currently have a given stock status.
     *
     * @param status the status to count
     * @return the number of items with this status
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public int countByStatus(StockStatus status) {
        Objects.requireNonNull(status, "status");
        int count = 0;
        for (Item item : items.values()) {
            if (item.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    /**
     * Tells whether the inventory has no items.
     *
     * @return {@code true} if there are no items
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the number of items in the inventory.
     *
     * @return the item count
     */
    public int size() {
        return items.size();
    }

    /**
     * Registers a listener to be told about every change.
     *
     * @param listener the listener to add
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addListener(InventoryListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Unregisters a listener. Does nothing if it was not registered.
     *
     * @param listener the listener to remove
     */
    public void removeListener(InventoryListener listener) {
        listeners.remove(listener);
    }

    /**
     * Tells every registered listener that the inventory has changed.
     */
    private void notifyListeners() {
        for (InventoryListener listener : listeners) {
            listener.inventoryChanged();
        }
    }

    /**
     * Checks whether another item already has a name, ignoring case.
     *
     * @param name      the trimmed name to check
     * @param excludeId the ID of an item to skip, so an item may keep its own
     *                  name; {@link #NO_ID} to check every item
     * @return {@code true} if an item other than {@code excludeId} has this name
     */
    private boolean isNameTaken(String name, int excludeId) {
        for (Item item : items.values()) {
            if (item.getId() != excludeId && item.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the exception reported when an ID is not in the inventory.
     *
     * @param id the ID that was not found
     * @return an exception with a message such as "No item with ID #0007 exists."
     */
    private static ItemNotFoundException notFound(int id) {
        return new ItemNotFoundException(String.format("No item with ID #%04d exists.", id));
    }
}
