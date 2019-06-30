package dev.dworks.apps.anexplorer.misc;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.annotation.NonNull;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;

import static dev.dworks.apps.anexplorer.misc.MimeTypes.BASIC_MIME_TYPE;
import static dev.dworks.apps.anexplorer.provider.StorageProvider.FILE_URI;

public class FileUtils {

    private static final String TAG = "FileUtils";
    private static final int BUFFER = 2048;

    /** Regular expression for safe filenames: no spaces or metacharacters */
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");
    private static final File[] EMPTY = new File[0];
    private static final Locale LOCALE = Resources.getSystem().getConfiguration().locale;

    /**
     * Test if a file lives under the given directory, either as a direct child
     * or a distant grandchild.
     * <p>
     * Both files <em>must</em> have been resolved using
     * {@link File#getCanonicalFile()} to avoid symlink or path traversal
     * attacks.
     */
    public static boolean contains(File[] dirs, File file) {
        for (File dir : dirs) {
            if (contains(dir, file)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Test if a file lives under the given directory, either as a direct child
     * or a distant grandchild.
     * <p>
     * Both files <em>must</em> have been resolved using
     * {@link File#getCanonicalFile()} to avoid symlink or path traversal
     * attacks.
     */
    public static boolean contains(File dir, File file) {
        if (dir == null || file == null) return false;
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

    private static boolean isValidExtFilenameChar(char c) {
        switch (c) {
            case '\0':
            case '/':
                return false;
            default:
                return true;
        }
    }
    /**
     * Check if given filename is valid for an ext4 filesystem.
     */
    public static boolean isValidExtFilename(String name) {
        return (name != null) && name.equals(buildValidExtFilename(name));
    }
    /**
     * Mutate the given filename to make it valid for an ext4 filesystem,
     * replacing any invalid characters with "_".
     */
    public static String buildValidExtFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        final StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (isValidExtFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        trimFilename(res, 255);
        return res.toString();
    }
    private static boolean isValidFatFilenameChar(char c) {
        if ((0x00 <= c && c <= 0x1f)) {
            return false;
        }
        switch (c) {
            case '"':
            case '*':
            case '/':
            case ':':
            case '<':
            case '>':
            case '?':
            case '\\':
            case '|':
            case 0x7F:
                return false;
            default:
                return true;
        }
    }
    /**
     * Check if given filename is valid for a FAT filesystem.
     */
    public static boolean isValidFatFilename(String name) {
        return (name != null) && name.equals(buildValidFatFilename(name));
    }
    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     */
    public static String buildValidFatFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        final StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (isValidFatFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit.
        trimFilename(res, 255);
        return res.toString();
    }

    public static String trimFilename(String str, int maxBytes) {
        final StringBuilder res = new StringBuilder(str);
        trimFilename(res, maxBytes);
        return res.toString();
    }

    private static void trimFilename(StringBuilder res, int maxBytes) {
        byte[] raw = res.toString().getBytes(StandardCharsets.UTF_8);
        if (raw.length > maxBytes) {
            maxBytes -= 3;
            while (raw.length > maxBytes) {
                res.deleteCharAt(res.length() / 2);
                raw = res.toString().getBytes(StandardCharsets.UTF_8);
            }
            res.insert(res.length() / 2, "...");
        }
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
        if (file == null || beforeDir == null || afterDir == null) return null;
        if (contains(beforeDir, file)) {
            final String splice = file.getAbsolutePath().substring(
                    beforeDir.getAbsolutePath().length());
            return new File(afterDir, splice);
        }
        return null;
    }

    public static String formatFileCount(int count) {
        String value = NumberFormat.getInstance().format(count);
        return count == 0 ? "empty" : value + " file" + (count == 1 ? "" : "s");
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

    public static boolean moveDocument(File fileFrom, File fileTo, String name) {

        if (fileTo.isDirectory() && fileTo.canWrite()) {
            if (fileFrom.isFile()) {
                return copyDocument(fileFrom, fileTo, name);
            } else if (fileFrom.isDirectory()) {
                File[] filesInDir = fileFrom.listFiles();
                File filesToDir = new File(fileTo, fileFrom.getName());
                if (!filesToDir.mkdirs()) {
                    return false;
                }

                for (int i = 0; i < filesInDir.length; i++) {
                    moveDocument(filesInDir[i], filesToDir, null);
                }
                return true;
            }
        } else {
            return false;
        }
        return false;
    }

    public static boolean copyDocument(File file, File dest, String name) {
        if (!file.exists() || file.isDirectory()) {
            Log.v(TAG, "copyDocument: file not exist or is directory, " + file);
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
            Log.e(TAG, "copyDocument: file not found, " + file);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "copyDocument: " + e.toString());
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
            destFolder.mkdirs();
            while ((entry = zis.getNextEntry()) != null) {
                File dest = new File(destFolder, entry.getName());
                dest.getParentFile().mkdirs();

                if(entry.isDirectory()) {
                    if (!dest.exists()) {
                        dest.mkdirs();
                    }
                } else {
                    int size;
                    byte[] buffer = new byte[2048];
                    FileOutputStream fos = new FileOutputStream(dest);
                    BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                    while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                        bos.write(buffer, 0, size);
                    }
                    bos.flush();
                    bos.close();
                    IoUtils.flushQuietly(bos);
                    IoUtils.closeQuietly(bos);
                }
                zis.closeEntry();
            }
            IoUtils.closeQuietly(zis);
            IoUtils.closeQuietly(fis);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
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

    private static String getTypeForFile(DocumentFile file) {
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

        return BASIC_MIME_TYPE;
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
            final String name = filename.toLowerCase(LOCALE);
            if (!onlyFolders && (!filename.startsWith("."))) {
                return name.contains(searchQuery);
            } else {
                if (!dir.isDirectory() && !filename.startsWith(".")) {
                    return name.contains(searchQuery) || name.endsWith(searchQuery);
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

    public static void updateMediaStore(Context context, String... pathsArray){
        MediaScannerConnection.scanFile(context, pathsArray, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String s, Uri uri) {
                //Log.i("Scanner", "Scanned " + s + ":");
                //Log.i("Scanner", "-> uri=" + uri);
            }
        });
    }

    public static void updateMediaStore(Context context, ArrayList<DocumentInfo> docs, String parentPath) {
        try {
            if(Utils.hasKitKat()){
                ArrayList<String> paths = new ArrayList<>();
                for(DocumentInfo doc : docs){
                    paths.add(parentPath + File.separator + doc.displayName);
                }
                String[] pathsArray = paths.toArray(new String[paths.size()]);
                FileUtils.updateMediaStore(context, pathsArray);
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

    public static void updateMediaStore(Context context, String path) {
        try {
            if(Utils.hasKitKat()){
                FileUtils.updateMediaStore(context, new String[]{path});
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

    public static void removeMediaStore(Context context, File file) {
        try {
            final ContentResolver resolver = context.getContentResolver();

            // Remove media store entries for any files inside this directory, using
            // path prefix match. Logic borrowed from MtpDatabase.
            if (file.isDirectory()) {
                final String path = file.getAbsolutePath() + "/";
                resolver.delete(FILE_URI,
                        "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)",
                        new String[] { path + "%", Integer.toString(path.length()), path });
            }

            // Remove media store entry for this exact file.
            final String path = file.getAbsolutePath();
            resolver.delete(FILE_URI,
                    "_data LIKE ?1 AND lower(_data)=lower(?2)",
                    new String[] { path, path });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Generates a unique file name under the given parent directory. If the display name doesn't
     * have an extension that matches the requested MIME type, the default extension for that MIME
     * type is appended. If a file already exists, the name is appended with a numerical value to
     * make it unique.
     *
     * For example, the display name 'example' with 'text/plain' MIME might produce
     * 'example.txt' or 'example (1).txt', etc.
     *
     * @throws FileNotFoundException
     */
    public static File buildUniqueFile(File parent, String mimeType, String displayName)
            throws FileNotFoundException {
        String name;
        String ext;
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            name = displayName;
            ext = null;
        } else {
            String mimeTypeFromExt;
            // Extract requested extension from display name
            final int lastDot = displayName.lastIndexOf('.');
            if (lastDot >= 0) {
                name = displayName.substring(0, lastDot);
                ext = displayName.substring(lastDot + 1);
                mimeTypeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        ext.toLowerCase());
            } else {
                name = displayName;
                ext = null;
                mimeTypeFromExt = null;
            }
            if (mimeTypeFromExt == null) {
                mimeTypeFromExt = BASIC_MIME_TYPE;
            }
            final String extFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                    mimeType);
            if (dev.dworks.apps.anexplorer.libcore.util.Objects.equals(mimeType, mimeTypeFromExt)
                    || dev.dworks.apps.anexplorer.libcore.util.Objects.equals(ext, extFromMimeType)) {
                // Extension maps back to requested MIME type; allow it
            } else {
                // No match; insist that create file matches requested MIME
                name = displayName;
                ext = extFromMimeType;
            }
        }
        File file = buildFile(parent, name, ext);
        // If conflicting file, try adding counter suffix
        int n = 0;
        while (file.exists()) {
            if (n++ >= 32) {
                throw new FileNotFoundException("Failed to create unique file");
            }
            file = buildFile(parent, name + " (" + n + ")", ext);
        }
        return file;
    }

    private static File buildFile(File parent, String name, String ext) {
        if (TextUtils.isEmpty(ext)) {
            return new File(parent, name);
        } else {
            return new File(parent, name + "." + ext);
        }
    }

    public static @NonNull File[] listFilesOrEmpty(File dir) {
        File[] res = dir.listFiles();
        if (res != null) {
            return res;
        } else {
            return EMPTY;
        }
    }

    public static boolean copy(InputStream inputStream, OutputStream outputStream){
        boolean successful = false;
        try {
            IoUtils.copy(inputStream, outputStream);
            outputStream.flush();
            successful = true;
        } catch (IOException e) {
            Log.e("TransferThread", "writing failed");
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(inputStream);
            IoUtils.closeQuietly(outputStream);
        }

        return successful;
    }

    public static boolean moveDocument(Context context, DocumentFile fileFrom, DocumentFile fileTo) {

        if (fileTo.isDirectory() /*&& fileTo.canWrite()*/) {
            if (fileFrom.isFile()) {
                return copyDocument(context, fileFrom, fileTo);
            } else if (fileFrom.isDirectory()) {
                DocumentFile[] filesInDir = fileFrom.listFiles();
                DocumentFile filesToDir = fileTo.createDirectory(fileFrom.getName());
                if (!filesToDir.exists()) {
                    return false;
                }

                for (int i = 0; i < filesInDir.length; i++) {
                    moveDocument(context, filesInDir[i], filesToDir);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean copyDocument(Context context, DocumentFile file, DocumentFile dest) {
        if (!file.exists() || file.isDirectory()) {
            Log.v(TAG, "copyDocument: file not exist or is directory, " + file);
            return false;
        }
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        byte[] data = new byte[BUFFER];
        int read = 0;
        try {
            if (!dest.exists()) {
                dest = dest.getParentFile().createDirectory(dest.getName());
                if(!dest.exists()){
                    return false;
                }
            }

            String mimeType = getTypeForFile(file);
            String displayName = FileUtils.getNameFromFilename(file.getName());
            DocumentFile destFile = dest.createFile(mimeType, displayName);

            int n = 0;
            while (destFile == null && n++ < 32){
                String destName = displayName + " (" + n + ")";
                destFile = dest.createFile(mimeType, destName);
            }

            if(destFile == null){
                return false;
            }

            bos = new BufferedOutputStream(getOutputStream(context, destFile));
            bis = new BufferedInputStream(getInputStream(context, file));
            while ((read = bis.read(data, 0, BUFFER)) != -1) {
                bos.write(data, 0, read);
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "copyDocument: file not found, " + file);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "copyDocument: " + e.toString());
        } finally {
            //flush and close
            IoUtils.flushQuietly(bos);
            IoUtils.closeQuietly(bos);
            IoUtils.closeQuietly(bis);
        }

        return false;
    }


    public static OutputStream getOutputStream(Context context, DocumentFile documentFile) throws FileNotFoundException {
        return context.getContentResolver().openOutputStream(documentFile.getUri());
    }

    public static InputStream getInputStream(Context context, DocumentFile documentFile) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(documentFile.getUri());
    }


    public static String getFilenameFromContentUri(Context context, Uri uri) {
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        Cursor cursor = null;
        String name = "";
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
            }
        } catch (Exception e){
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return name;
    }

    public static Uri getContentUriFromFilePath(Context context, String path) {
        String[] projection = { MediaStore.MediaColumns._ID };
        String selection = MediaStore.MediaColumns.DATA + "= ?";
        String[] selectionArgs = { path };
        Cursor cursor = null;
        try {
            Uri uri = getContentUri(path);
            cursor = context.getContentResolver().query(uri, null,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                return Uri.withAppendedPath(uri, String.valueOf(id));
            }
        } catch (Exception e){
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    public static Uri getContentUri(String path) {
        String mimeType = getTypeForFile(new File(path));
        final String typeOnly = mimeType.split("/")[0];
        if ("audio".equals(typeOnly)) {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if ("image".equals(typeOnly)) {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }else if ("video".equals(typeOnly)) {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    public static File getPreviewFile(File folder){
        File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory();
            }
        });
        if (files.length == 0){
            return null;
        }
        return files[0];
    }
}