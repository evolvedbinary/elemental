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

import java.text.Collator;

import org.exist.dom.QName;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Dependency;
import org.exist.xquery.Function;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Profiler;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.ComputableValue;
import org.exist.xquery.value.DoubleValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

/**
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class FunMin extends CollatingFunction {

	public final static FunctionSignature signatures[] = {
		new FunctionSignature(
			new QName("min", Function.BUILTIN_FUNCTION_NS),
			"Selects an item from the input sequence $a whose value is less than or equal to " +
			"the value of every other item in the input sequence.",
			new SequenceType[] { new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_MORE)},
			new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_ONE)
		),
		new FunctionSignature(
			new QName("min", Function.BUILTIN_FUNCTION_NS),
			"Selects an item from the input sequence $a whose value is less than or equal to " +
			"the value of every other item in the input sequence. The collation specified in $b is " +
			"used for string comparisons.",
			new SequenceType[] { 
				new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_MORE),
				new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE)
			},
			new SequenceType(Type.ATOMIC, Cardinality.ZERO_OR_ONE)
		)
	};

	/**
	 * @param context
	 * @param signature
	 */
	public FunMin(XQueryContext context, FunctionSignature signature) {
		super(context, signature);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.dom.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);       
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            if (contextItem != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
        }
        
		if (contextItem != null)
			contextSequence = contextItem.toSequence();
        
        Sequence result;
		Sequence arg = getArgument(0).eval(contextSequence, contextItem);
		if (arg.getLength() == 0)
			result = Sequence.EMPTY_SEQUENCE;
        else {
    		Collator collator = getCollator(contextSequence, contextItem, 2);
    		SequenceIterator iter = arg.unorderedIterator();
            ComputableValue min = null;
    		while (iter.hasNext()) {
                Item nextItem = iter.nextItem();
                AtomicValue nextValue = nextItem.atomize();
                if (Type.subTypeOf(nextValue.getType(), Type.NUMBER) && ((NumericValue) nextValue).isNaN()) {
                    result = DoubleValue.NaN;
                    break;
                }                
                nextValue = nextValue.convertTo(Type.NUMBER);  
                if (min == null)
                    min = (ComputableValue)nextValue;
                else
                    //TODO : use ComputableValue.max with the collator ! -pb   
                    //Ugly type-casting -pb
                    min = (ComputableValue) min.min(collator, nextValue);
            }           
            result = min;
        }
    
        if (context.getProfiler().isEnabled()) 
            context.getProfiler().end(this, "", result); 
        
        return result;   
    }

}
