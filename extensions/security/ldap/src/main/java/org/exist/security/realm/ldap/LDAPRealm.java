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
package org.exist.security.realm.ldap;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import com.evolvedbinary.j8fu.function.BiFunctionE;
import com.evolvedbinary.j8fu.function.Function3E;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.config.Configuration;
import org.exist.config.annotation.*;
import org.exist.security.AXSchemaType;
import org.exist.security.AbstractAccount;
import org.exist.security.AbstractRealm;
import org.exist.security.Account;
import org.exist.security.AuthenticationException;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SchemaType;
import org.exist.security.Subject;
import org.exist.security.internal.SecurityManagerImpl;
import org.exist.security.internal.SubjectAccreditedImpl;
import org.exist.security.internal.aider.GroupAider;
import org.exist.security.internal.aider.UserAider;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.security.realm.ldap.LDAPUtils.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 * @author <a href="mailto:adam@exist-db.org">Adam Retter</a>
 */
@ConfigurationClass("realm") //TODO: id = LDAP
public class LDAPRealm extends AbstractRealm {

    private static final Logger LOG = LogManager.getLogger(LDAPRealm.class);

    private static final Pattern SID_PATTERN = Pattern.compile("S-[0-9]+-[0-9]+(-[0-9]+)+");

    @ConfigurationFieldAsAttribute("id")
    public static String ID = "LDAP";

    @ConfigurationFieldAsAttribute("version")
    public static final String version = "1.0";

    @ConfigurationFieldAsAttribute("principals-are-case-insensitive")
    private boolean principalsAreCaseInsensitive;

    @ConfigurationFieldAsElement("context")
    protected LDAPContextFactory ldapContextFactory;

    public LDAPRealm(final SecurityManagerImpl sm, final Configuration config) {
        super(sm, config);
    }

    protected LDAPContextFactory ensureContextFactory() {
        if (this.ldapContextFactory == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No LdapContextFactory specified - creating a default instance.");
            }
            this.ldapContextFactory = new LDAPContextFactory(configuration);
        }
        return this.ldapContextFactory;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void start(final DBBroker broker, final Txn transaction) throws EXistException {
        super.start(broker, transaction);
    }

    /**
     * When principals-are-case-insensitive="true"
     * is set in the Realm security config then
     * the provided principal will be converted
     * to lower case.
     *
     * @param principal the principal
     *
     * @return the principal which may have been lower-cased.
     */
    private @Nullable String ensureCase(@Nullable final String principal) {
        if (principal == null) {
            return null;
        }

        if (principalsAreCaseInsensitive) {
            return principal.toLowerCase();
        }

        return principal;
    }

    @Override
    public Subject authenticate(final String username, final Object credentials) throws AuthenticationException {
        final String name = ensureCase(username);

        // Binds using the username and password provided by the user.
        LdapContext ctx = null;
        try {
            ctx = getContextWithCredentials(Optional.of(Tuple(name, String.valueOf(credentials))));

            final AbstractAccount account = (AbstractAccount) getAccount(ctx, name);
            if (account == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Account '{}' can not be found.", name);
                }
                throw new AuthenticationException(
                        AuthenticationException.ACCOUNT_NOT_FOUND,
                        "Account '" + name + "' can not be found.");
            }

            return new AuthenticatedLdapSubjectAccreditedImpl(account, ctx, String.valueOf(credentials));

        } catch (final NamingException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
            if (e instanceof javax.naming.AuthenticationException) {
                throw new AuthenticationException(AuthenticationException.ACCOUNT_NOT_FOUND, e.getMessage());
            } else {
                throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, e.getMessage());
            }

        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    /**
     * Find all the groups for an LDAP user.
     *
     * Groups have a `member` attribute, so we look for groups that have an attribute that matches member=user.dn
     *
     * @param ctx the LDAP context
     * @param broker the database broker
     * @param ldapUser details of the LDAP user to find group membership for
     *
     * @return the list of groups that the user is a member of
     *
     * @throws NamingException if an error occurs whilst querying LDAP
     */
    private List<Group> getGroupMembershipForLdapUser(final LdapContext ctx, final DBBroker broker, final SearchResult ldapUser) throws NamingException {
        final LDAPSearchContext search = ensureContextFactory().getSearch();

        @Nullable final String accountDnAttrName = search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.DN);
        if (accountDnAttrName == null) {
            LOG.warn("Expected a 'dn' attribute to be specified in the LDAP Realm config for search/account, but it is missing. Unable to find groups for a user name!");
            return Collections.emptyList();
        }

        @Nullable List<Group> memberOfGroups = null;

        final String userDistinguishedName = (String) ldapUser.getAttributes().get(accountDnAttrName).get();
        final List<String> memberOfGroupNames = findGroupnamesForUserDistinguishedName(ctx, userDistinguishedName);
        for (final String memberOfGroupName : memberOfGroupNames) {
            final Group group = getGroup(broker, ctx, memberOfGroupName);
            if (memberOfGroups == null) {
                memberOfGroups = new ArrayList<>();
            }
            memberOfGroups.add(group);
        }

        //TODO expand to a general method that rewrites the useraider based on the realTransformation
        if (memberOfGroups != null) {
            if (ensureContextFactory().getTransformationContext() != null) {
                final List<String> additionalGroupNames = ensureContextFactory().getTransformationContext().getAdditionalGroups();
                for (final String additionalGroupName : additionalGroupNames) {
                    final Group additionalGroup = getSecurityManager().getGroup(additionalGroupName);
                    if (additionalGroup != null) {
                        memberOfGroups.add(additionalGroup);
                    }
                }
            }
        }

