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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Paths;

@RestController
public class JsonValidatorController {
    private static final String JSON_CONTENT_TYPE = "application/json";

    private static Options createDatabaseOptions() {
        return new Options().setCreateIfMissing(true);
    }

    private static RocksDB createDatabaseConnection(Options databaseOptions) throws RocksDBException {
        return RocksDB.open(databaseOptions, Paths.get("").toAbsolutePath().toString() + "/rocksdb");
    }

    @RequestMapping(value = "/error")
    public JsonValidatorResponse error() {
        return new JsonValidatorResponse(null, null, "error", "errorString");
    }

    @RequestMapping(method=RequestMethod.GET, value="/schema/{SCHEMAID}", produces=JSON_CONTENT_TYPE)
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

    @RequestMapping(method= RequestMethod.POST, value="/schema/{SCHEMAID}", consumes = JSON_CONTENT_TYPE,produces=JSON_CONTENT_TYPE)
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

    @RequestMapping(method= RequestMethod.POST, value="/validate/{SCHEMAID}", consumes=JSON_CONTENT_TYPE, produces=JSON_CONTENT_TYPE)
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
}
