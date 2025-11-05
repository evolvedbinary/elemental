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
package org.exist.xquery.modules.expathrepo;

import org.exist.dom.QName;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.ErrorCodes.IErrorCode;

import javax.annotation.Nullable;

/**
 * EXPATH specific errors [EXP][DY|SE|ST][nnnn]
 *
 * EXP = EXPath
 * DY = Dynamic
 * DY = Dynamic
 * SE = Serialization
 * ST = Static
 * nnnn = number
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum EXPathErrorCode implements IErrorCode {
    EXPDY001 ("Package not found."),
    EXPDY002 ("Bad collection URI."),
    EXPDY003 ("Permission denied."),
    EXPDY004 ("Error in descriptor found."),
    EXPDY005 ("Invalid repo URI"),
    EXPDY006 ("Failed to connect to public repo"),
    // other error thrown from expath library
    EXPDY007 (null);
    
    public final static String EXPATH_ERROR_NS = "http://expath.org/ns/error";
    public final static String EXPATH_ERROR_PREFIX = "experr";

    private final ErrorCodes.ErrorCode errorCode;

    EXPathErrorCode(@Nullable final String description) {
        this.errorCode = new ErrorCodes.ErrorCode(new QName(name(), EXPATH_ERROR_NS, EXPATH_ERROR_PREFIX), description);
    }

    @Override
    public QName getErrorQName() {
        return errorCode.getErrorQName();
    }

    @Override
    public @Nullable String getDescription() {
        return errorCode.getDescription();
    }

    /**
     * Get the error code.
     *
     * @return the error code.
     */
    public ErrorCodes.ErrorCode getErrorCode() {
        return errorCode;
    }
}