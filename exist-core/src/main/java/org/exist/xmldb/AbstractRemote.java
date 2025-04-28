/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.xmldb;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.exist.security.ACLPermission;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.internal.aider.ACEAider;
import org.exist.security.internal.aider.PermissionAiderFactory;

/**
 * Base class for Remote XMLDB classes
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractRemote {

    protected RemoteCollection collection;

    AbstractRemote(final RemoteCollection collection) {
        this.collection = collection;
    }

    protected XmldbURI resolve(final XmldbURI name) {
        if (collection != null) {
            return collection.getPathURI().resolveCollectionPath(name);
        } else {
            return name;
        }
    }
    
    protected Stream<ACEAider> extractAces(final Object aclParameter) {
        return Optional.ofNullable((Object[])aclParameter)
                .map(Arrays::stream)
                .map(stream -> stream.map(o -> (ACEAider)o))
                .orElse(Stream.<ACEAider>empty());
    }
    
    protected Permission getPermission(final String owner, final String group, final int mode, final Stream<ACEAider> aces) throws PermissionDeniedException {
        final Permission perm = PermissionAiderFactory.getPermission(owner, group, mode);
        if (perm instanceof ACLPermission) {
            final ACLPermission aclPermission = (ACLPermission) perm;
            for (final ACEAider ace : aces.collect(Collectors.toList())) {
                aclPermission.addACE(ace.getAccessType(), ace.getTarget(), ace.getWho(), ace.getMode());
            }
        }
        return perm;
    }
}
