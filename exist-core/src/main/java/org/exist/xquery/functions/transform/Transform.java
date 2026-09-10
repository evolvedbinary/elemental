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
package org.exist.xquery.functions.transform;

import com.evolvedbinary.j8fu.Either;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.Map;
import org.exist.dom.INodeHandle;
import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.dom.persistent.NodeProxy;
import org.exist.http.servlets.ResponseWrapper;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.serializers.FeatureKeys;
import org.exist.util.serializer.XQuerySerializer;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.BooleanValue;
import org.exist.xslt.SaxonConfiguration;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Option;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.fn.transform.Convert;
import org.exist.xquery.functions.fn.transform.Options;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;
import org.exist.xslt.XSLTErrorsListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xquery.FunctionDSL.arities;
import static org.exist.xquery.FunctionDSL.arity;
import static org.exist.xquery.FunctionDSL.optManyParam;
import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.returnsNothing;
import static org.exist.xquery.FunctionDSL.returnsOptMany;
import static org.exist.xquery.functions.transform.TransformModule.functionSignatures;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class Transform extends BasicFunction {

    private static final FunctionParameterSequenceType FS_PARAM_INPUT = optManyParam("input", Type.NODE, "A sequence of nodes to transform");
    private static final FunctionParameterSequenceType FS_PARAM_STYLESHEET = param("stylesheet", Type.ITEM, "The XSLT Stylesheet. Should be either a document-node(), element(), or a URI (xs:anyURI or xs:string).");
    private static final FunctionParameterSequenceType FS_PARAM_PARAMETERS = optParam("parameters", Type.ELEMENT, "Parameters to supply to the XSLT Stylesheet. The format of the value should be like: <parameters><param name=\"param-name-1\" value=\"param-value-1\"/></parameters>. There are two special parameters named \"exist:stop-on-warn\" and \"exist:stop-on-error\". If set to value \"yes\", an XQuery error will be raised if the XSLT processor reports a warning or error.");
    private static final FunctionParameterSequenceType FS_PARAM_ATTRIBUTES = optParam("attributes", Type.ELEMENT, "Attributes to set on the transformer. The format of the value should be like: <attributes><attr name=\"attr-name-1\" value=\"attr-value-1\"/></attributes>.");
    private static final FunctionParameterSequenceType FS_PARAM_SERIALIZATION_OPTIONS = optParam("serialization-options", Type.STRING, "Options to set on the serializer. The format of the value should be like: 'method=xml omit-xml-declaration=yes'.");

    private static final String FS_TRANSFORM_NAME = "transform";
    static final FunctionSignature[] FS_TRANSFORM = functionSignatures(
        FS_TRANSFORM_NAME,
        "Applies the XSLT Stylesheet in $" + FS_PARAM_STYLESHEET.getAttributeName() + " to each node in $" + FS_PARAM_INPUT.getAttributeName() + ".",
        returnsOptMany(Type.NODE, "The transformed nodes"),
            arities(
                    arity(
                            FS_PARAM_INPUT,
                            FS_PARAM_STYLESHEET,
                            FS_PARAM_PARAMETERS
                    ),
                    arity(
                            FS_PARAM_INPUT,
                            FS_PARAM_STYLESHEET,
                            FS_PARAM_PARAMETERS,
                            FS_PARAM_ATTRIBUTES,
                            FS_PARAM_SERIALIZATION_OPTIONS
                    )
            )
    );

    private static final String FS_STREAM_TRANSFORM_NAME = "stream-transform";
    static final FunctionSignature[] FS_STREAM_TRANSFORM = functionSignatures(
            FS_STREAM_TRANSFORM_NAME,
            "Similarly to " + FS_TRANSFORM[0].getName().getExtendedStringValue() + ", this applies the XSLT Stylesheet in $" + FS_PARAM_STYLESHEET.getAttributeName() + " to each node in $" + FS_PARAM_INPUT.getAttributeName() + ". However the output is streamed directly to the current HTTP response. Note this function can only be used in a HTTP context.",
            returnsNothing(),
            arities(
                    arity(
                            FS_PARAM_INPUT,
                            FS_PARAM_STYLESHEET,
                            FS_PARAM_PARAMETERS
                    ),
                    arity(
                            FS_PARAM_INPUT,
                            FS_PARAM_STYLESHEET,
                            FS_PARAM_PARAMETERS,
                            FS_PARAM_ATTRIBUTES,
                            FS_PARAM_SERIALIZATION_OPTIONS
                    )
            )
    );

    private static final StringValue OUTPUT_KEY = new StringValue("output");
    private final org.exist.xquery.functions.fn.transform.Transform transform;

    public Transform(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
        this.transform = new org.exist.xquery.functions.fn.transform.Transform(this);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Sequence inputSequence = args[0];
        final Either<StringValue, NodeValue> stylesheet = processStylesheetArgument(args[1].itemAt(0));

        // Parse any parameters
        final Parameters parameters;
        if (args[2].isEmpty()) {
            parameters = new Parameters(Map.empty(), false, false);
        } else {
            parameters = processParametersArgument((Element) ((NodeValue) args[2].itemAt(0)).getNode());
        }

        // Parse any TransformerFactory attributes
        @Nullable List<Tuple2<String, Object>> attributes = null;
        if (getArgumentCount() > 3 && !args[3].isEmpty()) {
            attributes = processAttributesArgument((Element) ((NodeValue) args[3].itemAt(0)).getNode());
        }

        // Parse any serialization options
        final Properties serializationOptions;
        final boolean expandXincludes;
        @Nullable final String xincludePath;
        if (getArgumentCount() > 4 && !args[4].isEmpty()) {
            serializationOptions = processSerializationOptionsArgument(args[4].itemAt(0).getStringValue());
            expandXincludes = "yes".equals(serializationOptions.getProperty(EXistOutputKeys.EXPAND_XINCLUDES, "yes"));
            if (expandXincludes) {
                @Nullable String xiPath = serializationOptions.getProperty(EXistOutputKeys.XINCLUDE_PATH);
                if (xiPath != null && !xiPath.startsWith(XmldbURI.XMLDB_URI_PREFIX)) {
                    final Path f = Paths.get(xiPath).normalize();
                    if (!f.isAbsolute()) {
                        xiPath = Paths.get(context.getModuleLoadPath(), xiPath).normalize().toAbsolutePath().toString();
                    }
                }
                xincludePath = xiPath;
            } else {
                xincludePath = null;
            }
        } else {
            serializationOptions = new Properties();
            expandXincludes = true;
            xincludePath = null;
        }

        // Get a Saxon configuration
        final SaxonConfiguration saxonConfiguration = SaxonConfiguration.getConfiguration(getContext().getBroker().getBrokerPool().getConfiguration(), attributes);

        // Perform the transformation
        final XSLTErrorsListener<XPathException> errorListener = new StopErrorListener(this, parameters.stopOnError, parameters.stopOnWarn);
        if (isCalledAs(FS_TRANSFORM_NAME)) {
            return transform(saxonConfiguration, inputSequence, stylesheet, parameters, contextSequence, expandXincludes, xincludePath, errorListener);

        } else if (isCalledAs(FS_STREAM_TRANSFORM_NAME)) {
            if (context.getHttpContext() == null) {
                throw new XPathException(this, ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "The function " + FS_STREAM_TRANSFORM[0].getName() + " requires an HTTP context");
            }
            final ResponseWrapper response =  context.getHttpContext().getResponse();
            @Nullable final String mediaType = serializationOptions.getProperty(OutputKeys.MEDIA_TYPE);
            if (mediaType != null) {
                @Nullable final String encoding = serializationOptions.getProperty(OutputKeys.ENCODING);
                if (encoding != null) {
                    response.setContentType(mediaType + "; charset=" + encoding);
                } else {
                    response.setContentType(mediaType);
                }
            }

            try (final Writer writer = new OutputStreamWriter(response.getOutputStream())) {
                final Sequence results = transform(saxonConfiguration, inputSequence, stylesheet, parameters, contextSequence, expandXincludes, xincludePath, errorListener);
                final XQuerySerializer xquerySerializer = new XQuerySerializer(getContext().getBroker(), serializationOptions, writer);
                xquerySerializer.serialize(results);

                // ensure the response is fully written
                response.flushBuffer();

            } catch (final IOException | SAXException e) {
                throw new XPathException(this, "IO error while calling "+ FS_STREAM_TRANSFORM[0].getName() + ": " + e.getMessage(), e);
            }

            return Sequence.EMPTY_SEQUENCE;

        } else {
            throw new XPathException(this, "Unknown function signature: " + getSignature());
        }
    }

    private Sequence transform(final SaxonConfiguration saxonConfiguration, final Sequence inputSequence, final Either<StringValue, NodeValue> stylesheet, final Parameters parameters, final Sequence contextSequence, final boolean expandXincludes, @Nullable final String xincludePath, final XSLTErrorsListener<XPathException> errorListener) throws XPathException {
        final Sequence results = new ValueSequence();

        // setup the stylesheet, parameters, and vendor options for the transformation
        final IMap<AtomicValue, Sequence> transformOptions = MapType.newLinearMap(getContext().getDefaultCollator());
        stylesheet.fold(location -> Options.STYLESHEET_LOCATION.set(transformOptions, location), node -> Options.STYLESHEET_NODE.set(transformOptions, node));
        Options.STYLESHEET_PARAMS.set(transformOptions, new MapType(this, getContext(), parameters.stylesheetParameters, Type.QNAME));
        final IMap<AtomicValue, Sequence> vendorOptions = MapType.newLinearMap(getContext().getDefaultCollator());
        Options.EXPAND_XINCLUDES.set(vendorOptions, expandXincludes ? BooleanValue.TRUE : BooleanValue.FALSE);
        if (xincludePath != null) {
            Options.XINCLUDE_PATH.set(vendorOptions, new StringValue(xincludePath));
        }
        Options.VENDOR_OPTIONS.set(transformOptions, new MapType(this, getContext(), vendorOptions.forked(), Type.QNAME));

        final SequenceIterator inputSequenceIterator = inputSequence.iterate();
        while (inputSequenceIterator.hasNext()) {
            NodeValue inputItem = (NodeValue) inputSequenceIterator.nextItem();

            // setup the source node for the transformation
            if (((INodeHandle<?>)inputItem).getNodeType() != Node.DOCUMENT_NODE) {
                // NOTE(AR) if the node is not a document we have to wrap it in a document to preserve the previous (bad) behaviour of the transform:transform function
                try {
                    final DocumentBuilderReceiver builder = new DocumentBuilderReceiver(this);
                    builder.startDocument();
                    if (inputItem instanceof NodeProxy) {
                        builder.addReferenceNode((NodeProxy) inputItem);
                    } else {
                        inputItem.copyTo(getContext().getBroker(), builder);
                    }
                    builder.endDocument();
                    inputItem = (NodeValue) builder.getDocument();
                } catch (final SAXException e) {
                    throw new XPathException(this, "Unable to wrap node type: " + inputItem.getNode().getNodeType() + " in an in-memory document");
                }
            }
            Options.SOURCE_NODE.set(transformOptions, inputItem);

            final Convert.ToSaxon toSaxon = new Convert.ToSaxon(saxonConfiguration.getProcessor());
            final Options options = new Options(this, saxonConfiguration, toSaxon, new MapType(this, getContext(), transformOptions.forked(), Type.STRING));

            context.pushDocumentContext();
            try {
                final MapType transformResult = transform.eval(saxonConfiguration, toSaxon, options, contextSequence, errorListener);

                errorListener.checkForErrors();

                final Sequence principalResultDocument = transformResult.get(OUTPUT_KEY);
                if (!principalResultDocument.isEmpty()) {
                    final Item principalResultItem = principalResultDocument.itemAt(0);
                    if (principalResultItem instanceof INodeHandle<?> && ((INodeHandle<?>) principalResultItem).getNodeType() == Node.DOCUMENT_NODE) {
                        // NOTE(AR) if the result is a document we have to unwrap its children to preserve the previous (bad) behaviour of the transform:transform function
                        @Nullable Node node = ((Document) principalResultItem).getFirstChild();
                        while (node != null) {
                            results.add((NodeValue) node);
                            node = node.getNextSibling();
                        }

                    } else {
                        results.add(principalResultItem);
                    }
                }
            } finally {
                context.popDocumentContext();
            }
        }

        return results;
    }

    private Properties processSerializationOptionsArgument(final String serializationOptions) throws XPathException {
        final Properties serializationProps = new Properties();
        final String[] options = Option.tokenize(serializationOptions);
        for (final String option : options) {
            final String[] nameValue = Option.parseKeyValuePair(option);
            if (nameValue == null) {
                throw new XPathException(this, "Found invalid serialization option: " + option);
            }
            serializationProps.setProperty(nameValue[0], nameValue[1]);
        }
        return serializationProps;
    }

    private @Nullable List<Tuple2<String, Object>> processAttributesArgument(final Element attributes) throws XPathException {
        @Nullable List<Tuple2<String, Object>> transformerFactoryAttributes = null;

        final NodeList attrs = attributes.getElementsByTagName("attr");
        for (int i = 0; i < attrs.getLength(); i++) {
            final Element attr = (Element) attrs.item(i);
            final String name = attr.getAttribute("name");
            final String value = attr.getAttribute("value");
            if (name.isEmpty()) {
                throw new XPathException(this, "Attributes name attribute is missing");
            } else {
                if (transformerFactoryAttributes == null) {
                    transformerFactoryAttributes = new ArrayList<>();
                }
                transformerFactoryAttributes.add(Tuple(name, value));
            }
        }

        return transformerFactoryAttributes;
    }

    private Parameters processParametersArgument(final Element parameters) throws XPathException {
        final IMap<AtomicValue, Sequence> stylesheetParameters = MapType.newLinearMap(getContext().getDefaultCollator());
        boolean stopOnWarn = false;
        boolean stopOnError = false;

        final NodeList params = parameters.getElementsByTagName("param");
        for (int i = 0; i < params.getLength(); i++) {
            final Element param = (Element) params.item(i);
            final String name = param.getAttribute("name");
            final String value = param.getAttribute("value");
            if (name.isEmpty()) {
                throw new XPathException(this, "Parameters name attribute is missing");
            } else if ("exist:stop-on-warn".equals(name)) {
                stopOnWarn = "yes".equals(value);
            } else if ("exist:stop-on-error".equals(name)) {
                stopOnError = "yes".equals(value);
            } else {
                stylesheetParameters.put(new QNameValue(this, getContext(), name), new StringValue(this, value));
            }
        }

        return new Parameters(stylesheetParameters.forked(), stopOnWarn, stopOnError);
    }

    private Either<StringValue, NodeValue> processStylesheetArgument(final Item stylesheetArgument) throws XPathException {
        if (stylesheetArgument instanceof NodeValue) {
            return Either.Right((NodeValue) stylesheetArgument);

        } else if (stylesheetArgument instanceof AnyURIValue) {
            return Either.Left((StringValue) stylesheetArgument.convertTo(Type.STRING));

        } else if (stylesheetArgument instanceof StringValue) {
            return Either.Left((StringValue) stylesheetArgument);
        }

        throw new XPathException(this, "The parameter $" + FS_PARAM_STYLESHEET.getAttributeName() + " must be of type document-node(), element(), xs:anyURI, or xs:string");
    }

    private static class Parameters {
        final IMap<AtomicValue, Sequence> stylesheetParameters;
        final boolean stopOnWarn;
        final boolean stopOnError;

        private Parameters(final IMap<AtomicValue, Sequence> stylesheetParameters, final boolean stopOnWarn, final boolean stopOnError) {
            this.stylesheetParameters = stylesheetParameters;
            this.stopOnWarn = stopOnWarn;
            this.stopOnError = stopOnError;
        }
    }

    private static class StopErrorListener extends XSLTErrorsListener<XPathException> {
        private final Expression callingExpression;

        private StopErrorListener(final Expression callingExpression, final boolean stopOnError, final boolean stopOnWarn) {
            super(stopOnError, stopOnWarn);
            this.callingExpression = callingExpression;
        }

        @Override
        protected void raiseError(final String error, final TransformerException ex) throws XPathException {
            throw new XPathException(callingExpression, error, ex);
        }
    }
}
