package org.exist.util;

/**
 * A property which can be expressed as a {@link Str} or as a {@link String}
 * with maximum efficiency.
 * Use `key` or `string` as appropriate.
 */
public class Prop {

    public final Str key;
    public final String string;

    public static Prop of(final String s) {

        return new Prop(Str.of(s));
    }

    private Prop(final Str key) {
        this.key = key;
        this.string = key.toString();
    }

    @Override public String toString() {
        return String.format("Prop{%s}", string);
    }
}
