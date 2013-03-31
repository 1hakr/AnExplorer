/**
 * 
 */
package dev.dworks.apps.anexplorer;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * @author HaKr
 * 
 */
public class Setting extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private LinearLayout navigationPane;
	private RelativeLayout titlePane;
	private Integer themeType, themeTypeInt, themeTypeOrig, langType, langTypeOrig,
					viewTypeOrig, viewType, sortOrder, sortOrderOrig, sortType, sortTypeOrig;
	private static ListPreference themePref, viewPref, typePref, orderPref, langPref, freqPref;
	private boolean showHeader =  ExplorerOperations.checkDevice();
	private static final int[] SETTINGS = {
			R.xml.setting_display, R.xml.setting_preview, 
			R.xml.setting_sorting, R.xml.setting_wallpaper,
			R.xml.setting_login, R.xml.setting_advanced, 
			R.xml.setting_others };
	
	private AdView adView;
	private Integer prefType;
	
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	getPreferences();
    	themeTypeOrig = themeTypeInt;
    	langTypeOrig = langType;
    	viewTypeOrig = viewType;
    	sortOrderOrig = sortOrder;
    	sortTypeOrig = sortType;
		this.setTheme(themeType);
  
        super.onCreate(savedInstanceState);
        changeLang();      
    	this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	this.getSupportActionBar().setTitle(getResources().getString(R.string.constant_setting));         

        if(!showHeader){
            setContentView(R.layout.home);
        	String action = getIntent().getAction();
        	if(action != null){
	            prefType = Integer.valueOf(action);        
	            addPreferencesFromResource(SETTINGS[prefType]);
        	}
        	else{
        		prefType = -1;
        		addPreferencesFromResource(R.xml.settings_headers_legacy);      		
        	}
            initControls();
            initPreferences();          	
        }
        this.onConfigurationChanged(getResources().getConfiguration());
    }
    
    public void getPreferences(){
    	themeTypeInt = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("ThemePref", "2"));
    	themeType = ExplorerOperations.THEMES[themeTypeInt];
    	langType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("LangPref", "0"));
    	viewType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("ViewPref", "0"));
    	sortType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("SortingTypePref", "1"));
    	sortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("SortingOrderPref", "0"));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

	    adView = new AdView(this, AdSize.BANNER, "a14e25123e38970");
        if (showHeader) {	    
        	setListFooter(adView);
        }
        else{
        	getListView().addFooterView(adView);
        }
        adView.loadAd(new AdRequest());	    
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
    
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }    
    
    @SuppressWarnings("deprecation")
	private void initPreferences(){
        switch (prefType) {
		case 0:
    		themePref = (ListPreference) getPreferenceManager().findPreference("ThemePref");
    		themePref.setSummary(themePref.getEntry());
    		
    		viewPref = (ListPreference) getPreferenceManager().findPreference("ViewPref");
    		viewPref.setSummary(viewPref.getEntry());
			break;
			
		case 2: 
    		typePref = (ListPreference) getPreferenceManager().findPreference("SortingTypePref");
    		typePref.setSummary(typePref.getEntry());
    		
    		orderPref = (ListPreference) getPreferenceManager().findPreference("SortingOrderPref");
    		orderPref.setSummary(orderPref.getEntry());
    		break;
    		
		case 3:
    		freqPref = (ListPreference) getPreferenceManager().findPreference("freqPref");
    		freqPref.setSummary(freqPref.getEntry());
    		break;
    		
		case 6:
    		langPref = (ListPreference) getPreferenceManager().findPreference("LangPref");
    		langPref.setSummary(langPref.getEntry());
    		break;        		
		default:
			break;
		}     
    }

	private void initControls() {		
    	titlePane = (RelativeLayout)findViewById(R.id.title_pane);
    	navigationPane = (LinearLayout) findViewById(R.id.navigation_pane);
		navigationPane.setVisibility(View.GONE);
    	titlePane.setVisibility(View.GONE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				break;
		}	
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();		
		getPreferences();
    	if(themeTypeOrig != themeTypeInt || langTypeOrig != langType){
    		reStartActivity();	
    	}
    	else if(viewTypeOrig != viewType
    			|| sortTypeOrig != sortType
    			|| sortOrder != sortOrderOrig){
    		Intent intent = getIntent();
    		Bundle extraBundle = new Bundle();
    		extraBundle.putBoolean("restart", true);
    		intent.putExtras(extraBundle);
    		setIntent(intent);
    	}
	}
	
    @SuppressWarnings("deprecation")
	@Override
    protected void onResume() {
        super.onResume();
        if (showHeader) {
        	getPreferences();
        }
        else {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);	
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        getPreferences();
        if (!showHeader) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }        
        super.onPause();        
    }	
	
    @SuppressWarnings("deprecation")
	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);
		if(pref instanceof ListPreference){
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
		if(key.compareTo("ThemePref") == 0 
				|| key.compareTo("LangPref") == 0 
				||  key.compareTo("ViewPref") == 0
				|| key.compareTo("SortingTypePref") == 0
				|| key.compareTo("SortingOrderPref") == 0){
			reStartActivity();
		}		
    }	
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    }
	
	@Override
	public void finish() {
		setResult(RESULT_FIRST_USER, getIntent());
		super.finish();
	}
	
	public void reStartActivity(){		
		Bundle extraBundle = new Bundle();
		extraBundle.putBoolean("restart", true);
		getIntent().putExtras(extraBundle);
		startActivity(getIntent());
		finish();
	}	

    public static class SettingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{
    	int prefType;
    	boolean isTablet;
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            isTablet =  ExplorerOperations.isTablet(getActivity().getApplicationContext());
            prefType = Integer.valueOf(getArguments().get("type").toString());            
            addPreferencesFromResource(SETTINGS[prefType]);

            if(!isTablet){
                getActivity().getActionBar().setTitle(getArguments().get("title").toString());	
            }
 
            Preference adPref = new Preference(getActivity());
            adPref.setLayoutResource(R.layout.adview_layout);
            getPreferenceScreen().addPreference(adPref);
            
            switch (prefType) {
			case 0:
        		themePref = (ListPreference) getPreferenceManager().findPreference("ThemePref");
        		themePref.setSummary(themePref.getEntry());
        		themePref.setOnPreferenceChangeListener(this);
        		
        		viewPref = (ListPreference) getPreferenceManager().findPreference("ViewPref");
        		viewPref.setSummary(viewPref.getEntry());
        		viewPref.setOnPreferenceChangeListener(this);
				break;
				
			case 2: 
        		typePref = (ListPreference) getPreferenceManager().findPreference("SortingTypePref");
        		typePref.setSummary(typePref.getEntry());
        		typePref.setOnPreferenceChangeListener(this);
        		
        		orderPref = (ListPreference) getPreferenceManager().findPreference("SortingOrderPref");
        		orderPref.setSummary(orderPref.getEntry());
        		orderPref.setOnPreferenceChangeListener(this);
        		break;
        		
			case 3:
        		freqPref = (ListPreference) getPreferenceManager().findPreference("freqPref");
        		freqPref.setSummary(freqPref.getEntry());
        		freqPref.setOnPreferenceChangeListener(this);
        		break;
        		
			case 6:
        		langPref = (ListPreference) getPreferenceManager().findPreference("LangPref");
        		langPref.setSummary(langPref.getEntry());
        		langPref.setOnPreferenceChangeListener(this);
        		break;        		
			default:
				break;
			}            
        }

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			
			if(preference instanceof ListPreference){
				ListPreference listPref = (ListPreference) preference;
				listPref.setSummary(listPref.getEntries()[Integer.valueOf(newValue.toString())]);
				if(preference.getKey().compareTo("ThemePref") == 0 || preference.getKey().compareTo("LangPref") == 0){
					Bundle extraBundle = new Bundle();
					extraBundle.putBoolean("restart", true);
					Intent intent = getActivity().getIntent();
					
					intent.putExtras(extraBundle);
					getActivity().startActivity(intent);
		    		getActivity().finish();					
				}
				if(preference.getKey().compareTo("viewPref") == 0
						|| preference.getKey().compareTo("SortingTypePref") == 0
						|| preference.getKey().compareTo("SortingOrderPref") == 0){
					Bundle extraBundle = new Bundle();
					extraBundle.putBoolean("restart", true);
					Intent intent = getActivity().getIntent();
					
					intent.putExtras(extraBundle);
					getActivity().setIntent(intent);		
				}				
				else if(preference.getKey().compareTo("freqPref") == 0){
                	AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        			int frequency_type = Integer.valueOf(sharedPreferences.getString("freqType", "1"));                	
                	Calendar calendar = Calendar.getInstance();
                	calendar.setTimeInMillis(System.currentTimeMillis());
                	
					Intent wallpaperIntent = new Intent(getActivity(), MyReceiver.class);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, wallpaperIntent, 0);
                	alarmManager.cancel(pendingIntent);
                	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                			calendar.getTimeInMillis() + ExplorerOperations.getFrequencySec(frequency_type),
                			ExplorerOperations.getFrequencySec(frequency_type),
                			pendingIntent);					
				}
				return true;
			}
			else if(preference instanceof SwitchPreference){
				SwitchPreference switchPreference = (SwitchPreference) preference;
				if(preference.getKey().compareTo("WallpaperPref") == 0){
                	AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        			int frequency_type = Integer.valueOf(sharedPreferences.getString("freqType", "1"));                	
                	Calendar calendar = Calendar.getInstance();
                	calendar.setTimeInMillis(System.currentTimeMillis());
					Intent wallpaperIntent = new Intent(getActivity(), MyReceiver.class);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, wallpaperIntent, 0);
					
                	if(switchPreference.isChecked()){
                    	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    			calendar.getTimeInMillis() + ExplorerOperations.getFrequencySec(frequency_type),
                    			ExplorerOperations.getFrequencySec(frequency_type),
                    			pendingIntent);		                		
                	}
                	else{
	                	alarmManager.cancel(pendingIntent);
                	}
				}
				else if(preference.getKey().compareTo("RootAccessPref") == 0){
					if(!ExplorerOperations.checkSu()){
						switchPreference.setChecked(false);
					}
				}
				else if(preference.getKey().compareTo("MounWritePref") == 0){
					if(!ExplorerOperations.checkSu()){
						switchPreference.setChecked(false);
					}
				}				
			}
			return false;
		}		
    }
}