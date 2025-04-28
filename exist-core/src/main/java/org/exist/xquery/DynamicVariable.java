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
package org.exist.xquery;

import net.jcip.annotations.Immutable;
import org.exist.dom.QName;
import org.exist.dom.persistent.DocumentSet;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;

import java.util.function.Supplier;

@Immutable
public class DynamicVariable implements Variable {

    private final QName name;
    private final Supplier<Sequence> valueSupplier;

    public DynamicVariable(final QName name, final Supplier<Sequence> valueSupplier) {
        this.name = name;
        this.valueSupplier = valueSupplier;
    }

    @Override
    public void setValue(final Sequence val) {
        throwImmutable();
    }

    @Override
    public Sequence getValue() {
        return valueSupplier.get();
    }

    @Override
    public QName getQName() {
        return name;
    }

    @Override
    public int getType() {
        return valueSupplier.get().getItemType();
    }

    @Override
    public void setSequenceType(final SequenceType type) {
        throwImmutable();
    }

    @Override
    public SequenceType getSequenceType() {
        final Sequence value = getValue();
        return new SequenceType(value.getItemType(), value.getCardinality());
    }

    @Override
    public void setStaticType(final int type) {
        throwImmutable();
    }

    @Override
    public int getStaticType() {
        return getType();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void setIsInitialized(final boolean initialized) {
    }

    @Override
    public int getDependencies(final XQueryContext context) {
        return 0;
    }

    @Override
    public Cardinality getCardinality() {
        return getValue().getCardinality();
    }

    @Override
    public void setStackPosition(final int position) {
    }

    @Override
    public DocumentSet getContextDocs() {
        return DocumentSet.EMPTY_DOCUMENT_SET;
    }

    @Override
    public void setContextDocs(final DocumentSet docs) {
        throwImmutable();
    }

    @Override
    public void checkType() {

    }

    private static void throwImmutable() {
        throw new UnsupportedOperationException("Changing a dynamic variable is not permitted");
    }
}
