package org.rzats.jsonschema.database;

/**
 * An exception used in {@link DatabaseProvider} implementations to indicate a database error.
 * <p>
 * Database-specific exceptions (e.g. {@link org.rocksdb.RocksDBException}) in provider implementations
 * should be converted to this exception type.
 */
public class DatabaseProviderException extends Exception {
    public DatabaseProviderException() {
        super();
    }

    public DatabaseProviderException(String message) {
        super(message);
    }

    public DatabaseProviderException(Throwable cause) {
        super(cause);
    }

    public DatabaseProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
