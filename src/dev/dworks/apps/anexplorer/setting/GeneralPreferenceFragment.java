package dev.dworks.apps.anexplorer.setting;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ROOT_MODE;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_TRANSLUCENT_MODE;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;

public class GeneralPreferenceFragment extends PreferenceFragment {
	
	public GeneralPreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);

		if(!Utils.isRooted()){
			Preference preference = findPreference(KEY_ROOT_MODE);
			if(null != preference)
			getPreferenceScreen().removePreference(preference);
		}
		
		if(!Utils.hasKitKat()){
			Preference preference = findPreference(KEY_TRANSLUCENT_MODE);
			if(null != preference)
			getPreferenceScreen().removePreference(preference);				
		}
	}
}