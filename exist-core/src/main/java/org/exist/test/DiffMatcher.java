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
package org.exist.test;

import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.util.Convert;

import javax.xml.transform.Source;

/**
 * Implementation of a Hamcrest Matcher
 * which will compare XML nodes.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class DiffMatcher extends DiagnosingMatcher<Sequence> {
    private final Source expectedSource;
    private final boolean identical;

    private DiffMatcher(final Source expectedSource) {
        this(expectedSource, false);
    }

    private DiffMatcher(final Source expectedSource, final boolean identical) {
        this.expectedSource = expectedSource;
        this.identical = identical;
    }

    /**
     * Compares that the XML sources are similar.
     *
     * In this context "similar" is defined by {@link DiffBuilder#checkForSimilar()}.
     *
     * @param expectedSource the expected XML
     *
     * @return The Hamcrest Matcher
     */
    public static DiffMatcher hasSimilarXml(final Source expectedSource) {
        return new DiffMatcher(expectedSource);
    }

    /**
     * Compares that the XML sources are identical.
     *
     * In this context "similar" is defined by {@link DiffBuilder#checkForIdentical()} ()}.
     *
     * @param expectedSource the expected XML
     *
     * @return The Hamcrest Matcher
     */
    public static DiffMatcher hasIdenticalXml(final Source expectedSource) {
        return new DiffMatcher(expectedSource, true);
    }

    @Override
    public boolean matches(final Object item, final Description mismatch) {
        if (item == null) {
            mismatch.appendText("null");
            return false;
        }

        final Item actualItem;
        if (item instanceof NodeValue) {
            actualItem = (NodeValue) item;

        } else if (item instanceof Sequence actual) {

            if (actual.getItemCount() != 1) {
                mismatch.appendText("Sequence does not contain 1 item");
                return false;
            }

            actualItem = actual.itemAt(0);
            if (!(actualItem instanceof NodeValue)) {
                mismatch.appendText("Sequence does not contain a Node");
                return false;
            }
        } else {
            mismatch.appendText("is not a Node");
            return false;
        }

        final Source actualSource = Input.fromNode((org.w3c.dom.Node) actualItem).build();

        DiffBuilder diffBuilder = DiffBuilder.compare(expectedSource)
                .withTest(actualSource);
        if (identical) {
            diffBuilder = diffBuilder.checkForIdentical();
        } else {
            diffBuilder = diffBuilder.checkForSimilar();
        }

        final Diff diff = diffBuilder.build();
        if (diff.hasDifferences()) {
            mismatch.appendText("differences: " + diff.toString());
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description
                .appendText("nodes match ")
                .appendValue(expectedSource);
    }

    /**
     * Creates an Document Source form an XML String.
     *
     * @param str a string representation of XML.
     *
     * @return a Document Source.
     */
    public static Source docSource(final String str) {
        return Input.fromString(str).build();
    }

    /**
     * Creates an Element Source form an XML String.
     *
     * @param str a string representation of XML.
     *
     * @return an Element Source.
     */
    public static Source elemSource(final String str) {
        final Node documentNode = Convert.toNode(docSource(str));
        final Node firstElement = documentNode.getFirstChild();
        return Input.fromNode(firstElement).build();
    }
}
