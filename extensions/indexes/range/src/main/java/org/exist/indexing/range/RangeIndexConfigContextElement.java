/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Handles context of an index definition:
 *
 * <pre>
 *   &lt;context id="context-id" qname="c:w" pre-context-size="7" post-context-size="7"/&gt;
 * </pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RangeIndexConfigContextElement extends AbstractRangeIndexConfigElement {

    private static final Logger LOG = LogManager.getLogger(RangeIndexConfigContextElement.class);
    private static final int DEFAULT_CONTEXT_SIZE = 7;

    private final String id;
    private final int preContextSize;
    private final int postContextSize;

    public RangeIndexConfigContextElement(final Element element, final Map<String, String> namespaces) throws DatabaseConfigurationException {
        super(element, namespaces);

        this.id = element.getAttribute("id");
        if (id.isEmpty()) {
            throw new DatabaseConfigurationException("Range index module: context element requires an id attribute");
        }

        int pre = DEFAULT_CONTEXT_SIZE;
        final String preStr = element.getAttribute("pre-context-size");
        if (!preStr.isEmpty()) {
            try {
                pre = Integer.parseInt(preStr);
            } catch (final NumberFormatException e) {
                LOG.warn("Range index module: pre-context-size value is not an integer, switching to default: " + DEFAULT_CONTEXT_SIZE);
            }
        }
        this.preContextSize = pre;

        int post = DEFAULT_CONTEXT_SIZE;
        final String postStr = element.getAttribute("post-context-size");
        if (!postStr.isEmpty()) {
            try {
                post = Integer.parseInt(postStr);
            } catch (final NumberFormatException e) {
                LOG.warn("Range index module: post-context-size value is not an integer, switching to default: " + DEFAULT_CONTEXT_SIZE);
            }
        }
        this.postContextSize = post;
    }

    public String getId() {
        return id;
    }

    public int getPreContextSize() {
        return preContextSize;
    }

    public int getPostContextSize() {
        return postContextSize;
    }

    @Override
    public boolean match(final NodePath other) {
        return path.match(other);
    }

    @Override
    public boolean find(final NodePath other) {
        return match(other);
    }

    @Override
    public TextCollector newCollector(final NodePath path) {
        return new ContextTextCollector(this, path);
    }
}
