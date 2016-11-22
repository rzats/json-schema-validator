package org.rzats.jsonschema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;

@RestController
public class JsonValidatorController implements ErrorController {
    private static final Logger logger = LoggerFactory.getLogger(JsonValidatorController.class);
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String ERROR_PATH = "/error";
    private static final String SCHEMA_PATH = "/schema/";
    private static final String VALIDATE_PATH = "/validate/";

    private static Options createDatabaseOptions() {
        return new Options().setCreateIfMissing(true);
    }

    private static RocksDB createDatabaseConnection(Options databaseOptions) throws RocksDBException {
        return RocksDB.open(databaseOptions, Paths.get("").toAbsolutePath().toString() + "/rocksdb");
    }

    private static String responseToString(JsonValidatorResponse response) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(response);
    }

    @RequestMapping(value = ERROR_PATH)
    public JsonValidatorResponse error(HttpServletResponse response) {
        return new JsonValidatorResponse(null, null, "error",
                String.format("Invalid request (error code %s)", String.valueOf(response.getStatus())));
    }

    @RequestMapping(method = RequestMethod.GET, value = SCHEMA_PATH + "{SCHEMAID}", produces = JSON_CONTENT_TYPE)
    public String downloadSchema(@PathVariable(value = "SCHEMAID") String id) {
        try (Options databaseOptions = createDatabaseOptions();
             RocksDB databaseConnection = createDatabaseConnection(databaseOptions)) {
            byte[] schema = databaseConnection.get(id.getBytes());
            if (schema == null) {
                // ...
            }
            return new String(schema);
        } catch (RocksDBException e) {
            try {
                String responseString = responseToString(new JsonValidatorResponse("uploadSchema", id, "error", String.format("Database exception: %s", e.getMessage())));
                return responseString;
            } catch (JsonProcessingException e1) {
                return String.format("Exception while converting response to string: %s", e1.getMessage());
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = SCHEMA_PATH + "{SCHEMAID}", consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public JsonValidatorResponse uploadSchema(@PathVariable(value = "SCHEMAID") String id, @RequestBody String schema) {
        try (Options databaseOptions = createDatabaseOptions();
             RocksDB databaseConnection = createDatabaseConnection(databaseOptions)) {
            databaseConnection.put(id.getBytes(), schema.getBytes());
            return new JsonValidatorResponse("uploadSchema", id, "success", null);
        } catch (RocksDBException e) {
            return new JsonValidatorResponse("uploadSchema", id, "error", String.format("Database exception: %s", e.getMessage()));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = VALIDATE_PATH + "{SCHEMAID}", consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public JsonValidatorResponse validateDocument(@PathVariable(value = "SCHEMAID") String id, @RequestBody String json) {
        try (Options databaseOptions = createDatabaseOptions();
             RocksDB databaseConnection = createDatabaseConnection(databaseOptions)) {
            byte[] schemaBytes = databaseConnection.get(id.getBytes());
            String schemaString = new String(schemaBytes);
            JsonNode schemaNode = JsonLoader.fromString(schemaString);
            JsonNode documentNode = JsonLoader.fromString(json);

            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

            JsonSchema schema = factory.getJsonSchema(schemaNode);
            ProcessingReport report = schema.validate(documentNode);

            if (report.isSuccess()) {
                return new JsonValidatorResponse("validateDocument", id, "success", null);
            } else {
                return new JsonValidatorResponse("validateDocument", id, "error", report.toString());
            }
        } catch (RocksDBException e) {
            return new JsonValidatorResponse("validateDocument", id, "error", String.format("Database exception: %s", e.getMessage()));
        } catch (IOException e) {
            return new JsonValidatorResponse("validateDocument", id, "error", String.format("Exception while processing JSON: %s", e.getMessage()));
        } catch (ProcessingException e) {
            return new JsonValidatorResponse("validateDocument", id, "error", String.format("Exception while processing JSON schema: %s", e.getMessage()));
        }
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }


}
