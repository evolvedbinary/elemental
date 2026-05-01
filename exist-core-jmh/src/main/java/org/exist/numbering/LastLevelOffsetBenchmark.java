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
package org.exist.numbering;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for {@link DLNBase#lastLevelOffset()}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@State(Scope.Benchmark)
public class LastLevelOffsetBenchmark {

    private DLN shallowDln; // eg 1

    private DLN wideDln; // eg 1.500

    private DLN deepDln; // eg 1.1.1.1.1.1.1.1.1.1

    @Setup
    public void setup() {
        shallowDln = new DLN(1);

        wideDln = new DLN(1);
        wideDln.addLevelId(500, false);

        deepDln = new DLN(1);
        for (int i = 0; i < 9; i++) {
            deepDln.addLevelId(1, false);
        }

    }

    @Benchmark
    public void lastLevelOffset_shallow(final Blackhole bh) {
        bh.consume(shallowDln.lastLevelOffset());
    }

    @Benchmark
    public void lastLevelOffset_deep(final Blackhole bh) {
        bh.consume(deepDln.lastLevelOffset());
    }

    @Benchmark
    public void lastLevelOffset_wide(final Blackhole bh) {
        bh.consume(wideDln.lastLevelOffset());
    }

    public static void main(final String[] args) {
        final LastLevelOffsetBenchmark bench = new LastLevelOffsetBenchmark();
        bench.setup();

    }
}
