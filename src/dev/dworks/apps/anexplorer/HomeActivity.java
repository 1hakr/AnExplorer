package dev.dworks.apps.anexplorer;

import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import dev.dworks.libs.actionbartoggle.ActionBarToggle;
import dev.dworks.libs.actionbarplus.SherlockFragmentActivityPlus;
import com.actionbarsherlock.view.MenuItem;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.OnFragmentInteractionListener;

public class HomeActivity extends SherlockFragmentActivityPlus implements OnFragmentInteractionListener{

	private Context context;
	private Dialog splashScreenDialog;
	private boolean showSplashScreen, loginSuccess = false;
	
	//preferences
	private SharedPreferences preference = null;
	private Integer themeType, langType;
	private SharedPreferences.Editor editor;
	private Animation shake;
	private String password;
	private boolean auto_login, autoLoginChecked = false;	
	private HomeFragment homeFragment;
	private NavigationFragment navigationFragment;
	private SlidingPaneLayout sliding_pane;
	private ActionBarToggle mActionBarToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		context = this.getApplicationContext();
		// get preferences
		preference = PreferenceManager.getDefaultSharedPreferences(context);
		getPreference();
	
		editor = preference.edit();
		if(preference.getInt("adfreePref", -1) == -1){
			editor.putInt("adfreePref", 1);	
			editor.commit();
		} 

		if(preference.getBoolean("SessionLoginPref", false)){
			loginSuccess = true;
		}
		password = preference.getString("LoginPasswordPref", "");
		auto_login = preference.getBoolean("AutoLoginPref", false);
		
		this.setTheme(ExplorerOperations.THEMES[themeType]);
/*		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectNetwork()
		.detectDiskWrites()
		.detectDiskReads()
		.detectCustomSlowCalls()
		.penaltyFlashScreen()
		.penaltyLog()
		.build());	*/	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		//type = ExplorerOperations.isPhone(context) ? TYPES.Phone : TYPES.Tablet;
    	//initLogin();
        if(showSplashScreen){
        	editor.putBoolean("SplashScreenPref", true);
        	showTutorial();
        }
        
		Bundle arguments = new Bundle();
		homeFragment = new HomeFragment();
		homeFragment.setArguments(arguments);
		navigationFragment = new NavigationFragment();
		navigationFragment.setArguments(arguments);
		
