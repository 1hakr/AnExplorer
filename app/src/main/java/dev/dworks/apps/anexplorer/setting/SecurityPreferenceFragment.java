package dev.dworks.apps.anexplorer.setting;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.PinViewHelper;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_PIN_ENABLED;
import static dev.dworks.apps.anexplorer.setting.SettingsActivity.KEY_PIN_SET;

public class SecurityPreferenceFragment extends PreferenceFragment implements OnPreferenceClickListener {
    private Preference pin_set_preference;
    
    public SecurityPreferenceFragment() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_security);

        findPreference(KEY_PIN_ENABLED).setOnPreferenceClickListener(this);


        pin_set_preference = findPreference(KEY_PIN_SET);
		pin_set_preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
                SettingsActivity.logSettingEvent(preference.getKey());
				checkPin();
				return false;
			}
		});
		pin_set_preference.setSummary(SettingsActivity.isPinProtected(getActivity()) ? R.string.pin_set : R.string.pin_disabled);
    }
	
    private void confirmPin(final String pin) {
    	final Dialog d = new Dialog(getActivity(), R.style.Theme_Document_DailogPIN);
    	d.getWindow().setWindowAnimations(R.style.DialogEnterNoAnimation);
    	PinViewHelper pinViewHelper = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
            public void onEnter(String password) {
                super.onEnter(password);
                if (pin.equals(password)) {
                	SettingsActivity.setPin(getActivity(), password);
                	pin_set_preference.setSummary(SettingsActivity.isPinProtected(getActivity()) ? R.string.pin_set : R.string.pin_disabled);
                    if (password != null && password.length() > 0){
                        showMsg(R.string.pin_set);
                        setInstruction(R.string.pin_set);
                    }
                    d.dismiss();
                    return;
                }
                showError(R.string.pin_mismatch);
                setInstruction(R.string.pin_mismatch);
            }

            public void onCancel() {
                super.onCancel();
                d.dismiss();
            }
        };
        View view = pinViewHelper.getView();
        view.findViewById(R.id.logo).setVisibility(View.GONE);
        pinViewHelper.setInstruction(R.string.confirm_pin);
		d.setContentView(view);
        d.show();
    }
    
    private void setPin() {
    	final Dialog d = new Dialog(getActivity(), R.style.Theme_Document_DailogPIN);
    	d.getWindow().setWindowAnimations(R.style.DialogExitNoAnimation);
        View view = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
            public void onEnter(String password) {
                super.onEnter(password);
                confirmPin(password);
                d.dismiss();
            }

            public void onCancel() {
                super.onCancel();
                d.dismiss();
            }
        }.getView();
        view.findViewById(R.id.logo).setVisibility(View.GONE);
		d.setContentView(view);
        d.show();
    }

    private void checkPin() {
        if (SettingsActivity.isPinProtected(getActivity())) {
            final Dialog d = new Dialog(getActivity(), R.style.Theme_Document_DailogPIN);
            View view = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
                public void onEnter(String password) {
                    super.onEnter(password);
                    if (SettingsActivity.checkPin(getActivity(), password)) {
                        super.onEnter(password);
                        SettingsActivity.setPin(getActivity(), "");
                        pin_set_preference.setSummary(R.string.pin_disabled);
                        showMsg(R.string.pin_disabled);
                        setInstruction(R.string.pin_disabled);
                        d.dismiss();
                        return;
                    }
                    showError(R.string.incorrect_pin);
                    setInstruction(R.string.incorrect_pin);
                }

                public void onCancel() {
                    super.onCancel();
                    d.dismiss();
                }
            }.getView();
            view.findViewById(R.id.logo).setVisibility(View.GONE);
			d.setContentView(view);
			d.show();
        }
        else {
            setPin();
        }
    }
    public void showMsg(int msg){
        showToast(msg, ContextCompat.getColor(getActivity(), R.color.button_text_color_default), Snackbar.LENGTH_SHORT);
    }

    public void showError(int msg){
        showToast(msg, ContextCompat.getColor(getActivity(), R.color.button_text_color_red), Snackbar.LENGTH_SHORT);
    }

    public void showToast(int msg, int actionColor, int duration){
        if(!Utils.isActivityAlive(getActivity())){
            return;
        }
        final Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), msg, duration);
        snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        })
                .setActionTextColor(actionColor).show();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SettingsActivity.logSettingEvent(preference.getKey());
        return false;
    }
}