
package org.exist.xmldb;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.exist.source.Source;
import org.exist.xmlrpc.RpcAPI;
import org.exist.xquery.XPathException;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;


public class RemoteXPathQueryService implements XPathQueryServiceImpl, XQueryService {

    protected RemoteCollection collection;
	protected Hashtable namespaceMappings = new Hashtable(5);
	protected Hashtable variableDecls = new Hashtable();
	protected Properties outputProperties = null;
	
    public RemoteXPathQueryService( RemoteCollection collection ) {
        this.collection = collection;
        this.outputProperties = new Properties(collection.properties);
    }

	public ResourceSet query( String query ) throws XMLDBException {
		return query(query, null);
	}
	
    public ResourceSet query( String query, String sortExpr ) throws XMLDBException {
        try {
        	Hashtable optParams = new Hashtable();
            if(sortExpr != null)
            	optParams.put(RpcAPI.SORT_EXPR, sortExpr);
            if(namespaceMappings.size() > 0)
            	optParams.put(RpcAPI.NAMESPACES, namespaceMappings);
            if(variableDecls.size() > 0)
            	optParams.put(RpcAPI.VARIABLES, variableDecls);
            optParams.put(RpcAPI.BASE_URI, collection.getPath());
			Vector params = new Vector();
			params.addElement(query.getBytes("UTF-8"));
			params.addElement(optParams);
            Hashtable result = (Hashtable) collection.getClient().execute( "queryP", params );
            
            if(result.get(RpcAPI.ERROR) != null)
            	throwException(result);
            
            Vector resources = (Vector)result.get("results");
            int handle = -1;
            if(resources != null && resources.size() > 0)
            	handle = ((Integer)result.get("id")).intValue();
            return new RemoteResourceSet( collection, outputProperties, resources, handle );
        } catch ( XmlRpcException xre ) {
            throw new XMLDBException( ErrorCodes.VENDOR_ERROR, xre.getMessage(), xre );
        } catch ( IOException ioe ) {
            throw new XMLDBException( ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe );
        }
    }

    /**
	 * @param result
	 */
	private void throwException(Hashtable result) throws XMLDBException {
		String message = (String)result.get(RpcAPI.ERROR);
		Integer lineInt = (Integer)result.get(RpcAPI.LINE);
		Integer columnInt = (Integer)result.get(RpcAPI.COLUMN);
		int line = lineInt == null ? 0 : lineInt.intValue();
		int column = columnInt == null ? 0 : columnInt.intValue();
		XPathException cause = new XPathException(message, line, column);
		throw new XMLDBException(ErrorCodes.VENDOR_ERROR, message, cause);
	}

	/* (non-Javadoc)
     * @see org.exist.xmldb.XQueryService#execute(org.exist.source.Source)
     */
    public ResourceSet execute(Source source) throws XMLDBException {
        try {
            String xq = source.getContent();
            return query(xq, null);
        } catch (IOException e) {
            throw new XMLDBException( ErrorCodes.VENDOR_ERROR, e.getMessage(), e );
        }
    }
    
	public ResourceSet query( XMLResource res, String query )
		throws XMLDBException {
			return query(res, query, null);
	}

    public ResourceSet query( XMLResource res, String query, String sortExpr )
        throws XMLDBException {
        RemoteXMLResource resource = (RemoteXMLResource)res;
        try {
        	Hashtable optParams = new Hashtable();
        	if(namespaceMappings.size() > 0)
            	optParams.put(RpcAPI.NAMESPACES, namespaceMappings);
            if(variableDecls.size() > 0)
            	optParams.put(RpcAPI.VARIABLES, variableDecls);
        	if(sortExpr != null)
        		optParams.put(RpcAPI.SORT_EXPR, sortExpr);
			optParams.put(RpcAPI.BASE_URI, collection.getPath());
            Vector params = new Vector();
            params.addElement( query.getBytes("UTF-8") );
            params.addElement( resource.path );
            if(resource.id == null)
            	params.addElement("");
            else
            	params.addElement( resource.id );
            params.addElement( optParams );
			Hashtable result = (Hashtable) collection.getClient().execute( "queryP", params );
			
			if(result.get(RpcAPI.ERROR) != null)
            	throwException(result);
			
			Vector resources = (Vector)result.get("results");
			int handle = -1;
			if(resources != null && resources.size() > 0)
				handle = ((Integer)result.get("id")).intValue();
			return new RemoteResourceSet( collection, outputProperties, resources, handle );
        } catch ( XmlRpcException xre ) {
            throw new XMLDBException( ErrorCodes.VENDOR_ERROR, xre.getMessage(), xre );
        } catch ( IOException ioe ) {
            throw new XMLDBException( ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe );
        }
    }
    
    public ResourceSet queryResource( String resource, String query ) throws XMLDBException {
        return query( query );
    }

    public String getVersion() throws XMLDBException {
        return "1.0";
    }

    public void setCollection( Collection col ) throws XMLDBException {
    }

    public String getName() throws XMLDBException {
        return "XPathQueryService";
    }

    public String getProperty( String property ) throws XMLDBException {
    	return outputProperties.getProperty(property);
    }

    public void setProperty( String property, String value ) throws XMLDBException {
        outputProperties.setProperty(property, value);
    }

    public void clearNamespaces() throws XMLDBException {
    	namespaceMappings.clear();
    }

    public void removeNamespace( String ns ) throws XMLDBException {
        for(Iterator i = namespaceMappings.values().iterator(); i.hasNext(); ) {
        	if(((String)i.next()).equals(ns))
        		i.remove();
        }
    }

    public void setNamespace( String prefix, String namespace )
             throws XMLDBException {
        namespaceMappings.put(prefix, namespace);
    }

    public String getNamespace( String prefix ) throws XMLDBException {
        return (String)namespaceMappings.get(prefix);
    }

	/* (non-Javadoc)
	 * @see org.exist.xmldb.XPathQueryServiceImpl#declareVariable(java.lang.String, java.lang.Object)
	 */
	public void declareVariable(String qname, Object initialValue) throws XMLDBException {
		variableDecls.put(qname, initialValue);
	}

	/**
	 * The XML-RPC server automatically caches compiled queries.
	 * Thus calling this method has no effect.
	 * 
	 * @see org.exist.xmldb.XQueryService#compile(java.lang.String)
	 */
	public CompiledExpression compile(String query) throws XMLDBException {
		return new RemoteCompiledExpression(query);
	}
    
	/* (non-Javadoc)
	 * @see org.exist.xmldb.XQueryService#execute(org.exist.xmldb.CompiledExpression)
	 */
	public ResourceSet execute(CompiledExpression expression) throws XMLDBException {
		return query(((RemoteCompiledExpression)expression).getQuery());
	}

	/* (non-Javadoc)
	 * @see org.exist.xmldb.XQueryService#execute(org.xmldb.api.modules.XMLResource, org.exist.xmldb.CompiledExpression)
	 */
	public ResourceSet execute(XMLResource res, CompiledExpression expression)
			throws XMLDBException {
		return query(res, ((RemoteCompiledExpression)expression).getQuery());
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xmldb.XQueryService#setXPathCompatibility(boolean)
	 */
	public void setXPathCompatibility(boolean backwardsCompatible) {
		// TODO: not passed
	}

	/** 
	 * Calling this method has no effect. The server loads modules
	 * relative to its own context.
	 * 
	 * @see org.exist.xmldb.XQueryService#setModuleLoadPath(java.lang.String)
	 */
	public void setModuleLoadPath(String path) {		
	}
}

