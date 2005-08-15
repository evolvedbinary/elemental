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

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.BinaryDocument;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.NodeProxy;
import org.exist.dom.QName;
import org.exist.dom.SymbolTable;
import org.exist.memtree.MemTreeBuilder;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.User;
import org.exist.source.DBSource;
import org.exist.source.Source;
import org.exist.source.SourceFactory;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.util.Collations;
import org.exist.util.Configuration;
import org.exist.util.LockException;
import org.exist.xquery.parser.XQueryLexer;
import org.exist.xquery.parser.XQueryParser;
import org.exist.xquery.parser.XQueryTreeParser;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.Type;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * The current XQuery execution context. Contains the static as well
 * as the dynamic XQuery context components.
 * 
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class XQueryContext {
	
	private static final String JAVA_URI_START = "java:";
    private static final String XMLDB_URI_START = "xmldb:exist://";
    
    public final static String XML_NS = "http://www.w3.org/XML/1998/namespace";
	public final static String SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";
	public final static String SCHEMA_DATATYPES_NS =
		"http://www.w3.org/2001/XMLSchema-datatypes";
	public final static String SCHEMA_INSTANCE_NS =
		"http://www.w3.org/2001/XMLSchema-instance";
	public final static String XPATH_DATATYPES_NS =
		"http://www.w3.org/2003/05/xpath-datatypes";
	public final static String XQUERY_LOCAL_NS =
		"http://www.w3.org/2003/08/xquery-local-functions";
	public final static String EXIST_NS =
		"http://exist.sourceforge.net/NS/exist";
	
	private final static Logger LOG = Logger.getLogger(XQueryContext.class);

	private static final String TEMP_STORE_ERROR = "Error occurred while storing temporary data";
	
    private static final HashMap moduleClasses = new HashMap();
    
	// Static namespace/prefix mappings
	protected HashMap namespaces;
	
	// Local in-scope namespace/prefix mappings in the current context
	protected HashMap inScopeNamespaces = new HashMap();

	// Static prefix/namespace mappings
	protected HashMap prefixes;
	
	// Local prefix/namespace mappings in the current context
	protected HashMap inScopePrefixes = new HashMap();

	// Local namespace stack
	protected Stack namespaceStack = new Stack();

	// Known user defined functions in the local module
	protected TreeMap declaredFunctions = new TreeMap();

	// Globally declared variables
	protected TreeMap globalVariables = new TreeMap();

	// The last element in the linked list of local in-scope variables
	protected LocalVariable lastVar = null;
	
	// The current size of the variable stack
	protected int variableStackSize = 0;
	
	// Unresolved references to user defined functions
	protected Stack forwardReferences = new Stack();
	
	// List of pragmas declared for this query
	protected List pragmas = null;
	
    /**
     * the watchdog object assigned to this query
     *  
     * @uml.property name="watchdog"
     * @uml.associationEnd multiplicity="(1 1)"
     */
	protected XQueryWatchDog watchdog;
    
	/**
	 * Loaded modules.
	 */
	protected HashMap modules = new HashMap();

	/** 
	 * The set of statically known documents specified as
	 * an array of paths to documents and collections.
	 */
	protected String[] staticDocumentPaths = null;
	
	/**
	 * The actual set of statically known documents. This
	 * will be generated on demand from staticDocumentPaths.
	 */
	protected DocumentSet staticDocuments = null;
	
	/**
	 * The main database broker object providing access
	 * to storage and indexes. Every XQuery has its own
	 * DBBroker object.
	 */
	protected DBBroker broker;

	protected String baseURI = "";
    protected boolean baseURISetInProlog = false;
    
	protected String moduleLoadPath = ".";
	
	protected String defaultFunctionNamespace = Module.BUILTIN_FUNCTION_NS;

	/**
	 * The default collation URI
	 */
	private String defaultCollation = Collations.CODEPOINT;
	
	/**
	 * Default Collator. Will be null for the default unicode codepoint collation.
	 */
	private Collator defaultCollator = null;
	
	/**
	 * Set to true to enable XPath 1.0
	 * backwards compatibility.
	 */
	private boolean backwardsCompatible = true;

	/**
	 * Should whitespace inside node constructors be stripped?
	 */
	private boolean stripWhitespace = true;

	/**
	 * The position of the currently processed item in the context 
	 * sequence. This field has to be set on demand, for example,
	 * before calling the fn:position() function. 
	 */
	private int contextPosition = 0;

	/**
	 * The builder used for creating in-memory document 
	 * fragments
	 */
	private MemTreeBuilder builder = null;
	
	/**
	 * Stack for temporary document fragments
	 */
	private Stack fragmentStack = new Stack();

	/**
	 * The root of the expression tree
	 */
	private Expression rootExpression;
	
	/**
	 * Flag to indicate that the query should put an exclusive
	 * lock on all documents involved.
	 * 
	 * TODO: No longer needed?
	 */
	private boolean exclusive = false;
	
	/**
	 * Should all documents loaded by the query be locked?
	 * If set to true, it is the responsibility of the calling client
	 * code to unlock documents after the query has completed.
	 */
	private boolean lockDocumentsOnLoad = false;
    
    /**
     * Documents locked during the query.
     */
	private DocumentSet lockedDocuments = null;
	
    /**
     * Is profiling enabled?
     */
    private boolean profile = false;
    
    /**
     * The profiler instance used by this context.
     */
    private Profiler profiler = new Profiler();
    
	protected XQueryContext() {
		builder = new MemTreeBuilder(this);
		builder.startDocument();
	}
	
	public XQueryContext(DBBroker broker) {
		this();
		this.broker = broker;
		loadDefaults(broker.getConfiguration());
	}
	
    /**
     * @return true if profiling is enabled for this context.
     */
    public boolean isProfilingEnabled() {
        return profile;
    }
    
    /**
     * Returns the {@link Profiler} instance of this context 
     * if profiling is enabled.
     * 
     * @return the profiler instance.
     */
    public Profiler getProfiler() {
        return profiler;
    }
    
	/**
	 * Called from the XQuery compiler to set the root expression
	 * for this context.
	 * 
	 * @param expr
	 */
	public void setRootExpression(Expression expr) {
		this.rootExpression = expr;
	}
	
	/**
	 * Returns the root expression of the XQuery associated with
	 * this context.
	 * 
	 * @return
	 */
	public Expression getRootExpression() {
		return rootExpression;
	}
	
	/**
	 * Declare a user-defined prefix/namespace mapping.
	 * 
	 * eXist internally keeps a table containing all prefix/namespace
	 * mappings it found in documents, which have been previously
	 * stored into the database. These default mappings need not to be
	 * declared explicitely.
	 * 
	 * @param prefix
	 * @param uri
	 */
	public void declareNamespace(String prefix, String uri) {
		if (prefix == null)
			prefix = "";
		if(uri == null)
			uri = "";
		final String prevURI = (String)namespaces.get(prefix);
		// remove any namespace URI that has been previously declared
		// for the prefix
		if(prevURI != null)
		    prefixes.remove(prevURI);
		namespaces.put(prefix, uri);
		prefixes.put(uri, prefix);
	}

	public void declareNamespaces(Map namespaceMap) {
		Map.Entry entry;
		String prefix, uri;
		for(Iterator i = namespaceMap.entrySet().iterator(); i.hasNext(); ) {
			entry = (Map.Entry)i.next();
			prefix = (String)entry.getKey();
			uri = (String) entry.getValue();
			if(prefix == null)
				prefix = "";
			if(uri == null)
				uri = "";
            namespaces.put(prefix, uri);
			prefixes.put(uri, prefix);
		}
	}
	
	/**
	 * Declare an in-scope namespace. This is called during query execution.
	 * 
	 * @param prefix
	 * @param uri
	 */
	public void declareInScopeNamespace(String prefix, String uri) {
		if (prefix == null || uri == null)
			throw new IllegalArgumentException("null argument passed to declareNamespace");
		if (inScopeNamespaces == null)
			inScopeNamespaces = new HashMap();
		inScopeNamespaces.put(prefix, uri);
	}

	/**
	 * Returns the current default function namespace.
	 * 
	 * @return
	 */
	public String getDefaultFunctionNamespace() {
		return defaultFunctionNamespace;
	}

	/**
	 * Set the default function namespace. By default, this
	 * points to the namespace for XPath built-in functions.
	 * 
	 * @param uri
	 */
	public void setDefaultFunctionNamespace(String uri) {
		defaultFunctionNamespace = uri;
	}

	/**
	 * Set the default collation to be used by all operators and functions on strings.
	 * Throws an exception if the collation is unknown or cannot be instantiated.
	 * 
	 * @param uri
	 * @throws XPathException
	 */
	public void setDefaultCollation(String uri) throws XPathException {
		if(uri.equals(Collations.CODEPOINT) || uri.equals(Collations.CODEPOINT_SHORT)) {
			defaultCollation = Collations.CODEPOINT;
			defaultCollator = null;
		}
		defaultCollator = Collations.getCollationFromURI(this, uri);
		defaultCollation = uri;
	}
	
	public String getDefaultCollation() {
		return defaultCollation;
	}
	
	public Collator getCollator(String uri) throws XPathException {
		if(uri == null)
			return defaultCollator;
		return Collations.getCollationFromURI(this, uri);
	}
	
	public Collator getDefaultCollator() {
		return defaultCollator;
	}
	
	/**
	 * Return the namespace URI mapped to the registered prefix
	 * or null if the prefix is not registered.
	 * 
	 * @param prefix
	 * @return
	 */
	public String getURIForPrefix(String prefix) {
		String ns = (String) namespaces.get(prefix);
		if (ns == null)
			// try in-scope namespace declarations
			return inScopeNamespaces == null
				? null
				: (String) inScopeNamespaces.get(prefix);
		else
			return ns;
	}

	/**
	 * Return the prefix mapped to the registered URI or
	 * null if the URI is not registered.
	 * 
	 * @param uri
	 * @return
	 */
	public String getPrefixForURI(String uri) {
		String prefix = (String) prefixes.get(uri);
		if (prefix == null)
			return inScopePrefixes == null ? null : (String) inScopeNamespaces.get(uri);
		else
			return prefix;
	}

	/**
	 * Removes the namespace URI from the prefix/namespace 
	 * mappings table.
	 * 
	 * @param uri
	 */
	public void removeNamespace(String uri) {
		prefixes.remove(uri);
		for (Iterator i = namespaces.values().iterator(); i.hasNext();) {
			if (((String) i.next()).equals(uri)) {
				i.remove();
				return;
			}
		}
		inScopePrefixes.remove(uri);
		if (inScopeNamespaces != null) {
			for (Iterator i = inScopeNamespaces.values().iterator(); i.hasNext();) {
				if (((String) i.next()).equals(uri)) {
					i.remove();
					return;
				}
			}
		}
	}

	/**
	 * Clear all user-defined prefix/namespace mappings.
	 */
	public void clearNamespaces() {
		namespaces.clear();
		prefixes.clear();
		if (inScopeNamespaces != null) {
			inScopeNamespaces.clear();
			inScopePrefixes.clear();
		}
		loadDefaults(broker.getConfiguration());
	}

	/**
	 * Set the set of statically known documents for the current
	 * execution context. These documents will be processed if
	 * no explicit document set has been set for the current expression
	 * with fn:doc() or fn:collection().
	 * 
	 * @param docs
	 */
	public void setStaticallyKnownDocuments(String[] docs) {
		staticDocumentPaths = docs;
	}
	
	public void setStaticallyKnownDocuments(DocumentSet set) {
		staticDocuments = set;
	}
	
	/**
	 * Get the set of statically known documents.
	 * 
	 * @return
	 */
	public DocumentSet getStaticallyKnownDocuments() throws XPathException {
		if(staticDocuments != null)
            // the document set has already been built, return it
			return staticDocuments;
		staticDocuments = new DocumentSet();
		if(staticDocumentPaths == null)
            // no path defined: return all documents in the db 
			broker.getAllDocuments(staticDocuments);
		else {
			DocumentImpl doc;
			Collection collection;
			for(int i = 0; i < staticDocumentPaths.length; i++) {
				try {
					doc = broker.openDocument(staticDocumentPaths[i], Lock.READ_LOCK);
					if(doc != null) {
						if(doc.getPermissions().validate(broker.getUser(), Permission.READ)) {
							staticDocuments.add(doc);
						}
						doc.getUpdateLock().release(Lock.READ_LOCK);
						
					} else {
						collection = broker.getCollection(staticDocumentPaths[i]);
						if(collection != null) {
							LOG.debug("reading collection " + staticDocumentPaths[i]);
							collection.allDocs(broker, staticDocuments, true, true);
						}
					}
				} catch(PermissionDeniedException e) {
					LOG.warn("Permission denied to read resource " + staticDocumentPaths[i] + ". Skipping it.");
				}
			}
		}
		return staticDocuments;
	}
	
	/**
	 * Should loaded documents be locked?
	 * 
     * @see #setLockDocumentsOnLoad(boolean)
     * 
	 * @return
	 */
	public boolean lockDocumentsOnLoad() {
	    return lockDocumentsOnLoad;
	}
	
	/**
	 * If lock is true, all documents loaded during query execution
	 * will be locked. This way, we avoid that query results become
	 * invalid before the entire result has been processed by the client
	 * code. All attempts to modify nodes which are part of the result
	 * set will be blocked.
	 * 
	 * However, it is the client's responsibility to proper unlock
	 * all documents once processing is completed.
	 * 
	 * @param lock
	 */
	public void setLockDocumentsOnLoad(boolean lock) {
	    lockDocumentsOnLoad = lock;
	    if(lock)
	        lockedDocuments = new DocumentSet();
	}
	
	/**
	 * Returns the set of documents that have been loaded and
	 * locked during query execution.
	 * 
     * @see #setLockDocumentsOnLoad(boolean)
     * 
	 * @return
	 */
	public DocumentSet getLockedDocuments() {
	    return lockedDocuments;
	}
	
    /**
     * Release all locks on documents that have been locked
     * during query execution.
     *
     *@see #setLockDocumentsOnLoad(boolean)
     */
	public void releaseLockedDocuments() {
	    if(lockedDocuments != null)
	        lockedDocuments.unlock(false);
	    lockDocumentsOnLoad = false;
		lockedDocuments = null;
	}
	
    /**
     * Release all locks on documents not being referenced by the sequence.
     * This is called after query execution has completed. Only locks on those
     * documents contained in the final result set will be preserved. All other
     * locks are released as they are no longer needed.
     * 
     * @param seq
     * @return
     */
	public DocumentSet releaseUnusedDocuments(Sequence seq) {
	    if(lockedDocuments == null)
	        return null;
	    // determine the set of documents referenced by nodes in the sequence
        DocumentSet usedDocs = new DocumentSet();
        for(SequenceIterator i = seq.iterate(); i.hasNext(); ) {
            Item next = i.nextItem();
            if(Type.subTypeOf(next.getType(), Type.NODE)) {
                NodeValue node = (NodeValue) next;
                if(node.getImplementationType() == NodeValue.PERSISTENT_NODE) {
                    DocumentImpl doc = ((NodeProxy)node).getDocument();
                    if(!usedDocs.contains(doc.getDocId()))
	                    usedDocs.add(doc, false);
                }
            }
        }
        DocumentSet remaining = new DocumentSet();
        for(Iterator i = lockedDocuments.iterator(); i.hasNext(); ) {
            DocumentImpl next = (DocumentImpl) i.next();
            if(usedDocs.contains(next.getDocId())) {
               remaining.add(next); 
            } else {
//                LOG.debug("Releasing lock on " + next.getName());
                next.getUpdateLock().release(Lock.READ_LOCK);
            }
        }
//        LOG.debug("Locks remaining: " + remaining.getLength());
        lockDocumentsOnLoad = false;
		lockedDocuments = null;
        return remaining;
    }
	
	/**
	 * Prepare this XQueryContext to be reused. This should be
     * called when adding an XQuery to the cache.
	 */
	public void reset() {
		builder = new MemTreeBuilder(this);
		builder.startDocument();
		staticDocumentPaths = null;
		staticDocuments = null;
		lastVar = null;
		fragmentStack = new Stack();
		watchdog.reset();
        profiler.reset();
		for(Iterator i = modules.values().iterator(); i.hasNext(); ) {
			Module module = (Module)i.next();
			module.reset();
		}
	}
	
	/**
	 * Returns true if whitespace between constructed element nodes
	 * should be stripped by default.
	 * 
	 * @return
	 */
	public boolean stripWhitespace() {
		return stripWhitespace;
	}
	
	public void setStripWhitespace(boolean strip) {
		this.stripWhitespace = strip;
	}

	/**
	 * Return an iterator over all built-in modules currently
	 * registered.
	 * 
	 * @return
	 */
	public Iterator getModules() {
		return modules.values().iterator();
	}

	/**
	 * Get the built-in module registered for the given namespace
	 * URI.
	 * 
	 * @param namespaceURI
	 * @return
	 */
	public Module getModule(String namespaceURI) {
		return (Module) modules.get(namespaceURI);
	}

	/**
	 * For compiled expressions: check if the source of any
	 * module imported by the current query has changed since
	 * compilation.
	 * 
	 * @return
	 */
	public boolean checkModulesValid() {
		for(Iterator i = modules.values().iterator(); i.hasNext(); ) {
			Module module = (Module)i.next();
			if(!module.isInternalModule()) {
				if(!((ExternalModule)module).moduleIsValid()) {
					LOG.debug("Module with URI " + module.getNamespaceURI() + 
							" has changed and needs to be reloaded");
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Load a built-in module from the given class name and assign it to the
	 * namespace URI. The specified class should be a subclass of
	 * {@link Module}. The method will try to instantiate the class. If the
	 * class is not found or an exception is thrown, the method will silently 
	 * fail. The namespace URI has to be equal to the namespace URI declared
	 * by the module class. Otherwise, the module is not loaded.
	 * 
	 * @param namespaceURI
	 * @param moduleClass
	 */
	public Module loadBuiltInModule(String namespaceURI, String moduleClass) {
		Module module = getModule(namespaceURI);
		if (module != null) {
//			LOG.debug("module " + namespaceURI + " is already present");
			return module;
		}
		try {
            Class mClass = (Class) moduleClasses.get(moduleClass);
            if (mClass == null) {
    			mClass = Class.forName(moduleClass);
    			if (!(Module.class.isAssignableFrom(mClass))) {
    				LOG.warn(
    					"failed to load module. "
    						+ moduleClass
    						+ " is not an instance of org.exist.xquery.Module.");
    				return null;
    			}
                moduleClasses.put(moduleClass, mClass);
            }
			module = (Module) mClass.newInstance();
			if (!module.getNamespaceURI().equals(namespaceURI)) {
				LOG.warn("the module declares a different namespace URI. Skipping...");
				return null;
			}
			if (getPrefixForURI(module.getNamespaceURI()) == null
				&& module.getDefaultPrefix().length() > 0)
				declareNamespace(module.getDefaultPrefix(), module.getNamespaceURI());

			modules.put(module.getNamespaceURI(), module);
			//LOG.debug("module " + module.getNamespaceURI() + " loaded successfully.");
		} catch (ClassNotFoundException e) {
			//LOG.warn("module class " + moduleClass + " not found. Skipping...");
		} catch (InstantiationException e) {
			LOG.warn("error while instantiating module class " + moduleClass, e);
		} catch (IllegalAccessException e) {
			LOG.warn("error while instantiating module class " + moduleClass, e);
		}
		return module;
	}

	/**
	 * Declare a user-defined function. All user-defined functions are kept
	 * in a single hash map.
	 * 
	 * @param function
	 * @throws XPathException
	 */
	public void declareFunction(UserDefinedFunction function) throws XPathException {
		declaredFunctions.put(function.getSignature().getFunctionId(), function);
	}

	/**
	 * Resolve a user-defined function.
	 * 
	 * @param name
	 * @return
	 * @throws XPathException
	 */
	public UserDefinedFunction resolveFunction(QName name, int argCount) throws XPathException {
		FunctionId id = new FunctionId(name, argCount);
		UserDefinedFunction func = (UserDefinedFunction) declaredFunctions.get(id);
		return func;
	}
	
	public Iterator getSignaturesForFunction(QName name) {
		ArrayList signatures = new ArrayList(2);
		for (Iterator i = declaredFunctions.values().iterator(); i.hasNext(); ) {
			UserDefinedFunction func = (UserDefinedFunction) i.next();
			if (func.getName().equals(name))
				signatures.add(func.getSignature());
		}
		return signatures.iterator();
	}
	
	public Iterator localFunctions() {
		return declaredFunctions.values().iterator();
	}

	/**
	 * Declare a local variable. This is called by variable binding expressions like
	 * "let" and "for".
	 * 
	 * @param var
	 * @return
	 * @throws XPathException
	 */
	public LocalVariable declareVariable(LocalVariable var) throws XPathException {
		if(lastVar == null)
			lastVar = var;
		else {
			lastVar.addAfter(var);
			lastVar = var;
		}
		var.setStackPosition(variableStackSize);
		return var;
	}

	/**
	 * Declare a global variable as by "declare variable".
	 * 
	 * @param qname
	 * @param value
	 * @return
	 * @throws XPathException
	 */
	public Variable declareGlobalVariable(Variable var) throws XPathException {
		globalVariables.put(var.getQName(), var);
		var.setStackPosition(variableStackSize);
		return var;
	}
	
	/**
	 * Declare a user-defined variable.
	 * 
	 * The value argument is converted into an XPath value
	 * (@see XPathUtil#javaObjectToXPath(Object)).
	 * 
	 * @param qname the qualified name of the new variable. Any namespaces should
	 * have been declared before.
	 * @param value a Java object, representing the fixed value of the variable
	 * @return the created Variable object
	 * @throws XPathException if the value cannot be converted into a known XPath value
	 * or the variable QName references an unknown namespace-prefix. 
	 */
	public Variable declareVariable(String qname, Object value) throws XPathException {
		QName qn = QName.parse(this, qname, null);
		Variable var;
		Module module = getModule(qn.getNamespaceURI());
		if(module != null) {
			var = module.declareVariable(qn, value);
			return var;
		}
		Sequence val = XPathUtil.javaObjectToXPath(value, this);
		var = (Variable)globalVariables.get(qn);
		if(var == null) {
			var = new Variable(qn);
			globalVariables.put(qn, var);
		}
		var.setValue(val);
		return var;
	}

	/**
	 * Try to resolve a variable.
	 * 
	 * @param qname the qualified name of the variable as string
	 * @return the declared Variable object
	 * @throws XPathException if the variable is unknown
	 */
	public Variable resolveVariable(String name) throws XPathException {
		QName qn = QName.parse(this, name, null);
		return resolveVariable(qn);
	}
	
	/**
	 * Try to resolve a variable.
	 * 
	 * @param qname the qualified name of the variable
	 * @return the declared Variable object
	 * @throws XPathException if the variable is unknown
	 */
	public Variable resolveVariable(QName qname) throws XPathException {
		Variable var;
		// first, check if the variable is declared in a module
		Module module = getModule(qname.getNamespaceURI());
		if(module != null) {
			var = module.resolveVariable(qname);
			if(var != null)
				return var;
		}
		var = resolveLocalVariable(qname);
//		var = (Variable) variables.get(qname);
		if (var == null) {
			var = (Variable) globalVariables.get(qname);
		}
		if (var == null)
			throw new XPathException("variable $\"" + qname + "\" is not bound");
		return var;
	}

	private Variable resolveLocalVariable(QName qname) throws XPathException {
		for(LocalVariable var = lastVar; var != null; var = var.before) {
			if(qname.equals(var.getQName()))
				return var;
		}
		return null;
	}
	
	/**
	 * Turn on/off XPath 1.0 backwards compatibility.
	 * 
	 * If turned on, comparison expressions will behave like
	 * in XPath 1.0, i.e. if any one of the operands is a number,
	 * the other operand will be cast to a double.
	 * 
	 * @param backwardsCompatible
	 */
	public void setBackwardsCompatibility(boolean backwardsCompatible) {
		this.backwardsCompatible = backwardsCompatible;
	}

	/**
	 * XPath 1.0 backwards compatibility turned on?
	 * 
	 * In XPath 1.0 compatible mode, additional conversions
	 * will be applied to values if a numeric value is expected.
	 *  
	 * @return
	 */
	public boolean isBackwardsCompatible() {
		return this.backwardsCompatible;
	}

	/**
	 * Get the DBBroker instance used for the current query.
	 * 
	 * The DBBroker is the main database access object, providing
	 * access to all internal database functions.
	 * 
	 * @return
	 */
	public DBBroker getBroker() {
		return broker;
	}

	public void setBroker(DBBroker broker) {
		this.broker = broker;
	}
	
	/**
	 * Get the user which executes the current query.
	 * 
	 * @return
	 */
	public User getUser() {
		return broker.getUser();
	}

	/**
	 * Get the document builder currently used for creating
	 * temporary document fragments. A new document builder
	 * will be created on demand.
	 * 
	 * @return
	 */
	public MemTreeBuilder getDocumentBuilder() {
		if (builder == null) {
			builder = new MemTreeBuilder(this);
			builder.startDocument();
		}
		return builder;
	}
	
	/* Methods delegated to the watchdog */
	
	public void proceed() throws TerminatedException {
	    proceed(null);
	}
	
	public void proceed(Expression expr) throws TerminatedException {
	    watchdog.proceed(expr);
	}
	
	public void proceed(Expression expr, MemTreeBuilder builder) throws TerminatedException {
	    watchdog.proceed(expr, builder);
	}
	
	public void recover() {
	    watchdog.reset();
	    builder = null;
	}
	
	public XQueryWatchDog getWatchDog() {
	    return watchdog;
	}
	
	protected void setWatchDog(XQueryWatchDog watchdog) {
	    this.watchdog = watchdog;
	}
	
	/**
	 * Push any document fragment created within the current
	 * execution context on the stack.
	 */
	public void pushDocumentContext() {
	    fragmentStack.push(builder);
		builder = null;
	}

	public void popDocumentContext() {
		if (!fragmentStack.isEmpty()) {
			builder = (MemTreeBuilder) fragmentStack.pop();
		}
	}

	/**
	 * Set the base URI for the evaluation context.
	 * 
	 * This is the URI returned by the fn:base-uri()
	 * function.
	 * 
	 * @param uri
	 */
	public void setBaseURI(String uri) {
		setBaseURI(uri, false);
	}

    /**
     * Set the base URI for the evaluation context.
     * 
     * A base URI specified via the base-uri directive in the
     * XQuery prolog overwrites any other setting.
     * 
     * @param uri
     * @param setInProlog
     */
    public void setBaseURI(String uri, boolean setInProlog) {
        if (baseURISetInProlog)
            return;
        if (uri == null)
            baseURI = "";
        baseURI = uri;
        baseURISetInProlog = setInProlog;
    }
	/**
	 * Set the path to a base directory where modules should
	 * be loaded from. Relative module paths will be resolved against
	 * this directory. The property is usually set by the XQueryServlet or
	 * XQueryGenerator, but can also be specified manually. 
	 * 
	 * @param path
	 */
	public void setModuleLoadPath(String path) {
		this.moduleLoadPath = path;
	}
	
	public String getModuleLoadPath() {
		return moduleLoadPath;
	}
	
	/**
	 * Get the base URI of the evaluation context.
	 * 
	 * This is the URI returned by the fn:base-uri() function.
	 * 
	 * @return
	 */
	public String getBaseURI() {
		return baseURI;
	}
    
	/**
	 * Set the current context position, i.e. the position
	 * of the currently processed item in the context sequence.
	 * This value is required by some expressions, e.g. fn:position().
	 * 
	 * @param pos
	 */
	public void setContextPosition(int pos) {
		contextPosition = pos;
	}

	/**
	 * Get the current context position, i.e. the position of
	 * the currently processed item in the context sequence.
	 *  
	 * @return
	 */
	public int getContextPosition() {
		return contextPosition;
	}

	/**
	 * Push all in-scope namespace declarations onto the stack.
	 */
	public void pushInScopeNamespaces() {
		HashMap m = (HashMap) inScopeNamespaces.clone();
		namespaceStack.push(inScopeNamespaces);
		inScopeNamespaces = m;
	}

	public void popInScopeNamespaces() {
		inScopeNamespaces = (HashMap) namespaceStack.pop();
	}

	public void pushNamespaceContext() {
		HashMap m = (HashMap) namespaces.clone();
		HashMap p = (HashMap) prefixes.clone();
		namespaceStack.push(namespaces);
		namespaceStack.push(prefixes);
		namespaces = m;
		prefixes = p;
	}
	
	public void popNamespaceContext() {
		prefixes = (HashMap) namespaceStack.pop();
		namespaces = (HashMap) namespaceStack.pop();
	}
	
	/**
	 * Returns the last variable on the local variable stack.
	 * The current variable context can be restored by passing
	 * the return value to {@link #popLocalVariables(LocalVariable)}.
	 * 
	 * @return
	 */
	public LocalVariable markLocalVariables() {
		variableStackSize++;
		return lastVar;
	}
	
	/**
	 * Restore the local variable stack to the position marked
	 * by variable var.
	 * 
	 * @param var
	 */
	public void popLocalVariables(LocalVariable var) {
		if(var != null)
			var.after = null;
		lastVar = var;
		variableStackSize--;
	}

	/**
	 * Returns the current size of the stack. This is used to determine
	 * where a variable has been declared.
	 * 
	 * @return
	 */
	public int getCurrentStackSize() {
		return variableStackSize;
	}

	/**
	 * Import a module and make it available in this context. The prefix and
	 * location parameters are optional. If prefix is null, the default prefix specified
	 * by the module is used. If location is null, the module will be read from the
	 * namespace URI.
	 * 
	 * @param namespaceURI
	 * @param prefix
	 * @param location
	 * @throws XPathException
	 */
	public void importModule(String namespaceURI, String prefix, String location)
		throws XPathException {
		Module module = getModule(namespaceURI);
		if(module != null) {
			LOG.debug("Module " + namespaceURI + " already present.");
		} else {
			if(location == null)
				location = namespaceURI;
			// is it a Java module?
			if(location.startsWith(JAVA_URI_START)) {
				location = location.substring(JAVA_URI_START.length());
				module = loadBuiltInModule(namespaceURI, location);
			} else {
				Source source;
                // Is the module source stored in the database?
                if (location.startsWith(XMLDB_URI_START) || moduleLoadPath.startsWith(XMLDB_URI_START)) {
                    if (location.indexOf(':') < 0)
                        location = moduleLoadPath + '/' + location;
                    String path = location.substring(XMLDB_URI_START.length());
                    DocumentImpl sourceDoc = null;
                    try {
                        sourceDoc = broker.openDocument(path, Lock.READ_LOCK);
                        if (sourceDoc == null)
                            throw new XPathException("source for module " + location + " not found in database");
                        if (sourceDoc.getResourceType() != DocumentImpl.BINARY_FILE ||
                                !sourceDoc.getMimeType().equals("application/xquery"))
                            throw new XPathException("source for module " + location + " is not an XQuery or " +
                            "declares a wrong mime-type");
                        source = new DBSource(broker, (BinaryDocument) sourceDoc, true);
                        module = compileModule(namespaceURI, location, module, source);
                    } catch (PermissionDeniedException e) {
                        throw new XPathException("permission denied to read module source from " + location);
                    } finally {
                        if(sourceDoc != null)
                            sourceDoc.getUpdateLock().release(Lock.READ_LOCK);
                    }
                // No. Load from file or URL
                } else {
                    try {
                        source = SourceFactory.getSource(moduleLoadPath, location, true);
                    } catch (MalformedURLException e) {
                        throw new XPathException("source location for module " + namespaceURI + " should be a valid URL: " +
                                e.getMessage());
                    } catch (IOException e) {
                        throw new XPathException("source for module " + namespaceURI + " not found: " +
                                e.getMessage());
                    }
                    module = compileModule(namespaceURI, location, module, source);
                }
			}
		}
		if(prefix == null)
			prefix = module.getDefaultPrefix();
		declareNamespace(prefix, namespaceURI);
	}

    /**
     * @param namespaceURI
     * @param location
     * @param module
     * @param source
     * @return
     * @throws XPathException
     */
    private Module compileModule(String namespaceURI, String location, Module module, Source source) throws XPathException {
        LOG.debug("Loading module from " + location);
        Reader reader;
        try {
        	reader = source.getReader();
        } catch (IOException e) {
        	throw new XPathException("IO exception while loading module " + namespaceURI, e);
        }
        XQueryContext modContext = new ModuleContext(this);
        XQueryLexer lexer = new XQueryLexer(modContext, reader);
        XQueryParser parser = new XQueryParser(lexer);
        XQueryTreeParser astParser = new XQueryTreeParser(modContext);
        try {
        	parser.xpath();
        	if (parser.foundErrors()) {
        		LOG.debug(parser.getErrorMessage());
        		throw new XPathException(
        			"error found while loading module from " + location + ": "
        				+ parser.getErrorMessage());
        	}
        	AST ast = parser.getAST();

        	PathExpr path = new PathExpr(modContext);
        	astParser.xpath(ast, path);
        	if (astParser.foundErrors()) {
        		throw new XPathException(
        			"error found while loading module from " + location + ": "
        				+ astParser.getErrorMessage(),
        			astParser.getLastException());
        	}
        	path.analyze(null, 0);
        	ExternalModule modExternal = astParser.getModule();
        	if(modExternal == null)
        		throw new XPathException("source at " + location + " is not a valid module");
        	if(!modExternal.getNamespaceURI().equals(namespaceURI))
        		throw new XPathException("namespace URI declared by module (" + modExternal.getNamespaceURI() + 
        			") does not match namespace URI in import statement, which was: " + namespaceURI);
        	modules.put(modExternal.getNamespaceURI(), modExternal);
        	modExternal.setSource(source);
        	modExternal.setContext(modContext);
        	module = modExternal;
        } catch (RecognitionException e) {
        	throw new XPathException(
        		"error found while loading module from " + location + ": " + e.getMessage(),
        		e.getLine(), e.getColumn());
        } catch (TokenStreamException e) {
        	throw new XPathException(
        		"error found while loading module from " + location + ": " + e.getMessage(),
        		e);
        }
        declareModuleVars(module);
        return module;
    }

    private void declareModuleVars(Module module) {
        String moduleNS = module.getNamespaceURI();
        for (Iterator i = globalVariables.values().iterator(); i.hasNext(); ) {
            Variable var = (Variable) i.next();
            if (moduleNS.equals(var.getQName().getNamespaceURI())) {
                module.declareVariable(var);
				i.remove();
            }
        }
    }
    
	/**
	 * Add a forward reference to an undeclared function. Forward
	 * references will be resolved later.
	 * 
	 * @param call
	 */
	public void addForwardReference(FunctionCall call) {
		forwardReferences.push(call);
	}
	
	/**
	 * Resolve all forward references to previously undeclared functions.
	 * 
	 * @throws XPathException
	 */
	public void resolveForwardReferences() throws XPathException {
		while(!forwardReferences.empty()) {
			FunctionCall call = (FunctionCall)forwardReferences.pop();
			UserDefinedFunction func = resolveFunction(call.getQName(), call.getArgumentCount());
			if(func == null)
				throw new XPathException(call.getASTNode(), 
					"Call to undeclared function: " + call.getQName().toString());
			call.resolveForwardReference(func);
		}
	}
	
	/**
	 * Called by XUpdate to tell the query engine that it is running in
	 * exclusive mode, i.e. no other query is executed at the same time.
	 * 
	 * @param exclusive
	 */
	public void setExclusiveMode(boolean exclusive) {
	    this.exclusive = exclusive;
	}
	
	public boolean inExclusiveMode() {
	    return exclusive;
	}
	
	public void addPragma(String qnameString, String contents) throws XPathException {
		QName qn;
		try {
			qn = QName.parse(this, qnameString, defaultFunctionNamespace);
		} catch (XPathException e) {
			// unknown pragma: just ignore it
			LOG.debug("Ignoring unknown pragma: " + qnameString);
			return;
		}
		Pragma pragma = new Pragma(qn, contents);
		if(pragmas == null)
			pragmas = new ArrayList();
		pragmas.add(pragma);
		
		// check predefined pragmas
        if (Pragma.PROFILE_QNAME.compareTo(qn) == 0) {
            // configure profiling
            profiler.configure(pragma);
            profile = profiler.isEnabled();
        } else if(Pragma.TIMEOUT_QNAME.compareTo(qn) == 0)
			watchdog.setTimeoutFromPragma(pragma);
        else if(Pragma.OUTPUT_SIZE_QNAME.compareTo(qn) == 0)
			watchdog.setMaxNodesFromPragma(pragma);
	}
	
	public Pragma getPragma(QName qname) {
		if(pragmas != null) {
			Pragma pragma;
			for(int i = 0; i < pragmas.size(); i++) {
				pragma = (Pragma)pragmas.get(i);
				if(qname.compareTo(pragma.getQName()) == 0)
					return pragma;
			}
		}
		return null;
	}
	
	/**
	 * Store the supplied data to a temporary document fragment.
	 * 
	 * @param data
	 * @return
	 * @throws XPathException
	 */
	public DocumentImpl storeTemporaryDoc(org.exist.memtree.DocumentImpl doc) throws XPathException {
		try {
			DocumentImpl targetDoc = broker.storeTemporaryDoc(doc);
			watchdog.addTemporaryFragment(targetDoc.getFileName());
            LOG.debug("Stored: " + targetDoc.getDocId() + ": " + targetDoc.getName() +
            		": " + targetDoc.printTreeLevelOrder());
			return targetDoc;
		} catch (EXistException e) {
			throw new XPathException(TEMP_STORE_ERROR, e);
		} catch (PermissionDeniedException e) {
			throw new XPathException(TEMP_STORE_ERROR, e);
		} catch (LockException e) {
			throw new XPathException(TEMP_STORE_ERROR, e);
		}
	}
    
	/**
	 * Load the default prefix/namespace mappings table and set up
	 * internal functions.
	 */
	protected void loadDefaults(Configuration config) {
		this.watchdog = new XQueryWatchDog(this);
		SymbolTable syms = broker.getSymbols();
		String[] pfx = syms.defaultPrefixList();
		namespaces = new HashMap(pfx.length);
		prefixes = new HashMap(pfx.length);
		String sym;
		for (int i = 0; i < pfx.length; i++) {
			sym = syms.getDefaultNamespace(pfx[i]);
			namespaces.put(pfx[i], sym);
			prefixes.put(sym, pfx[i]);
		}

		// default namespaces
		declareNamespace("xml", XML_NS);
		declareNamespace("xs", SCHEMA_NS);
		declareNamespace("xdt", XPATH_DATATYPES_NS);
		declareNamespace("local", XQUERY_LOCAL_NS);
		declareNamespace("fn", Module.BUILTIN_FUNCTION_NS);
		declareNamespace("exist", EXIST_NS);

		// load built-in modules
		
		// these modules are loaded dynamically. It is not an error if the
		// specified module class cannot be found in the classpath.
		loadBuiltInModule(
			Module.BUILTIN_FUNCTION_NS,
			"org.exist.xquery.functions.ModuleImpl");
		
		String modules[][] = (String[][]) config.getProperty("xquery.modules");
		if ( modules != null ) {
			for (int i = 0; i < modules.length; i++) {
//				LOG.debug("Loading module " + modules[i][0]);
				loadBuiltInModule(modules[i][0], modules[i][1]);
			}
		}
	}
}
