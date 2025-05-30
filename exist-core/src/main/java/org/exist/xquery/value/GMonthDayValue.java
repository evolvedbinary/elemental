/*
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

import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.GregorianCalendar;

public class GMonthDayValue extends AbstractDateTimeValue {

    public GMonthDayValue() throws XPathException {
        super(null, stripCalendar(TimeUtils.getInstance().newXMLGregorianCalendar(new GregorianCalendar())));
    }

    public GMonthDayValue(final Expression expression) throws XPathException {
        super(expression, stripCalendar(TimeUtils.getInstance().newXMLGregorianCalendar(new GregorianCalendar())));
    }

    public GMonthDayValue(final XMLGregorianCalendar calendar) throws XPathException {
        this(null, calendar);
    }

    public GMonthDayValue(final Expression expression, XMLGregorianCalendar calendar) throws XPathException {
        super(expression, stripCalendar((XMLGregorianCalendar) calendar.clone()));
    }

    public GMonthDayValue(final String timeValue) throws XPathException {
        this(null, timeValue);
    }

    public GMonthDayValue(final Expression expression, String timeValue) throws XPathException {
        super(expression, timeValue);
        try {
            if (calendar.getXMLSchemaType() != DatatypeConstants.GMONTHDAY) {
                throw new IllegalStateException();
            }
        } catch (final IllegalStateException e) {
            throw new XPathException(getExpression(), "xs:time instance must not have year, month or day fields set");
        }
    }

    private static XMLGregorianCalendar stripCalendar(XMLGregorianCalendar calendar) {
        calendar = (XMLGregorianCalendar) calendar.clone();
        calendar.setYear(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setHour(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setMinute(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setSecond(DatatypeConstants.FIELD_UNDEFINED);
        calendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        return calendar;
    }

    @Override
    public AtomicValue convertTo(final int requiredType) throws XPathException {
        switch (requiredType) {
            case Type.G_MONTH_DAY:
            case Type.ANY_ATOMIC_TYPE:
            case Type.ITEM:
                return this;
            case Type.STRING:
                return new StringValue(getExpression(), getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getExpression(), getStringValue());
            default:
                throw new XPathException(getExpression(), ErrorCodes.FORG0001,
                        "Type error: cannot cast xs:time to "
                                + Type.getTypeName(requiredType));
        }
    }

    protected AbstractDateTimeValue createSameKind(XMLGregorianCalendar cal)
            throws XPathException {
        return new GMonthDayValue(getExpression(), cal);
    }

    public int getType() {
        return Type.G_MONTH_DAY;
    }

    protected QName getXMLSchemaType() {
        return DatatypeConstants.GMONTHDAY;
    }

    public ComputableValue minus(ComputableValue other) throws XPathException {
        throw new XPathException(getExpression(), "Subtraction is not supported on values of type " +
                Type.getTypeName(getType()));
    }
}
