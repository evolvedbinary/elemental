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

import org.exist.xquery.util.Error;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;

/**
 * Runtime-check for the cardinality of a function parameter.
 * 
 * @author wolf
 */
public class DynamicCardinalityCheck extends AbstractExpression {
    
	private Expression expression;
	private int requiredCardinality;
    private Error error;
    
	public DynamicCardinalityCheck(XQueryContext context, int requiredCardinality, Expression expr,
            Error error) {
		super(context);
		this.requiredCardinality = requiredCardinality;
		this.expression = expr;
        this.error = error;
	}
	
	/* (non-Javadoc)
     * @see org.exist.xquery.Expression#analyze(org.exist.xquery.Expression)
     */
    public void analyze(Expression parent, int flags) throws XPathException {
        expression.analyze(this, flags);
    }
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.xquery.StaticContext, org.exist.dom.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
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
		int seqLength = seq.getLength();
		if(seqLength > 0 && requiredCardinality == Cardinality.EMPTY) {
            error.addArgs(ExpressionDumper.dump(expression), 
                    Cardinality.getDescription(requiredCardinality), new Integer(seqLength));
            throw new XPathException(getASTNode(), error.toString());
        }
		if(seqLength == 0 && (requiredCardinality & Cardinality.ZERO) == 0) {
            error.addArgs(ExpressionDumper.dump(expression), 
                    Cardinality.getDescription(requiredCardinality), new Integer(seqLength));
            throw new XPathException(getASTNode(), error.toString());
        } else if(seqLength > 1 && (requiredCardinality & Cardinality.MANY) == 0) {
            error.addArgs(ExpressionDumper.dump(expression), 
                    Cardinality.getDescription(requiredCardinality), new Integer(seqLength));
            throw new XPathException(getASTNode(), error.toString());
        }
        
        if (context.getProfiler().isEnabled())           
            context.getProfiler().end(this, "", seq);  
        
		return seq;
	}
    
	/* (non-Javadoc)
     * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
     */
    public void dump(ExpressionDumper dumper) {
        if(dumper.verbosity() > 1) {            
            dumper.display("dynamic-cardinality-check"); 
            dumper.display("["); 
            dumper.display(Cardinality.getDescription(requiredCardinality));
            dumper.display(", ");             
        }
        expression.dump(dumper);
        if(dumper.verbosity() > 1)
	        dumper.display("]");
    }
    
    public String toString() {
    	StringBuffer result = new StringBuffer();
        result.append("dynamic-cardinality-check"); 
        result.append("["); 
        result.append(Cardinality.getDescription(requiredCardinality));
        result.append(", "); 
        result.append(expression.toString());
        result.append("]");
    	return result.toString();
    }    
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#returnsType()
	 */
	public int returnsType() {
		return expression.returnsType();
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
	public void resetState() {
		expression.resetState();
	}
}
