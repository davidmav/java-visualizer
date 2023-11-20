package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodArgument;

import java.lang.reflect.Field;

class ReflectionUtils {

    private ReflectionUtils() {}

    static String methodArgumentToString(MethodArgument methodArgumentConfig, Object targetObject,
                                                 Object[] methodArguments) throws NoSuchFieldException, IllegalAccessException {
        String argumentName = methodArgumentConfig.getArgumentPath();
        String[] split = argumentName.split("\\.");

        switch (methodArgumentConfig.getArgumentType()) {
            case METHOD -> {
                String positionalIndex = split[0];
                if (positionalIndex.charAt(0) == '[' && positionalIndex.charAt(positionalIndex.length() - 1) == ']') {
                    int position = Integer.parseInt(positionalIndex.substring(1, positionalIndex.length() - 1));
                    Object target = methodArguments[position];
                    return objectPathToString(target, split, true);
                } else {
                    throw new IllegalArgumentException("Illegal argumentName for " + methodArgumentConfig.getArgumentType());
                }
            }
            case TARGET -> {
                return objectPathToString(targetObject, split, false);
            }
            default -> {
                throw new IllegalArgumentException("Given " + methodArgumentConfig.getArgumentType() + " is not supported");
            }
        }
    }

    static String objectPathToString(Object targetObject, String[] split, boolean skipFirst) throws IllegalAccessException, NoSuchFieldException {
        Object currentObject = targetObject;
        for (int i = skipFirst ? 1 : 0; i < split.length; i++) {
            String nextField = split[i];
            Field field;
            try {
                field = currentObject.getClass().getField(nextField);
            } catch (NoSuchFieldException e) {
                field = currentObject.getClass().getDeclaredField(nextField);
            }
            if (!field.canAccess(currentObject)) {
                if (!field.trySetAccessible()) {
                    throw new IllegalAccessException("Could not access the field " + field.getName());
                }
            }
            currentObject = field.get(currentObject);
        }

        return currentObject.toString();
    }
}
