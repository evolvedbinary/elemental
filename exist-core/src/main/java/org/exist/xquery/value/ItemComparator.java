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
package org.exist.xquery.value;

import com.ibm.icu.text.Collator;
import net.jcip.annotations.NotThreadSafe;
import org.exist.xquery.Constants;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * Comparator for comparing instances of Item
 * apart from the XQuery atomic types there are
 * two Node types in eXist org.exist.dom.persistent.*
 * and org.exist.dom.memtree.* this class is
 * used so that both types can be compared to each other
 * as Item even though they have quite different inheritance
 * hierarchies.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class ItemComparator implements Comparator<Item> {

    @Nullable private final Collator collator;
    @Nullable private AtomicValueComparator atomicValueComparator = null;

    public ItemComparator() {
        this(null);
    }

    public ItemComparator(@Nullable final Collator collator) {
        this.collator = collator;
    }

    @Override
    public int compare(final Item n1, final Item n2) {
        if (n1 instanceof org.exist.dom.memtree.NodeImpl && (!(n2 instanceof org.exist.dom.memtree.NodeImpl))) {
            return Constants.INFERIOR;
        } else if (n1 instanceof AtomicValue && n2 instanceof AtomicValue) {
            if (atomicValueComparator == null) {
                atomicValueComparator = new AtomicValueComparator(collator);
            }
            return atomicValueComparator.compare((AtomicValue)n1, (AtomicValue)n2);
        } else if (n1 instanceof Comparable) {
            return ((Comparable) n1).compareTo(n2);
        } else {
            return Constants.INFERIOR;
        }
    }
}
