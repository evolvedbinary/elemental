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
package org.exist.validation.internal;

import java.io.IOException;
import java.util.*;

import com.evolvedbinary.j8fu.function.ConsumerE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.source.ClassLoaderSource;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;

import javax.annotation.Nullable;

/**
 *  Helper class for accessing grammars.
 *
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class DatabaseResources {
    
    public final static String QUERY_LOCATION = "org/exist/validation/internal/query/";
    
    public final static String FIND_XSD = QUERY_LOCATION + "find_schema_by_targetNamespace.xq";
    
    public final static String FIND_CATALOGS_WITH_DTD = QUERY_LOCATION + "find_catalogs_with_dtd.xq";
    
    public final static String PUBLICID = "publicId";
    
    public final static String TARGETNAMESPACE = "targetNamespace";
    
    public final static String CATALOG    = "catalog";
    
    public final static String COLLECTION = "collection";

    private BrokerPool brokerPool = null;

    private static final Logger LOGGER = LogManager.getLogger(DatabaseResources.class);
    
    
    /**
     *  Convert sequence into list of strings.
     *
     * @param   sequence  Result of query.
     * @return  List containing String objects.
     */
    public List<String> getAllResults(Sequence sequence){
        List<String> result = new ArrayList<>();
        
        try {
            final SequenceIterator i = sequence.iterate();         
            while(i.hasNext()){
                final String path =  i.nextItem().getStringValue();
                result.add(path);
            }
            
        } catch (final XPathException ex) {
            LOGGER.error("XQuery issue.", ex);
            result=null;
        }
        
        return result;
    }
    
    /**
     *  Get first entry of sequence as String.
     *
     * @param   sequence  Result of query.
     * @return  String containing representation of 1st entry of sequence.
     */
    public String getFirstResult(Sequence sequence){
        String result = null;
        
        try {
            final SequenceIterator i = sequence.iterate();
            if(i.hasNext()){
                result= i.nextItem().getStringValue();

                LOGGER.debug("Single query result: '{}'.", result);
                
            } else {
                LOGGER.debug("No query result.");
            }
            
        } catch (final XPathException ex) {
            LOGGER.error("XQuery issue ", ex);
        }
        
        return result;
    }
    
    
    public @Nullable Sequence executeQuery(final String queryPath, final Map<String,String> params, final Subject user){
        @Nullable final String namespace = params.get(TARGETNAMESPACE);
        @Nullable final String publicId = params.get(PUBLICID);
        @Nullable final String catalogPath = params.get(CATALOG);
        @Nullable final String collection = params.get(COLLECTION);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("collection={} namespace={} publicId={} catalogPath={}", collection, namespace, publicId, catalogPath);
        }

        try (final DBBroker broker = brokerPool.get(Optional.ofNullable(user))) {

            final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreCompilation = xqueryContext -> {
                if (collection != null) {
                    xqueryContext.declareVariable(new QName(COLLECTION, Namespaces.XQUERY_LOCAL_NS), true, collection);
                }

                if (namespace != null) {
                    xqueryContext.declareVariable(new QName(TARGETNAMESPACE, Namespaces.XQUERY_LOCAL_NS), true, namespace);
                }

                if (publicId != null) {
                    xqueryContext.declareVariable(new QName(PUBLICID, Namespaces.XQUERY_LOCAL_NS), true, publicId);
                }

                if (catalogPath != null) {
                    xqueryContext.declareVariable(new QName(CATALOG, Namespaces.XQUERY_LOCAL_NS), true, catalogPath);
                }
            };

            final Source source = new ClassLoaderSource(queryPath);
            final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, source, true, null, null, setupXqueryContextPreCompilation, null, null);
            return queryResult.result;
            
        } catch (final EXistException | XPathException | IOException | PermissionDeniedException ex) {
            LOGGER.error("Problem executing XQuery: {}", ex.getMessage(), ex);
            return null;
        }
    }
    
    
    /**
     * Creates a new instance of DatabaseResources.
     * 
     * 
     * 
     * @param pool  Instance shared broker pool.
     */
    public DatabaseResources(BrokerPool pool) {
        
        LOGGER.info("Initializing DatabaseResources");
        this.brokerPool = pool;
        
    }
    
    public String findXSD(String collection, String targetNamespace, Subject user){
        
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find schema with namespace '{}' in '{}'.", targetNamespace, collection);
        }
        
        final Map<String,String> params = new HashMap<>();
        params.put(COLLECTION, collection);
        params.put(TARGETNAMESPACE, targetNamespace);
        
        final Sequence result = executeQuery(FIND_XSD, params, user );
        
        return getFirstResult(result);
    }
    
    public String findCatalogWithDTD(String collection, String publicId, Subject user){
        
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find DTD with public '{}' in '{}'.", publicId, collection);
        }
        
        final Map<String,String> params = new HashMap<>();
        params.put(COLLECTION, collection);
        params.put(PUBLICID, publicId);
        
        final Sequence result = executeQuery(FIND_CATALOGS_WITH_DTD, params, user );
        
        return getFirstResult(result);
    }
    
}
