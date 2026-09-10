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
package org.exist.xquery.functions.fn.transform;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lacuna.bifurcan.IEntry;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.UncheckedXPathException;

import java.io.IOException;
import java.io.StringWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.INodeHandle;
import org.exist.dom.QName;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.serializers.NodeValueInputSource;
import org.exist.util.Holder;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.exist.xslt.SaxonConfiguration;
import org.exist.xslt.XsltURIResolverHelper;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.util.StringUtil.isNullOrEmpty;
import static org.exist.util.StringUtil.notNullOrEmpty;
import static org.exist.xquery.functions.fn.transform.Options.Option.*;

/**
 * Implementation of fn:transform.
 *
 * This is the core of the eval() function of fn:transform
 * It is a separate class due to the multiplicity of options that must be dealt with,
 * which lead to too much code in a single file.
 *
 * This class contains the core of the logic
 * - create an XSLT compiler (if we don't have one compiled already, which matches our stylesheet)
 * - set parameters on the compiler
 * - create a transformer from the compiler
 * - set parameters on the transformer (inputs)
 * - invoke the transformation
 * - deliver and postprocess the output in the required form
 *
 * The parsing and checking of the options to fn:transform is isolated in {@link Options}
 * Delivery and output of the result, depending on the required format, is in {@link Delivery}
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:alan@evolvedbinary.com">Alan Paxton</a>
 */
public class Transform {

    private static Logger LOGGER =  LogManager.getLogger(org.exist.xquery.functions.fn.transform.Transform.class);
    private static final org.exist.xquery.functions.fn.transform.Transform.ErrorListenerLog4jAdapter ERROR_LISTENER = new Transform.ErrorListenerLog4jAdapter(Transform.LOGGER);

    private static final Cache<String, XsltExecutable> XSLT_EXECUTABLE_CACHE = Caffeine.newBuilder()
            .maximumSize(25)
            .weakValues()
            .build();

    private final Expression callingExpression;

    public Transform(final Expression callingExpression) {
        this.callingExpression = callingExpression;
    }

