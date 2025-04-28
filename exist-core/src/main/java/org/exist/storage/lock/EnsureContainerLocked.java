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
 * An annotation for indicating that certain locks
 * must be held on the containing object before
 * a method may be called.
 *
 * As well as explicitly expressing intention, this annotation can be used
 * with {@link EnsureLockingAspect} to compile into the code runtime checks
 * which will enforce the locking policy.
 *
 * Typically this is used on methods within implementations of {@link org.exist.collections.Collection}
 * and {@link org.exist.dom.persistent.DocumentImpl}.
 * The typical use is to ensure that a container holds appropriate locks (by URI)
 * when calling the method accessors on their internal state.
 *
 * <pre>
 * public class MyCollectonImpl implements Collection {
 *     final XmldbURI uri;
 *     public MyCollectionImpl(@EnsureLocked(mode=LockMode.READ_LOCK, type=LockType.COLLECTION) final XmldbURI uri) {
 *         this.uri = uri;
 *     }
 *
 *     public XmldbURI getUri() {
 *         return uri;
 *     }
 *
 *     ...
 *
 *     <code>@EnsureContainerLocked(mode=LockMode.READ_LOCK)</code>
 *     public int countDocuments() {
 *         return documents.size();
 *     }
 * }</pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface EnsureContainerLocked {

    /**
     * Specifies the mode of the held lock.
     *
     * {@link Lock.LockMode#NO_LOCK} is used as the default, to allow {@code modeParam}
     * to be used instead.
     *
     * If neither {@code mode} or {@code modeParam} are specified, and there is not a
     * single {@link Lock.LockMode} type parameter that can be used
     * then an IllegalArgumentException will be generated if {@link EnsureLockingAspect}
     * detects this situation.
     * @return  the lock mode
     */
    Lock.LockMode mode() default Lock.LockMode.NO_LOCK;

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

}
