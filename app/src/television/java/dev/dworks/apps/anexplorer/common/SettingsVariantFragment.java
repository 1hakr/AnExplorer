package dev.dworks.apps.anexplorer.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceScreen;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import dev.dworks.apps.anexplorer.R;

public class SettingsVariantFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment {
        private final static String PREFERENCE_RESOURCE_ID = "preferenceResource";
        private final static String PREFERENCE_ROOT = "root";
        private PreferenceFragment mPreferenceFragment;

        @Override
        public void onPreferenceStartInitialScreen() {
            mPreferenceFragment = buildPreferenceFragment(R.xml.pref_settings, null);
            startPreferenceFragment(mPreferenceFragment);
        }

        @Override
        public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
                                                 Preference preference) {
            return false;
        }

        @Override
        public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
                PreferenceScreen preferenceScreen) {
            PreferenceFragment frag = buildPreferenceFragment(R.xml.pref_settings,
                    preferenceScreen.getKey());
            startPreferenceFragment(frag);
            return true;
        }

        @Override
        public Preference findPreference(CharSequence charSequence) {
            return mPreferenceFragment.findPreference(charSequence);
        }

        private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
            PreferenceFragment fragment = new PrefFragment();
            Bundle args = new Bundle();
            args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId);
            args.putString(PREFERENCE_ROOT, root);
            fragment.setArguments(args);
            return fragment;
        }

        public static class PrefFragment extends LeanbackPreferenceFragment {

            @Override
            public void onCreatePreferences(Bundle bundle, String s) {
                String root = getArguments().getString(PREFERENCE_ROOT, null);
                int prefResId = getArguments().getInt(PREFERENCE_RESOURCE_ID);
                if (root == null) {
                    addPreferencesFromResource(prefResId);
                } else {
                    setPreferencesFromResource(prefResId, root);
                }
            }

            @Override
            public boolean onPreferenceTreeClick(Preference preference) {
//                if (preference.getKey().equals(getString(R.string.pref_key_login))) {
//                    // Open an AuthenticationActivity
//                    startActivity(new Intent(getActivity(), AuthenticationActivity.class));
//                }
                return super.onPreferenceTreeClick(preference);
            }
        }
}
