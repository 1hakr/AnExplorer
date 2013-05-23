package dev.dworks.apps.anexplorer.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import dev.dworks.apps.anexplorer.AnExplorer;
import dev.dworks.apps.anexplorer.ExplorerActivity;
import dev.dworks.apps.anexplorer.MyReceiver;
import dev.dworks.apps.anexplorer.R;

/**
 * @author HaKr
 *
 */
public class ExplorerOperations {
	private static final String TAG = "ExplorerOperations";
	
	public File file;
	public Long length;
	public Long directorySize;
	public boolean isDirectory;
	public boolean isHidden;
	public boolean canRead;
	public boolean isNotException;
	
	private static final int BUFFER = 2048;
	
	//default directories
	public static final String DIR_ROOT = Environment.getRootDirectory().getParent();
	public static final String DIR_SDCARD = Environment.getExternalStorageDirectory().getPath();
	public static final String DIR_SDCARD_VERSION = Environment.getExternalStorageDirectory().getPath()+"/";
	public static final String DIR_EMMC = "/mnt/emmc";
	public static final String DIR_SYSTEM = "/system";
	public static final String DIR_APPS = "/system/app";
	public static final String DIR_MEDIA = DIR_SDCARD_VERSION + "Media";
	public static final String DIR_BLUETOOTH = DIR_SDCARD_VERSION + "Bluetooth";
	public static final String DIR_DOWNLOADS = DIR_SDCARD_VERSION + "Download";
	public static final String DIR_MUSIC = DIR_SDCARD_VERSION + "Music";
	public static final String DIR_GALLERY = DIR_SDCARD_VERSION + "DCIM";
	public static final String DIR_APP_BACKUP = DIR_SDCARD_VERSION + "AppBackup";
	public static final String DIR_APP_PROCESS = DIR_SDCARD_VERSION + "AppProcess";
	public static final String DIR_GALLERY_HIDDEN = DIR_SDCARD_VERSION + "HideFromGallery";
	public static final String DIR_WALLPAPER = DIR_SDCARD_VERSION + "Wallpaper";
	public static final String DIR_MOVIES = DIR_SDCARD_VERSION + "Movies";	
	
	//public static final String DIR_GALLERY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
	//public static final String DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
	//public static final String DIR_MOVIES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath();
	//public static final String DIR_MUSIC = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
	
	//public static final String[] DIRS = {DIR_ROOT, DIR_SDCARD, DIR_SYSTEM, DIR_DOWNLOADS, DIR_BLUETOOTH, DIR_MEDIA, DIR_MUSIC, DIR_GALLERY};
	//public static final String[] DIRS_NAMES = {"Phone", "SD Card", "System", "Downloads", "Bluetooth","RingTones", "Music", "Gallery"};
	public static final String[] DIRS = {DIR_ROOT, DIR_SDCARD, DIR_APP_BACKUP, DIR_APP_PROCESS, DIR_GALLERY_HIDDEN, DIR_WALLPAPER, DIR_DOWNLOADS, DIR_BLUETOOTH, DIR_MUSIC, DIR_GALLERY};
	public static final int[] DIRS_ICONS = {1, 2, 3, 4, 14, 17, 5, 6, 7, 8};
	public static final int[] DIRS_NAMES = {R.string.name_phone, R.string.name_sdcard, R.string.name_apps, R.string.name_processes, 
		R.string.name_unscannable, R.string.name_wallpaper, R.string.name_download, R.string.name_bluetooth, R.string.name_music, R.string.name_gallery};
	
	public static final String[] DIRS_NAV = {DIR_ROOT, DIR_SDCARD, DIR_EMMC, DIR_SYSTEM, DIR_DOWNLOADS, DIR_BLUETOOTH, DIR_MEDIA, DIR_MUSIC, DIR_GALLERY, DIR_MOVIES};
	public static final int[] DIRS_ICONS_NAV = {1, 2, 12, 11, 5, 6, 9, 7, 8, 10};	
	public static final int[] DIRS_NAMES_NAV = {R.string.name_phone, R.string.name_sdcard, R.string.name_emmc, R.string.name_system,
		R.string.name_download, R.string.name_bluetooth, R.string.name_media, R.string.name_music, R.string.name_gallery, R.string.name_movies};
	
	public static final int[] THEMES = {R.style.Theme_Dark, R.style.Theme_Light, R.style.Theme_Pleasant};
	
	//language
    public static final String[] LANGUAGES_LOCALE = {
        "en", "en", "cs", "de", "ja", "ru"
    };	
	
	//constants
	public static final String CONSTANT_PATH = "path";
	public static final String CONSTANT_TO_PATH = "topath";
	public static final String CONSTANT_PATH_LIST = "pathList";
	public static final String CONSTANT_COPY_PATH_LIST = "copyPathList";
	public static final String CONSTANT_MULTI_SELECTION = "multiselection";
	public static final String CONSTANT_APPS_NAME = "apps";
	public static final String CONSTANT_RUN_SU = "runSU";	
	
	public static final String CONSTANT_SEARCH = "Search";
	
	public static final String CATEGORY_OPERATION = "operation";
	public static final String CATEGORY_NAVIGATION = "navigation";
	
	//menu items
    public static final int MENU_CREATE = Menu.FIRST;
    public static final int MENU_EDIT = Menu.FIRST+1;
    public static final int MENU_SHARE = Menu.FIRST+2;
    public static final int MENU_COMPRESS = Menu.FIRST+3;
    public static final int MENU_SORT = Menu.FIRST+4;
    public static final int MENU_SELECT = Menu.FIRST+5;
    public static final int MENU_SEARCH = Menu.FIRST+6;
    public static final int MENU_ABOUT = Menu.FIRST+7;
    public static final int MENU_EXIT = Menu.FIRST+8;
    public static final int MENU_SYS_INFO = Menu.FIRST+9;
    public static final int MENU_SETTING = Menu.FIRST+10;
    public static final int MENU_TOOLS = Menu.FIRST+11;    
    
    //context menu items
    public static final int CONTEXT_MENU_RENAME = Menu.CATEGORY_ALTERNATIVE;
    public static final int CONTEXT_MENU_COPY = Menu.CATEGORY_ALTERNATIVE+1;
    public static final int CONTEXT_MENU_DELETE = Menu.CATEGORY_ALTERNATIVE+2;
    public static final int CONTEXT_MENU_PASTE = Menu.CATEGORY_ALTERNATIVE+3;
    public static final int CONTEXT_MENU_CUT = Menu.CATEGORY_ALTERNATIVE+4;
    public static final int CONTEXT_MENU_EXTRACT = Menu.CATEGORY_ALTERNATIVE+5;
    public static final int CONTEXT_MENU_PROPERTIES = Menu.CATEGORY_ALTERNATIVE+6;
    public static final int CONTEXT_MENU_OPEN_FILE_FOLDER = Menu.CATEGORY_ALTERNATIVE+7;
    public static final int CONTEXT_MENU_SELECTALL = Menu.CATEGORY_ALTERNATIVE+8;
    public static final int CONTEXT_MENU_UNSELECTALL = Menu.CATEGORY_ALTERNATIVE+9;
    public static final int CONTEXT_MENU_APP_DETAILS = Menu.CATEGORY_ALTERNATIVE+10;
    public static final int CONTEXT_MENU_APP_OPEN = Menu.CATEGORY_ALTERNATIVE+11;
    public static final int CONTEXT_MENU_APP_MARKET = Menu.CATEGORY_ALTERNATIVE+12;
    public static final int CONTEXT_MENU_STOP_PROCESS = Menu.CATEGORY_ALTERNATIVE+14;
    public static final int CONTEXT_MENU_HIDE_FOLDER = Menu.CATEGORY_ALTERNATIVE+15;
    public static final int CONTEXT_MENU_UNHIDE_FOLDER = Menu.CATEGORY_ALTERNATIVE+16;
    
    //dialogs
    public static final int DIALOG_ABOUT = 1;
    public static final int DIALOG_CREATE = 2;
    public static final int DIALOG_CLOSE = 3;
    public static final int DIALOG_DELETE = 4;
    public static final int DIALOG_RENAME = 5;
    public static final int DIALOG_CUT = 6;    
    public static final int DIALOG_COPY = 7;
    public static final int DIALOG_PASTE = 8;
    public static final int DIALOG_SHARE = 9;
    public static final int DIALOG_EXTRACT = 10;
    public static final int DIALOG_COMPRESS= 11;
    public static final int DIALOG_UNCOMPRESS= 12;    
    //public static final int DIALOG_SELECT = 13;
    public static final int DIALOG_PROPERTIES = 14;
    public static final int DIALOG_CREATE_FILE = 15;    
    public static final int DIALOG_SAVE = 16;
    public static final int DIALOG_UNINSTALL = 17;
    public static final int DIALOG_ADFREE = 18;
    public static final int DIALOG_STOP_PROCESS = 19;
    public static final int DIALOG_GALLERY_HIDE = 20;
    public static final int DIALOG_GALLERY_UNHIDE = 21;
    public static final int DIALOG_CLEAR_CACHE = 22;
    public static final int DIALOG_SET_WALLPAPER = 23;
    
    // partition types
    public static final int PARTITION_SYSTEM = 1;
    public static final int PARTITION_DATA = 2;
    public static final int PARTITION_CACHE = 3;
    public static final int PARTITION_RAM = 4;
    public static final int PARTITION_EXTERNAL = 5;
    public static final int PARTITION_EMMC = 6;
    public static final int PARTITION_ESTORAGE = 7;
   
	public SparseArray<String> listException = new SparseArray<String>();
	public SparseArray<String> foldersException = new SparseArray<String>();	
	//public ArrayList<Bitmap> cacheThumbnails = new ArrayList<Bitmap>();
	public List<String> cacheSize ;//= new ArrayList<String>();
	public static Context context;
	public static List<String> dirSize = null;
	public File[] listOfFiles = null;
	
