package io.wingie.a2acore.tools4ai.processor;

public class AIProcessingException extends Exception {
    public AIProcessingException(Exception e){
        super(e);
    }
    public AIProcessingException(String e) {
        super(e);
    }
}
