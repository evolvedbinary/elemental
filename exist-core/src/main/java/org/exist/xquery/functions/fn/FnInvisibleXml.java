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

package org.exist.xquery.functions.fn;

import static com.evolvedbinary.j8fu.Either.Left;
import static com.evolvedbinary.j8fu.Either.Right;
import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.functions.fn.FnModule.functionSignature;
import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.returns;

import com.evolvedbinary.j8fu.Either;
import org.exist.Namespaces;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.util.XMLReaderPool;
import org.exist.xquery.*;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.w3c.dom.Element;
import org.xml.sax.*;

import de.bottlecaps.markup.Blitz;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;

public class FnInvisibleXml extends BasicFunction {

    private static final String FS_INVISIBLE_XML_NAME = "invisible-xml";

    final static FunctionSignature FS_INVISIBLE_XML = functionSignature(
            FS_INVISIBLE_XML_NAME,
            "Evaluates invisible XML.",
            returns(Type.FUNCTION, "The iXML parsing function"),
            optParam("grammar", Type.ITEM, "The iXML grammar"),
            optParam("options", Type.MAP_ITEM, "Options for the iXML parser")
    );

    public FnInvisibleXml(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Sequence optionsArg = args[1];
        @Nullable final MapType options;
        if (optionsArg.isEmpty()) {
            options = null;
        } else {
            options = optionsArg.itemAt(0).toJavaObject(MapType.class);
        }

        final IxmlParserFunction fn;
        final Sequence grammarArg = args[0];
        if (grammarArg.isEmpty()) {
            // no grammar provided
            fn = new IxmlParserFunction(context, (StringValue) null, options);

        } else if (grammarArg.getItemType() == Type.STRING) {
            // grammar is a string
            final StringValue grammarString = grammarArg.itemAt(0).toJavaObject(StringValue.class);
            fn = new IxmlParserFunction(context, grammarString, options);

        } else {
            // grammar is an element
            final Element grammarElement = grammarArg.itemAt(0).toJavaObject(Element.class);
            fn = new IxmlParserFunction(context, grammarElement, options);
        }

        final InlineFunction functionResult = new InlineFunction(context, fn);
        return functionResult.eval(contextSequence, null);
    }

    private static class IxmlParserFunction extends UserDefinedFunction {

        private static final String FS_PARSE_INVISIBLE_XML_NAME = "parse-invisible-xml";
        private static final FunctionSignature FS_PARSE_INVISIBLE_XML = functionSignature(
                FS_PARSE_INVISIBLE_XML_NAME,
                "Gets the next random number generator.",
                returns(Type.DOCUMENT, "just a random string for now"),
                param("Parser inpuit", Type.STRING, "param description")
        );

        @Nullable final Either<StringValue, Element> grammar;
        @Nullable final MapType options;

        IxmlParserFunction(final XQueryContext context, @Nullable final StringValue grammar, @Nullable final MapType options) {
            super(context, FS_PARSE_INVISIBLE_XML);
            this.grammar = Left(grammar);
            this.options = options;
        }

        IxmlParserFunction(final XQueryContext context, @Nullable final Element grammar, @Nullable final MapType options) {
            super(context, FS_PARSE_INVISIBLE_XML);
            this.grammar = Right(grammar);
            this.options = options;
        }

        @Override
        public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {

            // get the input
            final Sequence inputArg = getArguments(contextSequence, contextItem)[0];
            final String input = inputArg.itemAt(0).getStringValue();

            // generate the default ixml grammar
            final String ixmlGrammar;
            if (grammar == null) {
                ixmlGrammar = Blitz.ixmlGrammar();
            } else if (grammar.isLeft()) {
                ixmlGrammar = grammar.left().get().getStringValue();
            } else {
                // TODO(YB) use XQuerySerializer class to serialize Element to String
                ixmlGrammar = ...
            }

            // TODO(YB) set any options

            // parse the input using the ixml grammar
            final String generatedXML = Blitz.generate(ixmlGrammar).parse(input);

            return parse(generatedXML);
        }

        private Sequence parse(final String xmlContent) throws XPathException {
            final XMLReaderPool pool = context.getBroker().getBrokerPool().getParserPool();
            final SAXAdapter adapter = new SAXAdapter(context);
            final XMLReader reader = pool.borrowXMLReader();
            try (final StringReader stringReader = new StringReader(xmlContent)) {
                reader.setContentHandler(adapter);
                reader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, adapter);
                reader.parse(new InputSource(stringReader));
                return adapter.getDocument();
            } catch (final SAXException | IOException e) {
                // TODO(YB) add error code
                throw new XPathException(this, e.getMessage(), e);
            } finally {
                pool.returnXMLReader(reader);
            }

        }
    }
}
