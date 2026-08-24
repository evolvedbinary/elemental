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
package org.exist.util.serializer;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.matches;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.siemens.ct.exi.core.exceptions.EXIException;
import org.easymock.Capture;
import org.exist.dom.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;

import com.siemens.ct.exi.main.api.sax.SAXEncoder;
import org.xml.sax.SAXException;

class EXISerializerTest {

	private EXISerializer serializer;
	private OutputStream mockOutputStream;
	private SAXEncoder mockEncoder;

    @BeforeEach
    void setUp() throws EXIException, IOException {
		mockOutputStream = createMock(OutputStream.class);
		serializer = new EXISerializer(mockOutputStream);
		mockEncoder = createMock(SAXEncoder.class);
		serializer.setEncoder(mockEncoder);
	}

    @Test
    void startDocument() throws SAXException {
		mockEncoder.startDocument();
		replay(mockEncoder);
		serializer.startDocument();
		verify(mockEncoder);
	}

    @Test
    void endDocument() throws SAXException {
		mockEncoder.endDocument();
		replay(mockEncoder);
		serializer.endDocument();
		verify(mockEncoder);
	}

    @Test
    void startPrefixMapping() throws SAXException {
		mockEncoder.startPrefixMapping("prefix", "uri");
		replay(mockEncoder);
		serializer.startPrefixMapping("prefix", "uri");
		verify(mockEncoder);
	}

    @Test
    void endPrefixMapping() throws SAXException {
		mockEncoder.endPrefixMapping("prefix");
		replay(mockEncoder);
		serializer.endPrefixMapping("prefix");
		verify(mockEncoder);
	}

    @Test
    void startElement() throws SAXException {
		QName testQName = new QName("local", "uri", "prefix");
		AttrList testAttrList = new AttrList();
		testAttrList.addAttribute(new QName("local", "uri"), "value");
		Capture<Attributes> capturedAttributes = Capture.newInstance();
		mockEncoder.startElement(matches("uri"), matches("local"), (String)isNull(), capture(capturedAttributes));
		replay(mockEncoder);
		serializer.startElement(testQName, testAttrList);
		verify(mockEncoder);
		List<Attributes> capturedAttributeList = capturedAttributes.getValues();
		assertEquals("local", capturedAttributeList.get(0).getLocalName(0));
		assertEquals("uri", capturedAttributeList.get(0).getURI(0));
		assertEquals("value", capturedAttributeList.get(0).getValue(0));
	}

    @Test
    void endElement() throws SAXException {
		QName testQName = new QName("local", "uri", "prefix");
		mockEncoder.endElement(matches("uri"), matches("local"), (String)isNull());
		replay(mockEncoder);
		serializer.endElement(testQName);
		verify(mockEncoder);
	}

    @Test
    void characters() throws SAXException {
		String testString = "test";
		CharSequence testSeq = testString;
		mockEncoder.characters(aryEq(testString.toCharArray()), eq(0), eq(testString.length()));
		replay(mockEncoder);
		serializer.characters(testSeq);
		verify(mockEncoder);
	}
	
}