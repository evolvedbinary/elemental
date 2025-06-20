/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.client.security;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Provides Auto Completion for JComboBox's.
 * See {@link #enable(JComboBox)}.
 *
 * The original code for this class was Public Domain
 * code by Thomas Bierhance, and was downloaded
 * from http://www.orbital-computer.de/JComboBox/.
 */
public class AutoCompletion<E> extends PlainDocument {
    private final JComboBox<E> comboBox;
    private ComboBoxModel<E> model;
    private JTextComponent editor;

    // flag to indicate if setSelectedItem has been called
    // subsequent calls to remove/insertString should be ignored
    private boolean selecting = false;

    private final boolean hidePopupOnFocusLoss;
    private boolean hitBackspace = false;
    private boolean hitBackspaceOnSelection;

    private final KeyListener editorKeyListener;
    private final FocusListener editorFocusListener;

    @SuppressWarnings("unchecked")
    public AutoCompletion(final JComboBox<E> comboBox) {
        this.comboBox = comboBox;
        this.model = comboBox.getModel();

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!selecting){
                    highlightCompletedText(0);
                }
            }
        });

        comboBox.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if ("editor".equals(e.getPropertyName())){
                    configureEditor((ComboBoxEditor) e.getNewValue());
                }
                if ("model".equals(e.getPropertyName())){
                    model = (ComboBoxModel) e.getNewValue();
                }
            }
        });

        this.editorKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (comboBox.isDisplayable()) {
                    comboBox.setPopupVisible(true);
                }
                hitBackspace = false;
                switch (e.getKeyCode()) {
                    // determine if the pressed key is backspace (needed by the remove method)
                    case KeyEvent.VK_BACK_SPACE:
                        hitBackspace = true;
                        hitBackspaceOnSelection = editor.getSelectionStart() != editor.getSelectionEnd();
                        break;

                    // ignore delete key
                    case KeyEvent.VK_DELETE:
                        e.consume();
                        comboBox.getToolkit().beep();
                        break;
                }
            }
        };

        // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
        this.hidePopupOnFocusLoss = System.getProperty("java.version").startsWith("1.5");

        // Highlight whole text when gaining focus
        this.editorFocusListener = new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                highlightCompletedText(0);
            }

            @Override
            public void focusLost(final FocusEvent e) {
                // Workaround for Bug 5100422 - Hide Popup on focus loss
                if (hidePopupOnFocusLoss) {
                    comboBox.setPopupVisible(false);
                }
            }
        };

        configureEditor(comboBox.getEditor());

        // Handle initially selected object
        final E selected = (E) comboBox.getSelectedItem();
        if (selected != null) {
            setText(selected.toString());
        }
        highlightCompletedText(0);
    }

    public static <T> AutoCompletion<T> enable(final JComboBox<T> comboBox) {
        // has to be editable
        comboBox.setEditable(true);
        // change the editor's document
        return new AutoCompletion<>(comboBox);
    }

    void configureEditor(final ComboBoxEditor newEditor) {
        if (editor != null) {
            editor.removeKeyListener(editorKeyListener);
            editor.removeFocusListener(editorFocusListener);
        }

        if (newEditor != null) {
            editor = (JTextComponent) newEditor.getEditorComponent();
            editor.addKeyListener(editorKeyListener);
            editor.addFocusListener(editorFocusListener);
            editor.setDocument(this);
        }
    }

    @Override
    public void remove(int offs, final int len) throws BadLocationException {
        // return immediately when selecting an item
        if (selecting) {
            return;
        }
        if (hitBackspace) {
            // user hit backspace => move the selection backwards
            // old item keeps being selected
            if (offs > 0) {
                if (hitBackspaceOnSelection) {
                    offs--;
                }
            } else {
                // User hit backspace with the cursor positioned on the start => beep
                comboBox.getToolkit().beep(); // when available use: UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
            }
            highlightCompletedText(offs);
        } else {
            super.remove(offs, len);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insertString(int offs, final String str, final AttributeSet a) throws BadLocationException {
        // return immediately when selecting an item
        if (selecting) {
            return;
        }
        // insert the string into the document
        super.insertString(offs, str, a);
        // lookup and select a matching item
        E item = lookupItem(getText(0, getLength()));
        if (item != null) {
            setSelectedItem(item);
        } else {
            // keep old item selected if there is no match
            item = (E) comboBox.getSelectedItem();
            // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
            offs = offs - str.length();
            // provide feedback to the user that his input has been received but can not be accepted
            comboBox.getToolkit().beep(); // when available use: UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
        }
        setText(item != null ? item.toString() : null);

        // select the completed part
        highlightCompletedText(offs + str.length());
    }

    private void setText(final String text) {
        try {
            // remove all text and insert the completed string
            super.remove(0, getLength());
            super.insertString(0, text, null);
        } catch (final BadLocationException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private void highlightCompletedText(final int start) {
        editor.setCaretPosition(getLength());
        editor.moveCaretPosition(start);
    }

    private void setSelectedItem(final E item) {
        selecting = true;
        model.setSelectedItem(item);
        selecting = false;
    }

    @SuppressWarnings("unchecked")
    private E lookupItem(final String pattern) {
        final E selectedItem = (E) model.getSelectedItem();
        // only search for a different item if the currently selected does not match
        if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
            return selectedItem;
        } else {
            // iterate over all items
            for (int i = 0, n = model.getSize(); i < n; i++) {
                final E currentItem = model.getElementAt(i);
                // current item starts with the pattern?
                if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), pattern)) {
                    return currentItem;
                }
            }
        }
        // no item starts with the pattern => return null
        return null;
    }

    // checks if str1 starts with str2 - ignores case
    private boolean startsWithIgnoreCase(final String str1, final String str2) {
        return str1.toUpperCase().startsWith(str2.toUpperCase());
    }
}
