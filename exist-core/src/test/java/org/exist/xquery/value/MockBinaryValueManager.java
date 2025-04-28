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

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.fail;

/**
 *
 * @author aretter
 */
public class MockBinaryValueManager implements BinaryValueManager {

    private Deque<BinaryValue> values = new ArrayDeque<>();

    @Override
    public void registerBinaryValueInstance(final BinaryValue binaryValue) {
        values.push(binaryValue);
    }

    @Override
    public void runCleanupTasks(final Predicate<Object> predicate) {
        if (values != null) {
            List<BinaryValue> removable = null;
            for(final Iterator<BinaryValue> iterator = values.iterator(); iterator.hasNext();) {
                final BinaryValue bv = iterator.next();
                try {
                    if (predicate.test(bv)) {
                        bv.close();
                        if(removable == null) {
                            removable = new ArrayList<>();
                        }
                        removable.add(bv);
                    }
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            }

            if(removable != null) {
                for(final BinaryValue bv : removable) {
                    values.remove(bv);
                }
            }
        }
    }

    @Override
    public String getCacheClass() {
        return "org.exist.util.io.MemoryFilterInputStreamCache";
    }
}