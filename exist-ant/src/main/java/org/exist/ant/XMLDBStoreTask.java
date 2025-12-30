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
package org.exist.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.exist.mediatype.MediaTypeUtil;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.Constants;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.XMLResource;
import xyz.elemental.mediatype.MediaType;
import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.StorageType;
import xyz.elemental.mediatype.impl.MediaTypeImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * An Ant task to store a set of files into eXist.
 *
 * The task expects a nested fileset element. The files selected by the fileset will be stored into the database.
 *
 * New collections can be created as needed. It is also possible to specify that files relative to the base directory should be stored into
 * subcollections of the root collection, where the relative path of the directory corresponds to the relative path of the subcollections.
 *
 * @author wolf
 *
 *         slightly modified by:
 * @author <a href="mailto:peter.klotz@blue-elephant-systems.com">Peter Klotz</a>
 */
public class XMLDBStoreTask extends AbstractXMLDBTask {
    private File mediaTypesFile = null;
    private File srcFile = null;
    private String targetFile = null;
    private List<FileSet> fileSetList = null;
    private boolean createCollection = false;
    private boolean createSubcollections = false;
    private boolean includeEmptyDirs = true;
    private String type = null;
    private String defaultMimeType = null;
    private String forceMimeType = null;
    private MediaTypeResolver mediaTypeResolver = null;

