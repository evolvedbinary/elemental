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

import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.lock.Lock.LockType;

/**
 * An annotation for indicating that certain locks
 * must be held on parameters to a method or return types.
 *
 * As well as explicitly expressing intention, this annotation can be used
 * with {@link EnsureLockingAspect} to compile into the code runtime checks
 * which will enforce the locking policy.
 *
 * Typically this is used with parameters of type {@link org.exist.collections.Collection}
 * and {@link org.exist.dom.persistent.DocumentImpl}. If this annotation is
 * used on an {@link org.exist.xmldb.XmldbURI} then a {@code type} value must
 * also be provided to indicate the type of the lock identified by the uri.
 *
 * For example we may indicate that Collection parameters to methods
 * must already be locked appropriately before the method is called:
 * <pre>
 * public Result copyCollection(
 *          {@code @EnsureLocked(mode=LockMode.READ_LOCK)} final Collection srcCollection,
 *          {@code @EnsureLocked(mode=LockMode.WRITE_LOCK)} final Collection destCollection) {
 *
 *    ...
 *
 * }
 * </pre>
 *
 * We may also indicate that objects returned from a function must have gained an appropriate
 * lock for the calling thread:
 *
 * <pre>
 * public {@code @EnsureLocked(mode=LockMode.READ_LOCK)} Collection openCollection(final XmldbURI uri, final LockMode lockMode) {
 *
 *    ...
 *
 * }
 * }</pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.PARAMETER})
public @interface EnsureLocked {

    /**
     * Specifies the mode of the held lock.
     *
     * {@link LockMode#NO_LOCK} is used as the default, to allow {@code modeParam}
     * to be used instead.
     *
     * If neither {@code mode} or {@code modeParam} are specified, and there is not a
     * single {@link Lock.LockMode} type parameter that can be used
     * then an IllegalArgumentException will be generated if {@link EnsureLockingAspect}
     * detects this situation.
     *
     * @return the lock mode
     */
    Lock.LockMode mode() default LockMode.NO_LOCK;

    /**
     * Specifies that the mode of the held lock is informed
     * by a parameter to the method.
     *
     * The value of this attribute is the (zero-based) index
     * of the parameter within the method signature.
     *
     * @return the mode
     */
    short modeParam() default NO_MODE_PARAM;

    short NO_MODE_PARAM = -1;

    /**
     * The type of the lock.
     *
     * Only needed if the annotation is not placed on a
     * {@link org.exist.collections.Collection} or {@link org.exist.dom.persistent.DocumentImpl}
     * parameter or return type.
     *
     * @return the lock type
     */
    Lock.LockType type() default LockType.UNKNOWN;
}
