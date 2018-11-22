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

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.OperationCanceledException;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogFragment;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IconColorUtils;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.CircleImage;

/**
 * Display document title editor and save button.
 */
public class DetailFragment extends DialogFragment {
	public static final String TAG_DETAIL = "DetailFragment";
	private static final String EXTRA_DOC = "document";
	private static final String EXTRA_IS_DIALOG = "is_dialog";
	
	private DocumentInfo doc;
	private boolean isDialog;
	
	private TextView name;
	private TextView type;
	private TextView size;
	private TextView contents;
	private TextView modified;
	private TextView path;
	private ImageView iconMime;
	private ImageView iconThumb;
	private FrameLayout icon;
	private View contents_layout;
    private CircleImage iconMimeBackground;
    private View path_layout;

    public static void show(FragmentManager fm, DocumentInfo doc) {
		final Bundle args = new Bundle();
		args.putParcelable(EXTRA_DOC, doc);
		
		final DetailFragment fragment = new DetailFragment();
		fragment.setArguments(args);

		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.container_info, fragment, TAG_DETAIL);
		ft.commitAllowingStateLoss();
	}

    public static void showAsDialog(FragmentManager fm, DocumentInfo doc) {
		final Bundle args = new Bundle();
		args.putParcelable(EXTRA_DOC, doc);
		args.putBoolean(EXTRA_IS_DIALOG, true);
		
		final DetailFragment fragment = new DetailFragment();
		fragment.setArguments(args);
		fragment.show(fm, TAG_DETAIL);
    }
        
	public static DetailFragment get(FragmentManager fm) {
		return (DetailFragment) fm.findFragmentByTag(TAG_DETAIL);
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
			isDialog = args.getBoolean(EXTRA_IS_DIALOG);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_detail, container, false);

		name = (TextView) view.findViewById(R.id.name);
		type = (TextView) view.findViewById(R.id.type);
		size = (TextView) view.findViewById(R.id.size);
		contents = (TextView) view.findViewById(R.id.contents);
		modified = (TextView) view.findViewById(R.id.modified);
		path = (TextView) view.findViewById(R.id.path);
		
		contents_layout = view.findViewById(R.id.contents_layout);
		path_layout = view.findViewById(R.id.path_layout);

		iconMime = (ImageView) view.findViewById(R.id.icon_mime);
		iconThumb = (ImageView) view.findViewById(R.id.icon_thumb);
        iconMimeBackground = (CircleImage)view.findViewById(R.id.icon_mime_background);

		icon = (FrameLayout)view.findViewById(android.R.id.icon);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(isDialog){
            setShowsDialog(isDialog);
			getDialog().setTitle("Details");
		}
		
		name.setText(doc.displayName);
        name.setTextColor(Utils.getLightColor(SettingsActivity.getPrimaryColor(getActivity())));
        iconMimeBackground.setBackgroundColor(IconColorUtils.loadMimeColor(getActivity(), doc.mimeType, doc.authority, doc.documentId, SettingsActivity.getPrimaryColor(getActivity())));
		path.setText(doc.path);
        if(!TextUtils.isEmpty(doc.path)){
            path.setText(doc.path);
        }
        else{
            path_layout.setVisibility(View.GONE);
        }
		modified.setText(Utils.formatTime(getActivity(), doc.lastModified));
		type.setText(IconUtils.getTypeNameFromMimeType(doc.mimeType));
		
		if(!TextUtils.isEmpty(doc.summary)){
			contents.setText(doc.summary);
			contents_layout.setVisibility(View.VISIBLE);
		}
        int docIcon = doc.icon;
        iconMime.setAlpha(1f);
        iconThumb.setAlpha(0f);
        iconThumb.setImageDrawable(null);

        if (docIcon != 0) {
            iconMime.setImageDrawable(IconUtils.loadPackageIcon(getActivity(), doc.authority, docIcon));
        } else {
            iconMime.setImageDrawable(IconUtils.loadMimeIcon(getActivity(), doc.mimeType, doc.authority, doc.documentId, DocumentsActivity.State.MODE_GRID));
        }
		new DetailTask().execute();
	}
	
	private class DetailTask extends AsyncTask<Void, Void, Void> {

		private Bitmap result;
		String sizeString = "";
		String filePath = "";
		
		@Override
		protected Void doInBackground(Void... params) {
			filePath = doc.path;

			if (!Utils.isDir(doc.mimeType)) {
                final boolean allowThumbnail = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, doc.mimeType);
				int thumbSize = getResources().getDimensionPixelSize(R.dimen.grid_width);
				Point mThumbSize = new Point(thumbSize, thumbSize);
				final Uri uri = DocumentsContract.buildDocumentUri(doc.authority, doc.documentId);
				final Context context = getActivity();
				final ContentResolver resolver = context.getContentResolver();
				ContentProviderClient client = null;
				try {

					if (doc.mimeType.equals(Document.MIME_TYPE_APK) && !TextUtils.isEmpty(filePath)) {
						result = ((BitmapDrawable) IconUtils.loadPackagePathIcon(context, filePath, Document.MIME_TYPE_APK)).getBitmap();
					} else {
						client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, uri.getAuthority());
						result = DocumentsContract.getDocumentThumbnail(resolver, uri, mThumbSize, null);
					}
				} catch (Exception e) {
					if (!(e instanceof OperationCanceledException)) {
						Log.w(TAG_DETAIL, "Failed to load thumbnail for " + uri + ": " + e);
					}
					CrashReportingManager.logException(e);
				} finally {
					ContentProviderClientCompat.releaseQuietly(client);
				}

				sizeString = Formatter.formatFileSize(context, doc.size);
			}
			else{
				if(!TextUtils.isEmpty(filePath)){
					File dir = new File(filePath);
					sizeString = Formatter.formatFileSize(getActivity(), Utils.getDirectorySize(dir));
				}				
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void e) {
			super.onPostExecute(e);
			if(!TextUtils.isEmpty(sizeString)) {
                size.setText(sizeString);
            }

			if(null != result){
                ImageView.ScaleType scaleType = doc.mimeType.equals(Document.MIME_TYPE_APK) ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP;
				iconThumb.setScaleType(scaleType);
				iconThumb.setTag(null);
				iconThumb.setImageBitmap(result);
				final float targetAlpha = iconMime.isEnabled() ? 1f : 0.5f;
                iconMimeBackground.animate().alpha(0f).start();
				iconMime.setAlpha(targetAlpha);
				iconMime.animate().alpha(0f).start();
				iconThumb.setAlpha(0f);
				iconThumb.animate().alpha(targetAlpha).start();
			}
		}
	}
}