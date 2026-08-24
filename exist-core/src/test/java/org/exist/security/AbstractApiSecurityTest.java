/*
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
package org.exist.security;

import static org.exist.TestUtils.ADMIN_DB_PWD;
import static org.exist.TestUtils.ADMIN_DB_USER;
import static org.junit.jupiter.api.Assertions.*;

import org.exist.storage.BrokerPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public abstract class AbstractApiSecurityTest {
    
    protected final static String TEST_COLLECTION1_NAME = "securityTest1";
    
    protected final static String TEST_COLLECTION1 = "/db/" + TEST_COLLECTION1_NAME;
    
    protected final static String TEST_XML_DOC1_NAME = "test.xml";
    protected final static String TEST_XML_DOC1 = TEST_COLLECTION1 + "/" + TEST_XML_DOC1_NAME;
    protected final static String TEST_XML_DOC1_CONTENT = "<test/>";
    
    protected final static String TEST_BIN_DOC1_NAME = "test.bin";
    protected final static String TEST_BIN_DOC1 = TEST_COLLECTION1 + "/" + TEST_BIN_DOC1_NAME;
    protected final static byte[] TEST_BIN_DOC1_CONTENT = "binary-test".getBytes();
    
    protected final static String TEST_USER1_UID = "test1";
    protected final static String TEST_USER1_PWD = TEST_USER1_UID;
    
    protected final static String TEST_USER2_UID = "test2";
    protected final static String TEST_USER2_PWD = TEST_USER2_UID;
    
    protected final static String TEST_GROUP_GID = "group1";
    protected final static String TEST_GROUP_PWD = TEST_GROUP_GID;
    
    @Test
    public void canReadXmlResourceWithOnlyExecutePermissionOnParentCollection(final BrokerPool pool) throws ApiException {
        chmodCol(pool, TEST_COLLECTION1, "--x------", TEST_USER1_UID, TEST_USER1_PWD);
        
        final String content = getXmlResourceContent(pool, TEST_XML_DOC1, TEST_USER1_UID, TEST_USER1_PWD);
        assertEquals(TEST_XML_DOC1_CONTENT, content);
    }
    
    @Test
    public void cannotReadXmlResourceWithoutExecutePermissionOnParentCollection(final BrokerPool pool) throws ApiException {
        chmodCol(pool, TEST_COLLECTION1, "rw-------", TEST_USER1_UID, TEST_USER1_PWD);
        
        try {
            final String content = getXmlResourceContent(pool, TEST_XML_DOC1, TEST_USER1_UID, TEST_USER1_PWD);
            fail("Expected READ collection denied!");
        } catch(final ApiException ae) {
            //do nothing <-- expected exception
        }
    }

    @Test
    public void cannotReadXmlResourceWithoutExecutePermissionOnParentCollectionViaACE(final BrokerPool pool) throws ApiException {
        chmodCol(pool, TEST_COLLECTION1, "rwx------", TEST_USER1_UID, TEST_USER1_PWD);

        addCollectionUserAce(pool, TEST_COLLECTION1, TEST_USER1_UID, "--x", false,  TEST_USER1_UID, TEST_USER1_PWD);

        try {
            final String content = getXmlResourceContent(pool, TEST_XML_DOC1, TEST_USER1_UID, TEST_USER1_PWD);
            fail("Expected READ collection denied!");
        } catch(final ApiException ae) {
            //do nothing <-- expected exception
        }
    }
    
    protected abstract void createCol(BrokerPool pool, String collectionName, String uid, String pwd) throws ApiException;
    protected abstract void removeCol(BrokerPool pool, String collectionName, String uid, String pwd) throws ApiException;
    
    protected abstract void chownCol(BrokerPool pool, String collectionUri, String owner_uid, String group_gid, String uid, String pwd) throws ApiException;
    protected abstract void chmodCol(BrokerPool pool, String collectionUri, String mode, String uid, String pwd) throws ApiException;
    protected abstract void chmodRes(BrokerPool pool, String resourceUri, String mode, String uid, String pwd) throws ApiException;
    protected abstract void chownRes(BrokerPool pool, String resourceUri, String owner_uid, String group_gid, String uid, String pwd) throws ApiException;

    protected abstract void addCollectionUserAce(BrokerPool pool, String collectionUri, String user_uid, String mode, boolean allow, String uid, String pwd) throws ApiException;
    
    protected abstract String getXmlResourceContent(BrokerPool pool, String resourceUri, String uid, String pwd) throws ApiException;
    
    protected abstract void removeAccount(BrokerPool pool, String account_uid, String uid, String pwd) throws ApiException;
    protected abstract void removeGroup(BrokerPool pool, String group_gid, String uid, String pwd) throws ApiException;
    protected abstract void createAccount(BrokerPool pool, String account_uid, String account_pwd, String group_uid, String uid, String pwd) throws ApiException;
    protected abstract void createGroup(BrokerPool pool, String group_gid, String uid, String pwd) throws ApiException;
    protected abstract void createXmlResource(BrokerPool pool, String resourceUri, String content, String uid, String pwd) throws ApiException;
    protected abstract void createBinResource(BrokerPool pool, String resourceUri, byte[] content, String uid, String pwd) throws ApiException;
    
    
    @BeforeEach
    public void setup(final BrokerPool pool) throws ApiException {
        
        chmodCol(pool, "/db", "rwxr-xr-x", ADMIN_DB_USER, ADMIN_DB_PWD); //ensure /db is always 755
        
        removeAccount(pool, TEST_USER1_UID, ADMIN_DB_USER, ADMIN_DB_PWD);
        removeGroup(pool, TEST_USER1_UID, ADMIN_DB_USER, ADMIN_DB_PWD);  // remove personal group!
        removeAccount(pool, TEST_USER2_UID, ADMIN_DB_USER, ADMIN_DB_PWD);
        removeGroup(pool, TEST_USER2_UID, ADMIN_DB_USER, ADMIN_DB_PWD);  // remove personal group!

        removeGroup(pool, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
        
        createGroup(pool, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
        createAccount(pool, TEST_USER1_UID, TEST_USER1_PWD, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
        createAccount(pool, TEST_USER2_UID, TEST_USER2_PWD, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);

        // create a collection /db/securityTest as user "test1"
        createCol(pool, TEST_COLLECTION1_NAME, ADMIN_DB_USER, ADMIN_DB_PWD);
        // pass ownership to test1
        chownCol(pool, TEST_COLLECTION1, TEST_USER1_UID, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
        chmodCol(pool, TEST_COLLECTION1, "rwxrwx---", ADMIN_DB_USER, ADMIN_DB_PWD);
        
        createXmlResource(pool, TEST_XML_DOC1, TEST_XML_DOC1_CONTENT, ADMIN_DB_USER, ADMIN_DB_PWD);
        chmodRes(pool, TEST_XML_DOC1, "rwxrwx---", ADMIN_DB_USER, ADMIN_DB_PWD);
        chownRes(pool, TEST_XML_DOC1, TEST_USER1_UID, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
        
        createBinResource(pool, TEST_BIN_DOC1, TEST_BIN_DOC1_CONTENT, ADMIN_DB_USER, ADMIN_DB_PWD);
        chmodRes(pool, TEST_BIN_DOC1, "rwxrwx---", ADMIN_DB_USER, ADMIN_DB_PWD);
        chownRes(pool, TEST_BIN_DOC1, TEST_USER1_UID, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
    }

    @AfterEach
    public void cleanup(final BrokerPool pool) throws ApiException {
        removeCol(pool, TEST_COLLECTION1_NAME, ADMIN_DB_USER, ADMIN_DB_PWD);

        removeAccount(pool, TEST_USER1_UID, ADMIN_DB_USER, ADMIN_DB_PWD);
        removeAccount(pool, TEST_USER2_UID, ADMIN_DB_USER, ADMIN_DB_PWD);
        removeGroup(pool, TEST_GROUP_GID, ADMIN_DB_USER, ADMIN_DB_PWD);
    }
    
    protected String getCollectionUri(String resourceUri) {
        return resourceUri.substring(0, resourceUri.lastIndexOf("/"));
    }
    
    protected String getResourceName(String resourceUri) {
        return resourceUri.substring(resourceUri.lastIndexOf("/") + 1);
    }
}
