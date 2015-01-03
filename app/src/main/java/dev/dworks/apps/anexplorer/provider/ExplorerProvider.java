package dev.dworks.apps.anexplorer.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

public class ExplorerProvider extends ContentProvider {
    private static final String TAG = "ExplorerProvider";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".explorer";
    private static final UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int URI_BOOKMARK = 1;
    private static final int URI_BOOKMARK_ID = 2;

    static {
        sMatcher.addURI(AUTHORITY, "bookmark", URI_BOOKMARK);
        sMatcher.addURI(AUTHORITY, "bookmark/*", URI_BOOKMARK_ID);
    }

    public static final String TABLE_BOOKMARK = "bookmark";

    public static class BookmarkColumns {
        public static final String TITLE = "title";
        public static final String ROOT_ID = DocumentsContract.Root.COLUMN_ROOT_ID;
        public static final String DOCUMENT_ID = DocumentsContract.Document.COLUMN_DOCUMENT_ID;
        public static final String AUTHORITY = "authority";
        public static final String ICON = "icon";
        public static final String PATH = "path";
        public static final String FLAGS = "flags";
    }

    public static Uri buildBookmark() {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY).appendPath("bookmark").build();
    }

    private DatabaseHelper mHelper;

    @SuppressWarnings("unused")
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "internal.db";

        private static final int VERSION_INIT = 1;
        private static final int VERSION_AS_BLOB = 3;
        private static final int VERSION_ADD_EXTERNAL = 4;
        private static final int VERSION_ADD_RECENT_KEY = 5;

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, VERSION_ADD_RECENT_KEY);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_BOOKMARK + " (" +
                    BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    BookmarkColumns.TITLE + " TEXT," +
                    BookmarkColumns.AUTHORITY + " TEXT," +
                    BookmarkColumns.ROOT_ID + " TEXT," +
                    BookmarkColumns.ICON + " INTEGER," +
                    BookmarkColumns.PATH + " TEXT," +
                    BookmarkColumns.FLAGS + " INTEGER," +
                    "UNIQUE (" + BookmarkColumns.AUTHORITY + ", " + BookmarkColumns.ROOT_ID + ", " + BookmarkColumns.PATH + ") ON CONFLICT REPLACE " +
                    ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database; wiping app data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARK);
            onCreate(db);
        }
    }

    public ExplorerProvider() {
    }

    @Override
    public boolean onCreate() {
        mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mHelper.getReadableDatabase();
        switch (sMatcher.match(uri)) {
            case URI_BOOKMARK:
                return db.query(TABLE_BOOKMARK, projection, null,
                        null, null, null, sortOrder);
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        final ContentValues key = new ContentValues();
        switch (sMatcher.match(uri)) {
            case URI_BOOKMARK:
                // Ensure that row exists, then update with changed values
                //db.insertWithOnConflict(TABLE_BOOKMARK, null, key, SQLiteDatabase.CONFLICT_IGNORE);
                db.insert(TABLE_BOOKMARK, null, values);

                return uri;
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        final ContentValues key = new ContentValues();
        int count;
        switch (sMatcher.match(uri)) {
            case URI_BOOKMARK_ID:
                String id = uri.getLastPathSegment();
                count = db.delete(TABLE_BOOKMARK,
                        BaseColumns._ID + "=?",
                        new String[]{id});
                break;
            case URI_BOOKMARK:
                count = db.delete(TABLE_BOOKMARK,
                        selection,
                        selectionArgs);

                break;
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }

        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }
}