        if (memberOfGroups == null) {
            return Collections.emptyList();
        }

        return memberOfGroups;
    }

    private List<SimpleEntry<SchemaType, String>> getMetadataForLdapUser(final SearchResult ldapUser) throws NamingException {
        final LDAPSearchAccount searchAccount = ensureContextFactory().getSearch().getSearchAccount();
        final Attributes userAttributes = ldapUser.getAttributes();
        return getMetadataForLdapPrincipal(searchAccount, userAttributes);
    }

    private List<SimpleEntry<SchemaType, String>> getMetadataForLdapGroup(final SearchResult ldapGroup) throws NamingException {
        final LDAPSearchGroup searchGroup = ensureContextFactory().getSearch().getSearchGroup();
        final Attributes groupAttributes = ldapGroup.getAttributes();
        return getMetadataForLdapPrincipal(searchGroup, groupAttributes);
    }

    private List<SimpleEntry<SchemaType, String>> getMetadataForLdapPrincipal(final AbstractLDAPSearchPrincipal searchPrincipal, final Attributes attributes) throws NamingException {
        @Nullable List<SimpleEntry<SchemaType, String>> metadata = null;

        // Get SchemaType values
        for (final SchemaType schemaType : searchPrincipal.getMetadataSearchAttributeKeys()) {
            final String searchAttribute = searchPrincipal.getMetadataSearchAttribute(schemaType);
            if (attributes != null) {
                final Attribute attribute = attributes.get(searchAttribute);
                if (attribute != null) {
                    final String attributeValue = attribute.get().toString();
                    if (metadata == null) {
                        metadata = new ArrayList<>();
                    }
                    metadata.add(new SimpleEntry<>(schemaType, attributeValue));
                }
            }
        }

        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata;
    }

    public Account refreshAccountFromLdap(final Account account) throws PermissionDeniedException, AuthenticationException {
        final int UPDATE_NONE = 0;
        final int UPDATE_GROUP = 1;
        final int UPDATE_METADATA = 2;

        final Subject invokingUser = getSecurityManager().getCurrentSubject();

        if (!invokingUser.hasDbaRole() && invokingUser.getId() != account.getId()) {
            throw new PermissionDeniedException("You do not have permission to modify the account");
        }

        LdapContext ctx = null;
        try {
            ctx = getContext(invokingUser);
            final SearchResult ldapUser = findAccountByAccountName(ctx, account.getName());
            if (ldapUser == null) {
                throw new AuthenticationException(AuthenticationException.ACCOUNT_NOT_FOUND, "Could not find the account in the LDAP");
            }

            final LdapContext ctx2 = ctx;
            return executeAsSystemUser(broker -> {

                    int update = UPDATE_NONE;

                    //1) get the ldap group membership
                    final List<Group> memberOfGroups = getGroupMembershipForLdapUser(ctx2, broker, ldapUser);

                    //2) get the ldap primary group
                    final String primaryGroup = findGroupBySID(ctx2, getPrimaryGroupSID(ldapUser));

                    //append the ldap primaryGroup to the head of the ldap group list, and compare
                    //to the account group list
                    memberOfGroups.add(0, getGroup(broker, ctx2, primaryGroup));

                    final String accountGroups[] = account.getGroups();

                    if (!accountGroups[0].equals(ensureCase(primaryGroup))) {
                        update |= UPDATE_GROUP;
                    } else {
                        if (accountGroups.length != memberOfGroups.size()) {
                            update |= UPDATE_GROUP;
                        } else {
                            for (final String accountGroup : accountGroups) {
                                boolean found = false;

                                for (final Group memberOfGroup : memberOfGroups) {
                                    if (accountGroup.equals(ensureCase(memberOfGroup.getName()))) {
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    update |= UPDATE_GROUP;
                                    break;
                                }
                            }
                        }
                    }

                    //3) check metadata
                    final List<SimpleEntry<SchemaType, String>> ldapMetadatas = getMetadataForLdapUser(ldapUser);
                    final Set<SchemaType> accountMetadataKeys = account.getMetadataKeys();

                    if (accountMetadataKeys.size() != ldapMetadatas.size()) {
                        update |= UPDATE_METADATA;
                    } else {
                        for (SchemaType accountMetadataKey : accountMetadataKeys) {
                            final String accountMetadataValue = account.getMetadataValue(accountMetadataKey);

                            boolean found = false;

                            for (final SimpleEntry<SchemaType, String> ldapMetadata : ldapMetadatas) {
                                if (accountMetadataKey.equals(ldapMetadata.getKey()) && accountMetadataValue.equals(ldapMetadata.getValue())) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                update |= UPDATE_METADATA;
                                break;
                            }
                        }
                    }

                    //update the groups?
                    if ((update & UPDATE_GROUP) == UPDATE_GROUP) {
                        try {
                            final Field fld = account.getClass().getSuperclass().getDeclaredField("groups");
                            fld.setAccessible(true);
                            fld.set(account, memberOfGroups);
                        } catch (final NoSuchFieldException | IllegalAccessException nsfe) {
                            throw new EXistException(nsfe.getMessage(), nsfe);
                        }
                    }

                    //update the metdata?
                    if ((update & UPDATE_METADATA) == UPDATE_METADATA) {
                        account.clearMetadata();
                        for (final SimpleEntry<SchemaType, String> ldapMetadata : ldapMetadatas) {
                            account.setMetadataValue(ldapMetadata.getKey(), ldapMetadata.getValue());
                        }
                    }

                    if (update != UPDATE_NONE) {
                        final boolean updated = getSecurityManager().updateAccount(account);
                        if (!updated) {
                            LOG.error("Could not update account");
                        }
                    }

                    return account;
            });
        } catch (final NamingException | EXistException ne) {
            throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage(), ne);
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    private Account createAccountInDatabase(final LdapContext ctx, final String username, final SearchResult ldapUser, final String primaryGroupName) throws AuthenticationException {

        //final LDAPSearchAccount searchAccount = ensureContextFactory().getSearch().getSearchAccount();

        try {
            return executeAsSystemUser(broker -> {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Saving account '{}'.", username);
                    }

                    // get (or create) the primary group if it doesn't exist
                    final Group primaryGroup = getGroup(broker, ctx, primaryGroupName);

                    //get (or create) member groups
                    /*LDAPSearchContext search = ensureContextFactory().getSearch();
                    String userDistinguishedName = (String)ldapUser.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.DN)).get();
                    List<String> memberOf_groupNames = findGroupnamesForUserDistinguishedName(invokingUser, userDistinguishedName);

                    List<Group> memberOf_groups = new ArrayList<Group>();
                    for(String memberOf_groupName : memberOf_groupNames) {
                        memberOf_groups.add(getGroup(invokingUser, memberOf_groupName));
                    }*/

                    //create the user account
                    final UserAider userAider = new UserAider(ID, username, primaryGroup);

                    //add the member groups
                    for (final Group memberOfGroup : getGroupMembershipForLdapUser(ctx, broker, ldapUser)) {
                        userAider.addGroup(memberOfGroup);
                    }

                    //store any requested metadata
                    for (final SimpleEntry<SchemaType, String> metadata : getMetadataForLdapUser(ldapUser)) {
                        userAider.setMetadataValue(metadata.getKey(), metadata.getValue());
                    }

                    final Account account = getSecurityManager().addAccount(userAider);

                    //LDAPAccountImpl account = sm.addAccount(instantiateAccount(ID, username));

                    //TODO expand to a general method that rewrites the useraider based on the realTransformation
                    /*
                    boolean updatedAccount = false;
                    if(ensureContextFactory().getTransformationContext() != null){
                        List<String> additionalGroupNames = ensureContextFactory().getTransformationContext().getAdditionalGroups();
                        if(additionalGroupNames != null) {
                            for(String additionalGroupName : additionalGroupNames) {
                                Group additionalGroup = getSecurityManager().getGroup(invokingUser, ensureCase(additionalGroupName));
                                if(additionalGroup != null) {
                                    account.addGroup(additionalGroup);
                                    updatedAccount = true;
                                }
                            }
                        }
                    }
                    if(updatedAccount) {
                        boolean updated = getSecurityManager().updateAccount(invokingUser, account);
                        if(!updated) {
                            LOG.error("Could not update account");
                        }
                    }*/

                    return account;
            });
        } catch (final Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e);
            }
            throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface LDAPFunction<R> extends Function3E<DBBroker, R, EXistException, PermissionDeniedException, NamingException> {}

    private <R> R executeAsSystemUser(final LDAPFunction<R> ldapFunction) throws EXistException, PermissionDeniedException, NamingException {
        try (final DBBroker broker = getDatabase().get(Optional.of(getSecurityManager().getSystemSubject()))) {
            //perform as SYSTEM user
            return ldapFunction.apply(broker);
        }
    }

    private Group createGroupInDatabase(final String groupName, final SearchResult ldapGroup) throws AuthenticationException {
        try {
            return executeAsSystemUser(broker -> {
                try {
                    return createGroupInDatabase(broker, groupName, ldapGroup);
                } catch (final AuthenticationException ae) {
                    LOG.error(ae.getMessage(), ae);
                    return null;
                }
            });
        } catch (final Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e);
            }
            throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, e.getMessage(), e);
        }
    }

    private Group createGroupInDatabase(final DBBroker broker, final String groupName, final SearchResult ldapGroup) throws AuthenticationException {
        final GroupAider groupAider = new GroupAider(ID, groupName);
        try {

            // set any group managers
            if (ensureContextFactory().getTransformationContext() != null) {
                final List<String> additionalGroupManagers = ensureContextFactory().getTransformationContext().getAdditionalGroupManagers();
                for (String additionalGroupManagerName : additionalGroupManagers) {

                    if ("default".equals(additionalGroupManagerName)) {
                        // default user should be taken from the default-username of the LDAP Realm Security Config
                        additionalGroupManagerName = ensureContextFactory().getSearch().getDefaultUsername();
                    }

                    // TODO(AR) adding group managers can cause a StackOverflowError at present
//                    // NOTE(AR) we need to make sure we are not requesting an account to be a group manager of a group that we are creating as part of creating an account, that is to say that a user cannot be a manager of their primary group
//                    @Nullable final PrincipalState additionalGroupManagerState = getAccountPrincipalState(additionalGroupManagerName);
//                    @Nullable final Account additionalGroupManager;
//                    if (PrincipalState.CREATING == additionalGroupManagerState) {
//                        // skip non-persistent entries
//                        additionalGroupManager = null;
//                    } else {
//                        additionalGroupManager = getSecurityManager().getAccount(additionalGroupManagerName);
//                    }
//
//                    if (additionalGroupManager != null) {
//                        groupAider.addManager(additionalGroupManager);
//                    }
                }
            }

            // store any requested metadata
            for (final SimpleEntry<SchemaType, String> metadata : getMetadataForLdapGroup(ldapGroup)) {
                groupAider.setMetadataValue(metadata.getKey(), metadata.getValue());
            }

            return getSecurityManager().addGroup(broker, groupAider);

        } catch (final Exception e) {
            throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, e.getMessage(), e);
        }
    }

    private LdapContext getContext(@Nullable final Subject invokingUser) throws NamingException {
        return getContext(Optional.ofNullable(invokingUser));
    }

    private LdapContext getContext(final Optional<Subject> invokingUser) throws NamingException {
        final Optional<Tuple2<String, String>> credentials = invokingUser
                .filter(subject -> subject instanceof AuthenticatedLdapSubjectAccreditedImpl)
                .map(subject -> (AuthenticatedLdapSubjectAccreditedImpl) subject)
                .map(subject -> Tuple(subject.getUsername(), subject.getAuthenticatedCredentials()));

        return getContextWithCredentials(credentials);
    }

    /**
     * Gets an LDAP Context for the provided user details,
     * or if none are provided the default configured
     * credentials are used.
     *
     * @param optCredentials Explicit credentials
     * @return An LDAP Context
     */
    private LdapContext getContextWithCredentials(final Optional<Tuple2<String, String>> optCredentials) throws NamingException {
        final LDAPContextFactory ctxFactory = ensureContextFactory();
        final Tuple2<String, String> credentials = optCredentials.orElseGet(() -> defaultCredentials(ctxFactory));
        return ctxFactory.getLdapContext(credentials._1, credentials._2, null);
    }

    private Tuple2<String, String> defaultCredentials(final LDAPContextFactory ctxFactory) {
        final LDAPSearchContext searchCtx = ctxFactory.getSearch();
        return Tuple(searchCtx.getDefaultUsername(), searchCtx.getDefaultPassword());
    }

    @Override
    public final synchronized @Nullable Account getAccount(String name) {
        name = ensureCase(name);

        // first attempt to get the cached account
        final Account account = super.getAccount(name);
        if (account != null) {
            return account;
        }

        // second find the account in LDAP
        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getDatabase().getActiveBroker().getCurrentSubject());
            return getAccount(ctx, name);
        } catch (final NamingException ne) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(ne.getMessage(), ne);
            }
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return null;
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    private synchronized @Nullable Account getAccount(final LdapContext ctx, String name) {
        name = ensureCase(name);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Get request for account '{}'.", name);
        }

        // first attempt to get the cached account
        final Account account = super.getAccount(name);
        if (account != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cached used.");
            }
            //XXX: synchronize with LDAP
            return account;
        }

        // if the account is not cached, we should try and find it in LDAP and cache it if it exists
        try {
            //do the lookup
            @Nullable final SearchResult ldapUser = findAccountByAccountName(ctx, name);

            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP search return '{}'.", ldapUser);
            }

            if (ldapUser == null) {
                return null;
            }

            //found a user from ldap so cache them and return
            try {
                final String primaryGroupSID = getPrimaryGroupSID(ldapUser);
                @Nullable final String primaryGroup = findGroupBySID(ctx, primaryGroupSID);

                if (primaryGroup == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("LDAP search for primary group by SID '{}', found nothing'.", primaryGroupSID);
                    }
                    return null;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("LDAP search for primary group by SID '{}', found '{}'.", primaryGroupSID, primaryGroup);
                }

                if (isFullyQualified(name)) {
                    // adjust name to make it unqualified for the database
                    name = extractPrincipalFromDn(name);
                }

                return createAccountInDatabase(ctx, name, ldapUser, ensureCase(primaryGroup));
                //registerAccount(acct); //TODO do we need this
            } catch (final AuthenticationException ae) {
                LOG.error(ae.getMessage(), ae);
                return null;
            }

        } catch (final NamingException ne) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(ne.getMessage(), ne);
            }
            //LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return null;
        }
    }

    @Override
    public boolean hasAccount(final Account account) {
        return hasAccount(account.getName());
    }

    @Override
    public boolean hasAccount(final String name) {
        @Nullable final PrincipalState accountState = getAccountPrincipalState(name);

        // NOTE(AR) We need to check here whether we are already in the process of creating it in the database, we then use that to prevent creating it recursively through calls to getAccount -> createAccountInDatabase
        if (PrincipalState.CREATING == accountState) {
            // skip non-persistent entries
            return false;
        }

        if (PrincipalState.PERSISTENT == accountState) {
            // we already have it
            return true;
        }

        return getAccount(name) != null;
    }

    private @Nullable PrincipalState getAccountPrincipalState(final String accountName) {
        return usersByName.read(principalDb -> {
            @Nullable final Tuple2<PrincipalState, Optional<Account>> principalEntry = principalDb.get(accountName);
            if (principalEntry == null) {
                return null;
            }

            return principalEntry._1;
        });
    }

    /**
     * The binary data is in form:
     * byte[0] - revision level
     * byte[1] - count of sub-authorities
     * byte[2-7] - 48 bit authority (big-endian)
     * and then count x 32 bit sub authorities (little-endian)
     *
     * The String value is: S-Revision-Authority-SubAuthority[n]...
     *
     * http://forums.oracle.com/forums/thread.jspa?threadID=1155740&tstart=0
     */
    private static String decodeSID(final byte[] sid) {

        final StringBuilder strSid = new StringBuilder("S-");

        // get version
        final int revision = sid[0];
        strSid.append(Integer.toString(revision));

        //next byte is the count of sub-authorities
        final int countSubAuths = sid[1] & 0xFF;

        //get the authority
        long authority = 0;
        //String rid = "";
        for (int i = 2; i <= 7; i++) {
            authority |= ((long) sid[i]) << (8 * (5 - (i - 2)));
        }
        strSid.append("-");
        strSid.append(Long.toHexString(authority));

        //iterate all the sub-auths
        int offset = 8;
        int size = 4; //4 bytes for each sub auth
        for (int j = 0; j < countSubAuths; j++) {
            long subAuthority = 0;
            for (int k = 0; k < size; k++) {
                subAuthority |= (long) (sid[offset + k] & 0xFF) << (8 * k);
            }

            strSid.append("-");
            strSid.append(subAuthority);

            offset += size;
        }

        return strSid.toString();
    }

    private String getPrimaryGroupSID(final SearchResult ldapUser) throws NamingException {
        final LDAPSearchContext search = ensureContextFactory().getSearch();

        @Nullable final String userObjectSidAttrName = search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.OBJECT_SID);
        String strUserObjectSid = null;
        if (userObjectSidAttrName != null) {
            @Nullable final Attribute userObjectSidAttr = ldapUser.getAttributes().get(userObjectSidAttrName);
            if (userObjectSidAttr != null) {
                @Nullable final Object userObjectSid = userObjectSidAttr.get();
                if (userObjectSid != null) {
                    if (userObjectSid instanceof String) {
                        strUserObjectSid = userObjectSid.toString();
                    } else if (userObjectSid instanceof byte[]) {
                        strUserObjectSid = decodeSID((byte[]) userObjectSid);
                    } else {
                        throw new NamingException("LDAP Account: " + ldapUser.getName() + " attribute: " + userObjectSidAttrName + " has an unexpected type of: " + userObjectSid.getClass().getName());
                    }
                } else {
                    LOG.warn("LDAP Account: " + ldapUser.getName() + " attribute: " + userObjectSidAttrName + " has a null value");
                }
            } else {
                LOG.warn("LDAP Account: " + ldapUser.getName() + " attribute: " + userObjectSidAttrName + " is not present");
            }
        }

        @Nullable final String userPrimaryGroupIdAttrName = search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.PRIMARY_GROUP_ID);
        @Nullable final String strUserPrimaryGroupId;
        if (userPrimaryGroupIdAttrName != null) {
            @Nullable final Attribute userPrimaryGroupIdAttr = ldapUser.getAttributes().get(userPrimaryGroupIdAttrName);
            if (userPrimaryGroupIdAttr != null) {
                @Nullable final Object userPrimaryGroupId = userPrimaryGroupIdAttr.get();
                if (userPrimaryGroupId != null) {
                    strUserPrimaryGroupId = (String) userPrimaryGroupId;
                } else {
                    throw new NamingException("LDAP Account: " + ldapUser.getName() + " attribute:" + userPrimaryGroupIdAttrName + " has null value");
                }
            } else {
                throw new NamingException("LDAP Account: " + ldapUser.getName() + " is missing attribute:" + userPrimaryGroupIdAttrName);
            }
        } else {
            throw new NamingException("Configuration for Account attribute primaryGroupID is missing from database security config.xml");
        }

        if (strUserObjectSid != null && SID_PATTERN.matcher(strUserObjectSid).matches()) {
            return strUserObjectSid.substring(0, strUserObjectSid.lastIndexOf('-') + 1) + strUserPrimaryGroupId;
        } else {
            return strUserPrimaryGroupId;
        }
    }

    @Override
    public final synchronized @Nullable Group getGroup(String name) {
        name = ensureCase(name);

        // first attempt to get the cached group
        final Group group = super.getGroup(name);
        if (group != null) {
            return group;
        }

        // second find the account in LDAP
        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getDatabase().getActiveBroker().getCurrentSubject());
            return getGroup(ctx, name);
        } catch (final NamingException ne) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(ne.getMessage(), ne);
            }
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return null;
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    private synchronized @Nullable Group getGroup(final Subject invokingUser, final DBBroker broker, String name) {
        name = ensureCase(name);

        final Group group = super.getGroup(name);
        if (group != null) {
            return group;
        }

        //if the group is not cached, we should try and find it in LDAP and cache it if it exists
        LdapContext ctx = null;
        try {
            ctx = getContext(invokingUser);
            return getGroup(broker, ctx, name);

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return null;

        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    private synchronized @Nullable Group getGroup(final LdapContext ctx, @Nullable String groupName) {
        return getGroup(ctx, groupName, this::createGroupInDatabase);
    }

    private synchronized @Nullable Group getGroup(final DBBroker broker, final LdapContext ctx, @Nullable String groupName) {
        return getGroup(ctx, groupName, (gn, ldapGroup) -> createGroupInDatabase(broker, gn, ldapGroup));
    }

    private synchronized @Nullable Group getGroup(final LdapContext ctx, @Nullable String groupName, final BiFunctionE<String, SearchResult, Group, AuthenticationException> fnCreateDatabaseGroup) {
        if (groupName == null) {
            return null;
        }

        groupName = ensureCase(escapeSearchAttribute(groupName));

        final Group group = super.getGroup(groupName);
        if (group != null) {
            return group;
        }

        //if the group is not cached, we should try and find it in LDAP and cache it if it exists
        try {
            //do the lookup
            @Nullable final SearchResult ldapGroup = findGroupByGroupName(ctx, removeDomainPostfix(groupName));
            if (ldapGroup == null) {
                return null;
            }

            //found a group from ldap so cache them and return
            try {
                return fnCreateDatabaseGroup.apply(groupName, ldapGroup);
                //registerGroup(grp); //TODO do we need to do this?
            } catch (final AuthenticationException ae) {
                LOG.error(ae.getMessage(), ae);
                return null;
            }

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return null;
        }
    }

    @Override
    public boolean hasGroup(final Group group) {
        return hasGroup(group.getName());
    }

    @Override
    public boolean hasGroup(final String name) {
        @Nullable final PrincipalState groupState = groupsByName.read(principalDb -> {
            @Nullable final Tuple2<PrincipalState, Optional<Group>> principalEntry = principalDb.get(name);
            if (principalEntry == null) {
                return null;
            }

            return principalEntry._1;
        });

        // NOTE(AR) We need to check here whether we are already in the process of creating it in the database, we then use that to prevent creating it recursively through calls to getAccount -> createGroupInDatabase
        if (PrincipalState.CREATING == groupState) {
            // skip non-persistent entries
            return false;
        }

        if (PrincipalState.PERSISTENT == groupState) {
            // we already have it
            return true;
        }

        return getGroup((Subject)null, getSecurityManager().getDatabase().getActiveBroker(), name) != null;
    }

    private String addDomainPostfix(String principalName) {
        if (!principalName.contains("@")) {
            principalName += '@' + ensureContextFactory().getDomain();
        }
        return principalName;
    }

    private String removeDomainPostfix(String principalName) {
        if (principalName.contains("@") && principalName.endsWith(ensureContextFactory().getDomain())) {
            principalName = principalName.substring(0, principalName.indexOf('@'));
        }
        return principalName;
    }

    private boolean checkAccountRestrictionList(final String accountName) {
        final LDAPSearchContext search = ensureContextFactory().getSearch();
        return checkPrincipalRestrictionList(accountName, search.getSearchAccount());
    }

    private boolean checkGroupRestrictionList(final String groupName) {
        final LDAPSearchContext search = ensureContextFactory().getSearch();
        return checkPrincipalRestrictionList(groupName, search.getSearchGroup());
    }

    private boolean checkPrincipalRestrictionList(final String principalName, final AbstractLDAPSearchPrincipal searchPrinciple) {

        String name = ensureCase(principalName);

        if (name.indexOf('@') > -1) {
            name = name.substring(0, name.indexOf('@'));
        }

        List<String> blackList = null;
        if (searchPrinciple.getBlackList() != null) {
            blackList = searchPrinciple.getBlackList().getPrincipals();
        }

        List<String> whiteList = null;
        if (searchPrinciple.getWhiteList() != null) {
            whiteList = searchPrinciple.getWhiteList().getPrincipals();
        }

        if (blackList != null) {
            for (String blackEntry : blackList) {
                if (ensureCase(blackEntry).equals(name)) {
                    return false;
                }
            }
        }

        if (whiteList != null && !whiteList.isEmpty()) {
            for (String whiteEntry : whiteList) {
                if (ensureCase(whiteEntry).equals(name)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Escapes '\', '(', and ')' characters.
     *
     * @param searchAttribute The search attribute string.
     *
     * @return the escaped search attribute.
     */
    private String escapeSearchAttribute(final String searchAttribute) {
        return searchAttribute
                .replace("\\", "\\5c")
                .replace("(", "\\28")
                .replace(")", "\\29");
    }

    private @Nullable SearchResult findAccountByAccountName(final DirContext ctx, String accountName) throws NamingException {
        if (!checkAccountRestrictionList(accountName)) {
            return null;
        }

        accountName = escapeSearchAttribute(removeDomainPostfix(accountName));

        final LDAPSearchContext search = ensureContextFactory().getSearch();
        final String searchBase;
        final String searchFilter;
        if (isFullyQualified(accountName)) {
            // account name is already fully qualified so we can just retrieve it directly
            searchBase = extractBaseFromDn(accountName);
            final SearchAttribute[] searchAttributes = extractSearchAttributesFromDn(accountName);
            searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), searchAttributes);
        } else {
            // search for the account by its name
            searchBase = search.getBase();
            final SearchAttribute searchAttribute = new SearchAttribute(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME), accountName);
            searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), searchAttribute);
        }

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                LOG.error("Matched multiple users for the accountName: {}", accountName);
            }
        }

        return searchResult;
    }

    private @Nullable String findGroupBySID(final DirContext ctx, final String sid) throws NamingException {

        final LDAPSearchContext search = ensureContextFactory().getSearch();
        final SearchAttribute sa = new SearchAttribute(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.OBJECT_SID), sid);
        final String searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), sa);

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ctx.search(search.getAbsoluteBase(), searchFilter, searchControls);

        if (results.hasMoreElements()) {
            SearchResult searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                LOG.error("Matched multiple groups for the group with SID: {}", sid);
                return null;
            } else {
                return addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get());
            }
        }
        LOG.error("Matched no group with SID: {}", sid);
        return null;
    }

    private @Nullable SearchResult findGroupByGroupName(final DirContext ctx, String groupName) throws NamingException {
        if (!checkGroupRestrictionList(groupName)) {
            return null;
        }

        groupName = escapeSearchAttribute(removeDomainPostfix(groupName));

        final LDAPSearchContext search = ensureContextFactory().getSearch();
        final String searchBase;
        final String searchFilter;
        if (isFullyQualified(groupName)) {
            // group name is already fully qualified so we can just retrieve it directly
            searchBase = extractBaseFromDn(groupName);
            final SearchAttribute[] searchAttributes = extractSearchAttributesFromDn(groupName);
            searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), searchAttributes);
        } else {
            // search for the group by its name
            searchBase = search.getAbsoluteBase();
            final SearchAttribute searchAttribute = new SearchAttribute(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME), groupName);
            searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), searchAttribute);
        }

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

        if (results.hasMoreElements()) {
            final SearchResult searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                LOG.error("Matched multiple groups for the groupName: {}", groupName);
                return null;
            } else {
                return searchResult;
            }
        }
        LOG.error("Matched no groups for the groupName: {}", groupName);
        return null;
    }

    // configurable methods
    @Override
    public boolean isConfigured() {
        return (configuration != null);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean updateAccount(final Account account) throws PermissionDeniedException, EXistException {
        return super.updateAccount(account);
    }

    @Override
    public boolean deleteAccount(final Account account) {
        // TODO we dont support writing to LDAP
        //XXX: delete local cache?
        return false;
    }

    @Override
    public boolean updateGroup(final Group group) throws PermissionDeniedException, EXistException {
        return super.updateGroup(group);
    }

    @Override
    public boolean deleteGroup(final Group group) {
        //XXX: delete local cache?
        return false;
    }

    private String buildSearchFilter(final String searchPrefix, @Nullable final String dn) {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(buildSearchCriteria(searchPrefix));
        if (dn != null) {
            builder.append("(");
            builder.append(dn);
            builder.append(")");
        }
        builder.append(")");
        return builder.toString();
    }

    private String buildSearchFilter(final String searchPrefix, @Nullable final SearchAttribute... searchAttributes) {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(buildSearchCriteria(searchPrefix));
        if (searchAttributes != null) {
            for (final SearchAttribute searchAttribute : searchAttributes) {
                if (searchAttribute.getName() != null && searchAttribute.getValue() != null) {
                    builder.append("(");
                    builder.append(searchAttribute.getName());
                    builder.append("=");
                    builder.append(searchAttribute.getValue());
                    builder.append(")");
                }
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private String buildSearchFilterUnion(final String searchPrefix, final List<SearchAttribute> searchAttributes) {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(buildSearchCriteria(searchPrefix));

        if (!searchAttributes.isEmpty()) {
            builder.append("(|");

            for (final SearchAttribute sa : searchAttributes) {
                builder.append("(");
                builder.append(sa.getName());
                builder.append("=");
                builder.append(sa.getValue());
                builder.append(")");
            }

            builder.append(")");
        }

        builder.append(")");
        return builder.toString();
    }

    private String buildSearchCriteria(final String searchPrefix) {
        return "&(" + searchPrefix + ")";
    }

    @Override
    public List<String> findUsernamesWhereNameStarts(String startsWith) {

        startsWith = escapeSearchAttribute(ensureCase(startsWith));

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(search.getSearchAccount().getMetadataSearchAttribute(AXSchemaType.FULLNAME), startsWith + "*");
            final String searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> userNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String username = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkAccountRestrictionList(username)) {
                    if (userNames == null) {
                        userNames = new ArrayList<>();
                    }
                    userNames.add(username);
                }
            }

            if (userNames == null) {
                return Collections.emptyList();
            }

            return userNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findUsernamesWhereNamePartStarts(String startsWith) {

        startsWith = escapeSearchAttribute(ensureCase(startsWith));

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();

            final SearchAttribute firstNameSa = new SearchAttribute(search.getSearchAccount().getMetadataSearchAttribute(AXSchemaType.FIRSTNAME), startsWith + "*");
            final SearchAttribute lastNameSa = new SearchAttribute(search.getSearchAccount().getMetadataSearchAttribute(AXSchemaType.LASTNAME), startsWith + "*");
            final List<SearchAttribute> sas = Arrays.asList(firstNameSa, lastNameSa);

            final String searchFilter = buildSearchFilterUnion(search.getSearchAccount().getSearchFilterPrefix(), sas);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> userNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String username = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkAccountRestrictionList(username)) {
                    if (userNames == null) {
                        userNames = new ArrayList<>();
                    }
                    userNames.add(username);
                }
            }

            if (userNames == null) {
                return Collections.emptyList();
            }

            return userNames;
        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findUsernamesWhereUsernameStarts(String startsWith) {

        startsWith = escapeSearchAttribute(ensureCase(startsWith));

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME), startsWith + "*");
            final String searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> userNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String username = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));

                if (checkAccountRestrictionList(username)) {
                    if (userNames == null) {
                        userNames = new ArrayList<>();
                    }
                    userNames.add(username);
                }
            }

            if (userNames == null) {
                return Collections.emptyList();
            }

            return userNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }


    private List<String> findGroupnamesForUserDistinguishedName(final LdapContext ctx, String userDistinguishedName) {

        userDistinguishedName = escapeSearchAttribute(userDistinguishedName);

        try {
            final LDAPSearchContext search = ensureContextFactory().getSearch();

            @Nullable final String groupMemberAttrName = search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.MEMBER);
            if (groupMemberAttrName == null) {
                LOG.warn("Expected a 'member' attribute to be specified in the LDAP Realm config for search/group, but it is missing. Unable to find groups for a user name!");
                return Collections.emptyList();
            }

            final SearchAttribute sa = new SearchAttribute(groupMemberAttrName, userDistinguishedName);
            final String searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getAbsoluteBase(), searchFilter, searchControls);

            @Nullable List<String> groupNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String groupName = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkGroupRestrictionList(groupName)) {
                    if (groupNames == null) {
                        groupNames = new ArrayList<>();
                    }
                    groupNames.add(groupName);
                }
            }

            if (groupNames == null) {
                return Collections.emptyList();
            }

            return groupNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> findGroupnamesWhereGroupnameStarts(String startsWith) {

        startsWith = escapeSearchAttribute(ensureCase(startsWith));

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME), startsWith + "*");
            final String searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> groupNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String groupName = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkGroupRestrictionList(groupName)) {
                    if (groupNames == null) {
                        groupNames = new ArrayList<>();
                    }
                    groupNames.add(groupName);
                }
            }

            if (groupNames == null) {
                return Collections.emptyList();
            }

            return groupNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findGroupnamesWhereGroupnameContains(String fragment) {

        fragment = escapeSearchAttribute(ensureCase(fragment));

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME), "*" + fragment + "*");
            final String searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> groupNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String groupName = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkGroupRestrictionList(groupName)) {
                    if (groupNames == null) {
                        groupNames = new ArrayList<>();
                    }
                    groupNames.add(groupName);
                }
            }

            if (groupNames == null) {
                return Collections.emptyList();
            }

            return groupNames;

        } catch (final NamingException ne) {
            LOG.error(ne);
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findAllGroupNames() {
        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(null, null);
            final String searchFilter = buildSearchFilter(search.getSearchGroup().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> groupNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String groupName = (String) searchResult.getAttributes().get(search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get();
                if (checkGroupRestrictionList(groupName)) {
                    if (groupNames == null) {
                        groupNames = new ArrayList<>();
                    }

                    // expand group name to realm's group name
                    final String realmGroupName = ensureCase(addDomainPostfix(groupName));

                    groupNames.add(realmGroupName);
                }
            }

            if (groupNames == null) {
                return Collections.emptyList();
            }

            return groupNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findAllUserNames() {
        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            final LDAPSearchContext search = ensureContextFactory().getSearch();
            final SearchAttribute sa = new SearchAttribute(null, null);
            final String searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), sa);

            final SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)});

            final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);

            @Nullable List<String> userNames = null;
            while (results.hasMoreElements()) {
                final SearchResult searchResult = results.nextElement();
                final String accountName = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                if (checkAccountRestrictionList(accountName)) {
                    if (userNames == null) {
                        userNames = new ArrayList<>();
                    }
                    userNames.add(accountName);
                }
            }

            if (userNames == null) {
                return Collections.emptyList();
            }

            return userNames;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    @Override
    public List<String> findAllGroupMembers(String groupName) {

        groupName = ensureCase(escapeSearchAttribute(removeDomainPostfix(groupName)));

        if (!checkGroupRestrictionList(groupName)) {
            return Collections.emptyList();
        }

        LdapContext ctx = null;
        try {
            ctx = getContext(getSecurityManager().getCurrentSubject());

            //find the dn of the group
            SearchResult searchResult = findGroupByGroupName(ctx, groupName);
            if (searchResult == null) {
                // no such group
                return Collections.emptyList();
            }

            final LDAPSearchContext search = ensureContextFactory().getSearch();

            @Nullable final String accountMemberOfAttrName = search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.MEMBER_OF);
            @Nullable final String groupDnAttName = search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.DN);
            @Nullable final String groupMemberAttrName = search.getSearchGroup().getSearchAttribute(LDAPSearchAttributeKey.MEMBER);

            @Nullable List<String> groupMembers = null;

            if (accountMemberOfAttrName != null && groupDnAttName != null) {
                // Active Directory (like) - accounts have a memberOf attribute, so we look for accounts that are a member of our group by its dn

                @Nullable final Attribute groupDnAttr = searchResult.getAttributes().get(groupDnAttName);
                if (groupDnAttr != null) {
                    final String groupDn = (String) groupDnAttr.get();

                    // Find all accounts that have a memberOf=groupDn
                    final SearchAttribute sa = new SearchAttribute(accountMemberOfAttrName, groupDn);
                    final String searchFilter = buildSearchFilter(search.getSearchAccount().getSearchFilterPrefix(), sa);
                    final SearchControls searchControls = new SearchControls();
                    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                    searchControls.setReturningAttributes(new String[] { search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME) });
                    final NamingEnumeration<SearchResult> results = ctx.search(search.getBase(), searchFilter, searchControls);
                    while (results.hasMoreElements()) {
                        searchResult = results.nextElement();
                        final String member = ensureCase(addDomainPostfix((String) searchResult.getAttributes().get(search.getSearchAccount().getSearchAttribute(LDAPSearchAttributeKey.NAME)).get()));
                        if (checkAccountRestrictionList(member)) {
                            if (groupMembers == null) {
                                groupMembers = new ArrayList<>();
                            }
                            groupMembers.add(member);
                        }
                    }
                }

            } else if (groupMemberAttrName != null) {
                // NIS/POSIX (like) - groups have a member attribute, so we retrieve all the of those properties
                @Nullable final Attribute groupMemberAttr = searchResult.getAttributes().get(groupMemberAttrName);
                if (groupMemberAttr != null) {
                    for (int i = 0; i < groupMemberAttr.size(); i++) {
                        final String member = ensureCase(addDomainPostfix((String) groupMemberAttr.get(i)));
                        if (groupMembers == null) {
                            groupMembers = new ArrayList<>();
                        }
                        groupMembers.add(member);
                    }
                }
            }

            if (groupMembers == null) {
                return Collections.emptyList();
            }

            return groupMembers;

        } catch (final NamingException ne) {
            LOG.error(new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, ne.getMessage()));
            return Collections.emptyList();
        } finally {
            LDAPUtils.closeContext(ctx);
        }
    }

    public final class AuthenticatedLdapSubjectAccreditedImpl extends SubjectAccreditedImpl {
        private final String authenticatedCredentials;

        public AuthenticatedLdapSubjectAccreditedImpl(final AbstractAccount account, final LdapContext ctx, final String authenticatedCredentials) {
            super(account, ctx);
            this.authenticatedCredentials = authenticatedCredentials;
        }

        private String getAuthenticatedCredentials() {
            return authenticatedCredentials;
        }
    }
}
