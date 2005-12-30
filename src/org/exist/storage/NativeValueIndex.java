/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.AttrImpl;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.ElementImpl;
import org.exist.dom.ExtArrayNodeSet;
import org.exist.dom.NodeImpl;
import org.exist.dom.NodeProxy;
import org.exist.dom.NodeSet;
import org.exist.dom.TextImpl;
import org.exist.dom.XMLUtil;
import org.exist.storage.btree.BTreeCallback;
import org.exist.storage.btree.BTreeException;
import org.exist.storage.btree.DBException;
import org.exist.storage.btree.IndexQuery;
import org.exist.storage.btree.Value;
import org.exist.storage.index.BFile;
import org.exist.storage.io.VariableByteArrayInput;
import org.exist.storage.io.VariableByteInput;
import org.exist.storage.io.VariableByteOutputStream;
import org.exist.storage.lock.Lock;
import org.exist.util.ByteConversion;
import org.exist.util.LockException;
import org.exist.util.LongLinkedList;
import org.exist.util.ReadOnlyException;
import org.exist.util.UTF8;
import org.exist.util.ValueOccurrences;
import org.exist.util.XMLString;
import org.exist.xquery.Constants;
import org.exist.xquery.TerminatedException;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

/**
 * Maintains an index on typed node values.
 * 
 * TODO: Check correct types during validation.
 * 
 * In the BTree single BFile, the keys are :
 * (collectionId, indexType, indexData)
 * and the values are : gid1, gid2-gid1, ...
 * <b></b>
 * <p>Algorithm:</p>
 * When a node is stored, an entry is added or updated in the {@link #pending} map, 
 * with given String content and basic type as key.
 * This way, the index entries are easily put in the persistent BFile storage by 
 * {@link #flush()} .
 * 
 * @author wolf
 */
public class NativeValueIndex implements ContentLoadingObserver {

    private final static Logger LOG = Logger.getLogger(NativeValueIndex.class);
    
	/** The broker that is using this value index */
	DBBroker broker;
	
	/** The datastore for this value index */
    protected BFile dbValues;
    
	/** A collection of key-value pairs that pending modifications for this value index.  
     * The keys are {@link org.exist.xquery.value.AtomicValue atomic values}
     * that implement {@link Indexable Indexable}.
	 * The values are {@link org.exist.util.LongLinkedList lists} containing
	 * the nodes GIDs (global identifiers.
	 * Do not confuse the keys with the ones used in persistent storage, created with
	 * {@link Indexable#serialize(short) */
    protected TreeMap pending = new TreeMap();
    
	/** The current document */
    private DocumentImpl doc;
    
	/** Work output Stream taht should be cleared before every use */
    private VariableByteOutputStream os = new VariableByteOutputStream();
    
    //TODO : reconsider this. Case sensitivity have nothing to do with atomic values -pb
    protected boolean caseSensitive = true;
    
    public NativeValueIndex(DBBroker broker, BFile dbValues) {
        this.broker = broker;
        this.dbValues = dbValues;
        //TODO : reconsider this. Case sensitivity have nothing to do with atomic values -pb
        Boolean caseOpt = (Boolean) broker.getConfiguration().getProperty("indexer.case-sensitive");
        if (caseOpt != null)
            caseSensitive = caseOpt.booleanValue();
    }
    
    /* (non-Javadoc)
     * @see org.exist.storage.ContentLoadingObserver#setDocument(org.exist.dom.DocumentImpl)
     */
    public void setDocument(DocumentImpl document) {
        this.doc = document;
    }    
    
    /** Store the given element's value in the value index.
     * @param xpathType The value type
     * @param node The element
     * @param content The string representation of the value
     */
    public void storeElement(int xpathType, ElementImpl node, String content) {
        AtomicValue atomic = convertToAtomic(xpathType, content);
        if (atomic == null)
            //TODO : throw an exception ? -pb
            return;
        LongLinkedList buf;
        //Is this indexable value already pending ?
        if (pending.containsKey(atomic))
            //Reuse the existing GIDs list
            buf = (LongLinkedList) pending.get(atomic);
        else {
            //Create a GIDs list
            buf = new LongLinkedList();
            pending.put(atomic, buf);
        }
        //Add node's GID to the list
        buf.add(node.getGID());
    }
    

