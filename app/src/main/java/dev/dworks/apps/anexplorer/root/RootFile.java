package dev.dworks.apps.anexplorer.root;

import java.io.File;

import dev.dworks.apps.anexplorer.misc.FileUtils;

import static java.io.File.separatorChar;

public class RootFile {

	private String  name = "", permission = "", trimName = "";
	private int length = 0;
    private boolean isValid = true;
    private int type = 0;
    private String[] flds;
    private String path;

    public RootFile(String path) {
    	this.path = fixSlashes(path);
		name = FileUtils.getName(path);
	}
    
	public RootFile(RootFile target, String result) {
    	flds= result.split("\\s+");
        if(flds.length > 3) {
        	permission = flds[0];
        	length = flds[flds.length - 1].length();
        	trimName = flds[flds.length - 1]; 
        	type = permission.startsWith("d") || permission.startsWith("l") ? 0 : 1;
        	name = permission.startsWith("l") ? flds[flds.length - 3] : trimName.endsWith("/") ? trimName.substring(0, length-1) : trimName;
        	path = fixSlashes(target.getAbsolutePath() + File.separator + name);
        }
        else{
        	isValid =  false;
        }
	}

    public RootFile(String target, String result) {
        name = result;
        path = fixSlashes(target + File.separator +  name);
    }
	
	public boolean isValid() {
		return isValid;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isDirectory() {
		return type == 0;
	}
	
	public boolean isFile() {
		return type == 1;
	}

	public int length() {
		return length;
	}

	public boolean canWrite() {
		return true;
	}

	// Removes duplicate adjacent slashes and any trailing slash.
	private static String fixSlashes(String origPath) {
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

	/**
	 * Returns the pathname of the parent of this file. This is the path up to
	 * but not including the last name. {@code null} is returned if there is no
	 * parent.
	 *
	 * @return this file's parent pathname or {@code null}.
	 */
	public String getParent() {
		int length = path.length(), firstInPath = 0;
		if (separatorChar == '\\' && length > 2 && path.charAt(1) == ':') {
			firstInPath = 2;
		}
		int index = path.lastIndexOf(separatorChar);
		if (index == -1 && firstInPath > 0) {
			index = 2;
		}
		if (index == -1 || path.charAt(length - 1) == separatorChar) {
			return null;
		}
		if (path.indexOf(separatorChar) == index
				&& path.charAt(firstInPath) == separatorChar) {
			return path.substring(0, index + 1);
		}
		return path.substring(0, index);
	}

	/**
	 * Returns a new file made from the pathname of the parent of this file.
	 * This is the path up to but not including the last name. {@code null} is
	 * returned when there is no parent.
	 *
	 * @return a new file representing this file's parent or {@code null}.
	 */
	public RootFile getParentFile() {
		String tempParent = getParent();
		if (tempParent == null) {
			return null;
		}
		return new RootFile(tempParent);
	}

	public String getAbsolutePath() {
		return path;
	}

	public String getPath() {
		return path;
	}
}