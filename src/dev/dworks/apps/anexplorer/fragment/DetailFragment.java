/*
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentInfo;

/**
 * Display document title editor and save button.
 */
public class DetailFragment extends Fragment implements OnClickListener{
	public static final String TAG = "DetailFragment";

	private DocumentInfo mReplaceTarget;
	private TextView mMoveInfo;
	private TextView mRootInfo;
	private ImageButton mSave;
	private ProgressBar mProgress;

	private ImageButton mCancel;
	private DocumentInfo doc;

	private static final String EXTRA_DOC = "doc" +
			"";

	public static void show(FragmentManager fm, DocumentInfo doc) {
		final Bundle args = new Bundle();
		args.putParcelable(EXTRA_DOC, doc);
		
		final DetailFragment fragment = new DetailFragment();
		fragment.setArguments(args);

		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.container_info, fragment, TAG);
		ft.commitAllowingStateLoss();
	}

	public static DetailFragment get(FragmentManager fm) {
		return (DetailFragment) fm.findFragmentByTag(TAG);
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
			doc = args.getParcelable(EXTRA_DOC);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//final Context context = inflater.getContext();

		final View view = inflater.inflate(R.layout.fragment_detail, container, false);

/*		mCancel = (ImageButton) view.findViewById(android.R.id.button2);
		mCancel.setOnClickListener(this);

		mMoveInfo = (TextView) view.findViewById(android.R.id.title);
		mMoveInfo.setText("Paste " + FileUtils.formatFileCount(docs.size()) + " in ");
		mMoveInfo.setEnabled(false);
		
		mRootInfo = (TextView) view.findViewById(android.R.id.text1);

		mSave = (ImageButton) view.findViewById(android.R.id.button1);
		mSave.setOnClickListener(this);
		mSave.setEnabled(false);

		mProgress = (ProgressBar) view.findViewById(android.R.id.progress);
*/
		return view;
	}

	/**
	 * Set given document as target for in-place writing if user hits save
	 * without changing the filename. Can be set to {@code null} if user
	 * navigates outside the target directory.
	 */
	public void setReplaceTarget(DocumentInfo replaceTarget) {
		mReplaceTarget = replaceTarget;

		if (mReplaceTarget != null) {
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
		final DocumentsActivity activity = DocumentsActivity.get(DetailFragment.this);
		switch (v.getId()) {
		case android.R.id.button1:
			if (mReplaceTarget != null) {
			}
			break;

		case android.R.id.button2:
			getActivity().getFragmentManager().beginTransaction().remove(this).commit();
			break;
		}
	}
}