package dev.dworks.apps.anexplorer.setting;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.SecurityHelper;
import dev.dworks.apps.anexplorer.misc.Utils;

import static android.app.Activity.RESULT_OK;
import static dev.dworks.apps.anexplorer.misc.SecurityHelper.REQUEST_CONFIRM_CREDENTIALS;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_SECURITY_ENABLED;

public class SecurityPreferenceFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener, OnPreferenceChangeListener {

    private SecurityHelper securityHelper;
    private Preference preference;

    public SecurityPreferenceFragment() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_security);

        securityHelper = new SecurityHelper(this);
        preference = findPreference(KEY_SECURITY_ENABLED);
        preference.setOnPreferenceClickListener(this);
        preference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SettingsActivity.logSettingEvent(preference.getKey());
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(Utils.hasMarshmallow() && securityHelper.isDeviceSecure()) {
            securityHelper.authenticate("AnExplorer", "Use device pattern to continue");
        }
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
}