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
package org.exist.storage.lock;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import net.jcip.annotations.NotThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A lock event listener which formats events as JSON and writes them to a file
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class LockEventJsonListener implements LockTable.LockEventListener {

    private final static Logger LOG = LogManager.getLogger(LockEventJsonListener.class);

    private volatile boolean registered = false;

    private final Path jsonFile;
    private final boolean prettyPrint;

    private OutputStream os = null;
    private JsonGenerator jsonGenerator = null;


    public LockEventJsonListener(final Path jsonFile) {
        this(jsonFile, false);
    }

    public LockEventJsonListener(final Path jsonFile, final boolean prettyPrint) {
        this.jsonFile = jsonFile;
        this.prettyPrint = prettyPrint;
    }

    @Override
    public void registered() {
        this.registered = true;
        try {
            this.os = Files.newOutputStream(jsonFile,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            final JsonFactory jsonFactory = new JsonFactory();
            this.jsonGenerator = jsonFactory.createGenerator(os, JsonEncoding.UTF8);
            if(prettyPrint) {
                this.jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
            }

            this.jsonGenerator.writeStartObject();
            this.jsonGenerator.writeArrayFieldStart("lockEvents");
        } catch (final IOException e) {
            LOG.error(e);
        }
    }

    @Override
    public void unregistered() {
        try {
            if(jsonGenerator != null) {
                this.jsonGenerator.writeEndArray();
                this.jsonGenerator.writeEndObject();
                this.jsonGenerator.close();
                this.jsonGenerator = null;
            }
        } catch (final IOException e) {
            LOG.error(e);
        }

        try {
            if(os != null) {
                this.os.close();
                this.os = null;
            }
        } catch (final IOException e) {
            LOG.error(e);
        }

        this.registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    @Override
    public void accept(final LockTable.LockEventType lockEventType, final long timestamp, final long groupId,
            final LockTable.Entry entry) {
        if(!registered) {
            return;
        }

        if(jsonGenerator != null) {
            try {
                jsonGenerator.writeStartObject();

                    // read count first to ensure memory visibility from volatile!
                    final int localCount = entry.count;

                    jsonGenerator.writeNumberField("timestamp", timestamp);
                    jsonGenerator.writeStringField("lockEventType", lockEventType.name());
                    jsonGenerator.writeNumberField("groupId", groupId);
                    jsonGenerator.writeStringField("id", entry.id);
                    jsonGenerator.writeStringField("thread", entry.owner);
                    if (entry.stackTraces != null) {
                        for (final StackTraceElement[] stackTrace : entry.stackTraces) {
                            stackTraceToJson(stackTrace);
                        }
                    }

                    jsonGenerator.writeObjectFieldStart("lock");
                        jsonGenerator.writeStringField("type", entry.lockType.name());
                        jsonGenerator.writeStringField("mode", entry.lockMode.name());
                        jsonGenerator.writeNumberField("holdCount", localCount);
                    jsonGenerator.writeEndObject();

                jsonGenerator.writeEndObject();
            } catch(final IOException e) {
                LOG.error(e);
            }
        }
    }

    private void stackTraceToJson(@Nullable final StackTraceElement[] stackTrace) throws IOException {
        jsonGenerator.writeArrayFieldStart("trace");

            if(stackTrace != null) {
                for(final StackTraceElement stackTraceElement : stackTrace) {
                    jsonGenerator.writeStartObject();
                        jsonGenerator.writeStringField("methodName", stackTraceElement.getMethodName());
                        jsonGenerator.writeStringField("className", stackTraceElement.getClassName());
                        jsonGenerator.writeNumberField("lineNumber",  stackTraceElement.getLineNumber());
                    jsonGenerator.writeEndObject();
                }
            }

        jsonGenerator.writeEndArray();
    }
}
