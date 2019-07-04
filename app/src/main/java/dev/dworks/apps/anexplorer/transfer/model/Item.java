package dev.dworks.apps.anexplorer.transfer.model;

import java.io.IOException;
import java.util.Map;

/**
 * Individual item for transfer
 *
 * Every individual file, URL, etc. for transfer must be an instance of a
 * class that implements Item. Items can have any number of properties, but
 * they must implement TYPE, NAME, and SIZE at a minimum.
 *
 * If items contain content (SIZE is nonzero), the I/O functions are used to
 * read and write the contents.
 */
abstract public class Item {

    /**
     * Unique identifier for the type of item
     */
    public static final String TYPE = "type";

    /**
     * Name of the item
     *
     * This value is displayed in some clients during transfer. Files, for
     * example, also use this property for the relative filename.
     */
    public static final String NAME = "name";

    /**
     * Size of the item content during transmission
     *
     * This number is sent over-the-wire as a string to avoid problems with
     * large integers in JSON. This number can be zero if there is no payload.
     */
    public static final String SIZE = "size";

    /**
     * Mode for opening items
     */
    public enum Mode {
        Read,
        Write,
    }

    /**
     * Retrieve a map of properties
     * @return property map
     */
    abstract public Map<String, Object> getProperties();

    /**
     * Retrieve the value of the specified property
     * @param key property to retrieve
     * @param defaultValue default value or null if required
     * @param class_ type for conversion
     * @param <T> type of value
     * @return value of the key
     */
    private <T> T getProperty(String key, T defaultValue, Class<T> class_) throws IOException {
        try {
            T value = class_.cast(getProperties().get(key));
            if (value == null) {
                if (defaultValue == null) {
                    throw new IOException(String.format("missing \"%s\" property", key));
                } else {
                    value = defaultValue;
                }
            }
            return value;
        } catch (ClassCastException e) {
            throw new IOException(String.format("cannot read \"%s\" property", key));
        }
    }

    /**
     * Retrieve the value of a string property
     * @param key property to retrieve
     * @param required true to require a value
     * @return value of the key
     */
    String getStringProperty(String key, boolean required) throws IOException {
        return getProperty(key, required ? "" : null, String.class);
    }

    /**
     * Retrieve the value of a long property
     * @param key property to retrieve
     * @param required true to require a value
     * @return value of the key
     */
    public long getLongProperty(String key, boolean required) throws IOException {
        try {
            return Long.parseLong(getProperty(key, required ? "0" : null, String.class));
        } catch (NumberFormatException e) {
            throw new IOException(String.format("\"%s\" is not an integer", key));
        }
    }

    /**
     * Retrieve the value of a boolean property
     * @param key property to retrieve
     * @param required true to require a value
     * @return value of the key
     */
    boolean getBooleanProperty(String key, boolean required) throws IOException {
        return getProperty(key, required ? null : false, Boolean.class);
    }

    /**
     * Open the item for reading or writing
     * @param mode open mode
     */
    abstract public void open(Mode mode) throws IOException;

    /**
     * Read data from the item
     * @param data array of bytes to read
     * @return number of bytes read
     * @throws IOException
     *
     * This method is invoked multiple times until all content has been read.
     */
    abstract public int read(byte[] data) throws IOException;

    /**
     * Write data to the item
     * @param data array of bytes to write
     */
    abstract public void write(byte[] data) throws IOException;

    /**
     * Close the item
     */
    abstract public void close() throws IOException;
}
