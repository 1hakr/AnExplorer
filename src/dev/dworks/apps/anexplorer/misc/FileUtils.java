/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.misc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;

public class FileUtils {

	private static final String TAG = "FileUtils";
    private static final int BUFFER = 2048;
    
    public static String formatFileCount(int count){
    	return count == 0 ? "emtpy" : count + " file" + (count == 1 ? "" : "s");
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

    
    public static boolean moveFile(File fileFrom, File fileTo, String name){

    	if(fileTo.isDirectory() && fileTo.canWrite()){
			if(fileFrom.isFile()){
				//copy file
				return copyFile(fileFrom, fileTo, name);
			}
			else if(fileTo.isDirectory()){
				File[] filesInDir = fileFrom.listFiles();
				File filesToDir = new File(fileTo, fileFrom.getName());
				
				// copy folder
				if(!filesToDir.mkdir()){
					return false;
				}
				
				// copy files in the folder
				for(int i = 0; i < filesInDir.length; i++){
					copyFile(filesInDir[i], filesToDir, null);
				}
				return true;
			}
		}
		else{
			return false;
		}
		return false;
    }
    
    // return new file path if successful, or return null
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
            
            // If conflicting file, try adding counter suffix
            int n = 0;
            while (destFile.exists() && n++ < 32) {
                String destName = 
                		 (!TextUtils.isEmpty(name) 
                 		? name : getNameFromFilename(file.getName()) )+ " (" + n + ")" + "."
                        + getExtFromFilename(file.getName());
                destFile = new File(dest, destName);
            }

            if (!destFile.createNewFile())
                return false;

/*            fo = new FileOutputStream(destFile);
            int count = 102400;
            byte[] buffer = new byte[count];
            while ((read = fi.read(buffer, 0, count)) != -1) {
                fo.write(buffer, 0, read);
            }
            */
			bos = new BufferedOutputStream(new FileOutputStream(destFile));
			bis = new BufferedInputStream(new FileInputStream(file));
			while((read = bis.read(data, 0, BUFFER)) != -1)
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
    
    public static boolean deleteFile(File file){
		if(file.exists() && file.isFile() && file.canWrite()){
			return file.delete();
		}
		else if(file.isDirectory()){
			if (null != file && file.list() != null && file.list().length == 0) {
				return file.delete();
			}
			else{
				String[] fileList = file.list();
				for(String filePaths : fileList){
					File tempFile = new File(file.getAbsolutePath() + "/" + filePaths);
					if(tempFile.isFile()){
						tempFile.delete();
					}
					else{
						deleteFile(tempFile);
						tempFile.delete();
					}
				}
				
			}
			if(file.exists()){
				return file.delete();
			}
		}
		return false;
    }

    public static boolean compressFile(File parent, List<File> files){
        boolean success = false;
        try {
            File dest = new File(parent, FileUtils.getNameFromFilename(files.get(0).getName()) + ".zip");
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(dest));
            compressFile("", zout, files.toArray(new File[0]));
            zout.close();
            success = true;
        }
        catch (Exception e){

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

    public static boolean uncompress(File zipFile){
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
        }
        catch (Exception e){

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


	private static class SearchFilter implements FilenameFilter{
		String searchQuery;
		boolean onlyFolders;
		
		public SearchFilter(String search){
			this.searchQuery = search;
		}
		
		@SuppressWarnings("unused")
		public SearchFilter(String search, boolean onlyFolders) {
			this.onlyFolders = onlyFolders;
		}

		@Override
		public boolean accept(File dir, String filename) {
			if(!onlyFolders && (!filename.startsWith("."))){	
				return filename.toLowerCase(Resources.getSystem().getConfiguration().locale).contains(searchQuery);
			}
			else {	
				if(!dir.isDirectory() && !filename.startsWith(".")){
					return filename.toLowerCase(Resources.getSystem().getConfiguration().locale).contains(searchQuery);	
				}
			}
			return false;
		}	
	}
	
	private static final int KILO = 1024;
    private static final int MEGA = KILO * KILO;
    /**
     * @return A string suitable for display in bytes, kilobytes or megabytes
     *         depending on its size.
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
        } else {
            DecimalFormat onePlace = new DecimalFormat("0.#");
            count = onePlace.format((float) size / (float) MEGA);
            return context.getString(R.string.megabytes, count);
        }
    }
}
