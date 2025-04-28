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
import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.http.RESTServer;
import org.exist.xquery.XPathException;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class AtomicValueComparator implements Comparator<AtomicValue> {

    protected static final Logger LOG = LogManager.getLogger(RESTServer.class);

    private final @Nullable Collator collator;

    public AtomicValueComparator() {
        this(null);
    }

    public AtomicValueComparator(@Nullable final Collator collator) {
        this.collator = collator;
    }

    @Override
    public int compare(final AtomicValue o1, final AtomicValue o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        try {
            return o1.compareTo(collator, o2);
        } catch (final XPathException e) {
            LOG.error(e.getMessage(), e);
            throw new ClassCastException(e.getMessage());
        }
    }
}
