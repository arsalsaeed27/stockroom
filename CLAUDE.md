# Stockroom — Inventory Management System (Software Construction OEL)

## Context

University Open-Ended Lab, group of 2: Arsal Saeed (506676) and Laiba Rehman (505099).
Title: "Java-Based Inventory Management System Using Modern Software Construction Tools".
Graded on: working features, clean OOP, Git history, complete Javadoc, and a report with GUI screenshots.
Deadline is tight. Build exactly the step you are asked for, nothing more.
s

## HARD RULES

- NEVER run git commands (no init, add, commit, push, branch). The user does all git manually.
- Java 17, Maven, pure Swing only. No runtime dependencies. JUnit 5 is the only (test-scoped) dependency.
- After each step, run `mvn -q compile` (and `mvn -q test` once tests exist). Fix all errors, then list the files you created or changed.
- Base package: `com.stockroom`. Sub-packages: `model`, `exception`, `service`, `persistence`, `ui`, `ui.theme`, `ui.components`.

## Functional requirements (ALL mandatory)

Each item has: auto-generated int ID, Name, Quantity, Low-stock Threshold, Stock Status.

1. Add item (name + quantity + optional threshold, default 5).
2. View all items. Empty inventory shows a clear message, both in an empty-state panel and when "View All" is clicked.
3. Update an existing item's name, quantity and threshold. Updating a non-existing item is handled gracefully
   (ItemNotFoundException, plus a friendly UI message). The UI MUST give a reachable path for this:
   an "Update by ID" field where the user can type any ID, and stale selections are also handled.
4. Delete an existing item, with a confirmation dialog.
5. Stock status: IN_STOCK / LOW_STOCK / OUT_OF_STOCK. It is DERIVED, never stored separately:
   quantity == 0 -> OUT_OF_STOCK; quantity <= threshold -> LOW_STOCK; else IN_STOCK.
   The user manages status through quantity and threshold. The form shows this rule as helper text.
6. Validation (lives in the service layer, NOT the GUI; the GUI only displays messages):
   - Name: trimmed; blank -> "Item name cannot be empty."; over 40 chars -> "Item name must be 40 characters or fewer.";
     a case-insensitive duplicate -> "An item named 'X' already exists." (renaming an item to its own name is allowed).
   - Quantity is entered as TEXT (JTextField, never a spinner) so non-numeric input is possible:
     blank -> "Quantity is required."; "-5" -> "Quantity cannot be negative.";
     "abc", "5.5", "1e3" -> "Quantity must be a whole number (e.g. 25).";
     over 1,000,000, or int overflow such as "99999999999" -> "Quantity cannot exceed 1,000,000."
   - Threshold: same rules, range 0–10,000, blank means the default of 5.

## Mandatory test cases (each needs a JUnit test AND must be demonstrable in the GUI)

TC1 add valid item -> added | TC2 empty name -> validation message | TC3 negative / non-numeric qty -> error message
TC4 view all -> all shown | TC5 view empty -> message | TC6 update existing -> updated
TC7 update non-existing -> handled gracefully | TC8 delete existing -> removed
Extra tests: TC9 duplicate name rejected, TC10 status derivation at the boundaries, TC11 CSV save/load round-trip,
TC12 delete non-existing handled.

## Architecture

- model: `StockStatus` (enum with a display label), `Item` (id, name, quantity, threshold; getStatus() derives the status).
- exception: `InventoryException` (base, checked), `InvalidItemException`, `ItemNotFoundException`, `DuplicateItemException`.
- service: `InputValidator` (parses and validates raw strings), `InventoryManager` (LinkedHashMap<Integer, Item> keyed by ID,
  auto-increment IDs, add/getAll/findById/getById/searchByName/update/delete/countByStatus/isEmpty/size,
  Observer pattern via `InventoryListener` so the UI refreshes automatically).
- persistence: `InventoryRepository` (interface), `CsvInventoryRepository` (data/inventory.csv, escapes commas and quotes,
  skips corrupt lines, auto-saves after every change, loads on startup).
- ui: `Main`, `InventoryGUI` (main JFrame), `ItemTableModel` (AbstractTableModel).
- ui.theme: `Theme` (all colours and fonts as constants, plus font loading).
- ui.components: custom-painted components (StampBadge renderer, LedgerButton, SlipField, Toast, SlipDialog, LevelBarRenderer, EmptyStatePanel).

## Javadoc rules (graded — zero exceptions)

