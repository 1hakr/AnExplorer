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

import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.common.DialogFragment;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor;
import dev.dworks.apps.anexplorer.misc.TintUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.DocumentsActivity.TAG;

/**
 * Dialog to create a new directory.
 */
public class RenameFragment extends DialogFragment {
    private static final String TAG_RENAME = "rename";
	private static final String EXTRA_DOC = "document";
	private boolean editExtension = true;
	private DocumentInfo doc;
	
    public static void show(FragmentManager fm, DocumentInfo doc) {
		final Bundle args = new Bundle();
		args.putParcelable(EXTRA_DOC, doc);
		
        final RenameFragment dialog = new RenameFragment();
        dialog.setArguments(args);
        dialog.show(fm, TAG_RENAME);
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final DocumentsActivity activity = (DocumentsActivity) getActivity();

        final DialogBuilder builder = new DialogBuilder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(context);

        final View view = dialogInflater.inflate(R.layout.dialog_create_dir, null, false);
        final EditText text1 = (EditText) view.findViewById(android.R.id.text1);
        TintUtils.tintWidget(text1);

        String nameOnly = editExtension ? doc.displayName : FileUtils.removeExtension(doc.mimeType, doc.displayName);
        text1.setText(nameOnly);
        text1.setSelection(text1.getText().length());
        
        builder.setTitle(R.string.menu_rename);
        builder.setView(view);

        builder.setPositiveButton(R.string.menu_rename, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String displayName = text1.getText().toString();
                final String fileName = editExtension ? displayName : FileUtils.addExtension(doc.mimeType, displayName);
                		
                new RenameTask(activity, doc, fileName).executeOnExecutor(
                        ProviderExecutor.forAuthority(doc.authority));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
    
    private class RenameTask extends AsyncTask<Void, Void, DocumentInfo> {
        private final DocumentsActivity mActivity;
        private final DocumentInfo mDoc;
		private final String mFileName;

        public RenameTask(
                DocumentsActivity activity, DocumentInfo doc, String fileName) {
            mActivity = activity;
            mDoc = doc;
            mFileName = fileName;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            final ContentResolver resolver = mActivity.getContentResolver();
            ContentProviderClient client = null;
            try {
                final Uri childUri = DocumentsContract.renameDocument(
                		resolver, mDoc.derivedUri, mFileName);
                return DocumentInfo.fromUri(resolver, childUri);
            } catch (Exception e) {
                Log.w(TAG, "Failed to rename directory", e);
                CrashReportingManager.logException(e);
                return null;
            } finally {
            	ContentProviderClientCompat.releaseQuietly(client);
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (!Utils.isActivityAlive(mActivity)){
               return;
            }
            if (result == null) {
                if(!mActivity.isSAFIssue(mDoc.documentId)) {
                    Utils.showError(mActivity, R.string.rename_error);
                }
            }
            mActivity.setPending(false);
        }
    }
}