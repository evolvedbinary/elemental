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
package org.exist.test.runner;

import org.exist.test.ExistEmbeddedServer;
import org.exist.util.XMLFilenameFilter;
import org.exist.util.XQueryFilenameFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Using <code>XSuite</code> as a runner allows you to manually
 * build a suite containing tests from both:
 *
 * 1. XQSuite - as defined in $EXIST_HOME/src/org/exist/xquery/lib/xqsuite/xqsuite.xql
 * 2. XML Test - as defined in $EXIST_HOME/src/org/exist/xquery/lib/test.xq
 *
 * To use it, annotate a class
 * with <code>@RunWith(XSuite.class)</code> and <code>@XSuiteClasses({"extensions/my-extension/src/test/xquery", ...})</code>.
 * When you run this class, it will run all the tests in all the suite classes.
 *
 * @author Adam Retter
 */
public class XSuite extends ParentRunner<Runner> {

    /**
     * Returns an empty suite.
     *
     * @return a runner for an empty suite.
     */
    public static Runner emptySuite() {
        try {
            return new XSuite((Class<?>) null, new String[0]);
        } catch (InitializationError e) {
            throw new RuntimeException("This shouldn't be possible");
        }
    }

    /**
     * The <code>XSuiteFiles</code> annotation specifies the directory/file containing the tests to be run when a class
     * annotated with <code>@RunWith(XSuite.class)</code> is run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface XSuiteFiles {
        /**
         * @return the classes to be run
         */
        String[] value();
    }

    /**
     * The <code>XSuiteParallel</code> annotation specifies that the tests will be run in parallel when a class
     * annotated with <code>@RunWith(XSuite.class)</code> is run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface XSuiteParallel {
    }

    private static String[] getAnnotatedDirectories(final Class<?> klass) throws InitializationError {
        final XSuite.XSuiteFiles annotation = klass.getAnnotation(XSuite.XSuiteFiles.class);
        if (annotation == null) {
            throw new InitializationError(String.format("class '%s' must have a XSuiteFiles annotation", klass.getName()));
        }
        return annotation.value();
    }

    private static boolean hasParallelAnnotation(@Nullable final Class<?> klass) {
        if (klass == null) {
            return false;
        }
        final XSuite.XSuiteParallel annotation = klass.getAnnotation(XSuite.XSuiteParallel.class);
        return annotation != null;
    }

    private final List<Runner> runners;

    /**
     * Called reflectively on classes annotated with <code>@RunWith(XSuite.class)</code>
     *
     * @param klass the root class
     * @param builder builds runners for classes in the suite
     *
     * @throws InitializationError if the XSuite cannot be constructed
     */
    public XSuite(final Class<?> klass, final RunnerBuilder builder) throws InitializationError {
        this(builder, klass, getAnnotatedDirectories(klass));
    }

    /**
     * Call this when there is no single root class (for example, multiple class names
     * passed on the command line to {@link org.junit.runner.JUnitCore}
     *
     * @param builder builds runners for classes in the suite
     * @param suites the directories/files in the suite
     *
     * @throws InitializationError if the XSuite cannot be constructed
     */
    public XSuite(final RunnerBuilder builder, final String[] suites) throws InitializationError {
        this(builder, null, suites);
    }

    /**
     * Call this when the default builder is good enough. Left in for compatibility with JUnit 4.4.
     *
     * @param klass the root of the suite
     * @param suites the directories/files in the suite
     *
     * @throws InitializationError if the XSuite cannot be constructed
     */
    protected XSuite(final Class<?> klass, final String[] suites) throws InitializationError {
        this(null, klass, suites);
    }

    /**
     * Called by this class and subclasses once the classes making up the suite have been determined.
     *
     * @param builder builds runners for classes in the suite
     * @param klass the root of the suite
     * @param suites the directories/files in the suite
     *
     * @throws InitializationError if the XSuite cannot be constructed
     */
    protected XSuite(final RunnerBuilder builder, final Class<?> klass, final String[] suites) throws InitializationError {
        this(klass, getRunners(suites, hasParallelAnnotation(klass)));
    }

    /**
     * Called by this class and subclasses once the runners making up the suite have been determined.
     *
     * @param klass root of the suite
     * @param runners for each class in the suite, a {@link Runner}
     *
     * @throws InitializationError if the XSuite cannot be constructed
     */
    protected XSuite(final Class<?> klass, final List<Runner> runners) throws InitializationError {
        super(klass);
        this.runners = Collections.unmodifiableList(runners);
    }

    /**
     * Get the runners for the suiteDirectories.
     *
     * @param suites/files the directories in the suite
     * @param parallel should a runner execute tests in parallel
     *
     * @throws InitializationError if the runners cannot be retrieved
     */
    private static List<Runner> getRunners(final String[] suites, final boolean parallel) throws InitializationError {
        if(suites == null) {
            return Collections.emptyList();
        }

        try {

            final List<Runner> runners = new ArrayList<>();
            for (final String suite : suites) {

                // if directory/file does not exist - throw an exception
                final Path path = Paths.get(suite);
                if (!Files.exists(path)) {
                    throw new InitializationError("XSuite does not exist: " + suite + ". path=" + path.toAbsolutePath().toString());
                }

                if (Files.isDirectory(path)) {
                    // directory of files of test(s)
                    try (final Stream<Path> children = Files.list(path)) {
                        for(final Path child : children.collect(Collectors.toList())) {
                            if(!Files.isDirectory(child)) {
                                final Runner runner = getRunner(child, parallel);
                                if(runner != null) {
                                    runners.add(runner);
                                }
                            }
                        }
                    }
                } else {
                    // just a file of test(s)
                    runners.add(getRunner(path, parallel));
                }
            }

            return runners;
        } catch (final IOException e) {
            throw new InitializationError(e);
        }
    }

    private static @Nullable Runner getRunner(final Path path, final boolean parallel) throws InitializationError {
        if(XMLFilenameFilter.asPredicate().test(path)) {
            return new XMLTestRunner(path, parallel);
        } else if(XQueryFilenameFilter.asPredicate().test(path) && !path.getFileName().toString().equals("runTests.xql")) {
            return new XQueryTestRunner(path, parallel);
        } else {
            return null;
        }
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @Override
    protected Description describeChild(final Runner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {
        runner.run(notifier);
    }

    @Override
    protected Statement withBeforeClasses(final Statement statement) {
        // get @BeforeClass methods
        final List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(BeforeClass.class);

        // inject a Server startup as though it were an @BeforeClass
        final Statement startExistDb = new StartExistDbStatement();

        return new RunXSuiteBefores(statement, startExistDb, befores, null);
    }

    @Override
    protected Statement withAfterClasses(final Statement statement) {
        // get @AfterClass methods
        final List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(AfterClass.class);

        // inject a Server shutdown as though it were an @AfterClass
        final Statement stopExist = new StopExistDbStatement();

        return new RunXSuiteAfters(statement, stopExist, afters, null);
    }

    private static class RunXSuiteBefores extends Statement {
        private final Statement next;
        private final Object target;
        private final Statement beforeXSuite;
        private final List<FrameworkMethod> befores;

        public RunXSuiteBefores(final Statement next, final Statement beforeXSuite, final List<FrameworkMethod> befores, final Object target) {
            this.next = next;
            this.beforeXSuite = beforeXSuite;
            this.befores = befores;
            this.target = target;
        }

        @Override
        public void evaluate() throws Throwable {
            beforeXSuite.evaluate();
            for (final FrameworkMethod before : befores) {
                before.invokeExplosively(target);
            }
            next.evaluate();
        }
    }

    private static class RunXSuiteAfters extends Statement {
        private final Statement next;
        private final Object target;
        private final Statement afterXSuite;
        private final List<FrameworkMethod> afters;

        public RunXSuiteAfters(final Statement next, final Statement afterXSuite, final List<FrameworkMethod> afters, final Object target) {
            this.next = next;
            this.afterXSuite = afterXSuite;
            this.afters = afters;
            this.target = target;
        }

        @Override
        public void evaluate() throws Throwable {
            final List<Throwable> errors = new ArrayList<>();
            try {
                next.evaluate();
            } catch (final Throwable e) {
                errors.add(e);
            } finally {
                try {
                    afterXSuite.evaluate();
                } catch (final Throwable e) {
                    errors.add(e);
                }
                for (final FrameworkMethod after : afters) {
                    try {
                        after.invokeExplosively(target);
                    } catch (final Throwable e) {
                        errors.add(e);
                    }
                }
            }
            MultipleFailureException.assertEmpty(errors);
        }
    }

    private static class StartExistDbStatement extends Statement {
        @Override
        public void evaluate() throws Throwable {
            if (EXIST_EMBEDDED_SERVER_CLASS_INSTANCE != null) {
                throw new IllegalStateException("EXIST_EMBEDDED_SERVER_CLASS_INSTANCE already instantiated");
            }
            EXIST_EMBEDDED_SERVER_CLASS_INSTANCE = newExistDbServer();
            EXIST_EMBEDDED_SERVER_CLASS_INSTANCE.startDb();
        }
    }

    private static class StopExistDbStatement extends Statement {
        @Override
        public void evaluate() {
            if (EXIST_EMBEDDED_SERVER_CLASS_INSTANCE == null) {
                throw new IllegalStateException("EXIST_EMBEDDED_SERVER_CLASS_INSTANCE already stopped");
            }
            EXIST_EMBEDDED_SERVER_CLASS_INSTANCE.stopDb();
            EXIST_EMBEDDED_SERVER_CLASS_INSTANCE = null;
        }
    }

    static ExistEmbeddedServer EXIST_EMBEDDED_SERVER_CLASS_INSTANCE = null;

    static ExistEmbeddedServer newExistDbServer() {
        return new ExistEmbeddedServer(true, true);
    }
}
