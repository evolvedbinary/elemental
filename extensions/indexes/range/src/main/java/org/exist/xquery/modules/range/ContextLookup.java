/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.xquery.modules.range;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.dom.persistent.NodeProxy;
import org.exist.indexing.range.RangeIndex;
import org.exist.indexing.range.RangeIndexWorker;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.range.RangeIndexModule.*;

/**
 * Lookup context information for a node indexed in the Range Index.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ContextLookup extends BasicFunction {

    /**
     * Indicates that the context size should be taken from the index definition.
     */
    public static final int INDEX_DEF_CONTEXT_SIZE = -1;

    private static final FunctionParameterSequenceType FS_PARAM_NODE = param("node", Type.NODE, "The indexed node to lookup the context for");
    private static final FunctionParameterSequenceType FS_PARAM_CONTEXT_NAME = param("context-name", Type.STRING, "The name of the context to lookup");
    private static final FunctionParameterSequenceType FS_PARAM_MAX_PRE_CONTEXT_SIZE = param("max-pre-context-size", Type.INTEGER, "The maximum pre-context size, from zero and up to that configured in the index definition");
    private static final FunctionParameterSequenceType FS_PARAM_MAX_POST_CONTEXT_SIZE = param("max-post-context-size", Type.INTEGER, "The maximum post-context size, from zero and up to that configured in the index definition");

    private static final String FS_CONTEXT_NAME = "context";
    static final FunctionSignature[] FS_CONTEXT = functionSignatures(
            FS_CONTEXT_NAME,
            "Retrieve the indexed context for a node",
            returnsOpt(Type.ARRAY, "The pre and post context of a node, or raises an error if no context is configured for the node"),
            arities(
               arity(
                       FS_PARAM_NODE,
                       FS_PARAM_CONTEXT_NAME
               ),
               arity(
                       FS_PARAM_NODE,
                       FS_PARAM_CONTEXT_NAME,
                       FS_PARAM_MAX_PRE_CONTEXT_SIZE,
                       FS_PARAM_MAX_POST_CONTEXT_SIZE
               )
            )
    );

    private static final String FS_PRE_CONTEXT_NAME = "pre-context";
    static final FunctionSignature[] FS_PRE_CONTEXT = functionSignatures(
            FS_PRE_CONTEXT_NAME,
            "Retrieve the indexed pre-context for a node",
            returnsOptMany(Type.NODE, "The pre-context of a node, or raises an error if no pre-context is configured for the node"),
            arities(
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_CONTEXT_NAME
                    ),
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_CONTEXT_NAME,
                            FS_PARAM_MAX_PRE_CONTEXT_SIZE
                    )
            )
    );

    private static final String FS_POST_CONTEXT_NAME = "post-context";
    static final FunctionSignature[] FS_POST_CONTEXT = functionSignatures(
            FS_POST_CONTEXT_NAME,
            "Retrieve the indexed post-context for a node",
            returnsOptMany(Type.NODE, "The post-context of a node, or raises an error if no pre-context is configured for the node"),
            arities(
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_CONTEXT_NAME
                    ),
                    arity(
                            FS_PARAM_NODE,
                            FS_PARAM_CONTEXT_NAME,
                            FS_PARAM_MAX_POST_CONTEXT_SIZE
                    )
            )
    );

    public ContextLookup(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final int maxPreContextSize;
        final int maxPostContextSize;

        if (isCalledAs(FS_CONTEXT_NAME) && args.length == 4) {
            maxPreContextSize = args[2].itemAt(0).toJavaObject(Integer.class);
            maxPostContextSize = args[3].itemAt(0).toJavaObject(Integer.class);

        } else if (isCalledAs(FS_PRE_CONTEXT_NAME) && args.length == 3) {
            maxPreContextSize = args[2].itemAt(0).toJavaObject(Integer.class);
            maxPostContextSize = 0;

        } else if (isCalledAs(FS_POST_CONTEXT_NAME) && args.length == 3) {
            maxPreContextSize = 0;
            maxPostContextSize = args[2].itemAt(0).toJavaObject(Integer.class);
        } else {
            maxPreContextSize = INDEX_DEF_CONTEXT_SIZE;
            maxPostContextSize = INDEX_DEF_CONTEXT_SIZE;
        }

        final Item node = args[0].itemAt(0);
        if (!(node instanceof NodeProxy)) {
            throw new XPathException(this, EXXQDYFT0002, "Context lookup may only be used with stored nodes");
        }
        final NodeProxy nodeProxy = (NodeProxy) node;

        final String contextName = args[1].itemAt(0).getStringValue();

        final RangeIndexWorker rangeIndexWorker = (RangeIndexWorker) context.getBroker().getIndexController().getWorkerByIndexId(RangeIndex.ID);
        rangeIndexWorker.setDocument(nodeProxy.getOwnerDocument());

        try {
            @Nullable final Tuple2<Sequence, Sequence> prePostContext = rangeIndexWorker.contextLookup(this, nodeProxy, contextName, maxPreContextSize, maxPostContextSize);
            if (prePostContext == null) {
                throw new XPathException(this, EXXQDYFT0003, "There is no context defined in the index definition for the node: " + nodeProxy.asStoredNode().getPath().toString() + ", in document: " + nodeProxy.getOwnerDocument().getDocumentURI());
            }

            if (isCalledAs(FS_PRE_CONTEXT_NAME)) {
                return prePostContext._1;
            } else if (isCalledAs(FS_POST_CONTEXT_NAME)) {
                return prePostContext._2;
            } else {
                return new ArrayType(this, context, Arrays.asList(prePostContext._1, prePostContext._2));
            }
        } catch (final IOException e) {
            throw new XPathException(this, EXXQDYFT0004, e);
        }
    }
}
