package dev.dworks.apps.anexplorer.setting;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import dev.dworks.apps.anexplorer.R;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ACCENT_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_PRIMARY_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_THEME_STYLE;

public class ThemePreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{

	public ThemePreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_theme);
		
		Preference preferencePrimaryColor = findPreference(KEY_PRIMARY_COLOR);
		preferencePrimaryColor.setOnPreferenceChangeListener(this);

		Preference preferenceAccentColor = findPreference(KEY_ACCENT_COLOR);
		preferenceAccentColor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object o) {
				SettingsActivity.logSettingEvent(preference.getKey());
				return false;
			}
		});

		Preference preferenceThemeStyle = findPreference(KEY_THEME_STYLE);
		preferenceThemeStyle.setOnPreferenceChangeListener(this);

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		SettingsActivity.logSettingEvent(preference.getKey());
        ((SettingsActivity)getActivity()).changeActionBarColor(Integer.valueOf(newValue.toString()));
		getActivity().recreate();
		return true;
	}
}