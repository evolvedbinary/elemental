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
package org.exist.xquery.utils;

import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Benchmarks on variations of Java String Join operations.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@State(Scope.Benchmark)
public class StringJoinBenchmark {

    private final int maxStringLength = 20;

    @Param({ "1", "2", "5", "10", "100", "1000", "10000" })
    private int numOfStrings;

    private final List<String> strings = new ArrayList();

//    @State(Scope.Thread)
//    public static class BuilderState {
//        StringBuilder builder;
//
//        @Setup(Level.Invocation)  //TODO(AR) check that Level.Invocation is correct
//        public void setUp() {
//            builder = new StringBuilder();
//        }
//    }

    @Setup(Level.Trial)
    public void setUp() {
        final byte[] strData = new byte[maxStringLength];
        final Random random = new Random();
        for (int i = 0; i < numOfStrings; i++) {
            final int strLen = random.nextInt(maxStringLength) + 1;
            random.nextBytes(strData);
            strings.add(new String(strData, 0, strLen, StandardCharsets.UTF_8));
        }
    }

    @Benchmark
    public StringBuilder forApproach(/*final BuilderState builderState*/) {
//        final StringBuilder builder = builderState.builder;
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(strings.get(i));
        }
        return builder;
    }

    @Benchmark
    public StringBuilder forApproachRadek(/*final BuilderState builderState*/) {
//        final StringBuilder builder = builderState.builder;
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.size(); i++) {
            builder.append(strings.get(i));
            builder.append(", ");
        }

        builder.substring(0, builder.length() - 3);

        return builder;
    }

    @Benchmark
    public StringBuilder forEachApproach(/*final BuilderState builderState*/) {
//        final StringBuilder builder = builderState.builder;
        final StringBuilder builder = new StringBuilder();

        boolean firstArgument = true;
        for (final String str : strings) {
            builder.append(str);
            if (firstArgument) {
                firstArgument = false;
            } else {
                builder.append(", ");
            }
        }
        return builder;
    }

    @Benchmark
    public StringBuilder forEachApproachRadek(/*final BuilderState builderState*/) {
//        final StringBuilder builder = builderState.builder;
        final StringBuilder builder = new StringBuilder();

        boolean firstArgument = true;
        for (final String str : strings) {
            builder.append(str);
            builder.append(", ");
        }

        builder.substring(0, builder.length() - 3);

        return builder;
    }

    @Benchmark
    public String jdkApproach() {
        return String.join(", ", strings);
    }

    public static void main(final String args[]) {
        // NOTE: just for running with the java debugger
        final StringJoinBenchmark stringJoinBenchmark = new StringJoinBenchmark();
        // stringJoinBenchmark.forApproach();
        // stringJoinBenchmark.forEachApproach();
        stringJoinBenchmark.jdkApproach();
    }
}
