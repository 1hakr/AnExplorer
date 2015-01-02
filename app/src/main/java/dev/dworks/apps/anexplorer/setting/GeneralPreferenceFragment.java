package dev.dworks.apps.anexplorer.setting;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_AS_DIALOG;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ROOT_MODE;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ACTIONBAR_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_TRANSLUCENT_MODE;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;

public class GeneralPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{
	
	public GeneralPreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);
		
		Preference preferenceActionBar = findPreference(KEY_ACTIONBAR_COLOR);
		preferenceActionBar.setOnPreferenceChangeListener(this);
		
		if(!Utils.isRooted()){
			PreferenceCategory pref = (PreferenceCategory) getPreferenceManager().findPreference("advanced");
			Preference preference = findPreference(KEY_ROOT_MODE);
			if(null != preference)
				pref.removePreference(preference);
		}
		
		if(!Utils.hasKitKat()){
			PreferenceCategory pref = (PreferenceCategory) getPreferenceManager().findPreference("theme");
			Preference preference = findPreference(KEY_TRANSLUCENT_MODE);
			if(null != preference)
				pref.removePreference(preference);				
			
		}
		
		if(!Utils.isTablet(getActivity())){
			PreferenceCategory pref = (PreferenceCategory) getPreferenceManager().findPreference("theme");
			Preference preference = findPreference(KEY_AS_DIALOG);
			if(null != preference)
				pref.removePreference(preference);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        ((SettingsActivity)getActivity()).changeActionBarColor(Integer.valueOf(newValue.toString()));
		return true;
	}
}