package dev.dworks.apps.anexplorer.setting;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ROOT_MODE;

public class AdvancedPreferenceFragment extends PreferenceFragment{

	public AdvancedPreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_advanced);

		if (!Utils.isRooted()) {
			PreferenceScreen screen = getPreferenceScreen();
			Preference preference = findPreference(KEY_ROOT_MODE);
			if (null != preference)
				screen.removePreference(preference);
		}
	}
}