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
 */
package org.exist.storage.serializers;

/**
 * Elemental Feature Keys for {@link Serializer}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface FeatureKeys {

    String NS = "http://ns.elemental.xyz/feature";

    /**
     * Enable or disable XInclude processing in {@link Serializer}.
     */
    String EXPAND_XINCLUDES = NS + "/" + EXistOutputKeys.EXPAND_XINCLUDES;

    /**
     * Set the path for resolving XInclude resources in {@link Serializer}.
     */
    String XINCLUDE_PATH = NS + "/" + EXistOutputKeys.XINCLUDE_PATH;
}