    /** Store the given attribute's value in the value index.
     * @param spec The index specification
     * @param node The attribute
     */
    public void storeAttribute(RangeIndexSpec spec, AttrImpl node) {
        AtomicValue atomic = convertToAtomic(spec.getType(), node.getValue());
        if(atomic == null)
            return;
        LongLinkedList buf;
        //Is this indexable value already pending ?
        if (pending.containsKey(atomic))
            //Reuse the existing GIDs list
            buf = (LongLinkedList) pending.get(atomic);
        else {
            //Create a GIDs list
            buf = new LongLinkedList();
            pending.put(atomic, buf);
        }
        //Add node's GID to the list
        buf.add(node.getGID());
    }
    
    /* (non-Javadoc)
     * @see org.exist.storage.IndexGenerator#sync()
     */
    public void sync() {
        Lock lock = dbValues.getLock();
        try {
            lock.acquire(Lock.WRITE_LOCK);
            dbValues.flush();            
        } catch (LockException e) {
            LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);
            //TODO : throw an exception ? -pb
        } catch (DBException e) {
            LOG.error(e.getMessage(), e); 
            //TODO : throw an exception ? -pb
        } finally {
            lock.release();
        }
    }    
    
	/* (non-Javadoc)
	 * @see org.exist.storage.IndexGenerator#flush()
	 */
    public void flush() {
        //TODO : return if doc == null? -pb        
        if (pending.size() == 0) 
            return;        
        Indexable indexable;
        //TODO : NativeElementIndex uses ArrayLists -pb
        LongLinkedList gidList;
        long gids[];
        int gidsCount;        
        long previousGID;
        long delta;
        Value ref;
        Map.Entry entry;        
        final short collectionId = this.doc.getCollection().getId();
        final Lock lock = dbValues.getLock();
        for (Iterator i = pending.entrySet().iterator(); i.hasNext();) {
            entry = (Map.Entry) i.next();
            indexable = (Indexable) entry.getKey();
            gidList = (LongLinkedList) entry.getValue();
            gids = gidList.getData();
            gidsCount = gids.length;
            //Don't forget this one
            Arrays.sort(gids);
            os.clear();
            os.writeInt(this.doc.getDocId());
            os.writeInt(gidsCount);
            //Compute the GID list
            previousGID = 0;
            for (int j = 0; j < gidsCount; j++) {                    
                delta = gids[j] - previousGID;                    
                os.writeLong(delta);
                previousGID = gids[j];
            }
            //Compute a key for the value
            ref = new Value(indexable.serialize(collectionId, caseSensitive));
            try {
                lock.acquire(Lock.WRITE_LOCK);
                //Store data
                if (dbValues.append(ref, os.data()) == BFile.UNKNOWN_ADDRESS) {
                    LOG.warn("Could not append index data for value '" +  ref + "'");                   
                }
            } catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);
               //TODO : return ?                         
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);  
                //TODO : return ?
            } catch (ReadOnlyException e) {
                LOG.warn(e.getMessage(), e);     
                //Return without clearing the pending entries
                return;                 
            } finally {
                lock.release();
            }
        }
        pending.clear();
    }
    
    /* (non-Javadoc)
     * @see org.exist.storage.IndexGenerator#remove()
     */
    public void remove() { 
        //TODO : return if doc == null? -pb  
        if (pending.size() == 0) 
            return;        
        Indexable indexable;
        //TODO : NativeElementIndex uses ArrayLists -pb
        LongLinkedList storedGIDList;
        LongLinkedList newGIDList;
        long[] gids;        
        int gidsCount;
        long storedGID;
        long previousGID;        
        long delta; 
        Map.Entry entry;    
        Value searchKey;
        Value value;
        VariableByteArrayInput is;
        int storedDocId;
        final short collectionId = this.doc.getCollection().getId();
        final Lock lock = dbValues.getLock();           
        for (Iterator i = pending.entrySet().iterator(); i.hasNext();) {
            entry = (Map.Entry) i.next();
            indexable = (Indexable) entry.getKey();
            storedGIDList = (LongLinkedList) entry.getValue();   
            //Compute a key for the value
            searchKey = new Value(indexable.serialize(collectionId, caseSensitive)); 
            newGIDList = new LongLinkedList();
            os.clear();              
            try {                    
                lock.acquire(Lock.WRITE_LOCK); 
                value = dbValues.get(searchKey);
                //Does the value already exist in the index ?
                if (value != null) {
                    //Add its data to the new list
                    is = new VariableByteArrayInput(value.getData());
                    try {                            
                        while (is.available() > 0) {
                            storedDocId = is.readInt();
                            gidsCount = is.readInt();
                            if (storedDocId != this.doc.getDocId()) {
                                // data are related to another document:
                                // append them to any existing data
                                os.writeInt(storedDocId);
                                os.writeInt(gidsCount);                                                                       
                                is.copyTo(os, gidsCount);
                            } else {
                                // data are related to our document:
                                // feed the new list with the GIDs
                                previousGID = 0;
                                for (int j = 0; j < gidsCount; j++) {
                                    delta = is.readLong();
                                    storedGID = previousGID + delta;  
                                    // add the node to the new list if it is not 
                                    // in the list of removed nodes
                                    if (!storedGIDList.contains(storedGID)) {
                                        newGIDList.add(storedGID);
                                    }
                                    previousGID = storedGID;
                                }
                            }
                        }
                    } catch (EOFException e) {
                        //Is it expected ? -pb
                        LOG.warn(e.getMessage(), e);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        //TODO : data will be saved although os is probably corrupted ! -pb
                    }
                    //append the data from the new list
                    if (newGIDList.getSize() > 0) {                        
                        gids = newGIDList.getData();
                        gidsCount = gids.length;
                        //Don't forget this one
                        Arrays.sort(gids);
                        os.writeInt(this.doc.getDocId());
                        os.writeInt(gidsCount);
                        previousGID = 0;
                        for (int j = 0; j < gidsCount; j++) {
                            delta = gids[j] - previousGID;                            
                            os.writeLong(delta);
                            previousGID = gids[j];
                        } 
                    }
                }                
                //Store the data
                if (value == null) {
                    if (dbValues.put(searchKey, os.data()) == BFile.UNKNOWN_ADDRESS) {
                        LOG.error("Could not put index data for value '" +  searchKey + "'");  
                    }                    
                } else {
                    if (dbValues.update(value.getAddress(), searchKey, os.data()) == BFile.UNKNOWN_ADDRESS) {
                        LOG.error("Could not update index data for value '" +  searchKey + "'");  
                    }                    
                }  
            } catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);
                //TODO : return ?
            } catch (ReadOnlyException e) {
                LOG.warn("Read-only error on '" + dbValues.getFile().getName() + "'", e);                       
            } finally {
                lock.release();
            }            
        }
        pending.clear();
    }    
    
    /* Drop all index entries for the given collection.
	 * @see org.exist.storage.IndexGenerator#dropIndex(org.exist.collections.Collection)
	 */
    public void dropIndex(Collection collection) {        
        Value ref = new ElementValue(collection.getId());
        IndexQuery query = new IndexQuery(IndexQuery.TRUNC_RIGHT, ref);
        Lock lock = dbValues.getLock();
        try {
            lock.acquire(Lock.WRITE_LOCK);
            //TODO : flush ? -pb
            dbValues.removeAll(query);
        } catch (LockException e) {
            LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);
        } catch (BTreeException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            lock.release();
        }
    }
    
    /* Drop all index entries for the given document.
	 * @see org.exist.storage.IndexGenerator#dropIndex(org.exist.dom.DocumentImpl)
	 */
    //TODO : note that this is *not* this.doc -pb
    public void dropIndex(DocumentImpl document) throws ReadOnlyException {
        Value key;
        Value value;
        int gidsCount;
        long delta;        
        VariableByteArrayInput is;
        int storedDocId;
        boolean changed;        
        final short collectionId = document.getCollection().getId();
        final Value ref = new ElementValue(collectionId);
        final IndexQuery query = new IndexQuery(IndexQuery.TRUNC_RIGHT, ref);
        final Lock lock = dbValues.getLock();
        try {
            lock.acquire(Lock.WRITE_LOCK);
            ArrayList elements = dbValues.findKeys(query);
            for (int i = 0; i < elements.size(); i++) {
                changed = false;
                key = (Value) elements.get(i);
                value = dbValues.get(key);                
                is = new VariableByteArrayInput(value.getData());
                os.clear();                
                while (is.available() > 0) {
                    storedDocId = is.readInt();
                    gidsCount = is.readInt();                        
					if (storedDocId != document.getDocId()) {
					    // data are related to another document:
                        // copy them to any existing data
                        os.writeInt(storedDocId);
                        os.writeInt(gidsCount);
                        for (int j = 0; j < gidsCount; j++) {
                            delta = is.readLong();
                            os.writeLong(delta);
                        }
                    } else {
                        // data are related to our document:
                        // skip them                          
                        is.skip(gidsCount);
                        changed = true;
                    }
                }
                //Store new data, if relevant
                if (changed) {
                    if (os.data().size() == 0) {
                        //Well, nothing to store : remove the existing data
                        dbValues.remove(key);
                    } else {                      
                        if (dbValues.put(key, os.data()) == BFile.UNKNOWN_ADDRESS) {
                            LOG.warn("Could not put index data for value '" +  ref + "'");
                        }
                    }
                }
            }            
        } catch (LockException e) {
            LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);       
        } catch (TerminatedException e) {
            LOG.warn(e.getMessage(), e);            
        } catch (BTreeException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);    
        } finally {
            lock.release();
        }
    }
    
	/* (non-Javadoc)
	 * @see org.exist.storage.IndexGenerator#reindex(org.exist.dom.DocumentImpl, org.exist.dom.NodeImpl)
	 */
    //TODO : note that this is *not* this.doc -pb
    public void reindex(DocumentImpl document, NodeImpl node) {
        if (pending.size() == 0) 
            return;        
        Indexable indexable;
        //TODO : NativeElementIndex uses ArrayLists -pb
        LongLinkedList storedGIDList;
        LongLinkedList newGIDList;
        long[] gids;        
        int gidsCount;
        long storedGID;
        long previousGID;        
        long delta;        
        Value ref;
        Map.Entry entry;        
        VariableByteInput is;        
        int storedDocId;
        long address;
        final short collectionId = document.getCollection().getId();
        final Lock lock = dbValues.getLock();              
        for (Iterator i = pending.entrySet().iterator(); i.hasNext();) {
            //Compute a key for the value
            entry = (Map.Entry) i.next();
            indexable = (Indexable) entry.getKey();
            storedGIDList = (LongLinkedList) entry.getValue();
            ref = new Value(indexable.serialize(collectionId, caseSensitive)); 
            try {
                lock.acquire(Lock.WRITE_LOCK);
                is = dbValues.getAsStream(ref);
                os.clear();
                newGIDList = new LongLinkedList();
                //Does the value already has data in the index ?
                if (is != null) {                    
                    try {
                        while (is.available() > 0) {
                            storedDocId = is.readInt();
                            gidsCount = is.readInt();
                            if (storedDocId != document.getDocId()) {
                                // data are related to another document:
                                // append them to any existing data
                                os.writeInt(gidsCount);
                                os.writeInt(gidsCount);
                                is.copyTo(os, gidsCount);
                            } else {
                                // data are related to our document:
                                // feed the new list with the GIDs                                    
                                previousGID = 0;
                                for (int j = 0; j < gidsCount; j++) {
                                    delta = is.readLong();
                                    storedGID = previousGID + delta;    
                                    if (node == null) {
                                        if (document.getTreeLevel(storedGID) < document.reindexRequired())
                                            storedGIDList.add(storedGID);
                                    } else {
                                         if (!XMLUtil.isDescendant(document, node.getGID(), storedGID))
                                             //TO UNDERSTAND : what will these GIDs become ? -pb
                                             newGIDList.add(storedGID);
                                    }
                                    previousGID = storedGID;
                                }
                            }
                        }
                    } catch (EOFException e) {
                        //Is it expected ? Remove this block if not -pb
                        LOG.warn(e.getMessage(), e);
                    }
                }
                // append the new list to any existing data
                gids = storedGIDList.getData();
                gidsCount = gids.length;
                //Don't forget this one
                Arrays.sort(gids);
                os.writeInt(document.getDocId());
                os.writeInt(gidsCount);
                previousGID = 0;
                for (int j = 0; j < gidsCount; j++) {
                    delta = gids[j] - previousGID;                        
                    os.writeLong(delta);
                    previousGID = gids[j];
                }
                //Store the data
                if (is == null) {
                    //TODO : Should is be null, what will there be in os.data() ? -pb
                    if (dbValues.put(ref, os.data()) == BFile.UNKNOWN_ADDRESS) {
                        LOG.warn("Could not put index data for value '" +  ref + "'");
                    }
                } else {
                    address = ((BFile.PageInputStream) is).getAddress();
                    if (dbValues.update(address, ref, os.data()) == BFile.UNKNOWN_ADDRESS) {
                        LOG.warn("Could not update index data for value '" +  ref + "'");
                    }
                }
                
            } catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);     
                return;
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                is = null;
                //TODO : return ?
            } catch (ReadOnlyException e) {
                LOG.warn(e.getMessage(), e);  
                //TODO : return ?
            } finally {
                lock.release(Lock.WRITE_LOCK);
            }
        }
        pending.clear();
    }
    
	/** find
	 * @param relation binary operator used for the comparison
	 * @param value right hand comparison value */
    public NodeSet find(int relation, DocumentSet docs, NodeSet contextSet, Indexable value) 
            throws TerminatedException {
        final int idxOp =  checkRelationOp(relation);
        final NodeSet result = new ExtArrayNodeSet();
        final SearchCallback callback = new SearchCallback(docs, contextSet, result, true);
        final Lock lock = dbValues.getLock();
        for (Iterator iter = docs.getCollectionIterator(); iter.hasNext();) {
            Collection collection = (Collection) iter.next();
            short collectionId = collection.getId();
            byte[] key = value.serialize(collectionId, caseSensitive);
			IndexQuery query = new IndexQuery(idxOp, new Value(key));
            Value keyPrefix = computeKeyPrefix(value.getType(), collectionId);
			try {
				lock.acquire();
				try {
					dbValues.query(query, keyPrefix, callback);
				} catch (IOException e) {
                    LOG.error(e.getMessage(), e);
				} catch (BTreeException e) {
                    LOG.warn(e.getMessage(), e);
				}
			} catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);  
			} finally {
				lock.release();
			}
        }
        return result;
    }
    
    public NodeSet match(DocumentSet docs, NodeSet contextSet, String expr, int type)
            throws TerminatedException, EXistException {
        return match(docs, contextSet, expr, type, 0, true);
    }
    
	/** Regular expression search
	 * @param type  like type argument for {@link RegexMatcher} constructor
	 * @param flags like flags argument for {@link RegexMatcher} constructor
	 *  */
    public NodeSet match(DocumentSet docs, NodeSet contextSet, String expr, int type, int flags, boolean caseSensitiveQuery)
        throws TerminatedException, EXistException {
        
    	// if the regexp starts with a char sequence, we restrict the index scan to entries starting with
    	// the same sequence. Otherwise, we have to scan the whole index.
        StringValue startTerm = null;
        if (expr.startsWith("^") && caseSensitiveQuery == caseSensitive) {
        	StringBuffer term = new StringBuffer();
    		for (int j = 1; j < expr.length(); j++)
    			if (Character.isLetterOrDigit(expr.charAt(j)))
    				term.append(expr.charAt(j));
    			else
    				break;
    		if(term.length() > 0) {
                startTerm = new StringValue(term.toString());
                LOG.debug("Match will begin index scan at '" + startTerm + "'");
    		}
        }
        
		TermMatcher comparator = new RegexMatcher(expr, type, flags);
        NodeSet result = new ExtArrayNodeSet();
        RegexCallback callback = new RegexCallback(docs, contextSet, result, comparator);
        final Lock lock = dbValues.getLock();
        for (Iterator iter = docs.getCollectionIterator(); iter.hasNext();) {
			Collection collection = (Collection) iter.next();
			short collectionId = collection.getId();
			Value searchKey;
			if (startTerm != null) {
                byte[] key = startTerm.serialize(collectionId, caseSensitive);
                searchKey = new Value(key);
            } else {
                searchKey = computeKeyPrefix(Type.STRING, collectionId);                
				//key = new byte[3];
				//ByteConversion.shortToByte(collectionId, key, 0);
				//key[2] = (byte) Type.STRING;
			}
			IndexQuery query = new IndexQuery(IndexQuery.TRUNC_RIGHT, searchKey);
			try {
				lock.acquire();
				dbValues.query(query, callback);
            } catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);  
			} catch (IOException e) {
                LOG.error(e.getMessage(), e);
			} catch (BTreeException e) {
                LOG.error(e.getMessage(), e);			
			} finally {
				lock.release();
			}
        }
        return result;
    }
    
    public ValueOccurrences[] scanIndexKeys(DocumentSet docs, NodeSet contextSet, Indexable start) {        
        final int type = ((Item) start).getType();
        final boolean stringType = Type.subTypeOf(type, Type.STRING);
        final IndexScanCallback cb = new IndexScanCallback(docs, contextSet, type);
        final Lock lock = dbValues.getLock();
        for (Iterator i = docs.getCollectionIterator(); i.hasNext();) {
            Collection collection = (Collection) i.next();
            short collectionId = collection.getId();
            int op = stringType ? IndexQuery.TRUNC_RIGHT : IndexQuery.GEQ;
            byte[] startKey = start.serialize(collectionId, caseSensitive);            
            IndexQuery query = new IndexQuery(op, new Value(startKey));            
            try {
                lock.acquire();               
                if (stringType)
                    dbValues.query(query, cb);
                else {
                    Value keyPrefix = computeKeyPrefix(start.getType(), collectionId);
                    dbValues.query(query, keyPrefix, cb);
                }
            } catch (LockException e) {
                LOG.warn("Failed to acquire lock for '" + dbValues.getFile().getName() + "'", e);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } catch (BTreeException e) {
                LOG.error(e.getMessage(), e);
            } catch (TerminatedException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                lock.release();
            }
        }
        Map map = cb.map;
        ValueOccurrences[] result = new ValueOccurrences[map.size()];        
        return (ValueOccurrences[]) map.values().toArray(result);
    }
    
    /**
     * Returns a search key for a collectionId/type combination.
     */
    private Value computeKeyPrefix(int type, short collectionId) {
        byte[] data = new byte[3];
        ByteConversion.shortToByte(collectionId, data, 0);
        data[2] = (byte) type;
        return new Value(data);
    }    

    protected int checkRelationOp(int relation) {
        int indexOp;
        switch(relation) {
        	case Constants.LT:
        	    indexOp = IndexQuery.LT;
        		break;
        	case Constants.LTEQ:
        	    indexOp = IndexQuery.LEQ;
        		break;
        	case Constants.GT:
        	    indexOp = IndexQuery.GT;
        		break;
        	case Constants.GTEQ:
        	    indexOp = IndexQuery.GEQ;
        		break;
        	case Constants.NEQ:
        	    indexOp = IndexQuery.NEQ;
        		break;
        	case Constants.EQ:
        	default:
        	    indexOp = IndexQuery.EQ;
        		break;
        }
        return indexOp;
    }    
    
    /**
     * @param xpathType
     * @param value
     * @return <code>null</null> if atomization fails or if the atomic value is not indexable.
     * Should we throw an exception instead ? -pb
     */
    private AtomicValue convertToAtomic(int xpathType, String value) {
        AtomicValue atomic = null;
        if (Type.subTypeOf(xpathType, Type.STRING)) {
            atomic = new StringValue(value);
        } else {
            try {
                atomic = new StringValue(value).convertTo(xpathType);
            } catch (XPathException e) {
                LOG.warn("Node value '" + value + "' cannot be converted to " + 
                        Type.getTypeName(xpathType));
                return null;
            }
        }
        if (atomic == null) {
            LOG.warn("Node value '" + Type.getTypeName(xpathType) + "(" + value + ")'" +
            " cannot be used as index key. It is null.");
            return null;
        }            
        if (!(atomic instanceof Indexable)) {
            LOG.warn("Node value '" + Type.getTypeName(xpathType) + "(" + value + ")'" +
            " cannot be used as index key. It does not implement " + Indexable.class.getName());
            return null;
        }
        return atomic;        
    }
    
    public String toString() {
        return this.getClass().getName() + " at "+ dbValues.getFile().getName() +
        " owned by " + broker.toString() + " (case sensitive = " + caseSensitive + ")";
    }
    
	/** TODO document */
    class SearchCallback implements BTreeCallback {
        
        DocumentSet docs;
        NodeSet contextSet;
        NodeSet result;
        boolean returnAncestor;
        
        public SearchCallback(DocumentSet docs, NodeSet contextSet, NodeSet result, boolean returnAncestor) {
            this.docs = docs;
            this.contextSet = contextSet;
            this.result = result;
            this.returnAncestor = returnAncestor;
        }
        
        /* (non-Javadoc)
         * @see org.dbxml.core.filer.BTreeCallback#indexInfo(org.dbxml.core.data.Value, long)
         */
        public boolean indexInfo(Value value, long pointer) throws TerminatedException {
            VariableByteInput is = null;
			try {
				is = dbValues.getAsStream(pointer);
			} catch (IOException e) {
				LOG.warn(e.getMessage(), e);
			}            
			if (is == null)
				return true;
            
            int storedDocId;            
            int gidsCount;
            long storedGID;
            long delta;  
            DocumentImpl storedDocument;        
            NodeProxy storedNode, parentNode;            
			try {
                int sizeHint = -1;
                while (is.available() > 0) {
                    storedDocId = is.readInt();
                	gidsCount = is.readInt();
                    storedDocument = docs.getDoc(storedDocId);
                    //TOUNDERSTAND : how could this be possible ? -pb
                	if (storedDocument == null) {
                        is.skip(gidsCount);
                        continue;                        
                    }               
                    //TOUNDERSTAND : does a null contextSet makes sense ? -pb                    
                	if (contextSet != null) { 
                	    if (!contextSet.containsDoc(storedDocument)) {
                	        is.skip(gidsCount);
                	        continue;
                        }
                        sizeHint = contextSet.getSizeHint(storedDocument);
                	}
                	//Process the nodes
                    storedGID = 0;                	
                	for (int j = 0; j < gidsCount; j++) {
                        delta = is.readLong();
                        storedGID = storedGID + delta;                        
                        storedNode = new NodeProxy(storedDocument, storedGID);						
                		// if a context set is specified, we can directly check if the
                		// matching node is a descendant of one of the nodes
                		// in the context set.
                		if (contextSet != null) {
                            if (returnAncestor) {
                                parentNode = contextSet.parentWithChild(storedNode, false, true, NodeProxy.UNKNOWN_NODE_LEVEL);
                                if (parentNode != null) 
                                    result.add(parentNode, sizeHint);
                			} else
                                result.add(storedNode, sizeHint);
                		// otherwise, we add all nodes without check
                		} else {
                			result.add(storedNode, sizeHint);
                		}
                	}
                }
			} catch (EOFException e) {
			    // EOF is expected here
            } catch (IOException e) {                
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }
    
	/** TODO document */
    private class RegexCallback extends SearchCallback {
    	
    	private TermMatcher matcher;
    	private XMLString key = new XMLString(128);
        
    	public RegexCallback(DocumentSet docs, NodeSet contextSet, NodeSet result, TermMatcher matcher) {
    		super(docs, contextSet, result, true);
    		this.matcher = matcher;
    	}
    	
    	/**
		 * @see org.exist.storage.NativeValueIndex.SearchCallback#indexInfo(org.dbxml.core.data.Value, long)
		 */
		public boolean indexInfo(Value value, long pointer) throws TerminatedException {
            key.reuse();
            UTF8.decode(value.data(), value.start() + 3, value.getLength() - 3, key);
			if(matcher.matches(key)) {
				super.indexInfo(value, pointer);
			}
			return true;
		}
    }
    
    private final class IndexScanCallback implements BTreeCallback{
        
        private DocumentSet docs;
        private NodeSet contextSet;
        private Map map = new TreeMap();
        private int type;
        
        IndexScanCallback(DocumentSet docs, NodeSet contextSet, int type) {
            this.docs = docs;
            this.contextSet = contextSet;
            this.type = type;
        }
        
        /* (non-Javadoc)
         * @see org.dbxml.core.filer.BTreeCallback#indexInfo(org.dbxml.core.data.Value, long)
         */
        public boolean indexInfo(Value key, long pointer) throws TerminatedException {
            
            AtomicValue atomic;
            try {
                atomic = ValueIndexFactory.deserialize(key.data(), key.start(), key.getLength());
                if (atomic.getType() != type)
                    return false;
            } catch (EXistException e) {
                LOG.warn(e.getMessage(), e);
                return true;
            }
            
            VariableByteInput is = null;
            try {
                is = dbValues.getAsStream(pointer);
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);
            }
            if (is == null)
                return true;
            
            int storedDocId;
            int gidsCount;
            long storedGID; 
            long delta;       
            DocumentImpl storedDocument;                
            boolean docAdded;
            ValueOccurrences oc = (ValueOccurrences) map.get(atomic);
            try {
                while (is.available() > 0) {
                    storedDocId = is.readInt();
                    gidsCount = is.readInt();
                    storedDocument = docs.getDoc(storedDocId);                    
                    if (storedDocument == null) {
                        is.skip(gidsCount);
                        continue;
                    }
                    docAdded = false;
                    storedGID = 0;                    
                    for (int j = 0; j < gidsCount; j++) {
                        delta = is.readLong();
                        storedGID = storedGID + delta;
                        //TODO : what if contextSet == null ? -pb
                        //See above where we have this behaviour :
                        //otherwise, we add all nodes without check
                        if (contextSet != null) {
                            if (contextSet.parentWithChild(storedDocument, storedGID, false, true) != null) {
                                if (oc == null) {
                                    oc = new ValueOccurrences(atomic);
                                    map.put(atomic, oc);
                                }
                                if (!docAdded) {
                                    oc.addDocument(storedDocument);
                                    docAdded = true;
                                }
                                oc.addOccurrences(1);
                            }
                        }
                    }
                }
            } catch(EOFException e) {
                //Is it expected ? -pb
                LOG.warn(e.getMessage(), e);
            } catch(IOException e) {
                LOG.error(e.getMessage(), e);
            }
            return true;
        }
    }

    public void storeAttribute(AttrImpl node, NodePath currentPath, boolean fullTextIndexSwitch) {
    	// TODO Auto-generated method stub    	
    }
    
    public void storeText(TextImpl node, NodePath currentPath, boolean fullTextIndexSwitch) {
    	// TODO Auto-generated method stub    	
    }
    
    public void startElement(ElementImpl impl, NodePath currentPath, boolean index) {
    	// TODO Auto-generated method stub    	
    }
    
    public void endElement(int xpathType, ElementImpl node, String content) {
    	// TODO Auto-generated method stub    	
    }
    
    public void removeElement(ElementImpl node, NodePath currentPath, String content) {
    	// TODO Auto-generated method stub    	
    }

}
