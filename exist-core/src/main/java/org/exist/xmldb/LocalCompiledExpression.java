/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
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
package org.exist.xmldb;

import org.exist.xquery.XQueryUtil;
import org.xmldb.api.base.CompiledExpression;

/**
 * XML:DB Local Compiled Expression wrapper for a compiled XQuery.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LocalCompiledExpression implements CompiledExpression, AutoCloseable {

    private final XQueryUtil.CompilationResult compilationResult;

    public LocalCompiledExpression(final XQueryUtil.CompilationResult compilationResult) {
        this.compilationResult = compilationResult;
    }

    /**
     * Get the underlying compilation result.
     *
     * @return the underlying compilation result.
     */
    public XQueryUtil.CompilationResult getCompilationResult() {
        return compilationResult;
    }

    @Override
    public void reset() {
        compilationResult.reset();
    }

    @Override
    public void close() {
        compilationResult.close();
    }
}
