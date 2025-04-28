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
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

/**
 * Editor for a T using a JCheckBox.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CheckboxTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor {

    private final Function<T, Tuple2<String, Boolean>> valueStateFn;
    private final Function<Tuple2<String, Boolean>, T> stateValueFn;
    private T current;

    public CheckboxTableCellEditor(final Function<T, Tuple2<String, Boolean>> valueStateFn, final Function<Tuple2<String, Boolean>, T> stateValueFn) {
        super();
        this.valueStateFn = valueStateFn;
        this.stateValueFn = stateValueFn;
    }

    @Override
    public Object getCellEditorValue() {
        return current;
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        final T typedValue = (T)value;
        final Tuple2<String, Boolean> state = valueStateFn.apply(typedValue);

        final JCheckBox chkBox = new JCheckBox(state._1, state._2);
        
        chkBox.setHorizontalAlignment(SwingConstants.LEFT);
        chkBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        chkBox.addActionListener(e -> {
            current = stateValueFn.apply(Tuple(state._1, !state._2));
            fireEditingStopped(); //notify that editing is done!
        });
        
        return chkBox;
    }
}
