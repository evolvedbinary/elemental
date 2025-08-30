/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package xyz.elemental.build.tools.spoon.processors;

import spoon.SpoonException;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.support.Level;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PermissionRequiredProcessor extends AbstractProcessor<CtElement> {

    private static final String PERMISSION_REQUIRED_ANNOTATION_NAME = "org.exist.security.PermissionRequired";
    private static final String PERMISSION_REQUIRED_ANNOTATION_QUALIFIED_NAME = PERMISSION_REQUIRED_ANNOTATION_NAME + "$PermissionRequired";  // NOTE(AR) needed when Spoon `--cpmode NOCLASSPATH` is set
    private static final String PERMISSION_REQUIRED_UNDEFINED = PERMISSION_REQUIRED_ANNOTATION_NAME + ".UNDEFINED";
    private static final String PERMISSION_REQUIRED_CHECK_CLASS_NAME = "org.exist.security.PermissionRequiredCheck";

    private final boolean consumeAnnotations = true;

    public PermissionRequiredProcessor() {
        super();
    }

    @Override
    public final boolean isToBeProcessed(@Nullable final CtElement element) {
        if (element != null) {
            final List<CtAnnotation<? extends Annotation>> annotations = element.getAnnotations();
            for (final CtAnnotation<? extends Annotation> annotation : annotations) {
                if (PERMISSION_REQUIRED_ANNOTATION_NAME.equals(annotation.getAnnotationType().getQualifiedName())
                        || PERMISSION_REQUIRED_ANNOTATION_QUALIFIED_NAME.equals(annotation.getAnnotationType().getQualifiedName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void process(final CtElement element) {
        if (element instanceof CtMethod method) {
            final String methodName = ((CtClass<?>) method.getParent()).getQualifiedName() + "." + method.getSimpleName();
            final CtBlock<?> methodBody = method.getBody();
            final List<CtAnnotation<?>> permissionRequiredMethodAnnotations = filterAnnotationsByName(method.getAnnotations(), PERMISSION_REQUIRED_ANNOTATION_NAME, PERMISSION_REQUIRED_ANNOTATION_QUALIFIED_NAME);
            for (final CtAnnotation<?> permissionRequiredMethodAnnotation : permissionRequiredMethodAnnotations) {
                final CtCodeSnippetStatement invokeCheckMethodPermissions = constructCheckMethodPermissionsInvocation(permissionRequiredMethodAnnotation);
                methodBody.insertBegin(invokeCheckMethodPermissions);

                logInfo(String.format("Processed method: %s", methodName));

                if (consumeAnnotations) {
                    method.removeAnnotation(permissionRequiredMethodAnnotation);
                }
            }

        } else if (element instanceof CtParameter parameter) {
            final CtExecutable<?> method = parameter.getParent();
            final String methodName = ((CtClass<?>) method.getParent()).getQualifiedName() + "." + method.getSimpleName();
            final CtBlock<?> methodBody = method.getBody();

            final List<CtAnnotation<?>> permissionRequiredMethodAnnotations = filterAnnotationsByName(method.getAnnotations(), PERMISSION_REQUIRED_ANNOTATION_NAME, PERMISSION_REQUIRED_ANNOTATION_QUALIFIED_NAME);

            final List<CtAnnotation<?>> permissionRequiredMethodParameterAnnotations = filterAnnotationsByName(parameter.getAnnotations(), PERMISSION_REQUIRED_ANNOTATION_NAME, PERMISSION_REQUIRED_ANNOTATION_QUALIFIED_NAME);
            for (final CtAnnotation<?> permissionRequiredMethodParameterAnnotation : permissionRequiredMethodParameterAnnotations) {
                @Nullable final CtAnnotation<?> permissionRequiredMethodAnnotation;
                if (permissionRequiredMethodAnnotations.isEmpty()) {
                    permissionRequiredMethodAnnotation = null;
                } else if (permissionRequiredMethodAnnotations.size() == 1) {
                    permissionRequiredMethodAnnotation = permissionRequiredMethodAnnotations.get(0);
                } else {
                    // NOTE(AR) at the moment we can only handle one method annotation when there is a parameter annotation
                    throw new SpoonException("Only one " + PERMISSION_REQUIRED_ANNOTATION_NAME + " annotation is allowed on method: " + methodName);
                }
                final CtCodeSnippetStatement invokeCheckMethodParameterPermissions = constructCheckMethodParameterPermissionsInvocation(permissionRequiredMethodAnnotation, permissionRequiredMethodParameterAnnotation);
                methodBody.insertBegin(invokeCheckMethodParameterPermissions);

                logInfo(String.format("Processed parameter: %s on method: %s", parameter.getSimpleName(), methodName));

                if (consumeAnnotations) {
                    parameter.removeAnnotation(permissionRequiredMethodParameterAnnotation);
                }
            }

        } else {
            throw new SpoonException("The " + PERMISSION_REQUIRED_ANNOTATION_NAME + " annotation is only supported on methods and method parameters");
        }
    }

    private CtCodeSnippetStatement constructCheckMethodPermissionsInvocation(final CtAnnotation<?> permissionRequiredMethodAnnotation) {
        final CtCodeSnippetStatement snippet = getFactory().Core().createCodeSnippetStatement();

        final Requires methodRequires = getRequires(permissionRequiredMethodAnnotation);

        final String code = String.format("%s.checkMethodPermissions(this, (byte)(%s), (byte)(%s), (byte)(%s))",
            PERMISSION_REQUIRED_CHECK_CLASS_NAME,
            methodRequires.user,
            methodRequires.group,
            methodRequires.mode
        );

        snippet.setValue(code);
        return snippet;
    }

    private CtCodeSnippetStatement constructCheckMethodParameterPermissionsInvocation(@Nullable final CtAnnotation<?> permissionRequiredMethodAnnotation, final CtAnnotation<?> permissionRequiredMethodParameterAnnotation) {
        final CtCodeSnippetStatement snippet = getFactory().Core().createCodeSnippetStatement();

        final Requires methodRequires = getRequires(permissionRequiredMethodAnnotation);
        final String parameterName = permissionRequiredMethodParameterAnnotation.getParent(CtParameter.class).getSimpleName();
        final Requires methodParameterRequires = getRequires(permissionRequiredMethodParameterAnnotation);

        final String code = String.format("%s.checkMethodParameterPermissions(this, (byte)(%s), (byte)(%s), (byte)(%s), %s, (byte)(%s), (byte)(%s), (byte)(%s))",
            PERMISSION_REQUIRED_CHECK_CLASS_NAME,
            methodRequires.user,
            methodRequires.group,
            methodRequires.mode,
            parameterName,
            methodParameterRequires.user,
            methodParameterRequires.group,
            methodParameterRequires.mode
        );

        snippet.setValue(code);
        return snippet;
    }

    private Requires getRequires(@Nullable final CtAnnotation<?> annotation) {
        String user = PERMISSION_REQUIRED_UNDEFINED;
        String group = PERMISSION_REQUIRED_UNDEFINED;
        String mode = PERMISSION_REQUIRED_UNDEFINED;

        if (annotation != null) {
            for (final Map.Entry<String, CtExpression> value : annotation.getValues().entrySet()) {
                switch (value.getKey()) {
                    case "user":
                        user = value.getValue().toString();
                        break;
                    case "group":
                        group = value.getValue().toString();
                        break;
                    case "mode":
                        mode = value.getValue().toString();
                        break;
                }
            }
        }

        return new Requires(user, group, mode);
    }

    private static List<CtAnnotation<?>> filterAnnotationsByName(final List<CtAnnotation<?>> annotations, final String... annotationNames) {
        @Nullable List<CtAnnotation<?>> filteredAnnotations = null;
        for (final CtAnnotation<?> annotation : annotations) {
            for (final String annotationName : annotationNames) {
                if (annotationName.equals(annotation.getAnnotationType().getQualifiedName())) {
                    if (filteredAnnotations == null) {
                        filteredAnnotations = new ArrayList<>(annotations.size());
                    }
                    filteredAnnotations.add(annotation);
                }
            }
        }

        if (filteredAnnotations == null) {
            filteredAnnotations = Collections.emptyList();
        }

        return filteredAnnotations;
    }

    private void logInfo(final String message) {
        getEnvironment().report(this, Level.INFO, message);
    }

    private static class Requires {
        final String user;
        final String group;
        final String mode;

        public Requires(final String user, final String group, final String mode) {
            this.user = user;
            this.group = group;
            this.mode = mode;
        }
    }
}
