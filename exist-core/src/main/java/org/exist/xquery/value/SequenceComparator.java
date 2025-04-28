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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.http.RESTServer;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class SequenceComparator implements Comparator<Sequence> {

    protected static final Logger LOG = LogManager.getLogger(RESTServer.class);

    private final @Nullable Collator collator;
    private @Nullable ItemComparator itemComparator = null;

    public SequenceComparator() {
        this(null);
    }

    public SequenceComparator(@Nullable final Collator collator) {
        this.collator = collator;
    }

    @Override
    public int compare(final Sequence o1, final Sequence o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        final int o1Count = o1.getItemCount();
        final int o2Count = o1.getItemCount();

        if (o1Count < o2Count) {
            return -1;
        } else if(o1Count > o2Count) {
            return 1;
        } else if (o1Count == 0 && o1Count == o2Count) {
            return 0;
        }

        for (int i = 0; i < o1Count; i++) {
            if (itemComparator == null) {
                itemComparator = new ItemComparator(collator);
            }

            final Item i1 = o1.itemAt(i);
            final Item i2 = o2.itemAt(i);
            final int result = itemComparator.compare(i1, i2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }
}
