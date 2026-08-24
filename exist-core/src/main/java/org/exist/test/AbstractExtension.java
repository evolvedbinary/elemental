package org.exist.test;

import org.junit.jupiter.api.extension.ExtensionContext;

import javax.annotation.Nullable;

public abstract class AbstractExtension {

    protected void put(final ExtensionContext extensionContext, final String key, final @Nullable Object value) {
        getStore(extensionContext).put(key, value);
    }

    protected @Nullable Object get(final ExtensionContext extensionContext, final String key) {
        return getStore(extensionContext).get(key);
    }

    protected @Nullable Object remove(final ExtensionContext extensionContext, final String key) {
        return getStore(extensionContext).remove(key);
    }

    protected abstract ExtensionContext.Store getStore(final ExtensionContext extensionContext);
}
