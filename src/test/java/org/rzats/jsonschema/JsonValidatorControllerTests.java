package org.rzats.jsonschema;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsonValidatorControllerTests {
    @Autowired
    private MockMvc mvc;

    private String readResource(String fileName) throws Exception {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    @Test
    public void testAUploadSchema() throws Exception {
        String badSchema = readResource("badSchema.json");
        String goodSchema = readResource("goodSchema.json");

        String goodSchemaSuccess = new JsonValidatorResponse("uploadSchema", "goodSchema", "success", null)
                .toJsonString();
        String badSchemaError = new JsonValidatorResponse("uploadSchema", "badSchema", "error", null)
                .toJsonString();
        String goodSchemaError = new JsonValidatorResponse("uploadSchema", "goodSchema", "error", "Schema with id goodSchema already exists (use /schema/SCHEMAID?override=1 to overwrite)")
                .toJsonString();

        this.mvc.perform(post("/schema/goodSchema").contentType(MediaType.APPLICATION_JSON).content(goodSchema))
                .andExpect(content().json(goodSchemaSuccess));

        this.mvc.perform(post("/schema/badSchema").contentType(MediaType.APPLICATION_JSON).content(badSchema))
                .andExpect(content().json(badSchemaError));

        this.mvc.perform(post("/schema/goodSchema").contentType(MediaType.APPLICATION_JSON).content(goodSchema))
                .andExpect(content().json(goodSchemaError));

        this.mvc.perform(post("/schema/goodSchema?override=1").contentType(MediaType.APPLICATION_JSON).content(goodSchema))
                .andExpect(content().json(goodSchemaSuccess));
    }

    @Test
    public void testBDownloadSchema() throws Exception {
        String goodSchema = readResource("goodSchema.json");

        String nonexistentSchemaError = new JsonValidatorResponse("downloadSchema", "nonexistentSchema", "error", "Schema with id nonexistentSchema doesn't exist")
                .toJsonString();

        this.mvc.perform(get("/schema/goodSchema"))
                .andExpect(content().json(goodSchema));

        this.mvc.perform(get("/schema/nonexistentSchema"))
                .andExpect(content().json(nonexistentSchemaError));
    }

    @Test
    public void testCValidateDocument() throws Exception {
        String invalidDocument = readResource("validDocument.json");
        String validDocument = readResource("invalidDocument.json");

        String validDocumentSuccess = new JsonValidatorResponse("validateDocument", "goodSchema", "success", null).toJsonString();
        String validDocumentError= new JsonValidatorResponse("validateDocument", "nonexistentSchema", "error", "Schema with id nonexistentSchema doesn't exist")
                .toJsonString();
        String invalidDocumentError = new JsonValidatorResponse("validateDocument", "goodSchema", "error", null).toJsonString();

        this.mvc.perform(post("/validate/goodSchema").contentType(MediaType.APPLICATION_JSON).content(validDocument))
                .andExpect(content().json(validDocumentSuccess));

        this.mvc.perform(post("/validate/nonexistentSchema").contentType(MediaType.APPLICATION_JSON).content(validDocument))
                .andExpect(content().json(validDocumentError));

        this.mvc.perform(post("/validate/goodSchema").contentType(MediaType.APPLICATION_JSON).content(invalidDocument))
                .andExpect(content().json(invalidDocumentError));
    }
}
