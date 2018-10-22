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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.BaseFragment;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.MaterialProgressBar;

/**
 * Display document title editor and save button.
 */
public class MoveFragment extends BaseFragment implements OnClickListener{
	public static final String TAG = "MoveFragment";

	private DocumentInfo mReplaceTarget;
	private TextView mMoveInfo;
	private TextView mRootInfo;
	private ImageButton mSave;
	private MaterialProgressBar mProgress;

	private ImageButton mCancel;
	private ArrayList<DocumentInfo> docs;

	private static final String EXTRA_DELETE_AFTER = "delete_after";
	private static final String EXTRA_DOC_LIST = "doc_list";

	public static void show(FragmentManager fm, ArrayList<DocumentInfo> docs, boolean deleteAfter) {
		final Bundle args = new Bundle();
		args.putParcelableArrayList(EXTRA_DOC_LIST, docs);
		args.putBoolean(EXTRA_DELETE_AFTER, deleteAfter);
		
		final MoveFragment fragment = new MoveFragment();
		fragment.setArguments(args);

		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.container_save, fragment, TAG);
		ft.commitAllowingStateLoss();
	}

	public static MoveFragment get(FragmentManager fm) {
		return (MoveFragment) fm.findFragmentByTag(TAG);
	}
	
	public static void hide(FragmentManager fm){
		if(null != get(fm)){
			fm.beginTransaction().remove(get(fm)).commitAllowingStateLoss();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if(null != args){
			docs = args.getParcelableArrayList(EXTRA_DOC_LIST);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//final Context context = inflater.getContext();

		final View view = inflater.inflate(R.layout.fragment_move, container, false);
		view.findViewById(R.id.background).setBackgroundColor(SettingsActivity.getPrimaryColor());

		mCancel = (ImageButton) view.findViewById(android.R.id.button2);
		mCancel.setOnClickListener(this);

		mMoveInfo = (TextView) view.findViewById(android.R.id.title);
		mMoveInfo.setText("Paste " + FileUtils.formatFileCount(docs.size()) + " in ");
		mMoveInfo.setEnabled(false);
		
		mRootInfo = (TextView) view.findViewById(android.R.id.text1);

		mSave = (ImageButton) view.findViewById(android.R.id.button1);
		mSave.setOnClickListener(this);
		mSave.setEnabled(false);

		mProgress = (MaterialProgressBar) view.findViewById(android.R.id.progress);
		mProgress.setColor(SettingsActivity.getAccentColor());

		return view;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mReplaceTarget != null) {
            mRootInfo.setText(mReplaceTarget.displayName);
        }
    }

    /**
	 * Set given document as target for in-place writing if user hits save
	 * without changing the filename. Can be set to {@code null} if user
	 * navigates outside the target directory.
	 */
	public void setReplaceTarget(DocumentInfo replaceTarget) {
		mReplaceTarget = replaceTarget;

		if (mRootInfo != null && mReplaceTarget != null) {
			mRootInfo.setText(replaceTarget.displayName);
		}
	}

	public void setSaveEnabled(boolean enabled) {
		mMoveInfo.setEnabled(enabled);
		mSave.setEnabled(enabled);
	}

	public void setPending(boolean pending) {
		mSave.setVisibility(pending ? View.INVISIBLE : View.VISIBLE);
		mProgress.setVisibility(pending ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onClick(View v) {
		final DocumentsActivity activity = DocumentsActivity.get(MoveFragment.this);
		switch (v.getId()) {
		case android.R.id.button1:
			if (mReplaceTarget != null) {
				final boolean deleteAfter = getArguments().getBoolean(EXTRA_DELETE_AFTER);
				activity.onMoveRequested(docs, mReplaceTarget, deleteAfter);
			}
			break;

		case android.R.id.button2:
			getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
			break;
		}
	}
}