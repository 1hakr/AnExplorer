/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.BaseFragment;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

/**
 * Display document title editor and save button.
 */
public class SaveFragment extends BaseFragment implements OnClickListener{
	public static final String TAG = "SaveFragment";

	private DocumentInfo mReplaceTarget;
	private EditText mDisplayName;
	private ImageButton mSave;
	private ProgressBar mProgress;
	private boolean mIgnoreNextEdit;

	private ImageButton mCancel;

	private static final String EXTRA_MIME_TYPE = "mime_type";
	private static final String EXTRA_DISPLAY_NAME = "display_name";

	public static void show(FragmentManager fm, String mimeType, String displayName) {
		final Bundle args = new Bundle();
		args.putString(EXTRA_MIME_TYPE, mimeType);
		args.putString(EXTRA_DISPLAY_NAME, displayName);

		final SaveFragment fragment = new SaveFragment();
		fragment.setArguments(args);

		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.container_save, fragment, TAG);
		ft.commitAllowingStateLoss();
	}

	public static SaveFragment get(FragmentManager fm) {
		return (SaveFragment) fm.findFragmentByTag(TAG);
	}
	
	public static void hide(FragmentManager fm){
		if(null != get(fm)){
			fm.beginTransaction().remove(get(fm)).commitAllowingStateLoss();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_save, container, false);

		view.findViewById(R.id.background).setBackgroundColor(SettingsActivity.getPrimaryColor());

		mCancel = (ImageButton) view.findViewById(android.R.id.button2);
		mCancel.setOnClickListener(this);

		mDisplayName = (EditText) view.findViewById(android.R.id.title);
		mDisplayName.addTextChangedListener(mDisplayNameWatcher);
		mDisplayName.setText(getArguments().getString(EXTRA_DISPLAY_NAME));

		mSave = (ImageButton) view.findViewById(android.R.id.button1);
		mSave.setOnClickListener(this);
		mSave.setEnabled(false);

		mProgress = (ProgressBar) view.findViewById(android.R.id.progress);

		return view;
	}

	private TextWatcher mDisplayNameWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (mIgnoreNextEdit) {
				mIgnoreNextEdit = false;
			} else {
				mReplaceTarget = null;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// ignored
		}

		@Override
		public void afterTextChanged(Editable s) {
			// ignored
		}
	};
	
	/**
	 * Set given document as target for in-place writing if user hits save
	 * without changing the filename. Can be set to {@code null} if user
	 * navigates outside the target directory.
	 */
	public void setReplaceTarget(DocumentInfo replaceTarget) {
		mReplaceTarget = replaceTarget;

		if (mReplaceTarget != null) {
			getArguments().putString(EXTRA_DISPLAY_NAME, replaceTarget.displayName);
			mIgnoreNextEdit = true;
			mDisplayName.setText(replaceTarget.displayName);
		}
	}

	public void setSaveEnabled(boolean enabled) {
		mSave.setEnabled(enabled);
	}

	public void setPending(boolean pending) {
		mSave.setVisibility(pending ? View.INVISIBLE : View.VISIBLE);
		mProgress.setVisibility(pending ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onClick(View v) {
		final BaseActivity activity = BaseActivity.get(SaveFragment.this);
		switch (v.getId()) {
		case android.R.id.button1:
			if (mReplaceTarget != null) {
				activity.onSaveRequested(mReplaceTarget);
			} else {
				final String mimeType = getArguments().getString(EXTRA_MIME_TYPE);
				final String displayName = mDisplayName.getText().toString();
                String extension = FileUtils.getExtFromFilename(displayName);
				activity.onSaveRequested(TextUtils.isEmpty(extension) ? mimeType : extension, displayName);
			}
			break;

		case android.R.id.button2:
			getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
			break;
		}
	}
}