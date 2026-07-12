/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
package org.exist.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.exist.collections.CollectionConfiguration;
import org.exist.collections.CollectionConfigurationManager;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.exist.xmldb.EXistResource;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.XMLResource;

/**
 * Class to represent a collection.xconf which holds the configuration data for a collection
 * 
 * @author <a href="mailto:adam.retter@devon.gov.uk">Adam Retter</a>
 * @serial 2006-08-25
 * @version 1.2
 */
public class CollectionXConf {

    public static final String TYPE_QNAME = "qname";
    public static final String TYPE_PATH = "path";

    private final InteractiveClient client;	//the client
	private final String path;				//path of the collection.xconf file
	private final boolean collectionExists;
	private final boolean resourceExists;


	private @Nullable LinkedHashMap<String, String> customNamespaces = null;		//custom namespaces
	private @Nullable RangeIndex[] rangeIndexes = null;			//range indexes model
	private @Nullable Trigger[] triggers = null;					//triggers model
	
	private boolean hasChanged = false;	//indicates if changes have been made to the current collection configuration
	
	
	/**
	 * Constructor
	 * 
	 * @param collectionName The path of the collection to retrieve the collection.xconf for
	 * @param client The interactive client
	 */
	CollectionXConf(final String collectionName, final InteractiveClient client) throws XMLDBException {
		this.client = client;
		
		// get configuration collection for the named collection
		this.path = CollectionConfigurationManager.CONFIG_COLLECTION + collectionName;

		try (final Collection collection = client.getCollection(path)) {

			if (collection == null) {
				// if no config collection for this collection exists, just return
				this.collectionExists = false;
				this.resourceExists = false;
				return;
			}
			this.collectionExists = true;

			// get the resource from the db
			@Nullable Resource resConfig = null;
			try {
				for (final String resource : collection.listResources()) {
					if (resource.endsWith(CollectionConfiguration.COLLECTION_CONFIG_SUFFIX)) {
						resConfig = collection.getResource(resource);
						if (BinaryResource.RESOURCE_TYPE.equals(resConfig.getResourceType())) {
							System.err.println("Found a possible Collection configuration document: " + resConfig.getId() + ", however it is a Binary document! A user may have stored the document as a Binary document by mistake. Skipping...");
							continue;
						}
						break;
					}
				}

				if (resConfig == null) {
					// if, no config file exists for that collection
					this.resourceExists = false;
					return;
				}
				this.resourceExists = true;

				// Parse the configuration file into a DOM
				final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				@Nullable Document docConfig = null;
				try {
					final DocumentBuilder builder = factory.newDocumentBuilder();
					try (final InputStream is = new UnsynchronizedByteArrayInputStream(resConfig.getContent().toString().getBytes())) {
						docConfig = builder.parse(is);
					}
				} catch (final ParserConfigurationException | SAXException | IOException pce) {
					System.err.println("Found a possible Collection configuration document: " + resConfig.getId() + ", however could not be parsed!" + pce.getMessage());
				}

				if (docConfig == null) {
					// if, no config document could be parsed for that collection
					return;
				}

				// Get the root of the collection.xconf
				final Element xconf = docConfig.getDocumentElement();

				// Read any custom namespaces from xconf
				this.customNamespaces = getCustomNamespaces(xconf);

				// Read Range Indexes from xconf
				this.rangeIndexes = getRangeIndexes(xconf);

				// Read Triggers from xconf
				this.triggers = getTriggers(xconf);

			} finally {
				if (resConfig != null) {
					((EXistResource) resConfig).close();
				}
			}
		}
	}
	
	/**
	 * Returns an array of the Range Indexes
	 * 
	 * @return Array of Range Indexes
	 */
	public RangeIndex[] getRangeIndexes()
	{
		return rangeIndexes;
	}
	
	/**
	 * Returns n specific Range Index
	 * 
	 * @param index	The numeric index of the Range Index to return
	 * 
	 * @return The Range Index
	 */
	public RangeIndex getRangeIndex(int index)
	{
		return rangeIndexes[index];
	}
	
