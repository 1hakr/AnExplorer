package dev.dworks.apps.anexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import dev.dworks.apps.anexplorer.ExplorerOperations.FileNavList;
import dev.dworks.apps.anexplorer.ExplorerOperations.OnFragmentInteractionListener;

/**
 * @author HaKr
 *
 */
public class HomeFragment extends SherlockListFragment {
	
	//private static final String TAG = "Explorer";
	public SparseIntArray iconCache = new SparseIntArray();
	RelativeLayout titlePane;
	
	boolean isRoot=true;
	private Context context;
	private ExplorerOperations fileExplorer;
	private Dialog splashScreenDialog;
	private boolean showNavigationPane;
	
	//preferences
	private GridView gridView;
	private boolean isTablet, isPhone;
	SharedPreferences.Editor editor;
	SharedPreferences adfreePref, exitPref;
	private AdView adView, adViewHeader;
	private ArrayList<FileNavList> fileListEntries;
	//private static final String APP_ERROR = "android.intent.action.APP_ERROR";
	//private static final String EXTRA_BUG_REPORT = "android.intent.extra.BUG_REPORT";
	private ArrayList<FileNavList> fileListNavEntries;
	
	private OnFragmentInteractionListener mListener;
	private View view;
	private Bundle myBundle;

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment HomeFragment.
	 */
	public static HomeFragment newInstance(String param1, String param2) {
		HomeFragment fragment = new HomeFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public HomeFragment() {
	}
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_home, container, false);
        context = this.getSherlockActivity();

		isPhone = ExplorerOperations.isPhone(context);
		isTablet = !isPhone && ExplorerOperations.isTablet(context);

