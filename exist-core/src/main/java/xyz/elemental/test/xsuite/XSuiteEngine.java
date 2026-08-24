package xyz.elemental.test.xsuite;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public class XSuiteEngine extends HierarchicalTestEngine<XSuiteExecutionContext> {

    @Override
    public String getId() {
        return "elemental-xsuite";
    }

    @Override
    protected XSuiteExecutionContext createExecutionContext(final ExecutionRequest request) {
        return null;
    }

    @Override
    public TestDescriptor discover(final EngineDiscoveryRequest discoveryRequest, final UniqueId uniqueId) {
        return null;
    }
}
