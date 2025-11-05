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
package org.exist.xquery.modules.lucene;

import java.util.List;
import java.util.Map;

import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.ErrorCodes.IErrorCode;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;

/**
 * Module function definitions for Lucene-based full text indexed searching.
 *
 * @author wolf
 * @author ljo
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LuceneModule extends AbstractInternalModule {

    public static final String NAMESPACE_URI = "http://exist-db.org/xquery/lucene";

    public static final String PREFIX = "ft";
    public final static String INCLUSION_DATE = "2008-09-03";
    public final static String RELEASED_IN_VERSION = "eXist-1.4";
    
    public static final FunctionDef[] functions = {
        new FunctionDef(Query.signatures[0], Query.class),
        new FunctionDef(Query.signatures[1], Query.class),
        new FunctionDef(QueryField.signatures[0], QueryField.class),
        new FunctionDef(QueryField.signatures[1], QueryField.class),
        new FunctionDef(Score.signature, Score.class),
        new FunctionDef(Optimize.signature, Optimize.class),
        new FunctionDef(Index.signatures[0], Index.class),
        new FunctionDef(Index.signatures[1], Index.class),
        new FunctionDef(Index.signatures[2], Index.class),
        new FunctionDef(InspectIndex.signatures[0], InspectIndex.class),
        new FunctionDef(RemoveIndex.signature, RemoveIndex.class),
        new FunctionDef(Search.signatures[0], Search.class),
        new FunctionDef(Search.signatures[1], Search.class),
        new FunctionDef(Search.signatures[2], Search.class),
        new FunctionDef(GetField.signatures[0], GetField.class),
        new FunctionDef(Facets.signatures[0], Facets.class),
        new FunctionDef(Facets.signatures[1], Facets.class),
        new FunctionDef(Facets.signatures[2], Facets.class),
        new FunctionDef(Field.FS_FIELD[0], Field.class),
        new FunctionDef(Field.FS_FIELD[1], Field.class),
        new FunctionDef(Field.FS_BINARY_FIELD[0], Field.class),
        new FunctionDef(Field.FS_BINARY_FIELD[1], Field.class),
        new FunctionDef(Field.FS_HIGHLIGHT_FIELD_MATCHES, Field.class),
        new FunctionDef(LuceneIndexKeys.signatures[0], LuceneIndexKeys.class)
    };

    public LuceneModule(Map<String, List<? extends Object>> parameters) {
        super(functions, parameters, false);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "A module for full text indexed searching based on Lucene.";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }

    static FunctionSignature functionSignature(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType... paramTypes) {
        return FunctionDSL.functionSignature(new QName(name, NAMESPACE_URI, PREFIX), description, returnType, paramTypes);
    }

    static FunctionSignature[] functionSignatures(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType[][] variableParamTypes) {
        return FunctionDSL.functionSignatures(new QName(name, NAMESPACE_URI, PREFIX), description, returnType, variableParamTypes);
    }

    enum LuceneErrorCode implements IErrorCode {
        EXXQDYFT0001 ("Permission denied."),
        EXXQDYFT0002 ("IO Exception in lucene index."),
        EXXQDYFT0003 ("Document not found."),
        EXXQDYFT0004 ("Wrong configuration passed to ft:query"),
        EXXQDYFT0005 ("Unable to deserialize binary value in call to ft:field"),
        EXXQDYFT0006 ("Unable to deserialize string value in call to ft:field"),
        EXXQDYFT0007 ("Unable to deserialize numeric value in call to ft:field");

        private final ErrorCodes.ErrorCode errorCode;

        LuceneErrorCode(final String description) {
            this.errorCode = new ErrorCodes.ErrorCode(new QName(name(), NAMESPACE_URI, PREFIX), description);
        }

        @Override
        public QName getErrorQName() {
            return errorCode.getErrorQName();
        }

        @Override
        public String getDescription() {
            return errorCode.getDescription();
        }

        /**
         * Get the error code.
         *
         * @return the error code.
         */
        public ErrorCodes.ErrorCode getErrorCode() {
            return errorCode;
        }
    }
}

