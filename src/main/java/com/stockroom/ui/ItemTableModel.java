package com.stockroom.ui;

import com.stockroom.model.Item;
import com.stockroom.model.StockStatus;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model that presents a list of {@link Item}s as ledger rows.
 *
 * <p>Columns are No. (ledger line), ID, Item, Qty, Threshold and Status.
 * The model is read-only; the inventory is changed through the entry slip,
 * never by editing cells. The Qty column's value is the whole {@link Item},
 * so its renderer can draw the level bar from both quantity and threshold;
 * sort it with a comparator on {@link Item#getQuantity()}.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class ItemTableModel extends AbstractTableModel {

    /** Column index of the ledger line number. */
    public static final int COL_LINE = 0;

    /** Column index of the item ID. */
    public static final int COL_ID = 1;

    /** Column index of the item name. */
    public static final int COL_NAME = 2;

    /** Column index of the quantity (the value is the whole item). */
    public static final int COL_QTY = 3;

    /** Column index of the low-stock threshold. */
    public static final int COL_THRESHOLD = 4;

    /** Column index of the derived stock status. */
    public static final int COL_STATUS = 5;

    /** Column header names, in column order. */
    private static final String[] COLUMN_NAMES = {"No.", "ID", "Item", "Qty", "Threshold", "Status"};

    /** Column value types, in column order, used for sorting. */
    private static final Class<?>[] COLUMN_TYPES = {
        Integer.class, Integer.class, String.class, Item.class, Integer.class, StockStatus.class
    };

    /** Rows currently shown, in inventory order. */
    private final List<Item> rows = new ArrayList<>();

    /**
     * Creates an empty table model.
     */
    public ItemTableModel() {
        super();
    }

    /**
     * Replaces every row with the given items and tells the table to
     * redraw.
     *
     * @param items the items to show, in display order
     */
    public void setItems(List<Item> items) {
        rows.clear();
        rows.addAll(items);
        fireTableDataChanged();
    }

    /**
     * Returns the item shown in a model row.
     *
     * @param modelRow row index in the model (convert view indexes first)
     * @return the item in that row
     * @throws IndexOutOfBoundsException if {@code modelRow} is not a valid row
     */
    public Item getItemAt(int modelRow) {
        return rows.get(modelRow);
    }

    /**
     * Returns the number of rows.
     *
     * @return the number of items shown
     */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Returns the number of columns.
     *
     * @return always 6
     */
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /**
     * Returns a column's header name.
     *
     * @param column column index
     * @return the header name
     */
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    /**
     * Returns the type of a column's values, so the row sorter compares
     * numbers as numbers and statuses by severity.
     *
     * @param column column index
     * @return the value class
     */
    @Override
    public Class<?> getColumnClass(int column) {
        return COLUMN_TYPES[column];
    }

    /**
     * Returns the value for one cell.
     *
     * @param rowIndex    model row index
     * @param columnIndex column index
     * @return the line number, ID, name, whole item (Qty column), threshold
     *         or status
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Item item = rows.get(rowIndex);
        return switch (columnIndex) {
            case COL_LINE -> rowIndex + 1;
            case COL_ID -> item.getId();
            case COL_NAME -> item.getName();
            case COL_QTY -> item;
            case COL_THRESHOLD -> item.getThreshold();
            case COL_STATUS -> item.getStatus();
            default -> null;
        };
    }
}
