/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.functions;

import org.exist.dom.QName;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Function;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Module;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.ComputableValue;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

/**
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class FunAvg extends Function {

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName("avg", Module.BUILTIN_FUNCTION_NS),
			"Returns the average of the values in the input sequence $a, that is, the "
				+ "sum of the values divided by the number of values.",
			new SequenceType[] { new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_MORE)},
			new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_ONE));

	/**
	 * @param context
	 * @param signature
	 */
	public FunAvg(XQueryContext context) {
		super(context, signature);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.dom.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem)
		throws XPathException {
		Sequence inner = getArgument(0).eval(contextSequence, contextItem);
		if (inner.getLength() == 0)
			return Sequence.EMPTY_SEQUENCE;

		SequenceIterator iter = inner.iterate();
		Item nextItem;
		AtomicValue nextValue;
		nextItem = iter.nextItem();
		nextValue = nextItem.atomize();
		if (!Type.subTypeOf(nextValue.getType(), Type.NUMBER))
			nextValue = nextValue.convertTo(Type.DOUBLE);
		ComputableValue sum = (ComputableValue) nextValue;
		while (iter.hasNext()) {
			nextItem = iter.nextItem();
			nextValue = nextItem.atomize();
			if (!Type.subTypeOf(nextValue.getType(), Type.NUMBER))
				nextValue = nextValue.convertTo(Type.DOUBLE);
			sum = sum.plus((NumericValue) nextValue);
		}
		return sum.div(new IntegerValue(inner.getLength()));
	}
}
