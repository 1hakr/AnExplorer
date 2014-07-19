package dev.dworks.apps.anexplorer;

/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.mozilla.universalchardet.UniversalDetector;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

public class NoteActivity extends Activity {
	private static final String ORIGINAL_CONTENT = "origContent";
	private static final int STATE_EDIT = 0;
	private static final int STATE_VIEW = 1;

	// Global mutable variables
	private int mState;
	private Uri mUri;
	private EditText mText;
	private String mOriginalContent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			mState = STATE_VIEW;
			mUri = intent.getData();
		}

		if (Intent.ACTION_PASTE.equals(action)) {
			performPaste();
			mState = STATE_EDIT;
		}

		setContentView(R.layout.note_editor);
		mText = (EditText) findViewById(R.id.note);

		if (savedInstanceState != null) {
			mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mState == STATE_EDIT) {
			// int colTitleIndex =
			// mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
			// String title = mCursor.getString(colTitleIndex);
			// Resources res = getResources();
			// String text =
			// String.format(res.getString(R.string.title_edit), title);
			// setTitle(text);
			// Sets the title to "create" for inserts
		} else if (mState == STATE_VIEW) {
			// setTitle(getText(R.string.title_create));
		}

		SpannableStringBuilder note = openFile(mUri.toString(), "UTF-8");
		mText.setTextKeepState(note);

		if (mOriginalContent == null) {
			 mOriginalContent = note.toString();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(ORIGINAL_CONTENT, mOriginalContent);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Get the current note text.
		String text = mText.getText().toString();
		int length = text.length();
		if (isFinishing() && (length == 0)) {
			setResult(RESULT_CANCELED);
			deleteNote();
		} else if (mState == STATE_EDIT) {
			updateNote(text, null);
		} else if (mState == STATE_VIEW) {
			//updateNote(text, text);
			//mState = STATE_EDIT;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.note_options, menu);

		if (mState == STATE_EDIT) {
			// Append to the
			// menu items for any other activities that can do stuff with it
			// as well. This does a query on the system for any activities that
			// implement the ALTERNATIVE_ACTION for our data, adding a menu item
			// for each one that is found.
			Intent intent = new Intent(null, mUri);
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, NoteActivity.class), null, intent, 0, null);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Check if note has changed and enable/disable the revert option
		/*
		 * int colNoteIndex =
		 * mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE); String
		 * savedNote = mCursor.getString(colNoteIndex); String currentNote =
		 * mText.getText().toString(); if (savedNote.equals(currentNote)) {
		 * menu.findItem(R.id.menu_revert).setVisible(false); } else {
		 * menu.findItem(R.id.menu_revert).setVisible(true); }
		 */
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.menu_save:
			String text = mText.getText().toString();
			updateNote(text, null);
			finish();
			break;
		case R.id.menu_delete:
			deleteNote();
			finish();
			break;
		/*
		 * case R.id.menu_revert: cancelNote(); break;
		 */
		}
		return super.onOptionsItemSelected(item);
	}

	// BEGIN_INCLUDE(paste)
	/**
	 * A helper method that replaces the note's data with the contents of the
	 * clipboard.
	 */
	private final void performPaste() {

		// Gets a handle to the Clipboard Manager
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		// Gets a content resolver instance
		ContentResolver cr = getContentResolver();

		// Gets the clipboard data from the clipboard
		ClipData clip = clipboard.getPrimaryClip();
		if (clip != null) {

			String text = null;
			String title = null;

			// Gets the first item from the clipboard data
			ClipData.Item item = clip.getItemAt(0);

			// Tries to get the item's contents as a URI pointing to a note
			Uri uri = item.getUri();

			// Tests to see that the item actually is an URI, and that the URI
			// is a content URI pointing to a provider whose MIME type is the
			// same
			// as the MIME type supported by the Note pad provider.
			if (uri != null) {// &&
								// NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri)))
								// {

			}

			// If the contents of the clipboard wasn't a reference to a note,
			// then
			// this converts whatever it is to text.
			if (text == null) {
				text = item.coerceToText(this).toString();
			}

			// Updates the current note with the retrieved title and text.
			updateNote(text, title);
		}
	}

	// END_INCLUDE(paste)

	private final void updateNote(String text, String title) {

		ContentValues values = new ContentValues();
		// values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
		// System.currentTimeMillis());

		if (mState == STATE_VIEW) {

			if (title == null) {

				int length = text.length();

				title = text.substring(0, Math.min(30, length));

				if (length > 30) {
					int lastSpace = title.lastIndexOf(' ');
					if (lastSpace > 0) {
						title = title.substring(0, lastSpace);
					}
				}
			}
			// values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
		} else if (title != null) {
			// values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
		}

		// values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

		getContentResolver().update(mUri, // The URI for the record to update.
				values, // The map of column names and new values to apply to
						// them.
				null, // No selection criteria are used, so no where columns are
						// necessary.
				null // No where columns are used, so no where arguments are
						// necessary.
				);

	}

	private final void cancelNote() {
		deleteNote();
		setResult(RESULT_CANCELED);
		finish();
	}

	private final void deleteNote() {
		mText.setText("");
	}

	private SpannableStringBuilder openFile(String uri, String charset) {
		if (uri.startsWith("content://")) {
			// content provider
			try {
				return openFile(getContentResolver().openInputStream(Uri.parse(uri)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// file
			File f = new File(uri);
			if (f.exists()) {
				String mFilename = uri;
				try {
					return openFile(new FileInputStream(f), charset);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}
	
	public SpannableStringBuilder openFile(InputStream is){
	    SpannableStringBuilder result = new SpannableStringBuilder();

	    try {
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);

	        // How do I make this load as multiline text?!?!
	        String line = null;

	        while((line = br.readLine()) != null) {
	            result.append(line+"\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
	    return result;
	}

	protected SpannableStringBuilder openFile(InputStream input, String encode) {
		SpannableStringBuilder result = new SpannableStringBuilder();
		InputStream is;
		int mLinebreak;
		int mLine = 0;
		int mLineToChar = -1;
		try {
			String mCharset = "utf-8";
			mLinebreak = LineBreak.LF;

			is = new BufferedInputStream(input, 65536);
			is.mark(65536);

			// preread leading 64KB
			int nread;
			byte[] buff = new byte[64 * 1024];
			nread = is.read(buff);

			if (nread <= 0) {
				if (encode.length() != 0) {
					mCharset = encode;
				}
				return new SpannableStringBuilder("");
			}

			// Detect charset
			UniversalDetector detector;
			if (encode == null || encode.length() == 0) {

				try {
					detector = new UniversalDetector(null);
					detector.handleData(buff, 0, nread);
					detector.dataEnd();
					encode = detector.getDetectedCharset();
					detector.reset();
				} catch (Exception e1) {
				}
			}
			is.reset();
			// detect linbreak code
			if (encode == null || encode.length() == 0) {
				encode = "utf-8";
			}
			Charset charset = Charset.forName(encode);

			byte[] cr = new byte[] { '\r', };
			byte[] lf = new byte[] { '\n', };
			if (charset != null) {
				ByteBuffer bb;
				bb = charset.encode("\r");
				cr = new byte[bb.limit()];
				bb.get(cr);
				bb = charset.encode("\n");
				lf = new byte[bb.limit()];
				bb.get(lf);
			}

			int linebreak = LineBreak.LF;
			if (cr.length == 1) {
				for (int i = 0; i < nread - 1; i++) {
					if (buff[i] == lf[0]) {
						linebreak = LineBreak.LF;
						break;
					} else if (buff[i] == cr[0]) {
						if (buff[i + 1] == lf[0]) {
							linebreak = LineBreak.CRLF;
						} else {
							linebreak = LineBreak.CR;
						}
						break;
					}
				}
			} else { // cr.length == 2 // we dont think in the case cr.length>2
				for (int i = 0; i < nread - 2; i += 2) {
					if (buff[i] == lf[0] && buff[i + 1] == lf[1]) {
						linebreak = LineBreak.LF;
						break;
					} else if (buff[i] == cr[0] && buff[i + 1] == cr[1]) {
						if (buff[i + 2] == lf[0] && buff[i + 3] == lf[1]) {
							linebreak = LineBreak.CRLF;
						} else {
							linebreak = LineBreak.CR;
						}
						break;
					}
				}
			}
			// if ( encode != null ){
			// Log.e( TAG , "CharSet="+encode+"Linebreak=" + new
			// String[]{"CR","LF","CRLF"}[linebreak]);
			// }else{
			// Log.e( TAG , "CharSet="+"--"+"Linebreak=" + new
			// String[]{"CR","LF","CRLF"}[linebreak]);
			// }
			mCharset = encode;
			mLinebreak = linebreak;

			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(is, encode), 8192 * 2);

				int line = 0;
				String text;
				while ((text = br.readLine()) != null) {
					// remove BOM
					if (line == 0) {
						if (text.length() > 0 && text.charAt(0) == 0xfeff) {
							text = text.substring(1);
						}
					}

					line++;
					if (line == mLine) {
						mLineToChar = result.length();
					}
					result.append(text);
					result.append('\n');
				}
				br.close();
				is.close();
				return result;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public class LineBreak {
		static public final int CR = 0;
		static public final int LF = 1;
		static public final int CRLF = 2;
	}
}