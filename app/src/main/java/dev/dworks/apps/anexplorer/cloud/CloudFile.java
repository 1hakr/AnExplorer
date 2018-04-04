package dev.dworks.apps.anexplorer.cloud;

import com.cloudrail.si.types.CloudMetaData;

public class CloudFile {

    CloudMetaData file;

    public CloudFile(String path) {
        file = new CloudMetaData();
        file.setPath(path);
        file.setFolder(true);
    }

    public CloudFile(CloudMetaData cloudMetaData) {
        file =cloudMetaData;
    }

    public String getAbsolutePath() {
        return getPath();
    }

    public String getPath() {
        return file.getPath();
    }

    public String getName() {
        return file.getName();
    }

    public boolean isDirectory() {
        return file.getFolder();
    }

    public boolean canWrite() {
        return true;
    }

    public long getSize() {
        return file.getSize();
    }

    public long lastModified() {
        if(getPath().equals("/")){
            return 0;
        }
        return file.getModifiedAt();
    }
}
