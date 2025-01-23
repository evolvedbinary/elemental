/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.exist.util.DatabaseConfigurationException;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Handles a reference to a context of an index definition:
 *
 * <pre>
 *   &lt;create qname="@lemma"&gt;
 *       &lt;context ref="context-id" name="lemma-context"/&gt;
 *  &lt;/create&gt;
 * </pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RangeIndexConfigContextRefElement {

    private static final String REF_ATTR = "ref";
    private static final String NAME_ATTR = "name";

    private final RangeIndexConfigContextElement rangeIndexConfigContextElement;
    private final String ref;
    private final String name;

    public RangeIndexConfigContextRefElement(final Element element, final Map<String, RangeIndexConfigContextElement> contextConfigs) throws DatabaseConfigurationException {
        final String ref = element.getAttribute(REF_ATTR);
        if (ref.isEmpty()) {
            throw new DatabaseConfigurationException("Range index module: context reference must have a valid ref attribute");
        }
        this.rangeIndexConfigContextElement = contextConfigs.get(ref);
        if (this.rangeIndexConfigContextElement == null) {
            throw new DatabaseConfigurationException("Range index module: context reference refers to a context '" + ref + "' which is not defined in the config");
        }
        this.ref = ref;

        final String name = element.getAttribute(NAME_ATTR);
        if (name.isEmpty()) {
            throw new DatabaseConfigurationException("Range index module: context reference must have a valid name attribute");
        }
        this.name = name;
    }

    public RangeIndexConfigContextElement getRangeIndexConfigContextElement() {
        return rangeIndexConfigContextElement;
    }

    public String getRef() {
        return ref;
    }

    public String getName() {
        return name;
    }
}
