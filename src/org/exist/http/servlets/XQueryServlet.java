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
package org.exist.http.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.exist.source.FileSource;
import org.exist.source.Source;
import org.exist.xmldb.CompiledExpression;
import org.exist.xmldb.XQueryService;
import org.exist.xquery.XPathException;
import org.exist.xquery.functions.request.RequestModule;
import org.exist.xquery.value.Item;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

/**
 * Servlet to generate HTML output from an XQuery file.
 * 
 * The servlet responds to an URL pattern as specified in the
 * WEB-INF/web.xml configuration file of the application. It will
 * interpret the path with which it is called as leading to a valid
 * XQuery file. The XQuery file is loaded, compiled and executed.
 * Any output of the script is sent back to the client.
 * 
 * The servlet accepts the following initialization parameters in web.xml:
 * 
 * <table border="0">
 * 	<tr><td>user</td><td>The user identity with which the script is executed.</td></tr>
 * 	<tr><td>password</td><td>Password for the user.</td></tr>
 * 	<tr><td>uri</td><td>A valid XML:DB URI leading to the root collection used to
 * 	process the request.</td></tr>
 * 	<tr><td>encoding</td><td>The character encoding used for XQuery files.</td></tr>
 * 	<tr><td>container-encoding</td><td>The character encoding used by the servlet
 * 	container.</td></tr>
 * 	<tr><td>form-encoding</td><td>The character encoding used by parameters posted
 * 	from HTML forms.</td></tr>
 * </table>
 * 
 * User identity and password may also be specified through the HTTP session attributes
 * "user" and "password". These attributes will overwrite any other settings.
 * 
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class XQueryServlet extends HttpServlet {

	public final static String DEFAULT_USER = "guest";
	public final static String DEFAULT_PASS = "guest";
	public final static String DEFAULT_URI = "xmldb:exist:///db";
	public final static String DEFAULT_ENCODING = "UTF-8";
	public final static String DEFAULT_CONTENT_TYPE = "text/html";
	
	public final static String DRIVER = "org.exist.xmldb.DatabaseImpl";
		
	private String user = null;
	private String password = null;
	private String collectionURI = null;
	
	private String containerEncoding = null;
	private String formEncoding = null;
	private String encoding = null;
	private String contentType = null;
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		user = config.getInitParameter("user");
		if(user == null)
			user = DEFAULT_USER;
		password = config.getInitParameter("password");
		if(password == null)
			password = DEFAULT_PASS;
		collectionURI = config.getInitParameter("uri");
		if(collectionURI == null)
			collectionURI = DEFAULT_URI;
		formEncoding = config.getInitParameter("form-encoding");
		if(formEncoding == null)
			formEncoding = DEFAULT_ENCODING;
		log("form-encoding = " + formEncoding);
		containerEncoding = config.getInitParameter("container-encoding");
		if(containerEncoding == null)
			containerEncoding = DEFAULT_ENCODING;
		log("container-encoding = " + containerEncoding);
		encoding = config.getInitParameter("encoding");
		if(encoding == null)
			encoding = DEFAULT_ENCODING;
		log("encoding = " + encoding);
		contentType = config.getInitParameter("content-type");
		if(contentType == null)
		    contentType = DEFAULT_CONTENT_TYPE;
		
		try {
			Class driver = Class.forName(DRIVER);
			Database database = (Database)driver.newInstance();
			database.setProperty("create-database", "true");
			DatabaseManager.registerDatabase(database);
		} catch(Exception e) {
			throw new ServletException("Failed to initialize database driver: " + e.getMessage(), e);
		}
		
		// set exist.home property if not set
		String homeDir = System.getProperty("exist.home");
		if(homeDir == null) {
			homeDir = config.getServletContext().getRealPath("/");
			System.setProperty("exist.home", homeDir);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void process(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		ServletOutputStream sout = response.getOutputStream();
		PrintWriter output = 
			new PrintWriter(new OutputStreamWriter(sout, formEncoding));
		response.setContentType(contentType + "; charset=" + formEncoding);
		response.addHeader( "pragma", "no-cache" );
		response.addHeader( "Cache-Control", "no-cache" );
		
		String path = request.getPathTranslated();
		if(path == null) {
			path = request.getRequestURI().substring(request.getContextPath().length());
			int p = path.lastIndexOf(';');
			if(p > -1)
				path = path.substring(0, p);
			path = getServletContext().getRealPath(path);
		}
		File f = new File(path);
		if(!f.canRead()) {
			sendError(output, "Cannot read source file", path);
			return;
		}
		
        //-------------------------------
        // Added by Igor Abade (igoravl@cosespseguros.com.br)
        // Date: Aug/06/2004
        //-------------------------------

        String contentType = this.contentType;
        try {
            contentType = getServletContext().getMimeType(path);
            if (contentType == null)
                contentType = this.contentType;
        }
        catch (Throwable e) {
            contentType = this.contentType;
        }
        finally {
            if (contentType.startsWith("text/") || (contentType.endsWith("+xml")))
                contentType += ";charset=" + formEncoding;
            response.setContentType(contentType );
        }

        //-------------------------------
        
		String baseURI = request.getRequestURI();
		int p = baseURI.lastIndexOf('/');
		if(p > -1)
			baseURI = baseURI.substring(0, p);
		baseURI = getServletContext().getRealPath(baseURI.substring(request.getContextPath().length()));
		String actualUser = null;
		String actualPassword = null;
		HttpSession session = request.getSession();
		if(session != null && request.isRequestedSessionIdValid()) {
			actualUser = getSessionAttribute(session, "user");
			actualPassword = getSessionAttribute(session, "password");
		}
		if(actualUser == null) actualUser = user;
		if(actualPassword == null) actualPassword = password;
		
		try {
			Collection collection = DatabaseManager.getCollection(collectionURI, actualUser, actualPassword);
			
			XQueryService service = (XQueryService)
				collection.getService("XQueryService", "1.0");
			service.setProperty("base-uri", baseURI);
			service.setModuleLoadPath(baseURI);
			String prefix = RequestModule.PREFIX;
			service.setNamespace(prefix, RequestModule.NAMESPACE_URI);
			service.declareVariable(prefix + ":request", 
					new HttpRequestWrapper(request, formEncoding, containerEncoding));
			service.declareVariable(prefix + ":response", new HttpResponseWrapper(response));
			service.declareVariable(prefix + ":session", new HttpSessionWrapper(session));
						
			Source source = new FileSource(f, encoding);
			ResourceSet result = service.execute(source);
			for(ResourceIterator i = result.getIterator(); i.hasMoreResources(); ) {
				Resource res = i.nextResource();
				output.println(res.getContent().toString());
			}
		} catch (XMLDBException e) {
			log(e.getMessage(), e);
			sendError(output, e.getMessage(), e);
		}
		output.flush();
	}
	
	private String getSessionAttribute(HttpSession session, String attribute) {
		Object obj = session.getAttribute(attribute);
		if(obj == null)
			return null;
		if(obj instanceof Item)
			try {
				return ((Item)obj).getStringValue();
			} catch (XPathException e) {
				return null;
			}
		return obj.toString();
	}
	
	private void sendError(PrintWriter out, String message, Exception e) {
		out.print("<html><head>");
		out.print("<title>XQueryServlet Error</title>");
		out.print("<link rel=\"stylesheet\" type=\"text/css\" href=\"error.css\"></head>");
		out.print("<body><h1>Error found</h1>");
		out.print("<div class='message'><b>Message:</b>");
		out.print(message);
		out.print("</div>");
		out.print("<h2>Exception Stacktrace:</h2>");
		out.print("<div class='exception'>");
		e.printStackTrace(out);
		out.print("</div></body></html>");
	}
	
	private void sendError(PrintWriter out, String message, String description) {
		out.print("<html><head>");
		out.print("<title>XQueryServlet Error</title>");
		out.print("<link rel=\"stylesheet\" type=\"text/css\" href=\"error.css\"></head>");
		out.println("<body><h1>Error found</h1>");
		out.print("<div class='message'><b>Message: </b>");
		out.print(message);
		out.print("</div><div class='description'>");
		out.print(description);
		out.print("</div></body></html>");
	}
	
	private String readQuery(File source) throws IOException {
		FileInputStream is = new FileInputStream(source);
		try {
			Reader reader = new InputStreamReader(is, encoding);
			char[] chars = new char[1024];
			StringBuffer buf = new StringBuffer();
			int read;
			while((read = reader.read(chars)) > -1)
				buf.append(chars, 0, read);
			return buf.toString();
		} finally {
			is.close();
		}
	}
	
	private static final class CachedQuery {
		
		long lastModified;
		String sourcePath;
		CompiledExpression expression;
		
		public CachedQuery(File sourceFile, CompiledExpression expression) {
			this.sourcePath = sourceFile.getAbsolutePath();
			this.lastModified = sourceFile.lastModified();
			this.expression = expression;
		}
		
		public boolean isValid() {
			File f = new File(sourcePath);
			if(f.lastModified() > lastModified)
				return false;
			return true;
		}
		
		public CompiledExpression getExpression() {
			return expression;
		}
	}

}
