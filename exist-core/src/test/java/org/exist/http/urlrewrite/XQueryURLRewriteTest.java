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
package org.exist.http.urlrewrite;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.junittoolbox.ParallelRunner;
import org.easymock.EasyMock;
import jakarta.servlet.http.HttpServletRequest;
import org.exist.http.urlrewrite.XQueryURLRewrite.RequestWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 *
 * @author aretter
 */
@RunWith(ParallelRunner.class)
public class XQueryURLRewriteTest
{
    @Test
    public void adjustPathForSourceLookup_fullXmldbUri() {
        XQueryURLRewrite rewriter = new XQueryURLRewrite();


        String basePath = "xmldb:exist:///db/adamretter.org.uk/";
        String path = "/db/adamretter.org.uk/blog/entries/some-entry.xml?edit";

        String adjustedPath = rewriter.adjustPathForSourceLookup(basePath, path);

        assertEquals(adjustedPath, "blog/entries/some-entry.xml?edit");
    }

    @Test
    public void adjustPathForSourceLookup_dbUri() {
        XQueryURLRewrite rewriter = new XQueryURLRewrite();


        String basePath = "/";
        String path = "/db/adamretter.org.uk/blog/entries/some-entry.xml?edit";

        String adjustedPath = rewriter.adjustPathForSourceLookup(basePath, path);

        assertEquals(adjustedPath, "adamretter.org.uk/blog/entries/some-entry.xml?edit");
    }

    @Test
    public void adjustPathForSourceLookup_fsUri() {
        XQueryURLRewrite rewriter = new XQueryURLRewrite();


        String basePath = "/";
        String path = "/xquery/functions.xql";

        String adjustedPath = rewriter.adjustPathForSourceLookup(basePath, path);

        assertEquals(adjustedPath, "xquery/functions.xql");
    }

    @Test
    public void requestWrapper_copiesRequestParams() {

        final Map<String, String[]> testParameterMap = new HashMap<String, String[]>();
        testParameterMap.put("paramName1", new String[] {"value1", "value1.1"});
        testParameterMap.put("paramName2", new String[] {"value2", "value2.1"});

        HttpServletRequest mockHttpServletRequest = EasyMock.createMock(HttpServletRequest.class);

        //standard request wrapper stuff
        expect(mockHttpServletRequest.getContentType()).andReturn("text/xml");
        //end standard request wrapper stuff

        expect(mockHttpServletRequest.getParameterMap()).andReturn(testParameterMap);


        replay(mockHttpServletRequest);
        RequestWrapper wrapper = new RequestWrapper(mockHttpServletRequest);
        verify(mockHttpServletRequest);

        assertEquals(testParameterMap.size(), wrapper.getParameterMap().size());

        for(String paramName : testParameterMap.keySet()) {
            assertArrayEquals(testParameterMap.get(paramName), wrapper.getParameterMap().get(paramName));
        }
    }

    @Test
    public void requestWrapper_addsParamAftercopiesRequestParams() {

        final Map<String, String[]> testParameterMap = new HashMap<String, String[]>();
        testParameterMap.put("paramName1", new String[] {"value1", "value1.1"});
        testParameterMap.put("paramName2", new String[] {"value2", "value2.1"});

        final String newRequestParamName = "newParamName";
        final String newRequestParamValue = "newParamValue";

        HttpServletRequest mockHttpServletRequest = EasyMock.createMock(HttpServletRequest.class);

        //standard request wrapper stuff
        expect(mockHttpServletRequest.getContentType()).andReturn("text/xml");
        //end standard request wrapper stuff

        expect(mockHttpServletRequest.getParameterMap()).andReturn(testParameterMap);


        replay(mockHttpServletRequest);
        RequestWrapper wrapper = new RequestWrapper(mockHttpServletRequest);
        wrapper.addParameter(newRequestParamName, newRequestParamValue);
        verify(mockHttpServletRequest);

        final Map<String, String[]> newTestParameterMap = new HashMap<String, String[]>();
        newTestParameterMap.putAll(testParameterMap);
        newTestParameterMap.put(newRequestParamName, new String[] {newRequestParamValue });

        assertEquals(newTestParameterMap.size(), wrapper.getParameterMap().size());

        for(String paramName : newTestParameterMap.keySet()) {
            assertArrayEquals(newTestParameterMap.get(paramName), wrapper.getParameterMap().get(paramName));
        }
    }
}