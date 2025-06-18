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
package org.exist.xquery.modules.compression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.util.*;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.util.io.FilterInputStreamCache;
import org.exist.util.io.FilterInputStreamCacheFactory;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.*;

import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @version 2.0
 */
public abstract class AbstractExtractFunction extends BasicFunction {

    private Sequence contextSequence;
    private FunctionReference entryFilterFunction;
    private Sequence[] filterParams;
    private FunctionReference entryDataFunction;
    private Sequence[] dataParams;
    private Charset encoding;


    public AbstractExtractFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        // get the parameters and check their types
        if (args[0].isEmpty()) {
            return Sequence.EMPTY_SEQUENCE;
        }
        final BinaryValue compressedData = ((BinaryValue) args[0].itemAt(0));

        if(!(args[1].itemAt(0) instanceof FunctionReference)) {
            throw new XPathException(this, "No entry-filter function provided.");
        }

        try {
            entryFilterFunction = (FunctionReference) args[1].itemAt(0);
            final FunctionSignature entryFilterFunctionSignature = entryFilterFunction.getSignature();

            if (args.length < 5) {
                if (!validateFunctionSignature(entryFilterFunctionSignature, new SequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE))) {
                    throw new XPathException(this, "entry-filter function must have a signature that matches: entry-filter($path as xs:string, $data-type as xs:string) as xs:boolean");
                }

                filterParams = new Sequence[2];

                if (!(args[2].itemAt(0) instanceof FunctionReference)) {
                    throw new XPathException(this, "No entry-data function provided.");
                }
                entryDataFunction = (FunctionReference) args[2].itemAt(0);
                final FunctionSignature entryDataFunctionSignature = entryDataFunction.getSignature();
                if (entryDataFunctionSignature.getArgumentCount() == 2) {
                    if (!validateFunctionSignature(entryDataFunctionSignature, new SequenceType(Type.ANY_URI, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE))) {
                        throw new XPathException(this, "entry-path function must have a signature that matches: entry-path($path as xs:string, $data-type as xs:string) as xs:anyURI");
                    }
                    dataParams = new Sequence[2];
                } else if (entryDataFunctionSignature.getArgumentCount() == 3) {
                    if (!validateFunctionSignature(entryDataFunctionSignature, new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.ITEM, Cardinality.ZERO_OR_ONE))) {
                        throw new XPathException(this, "entry-data function must have a signature that matches: entry-data($path as xs:string, $data-type as xs:string, $data as item()?) as item()*");
                    }
                    dataParams = new Sequence[3];
                } else {
                    throw new XPathException(this, "entry-data/entry-path function must have a signature that matches either: entry-data($path as xs:string, $data-type as xs:string, $data as item()?) as item()*, or entry-path($path as xs:string, $data-type as xs:string) as xs:anyURI");
                }

