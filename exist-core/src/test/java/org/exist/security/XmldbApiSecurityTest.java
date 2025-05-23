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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exist.security.internal.aider.ACEAider;
import org.exist.security.internal.aider.GroupAider;
import org.exist.security.internal.aider.UserAider;
import org.exist.test.ExistWebServer;
import org.exist.xmldb.UserManagementService;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(Parameterized.class)
public class XmldbApiSecurityTest extends AbstractApiSecurityTest {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    @Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }
    
    @Parameter
    public String apiName;
    
    @Parameter(value = 1)
    public String baseUri;

    private final String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @Override
    protected void createCol(final String collectionName, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            CollectionManagementService cms = col.getService(CollectionManagementService.class);
            cms.createCollection(collectionName);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void removeCol(final String collectionName, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            final Collection child = col.getChildCollection(collectionName);
            if(child != null) {
                child.close();
                final CollectionManagementService cms = col.getService(CollectionManagementService.class);
                cms.removeCollection(collectionName);
            }
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
      
    @Override
    protected void chownCol(final String collectionUri, final String owner_uid, final String group_gid, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + collectionUri, uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);
            
            ums.chown(ums.getAccount(owner_uid), group_gid);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void chmodCol(final String collectionUri, final String mode, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + collectionUri, uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);

            ums.chmod(mode);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void chownRes(final String resourceUri, final String owner_uid, final String group_gid, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + getCollectionUri(resourceUri), uid, pwd);
            
            final Resource resource = col.getResource(getResourceName(resourceUri));
            final UserManagementService ums = col.getService(UserManagementService.class);
            
            ums.chown(resource, ums.getAccount(owner_uid), group_gid);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void chmodRes(final String resourceUri, final String mode, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + getCollectionUri(resourceUri), uid, pwd);
            
            final Resource resource = col.getResource(getResourceName(resourceUri));
            final UserManagementService ums = col.getService(UserManagementService.class);
            ums.chmod(resource, mode);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }

    @Override
    protected void addCollectionUserAce(final String collectionUri, final String user_uid, final String mode, final boolean allow, final String uid, final String pwd) throws ApiException {
        Collection parentCol = null;
        Collection subCol = null;
        try {
            final String parentColUri = collectionUri.substring(0, collectionUri.lastIndexOf('/'));
            final String subColName = collectionUri.substring(collectionUri.lastIndexOf('/') + 1);

            parentCol = DatabaseManager.getCollection(getBaseUri() + parentColUri, uid, pwd);
            final UserManagementService ums = parentCol.getService(UserManagementService.class);

            final Permission subColPermissions = ums.getSubCollectionPermissions(parentCol, subColName);

            subCol = DatabaseManager.getCollection(getBaseUri() + collectionUri, uid, pwd);
            final List<ACEAider> aces = new ArrayList<>();
            final ACEAider ace = new ACEAider(allow ? ACLPermission.ACE_ACCESS_TYPE.ALLOWED : ACLPermission.ACE_ACCESS_TYPE.DENIED, ACLPermission.ACE_TARGET.USER, user_uid, SimpleACLPermission.aceSimpleSymbolicModeToInt(mode));
            aces.add(ace);
            ums.setPermissions(subCol, subColPermissions.getOwner().getName(), subColPermissions.getGroup().getName(), subColPermissions.getMode(), aces);

        } catch(final XMLDBException | PermissionDeniedException e) {
            throw new ApiException(e);
        } finally {
            if(subCol != null) {
                try {
                    subCol.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }

            if(parentCol != null) {
                try {
                    parentCol.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }

    @Override
    protected String getXmlResourceContent(final String resourceUri, final String uid, final String pwd) throws ApiException {
        
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + getCollectionUri(resourceUri), uid, pwd);
            final Resource resource = col.getResource(getResourceName(resourceUri));
            return (String)resource.getContent();
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }

    @Override
    protected void removeAccount(final String account_uid, final String uid, final String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);

            final Account acct = ums.getAccount(account_uid);
            if(acct != null){
                ums.removeAccount(acct);
            }
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void removeGroup(String group_uid, String uid, String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);

            final Group grp = ums.getGroup(group_uid);
            if(grp != null){
                ums.removeGroup(grp);
            }
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }

    @Override
    protected void createAccount(String account_uid, String account_pwd, String group_uid, String uid, String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);

            final Group group = ums.getGroup(group_uid);

            final Account user = new UserAider(account_uid, group);
            user.setPassword(account_pwd);
            ums.addAccount(user);
            
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void createGroup(String group_uid, String uid, String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + "/db", uid, pwd);
            final UserManagementService ums = col.getService(UserManagementService.class);

            Group group = new GroupAider("exist", group_uid);
            ums.addGroup(group);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }

    @Override
    protected void createXmlResource(String resourceUri, String content, String uid, String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + getCollectionUri(resourceUri), uid, pwd);
            Resource resource = col.createResource(getResourceName(resourceUri), XMLResource.class);
            resource.setContent(content);
            col.storeResource(resource);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
    
    @Override
    protected void createBinResource(String resourceUri, byte[] content, String uid, String pwd) throws ApiException {
        Collection col = null;
        try {
            col = DatabaseManager.getCollection(getBaseUri() + getCollectionUri(resourceUri), uid, pwd);
            Resource resource = col.createResource(getResourceName(resourceUri), BinaryResource.class);
            resource.setContent(content);
            col.storeResource(resource);
        } catch(final XMLDBException xmldbe) {
            throw new ApiException(xmldbe);
        } finally {
            if(col != null) {
                try {
                    col.close();
                } catch (final XMLDBException xmldbe) {
                    throw new ApiException(xmldbe);
                }
            }
        }
    }
}
