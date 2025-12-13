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
package xyz.elemental.xquery.modules.kwic;

import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.math.BigInteger;

import static org.exist.util.StringUtil.notNullOrEmptyOrWs;
import static org.exist.xquery.FunctionDSL.*;
import static xyz.elemental.xquery.modules.kwic.KwicModule.functionSignatures;

public class KwicSummary extends BasicFunction {

    private final static int DEFAULT_CHARACTER_SUMMARY_LENGTH = 120;
    private final static int DEFAULT_CHARACTER_KWIC_LENGTH = 40;

    private static final FunctionParameterSequenceType FS_PARAM_ROOT = param("root", Type.NODE, "The element which should be used as context for the match. It defines the boundaries for the text extraction.");
    private static final FunctionParameterSequenceType FS_PARAM_NODE = param("node", Type.ELEMENT, "An \"exist:match\" element, or an element with an \"exist:matches\" attribute to process.");
    private static final FunctionParameterSequenceType FS_PARAM_CONFIG = param("config", Type.ELEMENT, "A \"config\" element which configures the behaviour of the function. The following options are supported: <config table=\"true|false\" width=\"number-of-characters\" whitespace-text-nodes=\"collapse|drop|leave\" debug-text-nodes=\"true|false\" link=\"url\"/>");

    private static final String FS_SUMMARY_NAME = "summary";
    static final FunctionSignature[] FS_SUMMARY = functionSignatures(
        FS_SUMMARY_NAME,
        "Print a summary of the match in $node. Outputs a predefined amount of text to the left and the right of the match.",
        returns(Type.ELEMENT, "The summary."),
        arities(
            arity(
                FS_PARAM_ROOT,
                FS_PARAM_NODE,
                FS_PARAM_CONFIG
            ),
            arity(
                FS_PARAM_ROOT,
                FS_PARAM_NODE,
                FS_PARAM_CONFIG,
                optParam("callback", Type.FUNCTION, "A callback function which will be called" +
                    " once for every text node before it is appended to the displayed text. The function" +
                    " should accept 2 parameters: 1) a single text node, 2) a string indicating the" +
                    " current direction in which text is appended, i.e. $kwic:MODE_BEFORE or $kwic:MODE_AFTER." +
                    " The function may return the empty sequence if the current node should be ignored" +
                    " (e.g. if it belongs to a \"footnote\" which should not be displayed). Otherwise it should" +
                    " return a single string."
                )
            )
        )
    );

    public KwicSummary(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        // parse parameters and config
        final Node root = args[0].itemAt(0).toJavaObject(Node.class);
        final Element node = args[1].itemAt(0).toJavaObject(Element.class);
        final Config config = Config.fromConfigElement(args[2].itemAt(0).toJavaObject(Element.class));
        @Nullable final FunctionReference callback;
        if (args.length == 4) {
            callback = args[3].itemAt(0).toJavaObject(FunctionReference.class);
        } else {
            callback = null;
        }

        final Node[] prevTrunc = truncatePrevious(root, node, null, config.characterWidth, BigInteger.ZERO, callback);

        return null;
    }

    /**
     * function kwic:truncate-previous($root as node(), $node as node()?, $truncated as item()*, $max as xs:integer, $chars as xs:integer, $callback as (function(text(), xs:string) as text()?)?)
     */
    private static Node[] truncatePrevious(final Node root, @Nullable final Element node, final Node[] truncated, final BigInteger max, final BigInteger chars, @Nullable final FunctionReference callback) {
        if (node == null) {
            return truncated;
        }

        // TODO(AR) get preceding::text()[1] is tricky and will need some work to implement in Java
        // TODO(AR) first maybe study how this works for the preceding axes implemented in Java and learn if that is good or perhaps there is a better way?

        //final Node next = node.getPr

        return null;
    }

    private static class Config {
        final BigInteger characterWidth;
        final boolean table;
        final WhitespaceTextNodeHandling whitespaceTextNodeHandling;
        final boolean debugTextNodes;
        @Nullable final String link;

        private Config(final BigInteger characterWidth, final boolean table, final WhitespaceTextNodeHandling whitespaceTextNodeHandling, final boolean debugTextNodes, @Nullable final String link) {
            this.characterWidth = characterWidth;
            this.table = table;
            this.whitespaceTextNodeHandling = whitespaceTextNodeHandling;
            this.debugTextNodes = debugTextNodes;
            this.link = link;
        }

        public static Config fromConfigElement(final Element element) throws IllegalArgumentException {
            if (!"config".equals(element.getLocalName())) {
                throw new IllegalArgumentException("Expected element to be named \"config\"");
            }

            final BigInteger characterWidth;
            @Nullable final String attrWidth = element.getAttribute("width");
            if (notNullOrEmptyOrWs(attrWidth)) {
                try {
                    characterWidth = new BigInteger(attrWidth);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("The \"width\" attribute must be a valid xs:integer: " + e.getMessage());
                }
            } else {
                characterWidth = BigInteger.valueOf(DEFAULT_CHARACTER_KWIC_LENGTH);
            }

            final boolean table = getBooleanAttribute(element, "table");

            final WhitespaceTextNodeHandling whitespaceTextNodeHandling;
            @Nullable final String attrWhitespaceTextNodes = notNullOrEmptyOrWs(element.getAttribute("whitespace-text-nodes"), null);
            if (attrWhitespaceTextNodes != null) {
                try {
                    whitespaceTextNodeHandling = WhitespaceTextNodeHandling.valueOf(attrWhitespaceTextNodes.toUpperCase());
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException("The \"whitespace-text-nodes\" attribute must have the value: \"leave\", \"collapse\", or \"drop\": " + e.getMessage());
                }
            } else {
                whitespaceTextNodeHandling = WhitespaceTextNodeHandling.LEAVE;
            }

            final boolean debugTextNodes = getBooleanAttribute(element, "debug-text-nodes");

            @Nullable final String link = notNullOrEmptyOrWs(element.getAttribute("link"), null);

            return new Config(characterWidth, table, whitespaceTextNodeHandling, debugTextNodes, link);
        }

        private static boolean getBooleanAttribute(final Element element, final String attrName) {
            final String attrValue = notNullOrEmptyOrWs(element.getAttribute(attrName), "false");
            return "true".equals(attrValue) || "yes".equals(attrValue);
        }
    }

    private enum WhitespaceTextNodeHandling {
        LEAVE,
        COLLAPSE,
        DROP
    }
}
