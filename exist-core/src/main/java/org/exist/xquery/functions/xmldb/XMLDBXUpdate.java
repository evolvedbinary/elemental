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
package org.exist.xquery.functions.xmldb;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.*;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.lock.Lock;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.exist.xupdate.Modification;
import org.exist.xupdate.XUpdateProcessor;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import java.util.List;
import java.util.Properties;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.functions.xmldb.XMLDBModule.functionSignatures;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XMLDBXUpdate extends BasicFunction {
    private static final FunctionParameterSequenceType FS_PARAM_COLLECTION_URI = param("collection-uri", Type.STRING, "The collection URI");
    private static final FunctionParameterSequenceType FS_PARAM_DOCUMENT_URI = param("document-uri", Type.STRING, "The document URI");
    private static final FunctionParameterSequenceType FS_PARAM_MODIFICATIONS = param("modifications", Type.NODE, "The XUpdate modifications to be processed");

    private static final String FS_UPDATE_NAME = "update";
    static final FunctionSignature[] FS_UPDATE = functionSignatures(
            FS_UPDATE_NAME,
            "Processes an XUpdate request, $modifications, against a collection $collection-uri. "
                + XMLDBModule.COLLECTION_URI
                + "The modifications are passed in a "
                + "document conforming to the XUpdate specification. "
                + "https://xmldb-org.sourceforge.net/xupdate/xupdate-wd.html"
                + "The function returns the number of modifications caused by the XUpdate.",
            returns(Type.INTEGER, "The number of modifications, as an xs:integer, caused by the XUpdate"),
            arities(
                arity(
                    FS_PARAM_COLLECTION_URI,
                    FS_PARAM_MODIFICATIONS
                ),
                arity(
                    FS_PARAM_COLLECTION_URI,
                    FS_PARAM_DOCUMENT_URI,
                    FS_PARAM_MODIFICATIONS
                )
            )
    );

	public XMLDBXUpdate(final XQueryContext context, final FunctionSignature functionSignature) {
		super(context, functionSignature);
	}

	@Override
	public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final String collectionUri = args[0].itemAt(0).getStringValue();
        @Nullable final String documentUri;
        final Item modifications;
        if (args.length == 3) {
            documentUri = args[1].itemAt(0).getStringValue();
            modifications = args[2].itemAt(0);
        } else {
            documentUri = null;
            modifications = args[1].itemAt(0);
        }

        final MutableDocumentSet documentSet = new DefaultDocumentSet();
        try (final Collection collection = context.getBroker().openCollection(XmldbURI.create(collectionUri), Lock.LockMode.READ_LOCK)) {
            if (documentUri == null) {
                collection.allDocs(context.getBroker(), documentSet, true);

            } else {
                try (final LockedDocument lockedDocument = collection.getDocumentWithLock(context.getBroker(), XmldbURI.create(documentUri), Lock.LockMode.READ_LOCK)) {

                    // NOTE: early release of Collection lock inline with Asymmetrical Locking scheme
                    collection.close();

                    final DocumentImpl doc = lockedDocument == null ? null : lockedDocument.getDocument();
                    if (doc == null) {
                        throw new XPathException(this, "Resource not found: " + documentUri);
                    }
                    documentSet.add(doc);
                }
            }

            final XUpdateProcessor processor = new XUpdateProcessor(context.getBroker(), documentSet);
            modifications.toSAX(context.getBroker(), processor, new Properties());
            final List<Modification> modificationList = processor.getModifications();
            long mods = 0;
            for (final Modification modification : modificationList) {
                mods += modification.process(context.getBroker().getCurrentTransaction());
                context.getBroker().flush();
            }

            context.getRootExpression().resetState(false);

            return new IntegerValue(this, mods);

        } catch (final PermissionDeniedException | LockException | EXistException | ParserConfigurationException | SAXException e) {
            throw new XPathException(this, e);
        }
	}
}
