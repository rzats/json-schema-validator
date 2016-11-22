package org.rzats.jsonschema.database;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

/**
 * An implementation of {@link DatabaseProvider} using an underlying RocksDB key-value database.
 */
@Service
public class RocksDbProvider implements DatabaseProvider {
    private static final String DATABASE_PATH = "/rocksdb";

    /**
     * Factory method for the RocksDB Options object.
     * Being a C++ object, the options should be disposed using a try-with-resources block (or Options.close()).
     *
     * @return The Options object.
     */
    private Options createDatabaseOptions() {
        return new Options().setCreateIfMissing(true);
    }

    /**
     * Factory method for the RocksDB connection handle.
     * Being a C++ object, the handle should be disposed using a try-with-resources block (or RocksDB.close()).
     *
     * @param databaseOptions The RocksDB Options object. Create an instance of this using {@link #createDatabaseOptions}.
     * @return The RocksDB connection handle.
     * @throws RocksDBException if a database connection could not be established.
     */
    private RocksDB createDatabaseConnection(Options databaseOptions) throws RocksDBException {
        return RocksDB.open(databaseOptions, Paths.get("").toAbsolutePath().toString() + DATABASE_PATH);
    }

    @Override
    public byte[] get(byte[] key) throws DatabaseProviderException {
        try (Options options = createDatabaseOptions(); RocksDB connection = createDatabaseConnection(options)) {
            return connection.get(key);
        } catch (RocksDBException e) {
            throw new DatabaseProviderException(e);
        }
    }

    @Override
    public void put(byte[] key, byte[] value) throws DatabaseProviderException {
        try (Options options = createDatabaseOptions(); RocksDB connection = createDatabaseConnection(options)) {
            connection.put(key, value);
        } catch (RocksDBException e) {
            throw new DatabaseProviderException(e);
        }
    }
}
