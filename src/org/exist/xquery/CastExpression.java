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
package org.exist.xquery;

import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

/**
 * CastExpression represents cast expressions as well as all type 
 * constructors.
 * 
 * @author wolf
 */
public class CastExpression extends AbstractExpression {

	private Expression expression;
	private int requiredType;
	private int cardinality = Cardinality.EXACTLY_ONE;
	
	/**
	 * Constructor. When calling {@link #eval(Sequence, Item)} 
	 * the passed expression will be cast into the required type and cardinality.
	 * 
	 * @param context
	 */
	public CastExpression(XQueryContext context, Expression expr, int requiredType, int cardinality) {
		super(context);
		this.expression = expr;
		this.requiredType = requiredType;
		this.cardinality = cardinality;
		if(!Type.subTypeOf(expression.returnsType(), Type.ATOMIC))
			expression = new Atomize(context, expression);
	}

	/* (non-Javadoc)
     * @see org.exist.xquery.Expression#analyze(org.exist.xquery.Expression)
     */
    public void analyze(Expression parent, int flags) throws XPathException {
        expression.analyze(this, flags);
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
        
		Sequence seq = expression.eval(contextSequence, contextItem);
		if (seq.getLength() == 0) {
			if ((cardinality & Cardinality.ZERO) == 0)
				throw new XPathException(getASTNode(), "Type error: empty sequence is not allowed here");
			else
				return Sequence.EMPTY_SEQUENCE;
		}
        
        Item item = seq.itemAt(0);
        try {
            // casting to QName needs special treatment
            if(requiredType == Type.QNAME) {
                if(item.getType() == Type.ATOMIC || Type.subTypeOf(item.getType(), Type.STRING)) {
                    return new QNameValue(context, item.getStringValue());
                } else {
                    throw new XPathException(getASTNode(), "Cannot cast " + Type.getTypeName(item.getType()) + 
                            " to xs:QName");
                }
            } else
        		return (AtomicValue)seq.itemAt(0).convertTo(requiredType);
		} catch(XPathException e) {
			e.setASTNode(getASTNode());
			throw e;
		}
	}

	/* (non-Javadoc)
     * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
     */
    public void dump(ExpressionDumper dumper) {
        expression.dump(dumper);
        dumper.display(" cast as ");
        dumper.display(Type.getTypeName(requiredType));
    }
    
    public String toString() {
    	StringBuffer result = new StringBuffer();
    	result.append(expression.toString());
    	result.append(" cast as ");
    	result.append(Type.getTypeName(requiredType));
    	return result.toString();
    }    
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#returnsType()
	 */
	public int returnsType() {
		return requiredType;
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#getDependencies()
	 */
	public int getDependencies() {
		return Dependency.CONTEXT_SET + Dependency.CONTEXT_ITEM;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#getCardinality()
	 */
	public int getCardinality() {
		return Cardinality.ZERO_OR_ONE;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#resetState()
	 */
	public void resetState() {
		expression.resetState();
	}
	
}
