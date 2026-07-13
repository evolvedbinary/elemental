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

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import java.io.*;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.evolvedbinary.j8fu.function.BiConsumerE;
import com.evolvedbinary.j8fu.function.ConsumerE;
import com.evolvedbinary.j8fu.tuple.Tuple3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.debuggee.Debuggee;
import org.exist.dom.QName;
import org.exist.security.EffectiveSubject;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.source.FileSource;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.DBBroker;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.parser.XQueryLexer;
import org.exist.xquery.parser.XQueryParser;
import org.exist.xquery.parser.XQueryTreeParser;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.BinaryValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;

import javax.annotation.Nullable;


/**
 * @author wolf
 */
public class XQuery {

    private final static Logger LOG = LogManager.getLogger(XQuery.class);

    /**
     * Compiles an XQuery from a String.
     *
     * @param broker the database broker (unused)
     * @param context the XQuery context
     * @param expression the expression to compile
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     *
     * @deprecated Use {@link #compile(XQueryContext, String)} instead.
     */
    @Deprecated
    public CompiledXQuery compile(final DBBroker broker, final XQueryContext context, final String expression) throws XPathException, PermissionDeniedException {
        return compile(context, expression);
    }

    /**
     * Compiles an XQuery from a String.
     *
     * @param context the XQuery context
     * @param expression the expression to compile
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     */
    public CompiledXQuery compile(final XQueryContext context, final String expression) throws XPathException, PermissionDeniedException {
    	final Source source = new StringSource(expression);
    	try {
            return compile(context, source);
        } catch(final IOException ioe) {
            //should not happen because expression is a String
            throw new XPathException(context != null ? context.getRootExpression() : null, ioe.getMessage());
        }
    }

    /**
     * Compiles an XQuery from a Source.
     *
     * @param broker the database broker (unused)
     * @param context the XQuery context
     * @param source the source of the XQuery to compile
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws IOException if an IO error occurs when reading the source
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     *
     * @deprecated Use {@link #compile(XQueryContext, Source)} instead.
     */
    @Deprecated
    public CompiledXQuery compile(final DBBroker broker, final XQueryContext context, final Source source) throws XPathException, IOException, PermissionDeniedException {
        return compile(context, source);
    }

    /**
     * Compiles an XQuery from a Source.
     *
     * @param context the XQuery context
     * @param source the source of the XQuery to compile
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws IOException if an IO error occurs when reading the source
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     */
    public CompiledXQuery compile(final XQueryContext context, final Source source) throws XPathException, IOException, PermissionDeniedException {
        return compile(context, source, false);
    }

    /**
     * Compiles an XQuery from a Source.
     *
     * @param broker the database broker (unused)
     * @param context the XQuery context
     * @param source the source of the XQuery to compile
     * @param xpointer true if the query is part of an XPointer, false otherwise
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws IOException if an IO error occurs when reading the source
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     *
     * @deprecated Use {@link #compile(XQueryContext, Source, boolean)} instead.
     */
    @Deprecated
    public CompiledXQuery compile(final DBBroker broker, final XQueryContext context, final Source source, final boolean xpointer) throws XPathException, IOException, PermissionDeniedException {
        return compile(context, source, xpointer);
    }

    /**
     * Compiles an XQuery from a Source.
     *
     * @param context the XQuery context
     * @param source the source of the XQuery to compile
     * @param xpointer true if the query is part of an XPointer, false otherwise
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws IOException if an IO error occurs when reading the source
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     */
    public CompiledXQuery compile(final XQueryContext context, final Source source, final boolean xpointer) throws XPathException, IOException, PermissionDeniedException {

        context.setSource(source);

        try(final Reader reader = source.getReader()) {
            return compile(context, reader, xpointer);
        } catch(final UnsupportedEncodingException e) {
            throw new XPathException(context.getRootExpression(), ErrorCodes.XQST0087, "unsupported encoding " + e.getMessage());
        }
    }

