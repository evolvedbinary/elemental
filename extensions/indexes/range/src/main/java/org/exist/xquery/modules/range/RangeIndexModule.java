/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.xquery.modules.range;

import org.exist.dom.QName;
import org.exist.indexing.range.RangeIndex;
import org.exist.xquery.*;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.exist.xquery.FunctionDSL.functionDefs;

public class RangeIndexModule extends AbstractInternalModule {

    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/range";
    public final static String PREFIX = "range";
    public final static String RELEASED_IN_VERSION = "eXist-2.2";

    public static final FunctionDef[] functions = functionDefs(
            functionDefs(Lookup.class,
                    Lookup.signatures[0],
                    Lookup.signatures[1],
                    Lookup.signatures[2],
                    Lookup.signatures[3],
                    Lookup.signatures[4],
                    Lookup.signatures[5],
                    Lookup.signatures[6],
                    Lookup.signatures[7],
                    Lookup.signatures[8],
                    Lookup.signatures[9]
            ),
            functionDefs(FieldLookup.class,
                    FieldLookup.signatures[0],
                    FieldLookup.signatures[1],
                    FieldLookup.signatures[2],
                    FieldLookup.signatures[3],
                    FieldLookup.signatures[4],
                    FieldLookup.signatures[5],
                    FieldLookup.signatures[6],
                    FieldLookup.signatures[7],
                    FieldLookup.signatures[8],
                    FieldLookup.signatures[9],
                    FieldLookup.signatures[10]
            ),
            functionDefs(Optimize.class,
                    Optimize.signature
            ),
            functionDefs(IndexKeys.class,
                    IndexKeys.signatures[0],
                    IndexKeys.signatures[1]
            ),
            functionDefs(ContextLookup.class,
                    ContextLookup.FS_CONTEXT[0],
                    ContextLookup.FS_CONTEXT[1],
                    ContextLookup.FS_PRE_CONTEXT[0],
                    ContextLookup.FS_PRE_CONTEXT[1],
                    ContextLookup.FS_POST_CONTEXT[0],
                    ContextLookup.FS_POST_CONTEXT[1]
            )
    );

    public final static Map<String, RangeIndex.Operator> OPERATOR_MAP = new HashMap<>();
    static {
        OPERATOR_MAP.put("eq", RangeIndex.Operator.EQ);
        OPERATOR_MAP.put("lt", RangeIndex.Operator.LT);
        OPERATOR_MAP.put("gt", RangeIndex.Operator.GT);
        OPERATOR_MAP.put("ge", RangeIndex.Operator.GE);
        OPERATOR_MAP.put("le", RangeIndex.Operator.LE);
        OPERATOR_MAP.put("ne", RangeIndex.Operator.NE);
        OPERATOR_MAP.put("starts-with", RangeIndex.Operator.STARTS_WITH);
        OPERATOR_MAP.put("ends-with", RangeIndex.Operator.ENDS_WITH);
        OPERATOR_MAP.put("contains", RangeIndex.Operator.CONTAINS);
        OPERATOR_MAP.put("matches", RangeIndex.Operator.MATCH);

    }

    protected final static class RangeIndexErrorCode extends ErrorCodes.ErrorCode {

        public RangeIndexErrorCode(String code, String description) {
            super(new QName(code, NAMESPACE_URI, PREFIX), description);
        }

    }

    public static final ErrorCodes.ErrorCode EXXQDYFT0001 = new RangeIndexErrorCode("EXXQDYFT0001", "Collation not supported");
    public static final ErrorCodes.ErrorCode EXXQDYFT0002 = new RangeIndexErrorCode("EXXQDYFT0002", "Node type is in-memory and is not a stored node");
    public static final ErrorCodes.ErrorCode EXXQDYFT0003 = new RangeIndexErrorCode("EXXQDYFT0003", "There is no context defined in the index definition for this node");
    public final static ErrorCodes.ErrorCode EXXQDYFT0004 = new RangeIndexErrorCode("EXXQDYFT0004", "An I/O error occurred whilst searching the index");

    public RangeIndexModule(Map<String, List<? extends Object>> parameters) {
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
        return "Functions to access the range index.";
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
}
