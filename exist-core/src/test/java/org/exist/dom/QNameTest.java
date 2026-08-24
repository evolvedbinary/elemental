/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.exist.dom;

import org.exist.xquery.XQueryContext;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;

import static org.exist.dom.QName.Validity.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class QNameTest {

    @Test
    void validLocalPart_1() {
        final QName qName = new QName("valid-name", XMLConstants.NULL_NS_URI);
        assertEquals("valid-name", qName.getLocalPart());
        assertEquals(XMLConstants.NULL_NS_URI, qName.getNamespaceURI());
        assertNull(qName.getPrefix());  //TODO(AR) should this be XMLConstants.DEFAULT_NS_PREFIX
        assertEquals(VALID.val, qName.isValid(false));
    }

    @Test
    void invalidLocalPart_1() {
        final QName qname = new QName("invalid^Name", XMLConstants.NULL_NS_URI);
        assertEquals(INVALID_LOCAL_PART.val, qname.isValid(false));
    }

    @Test
    void invalidLocalPart_validNamespace_1() {
        final QName qname = new QName("invalid^Name", "http://some/ns");
        assertEquals(INVALID_LOCAL_PART.val, qname.isValid(false));
    }

    @Test
    void validWildcard_1() {
        final QName qName = new QName.WildcardLocalPartQName("abc");
        assertEquals(VALID.val, qName.isValid(true));
    }

    @Test
    void invalidWildcard_1() {
        final QName qName = new QName.WildcardLocalPartQName("abc");
        assertEquals(INVALID_LOCAL_PART.val, qName.isValid(false));
    }

    @Test
    void validWildcard_2() {
        final QName qName = new QName.WildcardNamespaceURIQName("xyz");
        assertEquals(VALID.val, qName.isValid(true));
    }

    @Test
    void validWildcard_3() {
        final QName qName = QName.WildcardQName.getInstance();
        assertEquals(VALID.val, qName.isValid(true));
    }

    @Test
    void invalidWildcard_3() {
        final QName qName = QName.WildcardQName.getInstance();
        assertEquals(INVALID_LOCAL_PART.val ^ INVALID_PREFIX.val, qName.isValid(false));
    }

    @Test
    void isQName_illegalFormat1() {
        assertEquals(ILLEGAL_FORMAT.val, QName.isQName("emp:"));
    }

    @Test
    void isQName_illegalFormat2() {
        assertEquals(ILLEGAL_FORMAT.val, QName.isQName(":emp"));
    }

    @Test
    void parseEqNameWithDefaultNS() throws XPathException, QName.IllegalQNameException {
        final XQueryContext context = new XQueryContext();
        context.declareNamespace(XMLConstants.DEFAULT_NS_PREFIX, "a");
        final QName parsed = QName.parse(context, "Q{a}b", XMLConstants.DEFAULT_NS_PREFIX);
        assertEquals("b", parsed.getStringValue());
        assertEquals("a", parsed.getNamespaceURI());
    }

    @Test
    void parseEqNameWithNSBound() throws XPathException, QName.IllegalQNameException {
        final XQueryContext context = new XQueryContext();
        context.declareNamespace("c", "a");
        final QName parsed = QName.parse(context, "Q{a}b", XMLConstants.DEFAULT_NS_PREFIX);
        assertEquals("b", parsed.getStringValue());
        assertEquals("a", parsed.getNamespaceURI());
    }

    @Test
    void parseInvalidEqName() {
        try {
            QName.parse(new XQueryContext(), "Q{:b", XMLConstants.DEFAULT_NS_PREFIX);
            fail("invalid QName in clark notation was parsed");
        } catch (Exception e) {
            assertEquals("No namespace defined for prefix Q{. QName is invalid: INVALID_PREFIX", e.getMessage());
        }
    }

    @Test
    void parseClarkNotationWithDefaultNS() throws XPathException, QName.IllegalQNameException {
        final XQueryContext context = new XQueryContext();
        context.declareNamespace(XMLConstants.DEFAULT_NS_PREFIX, "a");
        final QName parsed = QName.parse(context, "{a}b", XMLConstants.DEFAULT_NS_PREFIX);
        assertEquals("b", parsed.getStringValue());
        assertEquals("a", parsed.getNamespaceURI());
    }

    @Test
    void parseClarkNotationWithNSBound() throws XPathException, QName.IllegalQNameException {
        final XQueryContext context = new XQueryContext();
        context.declareNamespace("c", "a");
        final QName parsed = QName.parse(context, "{a}b", XMLConstants.DEFAULT_NS_PREFIX);
        assertEquals("b", parsed.getStringValue());
        assertEquals("a", parsed.getNamespaceURI());
    }

    @Test
    void parseInvalidClarkNotation() throws XPathException, QName.IllegalQNameException {
        try {
            QName.parse(new XQueryContext(), "{a:b", XMLConstants.DEFAULT_NS_PREFIX);
            fail("invalid QName in clark notation was parsed");
        } catch (Exception e) {
            assertEquals("No namespace defined for prefix {a. QName is invalid: INVALID_PREFIX", e.getMessage());
        }
    }
}

