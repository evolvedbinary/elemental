/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
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
package org.exist.storage;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.exist.util.DatabaseConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Top class for index definitions as specified in a collection configuration
 * or the main configuration file. The IndexSpec for a given collection can be retrieved through method
 * {@link org.exist.collections.Collection#getIdxConf(DBBroker)}.
 *  
 *  An index definition should have the following structure:
 *  
 *  <pre>
 *  &lt;index index-depth="idx-depth"&gt;
 *      &lt;fulltext default="all|none" attributes="true|false"&gt;
 *          &lt;include path="node-path"/&gt;
 *          &lt;exclude path="node-path"/&gt;
 *      &lt;/fulltext&gt;
 *      &lt;create path="node-path" type="schema-type"&gt;
 *  &lt;/index&gt;
 *  </pre>
 *  
 * @author wolf
 */
public class IndexSpec {

    private static final String TYPE_ATTRIB = "type";
    private static final String PATH_ATTRIB = "path";
    private static final String CREATE_ELEMENT = "create";
    private static final String FULLTEXT_ELEMENT = "fulltext";
    private static final String INDEX_DEPTH_ATTRIB = "index-depth";
    
    private final static Logger LOG = Logger.getLogger(IndexSpec.class);
    
    private FulltextIndexSpec ftSpec = null;
    private ValueIndexSpec specs[] = null;
    
    protected int depth = 1;
    
    public IndexSpec(Element index) throws DatabaseConfigurationException {
        read(index);
    }
    
    /**
     * Read index configurations from an "index" element node. The node should have
     * exactly one "fulltext" child node and zero or more "create" nodes. The "fulltext"
     * section  is forwarded to class {@link FulltextIndexSpec}. The "create" elements
     * add a {@link ValueIndexSpec} to the current configuration.
     *  
     * @param index
     * @param namespaces
     * @throws DatabaseConfigurationException
     */
    public void read(Element index) throws DatabaseConfigurationException {
        Map namespaces = getNamespaceMap(index);
        String indexDepth = index.getAttribute(INDEX_DEPTH_ATTRIB);
		if (indexDepth != null && indexDepth.length() > 0)
			try {
				int depth = Integer.parseInt(indexDepth);
				setIndexDepth(depth);
			} catch (NumberFormatException e) {
			}
			
        NodeList cl = index.getChildNodes();
        for(int i = 0; i < cl.getLength(); i++) {
            Node node = cl.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
	            if(FULLTEXT_ELEMENT.equals(node.getLocalName())) {
//	                if(ftSpec != null)
//	                    throw new DatabaseConfigurationException("Only one fulltext section is allowed per index");
	                ftSpec = new FulltextIndexSpec(namespaces, (Element)node);
	            } else if(CREATE_ELEMENT.equals(node.getLocalName())) {
	                Element elem = (Element) node;
	                String path = elem.getAttribute(PATH_ATTRIB);
	                String type = elem.getAttribute(TYPE_ATTRIB);
	                ValueIndexSpec valueIdx = new ValueIndexSpec(namespaces, path, type);
	                addValueIndex(valueIdx);
	            }
            }
        }
    }

    /**
     * Returns the current index depth, i.e. the level in the tree up to which
     * node ids are added to the B+-tree in the main dom.dbx. Nodes below
     * the current index depth are not added. The main B+-tree is only required when
     * retrieving nodes for display. Usually, it is not necessary to add all node levels
     * there. Nodes in lower levels of the tree can be retrieved via their parent
     * nodes.
     * 
     * @return
     */
    public int getIndexDepth() {
		return depth;
	}
	
    /**
     * Set the current index depth {@see #getIndexDepth()}.
     * 
     * @param depth
     */
	public void setIndexDepth( int depth ) {
		this.depth = depth;
	}
	
    /**
     * Returns the fulltext index configuration object for the current
     * configuration.
     * 
     * @return
     */
    public FulltextIndexSpec getFulltextIndexSpec() {
        return ftSpec;
    }
    
    /**
     * Returns the {@link ValueIndexSpec} defined for the given
     * node path or null if no index has been configured.
     * 
     * @param path
     * @return
     */
    public ValueIndexSpec getIndexByPath(NodePath path) {
        if(specs != null) {
	        for(int i = 0; i < specs.length; i++) {
	            if(specs[i].matches(path))
	                return specs[i];
	        }
        }
        return null;
    }
    
    /**
     * Add a {@link ValueIndexSpec}.
     * 
     * @param valueIdx
     */
    private void addValueIndex(ValueIndexSpec valueIdx) {
        if(specs == null) {
            specs = new ValueIndexSpec[1];
            specs[0] = valueIdx;
        } else {
            ValueIndexSpec nspecs[] = new ValueIndexSpec[specs.length + 1];
            System.arraycopy(specs, 0, nspecs, 0, specs.length);
            nspecs[specs.length] = valueIdx;
            specs = nspecs;
        }
    }
    
    /**
     * Returns a map containing all prefix/namespace mappings declared in
     * the index element.
     * 
     * @param elem
     * @return
     */
    private Map getNamespaceMap(Element elem) {
        HashMap map = new HashMap();
        NamedNodeMap attrs = elem.getAttributes();
        for(int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            LOG.debug(attr.getName());
            if(attr.getPrefix() != null && attr.getPrefix().equals("xmlns")) {
                map.put(attr.getLocalName(), attr.getValue());
            }
        }
        return map;
    }
}