    public MapType eval(final SaxonConfiguration saxonConfiguration, final Convert.ToSaxon toSaxon, final Options options, final Sequence contextSequence, @Nullable final ErrorListener errorListener) throws XPathException {
        if (!(options.xsltVersion.equals(V1_0) || options.xsltVersion.equals(V2_0) || options.xsltVersion.equals(V3_0))) {
            throw new XPathException(callingExpression, ErrorCodes.FOXT0001, "xslt-version: " + options.xsltVersion + " is not supported.");
        }

        try {
            final Holder<XPathException> compileException = new Holder<>();
            final XsltExecutable xsltExecutable;
            if (options.shouldCache.orElse(BooleanValue.TRUE).getValue()) {
                xsltExecutable = Transform.XSLT_EXECUTABLE_CACHE.get(executableCacheKey(options), key -> {
                    try {
                        return compileExecutable(saxonConfiguration, toSaxon, options);
                    } catch (final XPathException e) {
                        compileException.value = e;
                        return null;
                    }
                });
            } else {
                xsltExecutable = compileExecutable(saxonConfiguration, toSaxon, options);
            }

            if (compileException.value != null) {
                // if we could not compile the xslt, rethrow the error
                throw compileException.value;
            }
            if (xsltExecutable == null) {
                throw new XPathException(callingExpression, ErrorCodes.FOXT0003, "Unable to compile stylesheet (No error returned from compilation)");
            }

            final Xslt30Transformer xslt30Transformer = xsltExecutable.load30();
            xslt30Transformer.setMessageListener(new XsltMessageListener(saxonConfiguration.getProcessor(), getLogger()));
            @Nullable final String base = options.resolvedStylesheetBaseURI.map(AnyURIValue::getStringValue).orElse(null);
            final URIResolver uriResolver = XsltURIResolverHelper.getXsltURIResolver(callingExpression.getContext().getBroker().getBrokerPool(), xslt30Transformer.getURIResolver(), base, true);
            xslt30Transformer.setURIResolver(uriResolver);

            options.initialMode.ifPresent(qNameValue -> xslt30Transformer.setInitialMode(Convert.ToSaxon.of(qNameValue.getQName())));
            xslt30Transformer.setInitialTemplateParameters(options.templateParams, false);
            xslt30Transformer.setInitialTemplateParameters(options.tunnelParams, true);
            if (errorListener != null) {
                xslt30Transformer.setErrorListener(errorListener);
            }
            if (options.baseOutputURI.isPresent()) {
                final AtomicValue baseOutputURI = options.baseOutputURI.get();
                final AtomicValue asString = baseOutputURI.convertTo(Type.STRING);
                if (asString instanceof StringValue) {
                    xslt30Transformer.setBaseOutputURI(asString.getStringValue());
                }
            }

            // The delivery mechanism
            final SerializationProperties serializationProperties =
                    SerializationParameters.getAsSerializationProperties(
                            options.serializationParams.orElse(new MapType(callingExpression.getContext())),
                            (code, message) -> new XPathException(callingExpression, code, message));
            final Delivery delivery = new Delivery(callingExpression.getContext(), options.deliveryFormat, serializationProperties);

            // Record the secondary result documents generated
            final Map<URI, Delivery> resultDocuments = new HashMap<>();
            xslt30Transformer.setResultDocumentHandler(resultDocumentURI -> {
                final Delivery resultDelivery = new Delivery(callingExpression.getContext(), options.deliveryFormat, serializationProperties);
                resultDocuments.put(resultDocumentURI, resultDelivery);
                return resultDelivery.createDestination(xslt30Transformer, true);
            });

            if (options.globalContextItem.isPresent()) {
                final Item globalContextItem = options.globalContextItem.get();
                if (globalContextItem instanceof NodeValue) {
                    final NodeValue globalContextItemNodeValue = (NodeValue) globalContextItem;
                    final boolean globalContextItemIsDocument = ((INodeHandle<?>) globalContextItemNodeValue).getNodeType() == Node.DOCUMENT_NODE;

                    // read the global-context-item from Elemental and transform it with Saxon
                    final InputSource globalContextItemInputSource = new NodeValueInputSource(globalContextItemNodeValue, getBaseURI(globalContextItemNodeValue, callingExpression));
                    final org.exist.storage.serializers.Serializer xmlReader = callingExpression.getContext().getBroker().borrowSerializer();
                    try {
                        configureXmlReader(xmlReader, options);
                        final Source source = new SAXSource(xmlReader, globalContextItemInputSource);
                        final DocumentBuilder globalContextItemBuilder = toSaxon.newDocumentBuilder();
                        XdmNode xdmNode = globalContextItemBuilder.build(source);
                        // TODO(AR) START TEMP
                        if (!globalContextItemIsDocument) {
                            final Iterator<XdmNode> children = xdmNode.children().iterator();
                            if (children.hasNext()) {
                                xdmNode = children.next();
                            }
                        }
                        // TODO(AR) END TEMP
                        xslt30Transformer.setGlobalContextItem(xdmNode);

                    } finally {
                        callingExpression.getContext().getBroker().returnSerializer(xmlReader);
                    }
                } else {
                    final XdmItem xdmItem = (XdmItem) toSaxon.of(globalContextItem);
                    xslt30Transformer.setGlobalContextItem(xdmItem);
                }

            } else if (options.sourceNode.isPresent()) {
                // set the global-context-item as the root of the tree containing the source node
                NodeValue globalContextItemNodeValue = options.sourceNode.get();

                if (((INodeHandle<?>) globalContextItemNodeValue).getNodeType() != Node.DOCUMENT_NODE) {
                    // global-context-item is not at the root of the tree, so set it to the root
                    if (globalContextItemNodeValue.getImplementationType() == NodeValue.PERSISTENT_NODE) {
                        // Persistent DOM
                        globalContextItemNodeValue = NodeProxy.wrap(callingExpression, (DocumentImpl) globalContextItemNodeValue.getOwnerDocument());
                    } else {
                        // In-Memory DOM
                        globalContextItemNodeValue = (org.exist.dom.memtree.DocumentImpl) globalContextItemNodeValue.getOwnerDocument();
                    }
                }

                // read the global-context-item from Elemental and set it in Saxon
                final InputSource globalContextItemInputSource = new NodeValueInputSource(globalContextItemNodeValue, getBaseURI(globalContextItemNodeValue, callingExpression));

                final org.exist.storage.serializers.Serializer xmlReader = callingExpression.getContext().getBroker().borrowSerializer();
                try {
                    configureXmlReader(xmlReader, options);
                    final Source source = new SAXSource(xmlReader, globalContextItemInputSource);
                    final DocumentBuilder sourceBuilder = toSaxon.newDocumentBuilder();
                    final XdmNode xdmNode = sourceBuilder.build(source);
                    xslt30Transformer.setGlobalContextItem(xdmNode);

                    //TODO(AR) remove this after testing
//                DOMSource source = (DOMSource) sourceNode.get();
//                Node node = source.getNode();
//                if (node.getNodeType() != Node.DOCUMENT_NODE) {
//                    // not at the root of the tree, so get the root
//                    node = node.getOwnerDocument();
//                    source = new DOMSource(node, node.getBaseURI());
//                }

//                final DocumentBuilder sourceBuilder = toSaxon.newDocumentBuilder();
//                final XdmNode xdmNode = sourceBuilder.build(source);
//                xslt30Transformer.setGlobalContextItem(xdmNode);

                } finally {
                    callingExpression.getContext().getBroker().returnSerializer(xmlReader);
                }

            } else {
                xslt30Transformer.setGlobalContextItem(null);
            }

            final Transform.TemplateInvocation invocation = new Transform.TemplateInvocation(
                    options, delivery, xslt30Transformer, resultDocuments);
            return invocation.invoke(toSaxon);

        } catch (final SaxonApiException e) {
            throw originalXPathException("Could not transform input using: " + options.xsltSource._1 + ", at line: " + e.getLineNumber() + ". Error: ", e, ErrorCodes.FOXT0003);
        } catch (final  UncheckedXPathException e) {
            final Location location = e.getXPathException().getLocator();
            int line = -1;
            int column = -1;
            if (location != null) {
                line = location.getLineNumber();
                column = location.getColumnNumber();
            }
            throw originalXPathException("Could not transform input using: " + options.xsltSource._1 + ", at line: " + line + ", column: " + column + ". Error: ", e, ErrorCodes.FOXT0003);
        }
    }


