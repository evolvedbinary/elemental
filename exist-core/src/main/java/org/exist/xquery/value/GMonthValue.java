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
package org.exist.xquery.value;

import com.ibm.icu.text.Collator;
import org.exist.util.ByteConversion;
import org.exist.xquery.Constants;
import org.exist.xquery.Constants.Comparison;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.GregorianCalendar;

/**
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class GMonthValue extends AbstractDateTimeValue {

    public static final int SERIALIZED_SIZE = 3;

    protected boolean addTrailingZ = false;

    public GMonthValue() throws XPathException {
        super(null, stripCalendar(TimeUtils.getInstance().newXMLGregorianCalendar(new GregorianCalendar())));
    }

    public GMonthValue(final Expression expression) throws XPathException {
        super(expression, stripCalendar(TimeUtils.getInstance().newXMLGregorianCalendar(new GregorianCalendar())));
    }

    public GMonthValue(final XMLGregorianCalendar calendar) throws XPathException {
        this(null, calendar);
    }

    public GMonthValue(final Expression expression, XMLGregorianCalendar calendar) throws XPathException {
        super(expression, stripCalendar((XMLGregorianCalendar) calendar.clone()));
    }

    public GMonthValue(final String timeValue) throws XPathException {
        this(null, timeValue);
    }

    public GMonthValue(final Expression expression, String timeValue) throws XPathException {
        super(expression, fixTimezone(timeValue));
        timeValue = timeValue.trim();
        if (timeValue.endsWith("Z")) {
            addTrailingZ = true;
        }
        if (timeValue.endsWith("-00:00")) {
            addTrailingZ = true;
        }
        if (timeValue.endsWith("+00:00")) {
            addTrailingZ = true;
        }
        if (addTrailingZ) {
            this.calendar.setTimezone(0);
        }
        try {
            if (calendar.getXMLSchemaType() != DatatypeConstants.GMONTH) {
                throw new IllegalStateException();
            }
        } catch (final IllegalStateException e) {
            throw new XPathException(getExpression(), "xs:gMonth instance must not have year, month or day fields set");
        }
    }

    private static XMLGregorianCalendar stripCalendar(XMLGregorianCalendar calendar) {
        calendar = (XMLGregorianCalendar) calendar.clone();
        calendar.setYear(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setDay(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setHour(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setMinute(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setSecond(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        return calendar;
    }

    private static String fixTimezone(String value) {
        //TODO : should we imply a default "Z" here ?
        //TODO : should we raise an error on wrong TZ offsets (e.g. 60) ?
        int p = value.indexOf('Z');
        if (p != Constants.STRING_NOT_FOUND) {
            return value.substring(0, p);
        }
        p = value.indexOf("-00:00");
        if (p != Constants.STRING_NOT_FOUND) {
            return value.substring(0, p);
        }
        p = value.indexOf("+00:00");
        if (p != Constants.STRING_NOT_FOUND) {
            return value.substring(0, p);
        }
        return value;
    }

    public AtomicValue convertTo(int requiredType) throws XPathException {
        switch (requiredType) {
            case Type.GMONTH:
            case Type.ATOMIC:
            case Type.ITEM:
                return this;
            case Type.STRING:
                return new StringValue(getExpression(), getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getExpression(), getStringValue());
            default:
                throw new XPathException(getExpression(), ErrorCodes.FORG0001,
                        "Type error: cannot cast xs:gMonth to "
                                + Type.getTypeName(requiredType));
        }
    }

    protected AbstractDateTimeValue createSameKind(XMLGregorianCalendar cal)
            throws XPathException {
        return new GMonthValue(getExpression(), cal);
    }

    public int getType() {
        return Type.GMONTH;
    }
    
    /*
       public String getStringValue() throws XPathException {
    	String r = super.getStringValue();
    	if (addTrailingZ) 
    		return r + "Z";
    	return r;
    }
    */

    protected QName getXMLSchemaType() {
        return DatatypeConstants.GMONTH;
    }

    public ComputableValue minus(ComputableValue other) throws XPathException {
        throw new XPathException(getExpression(), "Subtraction is not supported on values of type " +
                Type.getTypeName(getType()));
    }

    @Override
    public int compareTo(Collator collator, AtomicValue other) throws XPathException {
        if (other.getType() == getType()) {
            if (!getTimezone().isEmpty()) {
                if (!((AbstractDateTimeValue) other).getTimezone().isEmpty()) {
                    if (!((DayTimeDurationValue) getTimezone().itemAt(0)).compareTo(null, Comparison.EQ, (DayTimeDurationValue) ((AbstractDateTimeValue) other).getTimezone().itemAt(0))) {
                        return DatatypeConstants.LESSER;
                    }
                } else {
                    if (!"PT0S".equals(((DayTimeDurationValue) getTimezone().itemAt(0)).getStringValue())) {
                        return DatatypeConstants.LESSER;
                    }
                }
            } else {
                if (!((AbstractDateTimeValue) other).getTimezone().isEmpty()) {
                    if (!"PT0S".equals(((DayTimeDurationValue) ((AbstractDateTimeValue) other).getTimezone().itemAt(0)).getStringValue())) {
                        return DatatypeConstants.LESSER;
                    }
                }
            }
            // filling in missing timezones with local timezone, should be total order as per XPath 2.0 10.4
            final int r = this.getImplicitCalendar().compare(((AbstractDateTimeValue) other).getImplicitCalendar());
            //getImplicitCalendar().compare(((AbstractDateTimeValue) other).getImplicitCalendar());
            if (r == DatatypeConstants.INDETERMINATE) {
                throw new RuntimeException("indeterminate order between " + this + " and " + other);
            }
            return r;
        }
        throw new XPathException(getExpression(), 
                "Type error: cannot compare " + Type.getTypeName(getType()) + " to "
                        + Type.getTypeName(other.getType()));
    }

    @Override
    public <T> T toJavaObject(final Class<T> target) throws XPathException {
        Throwable throwable = null;
        try {
            if (target == byte[].class) {
                return (T) serialize();
            } else if (target == ByteBuffer.class) {
                final ByteBuffer buf = ByteBuffer.allocate(SERIALIZED_SIZE);
                serialize(buf);
                return (T) buf;
            } else {
                return super.toJavaObject(target);
            }
        } catch (final IOException e) {
            throwable = e;
        }
        throw new XPathException(getExpression(), ErrorCodes.XPTY0004, "cannot convert value of type " + Type.getTypeName(getType()) + " to Java object of type " + target.getName(), throwable);
    }

    /**
     * Serializes to a ByteBuffer.
     *
     * 3 bytes where: [0 (Month), 1-2 (Timezone)]
     *
     * @return the serialized data.
     */
    public byte[] serialize() throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(SERIALIZED_SIZE);
        serialize(buf);
        return buf.array();
    }

    /**
     * Serializes to a ByteBuffer.
     *
     * 3 bytes where: [0 (Month), 1-2 (Timezone)]
     *
     * @param buf the ByteBuffer to serialize to.
     */
    public void serialize(final ByteBuffer buf) {
        buf.put((byte) calendar.getMonth());
        // values for timezone range from -14*60 to 14*60, so we can use a short, but
        // need to choose a different value for FIELD_UNDEFINED, which is not the same as 0 (= UTC)
        final int timezone = calendar.getTimezone();
        ByteConversion.shortToByteH((short) (timezone == DatatypeConstants.FIELD_UNDEFINED ? Short.MAX_VALUE : timezone), buf);
    }

    /**
     * Deserializes from a ByteBuffer.
     *
     * @param expression the expression that creates the GMonthValue object.
     * @param buf the ByteBuffer to deserialize from.
     *
     * @return the GMonthValue.
     */
    public static AtomicValue deserialize(@Nullable final Expression expression, final ByteBuffer buf) throws XPathException {
        final int month = buf.get();
        int timezone = ByteConversion.byteToShortH(buf);
        if (timezone == Short.MAX_VALUE) {
            timezone = DatatypeConstants.FIELD_UNDEFINED;
        }

        final XMLGregorianCalendar xmlGregorianCalendar = TimeUtils.getInstance().newXMLGregorianCalendar(DatatypeConstants.FIELD_UNDEFINED, month, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, timezone);
        return new GMonthValue(expression, xmlGregorianCalendar);
    }
}
