package org.rzats.jsonschema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.rzats.jsonschema.database.DatabaseProvider;
import org.rzats.jsonschema.database.DatabaseProviderException;
import org.rzats.jsonschema.database.RocksDbProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The main REST controller. Processes all the API endpoints, as well as error handling.
 */
@RestController
public class JsonValidatorController implements ErrorController {
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String ERROR_PATH = "/error";
    private static final String SCHEMA_PATH = "/schema";
    private static final String VALIDATE_PATH = "/validate";

    @Autowired
    private DatabaseProvider databaseProvider;

    /**
     * Creates a {@link JsonValidatorResponse} instance from the given parameters and returns it as a JSON string.
     *
     * @param action  A short name for the action (validateDocument, uploadSchema etc.)
     * @param id      The unique identifier of a JSON schema
     * @param status  The status of the request - "success" if valid, "error" otherwise
     * @param message The error message - either for internal exceptions or JSON validation errors
     * @return The JSON string representation of the constructed {@link JsonValidatorResponse}.
     */
    private static String responseAsString(String action, String id, String status, String message) {
        return new JsonValidatorResponse(action, id, status, message).toJsonString();
    }

    /**
     * The error mapping - catches HTTP errors (404/500 etc.) generally caused by invalid request URLs/parameters,
     * and wraps their error code in a {@link JsonValidatorResponse}.
     *
     * @param response The HTTP response containing the error code.
     * @return A {@link JsonValidatorResponse} containing the error code.
     */
    @RequestMapping(value = ERROR_PATH)
    public JsonValidatorResponse error(HttpServletResponse response) {
        return new JsonValidatorResponse(null, null, "error",
                String.format("Invalid request (error code %s)", String.valueOf(response.getStatus())));
    }


    /**
     * Downloads a JSON schema with a unique identifier.
     *
     * @param id The unique identifier of the JSON schema.
     * @return the schema's JSON string if it exists and could be successfully retrieved;
     * otherwise, a JSON string representation of a {@link JsonValidatorResponse} containing error details
     */
    @RequestMapping(method = RequestMethod.GET, value = SCHEMA_PATH + "/{SCHEMAID}", produces = JSON_CONTENT_TYPE)
    public String downloadSchema(@PathVariable(value = "SCHEMAID") String id) {
        try {
            // Fetch the schema from the database
            byte[] schemaBytes = databaseProvider.get(id.getBytes());

            if (schemaBytes == null) {
                return responseAsString("downloadSchema", id, "error",
                        String.format("Schema with id %s doesn't exist", id));
            }

            // Convert it to a string and return
            return new String(schemaBytes);
        } catch (DatabaseProviderException e) {
            return responseAsString("downloadSchema", id, "error",
                    String.format("Database exception: %s", e.getMessage()));
        }
    }

    /**
     * Uploads a JSON schema with a unique identifier.
     *
     * @param id       The unique identifier of the JSON schema.
     * @param override If set to 1, existing schemas will be overwritten (otherwise an error is returned)
     * @param schema   The JSON schema.
     * @return a JSON string representation of a {@link JsonValidatorResponse} either indicating a successful upload or containing error details
     */
    @RequestMapping(method = RequestMethod.POST, value = SCHEMA_PATH + "/{SCHEMAID}", consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public String uploadSchema(@PathVariable(value = "SCHEMAID") String id,
                               @RequestParam(required = false, defaultValue = "0") int override,
                               @RequestBody String schema) {
        try {
            if (override == 1 || databaseProvider.get(id.getBytes()) == null) {
                // Check if the schema is valid JSON (but not necessarily a valid schema)
                ObjectMapper mapper = new ObjectMapper();
                String prettySchema;
                try {
                    JsonNode tree = mapper.readTree(schema);
                    prettySchema = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(tree);
                } catch (IOException e) {
                    return responseAsString("uploadSchema", id, "error",
                            String.format("Invalid JSON: %s", e.getMessage()));
                }

                // Upload the schema
                databaseProvider.put(id.getBytes(), prettySchema.getBytes());
                return responseAsString("uploadSchema", id, "success", null);
            } else {
                return responseAsString("uploadSchema", id, "error",
                        String.format("Schema with id %s already exists (use %s/SCHEMAID?override=1 to overwrite)", id, SCHEMA_PATH));
            }
        } catch (DatabaseProviderException e) {
            return responseAsString("uploadSchema", id, "error",
                    String.format("Database exception: %s", e.getMessage()));
        }
    }

    /**
     * Validate a JSON document against a JSON schema.
     *
     * @param id   The unique identifier of the JSON schema.
     * @param json The JSON document.
     * @return a JSON string representation of a {@link JsonValidatorResponse} either indicating a successful validation or containing error details
     */
    @RequestMapping(method = RequestMethod.POST, value = VALIDATE_PATH + "/{SCHEMAID}", consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public String validateDocument(@PathVariable(value = "SCHEMAID") String id,
                                   @RequestBody String json) {
        try {
            // Fetch the schema from the database
            byte[] schemaBytes = databaseProvider.get(id.getBytes());

            if (schemaBytes == null) {
                return responseAsString("validateDocument", id, "error",
                        String.format("Schema with id %s doesn't exist", id));
            }

            String schemaString = new String(schemaBytes);

            // Convert the schema and the document to Json nodes
            JsonNode schemaNode = JsonLoader.fromString(schemaString);
            JsonNode documentNode = JsonLoader.fromString(json);

            // Validate the document against the schema
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(schemaNode);
            ProcessingReport report = schema.validate(documentNode);

            if (report.isSuccess()) {
                return responseAsString("validateDocument", id, "success", null);
            } else {
                StringBuilder messageBuilder = new StringBuilder();
                for (ProcessingMessage message : report) {
                    messageBuilder.append(message);
                }
                return responseAsString("validateDocument", id, "error", messageBuilder.toString());
            }
        } catch (DatabaseProviderException e) {
            return responseAsString("validateDocument", id, "error", String.format("Database exception: %s", e.getMessage()));
        } catch (IOException e) {
            return responseAsString("validateDocument", id, "error", String.format("Exception while processing JSON: %s", e.getMessage()));
        } catch (ProcessingException e) {
            return responseAsString("validateDocument", id, "error", String.format("Exception while processing JSON schema: %s", e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }
}
