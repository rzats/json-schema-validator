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
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

@RestController
public class JsonValidatorController implements ErrorController{
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

    @RequestMapping(value = ERROR_PATH)
    public JsonValidatorResponse error(HttpServletResponse response) {
        return new JsonValidatorResponse(null, null, "error",
                String.format("Invalid request (error code %s)", String.valueOf(response.getStatus())));
    }

    @RequestMapping(method=RequestMethod.GET, value= SCHEMA_PATH + "{SCHEMAID}", produces=JSON_CONTENT_TYPE)
    public String downloadSchema(@PathVariable(value = "SCHEMAID") String id) {
        try (Options databaseOptions = createDatabaseOptions();
        RocksDB databaseConnection = createDatabaseConnection(databaseOptions)){
            byte[] schema = databaseConnection.get(id.getBytes());
            String schemaString = new String(schema);
            return schemaString;
        } catch (RocksDBException e) {
            JsonValidatorResponse response = new JsonValidatorResponse("downloadSchema", id, "success", null);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = null;
            try {
                json = ow.writeValueAsString(response);
            } catch (JsonProcessingException e1) {
                e1.printStackTrace();
            }
            return json;
        }

    }

    @RequestMapping(method= RequestMethod.POST, value= SCHEMA_PATH + "{SCHEMAID}", consumes = JSON_CONTENT_TYPE,produces=JSON_CONTENT_TYPE)
    public JsonValidatorResponse uploadSchema(@PathVariable(value = "SCHEMAID") String id, @RequestBody String schema) {
        try (Options databaseOptions = createDatabaseOptions();
             RocksDB databaseConnection = createDatabaseConnection(databaseOptions)){
            databaseConnection.put(id.getBytes(), schema.getBytes());
            return new JsonValidatorResponse("uploadSchema", id, "success", null);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(method= RequestMethod.POST, value=VALIDATE_PATH + "{SCHEMAID}", consumes=JSON_CONTENT_TYPE, produces=JSON_CONTENT_TYPE)
    public JsonValidatorResponse validateDocument(@PathVariable(value = "SCHEMAID") String id, @RequestBody String json) {
        try (Options databaseOptions = createDatabaseOptions();
             RocksDB databaseConnection = createDatabaseConnection(databaseOptions)){
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ProcessingException e) {
            e.printStackTrace();
        }
        return new JsonValidatorResponse("validateDocument", id, "success", json);

    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }


}
