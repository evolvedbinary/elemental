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

import static org.exist.xquery.Cardinality.InternalValue.*;

/**
 * Defines <a href="https://www.w3.org/TR/xpath-31/#prod-xpath31-OccurrenceIndicator">XPath Occurrence Indicators</a>
 * (*,?,+), and additionally defines {@link #EMPTY_SEQUENCE}, and {@link #EXACTLY_ONE} for convenience.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum Cardinality {

    EMPTY_SEQUENCE(ZERO),

    //TODO(AR) can we eliminate this?
    EXACTLY_ONE(ONE),

    //TODO(AR) eliminate this in favour of probably ONE_OR_MORE
    _MANY(MANY),

    /**
     * indicator '?'
     */
    ZERO_OR_ONE((byte)(ZERO | ONE)),

    /**
     * indicator '+'
     */
    ONE_OR_MORE((byte)(ONE | MANY)),

    /**
     * indicator '*'
     */
    ZERO_OR_MORE((byte)(ZERO | ONE | MANY));


    private final byte val;

    Cardinality(final byte val) {
        this.val = val;
    }

    /**
     * The cardinality represents a sequence of at least one value.
     *
     * @return true if the cardinality represents a sequence of at least one value, or false otherwise.
     */
    public boolean atLeastOne() {
        return (val & ZERO) == 0;
    }

    /**
     * The cardinality represents a sequence of at most one value.
     *
     * @return true if the cardinality represents a sequence of at most one value, or false otherwise.
     */
    public boolean atMostOne() {
        return (val & MANY) == 0;
    }

    /**
     * Tests whether this Cardinality is a sub-cardinality or equal
     * of {@code other}.
     *
     * @param other the other cardinality
     *
     * @return true if this is a sub-cardinality or equal cardinality of {@code other}.
     */
    public boolean isSubCardinalityOrEqualOf(final Cardinality other) {
        return (val | other.val) == other.val;
    }

    /**
     * Tests whether this Cardinality is a super-cardinality or equal
     * of {@code other}.
     *
     * @param other the other cardinality
     *
     * @return true if this is a super-cardinality or equal cardinality of {@code other}.
     */
    public boolean isSuperCardinalityOrEqualOf(final Cardinality other) {
        return (val & other.val) == other.val;
    }

    /**
     * Given two cardinalities, return a cardinality that is capable of
     * representing both,
     * i.e.: {@code a.isSubCardinalityOrEqualOf(max(a, b)) && b.isSubCardinalityOrEqualOf(max(a, b)) && b.max(a, b))}
     *
     * @param a the first cardinality
     * @param b the second cardinality
     *
     * @return the super cardinality
     */
    public static Cardinality superCardinalityOf(final Cardinality a, final Cardinality b) {
        final byte max = (byte)(a.val | b.val);
        for (final Cardinality cardinality : Cardinality.values()) {
            if (cardinality.val == max) {
                return cardinality;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Get an XQuery notation representation of the cardinality.
     *
     * @return the XQuery notation
     */
    public String toXQueryCardinalityString() {
        // impossible
        return switch (this) {
            case EMPTY_SEQUENCE -> "empty-sequence()";
            case EXACTLY_ONE -> "";
            case ZERO_OR_ONE -> "?";
            case _MANY, ONE_OR_MORE -> "+";
            case ZERO_OR_MORE -> "*";
            default -> throw new IllegalArgumentException("Unknown cardinality: " + name());
        };
    }

    /**
     * Get a human pronounceable description of the cardinality.
     *
     * @return a pronounceable description
     */
    public String getHumanDescription() {
        // impossible
        return switch (this) {
            case EMPTY_SEQUENCE -> "empty";
            case EXACTLY_ONE -> "exactly one";
            case ZERO_OR_ONE -> "zero or one";
            case _MANY, ONE_OR_MORE -> "one or more";
            case ZERO_OR_MORE -> "zero or more";
            default -> throw new IllegalArgumentException("Unknown cardinality: " + name());
        };
    }

    /**
     * Get the Cardinality from an integer representation.
     *
     * @param intValue integer representation of cardinality
     *
     * @return the cardinality
     *
     * @deprecated You should not pass cardinality as integer values,
     *     this is for backwards compatibility with eXist-db
     */
    @Deprecated
    public static Cardinality fromInt(final int intValue) {
        for (final Cardinality cardinality : Cardinality.values()) {
            if (cardinality.val == intValue) {
                return cardinality;
            }
        }
        throw new IllegalArgumentException("No know cardinality for intValue: " + intValue);
    }

    /**
     * Compare this Cardinality to another Cardinality.
     *
     * NOTE we can't implement (override) {@link Comparable#compareTo(Object)}
     * here as it is final in {@link java.lang.Enum}.
     *
     * @param other the other Cardinality to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this Cardinality is less than, equal to, or greater than the other Cardinality.
     */
    public int compare(final Cardinality other) {
        return Byte.compare(val, other.val);
    }

    static class InternalValue {
        static final byte ZERO = 1;
        static final byte ONE = 2;
        static final byte MANY = 4;
    }
}
