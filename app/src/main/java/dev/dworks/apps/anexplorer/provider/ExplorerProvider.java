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
import android.os.Environment;
import android.provider.BaseColumns;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.network.NetworkConnection;

import static dev.dworks.apps.anexplorer.network.NetworkConnection.SERVER;

public class ExplorerProvider extends ContentProvider {
    private static final String TAG = "ExplorerProvider";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".explorer";
    private static final UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int URI_BOOKMARK = 1;
    private static final int URI_BOOKMARK_ID = 2;
    private static final int URI_CONNECTION = 3;
    private static final int URI_CONNECTION_ID = 4;

    static {
        sMatcher.addURI(AUTHORITY, "bookmark", URI_BOOKMARK);
        sMatcher.addURI(AUTHORITY, "bookmark/*", URI_BOOKMARK_ID);
        sMatcher.addURI(AUTHORITY, "connection", URI_CONNECTION);
        sMatcher.addURI(AUTHORITY, "connection/*", URI_CONNECTION_ID);
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

    public static final String TABLE_CONNECTION = "connection";
    public static class ConnectionColumns implements BaseColumns{
        public static final String NAME = "title";
        public static final String TYPE = "type";
        public static final String SCHEME = "scheme";
        public static final String PATH = "path";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String ANONYMOUS_LOGIN = "anonymous_login";
    }

    public static Uri buildBookmark() {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY).appendPath(TABLE_BOOKMARK).build();
    }

    public static Uri buildConnection() {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY).appendPath(TABLE_CONNECTION).build();
    }

    private DatabaseHelper mHelper;

    @SuppressWarnings("unused")
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "internal.db";
        private final Context mContext;
        private static final int VERSION_INIT = 5;
        private static final int VERSION_CONNECTIONS = 6;

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, VERSION_CONNECTIONS);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTablesV1(db);
            createTablesV2(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            int upgradeTo = oldVersion + 1;
            while (upgradeTo <= newVersion) {
                switch (upgradeTo) {
                    case VERSION_CONNECTIONS:
                        createTablesV2(db);
                        break;
                }
                upgradeTo++;
            }
        }

        private void addDefaultServer(SQLiteDatabase db){
            NetworkConnection connection = new NetworkConnection();
            connection.name = "Transfer to PC";
            connection.host = "";
            connection.port = 2211;
            connection.scheme = "ftp";
            connection.type = SERVER;
            connection.path = Environment.getExternalStorageDirectory().getAbsolutePath();
            connection.setAnonymous(true);

            ContentValues contentValues = new ContentValues();
            contentValues.put(ConnectionColumns.NAME, connection.getName());
            contentValues.put(ConnectionColumns.SCHEME, connection.getScheme());
            contentValues.put(ConnectionColumns.TYPE, connection.getType());
            contentValues.put(ConnectionColumns.PATH, connection.getPath());
            contentValues.put(ConnectionColumns.HOST, connection.getHost());
            contentValues.put(ConnectionColumns.PORT, connection.getPort());
            contentValues.put(ConnectionColumns.USERNAME, connection.getUserName());
            contentValues.put(ConnectionColumns.PASSWORD, connection.getPassword());
            contentValues.put(ConnectionColumns.ANONYMOUS_LOGIN, connection.isAnonymousLogin());
            db.insert(TABLE_CONNECTION, null, contentValues);

        }

        private void createTablesV1(SQLiteDatabase db) {
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

        private void createTablesV2(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_CONNECTION + " (" +
                    BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ConnectionColumns.NAME + " TEXT," +
                    ConnectionColumns.TYPE + " TEXT," +
                    ConnectionColumns.SCHEME + " TEXT," +
                    ConnectionColumns.PATH + " TEXT," +
                    ConnectionColumns.HOST + " TEXT," +
                    ConnectionColumns.PORT + " INTEGER," +
                    ConnectionColumns.USERNAME + " TEXT," +
                    ConnectionColumns.PASSWORD + " TEXT," +
                    ConnectionColumns.ANONYMOUS_LOGIN + " BOOLEAN," +
                    "UNIQUE (" + ConnectionColumns.NAME + ", " + ConnectionColumns.HOST + ", " + ConnectionColumns.PATH +  ") ON CONFLICT REPLACE " +
                    ")");

            addDefaultServer(db);
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
                return db.query(TABLE_BOOKMARK, projection, selection,
                        selectionArgs, null, null, sortOrder);
            case URI_CONNECTION:
                return db.query(TABLE_CONNECTION, projection, selection,
                        selectionArgs, null, null, sortOrder);
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
        switch (sMatcher.match(uri)) {
            case URI_BOOKMARK:
                // Ensure that row exists, then update with changed values
                //db.insertWithOnConflict(TABLE_BOOKMARK, null, key, SQLiteDatabase.CONFLICT_IGNORE);
                db.insert(TABLE_BOOKMARK, null, values);

                return uri;
            case URI_CONNECTION:
                db.insert(TABLE_CONNECTION, null, values);

                return uri;
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        switch (sMatcher.match(uri)) {
            case URI_CONNECTION:
                return db.update(TABLE_CONNECTION, values, selection, selectionArgs);
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        int count;
        String id;
        switch (sMatcher.match(uri)) {
            case URI_BOOKMARK_ID:
                id = uri.getLastPathSegment();
                count = db.delete(TABLE_BOOKMARK,
                        BaseColumns._ID + "=?",
                        new String[]{id});
                break;
            case URI_BOOKMARK:
                count = db.delete(TABLE_BOOKMARK,
                        selection,
                        selectionArgs);

                break;
            case URI_CONNECTION_ID:
                id = uri.getLastPathSegment();
                count = db.delete(TABLE_CONNECTION,
                        BaseColumns._ID + "=?",
                        new String[]{id});
                break;
            case URI_CONNECTION:
                count = db.delete(TABLE_CONNECTION,
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
