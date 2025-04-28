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
package org.exist.util;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class WeakLazyStripesStressTest {

    @JCStressTest
    @Outcome(id = "false", expect = Expect.FORBIDDEN, desc = "Different Object for Same Key")
    @Outcome(id = "true", expect = Expect.ACCEPTABLE, desc = "Same Object for Same Key")
    @State
    public static class SameObjectForSameKey {
        WeakLazyStripes<String, Lock> lockMap = new WeakLazyStripes<>((key) -> new ReentrantLock());
        Object ar1;
        Object ar2;
        Object ar3;
        Object ar4;
        Object ar5;
        Object ar6;
        Object ar7;
        Object ar8;


        @Actor
        public void actor1() {
            ar1 = lockMap.get("key1");
        }

        @Actor
        public void actor2() {
            ar2 = lockMap.get("key1");
        }

        @Actor
        public void actor3() {
            ar3 = lockMap.get("key1");
        }

        @Actor
        public void actor4() {
            ar4 = lockMap.get("key1");
        }

        @Actor
        public void actor5() {
            ar5 = lockMap.get("key1");
        }

        @Actor
        public void actor6() {
            ar6 = lockMap.get("key1");
        }

        @Actor
        public void actor7() {
            ar7 = lockMap.get("key1");
        }

        @Actor
        public void actor8() {
            ar8 = lockMap.get("key1");
        }

        @Arbiter
        public void arbiter(Z_Result r) {
            r.r1 = ar1 == ar2 && ar2 == ar3 && ar3 == ar4 && ar4 == ar5 && ar5 == ar6 && ar6 == ar7 && ar7 == ar8;
        }
    }

    @JCStressTest
    @Outcome(id = "false", expect = Expect.FORBIDDEN, desc = "Same Object for Different Keys")
    @Outcome(id = "true", expect = Expect.ACCEPTABLE, desc = "Different Object for Different Keys")
    @State
    public static class DifferentObjectForDifferentKeys {
        WeakLazyStripes<String, Lock> lockMap = new WeakLazyStripes<>((key) -> new ReentrantLock());
        Object ar1;
        Object ar2;
        Object ar3;
        Object ar4;
        Object ar5;
        Object ar6;
        Object ar7;
        Object ar8;


        @Actor
        public void actor1() {
            ar1 = lockMap.get("key1");
        }

        @Actor
        public void actor2() {
            ar2 = lockMap.get("key2");
        }

        @Actor
        public void actor3() {
            ar3 = lockMap.get("key3");
        }

        @Actor
        public void actor4() {
            ar4 = lockMap.get("key4");
        }

        @Actor
        public void actor5() {
            ar5 = lockMap.get("key5");
        }

        @Actor
        public void actor6() {
            ar6 = lockMap.get("key6");
        }

        @Actor
        public void actor7() {
            ar7 = lockMap.get("key7");
        }

        @Actor
        public void actor8() {
            ar8 = lockMap.get("key8");
        }

        @Arbiter
        public void arbiter(Z_Result r) {
            r.r1 = ar1 != ar2 && ar2 != ar3 && ar3 != ar4 && ar4 != ar5 && ar5 != ar6 && ar6 != ar7 && ar7 != ar8;
        }
    }

}
