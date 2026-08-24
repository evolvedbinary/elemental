package xyz.elemental.test.xsuite;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

public class XMLTestSetDescriptor extends AbstractTestDescriptor implements Node<XSuiteExecutionContext> {

    public XMLTestSetDescriptor(final UniqueId uniqueId, final String displayName, final @Nullable TestSource source) {
        super(uniqueId, displayName, source);
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    @Override
    public XSuiteExecutionContext execute(final XSuiteExecutionContext context, final DynamicTestExecutor executor) {

//        // 1. Arrange (from parent context)
//        context.beforeTest(definition);
//
//        try {
//            // 2. Act — execute the DSL-defined test
//            definition.execute(context);
//
//            // 3. Assert (optional, DSL-dependent)
//            definition.verify(context);
//
//        } finally {
//            // 4. Cleanup
//            context.afterTest(definition);
//        }

        return context;
    }
}
