package xyz.elemental.test.xsuite;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

public class XQSuiteTestDescriptor extends AbstractTestDescriptor implements Node<XSuiteExecutionContext> {

    public XQSuiteTestDescriptor(final UniqueId uniqueId, final String displayName, final @Nullable TestSource source) {
        super(uniqueId, displayName, source);
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }
}