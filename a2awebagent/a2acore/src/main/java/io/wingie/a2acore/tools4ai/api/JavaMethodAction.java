package io.wingie.a2acore.tools4ai.api;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.tools4ai.processor.AIProcessingException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;


public interface JavaMethodAction extends AIAction{


    public default ActionType getActionType (){
        return ActionType.JAVAMETHOD;
    }

    default String getJsonRPC() {
        JSONObject json = new JSONObject();
        json.put("actionName", getActionName());
        json.put("description", getDescription());
        json.put("actionType", getActionType());
        json.put("actionGroup", getActionGroup());
        json.put("actionClass", getActionClassName());
        try {
            String paramsJson = getActionParameters();
            JSONObject actionParams = new JSONObject(paramsJson);  // Convert to JSONObject
            json.put("actionParameters", actionParams);  // Add as nested object
        } catch (Exception e) {
            json.put("actionParameters", getActionParameters());  // Fallback to string
        }

        return json.toString();};
    @Override
    public default String getActionName()  {
        Method firstAnnotatedMethod = null;

        // Get all methods in the class
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class)) {  // Check for Action annotation
                firstAnnotatedMethod = method;  // Store the first annotated method
                break;
            }
        }
        if(firstAnnotatedMethod == null){
            return "No action name available";
        }
        return firstAnnotatedMethod.getName();


    }

    public default String getActionClassName() {
       return this.getClass().getName();
    }

    public default Class<?> getActionClass() {
        return this.getClass();
    }

    @Override
    public default String getDescription() {
        Method firstAnnotatedMethod = null;

        // Get all methods in the class
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class)) {  // Check for Action annotation
                firstAnnotatedMethod = method;  // Store the first annotated method
                break;
            }
        }
        if(firstAnnotatedMethod == null){
            return "No description available";
        }
        Action action = firstAnnotatedMethod.getAnnotation(Action.class);
        String description = action.description();
        if(description == null || description.isEmpty()) {
            description = firstAnnotatedMethod.getName();
        }
        return description;
    }

    @Override
    public default  ActionRisk getActionRisk() {
        return ActionRisk.LOW;
    }


    @Override
    public default String getActionGroup() {
        Agent predict = getActionClass().getAnnotation(Agent.class);
        return predict != null ? predict.groupName(): "No group name available";
    }

    @Override
    default String getGroupDescription() {
        Agent predict = getActionClass().getAnnotation(Agent.class);
        return predict != null ? predict.groupDescription(): "No group description available";
    }

    public default Object getActionInstance(){
        return this;
    }

    public default boolean isComplexMethod() throws AIProcessingException{
        boolean iscomplex = false;
        Method[] met = this.getClass().getMethods();
        for (Method methood:met
             ) {
            if(methood.getName().equals(getActionName())){
                Parameter[] params = methood.getParameters();
                for (Parameter param:params
                     ) {
                    Class<?> type = param.getType();
                    if ((type == String.class) || (type == int.class || type == Integer.class) || (type == double.class || type == Double.class) || (type == boolean.class || type == Boolean.class) || (type.isArray())) {
                     iscomplex = false;
                    } else {
                        iscomplex = true;
                    }

                }
            }
        }
        return iscomplex;
    }




}
