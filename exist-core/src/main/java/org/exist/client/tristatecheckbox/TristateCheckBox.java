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
package org.exist.client.tristatecheckbox;

import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * See <a href="https://stackoverflow.com/questions/1263323/tristate-checkboxes-in-java">Tristate Checkboxes in Java</a>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TristateCheckBox extends JCheckBox implements Icon, ActionListener {

    static final boolean INDETERMINATE_AS_SELECTED = true;  //consider INDETERMINATE as selected ?
    static final Icon icon = MetalIconFactory.getCheckBoxIcon();

    public TristateCheckBox() { this(""); }

    public TristateCheckBox(final String text) {
        this(text, TristateState.DESELECTED);
    }

    public TristateCheckBox(final String text, final TristateState state) {
        /* tri-state checkbox has 3 selection states:
         * 0 unselected
         * 1 mid-state selection
         * 2 fully selected
         */
        super(text, state == TristateState.SELECTED);

        switch (state) {
            case SELECTED: setSelected(true);
            case INDETERMINATE:
            case DESELECTED:
                putClientProperty("SelectionState", state);
                break;
            default:
                throw new IllegalArgumentException();
        }
        addActionListener(this);
        setIcon(this);
    }

    @Override
    public boolean isSelected() {
        if (INDETERMINATE_AS_SELECTED && (getSelectionState() != TristateState.DESELECTED)) {
            return true;
        } else {
            return super.isSelected();
        }
    }

    public TristateState getSelectionState() {
        return (getClientProperty("SelectionState") != null ? (TristateState) getClientProperty("SelectionState") :
                super.isSelected() ? TristateState.SELECTED : TristateState.DESELECTED);
    }

    public void setSelectionState(final TristateState state) {
        switch (state) {
            case SELECTED: setSelected(true);
                break;
            case INDETERMINATE:
            case DESELECTED: setSelected(false);
                break;
            default:
                throw new IllegalArgumentException();
        }
        putClientProperty("SelectionState", state);
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        icon.paintIcon(c, g, x, y);
        if (getSelectionState() != TristateState.INDETERMINATE) {
            return;
        }

        final int w = getIconWidth();
        final int h = getIconHeight();
        g.setColor(c.isEnabled() ? new Color(51, 51, 51) : new Color(122, 138, 153));
        g.fillRect(x+4, y+4, w-8, h-8);

        if (!c.isEnabled()) {
            return;
        }
        g.setColor(new Color(81, 81, 81));
        g.drawRect(x+4, y+4, w-9, h-9);
    }

    @Override
    public int getIconWidth() {
        return icon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return icon.getIconHeight();
    }

    public void actionPerformed(final ActionEvent e) {
        final TristateCheckBox tcb = (TristateCheckBox) e.getSource();
        if (tcb.getSelectionState() == TristateState.DESELECTED) {
            tcb.setSelected(false);
        }

        tcb.putClientProperty("SelectionState", tcb.getSelectionState() == TristateState.SELECTED ? TristateState.DESELECTED :
                tcb.getSelectionState().next());

//        // test
//        System.out.println(">>>>IS SELECTED: "+tcb.isSelected());
//        System.out.println(">>>>IN MID STATE: "+(tcb.getSelectionState() == TristateState.INDETERMINATE));
    }
}
