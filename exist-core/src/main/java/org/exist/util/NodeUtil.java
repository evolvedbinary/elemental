package org.exist.util;

import org.exist.dom.QName;
import org.exist.storage.ElementValue;
import org.w3c.dom.Node;

/**
 * Utility for QName creation based on a node type;
 */
public class NodeUtil {

    public static QName createQNameOfType(final short type, final String namespaceURI, final String localName, final String prefix) {
        final byte itype = type == Node.ATTRIBUTE_NODE ? ElementValue.ATTRIBUTE : ElementValue.ELEMENT;
        return new QName(localName, namespaceURI, prefix, itype);
    }
}
