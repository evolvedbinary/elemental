package org.exist.xmldb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.w3c.dom.DocumentFragment;

import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.apache.xmlrpc.XmlRpcException;
import org.exist.security.Permission;
import org.exist.util.Compressor;
import org.exist.util.serializer.DOMSerializer;
import org.exist.util.serializer.SAXSerializer;
import org.exist.storage.serializers.EXistOutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

public class RemoteXMLResource implements XMLResource, EXistResource {
	
    private final static Properties emptyProperties = new Properties();
	
    /**
     *  Use external XMLReader to parse XML.
     */
    private XMLReader xmlReader = null;
	
    protected String id;
    protected String documentName;
    protected String path = null ;
    protected int handle = -1;
    protected int pos = -1;
    protected RemoteCollection parent;
    protected String content = null;
    protected File file = null;
	
    protected Permission permissions = null;
    protected int contentLen = 0;
	
    protected Properties outputProperties = null;
    protected LexicalHandler lexicalHandler = null;
	
    protected Date datecreated= null;
    protected Date datemodified= null;
	
    public RemoteXMLResource(RemoteCollection parent, String docId, String id)
	throws XMLDBException {
	this(parent, -1, -1, docId, id);
    }

    public RemoteXMLResource(
			     RemoteCollection parent,
			     int handle,
			     int pos,
			     String docId,
			     String id)
	throws XMLDBException {
	this.handle = handle;
	this.pos = pos;
	this.parent = parent;
	this.id = id;
	int p;
	if (docId != null && (p = docId.lastIndexOf('/')) > -1) {
	    path = docId;
	    documentName = docId.substring(p + 1);
	} else {
	    path = parent.getPath() + '/' + docId;
	    documentName = docId;
	}
    }

    public Date getCreationTime() throws XMLDBException {
	Vector params = new Vector(1);
	params.addElement(path);
	try {
	    return (Date) ((Vector) parent.getClient().execute("getTimestamps", params)).get(0);
	} catch (XmlRpcException e) {
	    throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
	} catch (IOException e) {
	    throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
	}
    }

    public Date getLastModificationTime() throws XMLDBException {
	Vector params = new Vector(1);
	params.addElement(path);
	try {
	    return (Date) ((Vector) parent.getClient().execute("getTimestamps", params)).get(1);
	} catch (XmlRpcException e) {
	    throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
	} catch (IOException e) {
	    throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
	}
    }

    public Object getContent() throws XMLDBException {
	if (content != null) {
	    return content;
	}
	if (file != null) {
	    return file;
	}
	Properties properties = parent.getProperties();
	byte[] data = null;
	if (id == null) {
	    Vector params = new Vector();
	    params.addElement(path);
	    params.addElement(properties);
	    try {
		Hashtable table = (Hashtable) parent.getClient().execute("getDocumentData", params);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		int offset = ((Integer)table.get("offset")).intValue();
		data = (byte[])table.get("data");
		os.write(data);
		while(offset > 0) {
		    params.clear();
		    params.addElement(table.get("handle"));
		    params.addElement(new Integer(offset));
		    table = (Hashtable) parent.getClient().execute("getNextChunk", params);
		    offset = ((Integer)table.get("offset")).intValue();
		    data = (byte[])table.get("data");
		    os.write(data);
		}
		data = os.toByteArray();
	    } catch (XmlRpcException xre) {
		throw new XMLDBException(ErrorCodes.INVALID_RESOURCE, xre.getMessage(), xre);
	    } catch (IOException ioe) {
		throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
	    }
	} else {
	    Vector params = new Vector();
	    params.addElement(new Integer(handle));
	    params.addElement(new Integer(pos));
	    params.addElement(properties);
	    try {
		data = (byte[]) parent.getClient().execute("retrieve", params);
	    } catch (XmlRpcException xre) {
		throw new XMLDBException(ErrorCodes.INVALID_RESOURCE, xre.getMessage(), xre);
	    } catch (IOException ioe) {
		throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
	    }
	}

	if (properties.getProperty(EXistOutputKeys.COMPRESS_OUTPUT, "no").equals("yes")) {
	    try {
		data = Compressor.uncompress(data);
	    } catch (IOException e) {

	    }


	}


	try {
	    content = new String(data, properties.getProperty(OutputKeys.ENCODING, "UTF-8"));
	} catch (UnsupportedEncodingException ue) {
	    content = new String(data);
	}
	return content;
    }

