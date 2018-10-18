package dev.dworks.apps.anexplorer.directory;

import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.BaseActivity.TAG;

public class FolderSizeAsyncTask extends AsyncTask<Uri, Void, Long> implements ProviderExecutor.Preemptable {
		private final TextView mSizeView;
		private final CancellationSignal mSignal;
		private final String mPath;
		private final int mPosition;

		public FolderSizeAsyncTask(TextView sizeView, String path, int position) {
			mSizeView = sizeView;
			mSignal = new CancellationSignal();
			mPath = path;
			mPosition = position;
		}

		@Override
		public void preempt() {
			cancel(false);
			mSignal.cancel();
		}

		@Override
		protected Long doInBackground(Uri... params) {
			if (isCancelled())
				return null;

			Long result = null;
			try {
				if (!TextUtils.isEmpty(mPath)) {
					File dir = new File(mPath);
					result = Utils.getDirectorySize(dir);
				}
			} catch (Exception e) {
				if (!(e instanceof OperationCanceledException)) {
					Log.w(TAG, "Failed to calculate size for " + mPath + ": " + e);
				}
				CrashReportingManager.logException(e);
			}
			return result;
		}

		@Override
		protected void onPostExecute(Long result) {
            if (isCancelled()) {
                result = null;
            }
			if (mSizeView.getTag() == this && result != null) {
				mSizeView.setTag(null);
				String size = Formatter.formatFileSize(mSizeView.getContext(), result);
				mSizeView.setText(size);
				DocumentsApplication.getFolderSizes().put(mPosition, result);
			}
		}
	}