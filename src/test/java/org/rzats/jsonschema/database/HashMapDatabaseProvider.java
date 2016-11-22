package org.rzats.jsonschema.database;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link DatabaseProvider} using an underlying hashmap.
 */
@Service
@Primary
public class HashMapDatabaseProvider implements DatabaseProvider {
    private Map<ByteArrayWrapper, Byte[]> map = new HashMap<>();

    /**
     * Converts a byte[] array to a Byte[] array.
     *
     * @param primitives The byte[] array.
     * @return The converted Byte[] array.
     */
    private static Byte[] toObjects(byte[] primitives) {
        if (primitives == null) {
            return null;
        }
        Byte[] objects = new Byte[primitives.length];
        for (int i = 0; i < primitives.length; i++) {
            objects[i] = primitives[i];
        }
        return objects;
    }

    /**
     * Converts a Byte[] array to a byte[] array.
     *
     * @param objects The Byte[] array.
     * @return The converted byte[] array.
     */
    private static byte[] toPrimitives(Byte[] objects) {
        if (objects == null) {
            return null;
        }
        byte[] primitives = new byte[objects.length];
        for (int i = 0; i < objects.length; i++) {
            primitives[i] = objects[i];
        }
        return primitives;
    }

    @Override
    public byte[] get(byte[] key) throws DatabaseProviderException {
        return toPrimitives(map.get(new ByteArrayWrapper(key)));
    }

    @Override
    public void put(byte[] key, byte[] value) throws DatabaseProviderException {
        map.put(new ByteArrayWrapper(key), toObjects(value));
    }

    public final class ByteArrayWrapper {
        private final byte[] data;

        public ByteArrayWrapper(byte[] data) {
            if (data == null) {
                throw new NullPointerException();
            }
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ByteArrayWrapper && Arrays.equals(data, ((ByteArrayWrapper) other).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
