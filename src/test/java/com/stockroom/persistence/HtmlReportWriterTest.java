package com.stockroom.persistence;

import com.stockroom.model.StockReport;
import com.stockroom.model.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HtmlReportWriter}: HTML escaping of item names, the
 * file name and location, and the page content.
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
class HtmlReportWriterTest {

    /** Time stamp of the sample report. */
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 23, 14, 5);

    /** Item name containing every character that must be escaped. */
    private static final String NASTY_NAME = "<script>alert('x')</script> & \"Co\"";

    /** Temporary folder for written reports, created fresh for each test. */
    @TempDir
    Path tempDir;

    /**
     * Creates the test class; JUnit calls this before each test.
     */
    HtmlReportWriterTest() {
    }

    /**
     * Builds a small report with one low-stock item whose name contains
     * HTML, and one in-stock item.
     *
     * @return the sample report
     */
    private static StockReport sampleReport() {
        StockReport.Row low = new StockReport.Row(7, NASTY_NAME, 3, 5);
        StockReport.Row in = new StockReport.Row(2, "Washers", 1250, 10);
        return new StockReport(TIME,
                new StockReport.Totals(2, 1253, 1, 1, 0),
                List.of(new StockReport.Section(StockStatus.OUT_OF_STOCK, List.of(), 0),
                        new StockReport.Section(StockStatus.LOW_STOCK, List.of(low), 3),
                        new StockReport.Section(StockStatus.IN_STOCK, List.of(in), 1250)),
                List.of(new StockReport.ReorderLine(7, NASTY_NAME, StockStatus.LOW_STOCK, 3, 5, 7)));
    }

    /**
     * Special characters are replaced by entities.
     */
    @Test
    @DisplayName("escapeHtml replaces & < > \" '")
    void escapeHtmlReplacesSpecialCharacters() {
        assertEquals("&lt;b&gt;Bolts &amp; Nuts&lt;/b&gt; &quot;6&quot;&#39;",
                HtmlReportWriter.escapeHtml("<b>Bolts & Nuts</b> \"6\"'"));
        assertEquals("Plain name", HtmlReportWriter.escapeHtml("Plain name"));
        assertEquals("", HtmlReportWriter.escapeHtml(null));
    }

    /**
     * Item names never reach the page as raw markup, in the sections or the
     * reorder list.
     */
    @Test
    @DisplayName("Item names are escaped in the rendered page")
    void renderEscapesItemNames() {
        String html = HtmlReportWriter.render(sampleReport());

        assertFalse(html.contains("<script>"));
        String escaped = "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;Co&quot;";
        assertEquals(2, html.split(Pattern.quote(escaped), -1).length - 1);
    }

    /**
     * The report is written as {@code stock-report-yyyyMMdd-HHmm.html} in the
     * given folder, created if missing, and holds the expected content.
     *
     * @throws IOException if the file cannot be written or read
     */
    @Test
    @DisplayName("Report is written to the reports folder")
    void writeCreatesFile() throws IOException {
        Path folder = tempDir.resolve("reports");
        Path file = new HtmlReportWriter(folder).write(sampleReport());

        assertEquals(folder.resolve("stock-report-20261123-1405.html"), file);
        assertTrue(Files.isRegularFile(file));

        String html = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("STOCKROOM · STOCK SUMMARY REPORT"));
        assertTrue(html.contains("23 NOV 2026 · 14:05"));
        assertTrue(html.contains("#F3EEE3"));
        assertTrue(html.contains("page-break-before: always"));
        assertTrue(html.contains("1,250"));
        assertTrue(html.contains("50.0%"));
        assertTrue(html.contains("#0007"));
        assertTrue(html.contains("— NO ENTRIES —"));
        assertTrue(html.indexOf(">OUT OF STOCK<span") < html.indexOf(">LOW STOCK<span"));
        assertTrue(html.indexOf(">LOW STOCK<span") < html.indexOf(">IN STOCK<span"));
    }
}
