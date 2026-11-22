package com.stockroom.ui.components;

import com.stockroom.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Labelled text input in the style of a paper entry slip.
 *
 * <p>A SlipField stacks three parts:</p>
 * <ul>
 *   <li>a small-caps mono label;</li>
 *   <li>an underline-only text input. The underline is ink at rest, turns
 *       orange while the field has focus and status red while it shows an
 *       error;</li>
 *   <li>a message line that shows optional helper text in muted grey, or the
 *       current error in red.</li>
 * </ul>
 *
 * <p>The field only displays errors; it never validates. Validation
 * happens in the service layer, and the user interface passes the
 * resulting message to {@link #setError(String)}.</p>
 *
 * @author Arsal Saeed
 * @author Laiba Rehman
 * @version 1.0
 * @since 1.0
 */
public class SlipField extends JPanel {

    /** Vertical gap, in pixels, between the label, the input and the message. */
    private static final int GAP = 4;

    /** Font size, in points, of the label. */
    private static final float LABEL_SIZE = 10.5f;

    /** Font size, in points, of the text typed into the input. */
    private static final float INPUT_SIZE = 15f;

    /** Font size, in points, of the helper or error message. */
    private static final float MESSAGE_SIZE = 11f;

    /** Placeholder text that keeps the message line's height when it is empty. */
    private static final String BLANK = " ";

    /** Small-caps label above the input. */
    private final JLabel label;

    /** The underline-only text input. */
    private final UnderlineTextField field;

    /** Message line below the input, for helper or error text. */
    private final JLabel message;

    /** Helper text shown when there is no error; empty for none. */
    private String helperText;

    /** Current error message, or {@code null} when there is no error. */
    private String errorText;

    /**
     * Creates a slip field with no helper text.
     *
     * @param labelText the label shown above the input; it is shown in uppercase
     */
    public SlipField(String labelText) {
        this(labelText, "");
    }

    /**
     * Creates a slip field with helper text below the input.
     *
     * @param labelText  the label shown above the input; it is shown in uppercase
     * @param helperText hint shown below the input while there is no error
     */
    public SlipField(String labelText, String helperText) {
        super(new BorderLayout(0, GAP));
        setOpaque(false);
        this.helperText = helperText == null ? "" : helperText;

        label = new JLabel(labelText.toUpperCase(Locale.ROOT));
        label.setFont(Theme.tracked(Theme.monoSemiBold(LABEL_SIZE), Theme.TRACKING_LABEL));
        label.setForeground(Theme.MUTED);

        field = new UnderlineTextField();
        label.setLabelFor(field);

        message = new JLabel(BLANK);
        message.setFont(Theme.mono(MESSAGE_SIZE));

        add(label, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        refreshMessage();
    }

    /**
     * Returns the raw text typed into the input, untrimmed. The service
     * layer does the trimming and parsing.
     *
     * @return the input text
     */
    public String getText() {
        return field.getText();
    }

    /**
     * Replaces the text in the input.
     *
     * @param text the new text
     */
    public void setText(String text) {
        field.setText(text);
    }

    /**
     * Shows an error: the underline turns red and the message appears in
     * red below the input, replacing any helper text.
     *
     * @param error the error message; {@code null} or blank clears the error
     */
    public void setError(String error) {
        errorText = (error == null || error.isBlank()) ? null : error;
        field.setError(errorText != null);
        refreshMessage();
    }

    /**
     * Removes any error and brings back the helper text.
     */
    public void clearError() {
        setError(null);
    }

    /**
     * Reports whether the field is currently showing an error.
     *
     * @return {@code true} if an error is shown
     */
    public boolean hasError() {
        return errorText != null;
    }

    /**
     * Returns the error currently shown.
     *
     * @return the error message, or {@code null} if there is none
     */
    public String getErrorText() {
        return errorText;
    }

    /**
     * Changes the helper text shown below the input while there is no error.
     *
     * @param helperText the new helper text; {@code null} or empty for none
     */
    public void setHelperText(String helperText) {
        this.helperText = helperText == null ? "" : helperText;
        refreshMessage();
    }

    /**
     * Returns the underlying text field, for key bindings or document
     * listeners.
     *
     * @return the text input
     */
    public JTextField getTextField() {
        return field;
    }

    /**
     * Moves keyboard focus into the input.
     */
    public void focusField() {
        field.requestFocusInWindow();
    }

    /**
     * Registers a listener that runs when the user presses Enter in the
     * input.
     *
     * @param listener the listener to add
     */
    public void addActionListener(ActionListener listener) {
        field.addActionListener(listener);
    }

    /**
     * Enables or disables the field, greying out the label and input when
     * disabled.
     *
     * @param enabled {@code true} to enable the field
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        label.setForeground(enabled ? Theme.MUTED : Theme.RULE);
    }

    /**
     * Updates the message line to show the error if there is one, the
     * helper text otherwise, or a blank line that keeps the layout steady.
     */
    private void refreshMessage() {
        if (errorText != null) {
            message.setText(errorText);
            message.setForeground(Theme.STATUS_OUT);
        } else {
            message.setText(helperText.isEmpty() ? BLANK : helperText);
            message.setForeground(Theme.MUTED);
        }
    }

    /**
     * Transparent text field that draws only an underline. The underline is
     * ink at rest, orange with focus and red when an error is shown.
     *
     * @author Arsal Saeed
     * @author Laiba Rehman
     * @version 1.0
     * @since 1.0
     */
    private static final class UnderlineTextField extends JTextField {

        /** Thickness, in pixels, of the underline at rest. */
        private static final int LINE_REST = 1;

        /** Thickness, in pixels, of the underline with focus or an error. */
        private static final int LINE_ACTIVE = 2;

        /** Whether the field is showing an error. */
        private boolean error;

        /**
         * Creates the underline text field with ledger fonts and colours.
         */
        private UnderlineTextField() {
            setOpaque(false);
            setBorder(new EmptyBorder(4, 0, 8, 0));
            setFont(Theme.mono(INPUT_SIZE));
            setForeground(Theme.INK);
            setCaretColor(Theme.INK);
            setSelectionColor(Theme.ACCENT_TINT);
            setSelectedTextColor(Theme.INK);
            setDisabledTextColor(Theme.MUTED);
        }

        /**
         * Switches the error underline on or off.
         *
         * @param error {@code true} to show the red error underline
         */
        private void setError(boolean error) {
            this.error = error;
            repaint();
        }

        /**
         * Handles focus changes as usual, then repaints so the underline
         * colour follows the focus.
         *
         * @param e the focus event
         */
        @Override
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);
            repaint();
        }

        /**
         * Paints the text, then the underline in the colour for the current
         * state.
         *
         * @param g the graphics context to paint on
         */
        @Override
        protected void paintComponent(Graphics g) {
            Theme.applyRenderingHints(g);
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                boolean focused = isFocusOwner();
                if (error) {
                    g2.setColor(Theme.STATUS_OUT);
                } else if (focused) {
                    g2.setColor(Theme.ACCENT);
                } else {
                    g2.setColor(isEnabled() ? Theme.INK : Theme.RULE);
                }
                int thickness = (error || focused) ? LINE_ACTIVE : LINE_REST;
                g2.fillRect(0, getHeight() - thickness, getWidth(), thickness);
            } finally {
                g2.dispose();
            }
        }
    }
}
