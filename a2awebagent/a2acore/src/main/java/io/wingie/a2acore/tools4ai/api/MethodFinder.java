package io.wingie.a2acore.tools4ai.api;

import io.wingie.a2acore.tools4ai.processor.AIProcessingException;

import java.lang.reflect.Method;

public interface MethodFinder {
    Method findMethod(Class<?> clazz, String methodName) throws AIProcessingException;
}
