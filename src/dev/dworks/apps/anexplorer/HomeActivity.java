package dev.dworks.apps.anexplorer;

import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.FrameLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.OnFragmentInteractionListener;

public class HomeActivity extends SherlockFragmentActivity implements OnFragmentInteractionListener{

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
	private FrameLayout pane_list;
	private HomeFragment homeFragment;
	private NavigationFragment navigationFragment;

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
		if(preference.getInt("exitPref", -1) == -1){
			editor.putInt("exitPref", 1);
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
        
    	initLogin();		
        if(showSplashScreen && getIntent().getStringExtra("Splash") == null){
            showSplashScreen();
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
    	shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        pane_list = (FrameLayout) findViewById(R.id.pane_list);
	}


	private void getPreference() {
		themeType = Integer.valueOf(preference.getString("ThemePref", "2"));
		langType = Integer.valueOf(preference.getString("LangPref", "0"));
		showSplashScreen = !preference.getBoolean("SplashScreenPref", false);
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
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	    	pane_list.setVisibility(ExplorerOperations.showView(true));
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	    	pane_list.setVisibility(ExplorerOperations.showView(false));
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
                }
            }
        };
        splashTread.start();        
    }
    
	private void showLoginDialog(){
    	final boolean passwordSet = !ExplorerOperations.isEmpty(this.password);
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
        }
    	header.setText(format2String(R.string.app_name)+" "+format2String(R.string.msg_login)+ (!passwordSet ? " Setup" : ""));        
        
        dont_show_login.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
				autoLoginChecked = checked;
			}});
        
        final Dialog dialog = new Dialog(this, R.style.Theme_Dailog_Login);
        dialog.setContentView(loginView);
        dialog.setCancelable(false);
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
        		dialog.cancel();
				
			}});
        
        cancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				dialog.cancel();
				finallyFinish();
			}});        
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
    public void onBackPressed() {
    	finallyFinish();
    	super.onBackPressed();
    }
	
	@Override
	public void onFragmentInteraction(Bundle bundle) {
		int operation = bundle.getInt("operation");
		if(operation == 1){
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
}