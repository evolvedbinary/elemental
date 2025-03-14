/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import org.exist.util.Str;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class NamespaceSupportTest {

    @Test
    public void pushAndPop() {
        final NamespaceSupport nsSupport = new NamespaceSupport();
        nsSupport.pushContext();
        assertThat(nsSupport.getURI(Str.of("xml")), equalTo("http://www.w3.org/XML/1998/namespace"));
        assertThat(nsSupport.getURI(Str.of("")), equalTo(null));
        nsSupport.declarePrefix(Str.of(""), "http://www.w3.org/XML/2023/empty1");
        nsSupport.declarePrefix(Str.of("testprefix"), "http://www.w3.org/XML/2023/testprefix1");
        assertThat(nsSupport.getURI(Str.of("")), equalTo("http://www.w3.org/XML/2023/empty1"));
        assertThat(nsSupport.getURI(Str.of("testprefix")), equalTo("http://www.w3.org/XML/2023/testprefix1"));
        nsSupport.pushContext();
        assertThat(nsSupport.declarePrefix(Str.of(""), "http://www.w3.org/XML/2023/empty2"), equalTo(true));
        assertThat(nsSupport.declarePrefix(Str.of("testprefix"), "http://www.w3.org/XML/2023/testprefix2"), equalTo(true));
        nsSupport.declarePrefix(Str.of("level2"), "http://www.w3.org/XML/2023/level2");
        assertThat(nsSupport.declarePrefix(Str.of("xmlns"), "http://www.w3.org/XML/2023/level2"), equalTo(false));
        assertThat(nsSupport.declarePrefix(Str.of("xml"), "http://www.w3.org/XML/2023/level2"), equalTo(false));
        assertThat(nsSupport.getURI(Str.of("")), equalTo("http://www.w3.org/XML/2023/empty2"));
        assertThat(nsSupport.getURI(Str.of("testprefix")), equalTo("http://www.w3.org/XML/2023/testprefix2"));
        assertThat(nsSupport.getURI(Str.of("level2")), equalTo("http://www.w3.org/XML/2023/level2"));
        nsSupport.popContext();
        assertThat(nsSupport.getURI(Str.of("")), equalTo("http://www.w3.org/XML/2023/empty1"));
        assertThat(nsSupport.getURI(Str.of("testprefix")), equalTo("http://www.w3.org/XML/2023/testprefix1"));
        assertThat(nsSupport.getURI(Str.of("level2")), equalTo(null));
        nsSupport.popContext();
    }

}
