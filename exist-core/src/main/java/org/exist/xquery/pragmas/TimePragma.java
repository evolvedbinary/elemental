/*
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
package org.exist.xquery.pragmas;

import org.exist.xquery.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Sequence;

/**
 * An XQuery Pragma that will record the execution
 * time of the associated expression.
 */
public class TimePragma extends AbstractPragma {

    public static final QName TIME_PRAGMA_NAME = new QName("time", Namespaces.EXIST_NS, "exist");
    public static final QName DEPRECATED_TIMER_PRAGMA_NAME = new QName("timer", Namespaces.EXIST_NS, "exist");

    private Logger log = null;

    private long start;
    private boolean verbose = true;

    public TimePragma(final Expression expression, final QName qname, final String contents) throws XPathException {
        super(expression, qname, contents);
        if (contents != null && !contents.isEmpty()) {

            final String[] options = Option.tokenize(contents);
            for (final String option : options) {
                final String[] param = Option.parseKeyValuePair(option);
                if (param == null) {
                    throw new XPathException((Expression) null, "Invalid content found for pragma " + TIME_PRAGMA_NAME.getStringValue() + ": " + contents);
                }

                if ("verbose".equals(param[0])) {
                    verbose = "yes".equals(param[1]);
                } else if ("logger".equals(param[0])) {
                    log = LogManager.getLogger(param[1]);
                }
            }
        }
        if (log == null) {
            log = LogManager.getLogger(TimePragma.class);
        }
    }

    @Override
    public void before(final XQueryContext context, final Expression expression, final Sequence contextSequence) throws XPathException {
        this.start = System.currentTimeMillis();
    }

    @Override
    public void after(final XQueryContext context, final Expression expression) throws XPathException {
        final long elapsed = System.currentTimeMillis() - start;
        if (log.isTraceEnabled()) {
            if (verbose) {
                log.trace("Elapsed: {}ms. for expression:\n{}", elapsed, ExpressionDumper.dump(expression));
            } else {
                log.trace("Elapsed: {}ms.", elapsed);
            }
        }
    }
}
