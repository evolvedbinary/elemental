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
package org.exist.test.runner;

import com.evolvedbinary.j8fu.function.ConsumerE;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.source.FileSource;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.Sequence;
import org.junit.runner.Runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Base class for XSuite test runners.
 *
 * @author Adam Retter
 */
public abstract class AbstractTestRunner extends Runner {

    protected final Path path;
    protected final boolean parallel;

    protected AbstractTestRunner(final Path path, final boolean parallel) {
        this.path = path;
        this.parallel = parallel;
    }

    protected static Sequence executeQuery(final BrokerPool brokerPool, final Source source, final List<Function<XQueryContext, Tuple2<String, Object>>> externalVariableBindings) throws EXistException, PermissionDeniedException, XPathException, IOException, DatabaseConfigurationException {

        final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreCompilation = xqueryContext -> {
            // Setup paths in the context
            xqueryContext.setBaseURI(new AnyURIValue("/db"));
            if (source instanceof FileSource) {
                final Path queryPath = Paths.get(((FileSource) source).getPath().toAbsolutePath().toString());
                if (Files.isDirectory(queryPath)) {
                    xqueryContext.setModuleLoadPath(queryPath.toString());
                } else {
                    xqueryContext.setModuleLoadPath(queryPath.getParent().toString());
                }
            }
        };

        final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreExecution = xqueryContext -> {
            // Declare variables for the query
            for(final Function<XQueryContext, Tuple2<String, Object>> externalVariableBinding : externalVariableBindings) {
                final Tuple2<String, Object> nameValue = externalVariableBinding.apply(xqueryContext);
                xqueryContext.declareVariable(nameValue._1, true, nameValue._2);
            }
        };

        final SecurityManager securityManager = requireNonNull(brokerPool.getSecurityManager(), "securityManager is null");

        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            return XQueryUtil.query(broker, source, true, null, null, setupXqueryContextPreCompilation, setupXqueryContextPreExecution, null).result;
        }
    }

    protected static String checkDescription(Object source,  String description) {
        if (description == null) {
            throw new IllegalArgumentException(source + " description is null");
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException(source + " description is empty");
        }
        if (description.startsWith("(")) {
            throw new IllegalArgumentException(source + " description '" + description + "' starts with '('");
        }
        return description;
    }
}
