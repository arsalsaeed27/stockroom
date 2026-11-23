package com.stockroom.service;

import com.stockroom.exception.InventoryException;
import com.stockroom.model.StockReport;
import com.stockroom.model.StockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ReportGenerator}: section order and sorting,
 * subtotals, totals and percentages, reorder quantities, and an empty
 * inventory.
 *
 * <p>Each test uses a new in-memory manager and a clock fixed at
 * 23 Nov 2026, 14:05 UTC.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class ReportGeneratorTest {

    /** Fixed time every report in these tests is stamped with. */
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 11, 23, 14, 5);

    /** The inventory reported on, recreated before each test. */
    private InventoryManager manager;

    /** The generator under test, recreated before each test. */
    private ReportGenerator generator;

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    ReportGeneratorTest() {
    }

    /**
     * Gives each test an empty manager and a generator with a fixed clock.
     */
    @BeforeEach
    void setUp() {
        manager = new InventoryManager();
        Clock clock = Clock.fixed(FIXED_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        generator = new ReportGenerator(manager, clock);
    }

    /**
     * Adds a mix of items: two in stock, two low, two out.
     *
     * @throws InventoryException if a valid item is rejected
     */
    private void addSampleItems() throws InventoryException {
        manager.add("Washers", "40", "10");   // #1 IN
        manager.add("bolts", "3", "5");       // #2 LOW
        manager.add("Nails", "0", "4");       // #3 OUT
        manager.add("Anchors", "12", "5");    // #4 IN
        manager.add("Clamps", "0", "2");      // #5 OUT
        manager.add("Anvils", "5", "5");      // #6 LOW (at threshold)
    }

    /**
     * Sections always come OUT, LOW, IN, and rows inside a section are
     * sorted by name ignoring case.
     *
     * @throws InventoryException if a valid item is rejected
     */
    @Test
    @DisplayName("Sections are ordered OUT, LOW, IN and sorted by name")
    void sectionOrderAndSorting() throws InventoryException {
        addSampleItems();
        StockReport report = generator.generate();

        List<StockReport.Section> sections = report.sections();
        assertEquals(3, sections.size());
        assertEquals(StockStatus.OUT_OF_STOCK, sections.get(0).status());
        assertEquals(StockStatus.LOW_STOCK, sections.get(1).status());
        assertEquals(StockStatus.IN_STOCK, sections.get(2).status());

        assertEquals(List.of("Clamps", "Nails"), names(sections.get(0)));
        assertEquals(List.of("Anvils", "bolts"), names(sections.get(1)));
        assertEquals(List.of("Anchors", "Washers"), names(sections.get(2)));

        StockReport.Row bolts = sections.get(1).rows().get(1);
        assertEquals(2, bolts.id());
        assertEquals(3, bolts.quantity());
        assertEquals(5, bolts.threshold());
        assertEquals(FIXED_TIME, report.generatedAt());
    }

    /**
     * Each section's subtotal is the sum of its quantities, and the overall
     * units are the sum of every quantity.
     *
     * @throws InventoryException if a valid item is rejected
     */
    @Test
    @DisplayName("Section subtotals and total units")
    void subtotals() throws InventoryException {
        addSampleItems();
        StockReport report = generator.generate();

        assertEquals(0, report.sections().get(0).subtotalUnits());
        assertEquals(8, report.sections().get(1).subtotalUnits());
        assertEquals(52, report.sections().get(2).subtotalUnits());
        assertEquals(60, report.totals().totalUnits());
    }

    /**
     * Totals count the items per status and work out each status's share.
     *
     * @throws InventoryException if a valid item is rejected
     */
    @Test
    @DisplayName("Totals and percentages")
    void totalsAndPercentages() throws InventoryException {
        manager.add("Washers", "40", "10");
        manager.add("Bolts", "3", "5");
        manager.add("Nails", "0", "4");
        manager.add("Anchors", "12", "5");
        StockReport.Totals totals = generator.generate().totals();

        assertEquals(4, totals.itemCount());
        assertEquals(2, totals.inStock());
        assertEquals(1, totals.lowStock());
        assertEquals(1, totals.outOfStock());
        assertEquals(50.0, totals.percent(StockStatus.IN_STOCK), 1e-9);
        assertEquals(25.0, totals.percent(StockStatus.LOW_STOCK), 1e-9);
        assertEquals(25.0, totals.percent(StockStatus.OUT_OF_STOCK), 1e-9);

        manager.add("Clamps", "0", "2");
        manager.add("Hinges", "9", "3");
        StockReport.Totals six = generator.generate().totals();
        assertEquals(100.0 / 3, six.percent(StockStatus.OUT_OF_STOCK), 1e-9);
    }

    /**
     * The reorder list holds every OUT and LOW item, most urgent first and
     * by name, with a suggested quantity of (threshold * 2) - quantity.
     *
     * @throws InventoryException if a valid item is rejected
     */
    @Test
    @DisplayName("Reorder list and suggested quantities")
    void reorderQuantities() throws InventoryException {
        addSampleItems();
        List<StockReport.ReorderLine> lines = generator.generate().reorderList();

        assertEquals(List.of("Clamps", "Nails", "Anvils", "bolts"),
                lines.stream().map(StockReport.ReorderLine::name).toList());
        assertEquals(4, lines.get(0).suggestedQuantity());   // 2*2 - 0
        assertEquals(8, lines.get(1).suggestedQuantity());   // 4*2 - 0
        assertEquals(5, lines.get(2).suggestedQuantity());   // 5*2 - 5
        assertEquals(7, lines.get(3).suggestedQuantity());   // 5*2 - 3
        assertEquals(StockStatus.OUT_OF_STOCK, lines.get(0).status());
        assertEquals(StockStatus.LOW_STOCK, lines.get(3).status());
    }

    /**
     * An empty inventory gives zero totals, three empty sections in the
     * usual order and no reorder lines, without dividing by zero.
     */
    @Test
    @DisplayName("Empty inventory gives an empty report")
    void emptyInventory() {
        StockReport report = generator.generate();

        assertTrue(report.isEmpty());
        assertEquals(0, report.totals().itemCount());
        assertEquals(0, report.totals().totalUnits());
        assertEquals(0.0, report.totals().percent(StockStatus.IN_STOCK));
        assertEquals(StockReport.SECTION_ORDER,
                report.sections().stream().map(StockReport.Section::status).toList());
        report.sections().forEach(section -> {
            assertTrue(section.rows().isEmpty());
            assertEquals(0, section.subtotalUnits());
        });
        assertTrue(report.reorderList().isEmpty());
    }

    /**
     * A report is a snapshot: changing the inventory afterwards does not
     * change it.
     *
     * @throws InventoryException if a valid item is rejected or not found
     */
    @Test
    @DisplayName("Report is an immutable snapshot")
    void reportIsSnapshot() throws InventoryException {
        manager.add("Bolts", "3", "5");
        StockReport report = generator.generate();

        manager.updateItem(1, "Bolts", "50", "5");

        assertFalse(report.isEmpty());
        assertEquals(StockStatus.LOW_STOCK, report.sections().get(1).status());
        assertEquals(3, report.sections().get(1).rows().get(0).quantity());
        assertEquals(1, report.reorderList().size());
    }

    /**
     * Returns the item names of a section, in order.
     *
     * @param section the section
     * @return the names of its rows
     */
    private static List<String> names(StockReport.Section section) {
        return section.rows().stream().map(StockReport.Row::name).toList();
    }
}
