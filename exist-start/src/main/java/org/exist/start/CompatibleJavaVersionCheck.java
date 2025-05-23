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
 */
package org.exist.start;

import java.util.Optional;
import java.util.stream.Stream;

import static org.exist.start.CompatibleJavaVersionCheck.IncompatibleJavaVersion.IncompatibleJavaVersion;
import static org.exist.start.Main.ERROR_CODE_INCOMPATIBLE_JAVA_DETECTED;

public class CompatibleJavaVersionCheck {

    private static final IncompatibleJavaVersion[] INCOMPATIBLE_JAVA_VERSIONS = {
            IncompatibleJavaVersion(12),
            IncompatibleJavaVersion(13),
            IncompatibleJavaVersion(14),
            IncompatibleJavaVersion(15, 0, 2)
    };

    private static final String INCOMPATIBLE_JAVA_VERSION_NOTICE =
            "*****************************************************%n" +
            "Warning: Unreliable Java version has been detected!%n" +
            "%n" +
            "OpenJDK versions 12 through 15.0.1 suffer from a critical%n" +
            " bug in the JIT C2 compiler that will cause data loss in%n" +
            "Elemental.%n" +
            "%n" +
            "The problem has been reported to the OpenJDK community.%n" +
            "%n" +
            "For more information, see:%n" +
            "\t* https://bugs.openjdk.java.net/browse/JDK-8253191%n" +
            "\t* https://github.com/eXist-db/exist/issues/3375%n" +
            "%n" +
            "The detected version of Java on your system is: %s.%n" +
            "%n" +
            "To prevent potential data loss, Elemental will not be started.%n" +
            "To start Elemental, we recommend using Java 8 or 11.%n" +
            "*****************************************************";

    private static final Optional<String> RUNTIME_JAVA_VERSION = Optional.ofNullable(System.getProperty("java.version"));

    /**
     * Checks that the runtime version of Java is compatible.
     *
     * @throws StartException if the runtime version of Java is incompatible.
     */
    public static void checkForCompatibleJavaVersion() throws StartException {
        checkForCompatibleJavaVersion(RUNTIME_JAVA_VERSION);
    }

    static void checkForCompatibleJavaVersion(final Optional<String> checkJavaVersion) throws StartException {
        final Optional<int[]> maybeJavaVersionComponents = extractJavaVersionComponents(checkJavaVersion);

        if (!maybeJavaVersionComponents.isPresent()) {
            // Could not determine major java version, so best to let the user proceed...
            return;
        }

        // check for incompatible java version
        final int[] javaVersionComponents = maybeJavaVersionComponents.get();
        final int majorJavaVersion = javaVersionComponents[0];
        /* @Nullable */ final Integer minorJavaVersion = javaVersionComponents.length > 1 ? javaVersionComponents[1] : null;
        /* @Nullable */ final Integer patchJavaVersion = javaVersionComponents.length > 2 ? javaVersionComponents[2] : null;

        for (int i = 0; i < INCOMPATIBLE_JAVA_VERSIONS.length; i++) {
            final IncompatibleJavaVersion incompatibleJavaVersion = INCOMPATIBLE_JAVA_VERSIONS[i];

            // compare major versions
            if (majorJavaVersion == incompatibleJavaVersion.major) {

                // major version might be incompatible

                if (incompatibleJavaVersion.lessThanMinor != null && minorJavaVersion != null) {
                    // compare minor version
                    if (minorJavaVersion >= incompatibleJavaVersion.lessThanMinor) {
                        // minor version is compatible

                        if (incompatibleJavaVersion.lessThanPatch != null && patchJavaVersion != null) {
                            // compare patch version
                            if (patchJavaVersion >= incompatibleJavaVersion.lessThanPatch) {
                                // patch version is compatible
                                continue;
                            }
                        }
                    }
                }

                // version is NOT compatible!
                throw new StartException(ERROR_CODE_INCOMPATIBLE_JAVA_DETECTED, String.format(INCOMPATIBLE_JAVA_VERSION_NOTICE, RUNTIME_JAVA_VERSION));
            }

            // version is compatible
        }
    }

    static Optional<int[]> extractJavaVersionComponents(final Optional<String> javaVersion) {
        return javaVersion
                .map(str -> str.split("\\.|_|-"))
                .filter(ary -> ary.length > 0)
                .map(ary ->
                        Stream.of(ary)
                                .filter(str -> !str.isEmpty())
                                .map(str -> { try { return Integer.parseInt(str); } catch (final NumberFormatException e) { return -1; }})
                                .filter(i -> i != -1)
                                .mapToInt(Integer::intValue)
                                .toArray()
                )
                .filter(ary -> ary.length > 0);
    }

    static class IncompatibleJavaVersion {
        final int major;
        /* @Nullable */  final Integer lessThanMinor;
        /* @Nullable */ final Integer lessThanPatch;

        private IncompatibleJavaVersion(final int major, /* @Nullable */ Integer lessThanMinor, /* @Nullable */ Integer lessThanPatch) {
            this.major = major;
            this.lessThanMinor = lessThanMinor;
            this.lessThanPatch = lessThanPatch;
        }

        public static IncompatibleJavaVersion IncompatibleJavaVersion(final int major, /* @Nullable */ Integer lessThanMinor, /* @Nullable */ Integer lessThanPatch) {
            return new IncompatibleJavaVersion(major, lessThanMinor, lessThanPatch);
        }

        public static IncompatibleJavaVersion IncompatibleJavaVersion(final int major, /* @Nullable */ Integer lessThanMinor) {
            return IncompatibleJavaVersion(major, lessThanMinor, null);
        }

        public static IncompatibleJavaVersion IncompatibleJavaVersion(final int major) {
            return IncompatibleJavaVersion(major, null, null);
        }
    }
}
