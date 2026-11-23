# Stockroom

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

**Java-Based Inventory Management System Using Modern Software Construction Tools**
Software Construction, Open-Ended Lab.

Stockroom is a desktop inventory manager written in Java 17 and Swing. Its interface is styled as a physical
stockroom ledger: warm paper, black ink, one orange accent and rubber-stamp status badges. You can add, view,
update and delete items. Stock status is worked out automatically from quantity and threshold. Every change is
saved to a CSV file.

## Team

| Name         | Reg. No. |
| ------------ | -------- |
| Arsal Saeed  | 506676   |
| Laiba Rehman | 505099   |

## Features

**Inventory operations**

- **Add items** with a name, a quantity and an optional low-stock threshold (default 5). IDs are assigned automatically (`#0001`, `#0002`, …) and are never reused, even after a restart.
- **View all items** in a sortable ledger table. An empty inventory shows a clear "LEDGER EMPTY" panel, and **VIEW ALL** reports it as well.
- **Update items** by selecting a row, or by typing any ID into the **Update by ID** form. Unknown IDs and stale selections give a friendly "not found" message instead of an error.
- **Delete items** through a confirmation dialog ("VOID ENTRY #0007?").
- **Search and filter** by name ("FIND IN LEDGER…") and by status (ALL / IN / LOW / OUT).
- **Stock summary report** with **GENERATE REPORT** (or Ctrl+R): saves a print-ready HTML page to
  `reports/stock-report-yyyyMMdd-HHmm.html` and opens it in the browser. It shows totals, the items grouped
  OUT / LOW / IN with unit subtotals, and a reorder list with suggested quantities of `(threshold × 2) − quantity`.

**Stock status** is derived and never stored:

| Condition               | Status       |
| ----------------------- | ------------ |
| quantity = 0            | OUT OF STOCK |
| quantity ≤ threshold    | LOW STOCK    |
| otherwise               | IN STOCK     |

**Validation** happens in the service layer; the GUI only shows the messages, inline under the field and as a toast.

| Input                                      | Message                                                  |
| ------------------------------------------ | -------------------------------------------------------- |
| Blank name                                 | Item name cannot be empty.                               |
| Name over 40 characters                    | Item name must be 40 characters or fewer.                |
| Duplicate name (case-insensitive)          | An item named 'X' already exists.                        |
| Blank quantity                             | Quantity is required.                                    |
| `-5`                                       | Quantity cannot be negative.                             |
| `abc`, `5.5`, `1e3`                        | Quantity must be a whole number (e.g. 25).               |
| Over 1,000,000, or `99999999999` (overflow) | Quantity cannot exceed 1,000,000.                        |
| Threshold                                  | Same rules, range 0–10,000; blank means 5.               |

**Persistence**

- Data is stored in `data/inventory.csv`. It is loaded on startup and saved automatically after every change.
- Commas and quotes in names are escaped, corrupt lines are skipped, and the file is written atomically.
- The status bar shows when the data was last saved, for example `SAVED 23:41 · data/inventory.csv`.

**Keyboard shortcuts**

| Key    | Action                                 |
| ------ | -------------------------------------- |
| Ctrl+N | New entry                              |
| Ctrl+F | Find in ledger                         |
| Ctrl+R | Generate a stock summary report        |
| Del    | Void (delete) the selected entry       |
| Esc    | Clear the Entry Slip                   |
| Enter  | Add / save from the Entry Slip         |

## Screenshots

> Placeholders. Save the screenshots in [`docs/screenshots/`](docs/screenshots/) with the file names below.

| Screen                                  | File                                                 |
| --------------------------------------- | ---------------------------------------------------- |
| Main window with items                  | ![Main window](docs/screenshots/01-main-window.png)          |
| Empty ledger (TC5)                      | ![Empty ledger](docs/screenshots/02-empty-ledger.png)        |
| Item added, with toast (TC1)            | ![Item added](docs/screenshots/03-item-added.png)            |
| Validation errors (TC2, TC3)            | ![Validation errors](docs/screenshots/04-validation-error.png) |
| Editing an item (TC6)                   | ![Editing an item](docs/screenshots/05-edit-mode.png)        |
| Update by ID, not found (TC7)           | ![Item not found](docs/screenshots/06-not-found.png)         |
| Void confirmation dialog (TC8)          | ![Void dialog](docs/screenshots/07-void-dialog.png)          |
| Duplicate name rejected (TC9)           | ![Duplicate name](docs/screenshots/08-duplicate-name.png)    |
| Search and status filter                | ![Search and filter](docs/screenshots/09-search-filter.png)  |

## Requirements

| Requirement | Version          | Notes                                                                               |
| ----------- | ---------------- | ----------------------------------------------------------------------------------- |
| JDK         | 17 or newer      | Must be a JDK, not just a JRE. `JAVA_HOME` must point to it.                        |
| Maven       | none needed      | The bundled Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.9 on first run. |
| Internet    | first build only | Maven downloads JUnit 5 and the build plugins into `~/.m2`.                         |

Everything else is in the repository. The fonts (Space Grotesk and IBM Plex Mono, SIL Open Font License 1.1) are
bundled in `src/main/resources/fonts`, and the app has no runtime dependencies. The build stops with a clear message
if the JDK is older than 17.

## How to Run

Run these commands from the project folder. On Windows use `mvnw` (PowerShell: `.\mvnw`); on macOS, Linux or
Git Bash use `./mvnw`. If Maven is installed you can use `mvn` instead.

