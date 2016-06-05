package dev.dworks.apps.anexplorer.setting;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import dev.dworks.apps.anexplorer.R;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ACTIONBAR_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_THEME_STYLE;

public class ThemePreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{

	public ThemePreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_theme);
		
		Preference preferenceActionBar = findPreference(KEY_ACTIONBAR_COLOR);
		preferenceActionBar.setOnPreferenceChangeListener(this);

		Preference preferenceThemeStyle = findPreference(KEY_THEME_STYLE);
		preferenceThemeStyle.setOnPreferenceChangeListener(this);

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        ((SettingsActivity)getActivity()).changeActionBarColor(Integer.valueOf(newValue.toString()));
		if(preference.getKey().contains(KEY_THEME_STYLE)){
			getActivity().recreate();
		}
		return true;
	}
}