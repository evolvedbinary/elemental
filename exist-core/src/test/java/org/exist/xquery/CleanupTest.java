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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.xquery;

import org.exist.dom.QName;
import org.exist.dom.persistent.DocumentSet;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.EXistXQueryService;
import org.exist.xmldb.LocalCompiledExpression;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Sequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test if inline functions and functions defined in imported modules are properly reset.
 *
 * @author Wolfgang
 */
public class CleanupTest {

    private final static String MODULE_NS = "http://exist-db.org/test";

    private final static String TEST_MODULE = "module namespace t=\"" + MODULE_NS + "\";\n" +
            "declare variable $t:VAR := 123;\n" +
            "declare variable $t:VAR2 := 456;\n" +
            "declare function t:test($a) { $a || $t:VAR };\n" +
            "declare function t:inline($a) { function() { $a } };";

    private final static String TEST_QUERY = "import module namespace t=\"" + MODULE_NS + "\" at " +
            "\"xmldb:exist:///db/test/test-module.xql\";" +
            "t:test('Hello world')";

    private final static String TEST_INLINE = "let $a := \"a\"\n" +
            "let $func := function() { $a }\n" +
            "return\n" +
            "   $func";

    private final static String INTERNAL_MODULE_TEST = "import module namespace tt=\"" + MODULE_NS + "\" at " +
            "\"java:org.exist.xquery.TestModule\";" +
            "tt:test()";

    private final static String INTERNAL_MODULE_EVAL_TEST = "import module namespace tt=\"" + MODULE_NS + "\" at " +
            "\"java:org.exist.xquery.TestModule\";" +
            "util:eval('123')," +
            "tt:test()";

    private Collection collection;

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeEach
    void setup() throws XMLDBException {
        final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        collection = service.createCollection("test");
        try (final Resource doc = collection.createResource("test-module.xql", BinaryResource.class)) {
            doc.setContent(TEST_MODULE);
            ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
            collection.storeResource(doc);
        }
    }

    @AfterEach
    void tearDown() throws XMLDBException {
        collection.close();
        final CollectionManagementService service =
                XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        service.removeCollection("test");
    }

    @Test
    void resetStateOfModuleVars() throws XMLDBException, XPathException {
        final EXistXQueryService service = collection.getService(EXistXQueryService.class);

        final LocalCompiledExpression compiledExpression = (LocalCompiledExpression) service.compile(TEST_QUERY);
        final XQueryUtil.CompilationResult compilationResult = compiledExpression.getCompilationResult();
        final CompiledXQuery compiledXquery = compilationResult.compiledXquery;


        final Module[] modules = compiledXquery.getContext().getModules(MODULE_NS);
        assertEquals(1, modules.length);
        final Module module = modules[0];
        final java.util.Collection<VariableDeclaration> varDecls = ((ExternalModule) module).getVariableDeclarations();
        final Iterator<VariableDeclaration> vi = varDecls.iterator();
        final VariableDeclaration var1 = vi.next();
        final VariableDeclaration var2 = vi.next();
        final FunctionCall root = (FunctionCall) ((PathExpr) compiledXquery).getFirst();
        final UserDefinedFunction calledFunc = root.getFunction();
        final Expression calledBody = calledFunc.getFunctionBody();

        // set some property so we can test if it gets cleared
        calledFunc.setContextDocSet(DocumentSet.EMPTY_DOCUMENT_SET);
        calledBody.setContextDocSet(DocumentSet.EMPTY_DOCUMENT_SET);
        var1.setContextDocSet(DocumentSet.EMPTY_DOCUMENT_SET);
        var2.setContextDocSet(DocumentSet.EMPTY_DOCUMENT_SET);

        // execute query and check result
        try (final EXistResourceSet result = (EXistResourceSet) service.execute(compiledExpression)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("Hello world123", result.getResource(0).getContent());
            }

            final Sequence[] args = calledFunc.getCurrentArguments();
            assertNull(args);
            assertNull(calledFunc.getContextDocSet());
            assertNull(calledBody.getContextDocSet());
            assertNull(var1.getContextDocSet());
            assertNull(var2.getContextDocSet());
        }
    }

    @Test
    void resetStateOfInlineFunc() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final BrokerPool pool = BrokerPool.getInstance();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(TEST_INLINE), false, Sequence.EMPTY_SEQUENCE, null, null, null, null)) {
            // execute query to get a function item
            final Sequence result = queryResult.result;
            assertEquals(1, result.getItemCount());
            final FunctionCall call = ((FunctionReference)result.itemAt(0)).getCall();
            // closure variables are set when function item is created, but should be cleared after query
            final List<ClosureVariable> closure = call.getFunction().getClosureVariables();
            assertNull(closure);
        }
    }

    @Test
    void preserveExternalVariable() throws XMLDBException, XPathException {
        // see https://github.com/eXist-db/exist/pull/1512 and use of util:eval
        final EXistXQueryService service = collection.getService(EXistXQueryService.class);

        final LocalCompiledExpression compiledExpression = (LocalCompiledExpression) service.compile(INTERNAL_MODULE_EVAL_TEST);
        final XQueryUtil.CompilationResult compilationResult = compiledExpression.getCompilationResult();
        final CompiledXQuery compiledXquery = compilationResult.compiledXquery;


        final Module[] modules = compiledXquery.getContext().getModules(MODULE_NS);
        assertEquals(1, modules.length);
        final Module module = modules[0];
        module.declareVariable(new QName("VAR", MODULE_NS, "t"), "TEST");

        try (final EXistResourceSet result = (EXistResourceSet) service.execute(compiledExpression)) {
            assertEquals(2, result.getSize());
            try (final Resource resource = result.getResource(1)) {
                assertEquals("TEST", result.getResource(1).getContent());
            }

            final Variable var = module.resolveVariable(new QName("VAR", MODULE_NS, "t"));
            assertNull(var);
        }
    }

    @Test
    void resetStateofInternalModule() throws XMLDBException, XPathException {
        final EXistXQueryService service = collection.getService(EXistXQueryService.class);

        final LocalCompiledExpression compiledExpression = (LocalCompiledExpression) service.compile(INTERNAL_MODULE_TEST);
        final XQueryUtil.CompilationResult compilationResult = compiledExpression.getCompilationResult();
        final CompiledXQuery compiledXquery = compilationResult.compiledXquery;

        final Module[] modules = compiledXquery.getContext().getModules(MODULE_NS);
        assertEquals(1, modules.length);
        final Module module = modules[0];
        module.declareVariable(new QName("VAR", MODULE_NS, "t"), "TEST");
        final InternalFunctionCall root = (InternalFunctionCall) ((PathExpr) compiledXquery).getFirst();
        final TestModule.TestFunction func = (TestModule.TestFunction) root.getFunction();

        try (final EXistResourceSet result = (EXistResourceSet) service.execute(compiledExpression)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("TEST", result.getResource(0).getContent());
            }
            assertFalse(func.dummyProperty);
        }
    }
}
