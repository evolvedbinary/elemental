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
package org.exist.xquery.functions.fn;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FnInvisibleXmlTest {

    // xquery version "3.1";

    // let $date-grammar := " date = year, -'-', month, -'-', day .
    // year = d, d, d, d .
    // month = '0', d | '1', ['0'|'1'|'2'] .
    // day = ['0'|'1'|'2'], d | '3', ['0'|'1'] .
    // -d = ['0'-'9'] ."

    // let $valid-date-input := "2023-10-31"

    // let $invalid-date-input := "2023-10-32"

    // let $alphabit-grammar := "S=A. A='a'."

    // let $alphabit-valid-input := "a"
    // (:expected <S><A>a</A></S> :)
    // let $alphabit-invalid-input := "b"
    // (:let $result := $parser("b"):)
    // (:return $result/*/@*:state = 'failed':)
    // (:expected true() :)
    // (:FOIX0002 when fail-on is true:)

    // (:let $parser := fn:invisible-xml($grammar, map { }):)
    // let $parser-fail-on := fn:invisible-xml($alphabit-grammar, map {
    // "fail-on-error": true() })

    // return $parser-fail-on($alphabit-invalid-input)

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static final String DATE_GRAMMAR = " date = year, -'-', month, -'-', day .\n" +
            " year = d, d, d, d .\n" +
            "month = '0', d | '1', ['0'|'1'|'2'] .\n" +
            "  day = ['0'|'1'|'2'], d | '3', ['0'|'1'] .\n" +
            "   -d = ['0'-'9'] .";
    private static final String DATE_VALID_INPUT = "2023-10-31";
    private static final String DATE_INVALID_INPUT = "2023-10-32";

    private static final String ALPHABIT_GRAMMAR = "S=A. A='a'.";
    private static final String ALPHABIT_VALID_INPUT = "a";
    private static final String ALPHABIT_INVALID_INPUT = "b";

    private static final String NO_OPTIONS = "map { }";
    private static final String FAIL_ON_ERROR_OPTIONS = "map { \"fail-on-error\": true() }";

    @Test
    public void dateValidInput() throws XMLDBException {
        final String result = existEmbeddedServer
                .executeOneValue(parseQuery(DATE_GRAMMAR, NO_OPTIONS, DATE_VALID_INPUT));
        assertEquals("<date><year>2023</year><month>10</month><day>31</day></date>", result);
    }

    @Test
    public void dateInvalidInput() throws XMLDBException {
        final String result = existEmbeddedServer
                .executeOneValue(failedStateQuery(DATE_GRAMMAR, NO_OPTIONS, DATE_INVALID_INPUT));
        assertEquals("true", result);
    }

    @Test
    public void dateInvalidInputFailOnError() {
        assertError("FOIX0002", parseQuery(DATE_GRAMMAR, FAIL_ON_ERROR_OPTIONS, DATE_INVALID_INPUT));
    }

    @Test
    public void alphabitValidInput() throws XMLDBException {
        final String result = existEmbeddedServer
                .executeOneValue(parseQuery(ALPHABIT_GRAMMAR, NO_OPTIONS, ALPHABIT_VALID_INPUT));
        assertEquals("<S><A>a</A></S>", result);
    }

    @Test
    public void alphabitValidInputFailOnError() throws XMLDBException {
        final String result = existEmbeddedServer
                .executeOneValue(parseQuery(ALPHABIT_GRAMMAR, FAIL_ON_ERROR_OPTIONS, ALPHABIT_VALID_INPUT));
        assertEquals("<S><A>a</A></S>", result);
    }

    @Test
    public void alphabitInvalidInput() throws XMLDBException {
        final String result = existEmbeddedServer
                .executeOneValue(failedStateQuery(ALPHABIT_GRAMMAR, NO_OPTIONS, ALPHABIT_INVALID_INPUT));
        assertEquals("true", result);
    }

    @Test
    public void alphabitInvalidInputFailOnError() {
        assertError("FOIX0002", parseQuery(ALPHABIT_GRAMMAR, FAIL_ON_ERROR_OPTIONS, ALPHABIT_INVALID_INPUT));
    }

    /**
     * Builds a query that creates a parser from the grammar and options, and
     * applies it to the input.
     */
    private static String parseQuery(final String grammar, final String options, final String input) {
        return "let $parser := fn:invisible-xml(" + stringLiteral(grammar) + ", " + options + ")\n" +
                "return fn:serialize($parser(" + stringLiteral(input) + "))";
    }

    /**
     * Builds a query that checks whether parsing the input produced a failed state.
     */
    private static String failedStateQuery(final String grammar, final String options, final String input) {
        return "let $parser := fn:invisible-xml(" + stringLiteral(grammar) + ", " + options + ")\n" +
                "let $result := $parser(" + stringLiteral(input) + ")\n" +
                "return $result/*/@*:state = 'failed'";
    }

    private static String stringLiteral(final String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void assertError(final String errorCode, final String query) {
        try {
            existEmbeddedServer.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(errorCode));
            return;
        }

        fail("Expected XPathException: err:" + errorCode);
    }
}
