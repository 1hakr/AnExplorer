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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
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

/**
 * Dialog to create a new file.
 */
public class CreateFileFragment extends DialogFragment {
    private static final String TAG = "create_file";
    private static final String EXTRA_MIME_TYPE = "mime_type";
    private static final String EXTRA_DISPLAY_NAME = "display_name";


    public static void show(FragmentManager fm, String mimeType, String displayName) {
        final Bundle args = new Bundle();
        args.putString(EXTRA_MIME_TYPE, mimeType);
        args.putString(EXTRA_DISPLAY_NAME, displayName);

        final CreateFileFragment dialog = new CreateFileFragment();
        dialog.setArguments(args);
        dialog.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final DialogBuilder builder = new DialogBuilder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(context);

        final View view = dialogInflater.inflate(R.layout.dialog_create_dir, null, false);
        final EditText text1 = (EditText) view.findViewById(android.R.id.text1);
        TintUtils.tintWidget(text1);

        String title = getArguments().getString(EXTRA_DISPLAY_NAME);
        if(!TextUtils.isEmpty(title)) {
            text1.setText(title);
            text1.setSelection(title.length());
        }
        builder.setTitle(R.string.menu_create_file);
        builder.setView(view);

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String displayName = text1.getText().toString();
                final String mimeType = getArguments().getString(EXTRA_MIME_TYPE);
                String extension = FileUtils.getExtFromFilename(displayName);
                final DocumentsActivity activity = (DocumentsActivity) getActivity();
                final DocumentInfo cwd = activity.getCurrentDirectory();
                new CreateFileTask(activity, cwd,
                        TextUtils.isEmpty(extension) ? mimeType : extension, displayName).executeOnExecutor(
                        ProviderExecutor.forAuthority(cwd.authority));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    private class CreateFileTask extends AsyncTask<Void, Void, Uri> {
        private final DocumentsActivity mActivity;
        private final DocumentInfo mCwd;
        private final String mMimeType;
        private final String mDisplayName;

        public CreateFileTask(DocumentsActivity activity,
                                DocumentInfo cwd,
                                String mimeType,
                                String displayName) {
            mActivity = activity;
            mCwd = cwd;
            mMimeType = mimeType;
            mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setPending(true);
        }

        @Override
        protected Uri doInBackground(Void... params) {
            final ContentResolver resolver = mActivity.getContentResolver();
            ContentProviderClient client = null;
            Uri childUri = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(
                        resolver, mCwd.derivedUri.getAuthority());
                childUri = DocumentsContract.createDocument(
                        resolver, mCwd.derivedUri, mMimeType, mDisplayName);
            } catch (Exception e) {
                Log.w(DocumentsActivity.TAG, "Failed to create document", e);
                CrashReportingManager.logException(e);
            } finally {
                ContentProviderClientCompat.releaseQuietly(client);
            }

            return childUri;
        }

        @Override
        protected void onPostExecute(Uri result) {
            if (result == null) {
                if(!mActivity.isSAFIssue(mCwd.documentId)) {
                    Utils.showError(mActivity, R.string.save_error);
                }
            }

            mActivity.setPending(false);
        }
    }
}
