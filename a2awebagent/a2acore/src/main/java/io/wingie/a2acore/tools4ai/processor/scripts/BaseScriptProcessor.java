package io.wingie.a2acore.tools4ai.processor.scripts;

public interface BaseScriptProcessor {

    public ScriptResult process(String fileName);

    public ScriptResult process(String fileName, ScriptCallback callback);

    default void processWebAction(String line,SeleniumCallback callback,int retryCount){};
}
