package org.rzats.jsonschema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The POJO class used to store REST API responses.
 * Automatically converted to JSON by Spring Boot.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonValidatorResponse {
    private String action;
    private String id;
    private String status;
    private String message;


    /**
     * The class constructor. null parameters are omitted from the JSON representation.
     *
     * @param action  A short name for the action (validateDocument, uploadSchema etc.)
     * @param id      The unique identifier of a JSON schema
     * @param status  The status of the request - "success" if valid, "error" otherwise
     * @param message The error message - either for internal exceptions or JSON validation errors
     */
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

    public String toJsonString() {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            return ow.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return String.format("Exception while converting response to string: %s", e.getMessage());
        }
    }
}
