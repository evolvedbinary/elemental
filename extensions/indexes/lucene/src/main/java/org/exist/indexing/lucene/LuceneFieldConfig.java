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
package org.exist.indexing.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;
import org.exist.dom.QName;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.numbering.NodeId;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.ElementValue;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.*;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for a field definition nested inside a lucene index configuration element.
 * A field must have a name attribute. It may have an expression attribute containing an XQuery
 * expression, which is called to retrieve the content to be indexed. If no expression attribute
 * is present, the field will share content with its parent expression.
 *
 * Optionally an if attribute may contain an XQuery expression to be evaluated. If the effective
 * boolean value of the result is false, the field will not be created.
 *
 * A field may also be associated with an analyzer, could have a type and may be stored or not.
 *
 * @author Wolfgang Meier
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LuceneFieldConfig extends AbstractFieldConfig {

    private static final BigDecimal BD_DOUBLE_MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);
    private static final BigDecimal BD_DOUBLE_MIN_VALUE = BigDecimal.valueOf(Double.MIN_VALUE);
    private static final BigInteger BI_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger BI_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

    private static final String ATTR_FIELD_NAME = "name";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_BINARY = "binary";
    private static final String ATTR_STORE = "store";
    private static final String ATTR_ANALYZER = "analyzer";
    private static final String ATTR_IF = "if";

    protected String fieldName;
    protected int type = Type.STRING;
    protected boolean binary = false;
    protected Field.Store store = Field.Store.YES;
    protected Analyzer analyzer= null;
    protected Optional<String> condition = Optional.empty();
    protected CompiledXQuery compiledCondition = null;

    LuceneFieldConfig(LuceneConfig config, Element configElement, Map<String, String> namespaces, AnalyzerConfig analyzers) throws DatabaseConfigurationException {
        super(config, configElement, namespaces);

        fieldName = configElement.getAttribute(ATTR_FIELD_NAME);
        if (fieldName.isEmpty()) {
            throw new DatabaseConfigurationException("Invalid config: attribute 'name' must be given");
        }

        final String typeStr = configElement.getAttribute(ATTR_TYPE);
        if (!typeStr.isEmpty()) {
            try {
                this.type = Type.getType(typeStr);
            } catch (XPathException e) {
                throw new DatabaseConfigurationException("Invalid type declared for field " + fieldName + ": " + typeStr);
            }
        }

        final String storeStr = configElement.getAttribute(ATTR_STORE);
        if (!storeStr.isEmpty()) {
            this.store = (storeStr.equalsIgnoreCase("yes") || storeStr.equalsIgnoreCase("true")) ? Field.Store.YES : Field.Store.NO;
        }

        final String analyzerOpt = configElement.getAttribute(ATTR_ANALYZER);
        if (!analyzerOpt.isEmpty()) {
            analyzer = analyzers.getAnalyzerById(analyzerOpt);
            if (analyzer == null) {
                throw new DatabaseConfigurationException("Analyzer for field " + fieldName + " not found");
            }
        }

        final String cond = configElement.getAttribute(ATTR_IF);
        if (!cond.isEmpty()) {
            this.condition = Optional.of(cond);
        }

        final String binaryStr = configElement.getAttribute(ATTR_BINARY);
        this.binary = binaryStr.equalsIgnoreCase("true") || binaryStr.equalsIgnoreCase("yes");
    }

    @Nonnull
    public String getName() {
        return fieldName;
    }

    @Nullable
    @Override
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    protected void build(final DBBroker broker, final DocumentImpl document, final NodeId nodeId, final Document luceneDoc, final CharSequence text, final Map<String, String> prefixToNamespaceMappings) {
        try {
            if (checkCondition(broker, document, nodeId)) {
                doBuild(broker, document, nodeId, luceneDoc, text, prefixToNamespaceMappings);
            }
        } catch (XPathException e) {
            LOG.warn("XPath error while evaluating expression for field named '{}': {}: {}", fieldName, expression, e.getMessage(), e);
        } catch (PermissionDeniedException e) {
            LOG.warn("Permission denied while evaluating expression for field named '{}': {}", fieldName, expression, e);
        }
    }

    private boolean checkCondition(DBBroker broker, DocumentImpl document, NodeId nodeId) throws PermissionDeniedException, XPathException {
        if (!condition.isPresent()) {
            return true;
        }

        if (compiledCondition == null && isValid) {
            compiledCondition = compile(broker, condition.get());
        }
        if (!isValid) {
            return false;
        }

        final XQuery xquery = broker.getBrokerPool().getXQueryService();
        final NodeProxy currentNode = new NodeProxy(null, document, nodeId);
        try {
            Sequence result = xquery.execute(broker, compiledCondition, currentNode);
            return result != null && result.effectiveBooleanValue();
        } catch (PermissionDeniedException | XPathException e) {
            isValid = false;
            throw e;
        } finally {
            compiledCondition.reset();
            compiledCondition.getContext().reset();
        }
    }

    @Override
    protected void processResult(final Sequence result, final Map<String, String> prefixToNamespaceMappings, final Document luceneDoc) throws XPathException {
        for (final SequenceIterator i = result.unorderedIterator(); i.hasNext(); ) {
            final Item item = i.nextItem();
            final String content = item.getStringValue();
            final Field field = binary ? convertToDocValue(content, prefixToNamespaceMappings) : convertToField(content, prefixToNamespaceMappings);
            if (field != null) {
                luceneDoc.add(field);
            }
        }
    }

    @Override
    protected void processText(final NodeId nodeId, final CharSequence text, final Map<String, String> prefixToNamespaceMappings, final Document luceneDoc) {
        final Field field;
        if (binary) {
            field = convertToDocValue(text.toString(), prefixToNamespaceMappings);
        } else {
            field = convertToField(text.toString(), prefixToNamespaceMappings);
        }
        if (field != null) {
            luceneDoc.add(field);
        }
    }

    private @Nullable Field convertToField(final String content, final Map<String, String> prefixToNamespaceMappings) {
        try {
            switch (type) {

                case Type.INTEGER:
                case Type.NON_POSITIVE_INTEGER:
                case Type.NEGATIVE_INTEGER:
                case Type.NON_NEGATIVE_INTEGER:
                case Type.POSITIVE_INTEGER:
                    final BigInteger integerValue = new IntegerValue(content, type).toJavaObject(BigInteger.class);
                    // NOTE(AR) we can only store this as a Java `long` type in Lucene, so we have to check if it will fit!
                    if (integerValue.subtract(BI_LONG_MAX_VALUE).compareTo(BigInteger.ZERO) > 0 || integerValue.subtract(BI_LONG_MIN_VALUE).compareTo(BigInteger.ZERO) < 0) {
                        LOG.warn("Field {} has an xs:integer value outside of the range {} to {}, this is unsupported due to limitations with Lucene 4.10.4. Content was: {}", fieldName, Long.MIN_VALUE, Long.MAX_VALUE, content);
                        return null;
                    }
                    return new LongField(fieldName, integerValue.longValue(), store == Field.Store.YES ? LongField.TYPE_STORED : LongField.TYPE_NOT_STORED);

                case Type.LONG:
                case Type.UNSIGNED_LONG:
                    final long longValue = new IntegerValue(content, type).getLong();
                    return new LongField(fieldName, longValue, store == Field.Store.YES ? LongField.TYPE_STORED : LongField.TYPE_NOT_STORED);

                case Type.INT:
                case Type.UNSIGNED_INT:
                case Type.SHORT:
                case Type.UNSIGNED_SHORT:
                case Type.BYTE:
                case Type.UNSIGNED_BYTE:
                    final int intValue = new IntegerValue(content, type).getInt();
                    return new IntField(fieldName, intValue, store == Field.Store.YES ? IntField.TYPE_STORED : IntField.TYPE_NOT_STORED);

                case Type.DECIMAL:
                    final BigDecimal bigDecimal = new DecimalValue(content).toJavaObject(BigDecimal.class);
                    // NOTE(AR) we can only store this as a Java `double` type in Lucene, so we have to check if it will fit!
                    if (bigDecimal.subtract(BD_DOUBLE_MAX_VALUE).compareTo(BigDecimal.ZERO) > 0 || bigDecimal.subtract(BD_DOUBLE_MIN_VALUE).compareTo(BigDecimal.ZERO) < 0) {
                        LOG.warn("Field {} has an xs:decimal value outside of the range {} to {}, this is unsupported due to limitations with Lucene 4.10.4. Content was: {}", fieldName, Double.MIN_VALUE, Double.MAX_VALUE, content);
                        return null;
                    }
                    return new DoubleField(fieldName, bigDecimal.doubleValue(), store == Field.Store.YES ? DoubleField.TYPE_STORED : DoubleField.TYPE_NOT_STORED);

                case Type.DOUBLE:
                    final double doubleValue = new DoubleValue(content).getDouble();
                    return new DoubleField(fieldName, doubleValue, store == Field.Store.YES ? DoubleField.TYPE_STORED : DoubleField.TYPE_NOT_STORED);

                case Type.FLOAT:
                    final float floatValue = new FloatValue(content).getFloat();
                    return new FloatField(fieldName, floatValue, store == Field.Store.YES ? FloatField.TYPE_STORED : FloatField.TYPE_NOT_STORED);

                case Type.DATE:
                    final DateValue dateValue = new DateValue(content);
                    final long longDateValue = dateValue.toJavaObject(long.class);
                    return new LongField(fieldName, longDateValue, store == Field.Store.YES ? LongField.TYPE_STORED : LongField.TYPE_NOT_STORED);

                case Type.TIME:
                    final TimeValue timeValue = new TimeValue(content);
                    final long longTimeValue = timeValue.toJavaObject(long.class);
                    return new LongField(fieldName, longTimeValue, store == Field.Store.YES ? LongField.TYPE_STORED : LongField.TYPE_NOT_STORED);

                case Type.DATE_TIME:
                    final DateTimeValue dateTimeValue = new DateTimeValue(content);
                    return new TextField(fieldName, dateTimeValue.toString(), store);

                case Type.DATE_TIME_STAMP:
                    final DateTimeStampValue dateTimeStampValue = new DateTimeStampValue(content);
                    return new TextField(fieldName, dateTimeStampValue.toString(), store);

                case Type.DURATION:
                    final DurationValue durationValue = new DurationValue(content);
                    return new TextField(fieldName, durationValue.toString(), store);

                case Type.YEAR_MONTH_DURATION:
                    final YearMonthDurationValue yearMonthDurationValue = new YearMonthDurationValue(content);
                    return new TextField(fieldName, yearMonthDurationValue.toString(), store);

                case Type.DAY_TIME_DURATION:
                    final DayTimeDurationValue dayTimeDurationValue = new DayTimeDurationValue(content);
                    return new TextField(fieldName, dayTimeDurationValue.toString(), store);

                case Type.G_YEAR_MONTH:
                    final GYearMonthValue gYearMonthValue = new GYearMonthValue(content);
                    return new TextField(fieldName, gYearMonthValue.toString(), store);

                case Type.G_YEAR:
                    final GYearValue gYearValue = new GYearValue(content);
                    return new TextField(fieldName, gYearValue.toString(), store);

                case Type.G_MONTH_DAY:
                    final GMonthDayValue gMonthDayValue = new GMonthDayValue(content);
                    return new TextField(fieldName, gMonthDayValue.toString(), store);

                case Type.G_MONTH:
                    final GMonthValue gMonthValue = new GMonthValue(content);
                    return new TextField(fieldName, gMonthValue.toString(), store);

                case Type.G_DAY:
                    final GDayValue gDayValue = new GDayValue(content);
                    return new TextField(fieldName, gDayValue.toString(), store);

                case Type.BOOLEAN:
                    final BooleanValue booleanValue = BooleanValue.valueOf(null, content);
                    return new IntField(fieldName, booleanValue.getValue() ? 1 : 0, store);

                case Type.BASE64_BINARY:
                    final BinaryValue base64Binary = new BinaryValueFromBinaryString(new Base64BinaryValueType(), content);
                    return new TextField(fieldName, base64Binary.getStringValue(), store);

                case Type.HEX_BINARY:
                    final BinaryValue hexBinary = new BinaryValueFromBinaryString(new HexBinaryValueType(), content);
                    return new TextField(fieldName, hexBinary.getStringValue(), store);

                case Type.ANY_URI:
                    final AnyURIValue anyURIValue = new AnyURIValue(content);
                    return new TextField(fieldName, anyURIValue.getStringValue(), store);

                case Type.QNAME:
                    final QNameValue qnameValue = getQNameValue(content, prefixToNamespaceMappings);
                    return new TextField(fieldName, qnameValue.getQName().getExtendedStringValue(), store);

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
                    final StringValue stringValue = new StringValue(content, type);
                    return new TextField(fieldName, stringValue.getStringValue(), store);

                case Type.NOTATION:
                default:
                    // NOTE(AR) report inability to index value
                    LOG.warn("Cannot convert field {} to type {}. Content was: {}", fieldName, Type.getTypeName(type), content);
            }
        } catch (final NumberFormatException | XPathException | QName.IllegalQNameException e) {
            // NOTE(AR) report inability to index value
            LOG.warn("Cannot convert field {} to type {}. Content was: {}. Error was: {}", fieldName, Type.getTypeName(type), content, e.getMessage());
        }
        return null;
    }

    private @Nullable Field convertToDocValue(final String content, final Map<String, String> prefixToNamespaceMappings) {
        try {
            final BytesRef bytesRef;
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
                    final IntegerValue iv = new IntegerValue(content, type);
                    bytesRef = new BytesRef(iv.toJavaObject(byte[].class));
                    break;

                case Type.DECIMAL:
                    final DecimalValue dv = new DecimalValue(content);
                    bytesRef = new BytesRef(dv.toJavaObject(byte[].class));
                    break;

                case Type.DOUBLE:
                    final DoubleValue dbv = new DoubleValue(content);
                    bytesRef = new BytesRef(dbv.toJavaObject(byte[].class));
                    break;

                case Type.FLOAT:
                    final FloatValue fv = new FloatValue(content);
                    bytesRef = new BytesRef(fv.toJavaObject(byte[].class));
                    break;

                case Type.DATE:
                    final DateValue dateValue = new DateValue(content);
                    bytesRef = new BytesRef(dateValue.toJavaObject(byte[].class));
                    break;

                case Type.TIME:
                    final TimeValue timeValue = new TimeValue(content);
                    bytesRef = new BytesRef(timeValue.toJavaObject(byte[].class));
                    break;

                case Type.DATE_TIME:
                    final DateTimeValue dateTimeValue = new DateTimeValue(content);
                    bytesRef = new BytesRef(dateTimeValue.toJavaObject(byte[].class));
                    break;

                case Type.DATE_TIME_STAMP:
                    final DateTimeStampValue dateTimeStampValue = new DateTimeStampValue(content);
                    bytesRef = new BytesRef(dateTimeStampValue.toJavaObject(byte[].class));
                    break;

                case Type.DURATION:
                    final DurationValue durationValue = new DurationValue(content);
                    bytesRef = new BytesRef(durationValue.toJavaObject(byte[].class));
                    break;

                case Type.YEAR_MONTH_DURATION:
                    final YearMonthDurationValue yearMonthDurationValue = new YearMonthDurationValue(content);
                    bytesRef = new BytesRef(yearMonthDurationValue.toJavaObject(byte[].class));
                    break;

                case Type.DAY_TIME_DURATION:
                    final DayTimeDurationValue dayTimeDurationValue = new DayTimeDurationValue(content);
                    bytesRef = new BytesRef(dayTimeDurationValue.toJavaObject(byte[].class));
                    break;

                case Type.G_YEAR_MONTH:
                    final GYearMonthValue gYearMonthValue = new GYearMonthValue(content);
                    bytesRef = new BytesRef(gYearMonthValue.toJavaObject(byte[].class));
                    break;

                case Type.G_YEAR:
                    final GYearValue gYearValue = new GYearValue(content);
                    bytesRef = new BytesRef(gYearValue.toJavaObject(byte[].class));
                    break;

                case Type.G_MONTH_DAY:
                    final GMonthDayValue gMonthDayValue = new GMonthDayValue(content);
                    bytesRef = new BytesRef(gMonthDayValue.toJavaObject(byte[].class));
                    break;

                case Type.G_MONTH:
                    final GMonthValue gMonthValue = new GMonthValue(content);
                    bytesRef = new BytesRef(gMonthValue.toJavaObject(byte[].class));
                    break;

                case Type.G_DAY:
                    final GDayValue gDayValue = new GDayValue(content);
                    bytesRef = new BytesRef(gDayValue.toJavaObject(byte[].class));
                    break;

                case Type.BOOLEAN:
                    final BooleanValue booleanValue = BooleanValue.valueOf(null, content);
                    bytesRef = new BytesRef(booleanValue.toJavaObject(byte[].class));
                    break;

                case Type.BASE64_BINARY:
                    final BinaryValue base64BinaryValue = new BinaryValueFromBinaryString(new Base64BinaryValueType(), content);
                    bytesRef = new BytesRef(base64BinaryValue.serialize());
                    break;

                case Type.HEX_BINARY:
                    final BinaryValue hexBinaryValue = new BinaryValueFromBinaryString(new HexBinaryValueType(), content);
                    bytesRef = new BytesRef(hexBinaryValue.serialize());
                    break;

                case Type.ANY_URI:
                    final AnyURIValue anyURIValue = new AnyURIValue(content);
                    bytesRef = new BytesRef(anyURIValue.toJavaObject(byte[].class));
                    break;

                case Type.QNAME:
                    final QNameValue qnameValue = getQNameValue(content, prefixToNamespaceMappings);
                    bytesRef = new BytesRef(qnameValue.toJavaObject(byte[].class));
                    break;

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
                    final StringValue stringValue = new StringValue(content, type);
                    bytesRef = new BytesRef(stringValue.toJavaObject(byte[].class));
                    break;

                case Type.NOTATION:
                default:
                    // NOTE(AR) report inability to index value
                    LOG.warn("Cannot convert field {} to type {}. Content was: {}", fieldName, Type.getTypeName(type), content);
                    return null;
            }

            return new BinaryDocValuesField(fieldName, bytesRef);

        } catch (final NumberFormatException | XPathException | QName.IllegalQNameException | IOException e) {
            // NOTE(AR) report inability to index value
            LOG.warn("Cannot convert field {} to type {}. Content was: {}. Error was: {}", fieldName, Type.getTypeName(type), content, e.getMessage());
            return null;
        }
    }

    private static QNameValue getQNameValue(final String content, final Map<String, String> prefixToNamespaceMappings) throws XPathException, QName.IllegalQNameException {
        final QName qname;
        final String qnameLocalName = QName.extractLocalName(content);
        @Nullable final String qnamePrefix = QName.extractPrefix(content);
        if (qnamePrefix != null) {
            @Nullable final String qnameNamespace = prefixToNamespaceMappings.get(qnamePrefix);
            if (qnameNamespace == null) {
                throw new XPathException("Lucene index module: Missing namespace declaration for qname value in field config");
            }
            qname = new QName(qnameLocalName, qnameNamespace, qnamePrefix, ElementValue.ATTRIBUTE);
        } else {
            qname = new QName(qnameLocalName, XMLConstants.NULL_NS_URI, null, ElementValue.ATTRIBUTE);
        }

        return new QNameValue(null, qname);
    }
}