    @Override
    public void execute() throws BuildException {
        if (uri == null) {
            throw new BuildException("you have to specify an XMLDB collection URI");
        }

        if ((fileSetList == null) && (srcFile == null)) {
            throw new BuildException("no file set specified");
        }

        registerDatabase();

        int p = uri.indexOf(XmldbURI.ROOT_COLLECTION);
        if (p == Constants.STRING_NOT_FOUND) {
            throw new BuildException("invalid uri: '" + uri + "'");
        }

        final String baseURI = uri.substring(0, p);

        final String path;
        if (p == (uri.length() - 3)) {
            path = "";
        } else {
            path = uri.substring(p + 3);
        }

        Collection root = null;
        try {

            if (createCollection) {
                root = DatabaseManager.getCollection(baseURI + XmldbURI.ROOT_COLLECTION, user, password);
                root = mkcol(root, baseURI, XmldbURI.ROOT_COLLECTION, path);
            } else {
                root = DatabaseManager.getCollection(uri, user, password);
            }
        } catch (final XMLDBException e) {
            final String msg = "XMLDB exception caught: " + e.getMessage();

            if (failonerror) {
                throw new BuildException(msg, e);
            } else {
                log(msg, e, Project.MSG_ERR);
                return;
            }
        }

        if (root == null) {
            final String msg = "Collection " + uri + " could not be found.";

            if (failonerror) {
                throw new BuildException(msg);
            } else {
                log(msg, Project.MSG_ERR);
            }

        } else {
            Resource res;
            Collection col = root;
            String relDir;
            String prevDir = null;

            if (srcFile != null) {
                log("Storing " + srcFile.getName());

                MediaType mediaType = getMediaTypeResolver().fromFileName(srcFile.getName());
                final String baseMediaType;

                if (forceMimeType != null) {
                    baseMediaType = forceMimeType;

                } else if (mediaType != null) {
                    baseMediaType = mediaType.getIdentifier();

                } else {
                    baseMediaType = defaultMimeType;
                }

                if (type != null) {
                    if ("xml".equals(type) && (mediaType == null || mediaType.getStorageType() != StorageType.XML)) {
                        if (baseMediaType != null) {
                            mediaType = MediaTypeImpl.builder(baseMediaType, StorageType.XML).build();
                        } else {
                            mediaType = mediaTypeResolver.fromString(MediaType.APPLICATION_XML);
                        }

                    } else if ("binary".equals(type) && (mediaType == null || mediaType.getStorageType() != StorageType.BINARY)) {
                        if (baseMediaType != null) {
                            mediaType = MediaTypeImpl.builder(baseMediaType, StorageType.BINARY).build();
                        } else {
                            mediaType = mediaTypeResolver.forUnknown();
                        }
                    }
                }

                // single file
                if (mediaType == null) {
                    final String msg = "Cannot guess mime-type kind for " + srcFile.getName() + ". Treating it as a binary.";
                    log(msg, Project.MSG_ERR);
                    if (baseMediaType != null) {
                        mediaType = MediaTypeImpl.builder(baseMediaType, StorageType.BINARY).build();
                    } else {
                        mediaType = mediaTypeResolver.forUnknown();
                    }
                }

                final String resourceType = mediaType.getStorageType() == StorageType.XML ? XMLResource.RESOURCE_TYPE : BinaryResource.RESOURCE_TYPE;

                if (targetFile == null) {
                    targetFile = srcFile.getName();
                }

                try {
                    log("Creating resource " + targetFile + " in collection " + col.getName() + " of type " + resourceType + " with media-type: " + mediaType.getIdentifier(), Project.MSG_DEBUG);
                    res = col.createResource(targetFile, resourceType);

                    if (srcFile.length() == 0) {
                        // note: solves bug id 2429889 when this task hits empty files
                    } else {
                        res.setContent(srcFile);
                        ((EXistResource) res).setMediaType(mediaType.getIdentifier());
                        col.storeResource(res);
                    }

                    if (permissions != null) {
                        setPermissions(res);
                    }
                } catch (final XMLDBException e) {
                    final String msg = "XMLDB exception caught: " + e.getMessage();

                    if (failonerror) {
                        throw new BuildException(msg, e);
                    } else {
                        log(msg, e, Project.MSG_ERR);
                    }
                }
            } else {

                for (final FileSet fileSet : fileSetList) {
                    log("Storing fileset", Project.MSG_DEBUG);

                    // using fileset
                    final DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
                    scanner.scan();
                    final String[] includedFiles = scanner.getIncludedFiles();
                    final String[] includedDirs = scanner.getIncludedDirectories();
                    log("Found " + includedDirs.length + " directories and " + includedFiles.length + " files.\n");

                    final File baseDir = scanner.getBasedir();

                    if (includeEmptyDirs && createSubcollections) {

                        for (final String included : includedDirs) {

                            try {
                                log("Creating " + included + " ...\n");

                                //TODO : use dedicated function in XmldbURI
                                // check whether the relative file path contains file seps

                                p = included.lastIndexOf(File.separatorChar);

                                if (p != Constants.STRING_NOT_FOUND) {
                                    relDir = included.substring(0, p);

                                    // It's necessary to do this translation on Windows, and possibly MacOS:
                                    relDir = relDir.replace(File.separatorChar, '/');

                                    if (createSubcollections && ((prevDir == null) || (!relDir.equals(prevDir)))) {

                                        //TODO : use dedicated function in XmldbURI
                                        col = mkcol(root, baseURI, XmldbURI.ROOT_COLLECTION + path, relDir);
                                        prevDir = relDir;
                                    }

                                } else {
                                    col = mkcol(root, baseURI, XmldbURI.ROOT_COLLECTION + path, included);
                                }
                            } catch (final XMLDBException e) {
                                final String msg = "XMLDB exception caught: " + e.getMessage();

                                if (failonerror) {
                                    throw new BuildException(msg, e);
                                } else {
                                    log(msg, e, Project.MSG_ERR);
                                }
                            }
                        }
                    }

                    for (final String included : includedFiles) {

                        try {
                            final File file = new File(baseDir, included);
                            log("Storing " + included + " ...\n");

                            //TODO : use dedicated function in XmldbURI
                            // check whether the relative file path contains file seps

                            p = included.lastIndexOf(File.separatorChar);

                            if (p != Constants.STRING_NOT_FOUND) {
                                relDir = included.substring(0, p);

                                // It's necessary to do this translation on Windows, and possibly MacOS:
                                relDir = relDir.replace(File.separatorChar, '/');

                                if (createSubcollections && ((prevDir == null) || (!relDir.equals(prevDir)))) {

                                    //TODO : use dedicated function in XmldbURI
                                    col = mkcol(root, baseURI, XmldbURI.ROOT_COLLECTION + path, relDir);
                                    prevDir = relDir;
                                }

                            } else {

                                // No file separator found in resource name, reset col to the root collection
                                col = root;
                            }

                            MediaType currentMediaType = getMediaTypeResolver().fromFileName(file.getName());
                            final String currentBaseMediaType;

                            if (forceMimeType != null) {
                                currentBaseMediaType = forceMimeType;

                            } else if (currentMediaType != null) {
                                currentBaseMediaType = currentMediaType.getIdentifier();

                            } else {
                                currentBaseMediaType = defaultMimeType;

                            }

                            if (type != null) {

                                if ("xml".equals(type) && (currentMediaType == null || currentMediaType.getStorageType() != StorageType.XML)) {
                                    if (currentBaseMediaType != null) {
                                        currentMediaType = MediaTypeImpl.builder(currentBaseMediaType, StorageType.XML).build();
                                    } else {
                                        currentMediaType = mediaTypeResolver.fromString(MediaType.APPLICATION_XML);
                                    }

                                } else if ("binary".equals(type) && (currentMediaType == null || currentMediaType.getStorageType() != StorageType.BINARY)) {
                                    if (currentBaseMediaType != null) {
                                        currentMediaType = MediaTypeImpl.builder(currentBaseMediaType, StorageType.BINARY).build();
                                    } else {
                                        currentMediaType = mediaTypeResolver.forUnknown();
                                    }
                                }
                            }

                            if (currentMediaType == null) {
                                final String msg = "Cannot find mime-type kind for " + file.getName() + ". Treating it as a binary.";
                                log(msg, Project.MSG_ERR);
                                if (currentBaseMediaType != null) {
                                    currentMediaType = MediaTypeImpl.builder(currentBaseMediaType, StorageType.BINARY).build();
                                } else {
                                    currentMediaType = mediaTypeResolver.forUnknown();
                                }
                            }

                            final String resourceType = currentMediaType.getStorageType() == StorageType.XML ? XMLResource.RESOURCE_TYPE : BinaryResource.RESOURCE_TYPE;
                            log("Creating resource " + file.getName() + " in collection " + col.getName() + " of type " + resourceType + " with media-type: " + currentMediaType.getIdentifier(), Project.MSG_DEBUG);
                            res = col.createResource(file.getName(), resourceType);
                            res.setContent(file);
                            ((EXistResource) res).setMediaType(currentMediaType.getIdentifier());
                            col.storeResource(res);

                            if (permissions != null) {
                                setPermissions(res);
                            }

                        } catch (final XMLDBException e) {
                            final String msg = "XMLDB exception caught: " + e.getMessage();

                            if (failonerror) {
                                throw new BuildException(msg, e);
                            } else {
                                log(msg, e, Project.MSG_ERR);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method allows more than one Fileset per store task!
     *
     * @param set DOCUMENT ME!
     */
    public void addFileset(final FileSet set) {
        if (fileSetList == null) {
            fileSetList = new ArrayList<>();
        }

        fileSetList.add(set);
    }

    public void setSrcFile(final File file) {
        this.srcFile = file;
    }

    public void setTargetFile(final String name) {
        this.targetFile = name;
    }

    public void setCreatecollection(final boolean create) {
        this.createCollection = create;
    }

    public void setCreatesubcollections(final boolean create) {
        this.createSubcollections = create;
    }

    public void setIncludeEmptyDirs(final boolean create) {
        this.includeEmptyDirs = create;
    }

    public void setMimeTypesFile(final File mediaTypesFile) {
        setMediaTypesFile(mediaTypesFile);
    }

    public void setMediaTypesFile(final File mediaTypesFile) {
        this.mediaTypesFile = mediaTypesFile;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setDefaultMimeType(final String mimeType) {
        this.defaultMimeType = mimeType;
    }

    public void setForceMimeType(final String mimeType) {
        this.forceMimeType = mimeType;
    }

    private MediaTypeResolver getMediaTypeResolver() throws BuildException {
        if (mediaTypeResolver == null) {
            if (mediaTypesFile != null && mediaTypesFile.exists()) {
                log("Trying to use MIME Types file " + mediaTypesFile.getAbsolutePath(), Project.MSG_DEBUG);
                mediaTypeResolver = MediaTypeUtil.newMediaTypeResolver(mediaTypesFile.getParentFile().toPath());
            }

            if (mediaTypeResolver == null) {
                log("Using default MIME Types resources", Project.MSG_DEBUG);
                mediaTypeResolver = MediaTypeUtil.newMediaTypeResolver(null);
            }
        }

        return mediaTypeResolver;
    }
}
