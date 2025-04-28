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
package org.exist.xquery.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class URIUtilsTest {

    /**
     * Unreserved Characters from <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.3">RFC 3986 Section 2.3</a>.
     */
    @Test
    void encodeForURIPathComponentUnreserved() {
        // alpha
        String encoded = URIUtils.encodeForURI("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", encoded);

        // digit
        encoded = URIUtils.encodeForURI("0123456789");
        assertEquals("0123456789", encoded);

        // hyphen
        encoded = URIUtils.encodeForURI("dash-case");
        assertEquals("dash-case", encoded);

        // full-stop
        encoded = URIUtils.encodeForURI("file.ext");
        assertEquals("file.ext", encoded);

        // underscore
        encoded = URIUtils.encodeForURI("snake_case");
        assertEquals("snake_case", encoded);

        // tilde
        encoded = URIUtils.encodeForURI("~home");
        assertEquals("~home", encoded);
    }

    /**
     * General Delimiters from <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.2">RFC 3986 Section 2.2</a>.
     */
    @Test
    void encodeForURIPathComponentGeneralDelimiter() {
        // colon
        String encoded = URIUtils.encodeForURI("a:b");
        assertEquals("a%3Ab", encoded);

        // forward slash
        encoded = URIUtils.encodeForURI("x/y");
        assertEquals("x%2Fy", encoded);

        // question mark
        encoded = URIUtils.encodeForURI("Goodbye?");
        assertEquals("Goodbye%3F", encoded);

        // hash
        encoded = URIUtils.encodeForURI("#comment");
        assertEquals("%23comment", encoded);

        // opening square bracket
        encoded = URIUtils.encodeForURI("[predicate");
        assertEquals("%5Bpredicate", encoded);

        // closing square bracket
        encoded = URIUtils.encodeForURI("predicate]");
        assertEquals("predicate%5D", encoded);

        // at symbol
        encoded = URIUtils.encodeForURI("adam@work");
        assertEquals("adam%40work", encoded);
    }

    /**
     * Sub Delimiters from <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.2">RFC 3986 Section 2.2</a>.
     */
    @Test
    void encodeForURIPathComponentSubDelimiter() {
        // exclamation mark
        String encoded = URIUtils.encodeForURI("Hello!");
        assertEquals("Hello%21", encoded);

        // dollar sign
        encoded = URIUtils.encodeForURI("$100");
        assertEquals("%24100", encoded);

        // ampersand
        encoded = URIUtils.encodeForURI("Jack&Jill");
        assertEquals("Jack%26Jill", encoded);

        // single quote
        encoded = URIUtils.encodeForURI("it's");
        assertEquals("it%27s", encoded);

        // opening bracket
        encoded = URIUtils.encodeForURI("(comment");
        assertEquals("%28comment", encoded);

        // closing bracket
        encoded = URIUtils.encodeForURI("comment)");
        assertEquals("comment%29", encoded);

        // asterisk
        encoded = URIUtils.encodeForURI("1*2");
        assertEquals("1%2A2", encoded);

        // plus sign
        encoded = URIUtils.encodeForURI("1+2");
        assertEquals("1%2B2", encoded);

        // comma
        encoded = URIUtils.encodeForURI("x,y");
        assertEquals("x%2Cy", encoded);

        // semi-colon
        encoded = URIUtils.encodeForURI("a;b");
        assertEquals("a%3Bb", encoded);

        // equals sign
        encoded = URIUtils.encodeForURI("n=1");
        assertEquals("n%3D1", encoded);
    }

    @Test
    void encodeForURIPathComponent() {
        // path
        String encoded = URIUtils.encodeForURI("/db/a/b/c");
        assertEquals("%2Fdb%2Fa%2Fb%2Fc", encoded);

        // space
        encoded = URIUtils.encodeForURI("hello world");
        assertEquals("hello%20world", encoded);

        // percent sign
        encoded = URIUtils.encodeForURI("99%");
        assertEquals("99%25", encoded);

        // percent sign
        encoded = URIUtils.encodeForURI("%2F");
        assertEquals("%252F", encoded);

        // double percent sign
        encoded = URIUtils.encodeForURI("99%%100");
        assertEquals("99%25%25100", encoded);
    }

    @Test
    void encodeForURIPathComponentUtf8() {
        // 2 byte character - yen sign
        String encoded = URIUtils.encodeForURI("¥");
        assertEquals("%C2%A5", encoded);

        // 3 byte character - samaritan letter tsasdiy
        encoded = URIUtils.encodeForURI("ࠑ");
        assertEquals("%E0%A0%91", encoded);

        // 4 byte character - phoenician letter het
        encoded = URIUtils.encodeForURI("\uD802\uDD07");
        assertEquals("%F0%90%A4%87", encoded);
    }
}
