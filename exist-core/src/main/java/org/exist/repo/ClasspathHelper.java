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
package org.exist.repo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.SystemProperties;
import org.exist.start.Classpath;
import org.exist.start.EXistClassLoader;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolService;
import org.expath.pkg.repo.FileSystemStorage;
import org.expath.pkg.repo.Package;
import org.expath.pkg.repo.PackageException;
import org.expath.pkg.repo.Packages;
import org.expath.pkg.repo.deps.ProcessorDependency;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to construct classpath for expath modules containing
 * jar files. Part of start.jar
 */
public class ClasspathHelper implements BrokerPoolService {

    private final static Logger LOG = LogManager.getLogger(ClasspathHelper.class);

    @Override
    public void prepare(final BrokerPool brokerPool) {
        final ClassLoader loader = brokerPool.getClassLoader();
        if (!(loader instanceof EXistClassLoader)) {
            return;
        }
        final Classpath cp = new Classpath();
        scanPackages(brokerPool, cp);
        ((EXistClassLoader)loader).addURLs(cp);
    }

    public static void updateClasspath(BrokerPool pool, org.expath.pkg.repo.Package pkg) throws PackageException {
        final ClassLoader loader = pool.getClassLoader();
        if (!(loader instanceof EXistClassLoader))
            {return;}
        if (!isCompatible(pkg)) {
            LOG.warn("Package {} is not compatible with this version of eXist. To avoid conflicts, Java libraries shipping with this package are not loaded.", pkg.getName());
            return;
        }
        final FileSystemStorage.FileSystemResolver resolver = (FileSystemStorage.FileSystemResolver) pkg.getResolver();
        final Path packageDir = resolver.resolveResourceAsFile(".");
        final Classpath cp = new Classpath();
        try {
            scanPackageDir(pkg, cp, packageDir);
            ((EXistClassLoader)loader).addURLs(cp);
        } catch (final IOException e) {
            LOG.warn("An error occurred while updating classpath for package {}", pkg.getName(), e);
        }
    }

    private static void scanPackages(BrokerPool pool, Classpath classpath) {
        try {
            final Optional<ExistRepository> repo = pool.getExpathRepo();
	    if (repo.isPresent()) {
            for (final Packages pkgs : repo.get().getParentRepo().listPackages()) {
                final Package pkg = pkgs.latest();
                if (!isCompatible(pkg)) {
                    LOG.warn("Package {} is not compatible with this version of eXist. To avoid conflicts, Java libraries shipping with this package are not loaded.", pkg.getName());
                } else {
                    try {
                        final FileSystemStorage.FileSystemResolver resolver = (FileSystemStorage.FileSystemResolver) pkg.getResolver();
                        final Path packageDir = resolver.resolveResourceAsFile(".");
                        scanPackageDir(pkg, classpath, packageDir);
                    } catch (final IOException e) {
                        LOG.warn("An error occurred while updating classpath for package {}", pkg.getName(), e);
                    }
                }
            }
	    }
        } catch (final Exception e) {
            LOG.warn("An error occurred while updating classpath for packages", e);
        }
    }

    private static boolean isCompatible(final Package pkg) throws PackageException {
        // determine the Elemental version this package is compatible with
        final Collection<ProcessorDependency> processorDeps = pkg.getProcessorDeps();
        final String procVersion = SystemProperties.getInstance().getSystemProperty("product-version", "1.0.0");
        final String eXistCompatibleProcVersion = SystemProperties.getInstance().getSystemProperty("exist-db-expath-pkg-compatible-version", "6.3.0");
        PackageLoader.Version requiresProcessorVersion = null;
        for (final ProcessorDependency dependency: processorDeps) {
            if (Deployment.PROCESSOR_NAME.equals(dependency.getProcessor()) || Deployment.EXIST_PROCESSOR_NAME.equals(dependency.getProcessor())) {
                if (dependency.getSemver() != null) {
                    requiresProcessorVersion = new PackageLoader.Version(dependency.getSemver(), true);
                } else if (dependency.getSemverMax() != null || dependency.getSemverMin() != null) {
                    requiresProcessorVersion = new PackageLoader.Version(dependency.getSemverMin(), dependency.getSemverMax());
                } else if (dependency.getVersions() != null) {
                    requiresProcessorVersion = new PackageLoader.Version(dependency.getVersions(), false);
                }
                break;
            }
        }
        if (requiresProcessorVersion == null) {

            // does the package contain XQuery Module(s) implemented in Java?
            final ExistPkgInfo existPkgInfo = (ExistPkgInfo) pkg.getInfo("exist");
            if (existPkgInfo == null) {
                // no Java modules
                return true;
            }

            final Set<URI> javaModules = existPkgInfo.getJavaModules();
            if (javaModules == null || javaModules.isEmpty()) {
                // no Java modules
                return true;
            }

            /*
                There are Elemental Java modules in the package,
                but the package does not declare which version
                of Elemental (the processor) that it depends upon,
                therefore we assume it is incompatible.

                NOTE - In older versions of Elemental, if the package
                did not declare a dependency on a specific processor
                version, we would check whether the version of
                Elemental was between 1.4.0 and 2.2.1
                (inclusive). As we are now past Elemental version
                5.2.0, that would always return false!
             */
            return false;

        } else {
            return requiresProcessorVersion.getDependencyVersion().isCompatible(procVersion)
                || requiresProcessorVersion.getDependencyVersion().isCompatible(eXistCompatibleProcVersion);
        }
    }

    private static void scanPackageDir(Package pkg, Classpath classpath, Path module) throws IOException {
        final Path dotExist =  module.resolve(".exist");
        if (Files.exists(dotExist)) {
            if (!Files.isDirectory(dotExist)) {
                throw new IOException("The .exist config dir is not a dir: " + dotExist);
            }

            final Path cp = dotExist.resolve("classpath.txt");
            if (Files.exists(cp)) {
                try (final BufferedReader reader = Files.newBufferedReader(cp)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Path p = Paths.get(line);
                        if (!p.isAbsolute()) {
                            final FileSystemStorage.FileSystemResolver res = (FileSystemStorage.FileSystemResolver) pkg.getResolver();
                            p = res.resolveComponentAsFile(line);
                        }
                        p = p.normalize().toAbsolutePath();

                        if (Files.exists(p)) {
                            classpath.addComponent(p.toString());
                        } else {
                            LOG.warn("Unable to add '" + p + "' to the classpath for EXPath package: " + pkg.getName() + ", as the file does not exist!");
                        }
                    }
                }
            }
        }
    }
}
