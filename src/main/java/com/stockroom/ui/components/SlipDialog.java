package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Undecorated, modal confirmation dialog styled as a paper slip. It is used
 * instead of a plain {@code JOptionPane}, for example to confirm
 * "VOID ENTRY #0007?".
 *
 * <p>The slip is a framed {@link SlipPanel}: a surface background, a 2px ink
 * frame and a perforated dashed edge at the top. It shows a small "CONFIRMATION SLIP" tag, a
 * Space Grotesk title, a muted detail line and two buttons: CANCEL and the
 * confirm action. Escape and Enter both cancel, so a stray key press never
 * deletes anything.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public final class SlipDialog extends JDialog {

    /** Smallest width, in pixels, of the dialog. */
    private static final int MIN_WIDTH = 440;

    /** Padding, in pixels, inside the slip frame. */
    private static final int PAD = 28;

    /** Small tag shown above the title. */
    private static final String TAG = "CONFIRMATION SLIP";

    /** Label of the cancel button. */
    private static final String CANCEL_LABEL = "CANCEL";

    /** Whether the user chose the confirm action. */
    private boolean confirmed;

    /**
     * Builds the dialog. Use {@link #confirm(Component, String, String, String)}
     * instead of calling this directly.
     *
     * @param owner          window the dialog belongs to, or {@code null}
     * @param title          main question, for example {@code "VOID ENTRY #0007?"}
     * @param detail         supporting line, for example the item name
     * @param confirmLabel   label of the confirm button, for example {@code "VOID"}
     * @param confirmVariant visual variant of the confirm button
     */
    private SlipDialog(Window owner, String title, String detail, String confirmLabel,
                       LedgerButton.Variant confirmVariant) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setTitle(title);

        JLabel tagLabel = new JLabel(TAG);
        tagLabel.setFont(Theme.tracked(Theme.monoSemiBold(10f), Theme.TRACKING_LABEL));
        tagLabel.setForeground(Theme.MUTED);

        JLabel titleLabel = new JLabel(title.toUpperCase(Locale.ROOT));
        titleLabel.setFont(Theme.tracked(Theme.display(26f), Theme.TRACKING_TIGHT));
        titleLabel.setForeground(Theme.INK);

        JLabel detailLabel = new JLabel(detail == null ? " " : detail);
        detailLabel.setFont(Theme.mono(13f));
        detailLabel.setForeground(Theme.MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(tagLabel);
        text.add(Box.createVerticalStrut(10));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(6));
        text.add(detailLabel);

        LedgerButton cancelButton = new LedgerButton(CANCEL_LABEL, LedgerButton.Variant.SECONDARY);
        cancelButton.addActionListener(this::onCancel);
        LedgerButton confirmButton = new LedgerButton(confirmLabel, confirmVariant);
        confirmButton.addActionListener(this::onConfirm);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(confirmButton);

        SlipPanel slip = new SlipPanel(true);
        slip.setLayout(new BorderLayout(0, 26));
        slip.setBorder(new EmptyBorder(PAD + SlipPanel.PERFORATION_CLEARANCE - 8, PAD, PAD, PAD));
        slip.add(text, BorderLayout.CENTER);
        slip.add(buttons, BorderLayout.SOUTH);
        setContentPane(slip);

        getRootPane().setDefaultButton(cancelButton);
        getRootPane().registerKeyboardAction(this::onCancel,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setSize(Math.max(MIN_WIDTH, getWidth()), getHeight());
        setLocationRelativeTo(owner);
    }

    /**
     * Shows a destructive confirmation slip and waits for the user's
     * answer.
     *
     * @param parent       any component in the owning window, or {@code null}
     * @param title        main question, for example {@code "VOID ENTRY #0007?"}
     * @param detail       supporting line, for example the item name
     * @param confirmLabel label of the confirm button, for example {@code "VOID"}
     * @return {@code true} if the user confirmed, {@code false} if they cancelled
     */
    public static boolean confirm(Component parent, String title, String detail, String confirmLabel) {
        return confirm(parent, title, detail, confirmLabel, LedgerButton.Variant.DESTRUCTIVE);
    }

    /**
     * Shows a confirmation slip with a chosen confirm-button style and waits
     * for the user's answer.
     *
     * @param parent         any component in the owning window, or {@code null}
     * @param title          main question
     * @param detail         supporting line
     * @param confirmLabel   label of the confirm button
     * @param confirmVariant visual variant of the confirm button
     * @return {@code true} if the user confirmed, {@code false} if they cancelled
     */
    public static boolean confirm(Component parent, String title, String detail, String confirmLabel,
                                  LedgerButton.Variant confirmVariant) {
        Window owner = null;
        if (parent instanceof Window window) {
            owner = window;
        } else if (parent != null) {
            owner = SwingUtilities.getWindowAncestor(parent);
        }
        SlipDialog dialog = new SlipDialog(owner, title, detail, confirmLabel, confirmVariant);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    /**
     * Records a confirmation and closes the dialog.
     *
     * @param e the button event (not used)
     */
    private void onConfirm(ActionEvent e) {
        confirmed = true;
        dispose();
    }

    /**
     * Records a cancellation and closes the dialog. Runs for the CANCEL
     * button, the Escape key and the Enter key.
     *
     * @param e the button or key event (not used)
     */
    private void onCancel(ActionEvent e) {
        confirmed = false;
        dispose();
    }

}
