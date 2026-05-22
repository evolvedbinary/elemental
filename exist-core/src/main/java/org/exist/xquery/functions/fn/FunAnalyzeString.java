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
package org.exist.xquery.functions.fn;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.regex.RegexIterator;
import net.sf.saxon.regex.RegularExpression;
import org.exist.dom.QName;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.*;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;

/**
 * XPath and XQuery 3.0 F+O fn:analyze-string()
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunAnalyzeString extends BasicFunction {

    /**
     * Implements a ByteBuddy Invocation Handler to implement the `characters`
     * method of Saxon 9's net.sf.saxon.regex.RegexIterator$MatchHandler
     * and Saxon 12's net.sf.saxon.regex.RegexMatchHandler
     */
    private static final InvocationHandler CHARACTERS_HANDLER = (final Object proxy, final Method method, final Object[] args) -> {
        final MemTreeBuilder builder = ((AbstractSaxonRegexMatchHandler) proxy).builder;
        final Object s = args[0];
        builder.characters(s.toString());
        return null;
    };

    private static final Constructor<? extends AbstractSaxonRegexMatchHandler> SAXON_MATCH_HANDLER_CLASS_CONSTRUCTOR = getSaxonMatchHandlerClassConstructor(createSaxonMatchHandlerClass());
    private static final Method SAXON_PROCESS_MATCHING_SUBSTRING_FN = getSaxonProcessMatchingSubstringFunction();

    private final static QName fnAnalyzeString = new QName("analyze-string", FnModule.NAMESPACE_URI);

    private final static QName QN_MATCH = new QName("match", FnModule.NAMESPACE_URI);
    private final static QName QN_GROUP = new QName("group", FnModule.NAMESPACE_URI);
    private final static QName QN_NR = new QName("nr", XMLConstants.NULL_NS_URI);
    private final static QName QN_NON_MATCH = new QName("non-match", FnModule.NAMESPACE_URI);
    
    public final static FunctionSignature[] signatures = {
        new FunctionSignature(
            fnAnalyzeString,
            "Analyzes a string using a regular expression, returning an XML " +
            "structure that identifies which parts of the input string matched " +
            "or failed to match the regular expression, and in the case of " +
            "matched substrings, which substrings matched each " +
            "capturing group in the regular expression.",
            new SequenceType[] { 
                new FunctionParameterSequenceType("input", Type.STRING,
                    Cardinality.ZERO_OR_ONE, "The input string"),
                new FunctionParameterSequenceType("pattern", Type.STRING,
                    Cardinality.EXACTLY_ONE, "The pattern")
            },
            new FunctionReturnSequenceType(Type.ELEMENT,
                Cardinality.EXACTLY_ONE, "The result of the analysis")
        ),
        new FunctionSignature(
            fnAnalyzeString,
            "Analyzes a string using a regular expression, returning an XML " +
            "structure that identifies which parts of the input string matched " +
            "or failed to match the regular expression, and in the case of " +
            "matched substrings, which substrings matched each " +
            "capturing group in the regular expression.",
            new SequenceType[] { 
                new FunctionParameterSequenceType("input", Type.STRING,
                    Cardinality.ZERO_OR_ONE, "The input string"),
                new FunctionParameterSequenceType("pattern", Type.STRING,
                    Cardinality.EXACTLY_ONE, "The pattern"),
                new FunctionParameterSequenceType("flags", Type.STRING,
                    Cardinality.EXACTLY_ONE, "Flags"),
            },
            new FunctionReturnSequenceType(Type.ELEMENT,
                Cardinality.EXACTLY_ONE, "The result of the analysis")
        )
    };

    public FunAnalyzeString(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        context.pushDocumentContext();
        try {
            final MemTreeBuilder builder = context.getDocumentBuilder();
            builder.startDocument();
            builder.startElement(new QName("analyze-string-result", FnModule.NAMESPACE_URI), null);
            String input = "";
            if (!args[0].isEmpty()) {
                input = args[0].itemAt(0).getStringValue();
            }
            if (input != null && !input.isEmpty()) {
                final String pattern = args[1].itemAt(0).getStringValue();
                String flags = "";
                if (args.length == 3) {
                    flags = args[2].itemAt(0).getStringValue();
                }
                analyzeString(builder, input, pattern, flags);
            }
            builder.endElement();
            builder.endDocument();
            return (NodeValue) builder.getDocument().getDocumentElement();
        } finally {
            context.popDocumentContext();
        }
    }

    private void analyzeString(final MemTreeBuilder builder, final String input, String pattern, final String flags) throws XPathException {
        final Configuration config = context.getBroker().getBrokerPool().getSaxonConfiguration();

        final List<String> warnings = new ArrayList<>(1);

        try {
            final RegularExpression regularExpression = config.compileRegularExpression(pattern, flags, "XP30", warnings);
            if (regularExpression.matches("")) {
                throw new XPathException(this, ErrorCodes.FORX0003, "regular expression could match empty string");
            }

            //TODO(AR) cache the regular expression... might be possible through Saxon config

            final RegexIterator regexIterator = regularExpression.analyze(input);
            Item item;
            while ((item = regexIterator.next()) != null) {
                if (regexIterator.isMatching()) {
                    match(builder, regexIterator);
                } else {
                    nonMatch(builder, item);
                }
            }

            for (final String warning : warnings) {
                LOG.warn(warning);
            }
        } catch (final net.sf.saxon.trans.XPathException e) {
            switch (e.getErrorCodeLocalPart()) {
                case "FORX0001":
                    throw new XPathException(this, ErrorCodes.FORX0001, e.getMessage());
                case "FORX0002":
                    throw new XPathException(this, ErrorCodes.FORX0002, e.getMessage());
                case "FORX0003":
                    throw new XPathException(this, ErrorCodes.FORX0003, e.getMessage());
                default:
                    throw new XPathException(this, e.getMessage());
            }
        }
    }
    
    private void match(final MemTreeBuilder builder, final RegexIterator regexIterator) throws net.sf.saxon.trans.XPathException {
        builder.startElement(QN_MATCH, null);

        try {
            final AbstractSaxonRegexMatchHandler matchHandler = SAXON_MATCH_HANDLER_CLASS_CONSTRUCTOR.newInstance(builder);
            SAXON_PROCESS_MATCHING_SUBSTRING_FN.invoke(regexIterator, matchHandler);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new net.sf.saxon.trans.XPathException("Unable to dynamically invoke net.sf.saxon.regex.RegexIterator#processMatchingSubsOkay,tring: " + e.getMessage(), e);
        }

        builder.endElement();
    }

    private static Method getSaxonProcessMatchingSubstringFunction() {
        final String saxonVersion = net.sf.saxon.Version.getProductVersion();
        try {
            final Class<?> matchHandlerInterfaceClazz;
            if (saxonVersion.startsWith("12.")) {
                matchHandlerInterfaceClazz = getSaxon12MatchHandlerInterfaceClass();
            } else {
                matchHandlerInterfaceClazz = getSaxon9MatchHandlerInterfaceClass();
            }

            return RegexIterator.class.getMethod("processMatchingSubstring", matchHandlerInterfaceClazz);

        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to dynamically access Saxon RegexIterator#processMatchingSubstring method: " + e.getMessage(), e);
        }
    }

    private static Constructor<? extends AbstractSaxonRegexMatchHandler> getSaxonMatchHandlerClassConstructor(final Class<? extends AbstractSaxonRegexMatchHandler> saxonMatchHandlerClass) {
        try {
            return saxonMatchHandlerClass.getDeclaredConstructor(MemTreeBuilder.class);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Unable to get constructor of dynamic Saxon Match Handler class: " + e.getMessage(), e);
        }
    }

    private static Class<? extends AbstractSaxonRegexMatchHandler> createSaxonMatchHandlerClass() {
        final String saxonVersion = net.sf.saxon.Version.getProductVersion();
        try {
            if (saxonVersion.startsWith("12.")) {
                return createSaxon12MatchHandlerClass();
            } else {
                return createSaxon9MatchHandlerClass();
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to dynamically create Saxon Match Handler class: " + e.getMessage(), e);
        }
    }

    private static Class<?> getSaxon12MatchHandlerInterfaceClass() throws ClassNotFoundException {
        return Class.forName("net.sf.saxon.regex.RegexMatchHandler");
    }

    private static Class<?> getSaxon9MatchHandlerInterfaceClass() throws ClassNotFoundException {
        return Class.forName("net.sf.saxon.regex.RegexIterator$MatchHandler");
    }

    private static Class<? extends AbstractSaxonRegexMatchHandler> createSaxon12MatchHandlerClass() throws ClassNotFoundException {
        final Class<?> matchHandlerInterfaceClazz = getSaxon12MatchHandlerInterfaceClass();
        return createSaxonMatchHandlerClass(matchHandlerInterfaceClazz);
    }

    private static Class<? extends AbstractSaxonRegexMatchHandler> createSaxon9MatchHandlerClass() throws ClassNotFoundException {
        final Class<?> matchHandlerInterfaceClazz = getSaxon9MatchHandlerInterfaceClass();
        return createSaxonMatchHandlerClass(matchHandlerInterfaceClazz);
    }

    private static Class<? extends AbstractSaxonRegexMatchHandler> createSaxonMatchHandlerClass(final Class<?> saxonMatchHandlerInterface) throws IllegalStateException{
        try (final DynamicType.Unloaded<AbstractSaxonRegexMatchHandler> unloadedAbstractSaxonRegexMatchHandler = new ByteBuddy().subclass(AbstractSaxonRegexMatchHandler.class)
                .implement(saxonMatchHandlerInterface)
                .method(ElementMatchers.named("characters"))
                .intercept(InvocationHandlerAdapter.of(CHARACTERS_HANDLER))
                .make()) {

            return unloadedAbstractSaxonRegexMatchHandler
                    .load(AbstractSaxonRegexMatchHandler.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
        }
    }

    /**
     * Implements the common methods of Saxon 9's net.sf.saxon.regex.RegexIterator$MatchHandler
     * and Saxon 12's net.sf.saxon.regex.RegexMatchHandler
     */
    private static abstract class AbstractSaxonRegexMatchHandler {
        private final MemTreeBuilder builder;

        public AbstractSaxonRegexMatchHandler(final MemTreeBuilder builder) {
            this.builder = builder;
        }

        public void onGroupStart(final int groupNumber) {
            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", QN_NR.getLocalPart(), QN_NR.getLocalPart(), "int", Integer.toString(groupNumber));
            builder.startElement(QN_GROUP, attributes);
        }

        public void onGroupEnd(final int groupNumber) {
            builder.endElement();
        }
    }

    private void nonMatch(final MemTreeBuilder builder, final Item item) {
        builder.startElement(QN_NON_MATCH, null);
        builder.characters(item.getStringValueCS());
        builder.endElement();
    }
}
