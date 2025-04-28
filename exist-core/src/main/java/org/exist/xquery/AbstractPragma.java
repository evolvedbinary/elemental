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

import org.exist.dom.QName;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;

/**
 * Base class for implementing an XQuery Pragma expression.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractPragma implements Pragma {
    private final QName name;
    private @Nullable final String contents;
    private @Nullable final Expression expression;

    public AbstractPragma(@Nullable final Expression expression, final QName name, @Nullable final String contents) {
        this.expression = expression;
        this.name = name;
        this.contents = contents;
    }

    @Override
    public QName getName() {
        return name;
    }

    public @Nullable Expression getExpression() {
        return expression;
    }

    @Override
    public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        // no-op by default
    }

    @Override
    public void before(final XQueryContext context, @Nullable final Expression expression, final Sequence contextSequence) throws XPathException {
        // no-op by default
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        // no-op by default
        return null;
    }

    @Override
    public void after(final XQueryContext context, @Nullable final Expression expression) throws XPathException {
        // no-op by default
    }

    protected @Nullable String getContents() {
        return contents;
    }

    @Override
    public void dump(final ExpressionDumper dumper) {
        dumper.display("(# " + getName().getStringValue());
        if (getContents() != null) {
            dumper.display(' ').display(getContents());
        }
    }

    @Override
    public void resetState(final boolean postOptimization) {
        //no-op by default
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("(# ");
        builder.append(name);
        if (contents != null && !contents.isEmpty()) {
            builder.append(' ').append(contents);
        }
        builder.append("#)");
        return builder.toString();
    }
}
