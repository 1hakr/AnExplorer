package dev.dworks.apps.anexplorer.transfer.model;

import android.net.Uri;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A URL for transfer
 *
 * This type of item includes a single URL that can be sent to other devices
 * for either a notification or opening in the browser.
 */
public class UrlItem extends Item {

    public static final String TYPE_NAME = "url";

    private Map<String, Object> mProperties;

    /**
     * Create an item for receiving a URL
     * @param properties item properties
     */
    public UrlItem(Map<String, Object> properties) throws IOException {
        mProperties = properties;
    }

    /**
     * Create a new item for the specified URI
     */
    public UrlItem(Uri uri) {
        mProperties = new HashMap<>();
        mProperties.put(TYPE, TYPE_NAME);
        mProperties.put(NAME, uri.toString());
        mProperties.put(SIZE, 0);
    }

    /**
     * Retrieve the URL
     */
    public String getUrl() throws IOException {
        return getStringProperty(NAME, true);
    }

    @Override
    public Map<String, Object> getProperties() {
        return mProperties;
    }

    @Override
    public void open(Mode mode) throws IOException {}

    @Override
    public int read(byte[] data) throws IOException {
        return 0;
    }

    @Override
    public void write(byte[] data) throws IOException {}

    @Override
    public void close() throws IOException {}
}
