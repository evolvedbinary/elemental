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
package org.exist.security.realm.iprange;

import com.evolvedbinary.j8fu.function.ConsumerE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.config.Configuration;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.config.annotation.ConfigurationFieldAsAttribute;
import org.exist.security.*;
import org.exist.security.internal.SecurityManagerImpl;
import org.exist.security.internal.SubjectAccreditedImpl;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.DBBroker;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.Properties;

/**
 * @author <a href="mailto:wshager@gmail.com">Wouter Hager</a>
 */
@ConfigurationClass("realm") //TODO: id = IPRange
public class IPRangeRealm extends AbstractRealm {

    @ConfigurationFieldAsAttribute("id")
    public final static String ID = "IPRange";

    @ConfigurationFieldAsAttribute("version")
    public final static String version = "1.0";

    private final static Logger LOG = LogManager.getLogger(IPRangeRealm.class);
    private static IPRangeRealm instance = null;

    private static final String IP_VARIABLE_NAME = "ip";

    private static final Source QUERY = new StringSource(
        "declare variable $" + IP_VARIABLE_NAME + " as xs:long external;\n" +
        "fn:collection('/db/system/security/iprange/accounts')/account/iprange[$ip ge number(start)][$ip le number(end)]/../name"
    );

    public IPRangeRealm(final SecurityManagerImpl sm, final Configuration config) {
        super(sm, config);
        instance = this;
    }

    static IPRangeRealm getInstance(){
        return instance;
    }

    private static long ipToLong(final InetAddress ip) {
        final byte[] octets = ip.getAddress();
        long result = 0;
        for (final byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean deleteAccount(final Account account) {
        return false;
    }

    @Override
    public boolean deleteGroup(final Group group) {
        return false;
    }

    @Override
    public Subject authenticate(final String ipAddress, final Object credentials) throws AuthenticationException {

        // Elevaste to system privileges
        try (final DBBroker broker = getSecurityManager().database().get(Optional.of(getSecurityManager().getSystemSubject()))) {

            // Convert IP address
            final long ipToTest = ipToLong(InetAddress.getByName(ipAddress));

            final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreExecution = xqueryContext -> {
                xqueryContext.declareVariable(IP_VARIABLE_NAME, new IntegerValue(ipToTest, Type.LONG));
            };

            // Execute xQuery
            final Properties outputProperties = new Properties();
            final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, QUERY, true, null, outputProperties, null, setupXqueryContextPreExecution, null);
            final SequenceIterator i = queryResult.result.iterate();

            // Get FIRST username when present
            final String username = i.hasNext() ? i.nextItem().getStringValue() : "";

            if (i.hasNext()) {
                LOG.warn("IP address {} matched multiple ipranges. Using first result only.", ipAddress);
            }

            if (!username.isEmpty()) {
                final Account account = getSecurityManager().getAccount(username);
                if (account != null) {
                    LOG.info("IPRangeRealm trying {}", account.getName());
                    return new SubjectAccreditedImpl((AbstractAccount) account, ipAddress);
                } else {
                    LOG.info("IPRangeRealm couldn't resolve account for {}", username);
                }

            } else {
                LOG.info("IPRangeRealm xquery found no matches");
            }
            return null;

        } catch (final EXistException | IOException | XPathException | PermissionDeniedException e) {
            throw new AuthenticationException(AuthenticationException.UNNOWN_EXCEPTION, e.getMessage());
        }
    }
}
