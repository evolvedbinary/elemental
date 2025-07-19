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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.dom.memtree.InMemoryNodeSet;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.persistent.Match;
import org.exist.dom.persistent.NodeProxy;
import org.exist.indexing.lucene.*;
import org.exist.storage.NodePath;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.lucene.LuceneModule.functionSignature;
import static org.exist.xquery.modules.lucene.LuceneModule.functionSignatures;

/**
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class Field extends BasicFunction {

    private static final FunctionParameterSequenceType FS_PARAM_NODE = param("node", Type.NODE, "the context node to check for attached fields");
    private static final FunctionParameterSequenceType FS_PARAM_FIELD = param("field", Type.STRING, "name of the field");
    private static final FunctionParameterSequenceType TYPE_PARAMETER = param("type", Type.STRING, "intended target type to cast the field value to. Casting may fail with a dynamic error.");

    private static final String FS_FIELD_NAME = "field";
    static final FunctionSignature[] FS_FIELD = functionSignatures(
            FS_FIELD_NAME,
            "Returns the value of a field attached to a particular node obtained via a full text search." +
            "The $type parameter allows you to name the target type into which the field " +
            "value should be cast. This is mainly relevant for fields having a different type than xs:string. " +
            "As lucene does not record type information, numbers or dates would be returned as strings by default.",
            returnsOptMany(Type.ITEM, "Sequence corresponding to the values of the field attached, cast to the desired target type"),
            arities(
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_FIELD
                    ),
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_FIELD,
                            TYPE_PARAMETER
                    )
            )
    );

    private static final String FS_BINARY_FIELD_NAME = "binary-field";
    static final FunctionSignature[] FS_BINARY_FIELD = functionSignatures(
            FS_BINARY_FIELD_NAME,
            "Returns the value of a binary field attached to a particular node obtained via a full text search." +
            "Accepts an additional parameter to name the target type into which the field " +
            "value should be cast. This is mainly relevant for fields having a different type than xs:string. " +
            "As lucene does not record type information, numbers or dates would be returned as strings by default.",
            returnsOptMany(Type.ITEM, "Sequence corresponding to the values of the field attached, cast to the desired target type"),
            arities(
                arity(
                    FS_PARAM_NODE,
                    FS_PARAM_FIELD
                ),
                arity(
                    FS_PARAM_NODE,
                    FS_PARAM_FIELD,
                    TYPE_PARAMETER
                )
            )
    );

    private static final String FS_HIGHLIGHT_FIELD_MATCHES_NAME = "highlight-field-matches";
    static final FunctionSignature FS_HIGHLIGHT_FIELD_MATCHES = functionSignature(
            FS_HIGHLIGHT_FIELD_MATCHES_NAME,
            "Highlights matches for the last executed lucene query within the value of a field " +
            "attached to a particular node obtained via a full text search. Only fields listed in the 'fields' option of ft:query will be " +
            "available to highlighting.",
            returnsOpt(Type.ELEMENT, "An exist:field containing the content of the requested field with all query matches enclosed in an exist:match"),
            FS_PARAM_NODE,
            FS_PARAM_FIELD
    );

    public Field(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final NodeValue nodeValue = (NodeValue) args[0].itemAt(0);
        if (nodeValue.getImplementationType() != NodeValue.PERSISTENT_NODE) {
            return Sequence.EMPTY_SEQUENCE;
        }

        final String fieldName = args[1].itemAt(0).getStringValue();

        int type = Type.STRING;
        if (getArgumentCount() == 3) {
            final String typeStr = args[2].itemAt(0).getStringValue();
            type = Type.getType(typeStr);
        }

        final NodeProxy proxy = (NodeProxy) nodeValue;
        final LuceneMatch match = getMatch(proxy);
        if (match == null) {
            return Sequence.EMPTY_SEQUENCE;
        }
        final String called = getSignature().getName().getLocalPart();

        final LuceneIndexWorker index = (LuceneIndexWorker) context.getBroker().getIndexController().getWorkerByIndexId(LuceneIndex.ID);
        try {
            Sequence result;
            switch (called) {
                case FS_FIELD_NAME:
                     result = getFieldValues(fieldName, type, match, index);
                     break;

                case FS_HIGHLIGHT_FIELD_MATCHES_NAME:
                    result = getFieldValues(fieldName, type, match, index);
                    result = highlightMatches(fieldName, proxy, match, result);
                    break;

                case FS_BINARY_FIELD_NAME:
                    result = getBinaryFieldValue(fieldName, type, match, index);
                    break;

                default:
                    throw new XPathException(this, ErrorCodes.FOER0000, "Unknown function: " + getName());
            }

            return result;

        } catch (final IOException e) {
            throw new XPathException(this, LuceneModule.EXXQDYFT0002, "Error retrieving field: " + e.getMessage(), e);
        }
    }

    private Sequence getBinaryFieldValue(final String fieldName, final int type, final LuceneMatch match, final LuceneIndexWorker index) throws IOException, XPathException {
        final BytesRef fieldValue = index.getBinaryField(match.getLuceneDocId(), fieldName);
        if (fieldValue == null) {
            return Sequence.EMPTY_SEQUENCE;
        }

        return bytesToAtomic(fieldValue, type);
    }

    private Sequence getFieldValues(final String fieldName, final int type, final LuceneMatch match, final LuceneIndexWorker index) throws IOException, XPathException {
        @Nullable final IndexableField[] fields = index.getField(match.getLuceneDocId(), fieldName);
        if (fields == null) {
            return Sequence.EMPTY_SEQUENCE;
        }

        final Sequence result = new ValueSequence(fields.length);
        for (final IndexableField field : fields) {
            if (field.numericValue() != null) {
                result.add(numberToAtomic(type, field.numericValue()));
            } else {
                result.add(stringToAtomic(type, field.stringValue()));
            }
        }
        return result;
    }

    /**
     * Highlight matches in field content using the analyzer defined for the field.
     *
     * @param fieldName the name of the field
     * @param proxy node on which the field is defined
     * @param match the lucene match attached to the node
     * @param text the content of the field
     * @return a sequence of exist:field elements containing the field content with matches enclosed in exist:match
     * @throws XPathException in case of error
     * @throws IOException in case of a lucene error
     */
    private Sequence highlightMatches(final String fieldName, final NodeProxy proxy, final LuceneMatch match, final Sequence text) throws XPathException, IOException {
        final LuceneIndexWorker index = (LuceneIndexWorker) context.getBroker().getIndexController().getWorkerByIndexId(LuceneIndex.ID);
        final Map<Object, Query> terms = index.getTerms(match.getQuery());
        final NodePath path = LuceneMatchListener.getPath(proxy);
        final LuceneConfig config = index.getLuceneConfig(context.getBroker(), proxy.getDocumentSet());
        LuceneIndexConfig idxConf = config.getConfig(path).next();
        if (idxConf == null) {
            // no lucene index: no fields to highlight
            return Sequence.EMPTY_SEQUENCE;
        }

        final Analyzer analyzer = idxConf.getAnalyzer();

        context.pushDocumentContext();
        try {
            final MemTreeBuilder builder = context.getDocumentBuilder();
            builder.startDocument();

            final InMemoryNodeSet result =  new InMemoryNodeSet(text.getItemCount());
            for (final SequenceIterator si = text.iterate(); si.hasNext(); ) {
                final int nodeNr = builder.startElement(Namespaces.EXIST_NS, "field", "exist:field", null);
                final String content = si.nextItem().getStringValue();
                int currentPos = 0;
                try (final Reader reader = new StringReader(content);
                     final TokenStream tokenStream = analyzer.tokenStream(fieldName, reader)) {
                    tokenStream.reset();
                    final MarkableTokenFilter stream = new MarkableTokenFilter(tokenStream);
                    while (stream.incrementToken()) {
                        String token = stream.getAttribute(CharTermAttribute.class).toString();
                        final Query query = terms.get(token);
                        if (query != null) {
                            if (match.getQuery() instanceof PhraseQuery) {
                                final Term phraseTerms[] = ((PhraseQuery) match.getQuery()).getTerms();
                                if (token.equals(phraseTerms[0].text())) {
                                    // Scan the following text and collect tokens to see
                                    // if they are part of the phrase.
                                    stream.mark();
                                    int t = 1;
                                    OffsetAttribute offset = stream.getAttribute(OffsetAttribute.class);
                                    final int startOffset = offset.startOffset();
                                    int endOffset = offset.endOffset();
                                    while (stream.incrementToken() && t < phraseTerms.length) {
                                        token = stream.getAttribute(CharTermAttribute.class).toString();
                                        if (token.equals(phraseTerms[t].text())) {
                                            offset = stream.getAttribute(OffsetAttribute.class);
                                            endOffset = offset.endOffset();
                                            t++;
                                            if (t == phraseTerms.length) {
                                                break;
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                    if (t == phraseTerms.length) {
                                        if (currentPos < startOffset) {
                                            builder.characters(content.substring(currentPos, startOffset));
                                        }
                                        builder.startElement(Namespaces.EXIST_NS, "match", "exist:match", null);
                                        builder.characters(content.substring(startOffset, endOffset));
                                        builder.endElement();
                                        currentPos = endOffset;
                                    }
                                } // End of phrase handling
                            } else {
                                final OffsetAttribute offset = stream.getAttribute(OffsetAttribute.class);
                                if (currentPos < offset.startOffset()) {
                                    builder.characters(content.substring(currentPos, offset.startOffset()));
                                }
                                builder.startElement(Namespaces.EXIST_NS, "match", "exist:match", null);
                                builder.characters(content.substring(offset.startOffset(), offset.endOffset()));
                                builder.endElement();
                                currentPos = offset.endOffset();
                            }
                        }
                    }
                }
                if (currentPos < content.length() - 1)  {
                    builder.characters(content.substring(currentPos));
                }
                builder.endElement();
                result.add(builder.getDocument().getNode(nodeNr));
            }
            return result;
        } finally {
            context.popDocumentContext();
        }
    }

    /**
     * Get the lucene match object attached to the given node
     *
     * @param proxy node to check for matches
     * @return the LuceneMatch object attached to the node or null
     */
    static @Nullable LuceneMatch getMatch(NodeProxy proxy) {
        Match match = proxy.getMatches();
        while (match != null) {
            if (match.getIndexId().equals(LuceneIndex.ID)) {
                return (LuceneMatch) match;
            }
            match = match.getNextMatch();
        }
        return null;
    }

    AtomicValue bytesToAtomic(final BytesRef field, final int type) throws XPathException, IOException {
        try {
            switch (type) {

                case Type.INTEGER:
                case Type.NON_POSITIVE_INTEGER:
                case Type.NEGATIVE_INTEGER:
                case Type.LONG:
                case Type.INT:
                case Type.SHORT:
                case Type.BYTE:
                case Type.NON_NEGATIVE_INTEGER:
                case Type.UNSIGNED_LONG:
                case Type.UNSIGNED_INT:
                case Type.UNSIGNED_SHORT:
                case Type.UNSIGNED_BYTE:
                case Type.POSITIVE_INTEGER:
                    return IntegerValue.deserialize(this, ByteBuffer.wrap(field.bytes), type);

                case Type.DECIMAL:
                    return DecimalValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DOUBLE:
                    return DoubleValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.FLOAT:
                    return FloatValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DATE:
                    return DateValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.TIME:
                    return TimeValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DATE_TIME:
                    return DateTimeValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DATE_TIME_STAMP:
                    return DateTimeStampValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DURATION:
                    return DurationValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.YEAR_MONTH_DURATION:
                    return YearMonthDurationValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.DAY_TIME_DURATION:
                    return DayTimeDurationValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.GYEARMONTH:
                    return GYearMonthValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.GYEAR:
                    return GYearValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.GMONTHDAY:
                    return GMonthDayValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.GMONTH:
                    return GMonthValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.GDAY:
                    return GDayValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.BOOLEAN:
                    return BooleanValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.BASE64_BINARY:
                    return BinaryValue.deserialize(this, new Base64BinaryValueType(), ByteBuffer.wrap(field.bytes));

                case Type.HEX_BINARY:
                    return BinaryValue.deserialize(this, new HexBinaryValueType(), ByteBuffer.wrap(field.bytes));

                case Type.ANY_URI:
                    return AnyURIValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.QNAME:
                    return QNameValue.deserialize(this, ByteBuffer.wrap(field.bytes));

                case Type.STRING:
                case Type.NORMALIZED_STRING:
                case Type.TOKEN:
                case Type.LANGUAGE:
                case Type.NMTOKEN:
                case Type.NAME:
                case Type.NCNAME:
                case Type.ID:
                case Type.IDREF:
                case Type.ENTITY:
                    return StringValue.deserialize(this, ByteBuffer.wrap(field.bytes), type);

                case Type.NOTATION:
                default:
                    throw new XPathException(this, LuceneModule.EXXQDYFT0005, "Cannot convert binary field to " + Type.getTypeName(type));
            }
        } catch (final NumberFormatException e) {
            throw new XPathException(this, e.getMessage(), e);
        }
    }

    private AtomicValue stringToAtomic(final int type, final String value) throws XPathException {
        switch (type) {
            case Type.INTEGER:
            case Type.NON_POSITIVE_INTEGER:
            case Type.NEGATIVE_INTEGER:
            case Type.LONG:
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
            case Type.NON_NEGATIVE_INTEGER:
            case Type.UNSIGNED_LONG:
            case Type.UNSIGNED_INT:
            case Type.UNSIGNED_SHORT:
            case Type.UNSIGNED_BYTE:
            case Type.POSITIVE_INTEGER:
                return new IntegerValue(this, value, type);

            case Type.DECIMAL:
                return new DecimalValue(this, value);

            case Type.DOUBLE:
                return new DoubleValue(this, value);

            case Type.FLOAT:
                return new FloatValue(this, value);

            case Type.DATE:
                return new DateValue(this, value);

            case Type.TIME:
                return new TimeValue(this, value);

            case Type.DATE_TIME:
                return new DateTimeValue(this, value);

            case Type.DATE_TIME_STAMP:
                return new DateTimeStampValue(this, value);

            case Type.DURATION:
                return new DurationValue(this, value);

            case Type.YEAR_MONTH_DURATION:
                return new YearMonthDurationValue(this, value);

            case Type.DAY_TIME_DURATION:
                return new DayTimeDurationValue(this, value);

            case Type.GYEARMONTH:
                return new GYearMonthValue(this, value);

            case Type.GYEAR:
                return new GYearValue(this, value);

            case Type.GMONTHDAY:
                return new GMonthDayValue(this, value);

            case Type.GMONTH:
                return new GMonthValue(this, value);

            case Type.GDAY:
                return new GDayValue(this, value);

            case Type.BOOLEAN:
                return BooleanValue.valueOf(this, value);

            case Type.BASE64_BINARY:
                return new BinaryValueFromBinaryString(this, new Base64BinaryValueType(), value);

            case Type.HEX_BINARY:
                return new BinaryValueFromBinaryString(this, new HexBinaryValueType(), value);

            case Type.ANY_URI:
                return new AnyURIValue(value);

            case Type.QNAME:
                try {
                    return new QNameValue(null, QName.parse(value));
                } catch (final QName.IllegalQNameException e) {
                    throw new XPathException(this, LuceneModule.EXXQDYFT0006, "Cannot convert string field to " + Type.getTypeName(type), e);
                }

            case Type.STRING:
            case Type.NORMALIZED_STRING:
            case Type.TOKEN:
            case Type.LANGUAGE:
            case Type.NMTOKEN:
            case Type.NAME:
            case Type.NCNAME:
            case Type.ID:
            case Type.IDREF:
            case Type.ENTITY:
                return new StringValue(this, value, type);

            case Type.NOTATION:
            default:
                throw new XPathException(this, LuceneModule.EXXQDYFT0006, "Cannot convert string field to " + Type.getTypeName(type));
        }
    }

    private AtomicValue numberToAtomic(final int type, final Number value) throws XPathException {
        switch (type) {
            case Type.INTEGER:
            case Type.NON_POSITIVE_INTEGER:
            case Type.NEGATIVE_INTEGER:
            case Type.LONG:
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
            case Type.NON_NEGATIVE_INTEGER:
            case Type.UNSIGNED_LONG:
            case Type.UNSIGNED_INT:
            case Type.UNSIGNED_SHORT:
            case Type.UNSIGNED_BYTE:
            case Type.POSITIVE_INTEGER:
                return new IntegerValue(this, value.longValue(), type);

            case Type.DECIMAL:
                return new DecimalValue(this, value.doubleValue());

            case Type.DOUBLE:
                return new DoubleValue(this, value.doubleValue());

            case Type.FLOAT:
                return new FloatValue(this, value.floatValue());

            case Type.DATE:
                return DateValue.deserialize(this, value.longValue());

            case Type.TIME:
                return TimeValue.deserialize(this, value.longValue());

            case Type.BOOLEAN:
                return value.intValue() == 1 ? BooleanValue.TRUE : BooleanValue.FALSE;

            case Type.DATE_TIME:
            case Type.DATE_TIME_STAMP:
            case Type.DURATION:
            case Type.YEAR_MONTH_DURATION:
            case Type.DAY_TIME_DURATION:
            case Type.GYEARMONTH:
            case Type.GYEAR:
            case Type.GMONTHDAY:
            case Type.GMONTH:
            case Type.GDAY:
            case Type.BASE64_BINARY:
            case Type.HEX_BINARY:
            case Type.ANY_URI:
            case Type.QNAME:
            case Type.NOTATION:
            case Type.STRING:
            case Type.NORMALIZED_STRING:
            case Type.TOKEN:
            case Type.LANGUAGE:
            case Type.NMTOKEN:
            case Type.NAME:
            case Type.NCNAME:
            case Type.ID:
            case Type.IDREF:
            case Type.ENTITY:
            default:
                throw new XPathException(this, LuceneModule.EXXQDYFT0007, "Cannot convert numeric field to " + Type.getTypeName(type));
        }
    }
}