    /**
     * Compiles an XQuery from a Source.
     *
     * @param context the XQuery context
     * @param reader the reader to use for obtaining theXQuery to compile
     * @param xpointer true if the query is part of an XPointer, false otherwise
     *
     * @return the compiled XQuery
     *
     * @throws XPathException if an error occurs during compilation
     * @throws PermissionDeniedException if the caller is not permitted to compile the XQuery
     */
    private CompiledXQuery compile(final XQueryContext context, final Reader reader, final boolean xpointer) throws XPathException, PermissionDeniedException {

        //check read permission
        @Nullable final Subject currentSubject = context.getSubject();
        if (currentSubject != null) {
            try {
                if (!context.getSource().getPermissions().validate(currentSubject, Permission.READ)) {
                    throw new PermissionDeniedException("Subject '" + currentSubject.getName() + "' does not have read  access to resource '" + context.getSource().pathOrShortIdentifier() + "'.");
                }
            } catch (final IOException e) {
                throw new PermissionDeniedException("Subject '" + currentSubject.getName() + "' does not have read  access to resource '" + context.getSource().pathOrShortIdentifier() + "'.", e);
            }
        }
        
    	//TODO: move XQueryContext.getUserFromHttpSession() here, have to check if servlet.jar is in the classpath
    	//before compiling/executing that code though to avoid a dependency on servlet.jar - reflection? - deliriumsky
    	
    	// how about - if(XQuery.class.getResource("servlet.jar") != null) do load my class with dependency and call method?
    	
    	/*
    	 	<|wolf77|> I think last time I checked, I already had problems with the call to
    	 	<|wolf77|> HTTPUtils.addLastModifiedHeader( result, context );
			<|wolf77|> in line 184 of XQuery.java, because it introduces another dependency on HTTP.
    	 */
    	
    	final long start = System.currentTimeMillis();
        final XQueryLexer lexer = new XQueryLexer(context, reader);
        final XQueryParser parser = new XQueryParser(lexer);
        final XQueryTreeParser treeParser = new XQueryTreeParser(context);
        try {
            if (xpointer) {
                parser.xpointer();
            } else {
                parser.xpath();
            }

            if (parser.foundErrors()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(parser.getErrorMessage());
                }
                final Exception lastException = parser.getLastException();
                if (lastException instanceof XPathException xpe) {
                    throw new StaticXQueryException(xpe.getColumn(), xpe.getLine(), parser.getErrorMessage(), xpe);
                } else {
                    throw new StaticXQueryException(context.getRootExpression(), parser.getErrorMessage());
                }
            }

            final AST ast = parser.getAST();
            if (ast == null) {
                throw new XPathException(context.getRootExpression(), "Unknown XQuery parser error: the parser returned an empty syntax tree.");
            }
            
//            LOG.debug("Generated AST: " + ast.toStringTree());

            final PathExpr expr;
            if (isLibraryModule(ast)) {
                // return new LibraryModuleRoot instead!
                expr = new LibraryModuleRoot(context);
            } else {
                expr = new PathExpr(context);
            }

            if (xpointer) {
                treeParser.xpointer(ast, expr);
            } else {
                treeParser.xpath(ast, expr);
            }
            
            if (treeParser.foundErrors()) {
                //AST treeAst = treeParser.getAST();
                throw new StaticXQueryException(ast.getLine(), ast.getColumn(), treeParser.getErrorMessage(), treeParser.getLastException());
            }
            
            context.getRootContext().resolveForwardReferences();
            context.analyzeAndOptimizeIfModulesChanged(expr);

            // Log the query if it is not too large, but avoid
            // dumping huge queries to the log
            if (LOG.isDebugEnabled()) {
                if (context.getExpressionCount() < 150) {
                    LOG.debug("Query diagnostics:\n{}", ExpressionDumper.dump(expr));
                } else {
                    LOG.debug("Query diagnostics:\n" + "[skipped: more than 150 expressions]");
                }
            }
            
            if (LOG.isDebugEnabled()) {
            	final NumberFormat nf = NumberFormat.getNumberInstance();
                LOG.debug("Compilation took {} ms", nf.format(System.currentTimeMillis() - start));
            }
            
            return expr;
        } catch(final RecognitionException e) {
            LOG.debug("Error compiling query: {}", e.getMessage(), e);
            String msg = e.getMessage();
            if (msg.endsWith(", found 'null'")) {
                msg = msg.substring(0, msg.length() - ", found 'null'".length());
            }
            throw new StaticXQueryException(e.getLine(), e.getColumn(), msg);
        } catch(final TokenStreamException e) {
            final String es = e.toString();
            if(es.matches("^line \\d+:\\d+:.+")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error compiling query: {}", e.getMessage(), e);
                }
                final int line = Integer.parseInt(es.substring(5, es.indexOf(':')));
                final String tmpColumn = es.substring(es.indexOf(':') + 1);
                final int column = Integer.parseInt(tmpColumn.substring(0, tmpColumn.indexOf(':')));
                throw new StaticXQueryException(line, column, e.getMessage(), e);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error compiling query: {}", e.getMessage(), e);
                }
                throw new StaticXQueryException((Expression) null, e.getMessage(), e);
            }
            
        }
    }

    /**
     * Searches from the root of the AST to find if this is a Library Module
     *
     * @param ast the root of the AST
     *
     * @return true if this is a library module, false otherwise
     */
    static boolean isLibraryModule(AST ast) {
        while (ast != null) {
            if (ast.getType() == XQueryTreeParser.MODULE_DECL) {
                return true;
            }

            // if we get as far as function declarations or global variable declarations we have gone too far
            if (ast.getType() == XQueryTreeParser.FUNCTION_DECL || ast.getType() == XQueryTreeParser.GLOBAL_VAR) {
                return false;
            }

            ast = ast.getNextSibling();
        }

        return false;
    }

    public Sequence execute(final DBBroker broker, final CompiledXQuery expression, final Sequence contextSequence) throws XPathException, PermissionDeniedException {
    	return execute(broker, expression, null, contextSequence, null, true);
    }

    public Sequence execute(final DBBroker broker, final CompiledXQuery expression, final Sequence contextSequence, final Properties outputProperties) throws XPathException, PermissionDeniedException {
        return execute(broker, expression, null, contextSequence,  outputProperties, true);
    }
    
    public Sequence execute(final DBBroker broker, final CompiledXQuery expression, final Sequence contextSequence, final boolean resetContext) throws XPathException, PermissionDeniedException {
    	return execute(broker, expression, null, contextSequence, null, resetContext);
    }

    public Sequence execute(final DBBroker broker, final CompiledXQuery expression, Sequence contextSequence, final Properties outputProperties, final boolean resetContext) throws XPathException, PermissionDeniedException {
        return execute(broker, expression, null, contextSequence, outputProperties, resetContext);
    }

    public Sequence execute(final DBBroker broker, final CompiledXQuery expression, @Nullable final Tuple3<QName, List<Expression>, Optional<ErrorCodes.ErrorCode>> functionCall, @Nullable Sequence contextSequence, final Properties outputProperties, final boolean resetContext) throws XPathException, PermissionDeniedException {
    	
        //check execute permissions
        try {
            if (!expression.getContext().getSource().getPermissions().validate(broker.getCurrentSubject(), Permission.EXECUTE)) {
                throw new PermissionDeniedException("Subject '" + broker.getCurrentSubject().getName() + "' does not have execute access to resource '" + expression.getContext().getSource().pathOrShortIdentifier() + "'.");
            }
        } catch (final IOException e) {
            throw new PermissionDeniedException("Subject '" + broker.getCurrentSubject().getName() + "' does not have execute access to resource '" + expression.getContext().getSource().pathOrShortIdentifier() + "'.", e);
        }
        
        final long start = System.currentTimeMillis();
    	
        final XQueryContext context = expression.getContext();

        expression.reset();
        if(resetContext) {
            //context.setBroker(broker);
            context.getWatchDog().reset();
        }

        if(context.requireDebugMode()) {
            final Debuggee debuggee = broker.getBrokerPool().getDebuggee();
            if (debuggee != null) {
                debuggee.joint(expression);
            }
        }
        
        //do any preparation before execution
        context.prepareForExecution();
        
        final Subject callingUser = broker.getCurrentSubject();

        //if setUid or setGid, become Effective User
        EffectiveSubject effectiveSubject = null;
        final Source src = expression.getContext().getSource();
        final Permission perm;
        try {
            perm = src.getPermissions();
        } catch (final IOException e) {
            throw new PermissionDeniedException(e);
        }
        if (perm.isSetUid()) {
            if(perm.isSetGid()) {
                //setUid and SetGid
                effectiveSubject = new EffectiveSubject(perm.getOwner(), perm.getGroup());
            } else {
                //just setUid
                effectiveSubject = new EffectiveSubject(perm.getOwner());
            }
        } else if (perm.isSetGid()) {
            //just setGid, so we use the current user as the effective user
            effectiveSubject = new EffectiveSubject(callingUser, perm.getGroup());
        }
        
        try {
            if(effectiveSubject != null) {
                broker.pushSubject(effectiveSubject); //switch to effective user (e.g. setuid/setgid)
            }
            
            context.getProfiler().traceQueryStart();
            broker.getBrokerPool().getProcessMonitor().queryStarted(context.getWatchDog());

            @Nullable Sequence result = null;
            FunctionCall call = null;
            try {

                // support for XQuery 3.0 - declare context item :=
                if(contextSequence == null) {
                    if(context.getContextItemDeclaration() != null) {
                        contextSequence = context.getContextItemDeclaration().eval(null, null);
                    }
                }

                if (expression instanceof LibraryModuleRoot) {
                    if (functionCall == null) {
                        if (expression != null) {
                            throw new XPathException(((LibraryModuleRoot) expression).getLine(), ((LibraryModuleRoot) expression).getColumn(), ErrorCodes.EXXQDY0005, "No function call details were provided when trying to execute a Library Module.");
                        } else {
                            throw new XPathException((Expression) null, ErrorCodes.EXXQDY0005, "No function call details were provided when trying to execute a Library Module.");
                        }
                    }

                    final QName functionName = functionCall._1;
                    final List<Expression> functionArgs = functionCall._2;
                    final int functionArity = functionArgs.size();
                    final UserDefinedFunction function = context.resolveFunction(functionName, functionArity);
                    if (function == null) {
                        final ErrorCodes.ErrorCode errorCode = functionCall._3.orElse(ErrorCodes.EXXQDY0006);
                        throw new XPathException(context != null ? context.getRootExpression() : null, errorCode, "No such function: " + functionName.getStringValue() + "#" + functionArity);
                    }

                    call = new FunctionCall(context, function);
                    call.setArguments(functionArgs);
                    call.analyze(new AnalyzeContextInfo());

                    result = call.eval(contextSequence, null);

                } else {
                    result = expression.eval(contextSequence, null);
                }

                if(LOG.isDebugEnabled()) {
                    final NumberFormat nf = NumberFormat.getNumberInstance();
                    LOG.debug("Execution took {} ms", nf.format(System.currentTimeMillis() - start));
                }

                if(outputProperties != null) {
                    context.checkOptions(outputProperties); //must be done before context.reset!
                }

                return result;
            } finally {
                context.getProfiler().traceQueryEnd(context);
                // track query stats before context is reset
                broker.getBrokerPool().getProcessMonitor().queryCompleted(context.getWatchDog());
                expression.reset();

                if(call != null) {
                    call.reset();
                }

                runCleanupTasks(context, result);

                if (resetContext) {
                    context.reset();
                }
            }
            
        } finally {
            if(effectiveSubject != null) {
                broker.popSubject();
            }
        }
    }

    /**
     * Execute an XPath or XQuery.
     *
     * @param broker the database broker.
     * @param expression the XPath or XQuery expression.
     * @param contextSequence the context sequence to use when executing the query, or null if there is no context sequence.
     *
     * @return the result of the query.
     *
     * @deprecated Use {@link XQueryUtil#query(DBBroker, Source, boolean, Sequence, Properties, ConsumerE, ConsumerE, BiConsumerE)} instead.
     */
    @Deprecated
    public Sequence execute(final DBBroker broker, final String expression, final Sequence contextSequence) throws XPathException, PermissionDeniedException {
        final XQueryContext context = new XQueryContext(broker.getBrokerPool());
        final CompiledXQuery compiled = compile(context, expression);
        return execute(broker, compiled, contextSequence);
    }

    /**
     * Execute an XPath or XQuery.
     *
     * @param broker the database broker.
     * @param file the file containing the XPath or XQuery.
     * @param contextSequence the context sequence to use when executing the query, or null if there is no context sequence.
     *
     * @return the result of the query.
     *
     * @deprecated Use {@link XQueryUtil#query(DBBroker, Source, boolean, Sequence, Properties, ConsumerE, ConsumerE, BiConsumerE)} instead.
     */
    @Deprecated
    public Sequence execute(final DBBroker broker, final File file, final Sequence contextSequence) throws XPathException, IOException, PermissionDeniedException {
        final XQueryContext context = new XQueryContext(broker.getBrokerPool());
        final CompiledXQuery compiled = compile(context, new FileSource(file.toPath(), true));
        return execute(broker, compiled, contextSequence);
    }

    /**
     * Runs cleanup tasks after query execution to free any resources that are no longer needed.
     *
     * @param xqueryContext the XQuery Context.
     * @param queryResult the result Sequence of the Query.
     */
    private static void runCleanupTasks(final XQueryContext xqueryContext, @Nullable final Sequence queryResult) {
        if (queryResult == null) {
            // There are no results produced by the query, so we can cleanup everything
            xqueryContext.runCleanupTasks();

        } else {
            // There are results produced, so we can only cleanup resources that don't appear in the result sequence
            cleanupOrphanedValues(xqueryContext, queryResult);
        }
    }

    /**
     * Runs cleanup tasks that cleanup items that do not appear in the Query Result.
     * At present the only items that may need to be cleaned up are BinaryValue types.
     *
     * @param xqueryContext the XQuery Context.
     * @param queryResult the result Sequence of the Query.
     */
    private static void cleanupOrphanedValues(final XQueryContext xqueryContext, final Sequence queryResult) {
        xqueryContext.runCleanupTasks(false, false, clazz -> clazz.equals(XQueryContext.BinaryValueCleanupTask.class), obj -> {
            if (obj instanceof BinaryValue) {
                final boolean hasReference = sequenceHasItemReference(queryResult, obj);
                return !hasReference;
            }

            return true;
        });
    }

    /**
     * Checks if a Sequence contains an Item.
     * The check is performed by reference equality.
     *
     * @param sequence the Sequence to inspect.
     * @param itemReference The item to look for by reference.
     *
     * @return true if the sequence contains the item, false otherwise.
     */
    private static boolean sequenceHasItemReference(final Sequence sequence, final Object itemReference) {
        if (sequence instanceof MapType) {
            return itemHasItemReference((MapType) sequence, itemReference);
        }

        if (sequence instanceof ArrayType) {
            return itemHasItemReference((ArrayType) sequence, itemReference);
        }

        try {
            final SequenceIterator itItem = sequence.iterate();
            while (itItem.hasNext()) {
                final Item item = itItem.nextItem();
                if (itemHasItemReference(item, itemReference)) {
                    return true;
                }
            }
        } catch (final XPathException e) {
            // no-op
        }

        return false;
    }

    /**
     * Checks if an Item matches an Item (by reference equality).
     * If the Item is a Map or Array, we also checks its members (recursively).
     *
     * @param item the Item to inspect.
     * @param itemReference The item to look for by reference.
     *
     * @return true if the Item matches or contains the Item reference, false otherwise.
     */
    private static boolean itemHasItemReference(final Item item, final Object itemReference) {
        if (item == itemReference) {
            return true;
        }

        if (item instanceof MapType) {
            final MapType mapType = (MapType) item;
            final Iterator<Sequence> itMapValues = mapType.valueIterator();
            while (itMapValues.hasNext()) {
                final Sequence mapValue = itMapValues.next();
                if (sequenceHasItemReference(mapValue, itemReference)) {
                    return true;
                }
            }
        }

        if (item instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) item;
            final Iterator<Sequence> itArrayValues = arrayType.iterator();
            while (itArrayValues.hasNext()) {
                final Sequence arrayValue = itArrayValues.next();
                if (sequenceHasItemReference(arrayValue, itemReference)) {
                    return true;
                }
            }
        }

        return false;
    }
}
