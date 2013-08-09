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
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.view.View;

import dev.dworks.libs.actionbarplus.SherlockFragmentActivityPlus;
import com.actionbarsherlock.view.MenuItem;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.MODES;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.OnFragmentInteractionListener;

public class ExplorerActivity extends SherlockFragmentActivityPlus implements OnFragmentInteractionListener,
	PanelSlideListener{
	private Context context;
	private Bundle bundle;
	
	//preferences
	private SharedPreferences preference = null;
	private Integer themeType, langType;
	private SharedPreferences.Editor editor;
	private ExplorerFragment explorerFragment;
	private NavigationFragment navigationFragment;
	private ExplorerFragment searchFragment;
	private MODES mode;
	private boolean showNavigationPane;
	private SlidingPaneLayout sliding_pane;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		context = this.getApplicationContext();
		// get preferences
		preference = PreferenceManager.getDefaultSharedPreferences(context);
		getPreference();
	
		this.setTheme(ExplorerOperations.THEMES[themeType]);
		super.onCreate(savedInstanceState);
		mode = getMode();
		setContentView(showNavigationPane ? R.layout.activity_home : R.layout.activity_home_single);
		//type = ExplorerOperations.isPhone(context) ? TYPES.Phone : TYPES.Tablet;
		initControls();
		initData();
	}
	
    private void initData() {
		explorerFragment = ExplorerFragment.newInstance(bundle);
		explorerFragment.setMode(mode);
		getSupportFragmentManager().beginTransaction().replace(R.id.pane_main, explorerFragment).commit();		
		
		if(showNavigationPane){
			navigationFragment = new NavigationFragment();
			navigationFragment.setArguments(new Bundle());			
			getSupportFragmentManager().beginTransaction().replace(R.id.pane_list, navigationFragment).commit();
	        sliding_pane = (SlidingPaneLayout)findViewById(R.id.sliding_pane);
	        sliding_pane.setSliderFadeColor(0);
	        sliding_pane.setPanelSlideListener(this);
		}
	}
    
	public MODES getMode() {
		MODES mode = MODES.None;
		bundle = getIntent().getExtras();
		if(null == bundle){
			bundle = new Bundle();
			bundle.putString(ExplorerOperations.CONSTANT_PATH, ExplorerOperations.DIR_SDCARD);	
		}
		//global search
		showNavigationPane = false;
		if(getIntent().getAction() != null){
			if(getIntent().getAction().equals(Intent.ACTION_SEARCH)){
				mode = MODES.SearchMode;
			}
			else if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				mode = MODES.FileMode;
			}	
		}
		else{
			String currentPath = bundle.getString(ExplorerOperations.CONSTANT_PATH);
			if(currentPath.compareTo(ExplorerOperations.DIR_APP_BACKUP) == 0){
				mode = MODES.AppMode;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_APP_PROCESS) == 0){
				mode = MODES.ProcessMode;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_GALLERY_HIDDEN) == 0){
				mode = MODES.HideFromGalleryMode;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_WALLPAPER) == 0){
				mode = MODES.WallpaperMode;
			}
			else{
				showNavigationPane = true;
			}
		}

		return mode;
	}
	
	private void initControls() {
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
	}
	
	public void changeLang(){
    	Locale locale = new Locale(ExplorerOperations.LANGUAGES_LOCALE[langType]);
    	locale = langType == 0 ? Resources.getSystem().getConfiguration().locale : locale;
    	Locale.setDefault(locale);
    	Configuration config = new Configuration();
    	config.locale = locale;
    	getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
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
		    	if(sliding_pane != null && sliding_pane.isSlideable() && sliding_pane.isOpen()){
		    		sliding_pane.closePane();
		    	}
				else{
					finish();
				}
			}
			else if(action == 3){
				AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "explorer", "file", 0L);
				Intent sendBackIntent = new Intent();
				if(null != bundle.getString("file")){
					sendBackIntent.setData(Uri.fromFile(new File(bundle.getString("file"))));
				}
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
	        AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "explorer", "search", 0L);
		}
		else if(operation == 3){
			int action = bundle.getInt("action");
			if(action == 3){
				explorerFragment.onNavListClick(bundle.getInt("position"));
			}
			else{
				if(null != navigationFragment){
					navigationFragment.initData(bundle);
				}
			}
		}
		else if(operation == 4){
			String path = bundle.getString(ExplorerOperations.CONSTANT_PATH);
			explorerFragment.resetList(path);
		}		
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	    	if(sliding_pane != null && sliding_pane.isSlideable() && sliding_pane.isOpen()){
	    		sliding_pane.closePane();
	    	}
			break;
		}
    	return super.onOptionsItemSelected(item);
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

	@Override
	public void onPanelOpened(View panel) {
		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "explorer", "onPanelOpened", 0L);
		explorerFragment.setHasOptionsMenu(false);
	}

	@Override
	public void onPanelClosed(View panel) {
		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "explorer", "onPanelClosed", 0L);
		explorerFragment.setHasOptionsMenu(true);
	}

	@Override
	public void onPanelSlide(View arg0, float arg1) {
		
	}	
}