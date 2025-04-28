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
package org.exist.storage.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for indicating that no locks
 * must be held on parameters to a method, or on return types.
 *
 * As well as explicitly expressing intention, this annotation can be used
 * with {@link EnsureLockingAspect} to compile into the code runtime checks
 * which will enforce the locking policy.
 *
 * Typically this is used with parameters of type {@link org.exist.collections.Collection}
 * and {@link org.exist.dom.persistent.DocumentImpl}.
 *
 * If this annotation is
 * used on an {@link org.exist.xmldb.XmldbURI} then a {@code type} value must
 * also be provided to indicate the type of the lock identified by the uri.
 *
 * For example we may indicate that Collection parameters to methods
 * should not be locked:
 * <pre>
 * {@code
 * public LockedCollection lockCollection(@EnsureUnlocked final Collection collection) {
 *
 *    ...
 *
 * }
 * }
 * </pre>

 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.PARAMETER})
public @interface EnsureUnlocked {

    /**
     * The type of the lock.
     *
     * Only needed if the annotation is not placed on a
     * {@link org.exist.collections.Collection} or {@link org.exist.dom.persistent.DocumentImpl}
     * parameter or return type.
     * @return the lock type
     */
    Lock.LockType type() default Lock.LockType.UNKNOWN;
}
