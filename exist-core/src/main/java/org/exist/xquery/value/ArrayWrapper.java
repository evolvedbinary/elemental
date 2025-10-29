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
package org.exist.xquery.value;

import org.exist.xquery.Expression;
import org.exist.xquery.XQueryContext;

/**
 * This class is used to wrap a Java Array
 * that should be converted to an XDM Array.
 * It helps disambiguate between a Java Array
 * that should be converted to an XDM Sequence,
 * and a Java Array that should be converted to
 * an XDM Array.
 *
 * See {@link org.exist.xquery.XPathUtil#javaObjectToXPath(Object, XQueryContext, boolean, boolean, boolean, Expression)}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ArrayWrapper<T> {
    public final T[] array;

    public ArrayWrapper(final T[] array) {
        this.array = array;
    }
}