	/**
	 * Returns the number of Range Indexes defined
	 *  
	 * @return The number of Range indexes
	 */
	public int getRangeIndexCount()
	{
		if(rangeIndexes != null)
		{
			return rangeIndexes.length;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Delete a Range Index
	 * 
	 * @param index	The numeric index of the Range Index to delete
	 */
	public void deleteRangeIndex(int index)
	{
		//can only remove an index which is in the array
		if(index < rangeIndexes.length)
		{
			hasChanged = true;
			
			//if its the last item in the array just null the array 
			if(rangeIndexes.length == 1)
			{
				rangeIndexes = null;
			}
			else
			{
				//else remove the item at index from the array
				RangeIndex newRangeIndexes[] = new RangeIndex[rangeIndexes.length - 1];
				int x = 0;
				for(int i = 0; i < rangeIndexes.length; i++)
				{
					if(i != index)
					{
						newRangeIndexes[x] = rangeIndexes[i];
						x++;
					}
				}	
				rangeIndexes = newRangeIndexes;
			}
		}
	}
	
	/**
	 * Update the details of a Range Index
	 *
	 * @param index		The numeric index of the range index to update
	 * @param type		The type of the index, either {@link #TYPE_PATH} or {@link #TYPE_QNAME}
	 * @param XPath		The new XPath, or null to just set the type
	 * @param xsType	The new type of the path, a valid xs:type, or just null to set the path
	 */
	public void updateRangeIndex(int index, String type, String XPath, String xsType)
	{
		hasChanged = true;

        if (type != null)
            {rangeIndexes[index].setType(type);}
        
        if(XPath != null)
			{rangeIndexes[index].setXpath(XPath);}
		
		if(xsType != null)
			{rangeIndexes[index].setXsType(xsType);}
	}
	
	/**
	 * Add a Range Index
	 *
	 * @param type		The type of the index, either {@link #TYPE_PATH} or {@link #TYPE_QNAME}
	 * @param XPath		The XPath to index
	 * @param xsType	The type of the path, a valid xs:type
	 */
	public void addRangeIndex(String type, String XPath, String xsType)
	{
		hasChanged = true;
		
		if(rangeIndexes == null)
		{
			rangeIndexes = new RangeIndex[1];
			rangeIndexes[0] = new RangeIndex(type, XPath, xsType);
		}
		else
		{
			RangeIndex newRangeIndexes[] = new RangeIndex[rangeIndexes.length + 1];
			System.arraycopy(rangeIndexes, 0, newRangeIndexes, 0, rangeIndexes.length);
			newRangeIndexes[rangeIndexes.length] = new RangeIndex(type, XPath, xsType);
			rangeIndexes = newRangeIndexes;
		}
	}
	
	/**
	 * Returns an array of Triggers
	 * 
	 * @return Array of Range Indexes
	 */
	public Trigger[] getTriggers()
	{
		return triggers;
	}
	
	/**
	 * Returns n specific Trigger
	 * 
	 * @param index	The numeric index of the Trigger to return
	 * 
	 * @return The Trigger
	 */
	public Trigger getTrigger(int index)
	{
		return triggers[index];
	}
	
	/**
	 * Returns the number of Triggers defined
	 *  
	 * @return The number of Triggers
	 */
	public int getTriggerCount()
	{
		if(triggers != null)
		{
			return triggers.length;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Delete a Trigger
	 * 
	 * @param index	The numeric index of the Trigger to delete
	 */
	public void deleteTrigger(int index)
	{
		//can only remove an index which is in the array
		if(index < triggers.length)
		{
			hasChanged = true;
			
			//if its the last item in the array just null the array 
			if(triggers.length == 1)
			{
				triggers = null;
			}
			else
			{
				//else remove the item at index from the array
				Trigger newTriggers[] = new Trigger[triggers.length - 1];
				int x = 0;
				for(int i = 0; i < triggers.length; i++)
				{
					if(i != index)
					{
						newTriggers[x] = triggers[i];
						x++;
					}
				}	
				triggers = newTriggers;
			}
		}
	}
	
	/**
	 * Update the details of a Trigger
	 *
	 * @param index		The numeric index of the range index to update
	 * @param triggerClass	The name of the new class for the trigger
	 * @param parameters The parameters to the trigger
	 * 
	 */
	public void updateTrigger(int index, String triggerClass, Properties parameters)
	{
		//TODO: finish this!!! - need to add code for parameters
		
		hasChanged = true;
		
		if(triggerClass != null) {
			triggers[index].setTriggerClass(triggerClass);
                }
	}
	
	/**
	 * Add a Trigger
	 *
	 * @param triggerClass The class for the Trigger
	 * @param parameters Parameters to pass to trigger
	 * 
	 */
	public void addTrigger(String triggerClass, Properties parameters)
	{
		//TODO: finish this!!! seee updateTrigger
		
		hasChanged = true;
		
		if(triggers == null)
		{
			triggers = new Trigger[1];
			triggers[0] = new Trigger(triggerClass, parameters);
		}
		else
		{
			Trigger newTriggers[] = new Trigger[triggers.length + 1];
			System.arraycopy(triggers, 0, newTriggers, 0, triggers.length);
			newTriggers[triggers.length] = new Trigger(triggerClass, parameters);
			triggers = newTriggers;
		}
	}
	
	private LinkedHashMap<String, String> getCustomNamespaces(Element xconf)
	{
		final NamedNodeMap attrs = xconf.getAttributes();
		
		//there will always be one attribute - the default namespace
		if(attrs.getLength() > 1)
		{
			final LinkedHashMap<String, String> namespaces = new LinkedHashMap<>();
			
			for(int i = 0; i < attrs.getLength(); i++)
			{
				final Node a = attrs.item(i);
				if(a.getNodeName().startsWith("xmlns:"))
				{
					final String namespaceLocalName = a.getNodeName().substring(a.getNodeName().indexOf(':')+1);
					namespaces.put(namespaceLocalName, a.getNodeValue());
				}
			}
			
			return namespaces;
		}
		
		return null;
	}

	//given the root element of collection.xconf it will return an array of range indexes
	private RangeIndex[] getRangeIndexes(Element xconf)
	{
		final NodeList nlRangeIndexes = xconf.getElementsByTagName("create");
        if(nlRangeIndexes.getLength() > 0)
		{
            final List<RangeIndex> rl = new ArrayList<>();
            for(int i = 0; i < nlRangeIndexes.getLength(); i++)
			{	
				final Element rangeIndex = (Element)nlRangeIndexes.item(i);
                if (rangeIndex.hasAttribute("type")) {
                    if (rangeIndex.hasAttribute("qname"))
                        {rl.add(new RangeIndex(TYPE_QNAME, rangeIndex.getAttribute("qname"), rangeIndex.getAttribute("type")));}
                    else
                        {rl.add(new RangeIndex(TYPE_PATH, rangeIndex.getAttribute("path"), rangeIndex.getAttribute("type")));}
                }
            }
            RangeIndex[] rangeIndexes = new RangeIndex[rl.size()];
            rangeIndexes = rl.toArray(rangeIndexes);
            return rangeIndexes;
		}
		return null;
	}
	
	//given the root element of collection.xconf it will return an array of triggers
	private Trigger[] getTriggers(Element xconf)
	{
		final NodeList nlTriggers = xconf.getElementsByTagName("trigger");
		if(nlTriggers.getLength() > 0)
		{
			final Trigger[] triggers = new Trigger[nlTriggers.getLength()]; 
			
			for(int i = 0; i < nlTriggers.getLength(); i++)
			{	
				final Element trigger = (Element)nlTriggers.item(i);
				
				final Properties parameters = new Properties();
				final NodeList nlTriggerParameters = trigger.getElementsByTagName("parameter");
				if(nlTriggerParameters.getLength() > 0)
				{
					for(int x = 0; x < nlTriggerParameters.getLength(); x++)
					{
						final Element parameter = (Element)nlTriggerParameters.item(x);
						parameters.setProperty(parameter.getAttribute("name"), parameter.getAttribute("value"));
					}
				}
				
				//create the trigger
				triggers[i] = new Trigger(trigger.getAttribute("class"), parameters);
			}
			
			return triggers;
		}
		return null;
	}
	
	//has the collection.xconf been modified?
	/**
	 * Indicates whether the collection configuration has changed
	 * 
	 * @return true if the configuration has changed, false otherwise
	 */
	public boolean hasChanged()
	{
		return hasChanged;
	}
	
	//produces a string of XML describing the collection.xconf
	private String toXMLString()
	{
		final StringBuilder xconf = new StringBuilder();
		
		xconf.append("<collection xmlns=\"http://exist-db.org/collection-config/1.0\"");
		if(customNamespaces != null)
		{
			for (final Map.Entry<String, String> entry : customNamespaces.entrySet())
			{
				xconf.append(" ");
				final String namespaceLocalName = entry.getKey();
				final String namespaceURL = entry.getValue();
				xconf.append("xmlns:").append(namespaceLocalName).append("=\"").append(namespaceURL).append("\"");
			}
		}
		xconf.append(">");
		xconf.append(System.getProperty("line.separator"));
		
		//index
		if(rangeIndexes != null)
		{
			xconf.append('\t');
			xconf.append("<index>");
			xconf.append(System.getProperty("line.separator"));
		
			//range indexes
			if(rangeIndexes != null)
			{
				for (RangeIndex rangeIndex : rangeIndexes) {
					xconf.append("\t\t\t");
					xconf.append(rangeIndex.toXMLString());
					xconf.append(System.getProperty("line.separator"));
				}
			}
			
			xconf.append('\t');
			xconf.append("</index>");
			xconf.append(System.getProperty("line.separator"));
		}
		
		//triggers
		if(triggers != null)
		{
			xconf.append('\t');
			xconf.append("<triggers>");

			for (Trigger trigger : triggers) {
				xconf.append("\t\t\t");
				xconf.append(trigger.toXMLString());
				xconf.append(System.getProperty("line.separator"));
			}
			
			xconf.append('\t');
			xconf.append("</triggers>");
			xconf.append(System.getProperty("line.separator"));
		}
		
		xconf.append("</collection>");
		
		return xconf.toString();
	}
	
	/**
	 * Saves the collection configuation back to the collection.xconf
	 * 
	 * @return true if the save succeeds, false otherwise
	 */
	public boolean save() {
		if (!collectionExists) {
			client.process("mkcol " + path);
		}

		try (final Collection collection = client.getCollection(path)) {
			@Nullable Resource resConfig = null;
			try {
				if (resourceExists) {
					resConfig = collection.getResource(CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE);
				} else {
					resConfig = collection.createResource(CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE, XMLResource.RESOURCE_TYPE);
				}

				// set the content of the collection.xconf
				resConfig.setContent(toXMLString());

				//s tore the collection.xconf
				collection.storeResource(resConfig);

			} finally {
				if (resConfig != null) {
					((EXistResource) resConfig).close();
				}
			}

			return true;

		} catch (final XMLDBException e) {
			return false;
		}
	}
	
	/**
	 * Represents a Range Index in the collection.xconf
	 */
	protected class RangeIndex
	{
        private String type;
        private String xpath;
		private String xsType;
		
		/**
		 * Constructor
		 *
		 * @param type type of the index, either "qname" or "path"
		 * @param xpath		The XPath to create a range index on
		 * @param xsType	The data type pointed to by the XPath as an xs:type 
		 */
		RangeIndex(final String type, final String xpath, final String xsType) {
            this.type = type;
            this.xpath = xpath;
			this.xsType = xsType;
		}
		
		public String getXpath() {
			return xpath;
		}
		
		public String getXsType()
		{
			return(xsType);
		}

        public String getType() {
            return type;
        }

        public void setXpath(final String xpath) {
			this.xpath = xpath;
		}
		
		public void setXsType(final String xsType) {
			this.xsType = xsType;
		}

        public void setType(final String type) {
            this.type = type;
        }
        
        //produces a collection.xconf suitable string of XML describing the range index
		protected String toXMLString() {
			final StringBuilder range = new StringBuilder();

            if (TYPE_PATH.equals(type)) {
				range.append("<create path=\"");
			} else {
				range.append("<create qname=\"");
			}
            range.append(xpath);
			range.append("\" type=\"");
			range.append(xsType);
			range.append("\"/>");
			
			return range.toString();
		}
	}
	
	/**
	 * Represents a Trigger in the collection.xconf
	 */
	protected static class Trigger {
		private String triggerClass = null;
		private final Properties parameters;
		
		/**
		 * Constructor
		 * 
		 * @param triggerClass				The fully qualified java class name of the trigger
		 * @param parameters				Properties describing any name=value parameters for the trigger
		 */
		Trigger(final String triggerClass, final Properties parameters) {
			this.triggerClass = triggerClass;
			this.parameters = parameters;
		}
		
		public String getTriggerClass() {
			return triggerClass;
		}
		
		public void setTriggerClass(final String triggerClass) {
			this.triggerClass = triggerClass;
		}
		
		//produces a collection.xconf suitable string of XML describing the trigger
		protected String toXMLString() {
			final StringBuilder trigger = new StringBuilder();
			
			if(!"".equals(triggerClass)) {
			
				trigger.append("<trigger class=\"");
				trigger.append(triggerClass);
				trigger.append("\">");
				
				//parameters if any
				if(parameters != null) {
					if(parameters.size() > 0) {
						for (Object o : parameters.keySet()) {
							final String name = (String) o;
							final String value = parameters.getProperty(name);

							trigger.append("<parameter name=\"");
							trigger.append(name);
							trigger.append("\" value=\"");
							trigger.append(value);
							trigger.append("\"/>");
						}
					}
				}
				
				trigger.append("</trigger>");
			}
			return trigger.toString();
		}
	}
}