    private XsltExecutable compileExecutable(final SaxonConfiguration saxonConfiguration, final Convert.ToSaxon toSaxon, final Options options) throws XPathException {
        final XsltCompiler xsltCompiler = saxonConfiguration.getProcessor().newXsltCompiler();
        final SingleRequestErrorListener errorListener = new SingleRequestErrorListener(Transform.ERROR_LISTENER);
        xsltCompiler.setErrorListener(errorListener);

        for (final Map.Entry<net.sf.saxon.s9api.QName, XdmValue> entry : options.staticParams.entrySet()) {
            xsltCompiler.setParameter(entry.getKey(), entry.getValue());
        }

        for (final IEntry<AtomicValue, Sequence> entry : options.stylesheetParams) {
            final QName qKey = ((QNameValue) entry.key()).getQName();
            final XdmValue value = toSaxon.of(entry.value());
            xsltCompiler.setParameter(new net.sf.saxon.s9api.QName(qKey.getPrefix(), qKey.getLocalPart()), value);
        }

        @Nullable final String base = options.resolvedStylesheetBaseURI.map(AnyURIValue::getStringValue).orElse(null);
        final URIResolver uriResolver = XsltURIResolverHelper.getXsltURIResolver(callingExpression.getContext().getBroker().getBrokerPool(), xsltCompiler.getURIResolver(), base, true);
        xsltCompiler.setURIResolver(uriResolver);
//        xsltCompiler.setURIResolver(new URIResolution.CompileTimeURIResolver(callingExpression) {
//            @Override  public Source resolve(final String href, final String base) throws TransformerException {
//                // Correct error from URI resolution when there is no base
//                try {
//                    final URI hrefURI = URI.create(href);
//                    if (options.resolvedStylesheetBaseURI.isEmpty() && !hrefURI.isAbsolute() && isNullOrEmpty(base)) {
//                        final XPathException resolutionException = new XPathException(callingExpression,
//                            ErrorCodes.XTSE0165,
//                            "transform using a relative href, \n" +
//                                "using option stylesheet-text, but without stylesheet-base-uri");
//                        throw new TransformerException(resolutionException);
//                    }
//                } catch (final IllegalArgumentException e) {
//                    throw new TransformerException(e);
//                }
//                // Checked the special error case, defer to eXist resolution
//                return super.resolve(href, base);
//            }
//        });

        try {
            options.resolvedStylesheetBaseURI.ifPresent(anyURIValue -> options.xsltSource._2.setSystemId(anyURIValue.getStringValue()));
            return xsltCompiler.compile(options.xsltSource._2); //TODO(AR) need to implement support for xslt-packages
        } catch (final SaxonApiException e) {
            final Optional<Exception> compilerException = errorListener.getWorst().map(e1 -> e1);
            throw originalXPathException("Could not compile stylesheet: ", compilerException.orElse(e), ErrorCodes.FOXT0003);
        }
    }

