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
package org.exist.xquery;

import antlr.BaseAST;
import antlr.CommonAST;
import org.exist.xquery.parser.XQueryTreeParser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XQueryIsLibraryModuleTest {

    @Test
    public void isLibraryModuleAstVersionDeclAndLibraryModule() {
        final BaseAST xqueryModuleDecl = new CommonAST();
        xqueryModuleDecl.setType(XQueryTreeParser.MODULE_DECL);

        final BaseAST xqueryDecl = new CommonAST();
        xqueryDecl.setType(XQueryTreeParser.VERSION_DECL);
        xqueryDecl.setText("3.1");
        xqueryDecl.setNextSibling(xqueryModuleDecl);

        assertTrue(XQuery.isLibraryModule(xqueryDecl));
    }

    @Test
    public void isLibraryModuleAstVersionDeclAndMainModule() {
        final BaseAST namespaceDecl = new CommonAST();
        namespaceDecl.setType(XQueryTreeParser.NAMESPACE_DECL);

        final BaseAST xqueryDecl = new CommonAST();
        xqueryDecl.setType(XQueryTreeParser.VERSION_DECL);
        xqueryDecl.setText("3.1");
        xqueryDecl.setNextSibling(namespaceDecl);

        assertFalse(XQuery.isLibraryModule(xqueryDecl));
    }

    @Test
    public void isLibraryModuleAstLibraryModule() {
        final BaseAST xqueryModuleDecl = new CommonAST();
        xqueryModuleDecl.setType(XQueryTreeParser.MODULE_DECL);

        assertTrue(XQuery.isLibraryModule(xqueryModuleDecl));
    }

    @Test
    public void isLibraryModuleAstMainModule() {
        final BaseAST namespaceDecl = new CommonAST();
        namespaceDecl.setType(XQueryTreeParser.NAMESPACE_DECL);

        assertFalse(XQuery.isLibraryModule(namespaceDecl));
    }

    @Test
    public void isLibraryModuleAstVersionDeclAndLibraryModuleAndProlog() {
        final BaseAST eof = new CommonAST();
        eof.setType(XQueryTreeParser.EOF);

        final BaseAST functionDecl = new CommonAST();
        functionDecl.setType(XQueryTreeParser.FUNCTION_DECL);
        functionDecl.setNextSibling(eof);

        final BaseAST importModuleDecl = new CommonAST();
        importModuleDecl.setType(XQueryTreeParser.MODULE_IMPORT);
        importModuleDecl.setNextSibling(functionDecl);

        final BaseAST importNamespaceDecl = new CommonAST();
        importNamespaceDecl.setType(XQueryTreeParser.NAMESPACE_DECL);
        importNamespaceDecl.setNextSibling(importModuleDecl);

        final BaseAST xqueryModuleDecl = new CommonAST();
        xqueryModuleDecl.setType(XQueryTreeParser.MODULE_DECL);
        xqueryModuleDecl.setNextSibling(importNamespaceDecl);

        final BaseAST xqueryDecl = new CommonAST();
        xqueryDecl.setType(XQueryTreeParser.VERSION_DECL);
        xqueryDecl.setText("3.1");
        xqueryDecl.setNextSibling(xqueryModuleDecl);

        assertTrue(XQuery.isLibraryModule(xqueryDecl));
    }

    @Test
    public void isLibraryModuleAstVersionDeclAndMainModuleAndProlog() {
        final BaseAST eof = new CommonAST();
        eof.setType(XQueryTreeParser.EOF);

        final BaseAST functionDecl = new CommonAST();
        functionDecl.setType(XQueryTreeParser.FUNCTION_DECL);
        functionDecl.setNextSibling(eof);

        final BaseAST importModuleDecl = new CommonAST();
        importModuleDecl.setType(XQueryTreeParser.MODULE_IMPORT);
        importModuleDecl.setNextSibling(functionDecl);

        final BaseAST importNamespaceDecl = new CommonAST();
        importNamespaceDecl.setType(XQueryTreeParser.NAMESPACE_DECL);
        importNamespaceDecl.setNextSibling(importModuleDecl);

        final BaseAST xqueryDecl = new CommonAST();
        xqueryDecl.setType(XQueryTreeParser.VERSION_DECL);
        xqueryDecl.setText("3.1");
        xqueryDecl.setNextSibling(importNamespaceDecl);

        assertFalse(XQuery.isLibraryModule(xqueryDecl));
    }
}
