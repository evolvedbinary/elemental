/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.indexing.range;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.exist.dom.NodeListImpl;
import org.exist.dom.QName;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xquery.value.Type;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.*;

public class RangeIndexConfig {

    public final static RangeIndexConfig DEFAULT_CONFIG = new RangeIndexConfig();
    
    static final String CONFIG_ROOT = "range";
    static final String CREATE_ELEM = "create";
    private static final String FIELD_ELEM = "field";
    private final static String CONDITION_ELEM = "condition";
    private final static String CONTEXT_ELEM = "context";

    private static final Logger LOG = LogManager.getLogger(RangeIndexConfig.class);

    private final Map<QName, RangeIndexConfigElement> paths;
    private @Nullable Map<String, RangeIndexConfigContextElement> contexts = null;

    private Analyzer analyzer;

    private final PathIterator iterator = new PathIterator();

    public RangeIndexConfig() {
        // default analyzer
        this.analyzer = new KeywordAnalyzer();
        this.paths = new TreeMap<>();
    }

    public RangeIndexConfig(final NodeList configNodes, final Map<String, String> namespaces) {
        this.paths = new TreeMap<>();
        parse(configNodes, namespaces);
    }

    public RangeIndexConfig(final RangeIndexConfig other) {
        this.paths = other.paths;
        this.analyzer = other.analyzer;
    }

    /* find one simple configuration for path */
    public RangeIndexConfigElement find(final NodePath path) {
        for (RangeIndexConfigElement rice : paths.values()) {
            do {
                if (rice.find(path) && !(rice instanceof ComplexRangeIndexConfigElement)) {
                    return rice;
                }
                rice = rice.getNext();
            } while (rice != null);
        }
        return null;
    }

    /* find all complex configurations for path (that might have different conditions) */
    public List<ComplexRangeIndexConfigElement> findAll(final NodePath path) {
        final List<ComplexRangeIndexConfigElement> rices = new ArrayList<>();
        for (RangeIndexConfigElement rice : paths.values()) {
            do {
                if (rice.find(path) && rice instanceof ComplexRangeIndexConfigElement) {
                    rices.add((ComplexRangeIndexConfigElement)rice);
                }

                rice = rice.getNext();
            } while (rice != null);
        }
        return rices;
    }