        fillBitmapCache();        
        initControls();        
		return view;
	}
	
	@Override
	public void onResume() {
		onConfigurationChanged(getResources().getConfiguration());
		fillData();
		super.onResume();
	}

	public void onButtonPressed(Bundle bundle) {
		if (mListener != null) {
			mListener.onFragmentInteraction(bundle);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
/*    
    
    private boolean isInternetConnected() {
    	ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    	if(null != networkInfo && networkInfo.isConnected()){
    		return true;
    	}
		return false;	
	}*/
	
	/**
	 * Initialises the controls in the activity content view
	 */
	public void initControls(){		
		//ads
        adView = (AdView) view.findViewById(R.id.adView);
        adViewHeader = new AdView((Activity) context, AdSize.SMART_BANNER, "a14e25123e38970");

    	gridView = (GridView) view.findViewById(R.id.grid_explorer);
    	titlePane = (RelativeLayout) view.findViewById(R.id.title_pane);
    	gridView.setVisibility(View.GONE);
    	titlePane.setVisibility(View.GONE);
	}
      
	/**
	 * Initialises the list for Explorer Home 
	 */
	private void fillData() {
		fileListEntries = new ArrayList<FileNavList>();
		fileListNavEntries = new ArrayList<FileNavList>();
		FileNavList fileList;
		fileExplorer = new ExplorerOperations();
		fileExplorer.setFile(new File(ExplorerOperations.DIR_ROOT));
		fileExplorer.setContext(context);

    	File newFile = new File("/mnt");
		String name = "";    				
		int icon, special_icon = 0;
    	for(int i = 0; i <ExplorerOperations.DIRS.length; i++ ){
    		if(i == 2 && newFile.exists()){
    			for(File file : (newFile).listFiles()){
    				String actualFilePath = file.getAbsolutePath();
    				String filePath = actualFilePath.toLowerCase(Resources.getSystem().getConfiguration().locale);
    				String fileName = file.getName();    							
    				if(file.canRead() && !filePath.contains("asec") && !filePath.contains("obb") && !filePath.contains("secure")){
    					if(fileName.compareToIgnoreCase("sdcard") == 0) {
    						continue;
    					}
    					else if(filePath.contains("sd")){
    						name = "SD Card (E)";
        					icon = 2;
        					fileList = new FileNavList(actualFilePath, name, icon, i);
        					fileListEntries.add(fileList);        					
        				}
        				else if(filePath.contains("usb")){
        					name = "USB Storage";
        					icon = 15;
        					fileList = new FileNavList(actualFilePath, name, icon, i);
        					fileListEntries.add(fileList);        					
        				}
        				else if(filePath.contains("hdd") || filePath.contains("ext")){
        					name = "HDD Storage";
        					icon = 16;
        					fileList = new FileNavList(actualFilePath, name, icon, i);
        					fileListEntries.add(fileList);        					
        				}        				
        				else if(filePath.contains("mmc")){
        					name = format2String(R.string.name_emmc);
        					icon = 12;
        					fileList = new FileNavList(actualFilePath, name, icon, i);
        					fileListEntries.add(fileList);
        				}         				
    				}
    			}
    		}
			fileList = new FileNavList(ExplorerOperations.DIRS[i], format2String(ExplorerOperations.DIRS_NAMES[i]), ExplorerOperations.DIRS_ICONS[i], i);
			fileListEntries.add(fileList);			
    	}
    	
    	for(int i = 0; i <ExplorerOperations.DIRS_NAV.length; i++ ){
			name = format2String(ExplorerOperations.DIRS_NAMES_NAV[i]);
			icon = iconCache.get(ExplorerOperations.DIRS_ICONS_NAV[i]);
    		if(i == 0 && isTablet){
				name = format2String(R.string.name_tablet);
    		}
    		if(i > 2){
    			icon = iconCache.get(-1);
    			special_icon = iconCache.get(ExplorerOperations.DIRS_ICONS_NAV[i]);
    		}    		
			fileList = new FileNavList(ExplorerOperations.DIRS[i], name, icon, i);
			fileList.setSpecialIcon(special_icon);
			fileListNavEntries.add(fileList);
    	}    	
    	
    	setListAdapter(new ListAdapter(context, R.layout.home_item, fileListEntries));
    	
    	if((isTablet && showNavigationPane) || (isPhone && showNavigationPane)){
   		
    	}
		myBundle = new Bundle();
		myBundle.putInt("operation", 3);
		myBundle.putInt("action", 1);
		myBundle.putInt("position", -1);
		myBundle.putString("base", "home");
		myBundle.putParcelableArrayList("navlist", fileListNavEntries);
		mListener.onFragmentInteraction(myBundle);     	
    	showAds();
	}
	
	private void showAds(){
        adView.loadAd(new AdRequest());
        adViewHeader.loadAd(new AdRequest());
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.options_home, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent settingIntent;
	    switch(item.getItemId()) {
	    	case ExplorerOperations.MENU_SYS_INFO:
	            break;
	    	case R.id.menu_setting: 
				settingIntent = new Intent(context, Setting.class);
				startActivityForResult(settingIntent, 0);
	            break;
	        case R.id.menu_help:
	        	showHelpScreen();
	            break;	            
	        case R.id.menu_about:
	        	showSelectedDialog(ExplorerOperations.DIALOG_ABOUT);
				/*settingIntent = new Intent(APP_ERROR);
				settingIntent.putExtra(EXTRA_BUG_REPORT, "");
				if(ExplorerOperations.isIntentAvailable(this, APP_ERROR)){
					startActivity(settingIntent);		
				} */       	
	            break;             
	    }
	    
	    return super.onOptionsItemSelected(item);
	}
	
	private boolean validateFolder(String path){
		File mainFile = new File(path);
		if(!ExplorerOperations.isSpecialPath(path) && !(mainFile.exists() && mainFile.canRead())){
			Toast.makeText(context, path == ExplorerOperations.DIR_SDCARD ? format2String(R.string.msg_sdcard_unmounted) : format2String(R.string.msg_folder_not_present), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if(validateFolder(fileListEntries.get(position).getPath())){
			myBundle = new Bundle();
			myBundle.putInt("operation", 1);
			myBundle.putString(ExplorerOperations.CONSTANT_PATH, fileListEntries.get(position).getPath());
			myBundle.putBoolean(ExplorerOperations.CONSTANT_SEARCH, false);
			mListener.onFragmentInteraction(myBundle);
		}
	}
	    
	class NavingationAdapter extends ArrayAdapter<FileNavList>{

		LayoutInflater inflater;

		public NavingationAdapter(Context context, int textViewResourceId, List<FileNavList> objects) {
			super(context, textViewResourceId, objects);
			inflater = ((Activity) context).getLayoutInflater();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.row_navigation, null);
			TextView name = (TextView) convertView.findViewById(R.id.name);
			ImageView fileIcon = (ImageView) convertView.findViewById(R.id.icon);
			ImageView fileIconSpecial = (ImageView) convertView.findViewById(R.id.icon_special);

			name.setText(position == 0 && isTablet ? format2String(R.string.name_tablet) : fileListNavEntries.get(position).getName());
			fileIcon.setImageResource(position > 2 ? iconCache.get(-1) : iconCache.get(ExplorerOperations.DIRS_ICONS_NAV[position]));
			fileIconSpecial.setImageResource(position > 2 ? iconCache.get(ExplorerOperations.DIRS_ICONS_NAV[position]) : 0);			
			return (convertView);
		}
	}
	    
	/**
	 * @author HaKr
	 *
	 */
	class ListAdapter extends ArrayAdapter<FileNavList>{

		Activity context;
		LayoutInflater inflater;

		public ListAdapter(Context context, int textViewResourceId, List<FileNavList> objects) {
			super(context, textViewResourceId, objects);
			this.context = (Activity) context;
			inflater = this.context.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			convertView = inflater.inflate(R.layout.home_item, null, false);
			TextView name = (TextView) convertView.findViewById(R.id.name);			
			ImageView fileIcon = (ImageView) convertView.findViewById(R.id.icon);
			ImageView fileIconSpecial = (ImageView) convertView.findViewById(R.id.icon_special);
			TextView totalSpace = (TextView)convertView.findViewById(R.id.totalSize);
			ProgressBar sizeBar = (ProgressBar) convertView.findViewById(R.id.sizeBar);
			RelativeLayout bar = (RelativeLayout) convertView.findViewById(R.id.bar);

			FileNavList fileList = getItem(position);
			name.setText(fileList.getName());
			String path = fileList.getPath();
			File file = new File(path);

			if(path == ExplorerOperations.DIR_ROOT){ 
				fileIcon.setImageResource(iconCache.get(fileList.getIcon()));
				bar.setVisibility(View.VISIBLE);
				
				name.setText(isTablet? format2String(R.string.name_tablet) : fileList.getName());	
				Long max = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_SYSTEM, true);
				Long current = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_SYSTEM, false);
				max += ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_CACHE, true);
				current += ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_CACHE, false);
				max += ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_DATA, true);
				current += ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_DATA, false);				
				
				totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");
				sizeBar.setMax((int)(max/100));
				sizeBar.setProgress((int)(max/100) - (int)(current/100));
			}
			else if(path == ExplorerOperations.DIR_SDCARD){
				fileIcon.setImageResource(iconCache.get(fileList.getIcon()));
				bar.setVisibility(View.VISIBLE);
				name.setText(fileList.getName() + ExplorerOperations.getExtStorageString());
				
				if (!file.exists() || !file.canRead()) {
					Toast.makeText(context, format2String(R.string.msg_sdcard_unmounted), Toast.LENGTH_SHORT).show();
					totalSpace.setText(format2String(R.string.msg_unmounted));
				}
				else{
					Long max = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_EXTERNAL, true);
					Long current = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_EXTERNAL, false);
					totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");				
					sizeBar.setMax((int)(max/100));
					sizeBar.setProgress((int)(max/100) - (int)(current/100));	
				}
			}
			else if(path == ExplorerOperations.DIR_APP_BACKUP){ 
				fileIcon.setImageResource(iconCache.get(fileList.getIcon()));
				bar.setVisibility(View.VISIBLE);
				
				Long max = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_DATA, true);
				Long current = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_DATA, false);
				totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");
				sizeBar.setMax((int)(max/100));
				sizeBar.setProgress((int)(max/100) - (int)(current/100));			
			}
			else if(path == ExplorerOperations.DIR_APP_PROCESS){ 
				fileIcon.setImageResource(iconCache.get(fileList.getIcon()));
				bar.setVisibility(View.VISIBLE);
				
				Long max = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_RAM, true);
				Long current = ExplorerOperations.getPartionSize(ExplorerOperations.PARTITION_RAM, false);
				totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");
				current = max - current;
				sizeBar.setMax((int)(max/100));
				sizeBar.setProgress((int)(current - 0)/100);				
			}
			else if(path == ExplorerOperations.DIR_GALLERY_HIDDEN){ 
				totalSpace.setText("");
				fileIcon.setImageResource(iconCache.get(14));
				fileIconSpecial.setImageResource(0); 
			}
			else if(path == ExplorerOperations.DIR_WALLPAPER){ 
				totalSpace.setText("");
				fileIcon.setImageResource(iconCache.get(17));
				fileIconSpecial.setImageResource(0); 		
			}			
			else if(ExplorerOperations.isExtStorage(file.getName())){ 
				fileIcon.setImageResource(iconCache.get(fileList.getIcon()));
				bar.setVisibility(View.VISIBLE);
				if (file.exists() && file.canRead()) {
					Long max = ExplorerOperations.getExtStorageSize(path, true);
					Long current = ExplorerOperations.getExtStorageSize(path, false);
					totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");				
					sizeBar.setMax((int)(max/100));
					sizeBar.setProgress((int)((max- current)/100));
				}
			}			
