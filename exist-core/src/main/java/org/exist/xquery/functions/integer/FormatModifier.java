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
package org.exist.xquery.functions.integer;

import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represent format modifier part of the formatting picture for integer formatting
 * {@see https://www.w3.org/TR/xpath-functions-31/#formatting-integers}
 */
class FormatModifier {

    enum Numbering {CARDINAL, ORDINAL}

    enum LetterSequence {ALPHABETIC, TRADITIONAL}

    final String modifier;
    final boolean isEmpty;
    Numbering numbering = Numbering.CARDINAL;
    String variation;
    LetterSequence letterSequence = LetterSequence.ALPHABETIC;

    static final Pattern modifierPattern = Pattern.compile("^(?:([co])(\\((.+)\\))?)?([at])?$");

    FormatModifier(final String modifier) throws XPathException {
        this.modifier = modifier;
        this.isEmpty = modifier.isEmpty();
        if (!isEmpty) {
            parseModifier();
        }
    }

    private void parseModifier() throws XPathException {
        final Matcher m = modifierPattern.matcher(modifier);
        if (!m.matches()) {
            throw new XPathException((Expression) null, ErrorCodes.FODF1310, "Modifier " + modifier + " is not a valid pattern modifier");
        }
        final String n = m.group(1);
        if (n != null) {
            if (n.equals("c")) numbering = Numbering.CARDINAL;
            if (n.equals("o")) numbering = Numbering.ORDINAL;
        }
        final String v = m.group(3);
        if (v != null) {
            variation = v;
        }
        final String l = m.group(4);
        if (l != null) {
            if (l.equals("a")) letterSequence = LetterSequence.ALPHABETIC;
            if (l.equals("t")) letterSequence = LetterSequence.TRADITIONAL;
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("numbering=").append(numbering).append("::");
        sb.append("variation=").append(variation).append("::");
        sb.append("lettersequence=").append(letterSequence).append("::");
        return sb.substring(0, sb.length() - 2);
    }
}
