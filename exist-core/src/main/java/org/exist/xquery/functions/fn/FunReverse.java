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
package org.exist.xquery.functions.fn;

import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.ValueSequence;
import org.exist.xquery.value.Type;

import static org.exist.xquery.FunctionDSL.optManyParam;
import static org.exist.xquery.FunctionDSL.returnsOptMany;
import static org.exist.xquery.functions.fn.FnModule.functionSignature;

/**
 * Implements the fn:reverse function.
 *
 * @author <a href="mailto:piotr@ideanest.com">Piotr Kaminski</a>
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunReverse extends BasicFunction {

    public static final FunctionSignature FS_REVERSE = functionSignature(
			"reverse",
			"Reverses the order of items in a sequence. If the argument is an empty sequence, the empty sequence is returned.",
            returnsOptMany(Type.ITEM, "The reverse order sequence"),
            optManyParam("arg", Type.ITEM, "The sequence to reverse")
    );
			
	public FunReverse(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Sequence seq = args[0];
        final Sequence result;
        if (seq.isEmpty()) {
            result = Sequence.EMPTY_SEQUENCE;
        } else {
            result = new ValueSequence();
            for (int i = seq.getItemCount() - 1; i >= 0; i--) {
                result.add(seq.itemAt(i));
            }
        }
        return result;
    }
}
