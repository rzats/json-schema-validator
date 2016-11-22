package org.rzats.jsonschema;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonValidatorResponse {
    private String action;
    private String id;
    private String status;
    private String message;

    public JsonValidatorResponse(String action, String id, String status, String message) {
        this.action = action;
        this.id = id;
        this.status = status;
        this.message = message;
    }

    public String getAction() {
        return action;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
