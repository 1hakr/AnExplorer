package dev.dworks.apps.anexplorer.root;

import java.io.File;

public class RootFile {

	private String  name = "", permission = "", trimName = "";
	private int length = 0;
    private boolean isValid = true;
    private int type = 0;
    private String[] flds;
    private String path;

    public RootFile(String path) {
    	this.path = path;
	}
    
	public RootFile(RootFile target, String result) {
    	flds= result.split("\\s+");
        if(flds.length > 3) {
        	permission = flds[0];
        	length = flds[flds.length - 1].length();
        	trimName = flds[flds.length - 1]; 
        	type = permission.startsWith("d") || permission.startsWith("l") ? 0 : 1;
        	name = permission.startsWith("l") ? flds[flds.length - 3] : trimName.endsWith("/") ? trimName.substring(0, length-1) : trimName;
        	path = target.getAbsolutePath() + File.separator + name;            	
        }
        else{
        	isValid =  false;
        }
	}

    public RootFile(String target, String result) {
        name = result;
        path = target+ result;
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

	public RootFile getParent() {
		return new RootFile(path);
	}

	public String getAbsolutePath() {
		return path;
	}

	public int length() {
		return length;
	}

	public boolean canWrite() {
		return true;
	}

	public String getPath() {
		return path;
	}
}