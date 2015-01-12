package dev.dworks.apps.anexplorer.misc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.common.collect.Lists;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.root.RootFile;

public class FileUtils {

    private static final String TAG = "FileUtils";
    private static final int BUFFER = 2048;

    /**
     * Test if a file lives under the given directory, either as a direct child
     * or a distant grandchild.
     * <p/>
     * Both files <em>must</em> have been resolved using
     * {@link File#getCanonicalFile()} to avoid symlink or path traversal
     * attacks.
     */
    public static boolean contains(File dir, File file) {
        if (file == null) return false;
        String dirPath = dir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (dirPath.equals(filePath)) {
            return true;
        }
        if (!dirPath.endsWith("/")) {
            dirPath += "/";
        }
        return filePath.startsWith(dirPath);
    }

    public static boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContents(file);
                }
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * Assert that given filename is valid on ext4.
     */
    public static boolean isValidExtFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c == '\0' || c == '/') {
                return false;
            }
        }
        return true;
    }

    public static String rewriteAfterRename(File beforeDir, File afterDir, String path) {
        if (path == null) return null;
        final File result = rewriteAfterRename(beforeDir, afterDir, new File(path));
        return (result != null) ? result.getAbsolutePath() : null;
    }

    public static String[] rewriteAfterRename(File beforeDir, File afterDir, String[] paths) {
        if (paths == null) return null;
        final String[] result = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            result[i] = rewriteAfterRename(beforeDir, afterDir, paths[i]);
        }
        return result;
    }

    /**
     * Given a path under the "before" directory, rewrite it to live under the
     * "after" directory. For example, {@code /before/foo/bar.txt} would become
     * {@code /after/foo/bar.txt}.
     */
    public static File rewriteAfterRename(File beforeDir, File afterDir, File file) {
        if (file == null) return null;
        if (contains(beforeDir, file)) {
            final String splice = file.getAbsolutePath().substring(
                    beforeDir.getAbsolutePath().length());
            return new File(afterDir, splice);
        }
        return null;
    }

    public static String formatFileCount(int count) {
        String value = NumberFormat.getInstance().format(count);
        return count == 0 ? "emtpy" : value + " file" + (count == 1 ? "" : "s");
    }

    private static List<File> searchFiles(File dir, FilenameFilter filter) {
        List<File> result = new ArrayList<File>();
        File[] filesFiltered = dir.listFiles(filter), filesAll = dir.listFiles();

        if (filesFiltered != null) {
            result.addAll(Arrays.asList(filesFiltered));
        }

        if (filesAll != null) {
            for (File file : filesAll) {
                if (file.isDirectory()) {
                    List<File> deeperList = searchFiles(file, filter);
                    result.addAll(deeperList);
                }
            }
        }
        return result;
    }

    public static ArrayList<File> searchDirectory(String searchPath, String searchQuery) {
        ArrayList<File> totalList = new ArrayList<File>();
        File searchDirectory = new File(searchPath);

        totalList.addAll(searchFiles(searchDirectory, new SearchFilter(searchQuery)));
        return totalList;
    }

    public static boolean moveFile(File fileFrom, File fileTo, String name) {

        if (fileTo.isDirectory() && fileTo.canWrite()) {
            if (fileFrom.isFile()) {
                return copyFile(fileFrom, fileTo, name);
            } else if (fileFrom.isDirectory()) {
                File[] filesInDir = fileFrom.listFiles();
                File filesToDir = new File(fileTo, fileFrom.getName());
                if (!filesToDir.mkdirs()) {
                    return false;
                }

                for (int i = 0; i < filesInDir.length; i++) {
                    moveFile(filesInDir[i], filesToDir, null);
                }
                return true;
            }
        } else {
            return false;
        }
        return false;
    }

    public static boolean copyFile(File file, File dest, String name) {
        if (!file.exists() || file.isDirectory()) {
            Log.v(TAG, "copyFile: file not exist or is directory, " + file);
            return false;
        }
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        byte[] data = new byte[BUFFER];
        int read = 0;
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(file);
            if (!dest.exists()) {
                if (!dest.mkdirs())
                    return false;
            }

            File destFile = new File(dest, !TextUtils.isEmpty(name)
                    ? name + "." + getExtFromFilename(file.getName())
                    : file.getName());

            int n = 0;
            while (destFile.exists() && n++ < 32) {
                String destName =
                        (!TextUtils.isEmpty(name)
                                ? name : getNameFromFilename(file.getName())) + " (" + n + ")" + "."
                                + getExtFromFilename(file.getName());
                destFile = new File(dest, destName);
            }

            if (!destFile.createNewFile())
                return false;
            bos = new BufferedOutputStream(new FileOutputStream(destFile));
            bis = new BufferedInputStream(new FileInputStream(file));
            while ((read = bis.read(data, 0, BUFFER)) != -1)
                bos.write(data, 0, read);

            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "copyFile: file not found, " + file);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "copyFile: " + e.toString());
        } finally {
            try {
                //FIXME
                //flush and close
                bos.flush();
                bis.close();
                bos.close();
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static boolean deleteFile(File file) {
        if (file.exists() && file.isFile() && file.canWrite()) {
            return file.delete();
        } else if (file.isDirectory()) {
            if (null != file && file.list() != null && file.list().length == 0) {
                return file.delete();
            } else {
                String[] fileList = file.list();
                for (String filePaths : fileList) {
                    File tempFile = new File(file.getAbsolutePath() + "/" + filePaths);
                    if (tempFile.isFile()) {
                        tempFile.delete();
                    } else {
                        deleteFile(tempFile);
                        tempFile.delete();
                    }
                }

            }
            if (file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    public static boolean compressFile(File parent, List<File> files) {
        boolean success = false;
        try {
            File dest = new File(parent, FileUtils.getNameFromFilename(files.get(0).getName()) + ".zip");
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(dest));
            compressFile("", zout, files.toArray(new File[files.size()]));
            zout.close();
            success = true;
        } catch (Exception e) {

        }
        return success;
    }

    private static void compressFile(String currentDir, ZipOutputStream zout, File[] files) throws Exception {
        byte[] buffer = new byte[1024];
        for (File fi : files) {
            if (fi.isDirectory()) {
                compressFile(currentDir + "/" + fi.getName(), zout, fi.listFiles());
                continue;
            }
            ZipEntry ze = new ZipEntry(currentDir + "/" + fi.getName());
            FileInputStream fin = new FileInputStream(fi.getPath());
            zout.putNextEntry(ze);
            int length;
            while ((length = fin.read(buffer)) > 0) {
                zout.write(buffer, 0, length);
            }
            zout.closeEntry();
            fin.close();
        }
    }

    public static boolean uncompress(File zipFile) {
        boolean success = false;
        try {
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            File destFolder = new File(zipFile.getParent(), FileUtils.getNameFromFilename(zipFile.getName()));
            //destFolder.mkdirs();
            while ((entry = zis.getNextEntry()) != null) {
                File dest = new File(destFolder, entry.getName());
                dest.getParentFile().mkdirs();
                int size;
                byte[] buffer = new byte[2048];
                FileOutputStream fos = new FileOutputStream(dest);
                BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                    bos.write(buffer, 0, size);
                }
                bos.flush();
                bos.close();
            }
            zis.close();
            fis.close();
            success = true;
        } catch (Exception e) {

        }
        return success;
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }


    public static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    public static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return "application/octet-stream";
    }

    /**
     * Remove file extension from name, but only if exact MIME type mapping
     * exists. This means we can reapply the extension later.
     */
    public static String removeExtension(String mimeType, String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String nameMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType.equals(nameMime)) {
                return name.substring(0, lastDot);
            }
        }
        return name;
    }

    /**
     * Add file extension to name, but only if exact MIME type mapping exists.
     */
    public static String addExtension(String mimeType, String name) {
        final String extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType);
        if (extension != null) {
            return name + "." + extension;
        }
        return name;
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf(File.separator);
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(index+1);
        }
    }

    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('.');
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    public static String getFullNameFromFilepath(String filename) {
        return removeExtension(getName(filename));
    }

    public static String getPathFromFilepath(String filepath) {
        int index = filepath.lastIndexOf(File.separator);
        if (index != -1) {
            int end = index + 1;
            if (end == 0) {
                end++;
            }
            return filepath.substring(0, end);
        }
        return "";
    }

    private static class SearchFilter implements FilenameFilter {
        String searchQuery;
        boolean onlyFolders;

        public SearchFilter(String search) {
            this.searchQuery = search;
        }

        @SuppressWarnings("unused")
        public SearchFilter(String search, boolean onlyFolders) {
            this.onlyFolders = onlyFolders;
        }

        @Override
        public boolean accept(File dir, String filename) {
            if (!onlyFolders && (!filename.startsWith("."))) {
                return filename.toLowerCase(Resources.getSystem().getConfiguration().locale).contains(searchQuery);
            } else {
                if (!dir.isDirectory() && !filename.startsWith(".")) {
                    return filename.toLowerCase(Resources.getSystem().getConfiguration().locale).contains(searchQuery);
                }
            }
            return false;
        }
    }

    private static final int KILO = 1024;
    private static final int MEGA = KILO * KILO;
    private static final int GIGA = MEGA * KILO;

    /**
     * @return A string suitable for display in bytes, kilobytes or megabytes
     * depending on its size.
     */
    public static String convertToHumanReadableSize(Context context, long size) {
        final String count;
        if (size == 0) {
            return "";
        } else if (size < KILO) {
            count = String.valueOf(size);
            return context.getString(R.string.bytes, count);
        } else if (size < MEGA) {
            count = String.valueOf(size / KILO);
            return context.getString(R.string.kilobytes, count);
        } else if (size < GIGA) {
            count = String.valueOf(size / MEGA);
            return context.getString(R.string.megabytes, count);
        }
        else {
            DecimalFormat onePlace = new DecimalFormat("0.#");
            count = onePlace.format((float) size / (float) GIGA);
            return context.getString(R.string.gigabytes, count);
        }
    }

    public static String makeFilePath(String parentPath, String name){
        if(TextUtils.isEmpty(parentPath) || TextUtils.isEmpty(name)){
            return "";
        }
        return parentPath + File.separator + name;
    }

    public static String makeFilePath(File parentFile, String name){
        if(null == parentFile || TextUtils.isEmpty(name)){
            return "";
        }
        return new File(parentFile, name).getPath();
    }

    public static void updateMedia(Context context, String... pathsArray){
        MediaScannerConnection.scanFile(context, pathsArray, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String s, Uri uri) {
                //Log.i("Scanner", "Scanned " + s + ":");
                //Log.i("Scanner", "-> uri=" + uri);
            }
        });
    }

    public static void updateMedia(Context context, ArrayList<DocumentInfo> docs, String parentPath) {
        try {
            if(Utils.hasKitKat()){
                ArrayList<String> paths = Lists.newArrayList();
                for(DocumentInfo doc : docs){
                    paths.add(parentPath + File.separator + doc.displayName);
                }
                String[] pathsArray = paths.toArray(new String[paths.size()]);
                FileUtils.updateMedia(context, pathsArray);
            }
            else{
                Uri contentUri = Uri.fromFile(new File(parentPath).getParentFile());
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
                context.sendBroadcast(mediaScanIntent);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updateMedia(Context context, String path) {
        try {
            if(Utils.hasKitKat()){
                FileUtils.updateMedia(context, new String[]{path});
            }
            else{
                Uri contentUri = Uri.fromFile(new File(path).getParentFile());
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
                context.sendBroadcast(mediaScanIntent);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}