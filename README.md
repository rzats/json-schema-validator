json-schema-validator
================

A REST service for validating JSON documents against JSON schemas. Uses [Spring Boot](https://projects.spring.io/spring-boot/) for the RESTful web service and [RocksDB](https://github.com/facebook/rocksdb) with the [RocksJava](https://github.com/facebook/rocksdb/wiki/RocksJava-Basics) bindings as persistent storage.

# Installation

- `git clone https://github.com/rzats/json-schema-editor.git`
- Run `./gradlew bootRun`. 
  - The associated dependencies (along with a JDK) will be retrieved automatically.

# API endpoints

## POST /schema/SCHEMAID

Uploads a JSON schema with a specified identifier `SCHEMAID`. The schema has to be a valid JSON document, though not necessarily a valid JSON schema. 

If the schema was successfully downloaded, a success response will be returned.

If a schema with the given identifier already exists, an error response will be returned - this can be bypassed by setting an optional `override` URL parameter to 1.

### Examples

_Note: Sample files for the POST requests are found in the `examples` folder._

#### Valid schema upload

**Request**:

`curl http://localhost:8080/schema/config-schema -XPOST -d @config-schema.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "uploadSchema",
  "id" : "config-schema",
  "status" : "success"
}
```

#### Invalid schema upload (not valid JSON)

**Request**:

`curl http://localhost:8080/schema/config-schema -XPOST -d @config-schema.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "uploadSchema",
  "id" : "config-schema",
  "status" : "error",
  "message" : "Invalid JSON: Unexpected character ('\"' (code 34)): was expecting a colon to separate field name and value {...}"
}
```

#### Invalid schema upload (identifier already exists)

**Request**:

`curl http://localhost:8080/schema/config-schema -XPOST -d @config-schema.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "uploadSchema",
  "id" : "config-schema",
  "status" : "error",
  "message" : "Schema with id config-schema already exists (use /schema/SCHEMAID?override=1 to overwrite)"
}
```

#### Valid schema upload (identifier exists, but override used)

**Request**:

`curl http://localhost:8080/schema/config-schema?override=1 -XPOST -d @config-schema.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "uploadSchema",
  "id" : "config-schema",
  "status" : "success"
}
```

## GET /schema/SCHEMAID

Downloads a JSON schema with a specified identifier `SCHEMAID` as a raw JSON string.

If it doesn't exist or cannot be retrieved, an error response is returned.

### Examples

#### Valid schema download

**Request**:

`curl http://localhost:8080/schema/config-schema`

**Response**:

```json
{
  "type" : "object",
  "properties" : {
    "source" : {
      "type" : "string"
    },
    "destination" : {
      "type" : "string"
    },
    "timeout" : {
      "type" : "integer",
      "minimum" : 0,
      "maximum" : 32767
    }
  },
  "required" : [ "source", "destination" ]
}
```

#### Invalid schema download (schema doesn't exist)

**Request**:

`curl http://localhost:8080/schema/nonexistent-schema`

**Response**:

```json
{
  "action" : "downloadSchema",
  "id" : "nonexistent-schema",
  "status" : "error",
  "message" : "Schema with id nonexistent-schema doesn't exist"
}
```


## POST /validate/SCHEMAID

Validates a JSON document against a schema with a specified identifier `SCHEMAID` - the schema has to have been uploaded previously.

If the schema exists in the database, can be retrieved and the document is successfully validated against it, a success response will be returned. Otherwise an error response will be returned.

### Examples

#### Valid document validation

**Request**:

`curl http://localhost:8080/validate/config-schema -XPOST -d @config.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "validateDocument",
  "id" : "config-schema",
  "status" : "success"
}
```

#### Invalid document validation (schema doesn't exist)

**Request**:

`curl http://localhost:8080/validate/nonexistent-schema -XPOST -d @config.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "validateDocument",
  "id" : "nonexistent-schema",
  "status" : "error",
  "message" : "Schema with id nonexistent-schema doesn't exist"
}
```

#### Invalid document validation (document does not conform to schema)

**Request**:

`curl http://localhost:8080/validate/config-schema -XPOST -d @config.json -H 'Content-Type: application/json'`

**Response**:

```json
{
  "action" : "validateDocument",
  "id" : "config-schema",
  "status" : "error",
  "message" : "error: object has missing required properties ([\"source\"])\n    level: \"error\"\n    schema: {...}"
}
```