    public Node getContentAsDOM() throws XMLDBException {
	if (content == null)
	    getContent();
	// content can be a file
	if (file != null)
	    getData();
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    factory.setValidating(false);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document doc = builder.parse(new InputSource(new StringReader(content)));
	    return doc.getDocumentElement();
	} catch (SAXException saxe) {
	    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, saxe.getMessage(), saxe);
	} catch (ParserConfigurationException pce) {
	    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, pce.getMessage(), pce);
	} catch (IOException ioe) {
	    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
	}
    }

    public void getContentAsSAX(ContentHandler handler) throws XMLDBException {
	if (content == null)
	    getContent();
	//		content can be a file
	if (file != null)
	    getData();
        
        XMLReader reader = null;
	if (xmlReader == null) {
	    SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	    saxFactory.setNamespaceAware(true);
	    saxFactory.setValidating(false);
            try {
                SAXParser sax = saxFactory.newSAXParser();
                reader = sax.getXMLReader();
            } catch (ParserConfigurationException pce) {
                throw new XMLDBException(ErrorCodes.VENDOR_ERROR, pce.getMessage(), pce);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
                throw new XMLDBException(ErrorCodes.VENDOR_ERROR, saxe.getMessage(), saxe);
            }
        } else {
            reader = xmlReader;
        }
	try {
	    reader.setContentHandler(handler);
	    if(lexicalHandler != null) {
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);
            }
	    reader.parse(new InputSource(new StringReader(content)));
        } catch (SAXException saxe) {
            saxe.printStackTrace();
            throw new XMLDBException(ErrorCodes.VENDOR_ERROR, saxe.getMessage(), saxe);
        } catch (IOException ioe) {
            throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
        }
    }

    public String getDocumentId() throws XMLDBException {
	return documentName;
    }

    public String getId() throws XMLDBException {
	if (id == null || id.equals("1")) 
	    return documentName; 
	return documentName + '_' + id;
    }

    public Collection getParentCollection() throws XMLDBException {
	return parent;
    }

    public String getResourceType() throws XMLDBException {
	return "XMLResource";
    }

    /**
     * Sets the external XMLReader to use.
     *
     * @param xmlReader the XMLReader
     */
    public void setXMLReader(XMLReader xmlReader) {
	this.xmlReader = xmlReader;
    }

    public void setContent(Object value) throws XMLDBException {
	if (value instanceof File) {
	    file = (File) value;
	} else
	    content = value.toString();
    }

    public void setContentAsDOM(Node root) throws XMLDBException {
	StringWriter sout = new StringWriter();
	DOMSerializer xmlout = new DOMSerializer(sout, getProperties());
	try {
	    switch (root.getNodeType()) {
	    case Node.ELEMENT_NODE :
		xmlout.serialize((Element) root);
		break;
	    case Node.DOCUMENT_FRAGMENT_NODE :
		xmlout.serialize((DocumentFragment) root);
		break;
	    case Node.DOCUMENT_NODE :
		xmlout.serialize((Document) root);
		break;
	    default :
		throw new XMLDBException(ErrorCodes.VENDOR_ERROR, "invalid node type");
	    }
	    content = sout.toString();
	} catch (TransformerException e) {
	    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
	}
    }

    public ContentHandler setContentAsSAX() throws XMLDBException {
	return new InternalXMLSerializer();
    }

    private class InternalXMLSerializer extends SAXSerializer {

	StringWriter writer = new StringWriter();

	public InternalXMLSerializer() {
	    super();
	    setOutput(writer, emptyProperties);
	}

	/**
	 * @see org.xml.sax.DocumentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
	    super.endDocument();
	    content = writer.toString();
	}
    }

    /* (non-Javadoc)
     * @see org.xmldb.api.modules.XMLResource#getSAXFeature(java.lang.String)
     */
    public boolean getSAXFeature(String arg0)
	throws SAXNotRecognizedException, SAXNotSupportedException {
	return false;
    }

    /* (non-Javadoc)
     * @see org.xmldb.api.modules.XMLResource#setSAXFeature(java.lang.String, boolean)
     */
    public void setSAXFeature(String arg0, boolean arg1)
	throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    /**
     * Force content to be loaded into mem
     * 
     * @throws XMLDBException
     */
    protected byte[] getData() throws XMLDBException {
	if (file != null) {
	    if (!file.canRead())
		throw new XMLDBException(
					 ErrorCodes.INVALID_RESOURCE,
					 "failed to read resource content from file " + file.getAbsolutePath());
	    try {
		final byte[] chunk = new byte[512];
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final FileInputStream in = new FileInputStream(file);
		int l;
		do {
		    l = in.read(chunk);
		    if (l > 0)
			out.write(chunk, 0, l);

		} while (l > -1);
		in.close();
		final byte[] data = out.toByteArray();
		//				content = new String(data);
		file = null;
		return data;
	    } catch (IOException e) {
		throw new XMLDBException(
					 ErrorCodes.INVALID_RESOURCE,
					 "failed to read resource content from file " + file.getAbsolutePath(),
					 e);
	    }
	} else if(content != null)
	    try {
		return content.getBytes("UTF-8");
	    } catch (UnsupportedEncodingException e) {
	    }
	return null;
    } 

    public void setContentLength(int len) {
	this.contentLen = len;
    }
	
    public int getContentLength() throws XMLDBException {
	return contentLen;
    }
	
    public void setPermissions(Permission perms) {
	permissions = perms;
    }

    public Permission getPermissions() {
	return permissions;
    }
	
    public void setLexicalHandler(LexicalHandler handler) {
	lexicalHandler = handler;
    }
	
    protected void setProperties(Properties properties) {
	this.outputProperties = properties;
    }
	
    private Properties getProperties() {
	return outputProperties == null ? parent.properties : outputProperties;
    }

    /* (non-Javadoc)
     * @see org.exist.xmldb.EXistResource#setMimeType(java.lang.String)
     */
    public void setMimeType(String mime) {
    }

    /* (non-Javadoc)
     * @see org.exist.xmldb.EXistResource#getMimeType()
     */
    public String getMimeType() {
        return "text/xml";
    }
}
