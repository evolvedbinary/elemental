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

import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;

/**
 * Marks an expression as being Materializable as per the
 * Materialization query execution model.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface Materializable {

    /**
     * Materialize the result.
     *
     * Depending on the context in which this expression is executed,
     * either the context sequence, the context item or both of them may
     * be set. An implementing class should know how to handle this.
     *
     * The general contract is as follows: if the {@link Dependency#CONTEXT_ITEM}
     * bit is set in the bit field returned by {@link Expression#getDependencies()}, the eval method will
     * be called once for every item in the context sequence. The <b>contextItem</b>
     * parameter will be set to the current item. Otherwise, the eval method will only be called
     * once for the whole context sequence and <b>contextItem</b> will be null.
     *
     * Tries to process the entire context set in one, single step whenever
     * possible. Thus, most classes only expect context to contain a list of
     * nodes which represents the current context of the expression.
     *
     * The position() function in XPath is an example for an expression,
     * which requires both, context sequence and context item to be set.
     *
     * The context sequence might be a node set, a sequence of atomic values or a single
     * node or atomic value.
     *
     * @param contextSequence the current context sequence, or null if there is no context sequence.
     * @param contextItem a single item, taken from context, or null if there is no context item.
     *                    This defines the item, the expression should work on.
     *
     * @return the result sequence.
     *
     * @throws XPathException if an error occurs during evaluation.
     */
    Sequence eval(@Nullable Sequence contextSequence, @Nullable Item contextItem) throws XPathException;
}
