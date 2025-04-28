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
 * must be held on the containing object before
 * a method may be called.
 *
 * As well as explicitly expressing intention, this annotation can be used
 * with {@link EnsureLockingAspect} to compile into the code runtime checks
 * which will enforce the locking policy.
 *
 * Typically this is used on methods within implementations of {@link org.exist.collections.Collection}
 * and {@link org.exist.dom.persistent.DocumentImpl}.
 * The typical use is to ensure that a container holds no locks (by URI)
 * when calling the method accessors on their internal state.
 *
 * <pre>
 *
 * public class MyCollectonImpl implements Collection {
 *     final XmldbURI uri;
 *     public MyCollectionImpl(final XmldbURI uri) {
 *         this.uri = uri;
 *     }
 *
 *     public XmldbURI getUri() {
 *         return uri;
 *     }
 *
 *     ...
 *
 *     <code>@EnsureContainerUnlocked</code>
 *     public int dirtyCountDocuments() {
 *         return documents.size();
 *     }
 * }
 *
 * </pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface EnsureContainerUnlocked {
}
