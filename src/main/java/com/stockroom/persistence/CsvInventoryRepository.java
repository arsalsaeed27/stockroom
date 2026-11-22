package com.stockroom.persistence;

import com.stockroom.model.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores the inventory in a UTF-8 CSV file, {@code data/inventory.csv} by default.
 *
 * <p>File layout:</p>
 * <pre>
 * # next-id: 8
 * id,name,quantity,threshold
 * 1,Hex Bolts,25,5
 * 2,"Nuts, hex",3,5
 * 3,"6"" Ruler",0,2
 * </pre>
 *
 * <p>Fields containing a comma, a double quote or a line break are wrapped in
 * double quotes, and quotes inside them are doubled (RFC 4180). Lines starting
 * with {@code #} are comments; the {@code next-id} comment keeps deleted IDs
 * from being reused after a restart.</p>
 *
 * <p>Loading is forgiving: a line with the wrong number of fields, a
 * non-numeric number or a blank name is skipped, and the rest of the file is
 * still loaded. Range and duplicate checks are left to the inventory manager.
 * Saving writes a temporary file first and then moves it over the real one,
 * so a failed save never leaves a half-written file.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class CsvInventoryRepository implements InventoryRepository {

    /** Default location of the data file, relative to the working directory. */
    public static final Path DEFAULT_FILE = Path.of("data", "inventory.csv");

    /** Column header row written to and expected in the file. */
    private static final String HEADER = "id,name,quantity,threshold";

    /** Start of the comment that stores the next ID. */
    private static final String NEXT_ID_PREFIX = "# next-id:";

    /** Number of fields in every item row. */
    private static final int FIELD_COUNT = 4;

    /** Line separator used when writing the file. */
    private static final String NEWLINE = "\n";

    /** The CSV file this repository reads and writes. */
    private final Path file;

    /**
     * Creates a repository backed by the default file, {@link #DEFAULT_FILE}.
     */
    public CsvInventoryRepository() {
        this(DEFAULT_FILE);
    }

    /**
     * Creates a repository backed by the given file. The file and its folder
     * are created on the first save if they do not exist.
     *
     * @param file the CSV file to read and write
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public CsvInventoryRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /**
     * Returns the CSV file this repository reads and writes.
     *
     * @return the data file path
     */
    public Path getFile() {
        return file;
    }

    /**
     * Loads the inventory from the CSV file, skipping corrupt lines. A
     * missing file gives an empty inventory.
     *
     * @return the items read and the next ID; the next ID is always greater
     *         than every loaded ID
     * @throws IOException if the file exists but cannot be read, or is not
     *                     valid UTF-8
     */
    @Override
    public InventoryData load() throws IOException {
        if (!Files.exists(file)) {
            return new InventoryData(List.of(), 1);
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<Item> items = new ArrayList<>();
        int nextId = 1;
        int maxId = 0;
        for (List<String> record : parseRecords(content)) {
            String first = record.get(0).trim();
            if (first.startsWith("#")) {
                nextId = Math.max(nextId, parseNextId(first));
            } else if (!isHeader(record)) {
                Item item = toItem(record);
                if (item != null) {
                    items.add(item);
                    maxId = Math.max(maxId, item.getId());
                }
            }
        }
        return new InventoryData(items, Math.max(nextId, maxId + 1));
    }

    /**
     * Writes the inventory to the CSV file, creating its folder if needed.
     * The old file is replaced only once the new one is fully written.
     *
     * @param data the items and next ID to store
     * @throws IOException if the folder or file cannot be written
     */
    @Override
    public void save(InventoryData data) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append(NEXT_ID_PREFIX).append(' ').append(data.nextId()).append(NEWLINE);
        out.append(HEADER).append(NEWLINE);
        for (Item item : data.items()) {
            out.append(item.getId()).append(',')
               .append(escape(item.getName())).append(',')
               .append(item.getQuantity()).append(',')
               .append(item.getThreshold()).append(NEWLINE);
        }

        Path target = file.toAbsolutePath();
        Path folder = target.getParent();
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, "inventory", ".tmp");
        try {
            Files.writeString(temp, out, StandardCharsets.UTF_8);
            moveIntoPlace(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Moves the finished temporary file over the target, atomically where the
     * file system allows it.
     *
     * @param temp   the fully written temporary file
     * @param target the data file to replace
     * @throws IOException if the file cannot be moved
     */
    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Quotes a field if it contains a comma, double quote or line break,
     * doubling any quotes inside it.
     *
     * @param value the raw field value
     * @return the value ready to write into a CSV row
     */
    static String escape(String value) {
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /**
     * Splits CSV text into records of fields, following RFC 4180 quoting.
     * Quoted fields may contain commas, doubled quotes and line breaks.
     * Blank lines are dropped.
     *
     * @param content the whole file content
     * @return the records, each a list of at least one field
     */
    static List<List<String>> parseRecords(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int length = content.length();
        while (i < length) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < length && content.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < length && content.charAt(i + 1) == '\n') {
                    i++;
                }
                endRecord(records, fields, field);
                fields = new ArrayList<>();
            } else {
                field.append(c);
            }
            i++;
        }
        endRecord(records, fields, field);
        return records;
    }

    /**
     * Finishes the current record and adds it to the list, unless it is a
     * blank line.
     *
     * @param records the records parsed so far
     * @param fields  the fields of the current record
     * @param field   the field being built; cleared afterwards
     */
    private static void endRecord(List<List<String>> records, List<String> fields,
                                  StringBuilder field) {
        fields.add(field.toString());
        field.setLength(0);
        boolean blank = fields.size() == 1 && fields.get(0).isBlank();
        if (!blank) {
            records.add(fields);
        }
    }

    /**
     * Tells whether a record is the column header row.
     *
     * @param record the parsed record
     * @return {@code true} if it is the header
     */
    private static boolean isHeader(List<String> record) {
        return String.join(",", record).trim().equalsIgnoreCase(HEADER);
    }

    /**
     * Reads the next ID from a comment line, if it is the next-id comment.
     *
     * @param comment the trimmed comment text, starting with {@code #}
     * @return the stored next ID, or 1 if this comment does not hold a valid one
     */
    private static int parseNextId(String comment) {
        if (!comment.startsWith(NEXT_ID_PREFIX)) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(comment.substring(NEXT_ID_PREFIX.length()).trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Turns one record into an item, or returns {@code null} if the record is
     * corrupt: wrong field count, a number that is not an integer, an ID below
     * 1, or a blank name.
     *
     * @param record the parsed record
     * @return the item, or {@code null} to skip this record
     */
    private static Item toItem(List<String> record) {
        if (record.size() != FIELD_COUNT) {
            return null;
        }
        String name = record.get(1).trim();
        try {
            int id = Integer.parseInt(record.get(0).trim());
            int quantity = Integer.parseInt(record.get(2).trim());
            int threshold = Integer.parseInt(record.get(3).trim());
            if (id < 1 || name.isEmpty()) {
                return null;
            }
            return new Item(id, name, quantity, threshold);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
