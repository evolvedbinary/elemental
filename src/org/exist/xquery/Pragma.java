/*
*  eXist Open Source Native XML Database
*  Copyright (C) 2001-04 Wolfgang M. Meier (wolfgang@exist-db.org) 
*  and others (see http://exist-db.org)
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

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exist.dom.QName;

/**
 * Represents an XQuery pragma. Pragmas are used to pass 
 * vendor-specific information to the XQuery engine. They may
 * occur anywhere inside the query. The specified pragmas can
 * be accessed through method 
 * {@link org.exist.xquery.XQueryContext#getPragma(QName)}.
 * 
 * @author wolf
 */
public class Pragma {

	public final static QName TIMEOUT_QNAME = new QName("timeout", XQueryContext.EXIST_NS);
	public final static QName OUTPUT_SIZE_QNAME = new QName("output-size-limit", XQueryContext.EXIST_NS);
	public final static QName SERIALIZE_QNAME = new QName("serialize", XQueryContext.EXIST_NS);
    public final static QName PROFILE_QNAME = new QName("profiling", XQueryContext.EXIST_NS);
	
	private final static String paramPattern =
		"\\s*([\\w\\.-]+)\\s*=\\s*('[^']*'|\"[^\"]*\"|[^\\s]+)";
	
	private static Pattern pattern = Pattern.compile(paramPattern);
	private static Matcher matcher = pattern.matcher("");
    
	private QName qname;
	private String contents;
	
	public Pragma(QName qname, String contents) {
		this.qname = qname;
		this.contents = contents;
	}
	
	public QName getQName() {
		return qname;
	}
	
	public String getContents() {
		return contents;
	}
	
	public String[] tokenizeContents() {
		if(contents == null)
			return new String[0];
		StringTokenizer tok = new StringTokenizer(contents, " \r\t\n");
		String[] items = new String[tok.countTokens()];
		for(int i = 0; tok.hasMoreTokens(); i++) {
			items[i] = tok.nextToken();
		}
		return items;
	}
	
	public static synchronized String[] parseKeyValuePair(String s) {
        matcher.reset(s);
		if(matcher.matches()) {
			String value = matcher.group(2);
			if(value.charAt(0) == '\'' || value.charAt(0) == '"')
				value = value.substring(1, value.length() - 1);
			return new String[] { matcher.group(1), value };
		}
		return null;
	}
	
	public boolean equals(Object other) {
		if (other instanceof Pragma) {
			return qname.equalsSimple(((Pragma)other).qname);
		}
		return false;
	}
}
