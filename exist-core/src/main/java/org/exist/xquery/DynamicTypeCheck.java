/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
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
package org.exist.xquery;

import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.*;

/**
 * Check a function parameter type at runtime.
 *  
 * @author wolf
 */
public class DynamicTypeCheck extends AbstractExpression {

	final private Expression expression;
	final private int requiredType;
	
	public DynamicTypeCheck(XQueryContext context, int requiredType, Expression expr) {
		super(context);
		this.requiredType = requiredType;
		this.expression = expr;
	}
	
    /* (non-Javadoc)
     * @see org.exist.xquery.Expression#analyze(org.exist.xquery.AnalyzeContextInfo)
     */
    public void analyze(AnalyzeContextInfo contextInfo) throws XPathException {
    	contextInfo.setParent(this);
        expression.analyze(contextInfo);
    }
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.xquery.StaticContext, org.exist.dom.persistent.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(
		Sequence contextSequence,
		Item contextItem)
		throws XPathException {
		final Sequence seq = expression.eval(contextSequence, contextItem);
        Sequence result = null;
        if (Type.subTypeOf(requiredType, Type.ANY_ATOMIC_TYPE) && !Type.subTypeOf(seq.getItemType(), requiredType)) {
            result = new ValueSequence();
        }
        if (seq.hasOne())
            {check(result, seq.itemAt(0));}
        else if (!seq.isEmpty()) {
            for(final SequenceIterator i = seq.iterate(); i.hasNext(); ) {
                final Item item = i.nextItem();
                check(result, item);
            }
        }
		return result == null ? seq : result;
	}

    private void check(Sequence result, Item item) throws XPathException {
        int type = item.getType();
        if (type == Type.NODE &&
                ((NodeValue) item).getImplementationType() == NodeValue.PERSISTENT_NODE) {
            type = ((NodeProxy) item).getNodeType();
            if (type == NodeProxy.UNKNOWN_NODE_TYPE)
                //Retrieve the actual node
                {type= ((NodeProxy) item).getNode().getNodeType();}
        }
        if (requiredType == Type.JAVA_OBJECT) {
            type = Type.JAVA_OBJECT;
        }
        if(type != requiredType && !Type.subTypeOf(type, requiredType)) {
            //TODO : how to make this block more generic ? -pb
            if (type == Type.UNTYPED_ATOMIC) {
                try {
                    item = item.convertTo(requiredType);
                //No way
                } catch (final XPathException e) {
                    throw new XPathException(expression, ErrorCodes.FOCH0002, "Required type is " +
                            Type.getTypeName(requiredType) + " but got '" + Type.getTypeName(item.getType()) + "(" +
                            item.getStringValue() + ")'");
                }
            //XDM: The dm:string-value accessor returns the string value of a node. It is defined on all seven node kinds.
            } else if (requiredType == Type.STRING && Type.subTypeOf(type, Type.NODE)) {
            	item = item.convertTo(Type.STRING);
            //Then, if numeric, try to refine the type
            //xs:decimal(3) treat as xs:integer
            } else if (Type.subTypeOfUnion(requiredType, Type.NUMERIC) && Type.subTypeOf(type, requiredType)) {
                try {
                    item = item.convertTo(requiredType);
                //No way
                } catch (final XPathException e) {
                    throw new XPathException(expression, ErrorCodes.FOCH0002, "Required type is " +
                            Type.getTypeName(requiredType) + " but got '" + Type.getTypeName(item.getType()) + "(" +
                            item.getStringValue() + ")'");
                }
            //Then, if duration, try to refine the type
            //No test on the type hierarchy ; this has to pass :
            //fn:months-from-duration(xs:duration("P1Y2M3DT10H30M"))
            //TODO : find a way to enforce the test (by making a difference between casting and treating as ?)
            } else if (Type.subTypeOf(requiredType, Type.DURATION) /*&& Type.subTypeOf(type, requiredType)*/) {
                try {
                    item = item.convertTo(requiredType);
                //No way
                } catch (final XPathException e) {
                    throw new XPathException(expression, ErrorCodes.FOCH0002, "Required type is " +
                            Type.getTypeName(requiredType) + " but got '" + Type.getTypeName(item.getType()) + "(" +
                            item.getStringValue() + ")'");
                }
            //Then, if date, try to refine the type
            //No test on the type hierarchy
            //TODO : find a way to enforce the test (by making a difference between casting and treating as ?)
            } else if (Type.subTypeOf(requiredType, Type.DATE) /*&& Type.subTypeOf(type, requiredType)*/) {
                try {
                    item = item.convertTo(requiredType);
                //No way
                } catch (final XPathException e) {
                    throw new XPathException(expression, ErrorCodes.FOCH0002, "Required type is " +
                            Type.getTypeName(requiredType) + " but got '" + Type.getTypeName(item.getType()) + "(" +
                            item.getStringValue() + ")'");
                }
            //URI type promotion: A value of type xs:anyURI (or any type derived
            //by restriction from xs:anyURI) can be promoted to the type xs:string.
            //The result of this promotion is created by casting the
            //original value to the type xs:string.
            } else if (type == Type.ANY_URI && requiredType == Type.STRING) {
                    item = item.convertTo(Type.STRING);
                    type = Type.STRING;
            } else {
                if (!(Type.subTypeOf(type, requiredType))) {
                    throw new XPathException(expression, ErrorCodes.XPTY0004,
                            Type.getTypeName(item.getType()) + "(" + item.getStringValue() +
                            ") is not a sub-type of " + Type.getTypeName(requiredType));

                } else
                    {throw new XPathException(expression, ErrorCodes.FOCH0002, "Required type is " +
                        Type.getTypeName(requiredType) + " but got '" + Type.getTypeName(item.getType()) + "(" +
                        item.getStringValue() + ")'");}
            }
        }
        if (result != null)
            {result.add(item);}
    }

    /* (non-Javadoc)
    * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
    */
    public void dump(ExpressionDumper dumper) {
        if(dumper.verbosity() > 1) {            
            dumper.display("dynamic-type-check"); 
            dumper.display("["); 
            dumper.display(Type.getTypeName(requiredType));
            dumper.display(", "); 
        }
        expression.dump(dumper);
        if(dumper.verbosity() > 1)
            {dumper.display("]");}
    }
    
    public String toString() {
        return expression.toString();
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
		return expression.getDependencies();
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#resetState()
	 */
	public void resetState(boolean postOptimization) {
		super.resetState(postOptimization);
		expression.resetState(postOptimization);
	}
	
	public void setContextDocSet(DocumentSet contextSet) {
		super.setContextDocSet(contextSet);
		expression.setContextDocSet(contextSet);
	}

    public int getLine() {
        return expression.getLine();
    }

    public int getColumn() {
        return expression.getColumn();
    }

    public void accept(ExpressionVisitor visitor) {
		expression.accept(visitor);
	}

    public int getSubExpressionCount() {
    	return 1;
    }
    
    public Expression getSubExpression(int index) {
    	if (index == 0) {return expression;}
    	
	    throw new IndexOutOfBoundsException("Index: "+index+", Size: "+getSubExpressionCount());
    }
    
}