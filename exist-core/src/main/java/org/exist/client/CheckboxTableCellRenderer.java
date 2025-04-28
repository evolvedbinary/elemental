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
package org.exist.client;

import com.evolvedbinary.j8fu.tuple.Tuple2;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

/**
 * Renders a T as a JCheckBox.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CheckboxTableCellRenderer<T> extends JCheckBox
        implements TableCellRenderer, UIResource {
    
    private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    private final Function<T, Tuple2<String, Boolean>> valueStateFn;

    public CheckboxTableCellRenderer(final Function<T, Tuple2<String, Boolean>> valueStateFn) {
        super();
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorderPainted(true);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        this.valueStateFn = valueStateFn;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, 
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        
        //set selected
        final Tuple2<String, Boolean> state = valueStateFn.apply((T)value);
        setSelected(state._2 != null && state._2);

        //set label
        if (value != null) {
            setText(state._1);
        }

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            setBorder(noFocusBorder);
        }

        return this;
    }
}