		getSupportFragmentManager().beginTransaction().replace(R.id.pane_main, homeFragment).commit();
		getSupportFragmentManager().beginTransaction().replace(R.id.pane_list, navigationFragment).commit();		
		initControls();
	}
	
    private void initControls() {
    	getSupportActionBar().setHomeButtonEnabled(true);
        
    	shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        sliding_pane = (SlidingPaneLayout)findViewById(R.id.sliding_pane);
        sliding_pane.setSliderFadeColor(0);
        int toggle = themeType == 1 ? R.drawable.ic_drawer : R.drawable.ic_drawer_light;
        
        mActionBarToggle = new ActionBarToggle(this, sliding_pane, toggle, R.string.drawer_open, R.string.drawer_close) {

        	@Override
        	public void onPanelOpened(View panel) {
        		super.onPanelOpened(panel);
        		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "onPanelOpened", 0L);
        		homeFragment.setHasOptionsMenu(false);
        		supportInvalidateOptionsMenu();
        	}

        	@Override
        	public void onPanelClosed(View panel) {
        		super.onPanelClosed(panel);
        		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "onPanelClosed", 0L);
        		homeFragment.setHasOptionsMenu(true);
        		supportInvalidateOptionsMenu();
        	}
        };
        
        sliding_pane.setPanelSlideListener(mActionBarToggle);
	}

	private void getPreference() {
		themeType = Integer.valueOf(preference.getString("ThemePref", "2"));
		langType = Integer.valueOf(preference.getString("LangPref", "0"));
		showSplashScreen = !preference.getBoolean("SplashScreenPref", false);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
        updateHomeButton();
        mActionBarToggle.syncState();
	}

	@Override
    protected void onResume() {
        changeLang();
    	super.onResume();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateHomeButton();
        mActionBarToggle.onConfigurationChanged(newConfig);
    }
	
	private void updateHomeButton() {
    	getSupportActionBar().setDisplayHomeAsUpEnabled(!sliding_pane.isSlideable());
	}

	public void changeLang(){
    	Locale locale = new Locale(ExplorerOperations.LANGUAGES_LOCALE[langType]);
    	locale = langType == 0 ? Resources.getSystem().getConfiguration().locale : locale;
    	Locale.setDefault(locale);
    	Configuration config = new Configuration();
    	config.locale = locale;
    	getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
    
	private String format2String(int id){
		return getResources().getString(id);
	}
	
	private void initLogin(){	
        if(!loginSuccess){
        	if(!auto_login)
            showLoginDialog();	
        }
        else{
        	showAdfreeDailog();
        }
	}
	
    private void showAdfreeDailog(){
    	if(preference.getInt("adfreePref", -2) == 1){
			showSelectedDialog(ExplorerOperations.DIALOG_ADFREE);    		
    	}
    }
    
	private void showSplashScreen(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View aboutView = factory.inflate(R.layout.splash, null);
        splashScreenDialog = new Dialog(this, R.style.Theme_Dailog_Splash);
        splashScreenDialog.setContentView(aboutView);
        splashScreenDialog.show();

        Thread splashTread = new Thread() {
            @Override
            public void run() {
            	try {
            		sleep(3000);
				} catch (Exception e) { }
				finally {
					splashScreenDialog.dismiss();
			        showTutorial();
                }
            }
        };
        splashTread.start();        
    }
    
	private void showLoginDialog(){
    	final boolean passwordSet = !TextUtils.isEmpty(this.password);
    	final String setPassword = this.password;
    			
        LayoutInflater factorys = LayoutInflater.from(this);
        final View loginView = factorys.inflate(R.layout.login, null);
        TextView header = (TextView) loginView.findViewById(R.id.login_header);
        final EditText password = (EditText) loginView.findViewById(R.id.password);
        final EditText password_repeat = (EditText) loginView.findViewById(R.id.password_repeat);
        
        CheckBox dont_show_login = (CheckBox) loginView.findViewById(R.id.dont_show_login);
        Button login = (Button) loginView.findViewById(R.id.login_button);
        Button cancel = (Button) loginView.findViewById(R.id.cancel_button);

        if(!passwordSet){
        	password_repeat.setVisibility(View.VISIBLE);
        	cancel.setText(R.string.menu_skip);
        	header.setVisibility(View.VISIBLE);
        }
    	header.setText(format2String(R.string.msg_login)+ (!passwordSet ? " Setup" : ""));        
        dont_show_login.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
				autoLoginChecked = checked;
			}});
        
        final Dialog dialog = new Dialog(this, R.style.Theme_Dailog_Login);
        dialog.setContentView(loginView);
        dialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
        dialog.show();        

        login.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if(passwordSet){
	            	if(password.length() == 0 || password.getText().toString() == ""
	            		|| password.getText().toString().compareTo(setPassword) != 0){
	                	password.startAnimation(shake);
	                	password.setError(format2String(R.string.msg_wrong_password));
	                	return;
	            	}
				}
				else{
	            	if(password.length() == 0 && password.getText().toString() == ""){
	                	password.startAnimation(shake);
	                	password.setError(format2String(R.string.msg_pwd_empty));
	                	return;
	            	}
	            	if(password_repeat.length() == 0 || password_repeat.getText().toString() == ""
	            		|| password_repeat.getText().toString().compareTo(password.getText().toString()) != 0){
	            		password_repeat.startAnimation(shake);
	            		password_repeat.setError(format2String(R.string.msg_pwd_empty));
	            		return;
	            	}
	            	editor.putString("LoginPasswordPref", password.getText().toString());
				}
				editor.putBoolean("AutoLoginPref", autoLoginChecked);
				editor.commit();
        		loginSuccess = true;
        		setSession(loginSuccess);
    			AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "login_done", 0L);
        		dialog.dismiss();
        		
			}});
        
        cancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if(!passwordSet){
					editor.putBoolean("AutoLoginPref", true);
					editor.commit();
					dialog.dismiss();
					AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "login_skip", 0L);
				}
				else{
					dialog.dismiss();
					finallyFinish();	
				}
			}});
    }
    	
    private void showTutorial() {
		if(preference.getInt("tutorialPref", -1) == -1){
	    	Intent intent = new Intent(HomeActivity.this, TutorialActivity.class);
	    	startActivity(intent);
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
    
    private void setSession(boolean set){
		editor.putBoolean("SessionLoginPref", set);
		editor.commit();
    }    
    
    private void finallyFinish() {
    	setSession(false);
    	finish();
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActionBarToggle.onOptionsItemSelected(item)) {
            return true;
        }
/*		switch (item.getItemId()) {
		case android.R.id.home:
			if(sliding_pane.isOpen()){
	    		sliding_pane.closePane();
	    	}
			else{
				sliding_pane.openPane();
			}
			break;
		}*/
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
    	if(sliding_pane.isSlideable() && sliding_pane.isOpen()){
    		sliding_pane.closePane();
    	}
    	else{
	    	finallyFinish();
	    	super.onBackPressed();
    	}
    }
	
	@Override
	public void onFragmentInteraction(Bundle bundle) {
		int operation = bundle.getInt("operation");
		if(operation == 1){
			AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_NAVIGATION, "home", bundle.getString(ExplorerOperations.CONSTANT_PATH), 0L);
			Intent intentExplorer = new Intent(context,ExplorerActivity.class);
			intentExplorer.putExtras(bundle);
			startActivityForResult(intentExplorer, 0);		
		}
		else if(operation == 2){
			
		}
		else if(operation == 3){
			navigationFragment.initData(bundle);
		}		
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_FIRST_USER){
    		Intent homeIntent = new Intent(HomeActivity.this, HomeActivity.class);
    		startActivity(homeIntent);
    		finish();    		
    	} 	
    }

/*	@Override
	public void onPanelSlide(View panel, float slideOffset) {
		
	}

	@Override
	public void onPanelOpened(View panel) {
		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "onPanelOpened", 0L);
		homeFragment.setHasOptionsMenu(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	}

	@Override
	public void onPanelClosed(View panel) {
		AnExplorer.tracker.sendEvent(ExplorerOperations.CATEGORY_OPERATION, "home", "onPanelClosed", 0L);
		homeFragment.setHasOptionsMenu(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}*/
}