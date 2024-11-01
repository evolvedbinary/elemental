/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
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
package org.exist.xquery.value;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class DateTimeTypesTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer server = new ExistXmldbEmbeddedServer(true, true, true);

    @Test
    public void compareDateTimeWithDateTimeStamp() throws XMLDBException {
        final String result = server.executeOneValue("xs:dateTime('2024-10-07T09:56:00+01:00') <= current-dateTime()");
        assertEquals("true", result);
    }

    @Test
    public void compareDateTimeStampWithDateTime() throws XMLDBException {
        final String result = server.executeOneValue("current-dateTime() <= xs:dateTime('2024-10-07T09:56:00+01:00')");
        assertEquals("false", result);
    }
}
