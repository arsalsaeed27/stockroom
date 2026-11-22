package com.stockroom.service;

import com.stockroom.exception.DuplicateItemException;
import com.stockroom.exception.InventoryException;
import com.stockroom.exception.ItemNotFoundException;
import com.stockroom.model.Item;
import com.stockroom.model.StockStatus;
import com.stockroom.persistence.CsvInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InventoryManager}, covering test cases TC1, TC4–TC9,
 * TC11 and TC12.
 *
 * <p>Each test starts with a new, empty manager. Error cases check the exact
 * message shown to the user and that the inventory did not change.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class InventoryManagerTest {

    /** The manager under test, recreated before each test. */
    private InventoryManager manager;

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    InventoryManagerTest() {
    }

    /**
     * Gives each test a new, empty manager.
     */
    @BeforeEach
    void setUp() {
        manager = new InventoryManager();
    }

    /**
     * TC1: a valid item is added with ID 1, trimmed name, given quantity
     * and threshold, and derived status.
     *
     * @throws InventoryException if the valid item is rejected
     */
    @Test
    @DisplayName("TC1 – Add a valid item")
    void tc1AddValidItem() throws InventoryException {
        Item item = manager.add("  Hex Bolts  ", "25", "10");

        assertEquals(1, item.getId());
        assertEquals("Hex Bolts", item.getName());
        assertEquals(25, item.getQuantity());
        assertEquals(10, item.getThreshold());
        assertEquals(StockStatus.IN_STOCK, item.getStatus());
        assertEquals(1, manager.size());
        assertSame(item, manager.getById(1));
    }

    /**
     * TC4: viewing all items returns every item in the order added.
     *
     * @throws InventoryException if a valid item is rejected
     */
    @Test
    @DisplayName("TC4 – View all items shows every item")
    void tc4ViewAllShowsEveryItem() throws InventoryException {
        manager.add("Hex Bolts", "25", "");
        manager.add("Washers", "3", "");
        manager.add("Wing Nuts", "0", "");

        List<Item> all = manager.getAll();

        assertEquals(3, all.size());
        assertEquals("Hex Bolts", all.get(0).getName());
        assertEquals("Washers", all.get(1).getName());
        assertEquals("Wing Nuts", all.get(2).getName());
        assertEquals(List.of(1, 2, 3),
                all.stream().map(Item::getId).toList());
        assertFalse(manager.isEmpty());
    }

    /**
     * TC5: viewing an empty inventory reports it as empty, so the GUI can
     * show its empty message.
     */
    @Test
    @DisplayName("TC5 – View empty inventory reports it is empty")
    void tc5ViewEmptyInventory() {
        assertTrue(manager.isEmpty());
        assertEquals(0, manager.size());
        assertTrue(manager.getAll().isEmpty());
    }

    /**
     * TC6: updating an existing item changes its name, quantity, threshold
     * and derived status, and keeps its ID.
     *
     * @throws InventoryException if the valid update is rejected
     */
    @Test
    @DisplayName("TC6 – Update an existing item")
    void tc6UpdateExistingItem() throws InventoryException {
        Item added = manager.add("Hex Bolts", "25", "10");

        Item updated = manager.updateItem(added.getId(), "Hex Bolts M8", "4", "6");

        assertEquals(1, updated.getId());
        assertEquals("Hex Bolts M8", updated.getName());
        assertEquals(4, updated.getQuantity());
        assertEquals(6, updated.getThreshold());
        assertEquals(StockStatus.LOW_STOCK, updated.getStatus());
        assertEquals("Hex Bolts M8", manager.getById(1).getName());
        assertEquals(1, manager.size());
    }

    /**
     * TC7: updating an ID that does not exist throws
     * {@link ItemNotFoundException} with a friendly message and changes nothing.
     *
     * @throws InventoryException if the setup item is rejected
     */
    @Test
    @DisplayName("TC7 – Update a non-existing item is handled gracefully")
    void tc7UpdateNonExistingItem() throws InventoryException {
        manager.add("Hex Bolts", "25", "10");

        ItemNotFoundException ex = assertThrows(ItemNotFoundException.class,
                () -> manager.updateItem(42, "Ghost", "1", "1"));

        assertEquals("No item with ID #0042 exists.", ex.getMessage());
        assertEquals(1, manager.size());
        assertEquals("Hex Bolts", manager.getById(1).getName());
    }

    /**
     * TC8: deleting an existing item removes it, and its ID is not reused.
     *
     * @throws InventoryException if a valid item is rejected or the delete fails
     */
    @Test
    @DisplayName("TC8 – Delete an existing item")
    void tc8DeleteExistingItem() throws InventoryException {
        manager.add("Hex Bolts", "25", "");
        Item washers = manager.add("Washers", "3", "");

        Item removed = manager.deleteItem(washers.getId());

        assertSame(washers, removed);
        assertEquals(1, manager.size());
        assertTrue(manager.findById(2).isEmpty());
        assertEquals(3, manager.add("Wing Nuts", "7", "").getId());
    }

    /**
     * TC9: a duplicate name, ignoring case and surrounding spaces, is
     * rejected on add and on update; renaming an item to its own name is allowed.
     *
     * @throws InventoryException if a valid add or self-rename is rejected
     */
    @Test
    @DisplayName("TC9 – Duplicate name is rejected")
    void tc9DuplicateNameRejected() throws InventoryException {
        manager.add("Hex Bolts", "25", "");
        Item washers = manager.add("Washers", "3", "");

        DuplicateItemException onAdd = assertThrows(DuplicateItemException.class,
                () -> manager.add("  hex bolts ", "5", ""));
        assertEquals("An item named 'hex bolts' already exists.", onAdd.getMessage());
        assertEquals(2, manager.size());

        DuplicateItemException onUpdate = assertThrows(DuplicateItemException.class,
                () -> manager.updateItem(washers.getId(), "HEX BOLTS", "3", ""));
        assertEquals("An item named 'HEX BOLTS' already exists.", onUpdate.getMessage());
        assertEquals("Washers", manager.getById(2).getName());

        assertEquals("WASHERS", manager.updateItem(2, "WASHERS", "3", "").getName());
    }

    /**
     * TC11: the inventory is saved to CSV automatically after each change,
     * and loading the file into a new manager gives the same items and the
     * same next ID.
     *
     * <p>Names contain commas and double quotes to check escaping. An item is
     * deleted so the next ID is above the highest remaining ID; the restored
     * manager must not reuse the deleted ID.</p>
     *
     * @param dir a temporary folder created and removed by JUnit
     * @throws InventoryException if a setup item is rejected
     * @throws IOException        if the CSV file cannot be written or read
     */
    @Test
    @DisplayName("TC11 – CSV save/load round-trip")
    void tc11CsvRoundTrip(@TempDir Path dir) throws InventoryException, IOException {
        Path file = dir.resolve("data").resolve("inventory.csv");
        InventoryManager original = new InventoryManager(new CsvInventoryRepository(file));
        original.add("Hex Bolts", "25", "");
        original.add("Nuts, hex", "3", "10");
        original.add("6\" Ruler, \"steel\"", "0", "2");
        original.add("To be voided", "1", "");
        original.deleteItem(4);

        assertTrue(Files.exists(file), "the file should be saved automatically");
        assertTrue(original.getLastSavedAt().isPresent());

        InventoryManager restored = new InventoryManager(new CsvInventoryRepository(file));
        restored.load();

        List<Item> before = original.getAll();
        List<Item> after = restored.getAll();
        assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) {
            Item expected = before.get(i);
            Item actual = after.get(i);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getQuantity(), actual.getQuantity());
            assertEquals(expected.getThreshold(), actual.getThreshold());
            assertEquals(expected.getStatus(), actual.getStatus());
        }
        assertEquals(5, restored.add("Washers", "40", "").getId(),
                "the deleted ID 4 must not be reused after a restart");
    }

    /**
     * TC12: deleting an ID that does not exist throws
     * {@link ItemNotFoundException} with a friendly message and changes nothing.
     *
     * @throws InventoryException if the setup item is rejected
     */
    @Test
    @DisplayName("TC12 – Delete a non-existing item is handled gracefully")
    void tc12DeleteNonExistingItem() throws InventoryException {
        manager.add("Hex Bolts", "25", "");

        ItemNotFoundException ex = assertThrows(ItemNotFoundException.class,
                () -> manager.deleteItem(7));

        assertEquals("No item with ID #0007 exists.", ex.getMessage());
        assertEquals(1, manager.size());
    }
}