	public BackgroundWork newBackgroundWork;
	public String filePath;
	public String fileToPath;
	public String parentPath;
	public String[] filePathList;
	public String[] appNameList;
	public boolean isMultiSelected, runSU;	
	
	SharedPreferences.Editor editor;
	int frequency_type;
	Dialog dialog = null;
	//private static Process console;
  	//private static OutputStream stdin;
  	//private static InputStream stdout;
	  
	public static Process process;
	
	private static String shell;	
	//private static final Pattern UID_PATTERN = Pattern.compile("^uid=(\\d+).*?");
	private static final String EXIT = "exit\n";
	private static final String[] SU_COMMANDS = new String[]{
	   "su",
	   "/system/xbin/su",
	   "/system/bin/su"
	  };
	
	/*private static final String[] TEST_COMMANDS = new String[]{
	    "id",
	    "/system/xbin/id",
	    "/system/bin/id"
	  };*/
	
	enum OUTPUT {
		STDOUT,
	    STDERR,
	    BOTH
	  }
	
	public static enum TYPES {
		Phone,
	    Tablet
	}
	
	public static enum MODES {
		None,
		SearchMode,
		FileMode,
	    AppMode,
	    ProcessMode,
	    RootMode,
	    ExplorerMode,
	    HideFromGalleryMode,
	    WallpaperMode
	}	
	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		public void onFragmentInteraction(Bundle bundle);
	}  
	public ExplorerOperations() {

	}
	
	public ExplorerOperations(boolean canSU) {
		process = getRootProcess();
	}	
	
	/**
	 * This is used to initialize the {@link #ExplorerOperations}
	 * 
	 * @param file Initialises with a file
	 */
	public void setFile(File file){
		this.file = file;
		isDirectory = this.file.isDirectory();
		isHidden = this.file.isHidden();
		canRead = this.file.canRead();
		length = file.length();
		directorySize = 0L;
		
		foldersException.put(1, "/mnt/");
		foldersException.put(2, "/proc/");
		foldersException.put(3, "/sys/");
		foldersException.put(4, Environment.getExternalStorageDirectory().getPath());
	}
	
	private static String format2String(int id){
		return context.getResources().getString(id);
	}
	
	/**
	 * @param path
	 * @return
	 */
	public boolean skipFolderSizeCalc(String path){
		return foldersException.indexOfValue(path) > 0 || path.contains("/sys") ? true : false;
	}
	
	/**
	 * @param context The activity context
	 */
	public void setContext(Context context) {
		ExplorerOperations.context = context;
		cacheSize = new ArrayList<String>();
	}
	
	public static boolean checkDevice(){
		return Build.VERSION.SDK_INT >= 11 ? true : false;
	}
	
	public static boolean isTablet(Context context){
		return ((context.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE);		
	}
	
	public static boolean isPhone(Context context){
		return ((context.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE);
	}
	
	public static boolean isSmall(Context context){
		return ((context.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) <= Configuration.SCREENLAYOUT_SIZE_SMALL);
	}
	
	public static boolean isSpecialMode(MODES mode){
		return (mode == MODES.ProcessMode 
				|| mode == MODES.AppMode 
				|| mode == MODES.HideFromGalleryMode
				|| mode == MODES.WallpaperMode);
	}
	
	public static class FileList{
		String name;
		String path;
		String size;
		Integer icon;
		Integer position;
		Integer selection;
		String infoCount;
		String infoPermission;
		String infoDate;
		String infoSmallDate;
		String packageName;
		
		public FileList(String path, String name, String size, Integer icon, Integer position, Integer selection) {
			this.path = path;
			this.name = name;
			this.size = size;
			this.icon = icon;
			this.position = position;
			this.selection = selection;
		}
		
		public void setInfo(String infoCount, String infoPermission, String infoDate, String infoSmallDate){
			this.infoCount = infoCount;
			this.infoPermission = infoPermission;
			this.infoDate = infoDate;
			this.infoSmallDate = infoSmallDate;
		}
		
		public String getName(){ return name; }
		public void setName(String name){ this.name = name;}
		
		public String getPath(){ return path; }
		public void setPath(String path){ this.path = path;}
		
		public String getSize(){ return size; }
		public void setSize(String size){ this.size = size;}
		
		public Integer getIcon(){ return icon; }
		public void setIcon(Integer icon){ this.icon = icon;}
		
		public Integer getSelection(){ return selection; }
		public void setSelection(Integer selection){ this.selection = selection;}
		
		public String getInfoCount(){ return infoCount; }
		public void setInfoCount(String infoCount){ this.infoCount = infoCount;}
		
		public String getInfoPermission(){ return infoPermission; }
		public void setInfoPermission(String infoPermission){ this.infoPermission = infoPermission;}
		
		public String getInfoDate(){ return infoDate; }
		public void setInfoDate(String infoDate){ this.infoDate = infoDate;}
		
		public String getInfoSmallDate(){ return infoSmallDate; }
		public void setInfoSmallDate(String infoSmallDate){ this.infoSmallDate = infoSmallDate;}
		
		public String getpackageName(){ return packageName; }
		public void setpackageName(String packageName){ this.packageName = packageName;}
		
		public int getPosition() { return position; }
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public static class FileNavList implements Parcelable{
		String name;
		String path;
		Integer icon;
		Integer special_icon = 0;
		Integer position;
		
		public FileNavList(String path, String name, Integer icon, Integer position) {
			this.path = path;
			this.name = name;
			this.icon = icon;
			this.position = position;
		}
		
		public String getName(){ return name; }
		public void setName(String name){ this.name = name;}
		
		public String getPath(){ return path; }
		public void setPath(String path){ this.path = path;}
		
		public Integer getIcon(){ return icon; }
		public void setIcon(Integer icon){ this.icon = icon;}

		public Integer getSpecialIcon(){ return special_icon; }
		public void setSpecialIcon(Integer special_icon){ this.special_icon = special_icon;}

		public int getPosition() { return position; }
		
		@Override
		public String toString() {
			return name;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeString(path);
			out.writeString(name);
			out.writeInt(icon);
			out.writeInt(position);
			out.writeInt(special_icon);
		}

		public static final Parcelable.Creator<FileNavList> CREATOR = new Parcelable.Creator<FileNavList>() {
			public FileNavList createFromParcel(Parcel in) {
				return new FileNavList(in);
			}

			public FileNavList[] newArray(int size) {
				return new FileNavList[size];
			}
		};

		private FileNavList(Parcel in) {
			path = in.readString();
			name = in.readString();
			icon = in.readInt();
			position = in.readInt();
			special_icon = in.readInt();
		}

	}
	
	/**
	 * @return if the file can be showed or not(has (.) or not)
	 */
	public boolean canShow(){
		if((file.getName().charAt(0) == '.'))// || (file.isHidden()))
			return false;
		return true;
	}
	
	public static String getExtStorageString(){
		if(Build.VERSION.SDK_INT >= 9){
			return Environment.isExternalStorageRemovable() ? " (E)" : " (I)";
		}
		return "";
	}
	
	public static boolean isSpecialPath(String filepath){
		return filepath.compareToIgnoreCase(ExplorerOperations.DIR_APP_BACKUP) == 0
				|| filepath.compareToIgnoreCase(ExplorerOperations.DIR_APP_PROCESS) == 0
				|| filepath.compareToIgnoreCase(ExplorerOperations.DIR_GALLERY_HIDDEN) == 0
				|| filepath.compareToIgnoreCase(ExplorerOperations.DIR_WALLPAPER) == 0;
	}
	
	public static boolean isExtStorage(String filePath){
		String path = filePath.toLowerCase(Resources.getSystem().getConfiguration().locale);//new File(filePath).getName();
		return path.contains("storage") || path.contains("sdcard") || path.contains("emmc") || path.contains("sd") || path.contains("usb") || path.contains("hdd"); 
	}
	
	public static boolean isExtStoragePath(String filePath){
		String path = new File(filePath).getName();
		return path.contains("storage") || path.contains("sdcard") || path.contains("emmc") || path.contains("sd") || path.contains("usb") || path.contains("hdd"); 
	}	
	
	public int getTypeForSpecialPaths(String path, String filePath){
		if(filePath.endsWith("Download")){
			return -3;
		}
		else if(filePath.endsWith("Media")){
			return -4;
		}
		else if(filePath.endsWith("Music")){
			return -5;
		}
		else if(filePath.endsWith("DCIM")){
			return -6;
		}
		else if(filePath.endsWith("Bluetooth")){
			return -7;
		}
		else if(filePath.endsWith("Movies")){
			return -8;
		}
		return 0;
	}
	
	public int getTypeForSpecialFolders(String parentPath, String filePath){
	
		if(parentPath.endsWith(DIR_ROOT)){
			if(filePath.compareToIgnoreCase(DIR_SDCARD) == 0  || filePath.endsWith("sdcard")){
				return -1;
			}
			else if(filePath.compareToIgnoreCase(DIR_SYSTEM)== 0){
				return -2;
			}
			if(filePath.endsWith("storage")){
				return 98;
			}			
		}
		else if(parentPath.endsWith("mnt") || parentPath.endsWith("storage")){
			if(filePath.compareToIgnoreCase(DIR_SDCARD)== 0){
				return -1;
			}
			else if(filePath.toLowerCase(Resources.getSystem().getConfiguration().locale).contains("sd")){
				return -1;
			}
			else if(filePath.contains("usb")){
				return 94;
			}
			else if(filePath.contains("hdd")){
				return 93;
			}			
			else if(filePath.compareToIgnoreCase(DIR_EMMC) == 0){
				return 97;
			}
		}
		else if(isExtStorage(parentPath)){ 
			return getTypeForSpecialPaths(parentPath+"/", filePath);
		}	
		return 0;
	}
	
	/**
	 * @return returns 0 for directory and 1-6 for file categories 
	 */
	public int getFileBasicType(){
    	return file.isDirectory() && !file.isFile() ? getTypeForSpecialFolders(file.getParent(), file.getPath()) : getFileType(file);
    }
	
	/**
	 * @return returns  1-6 for file categories
	 */
	public static int getFileType(File localFile){

		String ext = getFileExt(localFile);
		String mimeType = getMIMEType(localFile);
		String type = mimeType.substring(0, mimeType.indexOf("/"));
		int fileType;
		
		if(type.equalsIgnoreCase("audio")){
			if(ext.compareToIgnoreCase("mp3") == 0){
				fileType = 2;
			}
			else{
				fileType = 22;
			}
		}
		else if(type.equalsIgnoreCase("video") || ext.compareToIgnoreCase("flv") == 0){
			fileType = 3;	
		}
		else if(type.equalsIgnoreCase("image")){
			fileType = 4;	
		}
		else if(mimeType.equalsIgnoreCase("application/vnd.android.package-archive")){
			fileType = 5;	
		}
		else if(mimeType.equalsIgnoreCase("application/zip")){
			fileType = 6;	
		}		
		else if(mimeType.equalsIgnoreCase("application/x-shockwave-flash")){
			fileType = 7;
		}
		else if(mimeType.equalsIgnoreCase("application/pdf") || mimeType.equalsIgnoreCase("application/x-pdf")){
			fileType = 8;
		}
		else if(mimeType.equalsIgnoreCase("application/msword")){
			fileType = 9;
		}
		else if(mimeType.equalsIgnoreCase("application/vnd.ms-powerpoint")){
			fileType = 10;
		}
		else if(mimeType.equalsIgnoreCase("application/vnd.ms-excel")){
			fileType = 11;
		}
		else if(mimeType.equalsIgnoreCase("text/html")){
			fileType = 12;
		}		
		else fileType = 1;
		
    	return fileType;
    }
	
	/**
	 * @param file
	 * @return
	 */
	public static String getFilePermissions(File file) {
		String per = "";
		
		per += file.isDirectory() ? "d" : "-";
		per += file.canRead() ? "r" : "-";
		per += file.canWrite() ? "w" : "-";
		
		return per;
	}
	
	public static Process getRootProcess(){
		Runtime localRuntime = Runtime.getRuntime();
		try {
			if(process == null)
				process = localRuntime.exec(checkSu() ? "su" : "sh");	
		} catch (IOException e) {
			e.printStackTrace();
		} 

		return process;
	}	
	
	public synchronized boolean isSuAvailable() {
	    if (shell == null) {
	      checkSu();
	    }
	    return shell != null;
    }

	public static synchronized void setShell(String shell) {
	    ExplorerOperations.shell = shell;
	}

	public static boolean checkSu() {
		for (String command : SU_COMMANDS) {
		  shell = command;
		  if(new File(command).exists())
			  return true;
		  /*if (isRootUid()){
			  return true;  
		  }*/
		}
		shell = null;
		return false;
	}

	public ArrayList<CmdListItem> runCommand(String command){
		return runCommand(command, OUTPUT.STDERR, false);
	}
	
	public String runCommand(String command, boolean runSU){
		Process localProcess;
		DataOutputStream dos = null;
	    String output = "";
	    StringBuffer outputBuffer = new StringBuffer();
		BufferedReader bufferReader;	    
	    try {
	    	localProcess = Runtime.getRuntime().exec(checkSu() ? "su" : "sh");
	    	dos = new DataOutputStream(localProcess.getOutputStream());
	    	dos.writeBytes(command + '\n');
	    	dos.flush();
	    	dos.writeBytes(EXIT);
	    	dos.flush();
	    	localProcess.waitFor();
	    	bufferReader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
			while ((output = bufferReader.readLine()) != null) {
				outputBuffer.append(output);
			}
			dos.close();
			bufferReader.close();
			return outputBuffer.toString();	    	
	    } catch (Exception e) {
	    	Log.i(TAG, "runCommand error: " + e.getMessage());
	    }
		return null;
	}	
	
	public ArrayList<CmdListItem> runCommand(String command, OUTPUT o, boolean runSU){
		Process localProcess;
		DataOutputStream dos = null;
	    String output = "";
	    StringBuffer outputBuffer = new StringBuffer();
		BufferedReader bufferReader;
		ArrayList<CmdListItem> cmdListItem = new ArrayList<CmdListItem>();	    
	    try {
	    	localProcess = Runtime.getRuntime().exec(checkSu() ? "su" : "sh");
	    	dos = new DataOutputStream(localProcess.getOutputStream());
	    	dos.writeBytes(command + '\n');
	    	dos.flush();
	    	dos.writeBytes(EXIT);
	    	dos.flush();
	    	localProcess.waitFor();
	    	bufferReader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
			CmdListItem cmdItem;
			while ((output = bufferReader.readLine()) != null) {
				cmdItem = new CmdListItem(output);
				if(cmdItem.isValid){
					cmdListItem.add(cmdItem);	
				}
				//Log.i(TAG, cmdItem.name+";"+cmdItem.date+";"+cmdItem.permission);
				//Log.i(TAG, output);
				outputBuffer.append(output);
			}
			dos.close();
			bufferReader.close();
			return cmdListItem;	    	
	    } catch (Exception e) {
	    	Log.i(TAG, "runCommand error: " + e.getMessage());
	    }
		return null;
	}
	
    public static class CmdListItem { 
    	private String  name = "", permission = "", trimName = "";
		private int length = 0;
        private boolean isValid = true;
        private int type = 0;
        private String[] flds;
        
        public CmdListItem( String string ) {
        	flds= string.split("\\s+");
            if(flds.length > 3) {
            	permission = flds[0];
            	length = flds[flds.length - 1].length();
            	trimName = flds[flds.length - 1]; 
            	type = permission.startsWith("d") || permission.startsWith("l") ? 0 : 1;
            	name = permission.startsWith("l") ? flds[flds.length - 3] : trimName.endsWith("/") ? trimName.substring(0, length-1) : trimName;
            	//path = name;            	
            }
            else{
            	isValid =  false;
            }
        }
        public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPermission() {
			return permission;
		}
		public void setPermission(String permission) {
			this.permission = permission;
		}
		
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
    }	

	/**
	 * @param file
	 * @return
	 */
	private static String getFileExt(File file){
		String end = "";
		int lastIndex = file.getName().lastIndexOf(".");
		if(lastIndex != -1){
			end = file.getName().substring(lastIndex+1, file.getName().length()).toLowerCase(Resources.getSystem().getConfiguration().locale);	
		}
		return end;		
	}
	
	/**
	 * @param file
	 * @return
	 */
	private static String getFileName(File file){
		int endPosition = file.getName().lastIndexOf(".");
		String end = file.isDirectory() ? file.getName() : endPosition > 0 ? file.getName().substring(0, endPosition) : file.getName();
		return end;		
	}
 
    /**
     * @param file
     * @return
     */
    public static String getMIMEType(File file)
    {
        String end = getFileExt(file);
        String type = "";
        MimeTypeMap map = MimeTypeMap.getSingleton();
        type = map.getMimeTypeFromExtension(end) != null ? map.getMimeTypeFromExtension(end): file.isDirectory() ? "directory" : "application/*";
        return type;
    }
    
    /**
     * @param str
     * @return true if string is null or empty else false
     */
    public static boolean isEmpty(String str){
		return (str == null || str == "") ? true : false;
    }
    
    public static int showView(boolean show){
		return show ? View.VISIBLE : View.GONE;
    }    
    
	/**
	 * @return
	 */
	public String fileLength(){
    	String fileLength = "";
    	if(isDirectory){
    		fileLength = "";
    	}
    	else if(length < 1024){
    		fileLength = Formatter.formatShortFileSize(context, length);
    	}
    	else if(length > 1024){
    		fileLength = Formatter.formatFileSize(context, length);
    	}
    	return fileLength;
    }
	
	public static long getExtStorageSize(String path, boolean isTotal){
		return getPartionSize(path, isTotal);		
	}
	public static long getPartionSize(int type, boolean isTotal){
		Long size = 0L;
		
		switch (type) {
		case PARTITION_SYSTEM:
			size = getPartionSize(Environment.getRootDirectory().getPath(), isTotal);
			break;
		case PARTITION_DATA:
			size = getPartionSize(Environment.getDataDirectory().getPath(), isTotal);
			break;
		case PARTITION_CACHE:
			size = getPartionSize(Environment.getDownloadCacheDirectory().getPath(), isTotal);
			break;
		case PARTITION_EXTERNAL:
			size = getPartionSize(Environment.getExternalStorageDirectory().getPath(), isTotal);
			break;
		/*case PARTITION_EMMC:
			size = getPartionSize(DIR_EMMC, isTotal);
			break;*/
		case PARTITION_RAM:
			size = getSizeTotalRAM(isTotal);
			break;			
		}
		return size;
	}
	
	public static long getSizeTotalRAM(boolean isTotal) { 
		long tm=1000;
		if(isTotal) {
			try { 
				RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
				String load = reader.readLine();
				String[] totrm = load.split(" kB");
				String[] trm = totrm[0].split(" ");
				tm=Long.parseLong(trm[trm.length-1]);
				tm=tm*1024;
			} 
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		else{
			MemoryInfo mi = new MemoryInfo();
			ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			activityManager.getMemoryInfo(mi);
			long availableMegs = mi.availMem;
			tm = availableMegs;
		}		
		return tm;
	}
		/**
	 * @param isTotal  The parameter for calculating total size
	 * @return return Total Size when isTotal is {@value true} else return Free Size of Internal memory(data folder) 
	 */	
	public static Long getPartionSize(String path, boolean isTotal){
		StatFs stat = new StatFs(path);
		long blockSize = stat.getBlockSize();
		long availableBlocks = (isTotal ? (long)stat.getBlockCount() : (long)stat.getAvailableBlocks());
		return availableBlocks * blockSize;
	}
	
	/**
	 * @param dir
	 * @return
	 */
	public static long getDirectorySize(File dir) {
	    long result = 0;
	    if(dir.listFiles() != null && dir.listFiles().length > 0){
		    for(File eachFile : dir.listFiles()) {
		        result += eachFile.isDirectory() && eachFile.canRead() ? getDirectorySize(eachFile) : eachFile.length();  
		    }	
	    }
	    return result;
	}
	
	static private List<File> searchFiles(File dir, FilenameFilter filter){
		List<File> result = new ArrayList<File>();
		File[] filesFiltered = dir.listFiles(filter), filesAll = dir.listFiles();
		
		if(filesFiltered != null){
			result.addAll(Arrays.asList(filesFiltered));
		}
			
		if(filesAll != null){
			for(File file : filesAll) {
				if (file.isDirectory()) {
					List<File> deeperList = searchFiles(file, filter);
					result.addAll(deeperList);
				}
			}
		}
	    return result;
	}
	
	public static boolean openIntent(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list =
	            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if(list.size() > 0) {
            context.startActivity(intent);
            return true;
		}
        else{
			Toast.makeText(context, format2String(R.string.msg_cant_open), Toast.LENGTH_SHORT).show();
			return false;
		}	
	}	
	
	public static boolean isIntentAvailable(Context context, String action) {
	    final Intent intent = new Intent(action);
	    return isIntentAvailable(context, intent);
	}
	
	public static boolean isIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list =
	            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}	
	
	/**
	 * @param searchPath
	 * @param searchQuery
	 * @return
	 */
	public File[] searchDirectory(String searchPath, String searchQuery){
		ArrayList<File> totalList = new ArrayList<File>();
		File searchDirectory = new File(searchPath);

		totalList.addAll(searchFiles(searchDirectory, new SearchFilter(searchQuery)));
		return (File[]) totalList.toArray(new File[0]);
	}
	
	public File[] searchGallery(){
		ArrayList<File> totalList = new ArrayList<File>();
		File searchDirectory = new File(DIR_SDCARD);
		totalList.addAll(searchFiles(searchDirectory, new GalleryFilter()));
		return (File[]) totalList.toArray(new File[0]);
	}
	
	public boolean setWallpaper(String[] selectedFilePathList){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			status  = setWallpaper(path);
			count++;
			newBackgroundWork.onProgressUpdate(count, total);
			break;
		}
		return status;
	}	
	
	/**
	 * 
	 */
	public static boolean setWallpaper(String path){
	      WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
	      //Drawable wallpaperDrawable = wallpaperManager.getDrawable();
	      try {
			wallpaperManager.setBitmap(BitmapFactory.decodeFile(path));
			return true;
		} catch (IOException e) { }
	   /*      Bundle newExtras = new Bundle();
	         newExtras.putString("circleCrop", "true");
	         Intent cropIntent = new Intent();
	         // Uri would be something from MediaStore.Images.Media.EXTERNAL_CONTENT_URI
	         //cropIntent.setData(img.fullSizeImageUri());
	         // edit: it's inside com.android.gallery in case that is even installed.
	         // should work if you replace that with setClassName("com.android.gallery", "com.android.camera.CropImage")
	         cropIntent.setClassName("com.android.gallery", "com.android.camera.CropImage");
	         //cropIntent.setClass(this, CropImage.class);
	         cropIntent.putExtras(newExtras);
	         ((Activity) context).startActivityForResult(cropIntent, 1);*/
		return false;	      
	}
		
	@SuppressWarnings("unused")
	private Bitmap getReflection(Bitmap bitmap) {
	    Bitmap reflection = null;
	    if (reflection == null) {
	        int reflectionH = 80;
	        reflection = Bitmap.createBitmap(bitmap.getWidth(), reflectionH, Bitmap.Config.ARGB_8888);
	 
	        Bitmap blurryBitmap = Bitmap.createBitmap(bitmap, 0,
	                bitmap.getHeight() - reflectionH,
	                bitmap.getWidth(), reflectionH);
	        
	        blurryBitmap = Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(blurryBitmap,blurryBitmap.getWidth() / 2, blurryBitmap.getHeight() / 2, true), blurryBitmap.getWidth(), blurryBitmap.getHeight(), true);
	        BitmapShader bitmapShader = new BitmapShader(blurryBitmap, TileMode.CLAMP, TileMode.CLAMP);
	        Matrix invertMatrix = new Matrix();
	        invertMatrix.setScale(1f, -1f);
	        invertMatrix.preTranslate(0, -reflectionH);
	        bitmapShader.setLocalMatrix(invertMatrix);
	        Shader alphaGradient = new LinearGradient(0, 0, 0, reflectionH, 0x80ffffff, 0x00000000, TileMode.CLAMP);
	        ComposeShader compositor = new ComposeShader(bitmapShader, alphaGradient, PorterDuff.Mode.DST_IN);
	        Paint reflectionPaint = new Paint();
	        reflectionPaint.setShader(compositor);
	        Canvas canvas = new Canvas(reflection);
	        canvas.drawRect(0, 0, reflection.getWidth(), reflection.getHeight(), reflectionPaint);
	    }
	    return reflection;
	}
	
	public static Bitmap getThumbnailBitmap(String filePath) {
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(filePath, options);
	    
	    options.inSampleSize = calculateInSampleSize(options, 0, 0);
	    options.inJustDecodeBounds = false;
	    final Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
	    return bitmap;
	}	
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		byte[] buf = new byte[100*1024];
        int greatest, factor, b, thumb_sz = 64;
	    int inSampleSize = 1;
	    options.inSampleSize = 1;
        options.outWidth = 0;
        options.outHeight = 0;
        options.inTempStorage = buf;
        
        if( options.outWidth > 0 && options.outHeight > 0 ) {
            greatest = Math.max(options.outWidth, options.outHeight);
            factor = greatest / thumb_sz;
            for( b = 0x8000000; b > 0; b >>= 1 )
                if( b < factor ) break;
            inSampleSize = b;
        }
	    return inSampleSize;
	}	
	
	public static Bitmap getThumbnailBitmap2(String filePath){
		byte[] buf = new byte[100*1024];
        int greatest, factor, b, thumb_sz = 64;
		BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        options.outWidth = 0;
        options.outHeight = 0;
        options.inTempStorage = buf;

		try {
			FileInputStream FIS = new FileInputStream(filePath);
	        BitmapFactory.decodeStream(FIS, null, options);
	        
            if( options.outWidth > 0 && options.outHeight > 0 ) {
                greatest = Math.max(options.outWidth, options.outHeight);
                factor = greatest / thumb_sz;
                for( b = 0x8000000; b > 0; b >>= 1 )
                    if( b < factor ) break;
                
                options.inSampleSize = b;
                options.inJustDecodeBounds = false;
        		Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                if( bitmap != null ) {
                	bitmap.recycle();
                    return BitmapFactory.decodeFile(filePath, options);
                }
            }
            FIS.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param path
	 * @param name
	 * @return
	 */
	public boolean createFile(String path, String name, boolean runSU){
		String command = "";	
		if(name.length() != 0){
			if(runSU){
				command = "touch "+path+name;
				Log.i(TAG, command);
				if(null != runCommand(command, runSU)){
					return true;
				}
				return false;
			}
			else{
				try { return new File(path+"/"+name).createNewFile(); } catch (IOException e) { }
				return false;	
			}			
		}
		return false;
	}	
	
	/**
	 * @param path
	 * @param name
	 * @return
	 */
	public boolean createDir(String path, String name, boolean runSU){
		String command = "";		
		if(name.length() != 0){
			if(runSU){
				command = "mkdir "+path+name;
				Log.i(TAG, command);
				if(null != runCommand(command, runSU)){
					return true;
				}
				return false;
			}
			else{
				return (new File(path+"/"+name).mkdir());	
			}
		}
		return false;
	}
		
	/**
	 * @param file
	 */
	public static void openFile(File file, PackageManager packageManager){
		Intent intent;

		if(getFileType(file) == 7 || getFileType(file) == 12){
			intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
			context.startActivity(intent);
		}
		else{
	        intent = new Intent();
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.setAction(Intent.ACTION_VIEW);
	        String type = getMIMEType(file);
	        intent.setDataAndType(Uri.fromFile(file), type);
        	openIntent(context, intent);	
		}
    }
	
	public void zipFolder(File file, ZipOutputStream ZOS) throws IOException, FileNotFoundException{
		byte[] data = new byte[BUFFER];
		int read = 0;
		ZipEntry zipEntry;		
		BufferedInputStream BIS;
		
		if(file.isFile()){
			zipEntry = new ZipEntry(file.getName());
			ZOS.putNextEntry(zipEntry);
			BIS = new BufferedInputStream(new FileInputStream(file), BUFFER);
	        while ((read = BIS.read(data, 0, BUFFER)) != -1) { 
	        	ZOS.write(data, 0, read); 
	        }
	        ZOS.closeEntry();
	        BIS.close();	
		}
		else{
			zipEntry = new ZipEntry(file.getName()+"/");
			ZOS.putNextEntry(zipEntry);
			for(File path : file.listFiles()){
				zipFolder(path, ZOS);
			}
		} 
	}
	
	/**
	 * @param filePath
	 * @param newDirPath
	 * @return
	 * @throws IOException
	 */
	public boolean zipFile(String[] filePathList, String newDirPath, String fileName) throws IOException{
		ZipOutputStream ZOS;
		int count = 0, total = filePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		ZOS = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newDirPath + "/" + fileName + ".zip"), BUFFER));
		
		for(String path : filePathList){
			count++;
			zipFolder(new File(path), ZOS);
			newBackgroundWork.onProgressUpdate(count, total);
		}
		ZOS.close();
		
		return false;
	}
	
	/**
	 * @param filePath
	 * @param newDirPath
	 * @return
	 * @throws IOException
	 */
	public boolean unZipFile(String filePath, String newDirPath, String fileName) throws IOException{
		File file2Extract = new File(filePath); 
		File newDir2Extract;// = new File(newDirPath);
		byte[] data = new byte[BUFFER];
		ZipEntry zipEntry;
		ZipInputStream ZIS;
		int count = 0, total = new ZipFile(file2Extract).size();
		String newFolderName = ExplorerOperations.isEmpty(fileName) ? getFileName(file2Extract) : fileName;
		newBackgroundWork.onProgressUpdate(count, total);
		newDir2Extract = new File(newDirPath + "/" + newFolderName + "/");
		ZIS = new ZipInputStream(new FileInputStream(filePath));
		newDir2Extract.mkdir();

		while((zipEntry = ZIS.getNextEntry()) != null) {
			count++;
			if(zipEntry.isDirectory()) {
				String newDir = newDir2Extract.getPath() + "/" + zipEntry.getName() + "/";
				new File(newDir).mkdir();
				
			} else {
				int read = 0;
	            String newFolderPath = new File(zipEntry.getName()).getParent();
	            new File(newDir2Extract.getPath() + "/" + newFolderPath).mkdirs();

				FileOutputStream FOS = new FileOutputStream(newDir2Extract.getPath() + "/" + zipEntry.getName());
				while((read = ZIS.read(data, 0, BUFFER)) != -1){
					FOS.write(data, 0, read);	
				}		
				ZIS.closeEntry();
				FOS.close();
			}
			//Log.i(TAG, "count: "+count+" total: "+total);
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return false;
	}
	
	/**
	 * @param fromPath
	 * @param toDirPath
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public boolean copyFile(String fromPath, String toDirPath, String newFileName, boolean runSU) throws IOException, FileNotFoundException{
		File from2Copy = new File(fromPath); 
		File toDir2Copy = new File(toDirPath);
		byte[] data = new byte[BUFFER];
		int read = 0;
		String command = "";
		if(runSU){
			if(from2Copy.isFile()){
				command = "cp "+fromPath+" "+toDirPath;
			}
			else{
				command = "cp -r "+fromPath+" "+toDirPath;
			}
			Log.i(TAG, command);
			if(null != runCommand(command, runSU)){
				return true;
			}
			return false;
		}
		else{
			if(toDir2Copy.isDirectory() && toDir2Copy.canWrite()){
				if(from2Copy.isFile()){
					File newFile = new File(toDirPath +"/"+ (!isEmpty(newFileName) ? newFileName :from2Copy.getName()));
					BufferedOutputStream BOS = new BufferedOutputStream(new FileOutputStream(newFile));
					BufferedInputStream BIS = new BufferedInputStream(new FileInputStream(from2Copy));
					while((read = BIS.read(data, 0, BUFFER)) != -1)
						BOS.write(data, 0, read);
					
					//flush and close
					BOS.flush();
					BIS.close();
					BOS.close();
					
				}
				else if(from2Copy.isDirectory()){
					File[] filesInDir = from2Copy.listFiles();
					File newDir = new File(toDirPath + "/"+from2Copy.getName());
					
					// copy folder
					if(!newDir.mkdir()){
						return false;
					}
					
					// copy files in the folder
					for(int i = 0; i < filesInDir.length; i++){
						copyFile(filesInDir[i].getPath(), newDir.getPath(), "", runSU);
					}
				}
			}
			else{
				return false;
			}
			return true;	
		}
	}
	
	public boolean moveFile2Dir(String[] selectedFilePathList, String newDirPath, boolean runSU){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			count++;
			try {
				status = copyFile(path, newDirPath, "", runSU);
				status = status && deleteFile(path, runSU);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return status;
	}
	
	public boolean copyFile2Dir(String[] selectedFilePathList, String newDirPath, boolean runSU){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			count++;
			try {
				status = copyFile(path, newDirPath, "", runSU);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return status;
	}
	
	public boolean saveFile2Dir(String[] selectedFilePathList, String newDirPath){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			try {
				status = copyFile(path, newDirPath, appNameList[count]+".apk", false);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			count++;
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return status;
	}	
	
	public boolean stopProcesses(String[] selectedProcessNameList){
		ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		boolean status = false;
		int count = 0, total = selectedProcessNameList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String processName : selectedProcessNameList){
			count++;
			activityManager.killBackgroundProcesses(processName);
			newBackgroundWork.onProgressUpdate(count, total);
			status = true;
		}
		return status;
	}
	
	public boolean cleaAppsCache(String[] selectedFilePathList){
		boolean status = false;
		for(String path : selectedFilePathList){
			cleanUpCacheDirectory(context.getPackageManager().getPackageArchiveInfo(path, 0).packageName);
		}
		return status;
	}		
	
	public boolean cleanUpCacheDirectory(String packageName) {
		Context otherAppContext = null;
		try {
			otherAppContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) { }
		File testDir = otherAppContext.getCacheDir();
     	if(!testDir.exists()) {
     		return true;
     	}
     
      	String fList[] = testDir.list();
      	if(fList == null) {
      		testDir.delete();
          	return true;
      	}
      	for(int i = 0; i < fList.length; i++) {
      		File file = new File(testDir, fList[i]);
      		if(file.isDirectory()) {
      			cleanUpDirectory(testDir, fList[i]);
      		} else {
      			file.delete();
      		}
      	}
      	return true;
	}
	
	void cleanUpDirectory(File pDir, String dirName) {
		File testDir = new File(pDir,  dirName);
		if(!testDir.exists()) {
			return;
		}
        String fList[] = testDir.list();
        for(int i = 0; i < fList.length; i++) {
        	File file = new File(testDir, fList[i]);
            if(file.isDirectory()) {
            	cleanUpDirectory(testDir, fList[i]);
            } else {
                file.delete();
            }
        }
        testDir.delete();
	}	
	
	public boolean uninstallApps(String[] selectedFilePathList){
		boolean status = false;
		Intent intentUninstall;
		Uri packageUri;
		for(String path : selectedFilePathList){
			try {
				packageUri = Uri.fromParts("package", context.getPackageManager().getPackageArchiveInfo(path, 0).packageName,null);
				if(packageUri != null){
					intentUninstall = new Intent(Intent.ACTION_DELETE, packageUri);
					intentUninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intentUninstall);
					status =  true;	
				}	
			} catch (Exception e) { }
		}
		return status;
	}	
	
	public static boolean shareFile(String[] selectedFilePathList, Context context){
		ArrayList<Uri> uris = new ArrayList<Uri>();
		Intent intentShare = new Intent();
		
		for(int i = 0; i < selectedFilePathList.length; i++) {
			File file = new File(selectedFilePathList[i]);
			uris.add(Uri.fromFile(file));
		}

		intentShare.setAction(Intent.ACTION_SEND_MULTIPLE);
		intentShare.setType("*/*");
		intentShare.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

		context.startActivity(Intent.createChooser(intentShare, format2String(R.string.msg_share)));
		return false;
	}
	
	/**
	 * @param selectedFilePath
	 * @param newName
	 * @return
	 */
	public boolean renameFile(String selectedFilePath, String newName, boolean runSU){
		File oldFile = new File(selectedFilePath);
		File newFile = new File(newName); 
		boolean status = false;
		String onlyName = getFileName(newFile);
		String onlyOldName = getFileName(oldFile);
		String newExt = getFileExt(newFile);
		String oldExt = getFileExt(oldFile);
		String command = "";

		if(oldFile.isDirectory()){
			newFile = new File(oldFile.getParent() +"/"+ onlyName);
		}
		else{
			if(onlyName.compareToIgnoreCase(onlyOldName) != 0 || newExt.compareToIgnoreCase(oldExt) == 0){
				newFile = new File(oldFile.getParent() +"/"+ onlyName + "." +oldExt);
			}
			else if(onlyName.compareToIgnoreCase(onlyOldName) == 0 || newExt.compareToIgnoreCase(oldExt) != 0){
				newFile = new File(oldFile.getParent() +"/"+ onlyOldName + "."+newExt);			
			}			
		}
		
		if(runSU){
			command = "mv "+selectedFilePath+" "+newFile.getPath();
			Log.i(TAG, command);
			if(null != runCommand(command, runSU)){
				return true;
			}
			return false;
		}
		else{			
			if(oldFile.canWrite()){
				status = oldFile.renameTo(newFile);
			}
			//Log.i(TAG, "rename: "+ newFile.getPath().toString());	
			return status;	
		}
	}
	
	/**
	 * @param selectedFilePath
	 * @return
	 */
	public boolean deleteFile(String selectedFilePath, boolean runSU){
		File selectedFile = new File(selectedFilePath);
		String command = "";
		if(runSU){
			command = "rm -r "+selectedFilePath;
			Log.i(TAG, command);
			if(null != runCommand(command, runSU)){
				return true;
			}
			return false;
		}
		else{
			if(selectedFile.exists() && selectedFile.isFile() && selectedFile.canWrite()){
				return selectedFile.delete();
			}
			else if(selectedFile.isDirectory()){
				if(selectedFile.list() != null && selectedFile.list().length == 0){
					return selectedFile.delete();
				}
				else{
					String[] fileList = selectedFile.list();
					for(String innerPaths : fileList){
						File tempFile = new File(selectedFile.getAbsolutePath() + "/" + innerPaths);
						if(tempFile.isFile()){
							tempFile.delete();
						}
						else{
							deleteFile(tempFile.getAbsolutePath(), runSU);
							tempFile.delete();
						}
					}
					
				}
				if(selectedFile.exists()){
					return selectedFile.delete();
				}
			}
			return false;	
		}
	}
	
	/**
	 * @param selectedFilePathList
	 * @return
	 */
	public boolean deleteFile(String selectedFilePathList[], boolean runSU){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			count++;
			status = deleteFile(path, runSU);
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return status;	
	}
	
	public boolean hideFile(String selectedFilePathList[], boolean hide){
		boolean status = false;
		int count = 0, total = selectedFilePathList.length;
		String[] filePaths;
		newBackgroundWork.onProgressUpdate(count, total);
		for(String path : selectedFilePathList){
			filePaths = new String[] { new File(path).toString() };
			count++;
			if(hide){
				try { status = new File(path+"/.nomedia").createNewFile(); } catch (IOException e) { }			    	
			}
			else{
				status = new File(path+"/.nomedia").delete();
			}
			context.sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.fromFile(new File(path))));
			MediaScannerConnection.scanFile(context, filePaths, null,
			          new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) { 
		                    //Log.i("ExternalStorage", "Scanned " + path + ":");
		                    //Log.i("ExternalStorage", "-> uri=" + uri);
						}						
			 });			
			newBackgroundWork.onProgressUpdate(count, total);
		}
		return status;	
	}	
	
	public void zipDialog(final String filePath, final String[] filePathList, final String parentPath, final Context context, final boolean isCompress, final boolean isMultiSelected){
		final int options = isCompress ? R.array.compress_options : R.array.uncompress_options;
		String title = isCompress ? format2String(R.string.msg_compress) : format2String(R.string.msg_uncompress);
		String newFileName = isCompress ? "NewZipFile" : getFileName(new File(filePath));
        final LinearLayout layout = new LinearLayout(context);
        final EditText name = new EditText(context);
        name.setText(newFileName);
        @SuppressWarnings("deprecation")
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        name.setLayoutParams(params);
        layout.addView(name);
        
        
		new AlertDialog.Builder(context)
 	   	.setIcon(R.drawable.ic_menu_compress_new)
 	   	.setTitle(title)
 	   	.setView(layout)
 	   	.setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
			}
		})
        .setPositiveButton(title, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	String newZipFile = name.getText().toString();
            	if(isCompress){            		
                	newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_COMPRESS, context);
                	newBackgroundWork.execute(newZipFile.length() !=0 ? newZipFile : "NewZipFile");
            	}
            	else{
	    			newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_UNCOMPRESS, context);		    			
	    			newBackgroundWork.execute(newZipFile.length() !=0 ? newZipFile : "");
            	}
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 dialog.cancel();
            }
        })
        .show();
	}
	
	/**
	 * @param title
	 * @param path
	 * @param type
	 * @param context
	 */
	public void createOrRenameDialog(final String path ,final Context context, final boolean isCreate, final boolean isFileCreation){
		String title = isCreate ? isFileCreation ? format2String(R.string.msg_create_file) : format2String(R.string.msg_create_folder) : format2String(R.string.msg_rename_folder);		
        final LinearLayout layout = new LinearLayout(context);
        final EditText name = new EditText(context);
        @SuppressWarnings("deprecation")
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        name.setLayoutParams(params);
        if(!isCreate && path !=""){
        	//name.setText(getFileName(new File(path)));
        	name.setText(new File(path).getName());
        }
        layout.addView(name);
        //ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.Sherlock___Theme_Light);
        new AlertDialog.Builder(context)
        	.setIcon(R.drawable.ic_menu_alert)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	if(isCreate){
                		if(isFileCreation){
                        	newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_CREATE_FILE, context);
                        	newBackgroundWork.execute(name.getText().toString(), runSU ? "1" : "0");
                		}
                		else{
                        	newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_CREATE, context);
                        	newBackgroundWork.execute(name.getText().toString(), runSU ? "1" : "0");
                		}
                	}
                	else{
                    	newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_RENAME, context);
                    	newBackgroundWork.execute(name.getText().toString(), runSU ? "1" : "0");       	
                	}                	
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	dialog.cancel();
                }
            })
            .create()
            .show();
	}

	public static long getFrequencySec(int type){
		long millisec = 0L;
		switch (type) {
		case 0:
			millisec = AlarmManager.INTERVAL_DAY / 2;
			break;
		case 1:
			millisec = AlarmManager.INTERVAL_DAY;
			break;
		case 2:
			millisec = AlarmManager.INTERVAL_DAY * 7;
			break;
		case 3:
			millisec = AlarmManager.INTERVAL_DAY * 30;
			break;			
		}
		return millisec;
	}

	/**
	 * @param id
	 * @param context
	 * @param fileInfo
	 */
	public void onCreateSelectedDialog(final int id, final Context context, Bundle fileInfo) {

		filePath = fileInfo.getString(CONSTANT_PATH);
		fileToPath = fileInfo.getString(CONSTANT_TO_PATH);
		parentPath = new File(filePath).getParent();
		filePathList = fileInfo.getStringArray(CONSTANT_PATH_LIST);
		isMultiSelected = fileInfo.getBoolean(ExplorerOperations.CONSTANT_MULTI_SELECTION);
		runSU = fileInfo.getBoolean(ExplorerOperations.CONSTANT_RUN_SU);
		parentPath = runSU ? parentPath+"/" : parentPath;
		
		if(fileInfo.containsKey(CONSTANT_APPS_NAME))
			appNameList = fileInfo.getStringArray(CONSTANT_APPS_NAME);
		
    	switch(id){          
            
    	case ExplorerOperations.DIALOG_CREATE:
    		//ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.Sherlock___Theme_DarkActionBar);
    		dialog = new AlertDialog.Builder(context)
    		.setIcon(R.drawable.ic_menu_folder)
     	   	.setTitle(format2String(R.string.msg_new))  
    		.setItems(R.array.create_options, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
	                if(id == 0){
	                	createOrRenameDialog(filePath, context, true, true); 
	                }
	                else{	                	
	                	createOrRenameDialog(filePath, context, true, false);
	                }
				}})
			.create();
    		dialog.show();
    		break;

    	case ExplorerOperations.DIALOG_RENAME:
    		if(filePathList.length > 1){
    			Toast.makeText(context, format2String(R.string.msg_cant_rename), Toast.LENGTH_SHORT).show();
    			return;
    		}
    		createOrRenameDialog(isMultiSelected ? filePathList[0] : filePath, context, false, false);
    		break;
    		
    	case ExplorerOperations.DIALOG_DELETE:
    		dialog = new AlertDialog.Builder(context)
    		.setMessage(format2String(R.string.msg_want_delete))
     	   	.setIcon(R.drawable.ic_menu_delete)
     	   	.setTitle(format2String(R.string.constant_delete))
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	newBackgroundWork = new BackgroundWork(ExplorerOperations.DIALOG_DELETE, context);
                	newBackgroundWork.execute(runSU ? "1" : "0");
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                }
            })
            .create();
    		dialog.show();    		
    		break;
    		
    	case ExplorerOperations.DIALOG_UNINSTALL:
    		this.uninstallApps(filePathList);
    		break;
    		
    	case ExplorerOperations.DIALOG_SET_WALLPAPER:
    		if(checkDevice()){
    			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    			frequency_type = Integer.valueOf(sharedPreferences.getString("freqType", "1"));
	    		editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
	    		final Set<String> wallpaperSet = new HashSet<String>(Arrays.asList(filePathList));
	    		
	    		dialog = new AlertDialog.Builder(context)
	     	   	.setIcon(R.drawable.image)
	     	   	.setTitle(format2String(R.string.name_wallpaper))
	            .setCancelable(false)
	            .setSingleChoiceItems(R.array.frequency_options, frequency_type, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						frequency_type = which;
					}
				})
	            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
	                	AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	                	Calendar calendar = Calendar.getInstance();
	                	calendar.setTimeInMillis(System.currentTimeMillis());
	                	
						Intent wallpaperIntent = new Intent(context, MyReceiver.class);
						PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, wallpaperIntent, 0);
						editor.putInt("currentWallpaper", 0);
						editor.putString("freqPref", String.valueOf(frequency_type));
						editor.putStringSet("wallpaperSet", wallpaperSet);
						editor.commit();	                	
	                	newBackgroundWork = new BackgroundWork(id, context);
	                	newBackgroundWork.execute(runSU ? "1" : "0");
	                	alarmManager.cancel(pendingIntent);
	                	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
	                			calendar.getTimeInMillis() + getFrequencySec(frequency_type),
	                			getFrequencySec(frequency_type),
	                			pendingIntent);
					}
	            })
	            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
	            })
	            .create();
	    		dialog.show();
    		}
    		break;    		
	
    	case ExplorerOperations.DIALOG_CUT:   		
    	case ExplorerOperations.DIALOG_PASTE:
    	case ExplorerOperations.DIALOG_SAVE:    		
    	case ExplorerOperations.DIALOG_STOP_PROCESS:
    	case ExplorerOperations.DIALOG_GALLERY_HIDE:	
    	case ExplorerOperations.DIALOG_GALLERY_UNHIDE:
    	case ExplorerOperations.DIALOG_CLEAR_CACHE:
        	newBackgroundWork = new BackgroundWork(id, context);
        	newBackgroundWork.execute(runSU ? "1" : "0");    		
    		break;

    	case ExplorerOperations.DIALOG_COMPRESS:
			zipDialog(filePath, filePathList,parentPath, context, true, isMultiSelected);        	
    		break;
    		
    	case ExplorerOperations.DIALOG_UNCOMPRESS:
    		if(isMultiSelected && filePathList.length > 1){
    			Toast.makeText(context, format2String(R.string.msg_cant_extract), Toast.LENGTH_SHORT).show();
    			return;
    		}
    		if(!getMIMEType(new File(isMultiSelected ? filePathList[0] : filePath)).equalsIgnoreCase("application/zip")){
    			Toast.makeText(context, format2String(R.string.msg_cant_extract_other), Toast.LENGTH_SHORT).show();
    			return;
    		}
			zipDialog(filePath, filePathList, parentPath, context, false, isMultiSelected);
    		break;
    		
    	case ExplorerOperations.DIALOG_SHARE:  		
			shareFile(filePathList, context);
    		break;    		
    		
    	case ExplorerOperations.DIALOG_PROPERTIES:
    		if(isMultiSelected && filePathList.length > 1){
    			Toast.makeText(context, format2String(R.string.msg_cant_select), Toast.LENGTH_SHORT).show();
    			return;
    		}
            LayoutInflater factorys = LayoutInflater.from(context);
            final View properiesView = factorys.inflate(R.layout.properties, null);
            File newFile = new File(isMultiSelected ? filePathList[0] : filePath);
            TextView commonView;
            commonView = (TextView)properiesView.findViewById(R.id.name);
            commonView.setText(newFile.getName());
            commonView = (TextView)properiesView.findViewById(R.id.path);
            commonView.setText(newFile.getPath());
            commonView = (TextView)properiesView.findViewById(R.id.type);
            commonView.setText(getMIMEType(newFile));
            commonView = (TextView)properiesView.findViewById(R.id.totalFiles);
            commonView.setText(newFile.exists() && newFile.isDirectory() ? null != newFile.list() ? String.valueOf(newFile.list().length) : "-" : "-");
            commonView = (TextView)properiesView.findViewById(R.id.access);
            commonView.setText(getFilePermissions(newFile));
            commonView = (TextView)properiesView.findViewById(R.id.size);
            commonView.setText(Formatter.formatShortFileSize(context,newFile.isDirectory() ? getDirectorySize(newFile) : newFile.length()));            
            
            dialog = new AlertDialog.Builder(context)
     	   	.setIcon(R.drawable.ic_menu_info)
     	   	.setTitle(format2String(R.string.msg_properties))            
            .setView(properiesView)
            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	dialog.cancel();
                }
            })
            .create();
            dialog.show();            
            break;    		
    		
    	case ExplorerOperations.DIALOG_ABOUT:
    		AboutFragment AboutFragment = new AboutFragment();
    		AboutFragment.show(((SherlockFragmentActivity)context).getSupportFragmentManager(), "about");
            break;
            
    	case ExplorerOperations.DIALOG_ADFREE:
    		editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            CheckBox checkBox =  new CheckBox(context);
            checkBox.setText(format2String(R.string.msg_dont_show));
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean checked) {
					editor.putInt("adfreePref", checked ? 0 : 1);
					editor.commit();
					
				}});

    		dialog = new AlertDialog.Builder(context)
    		.setMessage(format2String(R.string.msg_want_adfree))
     	   	.setIcon(R.drawable.icon)
     	   	.setTitle(format2String(R.string.msg_adfree))
     	   	.setView(checkBox)
            .setPositiveButton(R.string.constant_support, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	Intent intentMarket = new Intent(Intent.ACTION_VIEW);
                	intentMarket.setData(Uri.parse("market://details?id=dev.dworks.apps.anexplorer.pro"));
                	((Activity) context).startActivity(intentMarket);
                }
            })
            .setNegativeButton(R.string.constant_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                }
            })
            .create();
    		dialog.show();    		
    		break;            
    	}
    }
	
	public static class AboutFragment extends SherlockDialogFragment implements View.OnLongClickListener{
		
		private View view;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			final TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.AppTheme);
            int theme = a.getResourceId(R.styleable.AppTheme_aboutTheme, 0);
            if(!isTablet(context)){
    			setStyle(STYLE_NO_TITLE, theme);
            }
            else{
            	setStyle(STYLE_NO_TITLE, getTheme());
            }
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			view = inflater.inflate(R.layout.about, container, false);
			initControls();
			return view;
		}
		
		private void initControls() {
			LinearLayout layout_ad = (LinearLayout) view.findViewById(R.id.layout_ad);
		    AdView adView = new AdView((Activity) context, AdSize.IAB_MRECT, "a14e25123e38970");
	        adView.loadAd(new AdRequest());	    
	        layout_ad.addView(adView);
	        
    		View actionView = view.findViewById(R.id.github_button);
            actionView.setOnLongClickListener(this);
            actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "github_button", 0L);
       				Uri uriUrl = Uri.parse("https://github.com/DWorkS/AnExplorer");
	    				Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl); 
	    				context.startActivity(launchBrowser);
	    				dismiss();
				}});
            actionView = view.findViewById(R.id.gplus_button);
            actionView.setOnLongClickListener(this);
    		actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "gplus_button", 0L);
       				Uri uriUrl = Uri.parse("https://plus.google.com/109240246596102887385");
	    				Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl); 
	    				context.startActivity(launchBrowser);
	    				dismiss();
				}});
            actionView = view.findViewById(R.id.twitter_button);
            actionView.setOnLongClickListener(this);
    		actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "twitter_button", 0L);
       				Uri uriUrl = Uri.parse("https://twitter.com/1HaKr");
	    				Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl); 
	    				context.startActivity(launchBrowser);
	    				dismiss();
				}});
            
            actionView = view.findViewById(R.id.feedback_button);
            actionView.setOnLongClickListener(this);
    		actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "feedback_button", 0L);
                	Intent intent = new Intent(Intent.ACTION_SEND);
                	intent.setType("text/email");
                	intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"hakr@dworks.in"});
                	intent.putExtra(Intent.EXTRA_SUBJECT, "AnExplorer Feedback");
                	((Activity) context).startActivity(Intent.createChooser(intent, "Send Feedback"));
                	dismiss();
				}});
            
            actionView = view.findViewById(R.id.rate_button);
            actionView.setOnLongClickListener(this);
    		actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "rate_button", 0L);
                	Intent intentMarket = new Intent(Intent.ACTION_VIEW);
                	intentMarket.setData(Uri.parse("market://details?id=dev.dworks.apps.anexplorer"));
                	((Activity) context).startActivity(intentMarket);
                	dismiss();
				}});
            
            actionView = view.findViewById(R.id.site_button);
            actionView.setOnLongClickListener(this);
    		actionView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "about", "site_button", 0L);
                	Intent intent = new Intent(Intent.ACTION_VIEW);
                	intent.setData(Uri.parse("market://details?id=dev.dworks.apps.anexplorer.pro"));
                	((Activity) context).startActivity(intent);
                	dismiss();
				}});
		}

		@Override
		public boolean onLongClick(View v) {
	        final int[] screenPos = new int[2];
	        final Rect displayFrame = new Rect();
	        v.getLocationOnScreen(screenPos);
	        v.getWindowVisibleDisplayFrame(displayFrame);
	        final int width = v.getWidth();
	        final int height = v.getHeight();
	        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
	        final int correctHeight = isTablet(context) ? height * 2 : height;
	        final int correctWidth = screenPos[0] + width > screenWidth / 2 ? screenPos[0] - width*3 : screenPos[0] - width;
	        Toast cheatSheet = Toast.makeText(context, v.getTag().toString(), Toast.LENGTH_SHORT);
	        cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, correctWidth, correctHeight);
	        cheatSheet.show();
			return true;
		}
	}
	
	/**
	 * @author HaKr
	 *
	 */
	public static class SearchFilter implements FilenameFilter{
		String searchQuery;
		boolean onlyFolders;
		/**
		 * @param search
		 */
		public SearchFilter(String search){
			this.searchQuery = search;
		}
		
		/**
		 * @param search
		 * @param onlyFolders
		 */
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
	
	public static class GalleryFilter implements FilenameFilter{

		@Override
		public boolean accept(File dir, String filename) {
			if(dir.isDirectory()){
				return filename.toLowerCase(Resources.getSystem().getConfiguration().locale).contains("nomedia");	
			}
			return false;
		}	
	}
	
	/*
	* accepts files of a given type
	*/
	public enum TypeFilter implements FileFilter {
        FILE, DIR, ALL;

        @Override
        public boolean accept(  File file ) {
        	return file != null && ( this == ALL || this == FILE && file.isFile() || this == DIR && file.isDirectory() );
        }
	}
	
	/*
	* accepts files that match a given pattern and type
	*/
	public class RegExFilter implements FileFilter {
        private final String pattern;

        public RegExFilter(String pattern ) {
	        super();
	        this.pattern = pattern;
        }

        @Override
        public boolean accept( final File file ) {
            return file.getName().matches( this.pattern );
        }
	}

	/*
	* accepts files that match a given suffix and type
	*/
	public class SuffixFilter extends RegExFilter {
		public SuffixFilter(final String suffix ) {
			super("^.*" + Pattern.quote( suffix ) + "$" );
        }
	}	
	
    //Sorting//
    /**
     * 
     */
    public static final Comparator<File> typeAlpha = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {	
			return file1.isDirectory() ? (file2.isDirectory() ? file1.getName().compareToIgnoreCase(file2.getName()) : -1) : (file2.isFile() ? file1.getName().compareToIgnoreCase(file2.getName()) : 1); 
		}
	};
	
    public static final Comparator<File> alphaAscending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {	
			return file1.getName().compareToIgnoreCase(file2.getName()); 
		}
	};

    public static final Comparator<File> alphaDescending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {	
			return file1.getName().compareToIgnoreCase(file2.getName()); 
		}
	};
	
    /**
     * 
     */
    public static final Comparator<File> typeAscending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {
			return file1.isDirectory() ? (file2.isDirectory() ? file1.getName().compareToIgnoreCase(file2.getName()) : -1) : (file2.isFile() ? file1.getName().compareToIgnoreCase(file2.getName()) : 1); 
		}
	};

    public static final Comparator<File> typeDescending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {
			return file1.isFile() ? (file2.isFile() ? file1.getName().compareToIgnoreCase(file2.getName()) : -1) : (file2.isDirectory() ? file1.getName().compareToIgnoreCase(file2.getName()) : 1); 
		}
	};
	
    public static final Comparator<CmdListItem> typeAscendingSU = new Comparator<CmdListItem>() {
		@Override
		public int compare(CmdListItem item1, CmdListItem item2) {
			return item1.type == 0 ? (item2.type == 0 ? item1.name.compareToIgnoreCase(item2.name) : -1) : (item2.type == 1 ? item1.name.compareToIgnoreCase(item2.name) : 1); 
		}
	};	
    /**
     * 
     */
    public static final Comparator<File> sizesAscending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {
			Long size1 = file1.length();
			Long size2 = file2.length();
			return size1.compareTo(size2); 
		}
	};

    public static final Comparator<File> sizesDescending = new Comparator<File>() {
		@Override
		public int compare(File file2, File file1) {
			Long size1 = file1.length();
			Long size2 = file2.length();
			return size1.compareTo(size2); 
		}
	};
	
    /**
     * 
     */
    public static final Comparator<File> datesAscending = new Comparator<File>() {
		@Override
		public int compare(File file2, File file1) {
			Long date1 = file1.lastModified();
			Long date2 = file2.lastModified();
			return date1.compareTo(date2); 
		}
	};
	
    public static final Comparator<File> datesDescending = new Comparator<File>() {
		@Override
		public int compare(File file1, File file2) {
			Long date1 = file1.lastModified();
			Long date2 = file2.lastModified();
			return date1.compareTo(date2); 
		}
	};
	/**
	 * @author HaKr
	 *This AsyncTask will be useful when there are large number of files involved 
	 *in file operation . we show a progress dialog. 
	 */
	private class BackgroundWork extends AsyncTask<String, Integer, boolean[]> {
		
		int operationType;
		ProgressDialog progressBar;
		private Context context; 
		String progressMessage;
		private Bundle bundle = new Bundle();
		
		public BackgroundWork(int operationType, Context context) {
			this.operationType = operationType;
			this.context = context;
			bundle.putInt("operation", 4);
		}
		
		@Override
		protected void onPreExecute() {
			switch (operationType) {
			case ExplorerOperations.DIALOG_CREATE:
				progressMessage = format2String(R.string.msg_creating_dir);
				break;
			case ExplorerOperations.DIALOG_CREATE_FILE:
				progressMessage = format2String(R.string.msg_creating_file);
				break;				
			case ExplorerOperations.DIALOG_RENAME:
				progressMessage = format2String(R.string.msg_renaming_file);				
				break;				
			case ExplorerOperations.DIALOG_DELETE:
				progressMessage = format2String(R.string.msg_deleting_file);				
				break;				
			case ExplorerOperations.DIALOG_CUT:
				progressMessage = format2String(R.string.msg_moving_file);				
				break;				
			case ExplorerOperations.DIALOG_PASTE:
				progressMessage = format2String(R.string.msg_pasting_file);
				break;
			case ExplorerOperations.DIALOG_COMPRESS:
				progressMessage = format2String(R.string.msg_compressing_file);				
				break;				
			case ExplorerOperations.DIALOG_UNCOMPRESS:
				progressMessage = format2String(R.string.msg_uncompressing_file);				
				break;
			case ExplorerOperations.DIALOG_SAVE:
				progressMessage = format2String(R.string.msg_saving_file);				
				break;
			case ExplorerOperations.DIALOG_STOP_PROCESS:
				progressMessage = format2String(R.string.msg_stopping_apps);				
				break;
			case ExplorerOperations.DIALOG_GALLERY_HIDE:
				progressMessage = format2String(R.string.msg_gallery_hide);				
				break;
			case ExplorerOperations.DIALOG_GALLERY_UNHIDE:
				progressMessage = format2String(R.string.msg_gallery_unhide);				
				break;	
			case ExplorerOperations.DIALOG_CLEAR_CACHE:
				progressMessage = format2String(R.string.msg_clearing_cache);				
				break;
				
			case ExplorerOperations.DIALOG_SET_WALLPAPER:
				progressMessage = format2String(R.string.msg_setting_wallpaper);				
				break;				
			}
			progressBar = new ProgressDialog(this.context);
			progressBar.setMax(0);
			progressBar.setProgressDrawable(this.context.getResources().getDrawable(R.drawable.progress_dialog));			
			progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressBar.setMessage(progressMessage);
			progressBar.setCancelable(false);
			progressBar.show();
			
			super.onPreExecute();
		}
		
		@Override
		protected boolean[] doInBackground(String... params) { 
			boolean[] result = {false};
			boolean runSU = false;
			switch (operationType) {
			case ExplorerOperations.DIALOG_CREATE:
				runSU = params[1].compareTo("1") == 0;
				result[0] = createDir(filePath, params[0], runSU);				
				break;
			case ExplorerOperations.DIALOG_CREATE_FILE:
				runSU = params[1].compareTo("1") == 0;
				result[0] = createFile(filePath, params[0], runSU);				
				break;				
			case ExplorerOperations.DIALOG_RENAME:
				runSU = params[1].compareTo("1") == 0;
				result[0] = renameFile(filePath, params[0], runSU);				
				break;				
			case ExplorerOperations.DIALOG_DELETE:
				runSU = params[0].compareTo("1") == 0;
				result[0] = isMultiSelected ? deleteFile(filePathList, runSU): deleteFile(filePath, runSU);
				break;				
			case ExplorerOperations.DIALOG_CUT:
				runSU = params[0].compareTo("1") == 0;
				result[0] = moveFile2Dir(filePathList, fileToPath, runSU);
				break;				
			case ExplorerOperations.DIALOG_PASTE:
				runSU = params[0].compareTo("1") == 0;
				result[0] = copyFile2Dir(filePathList, fileToPath, runSU);				
				break;
			case ExplorerOperations.DIALOG_SAVE:
				result[0] = saveFile2Dir(filePathList, fileToPath);				
				break;
			case ExplorerOperations.DIALOG_STOP_PROCESS:
				result[0] = stopProcesses(filePathList);				
				break;
			case ExplorerOperations.DIALOG_GALLERY_HIDE:
				result[0] = hideFile(filePathList, true);				
				break;
			case ExplorerOperations.DIALOG_GALLERY_UNHIDE:
				result[0] = hideFile(filePathList, false);				
				break;				
			case ExplorerOperations.DIALOG_CLEAR_CACHE:
				result[0] = cleaAppsCache(filePathList);				
				break;				
			case ExplorerOperations.DIALOG_COMPRESS:
        		try { 			
        			result[0] = zipFile(filePathList, filePath, params[0]);
				} catch (IOException e){
					e.printStackTrace();
				}
				break;				
			case ExplorerOperations.DIALOG_UNCOMPRESS:
	    		try {
	    			result[0] = unZipFile(filePath, parentPath, params[0]);	
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
				
			case ExplorerOperations.DIALOG_SET_WALLPAPER:
				result[0] = setWallpaper(filePathList);		
				
				break;					
			}
			return result;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			progressBar.setMax(values[1]);
			progressBar.setProgress(values[0]);			
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(boolean[] result) {
			switch (operationType) {
			case ExplorerOperations.DIALOG_CREATE:
				Toast.makeText(context,result[0] ? format2String(R.string.msg_dir_create_success) : format2String(R.string.msg_dir_create_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", filePath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;
			case ExplorerOperations.DIALOG_CREATE_FILE:
				Toast.makeText(context,result[0] ? format2String(R.string.msg_file_create_success) : format2String(R.string.msg_file_create_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", filePath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;				
			case ExplorerOperations.DIALOG_RENAME:
            	Toast.makeText(context,result[0] ?  format2String(R.string.msg_file_rename_success) : format2String(R.string.msg_file_rename_unsuccess), Toast.LENGTH_SHORT).show();	
				bundle.putString("path", parentPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );            	
				break;				
			case ExplorerOperations.DIALOG_DELETE:
            	Toast.makeText(context, result[0] ? format2String(R.string.msg_file_delete_success) : format2String(R.string.msg_file_delete_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", isMultiSelected ? filePath : parentPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );            	
				break;
			case ExplorerOperations.DIALOG_CUT:
				bundle.putString("path", fileToPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;				
			case ExplorerOperations.DIALOG_PASTE:
				bundle.putString("path", fileToPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle);				
				break;
			case ExplorerOperations.DIALOG_SAVE:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_app_save_success) : format2String(R.string.msg_app_save_unsuccess), Toast.LENGTH_SHORT).show();
				break;
			case ExplorerOperations.DIALOG_STOP_PROCESS:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_app_stop_success) : format2String(R.string.msg_app_stop_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", fileToPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;
			case ExplorerOperations.DIALOG_GALLERY_HIDE:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_folder_hidden_success) : format2String(R.string.msg_folder_hidden_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", fileToPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;
			case ExplorerOperations.DIALOG_GALLERY_UNHIDE:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_folder_unhidden_success) : format2String(R.string.msg_folder_unhidden_unsuccess), Toast.LENGTH_SHORT).show();
				bundle.putString("path", fileToPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;				
			case ExplorerOperations.DIALOG_CLEAR_CACHE:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_clear_cache_success) : format2String(R.string.msg_clear_cache_unsuccess), Toast.LENGTH_SHORT).show();
				break;								
			case ExplorerOperations.DIALOG_COMPRESS:
				if(result[0]){
					Toast.makeText(context, format2String(R.string.msg_cant_compress_zip), Toast.LENGTH_SHORT).show();	
				}
				bundle.putString("path", filePath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );				
				break;				
			case ExplorerOperations.DIALOG_UNCOMPRESS:
				if(result[0]){
					Toast.makeText(context, format2String(R.string.msg_cant_extract_zip), Toast.LENGTH_SHORT).show();	
				}
				bundle.putString("path", parentPath);
				((ExplorerActivity) context).onFragmentInteraction(bundle );
				break;
			case ExplorerOperations.DIALOG_SET_WALLPAPER:
				Toast.makeText(context, result[0] ? format2String(R.string.msg_set_wallpaper_success) : format2String(R.string.msg_set_wallpaper_unsuccess), Toast.LENGTH_SHORT).show();
				break;						
			}			
			progressBar.dismiss();
			super.onPostExecute(result);
		}	
	}
}