    /**
     * Search for an XPathException in the cause chain, and return it "directly"
     * Either an eXist XPathException, which is immediate
     * Or a Saxon XPathException, when we convert it to something similar in eXist.
     *
     * @param e the top of the exception stack
     * @param defaultErrorCode use this code and its description to fill in blanks in what we finally throw
     * @return XPathException the eventual eXist exception which the caller is expected to throw
     */
    private XPathException originalXPathException(final String prefix, @Nonnull final Throwable e, final ErrorCodes.ErrorCode defaultErrorCode) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof XPathException) {
                return new XPathException(callingExpression, ((XPathException) cause).getErrorCode(), prefix + cause.getMessage(), cause);
            }
            cause = cause.getCause();
        }

        cause = e;
        while (cause != null) {
            if (cause instanceof final net.sf.saxon.trans.XPathException xPathException) {
                final StructuredQName from = xPathException.getErrorCodeQName();
                if (from != null) {
                    final QName errorCodeQName = new QName(from.getLocalPart(), from.getURI(), from.getPrefix());
                    ErrorCodes.ErrorCode errorCode = null;
                    try {
                        errorCode = ErrorCodes.fromQName(errorCodeQName);
                    } catch (final IllegalArgumentException ee) {
                        errorCode = new ErrorCodes.DynamicErrorCode(errorCodeQName, cause.getMessage());
                    }
                    return new XPathException(callingExpression, errorCode, prefix + cause.getMessage());
                } else {
                    return new XPathException(callingExpression, defaultErrorCode, prefix + cause.getMessage());
                }
            }
            cause = cause.getCause();
        }

        return new XPathException(callingExpression, defaultErrorCode, prefix + e.getMessage());
    }

    /**
     * Hash on the options used to create a compiled executable
     * Hash should match only when the executable can be re-used.
     *
     * @param options options to read
     * @return a string, the hash we want
     */
    private String executableCacheKey(final Options options) {
        // TODO(AR) this needs improving should use a dedicated class ExecutableCacheKey for the return value - that class should contain only members that need to be compared for equality to determine the key
        final String uniquifier;
        if (options.resolvedStylesheetBaseURI.isPresent() || options.sourceTextChecksum.isPresent()) {
            uniquifier = "";
        } else {
            uniquifier = LocalDateTime.now().toString();
        }
        final String paramHash = Tuple(
                options.stylesheetParams,
                options.staticParams).toString();

        final String locationHash = Tuple(
                options.resolvedStylesheetBaseURI.map(AnyURIValue::getStringValue).orElse(""),
                options.sourceTextChecksum.orElse(0L),
                uniquifier,
                options.stylesheetNodeDocumentPath,
                options.stylesheetNodeDocumentPath).toString();

        return Tuple(options.saxonConfiguration.getConfiguration().hashCode(), locationHash, paramHash).toString();
    }

    private static void configureXmlReader(final org.exist.storage.serializers.Serializer xmlReader, final Options options) throws XPathException {
        if (options.vendorOptions.isPresent()) {
            final MapType vendorOptions = options.vendorOptions.get();
            final boolean expandXincludes = Options.EXPAND_XINCLUDES.get(options.xsltVersion, vendorOptions).map(BooleanValue::getValue).orElse(false);
            try {
                xmlReader.setProperty(EXistOutputKeys.EXPAND_XINCLUDES, expandXincludes ? "yes" : "no");
            } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                // no-op - exception will never be thrown in practice
            }
            if (expandXincludes) {
                @Nullable final String xincludePath = Options.XINCLUDE_PATH.get(options.xsltVersion, vendorOptions).map(StringValue::getStringValue).orElse(null);
                if (xincludePath != null) {
                    xmlReader.getXIncludeFilter().setModuleLoadPath(xincludePath);
                }
            }
        }
    }

    private static @Nullable String getBaseURI(final NodeValue nodeValue, final Expression callingExpression) {
        @Nullable String baseUri = nodeValue.getNode().getBaseURI();
        if (baseUri == null) {
            try {
                final AnyURIValue contextBaseURI = callingExpression.getContext().getBaseURI();
                if (contextBaseURI != null) {
                    baseUri = contextBaseURI.getStringValue();
                }
            } catch (final XPathException e) {
                // ignore
                return null;
            }
        }

        if (isNullOrEmpty(baseUri)) {
            return null;
        }

        return baseUri;
    }

    private class TemplateInvocation {

        final Options options;
        final Delivery delivery;
        final Destination destination;
        final Xslt30Transformer xslt30Transformer;
        final Map<URI, Delivery> resultDocuments;

        TemplateInvocation(final Options options, final Delivery delivery, final Xslt30Transformer xslt30Transformer, final Map<URI, Delivery> resultDocuments) {
            this.options = options;
            this.delivery = delivery;
            this.destination = delivery.createDestination(xslt30Transformer, false);
            this.xslt30Transformer = xslt30Transformer;
            this.resultDocuments = resultDocuments;
        }

        private MapType invokeCallFunction(final Convert.ToSaxon toSaxon) throws XPathException, SaxonApiException {
            assert options.initialFunction.isPresent();
            final net.sf.saxon.s9api.QName qName = Convert.ToSaxon.of(options.initialFunction.get().getQName());
            final XdmValue[] functionParams;
            if (options.functionParams.isPresent()) {
                functionParams = toSaxon.of(options.functionParams.get());
            } else {
                throw new XPathException(callingExpression, ErrorCodes.FOXT0002, "Error - transform using XSLT 3.0 option initial-function, but the corresponding option function-params was not supplied.");
            }

            xslt30Transformer.callFunction(qName, functionParams, destination);
            return makeResultMap(options, delivery, resultDocuments);
        }

        private MapType invokeCallTemplate() throws XPathException, SaxonApiException {
            assert options.initialTemplate.isPresent();
            if (options.initialMode.isPresent()) {
                throw new XPathException(callingExpression, ErrorCodes.FOXT0002,
                        Options.INITIAL_MODE.name + " supplied indicating apply-templates invocation, " +
                                "AND " + Options.INITIAL_TEMPLATE.name + " supplied indicating call-template invocation.");
            }

            // Convert using our own {@link Convert} class
            // The saxDestination conversion loses type information in some cases
            // e.g. fn-transform-63 from XQTS has a <xsl:template name='main' as='xs:integer'>
            // which alongside "delivery-format":"raw" fails to deliver an int

            final QName qName = options.initialTemplate.get().getQName();
            xslt30Transformer.callTemplate(Convert.ToSaxon.of(qName), destination);
            return makeResultMap(options, delivery, resultDocuments);
        }

        private MapType invokeApplyTemplates(final Convert.ToSaxon toSaxon) throws XPathException, SaxonApiException {
            if (options.initialMatchSelection.isPresent()) {
                final Sequence initialMatchSelection = options.initialMatchSelection.get();
                final Item initialMatchSelectionItem = initialMatchSelection.itemAt(0);
                if (initialMatchSelectionItem instanceof NodeValue) {

                    final NodeValue initialMatchSelectionNodeValue = (NodeValue) initialMatchSelectionItem;

                    // read the initial match selection from Elemental and transform it with Saxon
                    final InputSource initialMatchSelectionInputSource = new NodeValueInputSource(initialMatchSelectionNodeValue, getBaseURI(initialMatchSelectionNodeValue, callingExpression));
                    final org.exist.storage.serializers.Serializer xmlReader = callingExpression.getContext().getBroker().borrowSerializer();
                    try {
                        configureXmlReader(xmlReader, options);
                        final Source source = new SAXSource(xmlReader, initialMatchSelectionInputSource);
                        xslt30Transformer.applyTemplates(source, destination);

                    } finally {
                        callingExpression.getContext().getBroker().returnSerializer(xmlReader);
                    }

                    // TODO (AR) remove this after testing
//                    final Source sourceIMS = new DOMSource((Document)item, callingExpression.getContext().getBaseURI().getStringValue());
//                    xslt30Transformer.applyTemplates(sourceIMS, destination);
                } else {
                    final XdmValue selection = toSaxon.of(initialMatchSelection);
                    xslt30Transformer.applyTemplates(selection, destination);
                }
            } else if (options.sourceNode.isPresent()) {
                final NodeValue sourceNode = options.sourceNode.get();
                final boolean sourceNodeIsDocument = ((INodeHandle<?>) sourceNode).getNodeType() == Node.DOCUMENT_NODE;

                // read the source node from Elemental and transform it with Saxon
                final InputSource sourceNodeInputSource = new NodeValueInputSource(sourceNode, getBaseURI(sourceNode, callingExpression));
                final org.exist.storage.serializers.Serializer xmlReader = callingExpression.getContext().getBroker().borrowSerializer();
                try {
                    configureXmlReader(xmlReader, options);

                    // TODO(AR) START TEMP
                    if (!sourceNodeIsDocument) {
                        try {
                            xmlReader.setProperty(org.exist.storage.serializers.Serializer.GENERATE_DOC_EVENTS, "false");
                        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                            // no-op
                        }
                    }
                    // TODO(AR) END TEMP

                    final Source source = new SAXSource(xmlReader, sourceNodeInputSource);
                    xslt30Transformer.applyTemplates(source, destination);

                } finally {
                    callingExpression.getContext().getBroker().returnSerializer(xmlReader);
                }
            } else {
                throw new XPathException(callingExpression,
                        ErrorCodes.FOXT0002,
                        "One of " + Options.SOURCE_NODE.name + " or " +
                                Options.INITIAL_MATCH_SELECTION.name + " or " +
                                Options.INITIAL_TEMPLATE.name + " or " +
                                Options.INITIAL_FUNCTION.name + " is required.");
            }
            return makeResultMap(options, delivery, resultDocuments);
        }

        MapType invoke(final Convert.ToSaxon toSaxon) throws XPathException, SaxonApiException {
            if (options.initialFunction.isPresent()) {
                return invokeCallFunction(toSaxon);
            } else if (options.initialTemplate.isPresent()) {
                return invokeCallTemplate();
            } else {
                return invokeApplyTemplates(toSaxon);
            }
        }

        private MapType makeResultMap(final Options options, final Delivery primaryDelivery, final Map<URI, Delivery> resultDocuments) throws XPathException {

            try (final MapType outputMap = new MapType(callingExpression.getContext())) {
                final AtomicValue outputKey;
                outputKey = options.baseOutputURI.orElseGet(() -> new StringValue("output"));

                final Sequence primaryValue = postProcess(outputKey, primaryDelivery.convert(), options.postProcess);
                outputMap.add(outputKey, primaryValue);

                for (final Map.Entry<URI, Delivery> resultDocument : resultDocuments.entrySet()) {
                    final AnyURIValue key = new AnyURIValue(resultDocument.getKey());
                    final Delivery secondaryDelivery = resultDocument.getValue();
                    final Sequence value = postProcess(key, secondaryDelivery.convert(), options.postProcess);
                    outputMap.add(key, value);
                }

                return outputMap;
            }
        }
    }

     private Sequence postProcess(final AtomicValue key, final Sequence before, final Optional<FunctionReference> postProcessingFunction) throws XPathException {
        if (postProcessingFunction.isPresent()) {
            FunctionReference functionReference = postProcessingFunction.get();
            return functionReference.evalFunction(null, null, new Sequence[]{key, before});
        } else {
            return before;
        }
    }

    /**
     * Designed to be package-protected accessible so that we can observe logging in tests.
     *
     * @param logger the logger to use in testing.
     */
    static void setLogger(final Logger logger) {
        LOGGER = logger;
    }

    private Logger getLogger() {
        return LOGGER;
    }

    private static class ErrorListenerLog4jAdapter implements ErrorListener {
        private final Logger logger;

        public ErrorListenerLog4jAdapter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public void warning(final TransformerException e) {
            logger.warn(e.getMessage(), e);
        }

        @Override
        public void error(final TransformerException e) {
            logger.error(e.getMessage(), e);
        }

        @Override
        public void fatalError(final TransformerException e) {
            logger.fatal(e.getMessage(), e);
        }
    }

    private static class SingleRequestErrorListener implements ErrorListener {

        private Optional<TransformerException> lastError;
        private Optional<TransformerException> lastFatal;

        public Optional<TransformerException> getWorst() {
            if (lastFatal.isPresent()) return lastFatal;
            return lastError;
        }

        private final ErrorListener global;
        SingleRequestErrorListener(ErrorListener global) {
            this.global = global;
        }

        @Override
        public void warning(TransformerException exception) throws TransformerException {
            global.warning(exception);
        }

        @Override
        public void error(TransformerException exception) throws TransformerException {
            lastError = Optional.of(exception);
            global.error(exception);
        }

        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            lastFatal = Optional.of(exception);
            global.fatalError(exception);
        }
    }

    /**
     * A convenience for throwing a checked exception within fn:transform support code,
     * without the {@link XQueryContext} necessary for an immediate XPathException.
     *
     * Useful in a static helper class, for instance.
     */
    static class PendingException extends Exception {

        public PendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class XsltMessageListener implements MessageListener {

        private final Processor processor;
        private final Logger logger;

        public XsltMessageListener(final Processor processor, final Logger logger) {
            this.processor = processor;
            this.logger = logger;
        }

        @Override
        public void message(final XdmNode content, final boolean terminate, final SourceLocator locator) {

            try (final StringWriter writer = new StringWriter()) {
                final Serializer serializer = processor.newSerializer();
                serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                serializer.setOutputWriter(writer);
                serializer.serializeNode(content);

                @Nullable final String source;
                final int sourceLine;
                final int sourceColumn;
                if (locator != null) {
                    source = locator.getSystemId();
                    sourceLine = locator.getLineNumber();
                    sourceColumn = locator.getColumnNumber();
                } else {
                    source = null;
                    sourceLine = -1;
                    sourceColumn = -1;
                }

                final StringBuilder tag = new StringBuilder("<xsl:message terminate=\"" + terminate + "\"");
                if (notNullOrEmpty(source)) {
                    tag.append(" source=\"").append(source).append("\"");
                }
                if (sourceLine != -1) {
                    tag.append(" sourceLine=\"").append(sourceLine).append("\"");
                    tag.append(" sourceColumn=\"").append(sourceColumn).append("\"");
                }
                tag.append(">");

                logger.info("{}{}</xsl:message>", tag.toString(), writer.toString());
            } catch (final SaxonApiException e) {
                logger.error("Unable to serialize xsl:message content", e);
            } catch (final IOException e) {
                logger.error("Unable to close xsl:message writer", e);
            }
        }
    }
}
