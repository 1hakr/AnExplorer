package dev.dworks.apps.anexplorer.cloud;

import android.text.TextUtils;

import com.cloudrail.si.types.CloudMetaData;

import dev.dworks.apps.anexplorer.misc.FileUtils;

public class CloudFile {

    CloudMetaData file;

    public CloudFile(String path) {
        file = new CloudMetaData();
        file.setPath(path);
        file.setFolder(true);
    }

    public CloudFile(CloudFile parent, String path) {
        String parentPath = parent.getPath();
        String newPath = parent.getPath() + (TextUtils.isEmpty(path) ? "" : (parentPath.endsWith("/") ? "" : "/") + path);
        file = new CloudMetaData();
        file.setPath(newPath);
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
        String name = file.getName();
        name = TextUtils.isEmpty(name) ? FileUtils.getName(getPath()) : name;
        return name;
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
        Long lastModified = file.getModifiedAt();
        return null != lastModified ? lastModified : 0;
    }
}
