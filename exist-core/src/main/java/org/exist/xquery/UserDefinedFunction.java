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
import org.exist.dom.QName;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wolf
 */
public class UserDefinedFunction extends Function implements Cloneable {

	private Expression body;
	
	private List<QName> parameters = new ArrayList<>(5);
	
	private Sequence[] currentArguments = null;

    private DocumentSet[] contextDocs = null;
    
    private boolean bodyAnalyzed = false;
    
    private FunctionCall call;
    
    private boolean hasBeenReset = false;

    protected boolean visited = false;

    private List<ClosureVariable> closureVariables = null;
    
	public UserDefinedFunction(XQueryContext context, FunctionSignature signature) {
		super(context, signature);
	}
	
	public void setFunctionBody(Expression body) {
		this.body = body.simplify();
	}

    public Expression getFunctionBody() {
        return body;
    }
    
    public void addVariable(final String varName) throws XPathException {
		try {
			final QName qname = QName.parse(context, varName, null);
			addVariable(qname);
		} catch (final QName.IllegalQNameException e) {
			throw new XPathException(this, ErrorCodes.XPST0081, "No namespace defined for prefix " + varName);
		}
	}
	
    public void addVariable(QName varName) throws XPathException {
    	if (parameters.contains(varName))
			{throw new XPathException(this, "XQST0039: function " + getName() + " is already have parameter with the name "+varName);}

		parameters.add(varName);
    }
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.Function#setArguments(java.util.List)
	 */
	public void setArguments(Sequence[] args, DocumentSet[] contextDocs) throws XPathException {
		this.currentArguments = args;
        this.contextDocs = contextDocs;
    }
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.Function#analyze(org.exist.xquery.AnalyzeContextInfo)
	 */
	public void analyze(AnalyzeContextInfo contextInfo) throws XPathException {
		hasBeenReset = false;
		
		if(call != null && !call.isRecursive()) {
			// Save the local variable stack
			final LocalVariable mark = context.markLocalVariables(true);
			if (closureVariables != null)
				// if this is a inline function, context variables are known
	        	{context.restoreStack(closureVariables);}
			try {
				LocalVariable var;
				for(final QName varName : parameters) {
					var = new LocalVariable(varName);
					context.declareVariableBinding(var);
				}

				final AnalyzeContextInfo newContextInfo = new AnalyzeContextInfo(contextInfo);
				newContextInfo.setParent(this);
				if (!bodyAnalyzed) {
					if(body != null) {
						body.analyze(newContextInfo);
					}
					bodyAnalyzed = true;
				}
			} finally {
				// restore the local variable stack
				context.popLocalVariables(mark);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.dom.persistent.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
//        context.expressionStart(this);
        context.stackEnter(this);
        // make sure reset state is called after query has finished
	    hasBeenReset = false;
        // Save the local variable stack
        final LocalVariable mark = context.markLocalVariables(true);
        if (closureVariables != null)
        	{context.restoreStack(closureVariables);}
        Sequence result = null;
		try {
			QName varName;
			LocalVariable var;
			int j = 0;
			for (int i = 0; i < parameters.size(); i++, j++) {
				varName = parameters.get(i);
				var = new LocalVariable(varName);
				var.setValue(currentArguments[j]);
				if (contextDocs != null)
					{var.setContextDocs(contextDocs[i]);}
				context.declareVariableBinding(var);
				
				Cardinality actualCardinality;
				if (currentArguments[j].isEmpty()) {actualCardinality = Cardinality.EMPTY_SEQUENCE;}
				else if (currentArguments[j].hasMany()) {actualCardinality = Cardinality._MANY;}
				else {actualCardinality = Cardinality.EXACTLY_ONE;}
				
				if (!getSignature().getArgumentTypes()[j].getCardinality().isSuperCardinalityOrEqualOf(actualCardinality))
					{throw new XPathException(this, ErrorCodes.XPTY0004, "Invalid cardinality for parameter $" + varName +  
 						". Expected " + getSignature().getArgumentTypes()[j].getCardinality().getHumanDescription() +
 						", got " + currentArguments[j].getItemCount());}
			}
			result = body.eval(null, null);
			return result;
		} finally {
			// restore the local variable stack
            context.popLocalVariables(mark, result);
            context.stackLeave(this);
//            context.expressionEnd(this);
        }
	}
	
	/* (non-Javadoc)
     * @see org.exist.xquery.Function#dump(org.exist.xquery.util.ExpressionDumper)
     */
    public void dump(ExpressionDumper dumper) {
        final FunctionSignature signature = getSignature();
        if (signature.getName() != null)
        	{dumper.display(signature.getName());}
        dumper.display('(');
        for(int i = 0; i < signature.getArgumentTypes().length; i++) {
			if(i > 0)
				{dumper.display(", ");}
			dumper.display(signature.getArgumentTypes()[i]);
		}
		dumper.display(") ");
        dumper.display(signature.getReturnType().toString());
    }
    
    @Override
    public String toString() {
        final FunctionSignature signature = getSignature();
        final StringBuilder buf = new StringBuilder();
        if (signature.getName() != null) {
			buf.append(signature.getName());
		}
        buf.append('(');
        for (int i = 0; i < signature.getArgumentTypes().length; i++) {
			if (i > 0) {
				buf.append(", ");
			}
			buf.append('$');
			buf.append(getParameters().get(i));
			buf.append(" as ");
			buf.append(signature.getArgumentTypes()[i]);
		}
        buf.append(") as ");
		buf.append(signature.getReturnType());
        return buf.toString();
    }
    
	/* (non-Javadoc)
	 * @see org.exist.xquery.functions.Function#getDependencies()
	 */
	public int getDependencies() {
		return Dependency.CONTEXT_SET + Dependency.CONTEXT_ITEM
			+ Dependency.CONTEXT_POSITION;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.PathExpr#resetState()
	 */
	public void resetState(boolean postOptimization) {
		if (hasBeenReset) {
            return;
        }
		hasBeenReset = true;
		
		super.resetState(postOptimization);
        // Question: understand this test. Why not reset even is not in recursion ?
		// Answer: would lead to an infinite loop if the function is recursive.
        bodyAnalyzed = false;
        if(body != null) {
			body.resetState(postOptimization);
		}

        if (!postOptimization) {
            currentArguments = null;
            contextDocs = null;
        }
    }

    public void accept(ExpressionVisitor visitor) {
        if (visited)
            {return;}
        visited = true;
        visitor.visitUserFunction(this);
    }
    
    /**
     * Return the functions parameters list
     * 
     * @return List of function parameters
     */
    public List<QName> getParameters()
    {
    	return parameters;
    }

    public synchronized Object clone() {
    	try {
    		final UserDefinedFunction clone = (UserDefinedFunction) super.clone();
    		
    		clone.currentArguments = null;
    		clone.contextDocs = null;
    		
    		clone.body = this.body; // so body will be analyzed and optimized for all calls of such functions in recursion.  
    	    
    	    return clone;
    	} catch (final CloneNotSupportedException e) {
    	    // this shouldn't happen, since we are Cloneable
    	    throw new InternalError();
    	}
    }
    
    public FunctionCall getCaller(){
    	return call;
    }
    
    public void setCaller(FunctionCall call){
    	this.call = call;
    }
    
    public void setClosureVariables(List<ClosureVariable> vars) {
    	this.closureVariables = vars;
    	if (vars != null) {
    		// register the closure with the context so it gets cleared after execution
    		context.pushClosure(this);
		}
    }

    public List<ClosureVariable> getClosureVariables() {
        return closureVariables;
    }

    protected Sequence[] getCurrentArguments() {
        return currentArguments;
    }
}
