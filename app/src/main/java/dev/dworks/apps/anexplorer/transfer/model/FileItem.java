package dev.dworks.apps.anexplorer.transfer.model;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An individual file for transfer
 *
 * Note that Android's Java doesn't include java.nio.file so only the
 * last_modified property is usable on the platform.
 */
public class FileItem extends Item {

    public static final String TYPE_NAME = "file";

    // Additional properties for files
    private static final String READ_ONLY = "read_only";
    private static final String EXECUTABLE = "executable";
    private static final String LAST_MODIFIED = "last_modified";

    // Regexp for renaming files
    private static final Pattern sRenamePattern = Pattern.compile("^(.*?)((?:\\.tar)?\\.[^/]*)?$");

    private File mFile;
    private AssetFileDescriptor mAssetFileDescriptor;
    private Map<String, Object> mProperties;

    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    /**
     * Create a new file item using the provided properties
     * @param transferDirectory directory for the file
     * @param properties map of properties for the file
     * @param overwrite true to overwrite an existing file
     */
    public FileItem(String transferDirectory, Map<String, Object> properties, boolean overwrite) throws IOException {
        mProperties = properties;
        File parentDir = new File(transferDirectory);
        String filename = getStringProperty(NAME, true);
        mFile = new File(parentDir, filename);
        if (!overwrite) {
            int i = 2;
            while (mFile.exists()) {
                Matcher matcher = sRenamePattern.matcher(filename);
                if (!matcher.matches()) {
                    throw new IOException("unable to match regexp");
                }
                @SuppressLint("DefaultLocale")
                String newFilename = String.format(
                        "%s_%d%s",
                        matcher.group(1),
                        i++,
                        matcher.group(2) == null ? "" : matcher.group(2)
                );
                mFile = new File(parentDir, newFilename);
            }
        }
    }

    /**
     * Create a new file item from the specified file
     */
    public FileItem(File file) {
        this(file, file.getName());
    }

    /**
     * Create a new file item with the specified filename
     */
    public FileItem(File file, String filename) {
        mFile = file;
        mProperties = new HashMap<>();
        mProperties.put(TYPE, TYPE_NAME);
        mProperties.put(NAME, filename);
        mProperties.put(SIZE, Long.toString(mFile.length()));
        mProperties.put(READ_ONLY, !mFile.canWrite());
        mProperties.put(EXECUTABLE, mFile.canExecute());
        mProperties.put(LAST_MODIFIED, Long.toString(mFile.lastModified()));

        // TODO: these are used for temporary compatibility with 0.3.x
        mProperties.put("created", "0");
        mProperties.put("last_read", "0");
        mProperties.put("directory", false);
    }

    /**
     * Create a file from the specified asset file descriptor and URI
     * @param assetFileDescriptor file descriptor
     * @param filename filename to use
     */
    public FileItem(AssetFileDescriptor assetFileDescriptor, String filename) throws IOException {
        mAssetFileDescriptor = assetFileDescriptor;
        mProperties = new HashMap<>();
        mProperties.put(TYPE, TYPE_NAME);
        mProperties.put(NAME, filename);
        mProperties.put(SIZE, Long.toString(mAssetFileDescriptor.getLength()));

        // TODO: these are used for temporary compatibility with 0.3.x
        mProperties.put("created", "0");
        mProperties.put("last_read", "0");
        mProperties.put("last_modified", "0");
        mProperties.put("directory", false);
    }

    /**
     * Retrieve the underlying path for the item
     */
    public String getPath() {
        return mFile.getPath();
    }

    @Override
    public Map<String, Object> getProperties() {
        return mProperties;
    }

    @Override
    public void open(Mode mode) throws IOException {
        switch (mode) {
            case Read:
                if (mFile != null) {
                    mInputStream = new FileInputStream(mFile);
                } else {
                    mInputStream = new FileInputStream(
                            mAssetFileDescriptor.getFileDescriptor());
                }
                break;
            case Write:
                //noinspection ResultOfMethodCallIgnored
                mFile.getParentFile().mkdirs();
                mOutputStream = new FileOutputStream(mFile);
                break;
        }
    }

    @Override
    public int read(byte[] data) throws IOException {
        int numBytes = mInputStream.read(data);
        if (numBytes == -1) {
            numBytes = 0;
        }
        return numBytes;
    }

    @Override
    public void write(byte[] data) throws IOException {
        mOutputStream.write(data);
    }

    @Override
    public void close() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
            if (mAssetFileDescriptor != null) {
                mAssetFileDescriptor.close();
            }
        }
        if (mOutputStream != null) {
            mOutputStream.close();
            //noinspection ResultOfMethodCallIgnored
            mFile.setWritable(getBooleanProperty(READ_ONLY, false));
            //noinspection ResultOfMethodCallIgnored
            mFile.setExecutable(getBooleanProperty(EXECUTABLE, false));
            long lastModified = getLongProperty(LAST_MODIFIED, false);
            if (lastModified != 0) {
                //noinspection ResultOfMethodCallIgnored
                mFile.setLastModified(lastModified);
            }
        }
    }
}