```bash
# Run the tests
./mvnw test

# Build the runnable JAR (also runs the tests)
./mvnw package

# Start the application
java -jar target/stockroom-1.0.jar
```

The app creates `data/inventory.csv` in the folder you start it from.

## How to Generate the Javadoc

Every class, field, constructor and method is documented (including private members), and the code passes
`-Xdoclint:all` with zero warnings.

```bash
javadoc -d docs -author -version -private -Xdoclint:all -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 \
  -windowtitle "Stockroom API Documentation" -doctitle "Stockroom — Inventory Management System" \
  -sourcepath src/main/java -subpackages com.stockroom
```

Open `docs/index.html` in a browser. On Windows PowerShell, put the command on one line, without the `\`.

You can also run it through Maven, which writes the docs to `target/site/apidocs`:

```bash
./mvnw javadoc:javadoc
```

## Test Cases

All tests are JUnit 5 tests and run with `./mvnw test`. Each case can also be shown in the GUI.

| TC   | Scenario                          | Expected result                                   | JUnit test                                        | In the GUI                                                     |
| ---- | --------------------------------- | ------------------------------------------------- | ------------------------------------------------- | -------------------------------------------------------------- |
| TC1  | Add a valid item                  | Item added with the next ID and derived status    | `InventoryManagerTest.tc1AddValidItem`            | Fill in the Entry Slip, press **ADD TO LEDGER**                |
| TC2  | Empty name                        | "Item name cannot be empty."                      | `InputValidatorTest.tc2EmptyNameRejected`         | Leave the name blank and press **ADD TO LEDGER**               |
| TC3  | Negative / non-numeric quantity   | "Quantity cannot be negative." / "…whole number…" | `InputValidatorTest.tc3InvalidQuantityRejected`   | Enter `-5` or `abc` as the quantity                            |
| TC4  | View all items                    | Every item is listed                              | `InventoryManagerTest.tc4ViewAllShowsEveryItem`   | Press **VIEW ALL**                                             |
| TC5  | View an empty inventory           | Empty message shown                               | `InventoryManagerTest.tc5ViewEmptyInventory`      | Start with no data, or void every entry, then **VIEW ALL**     |
| TC6  | Update an existing item           | Name, quantity and threshold changed              | `InventoryManagerTest.tc6UpdateExistingItem`      | Select a row, edit it, press **SAVE CHANGES**                  |
| TC7  | Update a non-existing item        | `ItemNotFoundException`, friendly message         | `InventoryManagerTest.tc7UpdateNonExistingItem`   | Type an unknown ID in **Update by ID** and press **LOAD**      |
| TC8  | Delete an existing item           | Item removed                                      | `InventoryManagerTest.tc8DeleteExistingItem`      | Select a row, press **VOID ENTRY**, confirm                    |
| TC9  | Duplicate name                    | "An item named 'X' already exists."               | `InventoryManagerTest.tc9DuplicateNameRejected`   | Add an item whose name already exists (any letter case)        |
| TC10 | Status at the boundaries          | 0 → OUT, ≤ threshold → LOW, else IN               | `StatusDerivationTest.tc10StatusAtBoundaries`     | Set quantity to 0, to the threshold, and above it              |
| TC11 | CSV save/load round-trip          | Same items and next ID after reload               | `InventoryManagerTest.tc11CsvRoundTrip`           | Add items, close the app, reopen it                            |
| TC12 | Delete a non-existing item        | `ItemNotFoundException`, nothing removed          | `InventoryManagerTest.tc12DeleteNonExistingItem`  | Void an entry, then type its ID in **Update by ID** and press **LOAD**: "No item with ID #0007 exists." |

## Project Structure

```text
stockroom/
├── .github/workflows/ci.yml          # GitHub Actions: runs the tests on every push
├── .mvn/wrapper/                     # Maven Wrapper settings
├── docs/                             # Generated Javadoc (index.html) and screenshots/
├── pom.xml                           # Maven build: Java 17, JUnit 5, runnable JAR
├── mvnw, mvnw.cmd                    # Maven Wrapper scripts
└── src/
    ├── main/
    │   ├── java/com/stockroom/
    │   │   ├── model/                # Item, StockStatus
    │   │   ├── exception/            # InventoryException and its subclasses:
    │   │   │                         #   InvalidItemException, ItemNotFoundException, DuplicateItemException
    │   │   ├── service/              # InputValidator, InventoryManager, InventoryListener (Observer)
    │   │   ├── persistence/          # InventoryRepository, CsvInventoryRepository, InventoryData
    │   │   └── ui/                   # Main, InventoryGUI, ItemTableModel
    │   │       ├── theme/            # Theme: colours, fonts, font loading
    │   │       └── components/       # Custom-painted widgets: LedgerTable, StampBadge, LevelBarRenderer,
    │   │                             #   LedgerButton, SlipField, SlipDialog, Toast, EmptyStatePanel, …
    │   └── resources/fonts/          # Space Grotesk, IBM Plex Mono (+ OFL licences)
    └── test/java/com/stockroom/
        ├── model/                    # StatusDerivationTest
        └── service/                  # InputValidatorTest, InventoryManagerTest
```

**Layers**: `ui` → `service` → `persistence` / `model`. Validation and business rules live in `service`, so the GUI
only displays results. `InventoryManager` notifies `InventoryListener`s after every change (Observer pattern), and
the window refreshes itself. Storage sits behind the `InventoryRepository` interface.

## Tech Stack

- Java 17, Swing (no external UI libraries)
- Maven with the Maven Wrapper
- JUnit 5 (test scope only)
- Javadoc
- GitHub Actions
