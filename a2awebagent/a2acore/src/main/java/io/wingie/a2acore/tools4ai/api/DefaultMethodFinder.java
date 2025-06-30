package io.wingie.a2acore.tools4ai.api;


import io.wingie.a2acore.tools4ai.processor.AIProcessingException;

import java.lang.reflect.Method;

public class DefaultMethodFinder implements MethodFinder {
    @Override
    public Method findMethod(Class<?> clazz, String methodName) throws AIProcessingException {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AIProcessingException(methodName + " method not found in class " + clazz.getName());
    }
}