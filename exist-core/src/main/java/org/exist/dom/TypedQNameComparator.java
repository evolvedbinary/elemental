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
package org.exist.dom;

import org.exist.xquery.Constants;

import java.util.Comparator;

/**
 * Comparator for comparing two QNames which takes their
 * nameType into account
 *
 * Should be able to be removed in future when we further refactor
 * to decouple QName from nameType.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TypedQNameComparator implements Comparator<QName> {

    @Override
    public int compare(final QName q1, final QName q2) {
        if(q1.getNameType() != q2.getNameType()) {
        return q1.getNameType() < q2.getNameType() ? Constants.INFERIOR : Constants.SUPERIOR;
        }
        final int c = q1.getNamespaceURI().compareTo(q2.getNamespaceURI());
        return c == Constants.EQUAL ? q1.getLocalPart().compareTo(q2.getLocalPart()) : c;
    }
}
