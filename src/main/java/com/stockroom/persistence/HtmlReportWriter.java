package com.stockroom.persistence;

import com.stockroom.model.StockReport;
import com.stockroom.model.StockStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Writes a {@link StockReport} as one self-contained, print-friendly HTML
 * page, {@code reports/stock-report-yyyyMMdd-HHmm.html} by default.
 *
 * <p>The page follows the Stockroom Ledger look: paper background, black
 * ink, ruled tables, monospaced numerals and status-coloured section
 * headings. All styles are inline in the page, so it needs no other files
 * and no network access. When printed, each section starts on a new
 * page.</p>
 *
 * <p>Item names are HTML-escaped, so a name such as
 * {@code <b>Bolts & Nuts</b>} is shown as typed and never treated as
 * markup.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class HtmlReportWriter {

    /** Default folder for reports, relative to the working directory. */
    public static final Path DEFAULT_DIRECTORY = Path.of("reports");

    /** Start of every report file name. */
    private static final String FILE_PREFIX = "stock-report-";

    /** End of every report file name. */
    private static final String FILE_SUFFIX = ".html";

    /** Time stamp part of the file name, for example "20261123-1405". */
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ROOT);

    /** Date in the page header, for example "23 NOV 2026". */
    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    /** Time in the page header, for example "14:05". */
    private static final DateTimeFormatter HEADER_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    /** Title shown at the top of the page and in the browser tab. */
    private static final String REPORT_TITLE = "STOCKROOM · STOCK SUMMARY REPORT";

    /**
     * Inline style sheet of the page: the Stockroom Ledger colours and
     * fonts, ruled tables, and page breaks before each section in print.
     */
    private static final String STYLES = """
            :root {
              --paper: #F3EEE3; --surface: #FBF8F1; --ink: #1B1A17; --rule: #D9D0BE;
              --muted: #7A7263; --accent: #E8590C;
              --in: #2F6B3A; --low: #B7791F; --out: #A8322D;
              --mono: "IBM Plex Mono", ui-monospace, "Cascadia Mono", Consolas, "Courier New", monospace;
              --display: "Space Grotesk", "Segoe UI", "Helvetica Neue", Arial, sans-serif;
            }
            * { box-sizing: border-box; }
            html { background: var(--paper); }
            body {
              margin: 0 auto; max-width: 960px; padding: 40px 32px 56px;
              background: var(--paper); color: var(--ink);
              font-family: var(--mono); font-size: 13px; line-height: 1.45;
              -webkit-print-color-adjust: exact; print-color-adjust: exact;
            }
            header { border-bottom: 2px solid var(--ink); padding-bottom: 14px; }
            h1 {
              margin: 0; font-family: var(--display); font-weight: 700;
              font-size: 28px; letter-spacing: -0.02em;
            }
            .stamp {
              margin-top: 6px; color: var(--muted); font-size: 11px; letter-spacing: 0.12em;
            }
            .totals {
              display: grid; grid-template-columns: repeat(5, 1fr);
              border-bottom: 1px solid var(--rule); margin-bottom: 8px;
            }
            .totals div { padding: 16px 16px 14px; border-left: 1px solid var(--rule); }
            .totals div:first-child { border-left: none; padding-left: 0; }
            .caption { display: block; color: var(--muted); font-size: 10px; font-weight: 600; letter-spacing: 0.12em; }
            .figure { display: block; font-size: 28px; font-weight: 500; font-variant-numeric: tabular-nums; }
            .share { display: block; color: var(--muted); font-size: 11px; }
            section { margin-top: 36px; }
            h2 {
              margin: 0 0 10px; padding-left: 10px; border-left: 6px solid currentColor;
              font-family: var(--display); font-size: 18px; font-weight: 700; letter-spacing: 0.02em;
            }
            h2 .count { font-family: var(--mono); font-size: 12px; font-weight: 500; color: var(--muted); margin-left: 8px; }
            .status-out { color: var(--out); }
            .status-low { color: var(--low); }
            .status-in { color: var(--in); }
            .status-reorder { color: var(--accent); }
            table { width: 100%; border-collapse: collapse; }
            thead { display: table-header-group; }
            th {
              text-align: left; color: var(--muted); font-size: 10px; font-weight: 600;
              letter-spacing: 0.12em; padding: 6px 8px; border-bottom: 2px solid var(--ink);
            }
            td { padding: 8px; border-bottom: 1px solid var(--rule); }
            tr { break-inside: avoid; page-break-inside: avoid; }
            .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
            .id { color: var(--muted); white-space: nowrap; }
            .empty td { color: var(--muted); text-align: center; letter-spacing: 0.12em; }
            tfoot td { border-top: 2px solid var(--ink); border-bottom: none; font-weight: 600; }
            .badge {
              display: inline-block; padding: 1px 6px; border: 1.5px solid currentColor;
              font-size: 10px; font-weight: 600; letter-spacing: 0.08em;
            }
            footer {
              margin-top: 40px; padding-top: 10px; border-top: 1px dashed var(--rule);
              color: var(--muted); font-size: 11px;
            }
            @page { margin: 16mm; }
            @media print {
              body { max-width: none; padding: 0; }
              section { margin-top: 0; break-before: page; page-break-before: always; }
            }
            """;

    /** Folder the report files are written to. */
    private final Path directory;

    /**
     * Creates a writer that saves reports in {@link #DEFAULT_DIRECTORY}.
     */
    public HtmlReportWriter() {
        this(DEFAULT_DIRECTORY);
    }

    /**
     * Creates a writer that saves reports in the given folder. The folder is
     * created on the first write if it does not exist.
     *
     * @param directory the folder to write reports to
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public HtmlReportWriter(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    /**
     * Writes a report to {@code stock-report-yyyyMMdd-HHmm.html} in the
     * report folder, named after the report's time stamp. A report from the
     * same minute is replaced.
     *
     * @param report the report to write
     * @return the path of the written file
     * @throws IOException          if the folder or file cannot be written
     * @throws NullPointerException if {@code report} is {@code null}
     */
    public Path write(StockReport report) throws IOException {
        Objects.requireNonNull(report, "report");
        Files.createDirectories(directory);
        Path file = directory.resolve(fileNameFor(report));
        Files.writeString(file, render(report), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Returns the file name used for a report.
     *
     * @param report the report
     * @return a name such as {@code stock-report-20261123-1405.html}
     */
    public static String fileNameFor(StockReport report) {
        return FILE_PREFIX + report.generatedAt().format(FILE_STAMP) + FILE_SUFFIX;
    }

    /**
     * Renders a report as a complete HTML page.
     *
     * @param report the report to render
     * @return the HTML source of the page
     */
    public static String render(StockReport report) {
        String date = report.generatedAt().format(HEADER_DATE).toUpperCase(Locale.ROOT);
        String time = report.generatedAt().format(HEADER_TIME);

        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(REPORT_TITLE).append(" · ").append(date).append("</title>\n")
                .append("<style>\n").append(STYLES).append("</style>\n</head>\n<body>\n");

        html.append("<header>\n<h1>").append(REPORT_TITLE).append("</h1>\n")
                .append("<div class=\"stamp\">GENERATED ").append(date).append(" · ").append(time)
                .append("</div>\n</header>\n");

        appendTotals(html, report.totals());
        for (StockReport.Section section : report.sections()) {
            appendSection(html, section);
        }
        appendReorderList(html, report.reorderList());

        html.append("<footer>Suggested reorder = (threshold × 2) − quantity · ")
                .append(report.totals().itemCount()).append(" entries · END OF REPORT</footer>\n")
                .append("</body>\n</html>\n");
        return html.toString();
    }

    /**
     * Appends the totals strip: items, units, and the IN / LOW / OUT counts
     * with their percentages.
     *
     * @param html   the page being built
     * @param totals the overall totals
     */
    private static void appendTotals(StringBuilder html, StockReport.Totals totals) {
        html.append("<div class=\"totals\">\n");
        appendFigure(html, "ITEMS", formatNumber(totals.itemCount()), null, "");
        appendFigure(html, "TOTAL UNITS", formatNumber(totals.totalUnits()), null, "");
        appendFigure(html, "IN STOCK", formatNumber(totals.inStock()),
                formatPercent(totals.percent(StockStatus.IN_STOCK)), cssClass(StockStatus.IN_STOCK));
        appendFigure(html, "LOW STOCK", formatNumber(totals.lowStock()),
                formatPercent(totals.percent(StockStatus.LOW_STOCK)), cssClass(StockStatus.LOW_STOCK));
        appendFigure(html, "OUT OF STOCK", formatNumber(totals.outOfStock()),
                formatPercent(totals.percent(StockStatus.OUT_OF_STOCK)), cssClass(StockStatus.OUT_OF_STOCK));
        html.append("</div>\n");
    }

    /**
     * Appends one cell of the totals strip.
     *
     * @param html       the page being built
     * @param caption    small caption above the figure
     * @param figure     the large figure
     * @param share      percentage shown under the figure, or {@code null} for none
     * @param colorClass CSS class that colours the figure, or empty for ink
     */
    private static void appendFigure(StringBuilder html, String caption, String figure,
                                     String share, String colorClass) {
        html.append("<div><span class=\"caption\">").append(caption).append("</span>")
                .append("<span class=\"figure ").append(colorClass).append("\">").append(figure).append("</span>");
        if (share != null) {
            html.append("<span class=\"share\">").append(share).append(" of items</span>");
        }
        html.append("</div>\n");
    }

    /**
     * Appends one status section: a coloured heading, the item rows and a
     * subtotal of units. An empty section says so in a single row.
     *
     * @param html    the page being built
     * @param section the section to render
     */
    private static void appendSection(StringBuilder html, StockReport.Section section) {
        StockStatus status = section.status();
        html.append("<section>\n<h2 class=\"").append(cssClass(status)).append("\">")
                .append(status.getDisplayLabel())
                .append("<span class=\"count\">").append(section.rows().size())
                .append(section.rows().size() == 1 ? " ENTRY" : " ENTRIES").append("</span></h2>\n")
                .append("<table>\n<thead><tr><th>ID</th><th>ITEM</th>")
                .append("<th class=\"num\">QTY</th><th class=\"num\">THRESHOLD</th></tr></thead>\n<tbody>\n");
        if (section.rows().isEmpty()) {
            html.append("<tr class=\"empty\"><td colspan=\"4\">— NO ENTRIES —</td></tr>\n");
        }
        for (StockReport.Row row : section.rows()) {
            html.append("<tr><td class=\"id\">").append(formatId(row.id())).append("</td>")
                    .append("<td>").append(escapeHtml(row.name())).append("</td>")
                    .append("<td class=\"num\">").append(formatNumber(row.quantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatNumber(row.threshold())).append("</td></tr>\n");
        }
        html.append("</tbody>\n<tfoot><tr><td colspan=\"2\">SUBTOTAL</td>")
                .append("<td class=\"num\">").append(formatNumber(section.subtotalUnits())).append("</td>")
                .append("<td class=\"num\">units</td></tr></tfoot>\n</table>\n</section>\n");
    }

    /**
     * Appends the reorder list: every low or out-of-stock item with its
     * suggested reorder quantity, and the total units to reorder.
     *
     * @param html  the page being built
     * @param lines the reorder lines, most urgent first
     */
    private static void appendReorderList(StringBuilder html, List<StockReport.ReorderLine> lines) {
        html.append("<section>\n<h2 class=\"status-reorder\">REORDER LIST")
                .append("<span class=\"count\">").append(lines.size())
                .append(lines.size() == 1 ? " ENTRY" : " ENTRIES").append("</span></h2>\n")
                .append("<table>\n<thead><tr><th>ID</th><th>ITEM</th><th>STATUS</th>")
                .append("<th class=\"num\">QTY</th><th class=\"num\">THRESHOLD</th>")
                .append("<th class=\"num\">SUGGESTED REORDER</th></tr></thead>\n<tbody>\n");
        long totalSuggested = 0;
        if (lines.isEmpty()) {
            html.append("<tr class=\"empty\"><td colspan=\"6\">— NOTHING TO REORDER —</td></tr>\n");
        }
        for (StockReport.ReorderLine line : lines) {
            totalSuggested += line.suggestedQuantity();
            html.append("<tr><td class=\"id\">").append(formatId(line.id())).append("</td>")
                    .append("<td>").append(escapeHtml(line.name())).append("</td>")
                    .append("<td><span class=\"badge ").append(cssClass(line.status())).append("\">")
                    .append(line.status().getDisplayLabel()).append("</span></td>")
                    .append("<td class=\"num\">").append(formatNumber(line.quantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatNumber(line.threshold())).append("</td>")
                    .append("<td class=\"num\">").append(formatNumber(line.suggestedQuantity())).append("</td></tr>\n");
        }
        html.append("</tbody>\n<tfoot><tr><td colspan=\"5\">TOTAL TO REORDER</td>")
                .append("<td class=\"num\">").append(formatNumber(totalSuggested)).append("</td></tr></tfoot>\n")
                .append("</table>\n</section>\n");
    }

    /**
     * Escapes the characters that have a meaning in HTML, so text is shown
     * exactly as typed.
     *
     * @param text the text to escape; {@code null} is treated as empty
     * @return the text with {@code & < > " '} replaced by entities
     */
    static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    /**
     * Returns the CSS class that gives a status its colour.
     *
     * @param status the status
     * @return {@code status-in}, {@code status-low} or {@code status-out}
     */
    private static String cssClass(StockStatus status) {
        return switch (status) {
            case IN_STOCK -> "status-in";
            case LOW_STOCK -> "status-low";
            case OUT_OF_STOCK -> "status-out";
        };
    }

    /**
     * Formats an ID the way the ledger shows it.
     *
     * @param id the item ID
     * @return the ID with a hash and zero padding, for example "#0007"
     */
    private static String formatId(int id) {
        return String.format(Locale.ROOT, "#%04d", id);
    }

    /**
     * Formats a whole number with thousands separators.
     *
     * @param value the number
     * @return text such as "1,250"
     */
    private static String formatNumber(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Formats a percentage with one decimal place.
     *
     * @param percent a value from 0 to 100
     * @return text such as "33.3%"
     */
    private static String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
}
