package dev.dworks.apps.anexplorer.transfer.model;

import java.io.IOException;
import java.util.ArrayList;

/**
 * List of items to be transferred
 */
public class Bundle extends ArrayList<Item> {

    private long mTotalSize = 0;

    /**
     * Add the specified item to the bundle for transfer
     */
    public void addItem(Item item) throws IOException {
        add(item);
        mTotalSize += item.getLongProperty(Item.SIZE, true);
    }

    /**
     * Retrieve the total size of the bundle content
     * @return total size in bytes
     */
    public long getTotalSize() {
        return mTotalSize;
    }
}