- EVERY class, interface, enum, enum constant, constructor, method (including private and GUI handlers) and field gets `/** ... */`.
- Every class/interface/enum: description + `@author Arsal Saeed` + `@author Laiba Rehman` + `@version 1.0` + `@since 1.0`.
- Every method/constructor: `@param` for each parameter, `@return` for non-void, `@throws` for each exception.
- No anonymous classes or lambdas holding logic for event handlers: use method references to documented private methods.
- `src/main/java/com/stockroom/package-info.java` plus a package-info.java in EVERY sub-package, each with a description.
- The code must pass `javadoc -Xdoclint:all -private` with zero warnings.

## GUI design system — "Stockroom Ledger" (MUST NOT look like a generic AI or Bootstrap app)

Concept: a physical stockroom ledger or shipping manifest. Warm paper, black ink, one signal-orange accent, rubber stamps.

- Colours: paper bg #F3EEE3, surface #FBF8F1, ink #1B1A17, rules/borders #D9D0BE, muted text #7A7263,
  accent (one only) #E8590C. Status: in #2F6B3A, low #B7791F, out #A8322D.
- Fonts: bundle OFL fonts in src/main/resources/fonts (Space Grotesk for headings, IBM Plex Mono for data/labels/numbers),
  downloaded from the google/fonts GitHub repo. Register them via GraphicsEnvironment and fall back to SansSerif/Monospaced.
  Turn on text antialiasing.
- FORBIDDEN: rounded corners, gradients, drop shadows, dark mode, purple/blue palettes, emoji or icon-font icons,
  default Metal/Nimbus look, raw JOptionPane dialogs, sidebars full of cards.
- Layout (min 1100x700, default 1280x800):
  - Top "manifest header": big "STOCKROOM" wordmark (Space Grotesk, tight letter-spacing) + small mono line
    "INVENTORY MANIFEST · NO. 0001 · <today's date in caps>". On the right, three live counters (TOTAL / LOW / OUT)
    as large mono numerals separated by thin vertical rules; LOW and OUT numerals use their status colour when > 0.
    A 2px ink rule below the header.
  - Left (about 68%): the ledger table. Columns: No. (ledger line number), ID (mono, zero-padded "#0007"), Item,
    Qty (right-aligned mono + a thin level bar showing qty vs threshold), Threshold, Status.
    Horizontal ruled lines only, no vertical grid, no zebra stripes; row height 40; header in small caps mono, muted, with letter-spacing;
    selected row = surface colour with a 3px orange bar on the left edge.
    Status cell = rubber-stamp badge: uppercase mono, letter-spaced, 1.5px double border in the status colour, rotated −3°,
    slightly transparent ink.
    Above the table: a search field ("FIND IN LEDGER…") and a status filter (ALL / IN / LOW / OUT) styled as a segmented square toggle.
    Column sorting enabled.
  - Right (about 32%): the "Entry Slip" panel. Surface colour, perforated dashed top edge, title "ENTRY SLIP" + mode tag
    (NEW ENTRY / EDITING #0007). Fields are SlipFields: label in small caps mono above, underline-only input, underline
    turns orange on focus and status-red on error, with the inline error text in red below the field.
    Fields: Item name, Quantity, Low-stock threshold (helper: "Status → LOW when qty ≤ threshold, OUT at 0").
    Buttons: primary LedgerButton (solid ink, paper text, square, orange on hover) "ADD TO LEDGER" / "SAVE CHANGES";
    secondary outlined "CLEAR"; destructive outlined red "VOID ENTRY" (delete).
    Below a dashed divider: an "Update by ID" mini-form (ID field + "LOAD") that loads an item for editing or shows a not-found message.
    A "VIEW ALL" button clears search and filters and shows all items, or shows the empty message if there are none.
  - Empty state: a centred dashed-border box: "LEDGER EMPTY" (Space Grotesk, large) / "No entries on file. Press Ctrl+N to add your first item." (mono, muted).
  - Feedback: success = Toast styled as a printed receipt slip that slides up from the bottom-right, auto-hides after about 2.5s
    ("✓ ENTRY #0007 ADDED"). Errors = inline under the field + an error toast with a red edge.
    Confirm delete = custom undecorated SlipDialog ("VOID ENTRY #0007?" / item name / CANCEL + VOID).
  - Bottom status bar (mono, muted, small): keyboard hints "Ctrl+N new · Ctrl+F find · Del void · Esc clear" on the left,
    "SAVED 23:41 · data/inventory.csv" on the right.
- Small motion only: toast slide/fade via javax.swing.Timer, a hover colour change on buttons, a quick orange flash on a newly added row.
