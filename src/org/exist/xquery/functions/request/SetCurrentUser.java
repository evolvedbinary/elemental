/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 Wolfgang M. Meier
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
package org.exist.xquery.functions.request;

import org.exist.dom.QName;
import org.exist.http.servlets.RequestWrapper;
import org.exist.http.servlets.SessionWrapper;
import org.exist.security.SecurityManager;
import org.exist.security.User;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

/**
 * @author wolf
 */
public class SetCurrentUser extends BasicFunction {

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName("set-current-user", RequestModule.NAMESPACE_URI, RequestModule.PREFIX),
			"Encodes the specified URL with the current HTTP session-id.",
			new SequenceType[] {
				new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE),
				new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE)
			},
			new SequenceType(Type.BOOLEAN, Cardinality.ZERO_OR_ONE));
	
	public SetCurrentUser(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence)
			throws XPathException {
		RequestModule myModule = (RequestModule)context.getModule(RequestModule.NAMESPACE_URI);
		
		// request object is read from global variable $request
		Variable var = 
			myModule.resolveVariable(RequestModule.REQUEST_VAR);
		if (var.getValue().getItemType() != Type.JAVA_OBJECT)
			throw new XPathException("Variable $request is not bound to an Java object.");

		JavaObjectValue value = (JavaObjectValue) var.getValue().itemAt(0);
		RequestWrapper request;
		if(value.getObject() instanceof RequestWrapper) {
			request = (RequestWrapper)value.getObject();
		} else
			throw new XPathException("Variable $request is not bound to a Request object.");
		
		String userName = args[0].getStringValue();
		String passwd = args[1].getStringValue();
		
		SecurityManager security = context.getBroker().getBrokerPool().getSecurityManager();
		User user = security.getUser(userName);
		if (user == null)
			return Sequence.EMPTY_SEQUENCE;
		if (user.validate(passwd)) {
			context.getBroker().setUser(user);
			SessionWrapper session = request.getSession(true);
			session.setAttribute("user", userName);
			session.setAttribute("password", new StringValue(passwd));
			return BooleanValue.TRUE;
		} else {
			LOG.warn("Could not validate user " + userName);
			return BooleanValue.FALSE;
		}
	}

}
