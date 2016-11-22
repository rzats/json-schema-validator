package org.rzats.jsonschema.database;

/**
 * An interface acting as a wrapper for key-value databases.
 */
public interface DatabaseProvider {
    /**
     * Returns a byte array storing the value associated with the specified key.
     * If the key is not found, null will be returned.
     *
     * @param key The key, as a byte array.
     * @return The value with the given key, or null if it does not exist.
     * @throws DatabaseProviderException if a database error occurs.
     */
    byte[] get(byte[] key) throws DatabaseProviderException;

    /**
     * Sets the database's entry for the specified key to the value.
     *
     * @param key   The key, as a byte array.
     * @param value The value, as a byte array.
     * @throws DatabaseProviderException if a database error occurs.
     */
    void put(byte[] key, byte[] value) throws DatabaseProviderException;
}
