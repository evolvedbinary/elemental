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
package org.exist.xslt;

import java.util.stream.Stream;
import java.util.Hashtable;
import org.exist.util.Configuration;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.easymock.EasyMock;
import org.exist.storage.BrokerPool;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:adam@exist-db.org">Adam Retter</a>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TransformerFactoryAllocatorTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("net.sf.saxon.TransformerFactoryImpl"),
                Arguments.of("org.apache.xalan.processor.TransformerFactoryImpl")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void getTransformerFactory(final String transformerFactoryClass) {

        final  Hashtable<String,Object> testAttributes = new Hashtable<String,Object>();

        BrokerPool mockBrokerPool = EasyMock.createMock(BrokerPool.class);
        Configuration mockConfiguration = EasyMock.createMock(Configuration.class);

        expect(mockBrokerPool.getConfiguration()).andReturn(mockConfiguration);
        expect(mockConfiguration.getProperty(TransformerFactoryAllocator.PROPERTY_TRANSFORMER_CLASS)).andReturn(transformerFactoryClass);
        expect(mockBrokerPool.getConfiguration()).andReturn(mockConfiguration);
        expect(mockConfiguration.getProperty(TransformerFactoryAllocator.PROPERTY_TRANSFORMER_ATTRIBUTES)).andReturn(testAttributes);

        replay(mockBrokerPool, mockConfiguration);

        SAXTransformerFactory transformerFactory = TransformerFactoryAllocator.getTransformerFactory(mockBrokerPool);
        assertEquals(transformerFactoryClass, transformerFactory.getClass().getName());

        verify(mockBrokerPool, mockConfiguration);
    }
}
