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
package org.exist.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class JDKCompatibility {

    /**
     * Gets the {@code modifiers} field from Field.class.
     *
     * Compatible with JDK 8 through 13.
     *
     * @return the modifiers field
     *
     * @throws NoSuchFieldException if the modifiers field does not exist
     * @throws IllegalAccessException if access is not permitted
     */
    public static Field getModifiersField() throws NoSuchFieldException, IllegalAccessException {
        Field modifiersField = null;
        try {
            // JDK 11, 10, 9, 8
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (final NoSuchFieldException e) {
            // JDK 12+
            try {
                final Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                final boolean accessibleBeforeSet = getDeclaredFields0.isAccessible();
                getDeclaredFields0.setAccessible(true);
                final Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                getDeclaredFields0.setAccessible(accessibleBeforeSet);
                for (final Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        modifiersField = field;
                        break;
                    }
                }
                if (modifiersField == null) {
                    throw e;
                }
            } catch (final NoSuchMethodException | InvocationTargetException ex) {
                e.addSuppressed(ex);
                throw e;
            }
        }

        return modifiersField;
    }
}
