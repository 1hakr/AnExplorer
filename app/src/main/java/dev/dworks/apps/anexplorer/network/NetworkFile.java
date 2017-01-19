package dev.dworks.apps.anexplorer.network;

import android.net.Uri;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Created by HaKr on 31/12/16.
 */

public class NetworkFile{

	public static final char separatorChar = '/';
	public static final String separator = "/";
	private String path;
	private FTPFile file;
	private boolean isRoot = false;
	private Uri uri;
	public String host;

	public NetworkFile(String path, String host) {
		this.path = fixSlashes(path);
		this.host = host;
		if(path.equals(separator)){
			setRoot(true);
		}
	}

	public NetworkFile(NetworkFile dir, FTPFile file) {
		String name = file.getName();
		String dirPath = dir.getPath();
		host = dir.host;
		this.file = file;
		if (name == null) {
			throw new NullPointerException("name == null");
		}
		if (dirPath == null || dirPath.isEmpty()) {
			this.path = fixSlashes(name);
		} else if (name.isEmpty()) {
			this.path = fixSlashes(dirPath);
		} else {
			this.path = fixSlashes(join(dirPath, name));
		}
	}

	public String getName() {
		int separatorIndex = path.lastIndexOf(separator);
		return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1, path.length());
	}

	public String getParent() {
		int length = path.length(), firstInPath = 0;
		int index = path.lastIndexOf(separatorChar);
		if (index == -1 || path.charAt(length - 1) == separatorChar) {
			return null;
		}
		if (path.indexOf(separatorChar) == index
				&& path.charAt(firstInPath) == separatorChar) {
			return path.substring(0, index + 1);
		}
		return path.substring(0, index);
	}

	public NetworkFile getParentFile() {
		String tempParent = getParent();
		if (tempParent == null) {
			return null;
		}
		NetworkFile parent = new NetworkFile(tempParent, host);
		return parent;
	}

	/**
	 * Returns the file of this file.
	 */
	public String getPath() {
		return path;
	}

	public String getAbsolutePath() {
		return path;
	}

	public boolean isDirectory() {
		return isRoot || (null != file && file.isDirectory());
	}

	public boolean isFile() {
		return !isRoot && file.isFile();
	}

	public long getSize() {
		return null == file || isRoot ? 0 : file.getSize();
	}

	public boolean canWrite(){
		return true;
	}

	public long lastModified(){
		return null == file || isRoot ? 0 : file.getTimestamp().getTimeInMillis();
	}

	public void setRoot(boolean root){
		isRoot = root;
	}

	public  boolean isRoot(){
		return isRoot;
	}

	public void setHost(String host){
		this.host = host;
	}

	public String getHost(){
		return host;
	}

	public static String fixSlashes(String origPath) {
		// Remove duplicate adjacent slashes.
		boolean lastWasSlash = false;
		char[] newPath = origPath.toCharArray();
		int length = newPath.length;
		int newLength = 0;
		for (int i = 0; i < length; ++i) {
			char ch = newPath[i];
			if (ch == '/') {
				if (!lastWasSlash) {
					newPath[newLength++] = separatorChar;
					lastWasSlash = true;
				}
			} else {
				newPath[newLength++] = ch;
				lastWasSlash = false;
			}
		}
		// Remove any trailing slash (unless this is the root of the file system).
		if (lastWasSlash && newLength > 1) {
			newLength--;
		}
		// Reuse the original string if possible.
		return (newLength != length) ? new String(newPath, 0, newLength) : origPath;
	}

	// Joins two file components, adding a separator only if necessary.
	public static String join(String prefix, String suffix) {
		int prefixLength = prefix.length();
		boolean haveSlash = (prefixLength > 0 && prefix.charAt(prefixLength - 1) == separatorChar);
		if (!haveSlash) {
			haveSlash = (suffix.length() > 0 && suffix.charAt(0) == separatorChar);
		}
		return haveSlash ? (prefix + suffix) : (prefix + separatorChar + suffix);
	}
}