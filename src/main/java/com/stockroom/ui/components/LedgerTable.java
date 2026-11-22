package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.TableModel;

/**
 * Ledger table that can briefly flash a row orange, used to point out a
 * newly added entry.
 *
 * <p>The flash is a translucent orange wash over the whole row that fades
 * out over a little under a second. It is painted on top of the normal
 * cells, so the cell renderers need no changes. The flashed row is tracked
 * by its model index, so it stays on the right entry even if the table is
 * sorted or filtered while it fades.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class LedgerTable extends JTable {

    /** Length, in milliseconds, of the flash. */
    private static final long FLASH_MS = 900;

    /** Delay, in milliseconds, between flash animation frames. */
    private static final int FRAME_MS = 30;

    /** Strongest opacity of the orange wash, at the start of the flash. */
    private static final float PEAK_ALPHA = 0.32f;

    /** Timer that drives the fade. */
    private final Timer flashTimer;

    /** Model index of the row being flashed, or -1 for none. */
    private int flashModelRow = -1;

    /** {@link System#nanoTime()} reading taken when the flash started. */
    private long flashStart;

    /** Current opacity of the orange wash. */
    private float flashAlpha;

    /**
     * Creates a ledger table for a model.
     *
     * @param model the table model
     */
    public LedgerTable(TableModel model) {
        super(model);
        flashTimer = new Timer(FRAME_MS, this::onFlashTick);
    }

    /**
     * Flashes a row orange and scrolls it into view.
     *
     * @param viewRow the row's index in the view; out-of-range values are ignored
     */
    public void flashRow(int viewRow) {
        if (viewRow < 0 || viewRow >= getRowCount()) {
            return;
        }
        flashModelRow = convertRowIndexToModel(viewRow);
        flashStart = System.nanoTime();
        flashAlpha = PEAK_ALPHA;
        scrollRectToVisible(getCellRect(viewRow, 0, true));
        flashTimer.restart();
        repaint();
    }

    /**
     * Advances the fade by one frame; called by the flash timer.
     *
     * @param e the timer event (not used)
     */
    private void onFlashTick(ActionEvent e) {
        double t = (System.nanoTime() - flashStart) / 1_000_000.0 / FLASH_MS;
        if (t >= 1) {
            flashTimer.stop();
            flashModelRow = -1;
            flashAlpha = 0f;
        } else {
            flashAlpha = (float) (PEAK_ALPHA * (1 - t) * (1 - t));
        }
        repaint();
    }

    /**
     * Paints the table, then the orange wash over the flashed row.
     *
     * @param g the graphics context to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (flashModelRow < 0 || flashModelRow >= getModel().getRowCount()) {
            return;
        }
        int viewRow = convertRowIndexToView(flashModelRow);
        if (viewRow < 0) {
            return;
        }
        Rectangle row = getCellRect(viewRow, 0, true);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(flashAlpha));
            g2.setColor(Theme.ACCENT);
            g2.fillRect(0, row.y, getWidth(), row.height);
        } finally {
            g2.dispose();
        }
    }
}
