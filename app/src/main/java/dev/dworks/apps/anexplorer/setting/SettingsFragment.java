package dev.dworks.apps.anexplorer.setting;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.widget.Toolbar;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.SecurityHelper;
import dev.dworks.apps.anexplorer.misc.Utils;

import static android.app.Activity.RESULT_OK;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.misc.SecurityHelper.REQUEST_CONFIRM_CREDENTIALS;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ACCENT_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ADVANCED_DEVICES;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_FILE_HIDDEN;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_FILE_SIZE;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_FILE_THUMBNAIL;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_FOLDER_ANIMATIONS;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_FOLDER_SIZE;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_PRIMARY_COLOR;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_RECENT_MEDIA;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_ROOT_MODE;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_SECURITY_ENABLED;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_THEME_STYLE;

public class SettingsFragment extends PreferenceFragment
		implements OnPreferenceClickListener, OnPreferenceChangeListener {

	private SecurityHelper securityHelper;
	private Preference preference;

	public SettingsFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_settings);

		//General
		findPreference(KEY_FILE_SIZE).setOnPreferenceClickListener(this);
		findPreference(KEY_FOLDER_SIZE).setOnPreferenceClickListener(this);
		findPreference(KEY_FILE_THUMBNAIL).setOnPreferenceClickListener(this);
		findPreference(KEY_FILE_HIDDEN).setOnPreferenceClickListener(this);
		findPreference(KEY_RECENT_MEDIA).setOnPreferenceClickListener(this);

		//Theme
		Preference preferencePrimaryColor = findPreference(KEY_PRIMARY_COLOR);
		preferencePrimaryColor.setOnPreferenceChangeListener(this);
		preferencePrimaryColor.setOnPreferenceClickListener(this);

		findPreference(KEY_ACCENT_COLOR).setOnPreferenceClickListener(this);

		Preference preferenceThemeStyle = findPreference(KEY_THEME_STYLE);
		preferenceThemeStyle.setOnPreferenceChangeListener(this);
		preferenceThemeStyle.setOnPreferenceClickListener(this);
		if(isTelevision()){
			PreferenceCategory preferenceScreen = (PreferenceCategory) findPreference("pref_theme");
			preferenceScreen.removePreference(preferenceThemeStyle);
		}

		if(isWatch() || isTelevision() || !Utils.hasMarshmallow()){
			getPreferenceScreen().removePreference(findPreference("pref_security"));
		} else {
			//Security
			securityHelper = new SecurityHelper(this);
			preference = findPreference(KEY_SECURITY_ENABLED);
			preference.setOnPreferenceClickListener(this);
			preference.setOnPreferenceChangeListener(this);
		}

		//Advanced
		findPreference(KEY_ADVANCED_DEVICES).setOnPreferenceClickListener(this);
		findPreference(KEY_ROOT_MODE).setOnPreferenceClickListener(this);
		findPreference(KEY_FOLDER_ANIMATIONS).setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		SettingsActivity.logSettingEvent(preference.getKey());
		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CONFIRM_CREDENTIALS) {
			if (resultCode == RESULT_OK) {
				((SwitchPreference)preference).setChecked(!SettingsActivity.isSecurityEnabled(getActivity()));
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.getKey().equals(KEY_SECURITY_ENABLED)) {
			if (Utils.hasMarshmallow() && securityHelper.isDeviceSecure()) {
				securityHelper.authenticate("AnExplorer", "Use device pattern to continue");
			}
		} else {
			SettingsActivity.logSettingEvent(preference.getKey());
			((SettingsActivity) getActivity()).changeActionBarColor(Integer.valueOf(newValue.toString()));
			getActivity().recreate();
			return true;
		}
		return false;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference instanceof PreferenceScreen && !isWatch()) {
			setUpNestedScreen((PreferenceScreen) preference);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
		final Dialog dialog = preferenceScreen.getDialog();

		View listRoot = dialog.findViewById(android.R.id.list);
		LinearLayout root = null;
		try {
			root = (LinearLayout) listRoot.getParent();
		} catch (Exception e) {
			try {
				root = (LinearLayout) listRoot.getParent().getParent();
			} catch (Exception e1) {
				try {
					root = (LinearLayout) listRoot.getParent().getParent().getParent();
				} catch (Exception e2) {
					e.printStackTrace();
				}
			}
		}
		if(null == root){
			return;
		}
		AppBarLayout appBar = (AppBarLayout) LayoutInflater.from(getActivity())
				.inflate(R.layout.layout_toolbar, root, false);
		root.addView(appBar, 0);

		Toolbar toolbar = (Toolbar) appBar.getChildAt(0);
		toolbar.setTitle(preferenceScreen.getTitle());
		int color = SettingsActivity.getPrimaryColor(getActivity());
		toolbar.setBackgroundColor(color);
		int statusBarColor = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor(getActivity()));
		if(Utils.hasLollipop()){
			dialog.getWindow().setStatusBarColor(statusBarColor);
		}
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}
}