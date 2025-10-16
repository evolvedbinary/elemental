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
package org.exist.xmldb;

import com.evolvedbinary.j8fu.Either;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedValueRep.value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class XMLDBExternalVariableTest {

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }

    @Parameterized.Parameter
    public String apiName;

    @Parameterized.Parameter(value = 1)
    public String baseUri;

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @Test
    public void queryPostWithExternalVariableUntypedNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, null, (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }
    
    @Test
    public void queryPostWithExternalVariableUntypedSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    private void queryPostWithExternalVariable(@Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(null, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(@Nullable final ErrorCodes.ErrorCode expectedResponseCode, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<ErrorCodes.ErrorCode, String> expectedResponse, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(expectedResponse, externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable((ErrorCodes.ErrorCode) null, expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(@Nullable final ErrorCodes.ErrorCode expectedResponseCode, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<ErrorCodes.ErrorCode, String> expectedResponse, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        @Nullable final Object[] externalVariableValue = buildExternalVariableValue(externalVariableSequence);
        final String query = buildQueryExternalVariable(xqExternalVariableType);
        final Either<XMLDBException, ResourceSet> response = doPostWithAuth(externalVariableValue, query);

        if (expectedResponse._1 == null) {
            // We expect success
            assertTrue(response.isRight());
            final ResourceSet actualResultSet = response.right().get();

            final List<Tuple2<String, Object>> expectedResults = buildExistVariableResultSequence(expectedResult);

            assertEquals(expectedResults.size(), actualResultSet.getSize());
            for (int i = 0; i < expectedResults.size(); i++) {
                final Tuple2<String, Object> expected = expectedResults.get(i);
                final EXistResource actual = (EXistResource) actualResultSet.getResource(i);

                if (expected._1 != null) {
                    assertEquals(expected._1, actual.getTypeName());
                }
                assertEquals(expected._2, actual.getContent());
            }

        } else {
            // We expect an error, so check the error is the expected one
            assertTrue(response.isLeft());
            final XMLDBException errorResponse = response.left().get();
            assertTrue(errorResponse.getCause() instanceof XPathException);
            final XPathException errorResponseXPathException = (XPathException) errorResponse.getCause();
            assertEquals(expectedResponse._1, errorResponseXPathException.getErrorCode());

            // TODO(AR) what about expected response?
            if (expectedResponse._2 != null) {
                fail("TODO(AR) implement expectedResponse._2");
            }
        }
    }

    private static List<Tuple2<String, Object>> buildExistVariableResultSequence(final ExternalVariableValueRep... resultSequence) {
        final List<Tuple2<String, Object>> results = new ArrayList<>();
        for (final ExternalVariableValueRep resultItem : resultSequence) {

            @Nullable String type = null;
            Object value = null;

            if (resultItem instanceof TypedValueRep) {
                type = Type.getTypeName(((TypedValueRep) resultItem).getXdmType());
            }

            if (resultItem instanceof ValueRep) {
                value = ((ValueRep) resultItem).getContent();
            }

            results.add(Tuple(type, value));
        }
        return results;
    }

    private Either<XMLDBException, ResourceSet> doPostWithAuth(@Nullable final Object[] externalVariableValue, final String query) throws XMLDBException {
        try (final Collection dbCollection = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final XQueryService xqueryService = (XQueryService) dbCollection.getService("XQueryService", "1.0");

            if (externalVariableValue != null) {
                if (externalVariableValue.length == 1) {
                    // unbox an array of 1 item
                    xqueryService.declareVariable("local:my-variable", externalVariableValue[0]);
                } else {
                    xqueryService.declareVariable("local:my-variable", externalVariableValue);
                }
            }

            final CompiledExpression compiled = xqueryService.compile(query);
            try {
                return Either.Right(xqueryService.execute(compiled));
            } catch (final XMLDBException e) {
                return Either.Left(e);
            }
        }
    }

    private static @Nullable Object[] buildExternalVariableValue(@Nullable final ExternalVariableValueRep... externalVariableSequence) {
        if (externalVariableSequence == null) {
            return null;
        }

        final Object[] values = new Object[externalVariableSequence.length];
        for (int i = 0; i < externalVariableSequence.length; i++) {
            final ExternalVariableValueRep externalVariableItem = externalVariableSequence[i];

            int type = Type.ITEM;
            if (externalVariableItem instanceof TypedValueRep) {
                type = ((TypedValueRep) externalVariableItem).getXdmType();
            }

            if (externalVariableItem instanceof ValueRep) {
                values[i] = ((ValueRep) externalVariableItem).getContent();
            } else {
                throw new UnsupportedOperationException("TODO(AR) implement type conversion");
            }
        }
        return values;
    }

    private static String buildQueryExternalVariable(@Nullable final String xqExternalVariableType) {
        final StringBuilder builder = new StringBuilder();

        builder.append("declare variable $local:my-variable");
        if (xqExternalVariableType != null) {
            builder.append(" as ").append(xqExternalVariableType);
        }
        builder.append(" external;\n");
        builder.append("$local:my-variable\n");

        return builder.toString();
    }

    interface ExternalVariableValueRep {};
    interface ExternalVariableUntypedValueRep extends ExternalVariableValueRep {}
    interface ExternalVariableTypedValueRep extends ExternalVariableUntypedValueRep {
        int getXdmType();
    }

    private interface ValueRep extends ExternalVariableValueRep {
        String getContent();
    }

    private interface NamedValueRep extends ValueRep {
        String getName();
    }

    static class UntypedValueRep implements ValueRep, ExternalVariableUntypedValueRep {
        private final String content;

        public static UntypedValueRep value(final String content) {
            return new UntypedValueRep(content);
        }

        private UntypedValueRep(final String content) {
            this.content = content;
        }

        @Override
        public String getContent() {
            return content;
        }
    }

    static class TypedValueRep extends UntypedValueRep implements ExternalVariableTypedValueRep {
        final int xdmType;

        public static TypedValueRep value(final int xdmType, final String content) {
            return new TypedValueRep(xdmType, content);
        }

        private TypedValueRep(final int xdmType, final String content) {
            super(content);
            this.xdmType = xdmType;
        }

        @Override
        public int getXdmType() {
            return xdmType;
        }
    }
}
