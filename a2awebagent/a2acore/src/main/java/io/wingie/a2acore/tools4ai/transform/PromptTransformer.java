package io.wingie.a2acore.tools4ai.transform;

import io.wingie.a2acore.tools4ai.JsonUtils;
import io.wingie.a2acore.tools4ai.processor.AIProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PromptTransformer interface provides methods for transforming prompts into Java POJOs and JSON.
 * It is used to convert a given prompt text into a specific format based on the provided class name, function name, and description.
 */

public interface PromptTransformer {
    Logger LOGGER = LoggerFactory.getLogger(PromptTransformer.class);
    public default String transformIntoJson(String jsonString, String promptText) throws AIProcessingException{
        return transformIntoJson(jsonString,promptText,"get me values", "Get me the values in json");
    }
    public String transformIntoJson(String jsonString, String promptText, String funName, String description) throws AIProcessingException;
    public default  Object transformIntoPojo(String prompt, String className) throws AIProcessingException {
        return transformIntoPojo(prompt,  className,  "funName",  "description");
    }
    public default  Object transformIntoPojo(String prompt, Class<?> clazz) throws AIProcessingException {
        return transformIntoPojo(prompt,  clazz.getName(),  "funName",  "description");
    }

    public default Object transformIntoPojo(String prompt, String className, String funName, String description) throws AIProcessingException {
        try {
            JsonUtils util = new JsonUtils();
            Class<?> clazz = Class.forName(className);
            String jsonStr;
            if (clazz.getName().equalsIgnoreCase("java.util.Map")) {
                jsonStr = util.buildBlankMapJsonObject(null).toString(4);


            } else if (clazz.getName().equalsIgnoreCase("java.util.List")) {
                jsonStr = util.buildBlankListJsonObject(null).toString(4);


            } else {
                jsonStr = util.convertClassToJSONString(clazz);
            }
            LOGGER.debug("Prompt: for JSON :  {},{}", prompt, jsonStr);
            jsonStr = getJSONResponseFromAI(prompt, jsonStr);
            LOGGER.debug("return JSON :  {}",  jsonStr);

            jsonStr = jsonStr.trim();

            return util.populateClassFromJson(jsonStr);

        } catch (Exception e) {
            throw new AIProcessingException(e);
        }

    }

    public String getJSONResponseFromAI(String prompt, String jsonStr) throws AIProcessingException;
}
