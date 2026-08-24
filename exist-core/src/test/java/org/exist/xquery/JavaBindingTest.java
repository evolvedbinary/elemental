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
package org.exist.xquery;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.util.MapUtil.hashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class JavaBindingTest {

    private static Path getConfigFile() {
        final ClassLoader loader = JavaBindingTest.class.getClassLoader();
        final char separator = System.getProperty("file.separator").charAt(0);
        final String packagePath = JavaBindingTest.class.getPackage().getName().replace('.', separator);

        try {
            return Paths.get(loader.getResource(packagePath + separator + "JavaBindingTest.conf.xml").toURI());
        } catch (final URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @RegisterExtension
    public static EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(null, getConfigFile(), null, true, true);

    @Test
    void callStaticMethod(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
            "m:sin(3.2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.DOUBLE, item.getType());
                assertEquals(-0.058374143427580086, item.toJavaObject(double.class), 0);
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethod(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('m:sin'), 1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethodVarArgs0(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertFalse(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs1(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethodVarArgs1(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs1Sequence(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a'))"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs1Array(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a' })"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs2(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a', 'b')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs2Sequence(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a', 'b'))"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs2Array(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a', 'b' })"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethodVarArgs2(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 3)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs3(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a', 'b', 'c')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b,c", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs3Sequence(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a', 'b', 'c'))"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b,c", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodVarArgs3Array(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a', 'b', 'c' })"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("a,b,c", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethodVarArgs3(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 4)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodArrayParam(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "arys:copyOf(array { 'a', 'b', 'c' }, 2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.ARRAY_ITEM, item.getType());
                assertArrayEquals(new String[] {"a", "b"}, item.toJavaObject(String[].class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodArrayParamVarRefExplicitType(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "let $a1 as array(xs:string) := array { 'a', 'b', 'c' }\n" +
                "return\n" +
                "arys:copyOf($a1, 2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.ARRAY_ITEM, item.getType());
                assertArrayEquals(new String[] {"a", "b"}, item.toJavaObject(String[].class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodArrayParamVarRefImplicitType(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "let $a1 := array { 'a', 'b', 'c' }\n" +
                "return\n" +
                "arys:copyOf($a1, 2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.ARRAY_ITEM, item.getType());
                assertArrayEquals(new String[] {"a", "b"}, item.toJavaObject(String[].class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodMapParam(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "collections:unmodifiableMap(map { 'a': 1, 'b': 2, 'c': 3 })"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.MAP_ITEM, item.getType());
                assertEquals(hashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), item.toJavaObject(Map.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodMapParamVarRefExplicitType(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "let $m1 as map(xs:string, xs:integer) := map { 'a': 1, 'b': 2, 'c': 3 }\n" +
                "return\n" +
                "collections:unmodifiableMap($m1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.MAP_ITEM, item.getType());
                assertEquals(hashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), item.toJavaObject(Map.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodMapParamVarRefImplicitType(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "let $m1 := map { 'a': 1, 'b': 2, 'c': 3 }\n" +
                "return\n" +
                "collections:unmodifiableMap($m1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.MAP_ITEM, item.getType());
                assertEquals(hashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), item.toJavaObject(Map.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticMethodArrayParam(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:valueOf'), 1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callInstanceMethodReturnArray(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "let $jstr := s:new('hello')\n" +
                "return\n" +
                "s:toCharArray($jstr)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.ARRAY_ITEM, item.getType());
                assertArrayEquals(new byte[] { 'h', 'e', 'l', 'l', 'o' }, item.toJavaObject(byte[].class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableInstanceMethodReturnArray(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:toCharArray'), 1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodFloat(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:next-up(xs:float(1.7))"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.FLOAT, item.getType());
                assertEquals(1.7000002f, item.toJavaObject(float.class), 0);
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticMethodDouble(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:next-up(xs:double(1.7))"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.DOUBLE, item.getType());
                assertEquals(1.7000000000000002d, item.toJavaObject(double.class), 0);
            }

            transaction.commit();
        }
    }

    @Test
    void callStaticField(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:PI()"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.DOUBLE, item.getType());
                assertEquals(3.14159265358979323846, item.toJavaObject(double.class), 0);
            }

            transaction.commit();
        }
    }

    @Test
    void callInstanceField(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace holder = 'java:org.exist.util.Holder';\n" +
                "let $obj := holder:new(fn:true())\n" +
                "return\n" +
                "holder:value($obj)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableStaticField(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('m:PI'), 0)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callStringConstructor(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:new('hello world')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("hello world", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callListConstructor(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace list = 'java:java.util.ArrayList';\n" +
                "list:new()"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.JAVA_OBJECT, item.getType());
                assertEquals(new ArrayList<>(), item.toJavaObject(List.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableInstanceMethod(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:concat'), 2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callInstanceMethod(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "let $jstr := s:new('hello world')\n" +
                "return\n" +
                "s:concat($jstr, ' everyone')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("hello world everyone", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void functionAvailableConstructor(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:new'), 1)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.BOOLEAN, item.getType());
                assertTrue(item.toJavaObject(boolean.class));
            }

            transaction.commit();
        }
    }

    @Test
    void callVoidMethodReturnThis(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace uspa = 'java:org.exist.security.internal.aider.UnixStylePermissionAider?void=this';\n" +
                "let $aider := uspa:new()\n" +
                "let $aider2 := uspa:setOwnerMode($aider, 7)\n" +
                "return\n" +
                "uspa:getOwnerMode($aider2)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.INTEGER, item.getType());
                assertEquals(7, (int) item.toJavaObject(int.class));
            }

            transaction.commit();
        }
    }

    @Test
    void buildJavaList(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
                "declare namespace list = 'java:java.util.ArrayList';\n" +
            "let $list := list:new()\n" +
            "let $actions := (list:add($list, 'apples'), list:add($list, 'bananas'), list:add($list, 'cherries'))\n" +
            "return\n" +
            "fn:string-join((list:get($list, 2), list:get($list, 0), list:get($list, 1)), '.')"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("cherries.apples.bananas", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void buildJavaListWithVoidReturnThis(final BrokerPool brokerPool) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace list = 'java:java.util.ArrayList?void=this';\n" +
                "let $list := list:new()\n" +
                "let $_ := list:add($list, 'apples')\n" +
                "let $list := list:clear($list)\n" +
                "let $_ := list:add($list, 'cherries')\n" +
                "return\n" +
                "list:get($list, 0)"
        );

        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, query, false, null, null, null, null, null)) {
                Assertions.assertTrue(queryResult.result.hasOne());
                final Item item = queryResult.result.itemAt(0);

                assertEquals(Type.STRING, item.getType());
                assertEquals("cherries", item.toJavaObject(String.class));
            }

            transaction.commit();
        }
    }

    @Test
    void xqueryKebabCaseToJavaCamelCase() {
        assertEquals("nextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("nextUp"));
        assertEquals("nextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("next-up"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("otherNextUp"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("other-next-up"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("other-NextUp"));
    }
}