    private void parse(final NodeList configNodes, final Map<String, String> namespaces) {
        // default analyzer
        this.analyzer = new KeywordAnalyzer();
        for (int i = 0; i < configNodes.getLength(); i++) {
            final Node node = configNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && CONFIG_ROOT.equals(node.getLocalName())) {
                parseChildren(node.getChildNodes(), namespaces);
            }
        }
    }

    private void parseChildren(final NodeList configNodes, final Map<String, String> namespaces) {
        for (int i = 0; i < configNodes.getLength(); i++) {
            final Node node = configNodes.item(i);
            try {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    RangeIndexConfigElement newConfig = null;
                    if (CREATE_ELEM.equals(node.getLocalName())) {
                        final NodeList fields = getFieldsAndConditions((Element) node);
                        if (fields.getLength() > 0) {
                            newConfig = new ComplexRangeIndexConfigElement((Element) node, fields, namespaces, contexts);
                        } else {
                            newConfig = new BasicRangeIndexConfigElement((Element) node, namespaces, contexts);
                        }
                    } else if (CONTEXT_ELEM.equals(node.getLocalName())) {
                        newConfig = new RangeIndexConfigContextElement((Element) node, namespaces);
                        if (contexts == null) {
                            contexts = new HashMap<>();
                        }
                        contexts.put(((RangeIndexConfigContextElement) newConfig).getId(), (RangeIndexConfigContextElement) newConfig);
                    }

                    final RangeIndexConfigElement idxConf = paths.get(newConfig.getNodePath().getLastComponent());
                    if (idxConf == null) {
                        paths.put(newConfig.getNodePath().getLastComponent(), newConfig);
                    } else {
                        idxConf.add(newConfig);
                    }
                }
            } catch (final DatabaseConfigurationException e) {
                String uri = null;
                final Document doc = node.getOwnerDocument();
                if(doc != null) {
                    uri = doc.getDocumentURI();
                }

                if(uri != null) {
                    LOG.error("Invalid range index configuration (" + uri + "): " + e.getMessage());
                } else {
                    LOG.error("Invalid range index configuration: " + e.getMessage());
                }
            }
        }
    }

    public Analyzer getDefaultAnalyzer() {
        return analyzer;
    }

    public Analyzer getAnalyzer(final QName qname, final String fieldName) {
        Analyzer analyzer = null;
        if (qname != null) {
            final RangeIndexConfigElement idxConf = paths.get(qname);
            if (idxConf instanceof BasicRangeIndexConfigElement) {
                analyzer = ((BasicRangeIndexConfigElement) idxConf).getAnalyzer(null);
            }
        } else {
            for (final RangeIndexConfigElement idxConf: paths.values()) {
                if (idxConf instanceof ComplexRangeIndexConfigElement) {
                    analyzer = ((ComplexRangeIndexConfigElement) idxConf).getAnalyzer(fieldName);
                    if (analyzer != null) {
                        break;
                    }
                }
            }
        }
        return analyzer;
    }

    public boolean isCaseSensitive(final QName qname, final String fieldName) {
        boolean caseSensitive = true;
        if (qname != null) {
            final RangeIndexConfigElement idxConf = paths.get(qname);
            if (idxConf instanceof BasicRangeIndexConfigElement) {
                caseSensitive = ((BasicRangeIndexConfigElement) idxConf).isCaseSensitive(fieldName);
            }
        } else {
            for (final RangeIndexConfigElement idxConf: paths.values()) {
                if (idxConf instanceof ComplexRangeIndexConfigElement) {
                    caseSensitive = ((ComplexRangeIndexConfigElement) idxConf).isCaseSensitive(fieldName);
                    if (!caseSensitive) {
                        break;
                    }
                }
            }
        }
        return caseSensitive;
    }

    public Iterator<RangeIndexConfigElement> getConfig(final NodePath path) {
        iterator.reset(path);
        return iterator;
    }

    private NodeList getFieldsAndConditions(final Element root) {
        final NodeListImpl fields = new NodeListImpl();
        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && (FIELD_ELEM.equals(node.getLocalName()) || CONDITION_ELEM.equals(node.getLocalName()))) {
                fields.add(node);
            }
        }
        return fields;
    }

    public boolean matches(final NodePath path) {
        RangeIndexConfigElement idxConf = paths.get(path.getLastComponent());
        while (idxConf != null) {
            if (idxConf.match(path)) {
                return true;
            }
            idxConf = idxConf.getNext();
        }
        return false;
    }

    public int getType(final String field) {
        for (final RangeIndexConfigElement conf : paths.values()) {
            if (conf instanceof ComplexRangeIndexConfigElement) {
                int type = ((ComplexRangeIndexConfigElement) conf).getType(field);
                if (type != Type.ITEM) {
                    return type;
                }
            }
        }
        return Type.ITEM;
    }

    private class PathIterator implements Iterator<RangeIndexConfigElement> {
        private @Nullable RangeIndexConfigElement nextConfig;
        private NodePath path;
        private boolean atLast = false;

        protected void reset(final NodePath path) {
            this.atLast = false;
            this.path = path;
            this.nextConfig = paths.get(path.getLastComponent());
            if (this.nextConfig == null) {
                this.atLast = true;
            }
        }

        @Override
        public boolean hasNext() {
            return (nextConfig != null);
        }

        @Override
        public RangeIndexConfigElement next() {
            if (this.nextConfig == null) {
                return null;
            }

            final RangeIndexConfigElement currentConfig = this.nextConfig;
            this.nextConfig = this.nextConfig.getNext();
            if (this.nextConfig == null && !this.atLast) {
                this.atLast = true;
            }
            return currentConfig;
        }

        @Override
        public void remove() {
            //Nothing to do
        }
    }
}