/*			else if(ExplorerOperations.DIRS[position] == ExplorerOperations.DIR_SYSTEM){
				details.setVisibility(View.GONE);
				//fileIcon.setImageResource(iconCache.get(3));
				fileIconSpecial.setImageResource(iconCache.get(position+1));
				sizeBar.setVisibility(View.VISIBLE);
				
				Long max = ExplorerOperations.getSizeInternal(true);
				Long current = ExplorerOperations.getSizeInternal(false);
				totalSpace.setText("( "+Formatter.formatFileSize(context, current)+" / "+Formatter.formatFileSize(context, max)+" )");
				sizeBar.setMax((int)(max/100));
				sizeBar.setProgress((int)(max - current)/100);				
			}*/			
			else { 
				totalSpace.setText("");
				fileIcon.setImageResource(iconCache.get(-1));
				fileIconSpecial.setImageResource(iconCache.get(fileList.getIcon()));
			}
	
			return (convertView);
		}
	}
	
    /**
     * @param id
     */
    protected void showSelectedDialog(int id) {
        try {  	
	    	ExplorerOperations newExplorerOperations = new ExplorerOperations();
			Bundle fileInfo= new Bundle();
			fileInfo.putString(ExplorerOperations.CONSTANT_PATH, "");
			newExplorerOperations.onCreateSelectedDialog(id, context, fileInfo);
		} catch (Exception e) { }  		
    }
	
	/**
	 * Adding drawable to list iconCache for re-usability
	 */
	private void fillBitmapCache() {
		TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.AppTheme);
    	iconCache.put(-1, a.getResourceId(R.styleable.AppTheme_specialFolder, 0));
    	iconCache.put(0, R.drawable.file);
    	iconCache.put(1, R.drawable.root);
    	iconCache.put(2, R.drawable.sdcard);
    	iconCache.put(3, R.drawable.apps);
    	iconCache.put(4, R.drawable.process);
    	iconCache.put(5, R.drawable.download);
    	iconCache.put(6, R.drawable.bluetooth);
    	iconCache.put(7, R.drawable.music);
    	iconCache.put(8, R.drawable.gallery);
    	iconCache.put(9, R.drawable.ringtone);
    	iconCache.put(10, R.drawable.movies);
    	iconCache.put(11, R.drawable.system);
    	iconCache.put(12, R.drawable.emmc);
    	iconCache.put(14, R.drawable.lock);
    	iconCache.put(15, R.drawable.usb);
    	iconCache.put(16, R.drawable.hdd);
    	iconCache.put(17, R.drawable.image);
    }
	
	private String format2String(int id){
		return getResources().getString(id);
	}
	
    public void showHelpScreen(){
        LayoutInflater factory = LayoutInflater.from(context);
        final View aboutView = factory.inflate(R.layout.help, null);
        splashScreenDialog = new Dialog(context, R.style.Theme_Tranparent);
        splashScreenDialog.setContentView(aboutView);
        splashScreenDialog.show();      
    }  
}