                if (args.length == 4) {
                    encoding = Charset.forName(args[3].itemAt(0).getStringValue());
                } else {
                    encoding = StandardCharsets.UTF_8;
                }

            } else {
                if (!validateFunctionSignature(entryFilterFunctionSignature, new SequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE))) {
                    throw new XPathException(this, "entry-filter function must have a signature that matches: entry-filter($path as xs:string, $data-type as xs:string, $param as item()*) as xs:boolean");
                }

                filterParams = new Sequence[3];
                filterParams[2] = args[2];

                if (!(args[3].itemAt(0) instanceof FunctionReference)) {
                    throw new XPathException(this, "No entry-data function provided.");
                }
                entryDataFunction = (FunctionReference) args[3].itemAt(0);
                final FunctionSignature entryDataFunctionSignature = entryDataFunction.getSignature();
                if (entryDataFunctionSignature.getArgumentCount() == 3) {
                    if (!validateFunctionSignature(entryDataFunctionSignature, new SequenceType(Type.ANY_URI, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE))) {
                        throw new XPathException(this, "entry-path function must have a signature that matches: entry-path($path as xs:string, $data-type as xs:string, $param as item()*) as xs:anyURI");
                    }
                    dataParams = new Sequence[3];
                    dataParams[2] = args[4];
                } else if (entryDataFunctionSignature.getArgumentCount() == 4) {
                    if (!validateFunctionSignature(entryDataFunctionSignature, new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE), new SequenceType(Type.ITEM, Cardinality.ZERO_OR_ONE), new SequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE))) {
                        throw new XPathException(this, "entry-data function must have a signature that matches: entry-data($path as xs:string, $data-type as xs:string, $data as item()?, $param as item()*) as item()*");
                    }
                    dataParams = new Sequence[4];
                    dataParams[3] = args[4];
                } else {
                    throw new XPathException(this, "entry-data/entry-path function must have a signature that matches either: entry-data($path as xs:string, $data-type as xs:string, $data as item()?, $param as item()*) as item()*, or entry-path($path as xs:string, $data-type as xs:string, $param as item()*) as xs:anyURI");
                }

                if (args.length == 6) {
                    encoding = Charset.forName(args[5].itemAt(0).getStringValue());
                } else {
                    encoding = StandardCharsets.UTF_8;
                }
            }

            this.contextSequence = contextSequence;

            return processCompressedData(compressedData, encoding);

        } catch(final UnsupportedCharsetException e) {
            throw new XPathException(this, e.getMessage(), e);
        } finally {
            if (entryDataFunction != null) {
                entryDataFunction.close();
            }
            if (entryFilterFunction != null) {
                entryFilterFunction.close();
            }
        }
    }

    private boolean validateFunctionSignature(final FunctionSignature functionSignature, final SequenceType expectedReturnType, final SequenceType... expectedArgumentTypes) {
        final SequenceType actualReturnType = functionSignature.getReturnType();

        if (!areSequenceTypesCompatible(expectedReturnType, actualReturnType)) {
            return false;
        }

        final SequenceType[] actualArgumentTypes = functionSignature.getArgumentTypes();
        for (int i = 0; i < expectedArgumentTypes.length; i++) {
            final SequenceType expectedArgumentType = expectedArgumentTypes[i];
            final SequenceType actualArgumentType = actualArgumentTypes[i];

            if (!areSequenceTypesCompatible(expectedArgumentType, actualArgumentType)) {
                return false;
            }
        }

        return true;
    }

    private boolean areSequenceTypesCompatible(final SequenceType expectedSequenceType, final SequenceType actualSequenceType) {
        if (!Type.subTypeOf(expectedSequenceType.getPrimaryType(), actualSequenceType.getPrimaryType()) && !(actualSequenceType.getPrimaryType() == Sequence.EMPTY_SEQUENCE.getItemType() && expectedSequenceType.getCardinality().isSuperCardinalityOrEqualOf(Cardinality.EMPTY_SEQUENCE))) {
            return false;
        }

        if (!expectedSequenceType.getCardinality().isSuperCardinalityOrEqualOf(actualSequenceType.getCardinality())) {
            return false;
        }

        return true;
    }

    /**
     * Processes a compressed archive
     *
     * @param compressedData the compressed data to extract
     * @param encoding the encoding
     * @return Sequence of results
     *
     * @throws XPathException if a query error occurs
     */
    protected abstract Sequence processCompressedData(BinaryValue compressedData, Charset encoding) throws XPathException;

    /**
     * Processes a compressed entry from an archive
     *
     * @param name The name of the entry
     * @param isDirectory true if the entry is a directory, false otherwise
     * @param is an InputStream for reading the uncompressed data of the entry
     *
     * @return the result of processing the compressed entry.
     *
     * @throws XPathException if a query error occurs
     * @throws IOException if an I/O error occurs
     */
    protected Sequence processCompressedEntry(String name, final boolean isDirectory, final InputStream is) throws IOException, XPathException {
        final String dataType = isDirectory ? "folder" : "resource";

        // call the entry-filter function
        filterParams[0] = new StringValue(this, name);
        filterParams[1] = new StringValue(this, dataType);
        final Sequence entryFilterFunctionResult = entryFilterFunction.evalFunction(contextSequence, null, filterParams);
        if (BooleanValue.FALSE == entryFilterFunctionResult.itemAt(0)) {
            return Sequence.EMPTY_SEQUENCE;
        }

        // set common data params
        dataParams[0] = filterParams[0];
        dataParams[1] = filterParams[1];

        // Are we be calling an entry-data or an entry-path function?
        final boolean isEntryPathFunction = Type.subTypeOf(entryDataFunction.getSignature().getReturnType().getPrimaryType(), Type.ANY_URI);
        if (isEntryPathFunction) {
            // an entry-path function
            return callEntryPathFunction(isDirectory, is);

        } else {
            // an entry-data function
            return callEntryDataFunction(isDirectory, is);
        }
    }

    private Sequence callEntryPathFunction(final boolean isDirectory, final InputStream is) throws XPathException, IOException {
        final Sequence entryDataFunctionResult = entryDataFunction.evalFunction(contextSequence, null, dataParams);
        String path = entryDataFunctionResult.itemAt(0).getStringValue();

        final DBBroker broker = context.getBroker();

        try {
            // handle directory entries
            if (isDirectory) {
                try (final Collection collection = broker.getOrCreateCollection(broker.getCurrentTransaction(), XmldbURI.create(path))) {
                    return entryDataFunctionResult;
                }
            }

            // handle file entries
            final Path file = Paths.get(path).normalize();
            path = file.getParent().toAbsolutePath().toString();
            final String name = FileUtils.fileName(file);
            final MimeType mediaType = MimeTable.getInstance().getContentTypeFor(name);

            // store document
            try (final Collection collection = broker.getOrCreateCollection(broker.getCurrentTransaction(), XmldbURI.create(path));
                 final FilterInputStreamCache cache = FilterInputStreamCacheFactory.getCacheInstance(() -> (String) broker.getConfiguration().getProperty(Configuration.BINARY_CACHE_CLASS_PROPERTY), new CloseShieldInputStream(is));
                 final CachingFilterInputStream cfis = new CachingFilterInputStream(cache)) {
                broker.storeDocument(broker.getCurrentTransaction(), XmldbURI.create(name), new CachingFilterInputStreamInputSource(cfis), mediaType, collection);
            }

            return entryDataFunctionResult;
        } catch (final PermissionDeniedException | SAXException | LockException | EXistException e) {
            throw new XPathException(this, e.getMessage(), e);
        }
    }

    private Sequence callEntryDataFunction(final boolean isDirectory, final InputStream is) throws XPathException, IOException {
        // handle directory entries
        final Sequence uncompressedData;
        if (isDirectory) {
            uncompressedData = Sequence.EMPTY_SEQUENCE;
        } else {
            uncompressedData = parseDataToXdm(is);
        }

        //call the entry-data function
        dataParams[2] = uncompressedData;
        return entryDataFunction.evalFunction(contextSequence, null, dataParams);
    }

    private Sequence parseDataToXdm(final InputStream is) throws IOException, XPathException {
        final DBBroker broker = context.getBroker();

        //try and parse as an XML Document, fallback to xs:base64Binary
        try (final FilterInputStreamCache cache = FilterInputStreamCacheFactory.getCacheInstance(() -> (String) broker.getConfiguration().getProperty(Configuration.BINARY_CACHE_CLASS_PROPERTY), new CloseShieldInputStream(is));
             final CachingFilterInputStream cfis = new CachingFilterInputStream(cache)) {
            try {
                return ModuleUtils.streamToXML(context, cfis, this);
            } catch (final SAXException saxe) {
                cfis.reset();
                return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), cfis, this);
            }
        }
    }

}
