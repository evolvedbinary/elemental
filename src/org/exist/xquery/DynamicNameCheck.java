/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Team
 *
 *  http://exist-db.org
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

import org.exist.dom.DocumentSet;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;

/**
 * Check element or attribute name to match sequence type.
 * 
 * @author wolf
 */
public class DynamicNameCheck extends AbstractExpression {

	private NameTest test;
	private Expression expression;
	
	public DynamicNameCheck(XQueryContext context, NameTest test, Expression expression) {
		super(context);
		this.test = test;
		this.expression = expression;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
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
		Item item;
		for(SequenceIterator i = seq.iterate(); i.hasNext(); ) {
			item = i.nextItem();
			if(!Type.subTypeOf(item.getType(), test.getType())) {
				throw new XPathException(expression.getASTNode(), "Type error in expression" +
					": required type is " + Type.getTypeName(test.getType()) +
					"; got: " + Type.getTypeName(item.getType()) + ": " + item.getStringValue());
			}
			Node node = ((NodeValue) item).getNode();
			if(!test.matchesName(node))
				throw new XPathException(expression.getASTNode(), "Type error in expression: " +
						"required node name is " + test.getName() + "; got: " +
						node.getNodeName());
		}
        
        if (context.getProfiler().isEnabled())           
            context.getProfiler().end(this, "", seq);  
        
		return seq;
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#returnsType()
	 */
	public int returnsType() {
		return test.nodeType;
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#resetState()
	 */
	public void resetState() {
		super.resetState();
		expression.resetState();
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#analyze(org.exist.xquery.AnalyzeContextInfo)
	 */
	public void analyze(AnalyzeContextInfo contextInfo) throws XPathException {
		contextInfo.setParent(this);
		expression.analyze(contextInfo);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
	 */
	public void dump(ExpressionDumper dumper) {
        if(dumper.verbosity() > 1) {            
            dumper.display("dynamic-name-check"); 
            dumper.display("["); 
            dumper.display(Type.getTypeName(test.nodeType));
            dumper.display(", "); 
        }
        expression.dump(dumper);
        if(dumper.verbosity() > 1)
            dumper.display("]");
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
        result.append("dynamic-name-check");   
        result.append("["); 
        result.append(Type.getTypeName(test.nodeType));
        result.append(", "); 
        result.append(expression.toString());
        result.append("]");
		return result.toString();
	}	

	public void setContextDocSet(DocumentSet contextSet) {
		super.setContextDocSet(contextSet);
		expression.setContextDocSet(contextSet);
	}
}
