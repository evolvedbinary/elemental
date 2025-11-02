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
package org.exist.collections.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.exist.Indexer;
import org.exist.collections.Collection;
import org.exist.collections.CollectionConfiguration;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class DocumentTriggers implements DocumentTrigger, ContentHandler, LexicalHandler, ErrorHandler {
    
    private Indexer indexer;
    
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;
    private ErrorHandler errorHandler;
    
    private SAXTrigger last = null;
    
    private final List<DocumentTrigger> triggers;
    
    public DocumentTriggers(final DBBroker broker, final Txn transaction) throws TriggerException {
        this(broker, transaction, null, null, null);
    }
    
    public DocumentTriggers(final DBBroker broker, final Txn transaction, final Collection collection) throws TriggerException {
        this(broker, transaction, null, collection, broker.isTriggersEnabled() ? collection.getConfiguration(broker) : null);
    }

    public DocumentTriggers(final DBBroker broker, final Txn transaction, final Indexer indexer, final Collection collection, final CollectionConfiguration config) throws TriggerException {
        final List<TriggerProxy<? extends DocumentTrigger>> docTriggers = config != null ? config.documentTriggers() : null;
        final java.util.Collection<TriggerProxy<? extends DocumentTrigger>> masterTriggers = broker.getDatabase().getDocumentTriggers();
        
        triggers = new ArrayList<>(masterTriggers.size() + (docTriggers == null ? 0 : docTriggers.size()));
        
        for (final TriggerProxy<? extends DocumentTrigger> docTrigger : masterTriggers) {
            final DocumentTrigger instance = docTrigger.newInstance(broker, transaction, collection);
            register(instance);
        }
        
        if (docTriggers != null) {
            for (final TriggerProxy<? extends DocumentTrigger> docTrigger : docTriggers) {
                final DocumentTrigger instance = docTrigger.newInstance(broker, transaction, collection);
                register(instance);
            }
        }
        
        if (indexer != null) {
            finishPreparation(indexer);
        }
        
        last = null;
    }
    
    private void finishPreparation(final Indexer indexer) {
        if (last == null) {
            contentHandler = indexer;
            lexicalHandler = indexer;
            errorHandler = indexer;
        } else {
            last.next( indexer );
        }
        
        this.indexer = indexer;
    }

    private void register(final DocumentTrigger trigger) {
        if (trigger instanceof SAXTrigger) {
            final SAXTrigger filteringTrigger = (SAXTrigger) trigger;

            if (last == null) {
                contentHandler = filteringTrigger;
                lexicalHandler = filteringTrigger;
                errorHandler = filteringTrigger;

            } else {
                last.next( filteringTrigger );
            }
            
            last = filteringTrigger;
        }

        triggers.add(trigger);
    }

    @Override
    public void configure(final DBBroker broker, final Txn txn, final Collection parent, final Map<String, List<? extends Object>> parameters) throws TriggerException {
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        contentHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        contentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        contentHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        contentHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        contentHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
        contentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(final String target, final String data) throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(final String name) throws SAXException {
        contentHandler.skippedEntity(name);
    }

    @Override
    public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        lexicalHandler.startDTD(name, publicId, systemId);
    }

    @Override
    public void endDTD() throws SAXException {
        lexicalHandler.endDTD();
    }

    @Override
    public void startEntity(final String name) throws SAXException {
        lexicalHandler.startEntity(name);
    }

    @Override
    public void endEntity(final String name) throws SAXException {
        lexicalHandler.endEntity(name);
    }

    @Override
    public void startCDATA() throws SAXException {
        lexicalHandler.startCDATA();
    }

    @Override
    public void endCDATA() throws SAXException {
        lexicalHandler.endCDATA();
    }

    @Override
    public void comment(final char[] ch, final int start, final int length) throws SAXException {
        lexicalHandler.comment(ch, start, length);
    }

    @Override
    public void beforeCreateDocument(final DBBroker broker, final Txn txn, final XmldbURI uri) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeCreateDocument(broker, txn, uri);
            } catch (final Exception e) {
                logAndThrowError("beforeCreateDocument", trigger, uri, e);
            }
        }
    }

    @Override
    public void afterCreateDocument(final DBBroker broker, final Txn txn, final DocumentImpl document) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterCreateDocument(broker, txn, document);
            } catch (final Exception e) {
                logError("afterCreateDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void beforeUpdateDocument(final DBBroker broker, final Txn txn, final DocumentImpl document) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeUpdateDocument(broker, txn, document);
            } catch (final Exception e) {
                logAndThrowError("beforeUpdateDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void afterUpdateDocument(final DBBroker broker, final Txn txn, final DocumentImpl document) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterUpdateDocument(broker, txn, document);
            } catch (final Exception e) {
                logError("afterUpdateDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void beforeUpdateDocumentMetadata(final DBBroker broker, final Txn txn, final DocumentImpl document) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeUpdateDocumentMetadata(broker, txn, document);
            } catch (final Exception e) {
                logAndThrowError("beforeUpdateDocumentMetadata", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void afterUpdateDocumentMetadata(final DBBroker broker, final Txn txn, final DocumentImpl document) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterUpdateDocumentMetadata(broker, txn, document);
            } catch (final Exception e) {
                logError("afterUpdateDocumentMetadata", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void beforeCopyDocument(final DBBroker broker, final Txn txn, final DocumentImpl document, final XmldbURI newUri) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeCopyDocument(broker, txn, document, newUri);
            } catch (final Exception e) {
                logAndThrowError("beforeCopyDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void afterCopyDocument(final DBBroker broker, final Txn txn, final DocumentImpl document, final XmldbURI oldUri) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterCopyDocument(broker, txn, document, oldUri);
            } catch (final Exception e) {
                logError("afterCopyDocument", trigger, oldUri, e);
            }
        }
    }

    @Override
    public void beforeMoveDocument(final DBBroker broker, final Txn txn, final DocumentImpl document, final XmldbURI newUri) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeMoveDocument(broker, txn, document, newUri);
            } catch (final Exception e) {
                logAndThrowError("beforeMoveDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void afterMoveDocument(final DBBroker broker, final Txn txn, final DocumentImpl document, final XmldbURI oldUri) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterMoveDocument(broker, txn, document, oldUri);
            } catch (final Exception e) {
                logError("afterMoveDocument", trigger, oldUri, e);
            }
        }
    }

    @Override
    public void beforeDeleteDocument(final DBBroker broker, final Txn txn, final DocumentImpl document) throws TriggerException {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.beforeDeleteDocument(broker, txn, document);
            } catch (final Exception e) {
                logAndThrowError("beforeDeleteDocument", trigger, document.getURI(), e);
            }
        }
    }

    @Override
    public void afterDeleteDocument(final DBBroker broker, final Txn txn, final XmldbURI uri) {
        for (final DocumentTrigger trigger : triggers) {
            try {
                trigger.afterDeleteDocument(broker, txn, uri);
            } catch (final Exception e) {
                logError("afterDeleteDocument", trigger, uri, e);
            }
        }
    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public void setValidating(final boolean validating) {
        for (final DocumentTrigger trigger : triggers) {
            trigger.setValidating(validating);
        }
        
        indexer.setValidating(validating);
    }

    @Override
    public void warning(final SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.warning(exception);
        }
    }

    @Override
    public void error(final SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(exception);
        }
    }

    @Override
    public void fatalError(final SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.fatalError(exception);
        }
    }

    private void logAndThrowError(final String eventName, final DocumentTrigger documentTrigger, final XmldbURI source, final Exception e) throws TriggerException {
        logError(eventName, documentTrigger, source, e);
        throwError(e);
    }

    private void logError(final String eventName, final DocumentTrigger documentTrigger, final XmldbURI source, final Exception e) {
        final String message = String.format("Error in %s#%s triggered by: %s, %s", documentTrigger.getClass().getSimpleName(), eventName, source, e.getMessage());
        Trigger.LOG.error(message, e);
    }

    private void throwError(final Exception e) throws TriggerException {
        if (e instanceof TriggerException) {
            throw (TriggerException) e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new TriggerException(e);
        }
    }
}
