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
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;

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
import static org.exist.test.Util.executeQuery;
import static org.exist.test.Util.withCompiledQuery;
import static org.exist.util.MapUtil.HashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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

    @ClassRule
    public static ExistEmbeddedServer EXIST_EMBEDDED_SERVER = new ExistEmbeddedServer(null, getConfigFile(), null, true, true);

    @Test
    public void callStaticMethod() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
            "m:sin(3.2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.DOUBLE, queryResult.getType());
            assertEquals(-0.058374143427580086, queryResult.toJavaObject(double.class), 0);

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethod() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('m:sin'), 1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethodVarArgs0() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertFalse(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs1() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethodVarArgs1() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs1Sequence() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a'))"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs1Array() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a' })"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs2() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a', 'b')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs2Sequence() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a', 'b'))"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs2Array() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a', 'b' })"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethodVarArgs2() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 3)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs3() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', 'a', 'b', 'c')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b,c", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs3Sequence() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', ('a', 'b', 'c'))"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b,c", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodVarArgs3Array() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:join(',', array { 'a', 'b', 'c' })"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("a,b,c", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethodVarArgs3() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:join'), 4)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodArrayParam() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "arys:copyOf(array { 'a', 'b', 'c' }, 2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.ARRAY, queryResult.getType());
            assertArrayEquals(new String[] {"a", "b"}, queryResult.toJavaObject(String[].class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodArrayParamVarRefExplicitType() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "let $a1 as array(xs:string) := array { 'a', 'b', 'c' }\n" +
                "return\n" +
                "arys:copyOf($a1, 2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.ARRAY, queryResult.getType());
            assertArrayEquals(new String[] {"a", "b"}, queryResult.toJavaObject(String[].class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodArrayParamVarRefImplicitType() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace arys = 'java:java.util.Arrays';\n" +
                "let $a1 := array { 'a', 'b', 'c' }\n" +
                "return\n" +
                "arys:copyOf($a1, 2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.ARRAY, queryResult.getType());
            assertArrayEquals(new String[] {"a", "b"}, queryResult.toJavaObject(String[].class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodMapParam() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "collections:unmodifiableMap(map { 'a': 1, 'b': 2, 'c': 3 })"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.MAP, queryResult.getType());
            assertEquals(HashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), queryResult.toJavaObject(Map.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodMapParamVarRefExplicitType() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "let $m1 as map(xs:string, xs:integer) := map { 'a': 1, 'b': 2, 'c': 3 }\n" +
                "return\n" +
                "collections:unmodifiableMap($m1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.MAP, queryResult.getType());
            assertEquals(HashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), queryResult.toJavaObject(Map.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodMapParamVarRefImplicitType() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace collections = 'java:java.util.Collections';\n" +
                "let $m1 := map { 'a': 1, 'b': 2, 'c': 3 }\n" +
                "return\n" +
                "collections:unmodifiableMap($m1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.MAP, queryResult.getType());
            assertEquals(HashMap(Tuple("a", BigInteger.valueOf(1)), Tuple("b", BigInteger.valueOf(2)), Tuple("c", BigInteger.valueOf(3))), queryResult.toJavaObject(Map.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticMethodArrayParam() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:valueOf'), 1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callInstanceMethodReturnArray() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "let $jstr := s:new('hello')\n" +
                "return\n" +
                "s:toCharArray($jstr)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.ARRAY, queryResult.getType());
            assertArrayEquals(new byte[] { 'h', 'e', 'l', 'l', 'o' }, queryResult.toJavaObject(byte[].class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableInstanceMethodReturnArray() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:toCharArray'), 1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodFloat() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:next-up(xs:float(1.7))"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.FLOAT, queryResult.getType());
            assertEquals(1.7000002f, queryResult.toJavaObject(float.class), 0);

            transaction.commit();
        }
    }

    @Test
    public void callStaticMethodDouble() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:next-up(xs:double(1.7))"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.DOUBLE, queryResult.getType());
            assertEquals(1.7000000000000002d, queryResult.toJavaObject(double.class), 0);

            transaction.commit();
        }
    }

    @Test
    public void callStaticField() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "m:PI()"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.DOUBLE, queryResult.getType());
            assertEquals(3.14159265358979323846, queryResult.toJavaObject(double.class), 0);

            transaction.commit();
        }
    }

    @Test
    public void callInstanceField() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace holder = 'java:org.exist.util.Holder';\n" +
                "let $obj := holder:new(fn:true())\n" +
                "return\n" +
                "holder:value($obj)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableStaticField() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace m = 'java:java.lang.Math';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('m:PI'), 0)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callStringConstructor() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "s:new('hello world')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("hello world", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void callListConstructor() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace list = 'java:java.util.ArrayList';\n" +
                "list:new()"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.JAVA_OBJECT, queryResult.getType());
            assertEquals(new ArrayList<>(), queryResult.toJavaObject(List.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableInstanceMethod() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:concat'), 2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callInstanceMethod() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "let $jstr := s:new('hello world')\n" +
                "return\n" +
                "s:concat($jstr, ' everyone')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("hello world everyone", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void functionAvailableConstructor() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace s = 'java:java.lang.String';\n" +
                "import module namespace system = 'http://exist-db.org/xquery/system';\n" +
                "system:function-available(xs:QName('s:new'), 1)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.BOOLEAN, queryResult.getType());
            assertTrue(queryResult.toJavaObject(boolean.class));

            transaction.commit();
        }
    }

    @Test
    public void callVoidMethodReturnThis() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace uspa = 'java:org.exist.security.internal.aider.UnixStylePermissionAider?void=this';\n" +
                "let $aider := uspa:new()\n" +
                "let $aider2 := uspa:setOwnerMode($aider, 7)\n" +
                "return\n" +
                "uspa:getOwnerMode($aider2)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.INTEGER, queryResult.getType());
            assertEquals(7, (int) queryResult.toJavaObject(int.class));

            transaction.commit();
        }
    }

    @Test
    public void buildJavaList() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
                "declare namespace list = 'java:java.util.ArrayList';\n" +
            "let $list := list:new()\n" +
            "let $actions := (list:add($list, 'apples'), list:add($list, 'bananas'), list:add($list, 'cherries'))\n" +
            "return\n" +
            "fn:string-join((list:get($list, 2), list:get($list, 0), list:get($list, 1)), '.')"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("cherries.apples.bananas", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void buildJavaListWithVoidReturnThis() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final StringSource query = new StringSource(
            "declare namespace list = 'java:java.util.ArrayList?void=this';\n" +
                "let $list := list:new()\n" +
                "let $_ := list:add($list, 'apples')\n" +
                "let $list := list:clear($list)\n" +
                "let $_ := list:add($list, 'cherries')\n" +
                "return\n" +
                "list:get($list, 0)"
        );

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Item queryResult = withCompiledQuery(broker, query, compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertTrue(result.hasOne());
                return result.itemAt(0);
            });

            assertEquals(Type.STRING, queryResult.getType());
            assertEquals("cherries", queryResult.toJavaObject(String.class));

            transaction.commit();
        }
    }

    @Test
    public void xqueryKebabCaseToJavaCamelCase() {
        assertEquals("nextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("nextUp"));
        assertEquals("nextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("next-up"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("otherNextUp"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("other-next-up"));
        assertEquals("otherNextUp", JavaBinding.xqueryKebabCaseToJavaCamelCase("other-NextUp"));
    }
}
