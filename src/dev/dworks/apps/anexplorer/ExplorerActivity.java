package dev.dworks.apps.anexplorer;

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import dev.dworks.apps.anexplorer.ExplorerOperations.MODES;
import dev.dworks.apps.anexplorer.ExplorerOperations.OnFragmentInteractionListener;
import dev.dworks.apps.anexplorer.ExplorerOperations.TYPES;

public class ExplorerActivity extends SherlockFragmentActivity implements OnFragmentInteractionListener{

	private Context context;
	private Bundle bundle;
	
	//preferences
	private SharedPreferences preference = null;
	private Integer themeType, langType;
	private SharedPreferences.Editor editor;
	private TYPES type;
	private FrameLayout pane_list;
	//private FrameLayout pane_main;
	private ExplorerFragment explorerFragment;
	private NavigationFragment navigationFragment;
	private ExplorerFragment searchFragment;
	private MODES mode;
	private boolean showNavigationPane;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		context = this.getApplicationContext();
		// get preferences
		preference = PreferenceManager.getDefaultSharedPreferences(context);
		getPreference();
	
		this.setTheme(ExplorerOperations.THEMES[themeType]);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		type = ExplorerOperations.isPhone(context) ? TYPES.Phone : TYPES.Tablet;
        
		initControls();
		initData();
	}
	
    private void initData() {
		bundle = getIntent().getExtras();
		Bundle arguments = new Bundle();		
		if(null == bundle){
			bundle = new Bundle();
			bundle.putString(ExplorerOperations.CONSTANT_PATH, ExplorerOperations.DIR_SDCARD);	
		}
		mode = getMode();
		explorerFragment = ExplorerFragment.newInstance(bundle);
		explorerFragment.setMode(mode);
		getSupportFragmentManager().beginTransaction().replace(R.id.pane_main, explorerFragment).commit();		
		
		navigationFragment = new NavigationFragment();
		navigationFragment.setArguments(arguments);			
		getSupportFragmentManager().beginTransaction().replace(R.id.pane_list, navigationFragment).commit();			
	}
    
	private MODES getMode() {
		MODES mode = MODES.None;
		//global search
		if(getIntent().getAction() != null){
			if(getIntent().getAction().equals(Intent.ACTION_SEARCH)){
				mode = MODES.SearchMode;
				showNavigationPane = true;
			}
			else if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				mode = MODES.FileMode;
			}	
		}
		else{
			String currentPath = bundle.getString(ExplorerOperations.CONSTANT_PATH);
			if(currentPath.compareTo(ExplorerOperations.DIR_APP_BACKUP) == 0){
				mode = MODES.AppMode;
				showNavigationPane = false;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_APP_PROCESS) == 0){
				mode = MODES.ProcessMode;
				showNavigationPane = false;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_GALLERY_HIDDEN) == 0){
				mode = MODES.HideFromGalleryMode;
				showNavigationPane = false;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_WALLPAPER) == 0){
				mode = MODES.WallpaperMode;
				showNavigationPane = false;
			}
			else{
				showNavigationPane = true;
			}
		}
		return mode;
	}

	private void initControls() {
        pane_list = (FrameLayout) findViewById(R.id.pane_list);
       // pane_main = (FrameLayout) findViewById(R.id.pane_main);	
	}

	private void getPreference() {
		themeType = Integer.valueOf(preference.getString("ThemePref", "2"));
		langType = Integer.valueOf(preference.getString("LangPref", "0"));
	}

	@Override
    protected void onResume() {
        changeLang();
		this.onConfigurationChanged(getResources().getConfiguration());
    	super.onResume();
    }
    
	@Override
	protected void onPause() {
		super.onPause();
	}    
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    boolean showNavigation = false;
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	    	showNavigation = true && showNavigationPane;
	    	pane_list.setVisibility(ExplorerOperations.showView(showNavigation));
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	    	showNavigation = type == TYPES.Phone ? false : true && showNavigationPane;
	    	pane_list.setVisibility(ExplorerOperations.showView(showNavigation));
	    }
	}
	
	public void changeLang(){
    	Locale locale = new Locale(ExplorerOperations.LANGUAGES_LOCALE[langType]);
    	locale = langType == 0 ? Resources.getSystem().getConfiguration().locale : locale;
    	Locale.setDefault(locale);
    	Configuration config = new Configuration();
    	config.locale = locale;
    	getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
    
/*	private String format2String(int id){
		return getResources().getString(id);
	}*/
	
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
    
    public void showAdfreeDailog(){
    	if(preference.getInt("adfreePref", -2) == 1){
			showSelectedDialog(ExplorerOperations.DIALOG_ADFREE);    		
    	}
    }
    
    public void setSession(boolean set){
		editor.putBoolean("SessionLoginPref", set);
		editor.commit();
    }    
	
	@Override
	public void onFragmentInteraction(Bundle bundle) {
		int operation = bundle.getInt("operation");
		if(operation == 0){
			int action = bundle.getInt("action");
			if(action == 2){
				getSupportFragmentManager().popBackStack();
			}
			else if(action == 1){
				finish();
			}
			else if(action == 3){
				Intent sendBackIntent = new Intent();
				sendBackIntent.setData(Uri.fromFile(new File(bundle.getString("file"))));
				setResult(RESULT_OK, sendBackIntent);
				finish();
			}
		}
		else if(operation == 1){

		}
		else if(operation == 2){
			searchFragment = ExplorerFragment.newInstance(bundle);
			searchFragment.setMode(MODES.SearchMode);
	        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	        ft.replace(R.id.pane_main, searchFragment);
	        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	        ft.addToBackStack(null);
	        ft.commit();
	        mode = MODES.SearchMode;
		}
		else if(operation == 3){
			int action = bundle.getInt("action");
			if(action == 3){
				explorerFragment.onNavListClick(bundle.getInt("position"));
			}
			else{
				navigationFragment.initData(bundle);
			}
		}
		else if(operation == 4){
			String path = bundle.getString(ExplorerOperations.CONSTANT_PATH);
			explorerFragment.resetList(path);
		}		
	}
	
	@Override
	public void onBackPressed() {
		if(mode == MODES.SearchMode){
			if(searchFragment.goBack()){
				getSupportFragmentManager().popBackStack();
				mode = MODES.None;
			}
		}
		else{
			if(explorerFragment.goBack()){
				finish();
			}	
		}
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_FIRST_USER){
    		Intent homeIntent = new Intent(ExplorerActivity.this, ExplorerActivity.class);
    		setResult(RESULT_FIRST_USER);
    		startActivity(homeIntent);
    		finish();    		
    	} 	
    }	
}