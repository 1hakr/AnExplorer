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

package dev.dworks.apps.anexplorer.provider;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.CallSuper;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.libcore.util.Objects;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;

import static android.support.v4.app.AppOpsManagerCompat.MODE_ALLOWED;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_COMPRESS_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_COPY_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_CREATE_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_DELETE_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_IS_CHILD_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_MOVE_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_REMOVE_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_RENAME_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.METHOD_UNCOMPRESS_DOCUMENT;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.buildDocumentUri;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.buildDocumentUriMaybeUsingTree;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.buildTreeDocumentUri;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.getDocumentId;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.getRootId;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.getSearchDocumentsQuery;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.getTreeDocumentId;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.isTreeUri;

/**
 * Base class for a document provider. A document provider offers read and write
 * access to durable files, such as files stored on a local disk, or files in a
 * cloud storage service. To create a document provider, extend this class,
 * implement the abstract methods, and add it to your manifest like this:
 *
 * <pre class="prettyprint">&lt;manifest&gt;
 *    ...
 *    &lt;application&gt;
 *        ...
 *        &lt;provider
 *            android:name="com.example.MyCloudProvider"
 *            android:authorities="com.example.mycloudprovider"
 *            android:exported="true"
 *            android:grantUriPermissions="true"
 *            android:permission="android.permission.MANAGE_DOCUMENTS"
 *            android:enabled="@bool/isAtLeastKitKat"&gt;
 *            &lt;intent-filter&gt;
 *                &lt;action android:name="android.content.action.DOCUMENTS_PROVIDER" /&gt;
 *            &lt;/intent-filter&gt;
 *        &lt;/provider&gt;
 *        ...
 *    &lt;/application&gt;
 *&lt;/manifest&gt;</pre>
 * <p>
 * When defining your provider, you must protect it with
 * {@link android.Manifest.permission#MANAGE_DOCUMENTS}, which is a permission
 * only the system can obtain. Applications cannot use a documents provider
 * directly; they must go through {@link android.content.Intent#ACTION_OPEN_DOCUMENT} or
 * {@link android.content.Intent#ACTION_CREATE_DOCUMENT} which requires a user to actively
 * navigate and select documents. When a user selects documents through that UI,
 * the system issues narrow URI permission grants to the requesting application.
 * </p>
 * <h3>Documents</h3>
 * <p>
 * A document can be either an openable stream (with a specific MIME type), or a
 * directory containing additional documents (with the
 * {@link android.provider.DocumentsContract.Document#MIME_TYPE_DIR} MIME type). Each directory represents the top
 * of a subtree containing zero or more documents, which can recursively contain
 * even more documents and directories.
 * </p>
 * <p>
 * Each document can have different capabilities, as described by
 * {@link android.provider.DocumentsContract.Document#COLUMN_FLAGS}. For example, if a document can be represented
 * as a thumbnail, your provider can set
 * {@link android.provider.DocumentsContract.Document#FLAG_SUPPORTS_THUMBNAIL} and implement
 * that thumbnail.
 * </p>
 * <p>
 * Each document under a provider is uniquely referenced by its
 * {@link android.provider.DocumentsContract.Document#COLUMN_DOCUMENT_ID}, which must not change once returned. A
 * single document can be included in multiple directories when responding to
 * {@link #queryChildDocuments(String, String[], String)}. For example, a
 * provider might surface a single photo in multiple locations: once in a
 * directory of geographic locations, and again in a directory of dates.
 * </p>
 * <h3>Roots</h3>
 * <p>
 * All documents are surfaced through one or more "roots." Each root represents
 * the top of a document tree that a user can navigate. For example, a root
 * could represent an account or a physical storage device. Similar to
 * documents, each root can have capabilities expressed through
 * {@link android.provider.DocumentsContract.Root#COLUMN_FLAGS}.
 * </p>
 *
 * @see android.content.Intent#ACTION_OPEN_DOCUMENT
 * @see android.content.Intent#ACTION_OPEN_DOCUMENT_TREE
 * @see android.content.Intent#ACTION_CREATE_DOCUMENT
 */
public abstract class DocumentsProvider extends ContentProvider {
    private static final String TAG = "DocumentsProvider";

    public static final String DIRECTORY_SEPERATOR = "/";
    public static final String ROOT_SEPERATOR = ":";

    private static final int MATCH_ROOTS = 1;
    private static final int MATCH_ROOT = 2;
    private static final int MATCH_RECENT = 3;
    private static final int MATCH_SEARCH = 4;
    private static final int MATCH_DOCUMENT = 5;
    private static final int MATCH_CHILDREN = 6;
    private static final int MATCH_DOCUMENT_TREE = 7;
    private static final int MATCH_CHILDREN_TREE = 8;

    private String mAuthority;

    private UriMatcher mMatcher;

    /**
     * Implementation is provided by the parent class.
     */
    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        mAuthority = info.authority;

        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(mAuthority, "root", MATCH_ROOTS);
        mMatcher.addURI(mAuthority, "root/*", MATCH_ROOT);
        mMatcher.addURI(mAuthority, "root/*/recent", MATCH_RECENT);
        mMatcher.addURI(mAuthority, "root/*/search", MATCH_SEARCH);
        mMatcher.addURI(mAuthority, "document/*", MATCH_DOCUMENT);
        mMatcher.addURI(mAuthority, "document/*/children", MATCH_CHILDREN);
        mMatcher.addURI(mAuthority, "tree/*/document/*", MATCH_DOCUMENT_TREE);
        mMatcher.addURI(mAuthority, "tree/*/document/*/children", MATCH_CHILDREN_TREE);

        // Sanity check our setup
        if (!info.exported) {
            throw new SecurityException("Provider must be exported");
        }
        if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grantUriPermissions");
        }
/*        if (!android.Manifest.permission.MANAGE_DOCUMENTS.equals(info.readPermission)
                || !android.Manifest.permission.MANAGE_DOCUMENTS.equals(info.writePermission)) {
            throw new SecurityException("Provider must be protected by MANAGE_DOCUMENTS");
        }
*/
        super.attachInfo(context, info);
    }

    /**
     * Test if a document is descendant (child, grandchild, etc) from the given
     * parent. For example, providers must implement this to support
     * {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE}. You should avoid making network
     * requests to keep this request fast.
     *
     * @param parentDocumentId parent to verify against.
     * @param documentId child to verify.
     * @return if given document is a descendant of the given parent.
     * @see android.provider.DocumentsContract.Root#FLAG_SUPPORTS_IS_CHILD
     */
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return false;
    }

    /** {@hide} */
    private void enforceTree(Uri documentUri) {
        if (isTreeUri(documentUri)) {
            final String parent = getTreeDocumentId(documentUri);
            final String child = getDocumentId(documentUri);
            if (Objects.equals(parent, child)) {
                return;
            }
            if (!isChildDocument(parent, child)) {
                throw new SecurityException(
                        "Document " + child + " is not a descendant of " + parent);
            }
        }
    }

    /**
     * Create a new document and return its newly generated
     * {@link Document#COLUMN_DOCUMENT_ID}. A provider must allocate a new
     * {@link Document#COLUMN_DOCUMENT_ID} to represent the document, which must
     * not change once returned.
     *
     * @param parentDocumentId the parent directory to create the new document
     *            under.
     * @param mimeType the concrete MIME type associated with the new document.
     *            If the MIME type is not supported, the provider must throw.
     * @param displayName the display name of the new document. The provider may
     *            alter this name to meet any internal constraints, such as
     *            conflicting names.
     */
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Create not supported");
    }

    /**
     * Rename an existing document.
     * <p>
     * If a different {@link android.provider.DocumentsContract.Document#COLUMN_DOCUMENT_ID} must be used to
     * represent the renamed document, generate and return it. Any outstanding
     * URI permission grants will be updated to point at the new document. If
     * the original {@link android.provider.DocumentsContract.Document#COLUMN_DOCUMENT_ID} is still valid after the
     * rename, return {@code null}.
     *
     * @param documentId the document to rename.
     * @param displayName the updated display name of the document. The provider
     *            may alter this name to meet any internal constraints, such as
     *            avoiding conflicting names.
     */
    @SuppressWarnings("unused")
    public String renameDocument(String documentId, String displayName)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Rename not supported");
    }

    /**
     * Delete the requested document.
     * <p>
     * Upon returning, any URI permission grants for the given document will be
     * revoked. If additional documents were deleted as a side effect of this
     * call (such as documents inside a directory) the implementor is
     * responsible for revoking those permissions using
     * {@link #revokeDocumentPermission(String)}.
     *
     * @param documentId the document to delete.
     */
    @SuppressWarnings("unused")
    public void deleteDocument(String documentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Delete not supported");
    }

    /**
     * Copy the requested document or a document tree.
     * <p>
     * Copies a document including all child documents to another location within
     * the same document provider. Upon completion returns the document id of
     * the copied document at the target destination. {@code null} must never
     * be returned.
     *
     * @param sourceDocumentId the document to copy.
     * @param targetParentDocumentId the target document to be copied into as a child.
     */
    @SuppressWarnings("unused")
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Copy not supported");
    }

    /**
     * Move the requested document or a document tree.
     *
     * <p>Moves a document including all child documents to another location within
     * the same document provider. Upon completion returns the document id of
     * the copied document at the target destination. {@code null} must never
     * be returned.
     *
     * <p>It's the responsibility of the provider to revoke grants if the document
     * is no longer accessible using <code>sourceDocumentId</code>.
     *
     * @param sourceDocumentId the document to move.
     * @param sourceParentDocumentId the parent of the document to move.
     * @param targetParentDocumentId the target document to be a new parent of the
     *     source document.
     */
    @SuppressWarnings("unused")
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
            String targetParentDocumentId)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Move not supported");
    }

    /**
     * Removes the requested document or a document tree.
     *
     * <p>In contrast to {@link #deleteDocument} it requires specifying the parent.
     * This method is especially useful if the document can be in multiple parents.
     *
     * <p>It's the responsibility of the provider to revoke grants if the document is
     * removed from the last parent, and effectively the document is deleted.
     *
     * @param documentId the document to remove.
     * @param parentDocumentId the parent of the document to move.
     */
    @SuppressWarnings("unused")
    public void removeDocument(String documentId, String parentDocumentId)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Remove not supported");
    }

    public String compressDocument(String parentDocumentId, ArrayList<String> documentIds) throws FileNotFoundException {
        throw new UnsupportedOperationException("Move not supported");
    }

    public String uncompressDocument(String parentDocumentId) throws FileNotFoundException {
        throw new UnsupportedOperationException("Move not supported");
    }


    /**
     * Return all roots currently provided. A provider must define at least one
     * root to display to users, and it should avoid making network requests to
     * keep this request fast.
     * <p>
     * Each root is defined by the metadata columns described in {@link Root},
     * including {@link Root#COLUMN_DOCUMENT_ID} which points to a directory
     * representing a tree of documents to display under that root.
     * <p>
     * If this set of roots changes, you must call {@link ContentResolver#notifyChange(Uri,
     * android.database.ContentObserver)} to notify the system.
     *
     * @param projection list of {@link Root} columns to put into the cursor. If
     *            {@code null} all supported columns should be included.
     */
    public abstract Cursor queryRoots(String[] projection) throws FileNotFoundException;

    /**
     * Return recently modified documents under the requested root. This will
     * only be called for roots that advertise
     * {@link Root#FLAG_SUPPORTS_RECENTS}. The returned documents should be
     * sorted by {@link Document#COLUMN_LAST_MODIFIED} in descending order, and
     * limited to only return the 64 most recently modified documents.
     * <p>
     * Recent documents do not support change notifications.
     *
     * @param projection list of {@link Document} columns to put into the
     *            cursor. If {@code null} all supported columns should be
     *            included.
     * @see DocumentsContract#EXTRA_LOADING
     */
    public Cursor queryRecentDocuments(String rootId, String[] projection)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Recent not supported");
    }

    /**
     * Return metadata for the single requested document. You should avoid
     * making network requests to keep this request fast.
     *
     * @param documentId the document to return.
     * @param projection list of {@link Document} columns to put into the
     *            cursor. If {@code null} all supported columns should be
     *            included.
     */
    public abstract Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException;

    /**
     * Return the children documents contained in the requested directory. This
     * must only return immediate descendants, as additional queries will be
     * issued to recursively explore the tree.
     * <p>
     * If your provider is cloud-based, and you have some data cached or pinned
     * locally, you may return the local data immediately, setting
     * {@link DocumentsContract#EXTRA_LOADING} on the Cursor to indicate that
     * your provider is still fetching additional data. Then, when the network
     * data is available, you can call {@link ContentResolver#notifyChange(Uri,
     * android.database.ContentObserver)} to trigger a requery and return the
     * complete contents.
     *
     * @param parentDocumentId the directory to return children for.
     * @param projection list of {@link Document} columns to put into the
     *            cursor. If {@code null} all supported columns should be
     *            included.
     * @param sortOrder how to order the rows, formatted as an SQL
     *            {@code ORDER BY} clause (excluding the ORDER BY itself).
     *            Passing {@code null} will use the default sort order, which
     *            may be unordered. This ordering is a hint that can be used to
     *            prioritize how data is fetched from the network, but UI may
     *            always enforce a specific ordering.
     * @see DocumentsContract#EXTRA_LOADING
     * @see DocumentsContract#EXTRA_INFO
     * @see DocumentsContract#EXTRA_ERROR
     */
    public abstract Cursor queryChildDocuments(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException;

    /** {@hide} */
    public Cursor queryChildDocumentsForManage(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Manage not supported");
    }

    /**
     * Return documents that match the given query under the requested
     * root. The returned documents should be sorted by relevance in descending
     * order. How documents are matched against the query string is an
     * implementation detail left to each provider, but it's suggested that at
     * least {@link Document#COLUMN_DISPLAY_NAME} be matched in a
     * case-insensitive fashion.
     * <p>
     * Only documents may be returned; directories are not supported in search
     * results.
     * <p>
     * If your provider is cloud-based, and you have some data cached or pinned
     * locally, you may return the local data immediately, setting
     * {@link android.provider.DocumentsContract#EXTRA_LOADING} on the Cursor to indicate that
     * you are still fetching additional data. Then, when the network data is
     * available, you can send a change notification to trigger a requery and
     * return the complete contents.
     * <p>
     * To support change notifications, you must
     * {@link android.database.Cursor#setNotificationUri(android.content.ContentResolver, android.net.Uri)} with a relevant
     * Uri, such as {@link android.provider.DocumentsContract#buildSearchDocumentsUri(String,
     * String, String)}. Then you can call {@link android.content.ContentResolver#notifyChange(android.net.Uri,
     * android.database.ContentObserver, boolean)} with that Uri to send change
     * notifications.
     *
     * @param rootId the root to search under.
     * @param query string to match documents against.
     * @param projection list of {@link Document} columns to put into the
     *            cursor. If {@code null} all supported columns should be
     *            included.
     * @see DocumentsContract#EXTRA_LOADING
     * @see DocumentsContract#EXTRA_INFO
     * @see DocumentsContract#EXTRA_ERROR
     */
    public Cursor querySearchDocuments(String rootId, String query, String[] projection)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Search not supported");
    }

    /**
     * Return concrete MIME type of the requested document. Must match the value
     * of {@link Document#COLUMN_MIME_TYPE} for this document. The default
     * implementation queries {@link #queryDocument(String, String[])}, so
     * providers may choose to override this as an optimization.
     */
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final Cursor cursor = queryDocument(documentId, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE));
            } else {
                return null;
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    /**
     * Open and return the requested document.
     * <p>
     * Your provider should return a reliable {@link ParcelFileDescriptor} to
     * detect when the remote caller has finished reading or writing the
     * document. You may return a pipe or socket pair if the mode is exclusively
     * "r" or "w", but complex modes like "rw" imply a normal file on disk that
     * supports seeking.
     * <p>
     * If you block while downloading content, you should periodically check
     * {@link CancellationSignal#isCanceled()} to abort abandoned open requests.
     *
     * @param documentId the document to return.
     * @param mode the mode to open with, such as 'r', 'w', or 'rw'.
     * @param signal used by the caller to signal if the request should be
     *            cancelled. May be null.
     * @see ParcelFileDescriptor#open(java.io.File, int, android.os.Handler,
     *      OnCloseListener)
     * @see ParcelFileDescriptor#createReliablePipe()
     * @see ParcelFileDescriptor#createReliableSocketPair()
     * @see ParcelFileDescriptor#parseMode(String)
     */
    public abstract ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal) throws FileNotFoundException;

    /**
     * Open and return a thumbnail of the requested document.
     * <p>
     * A provider should return a thumbnail closely matching the hinted size,
     * attempting to serve from a local cache if possible. A provider should
     * never return images more than double the hinted size.
     * <p>
     * If a provider performs expensive operations to download or generate a
     * thumbnail, it should periodically check
     * {@link CancellationSignal#isCanceled()} to abort abandoned thumbnail
     * requests.
     *
     * @param documentId the document to return.
     * @param sizeHint hint of the optimal thumbnail dimensions.
     * @param signal used by the caller to signal if the request should be
     *            cancelled.
     * @see Document#FLAG_SUPPORTS_THUMBNAIL
     */
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, CancellationSignal signal)
            throws FileNotFoundException {
        throw new UnsupportedOperationException("Thumbnails not supported");
    }

    /**
     * Open and return the document in a format matching the specified MIME
     * type filter.
     * <p>
     * A provider may perform a conversion if the documents's MIME type is not
     * matching the specified MIME type filter.
     *
     * @param documentId the document to return.
     * @param mimeTypeFilter the MIME type filter for the requested format. May
     *            be *\/*, which matches any MIME type.
     * @param opts extra options from the client. Specific to the content
     *            provider.
     * @param signal used by the caller to signal if the request should be
     *            cancelled. May be null.
     * @see #getDocumentStreamTypes(String, String)
     */
    @SuppressWarnings("unused")
    public AssetFileDescriptor openTypedDocument(
            String documentId, String mimeTypeFilter, Bundle opts, CancellationSignal signal)
            throws FileNotFoundException {
        throw new FileNotFoundException("The requested MIME type is not supported.");
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #queryRoots(String[])
     * @see #queryRecentDocuments(String, String[])
     * @see #queryDocument(String, String[])
     * @see #queryChildDocuments(String, String[], String)
     * @see #querySearchDocuments(String, String, String[])
     */
    @Override
    public final Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        try {
            switch (mMatcher.match(uri)) {
                case MATCH_ROOTS:
                    return queryRoots(projection);
                case MATCH_RECENT:
                    return queryRecentDocuments(getRootId(uri), projection);
                case MATCH_SEARCH:
                    return querySearchDocuments(
                            getRootId(uri), getSearchDocumentsQuery(uri), projection);
                case MATCH_DOCUMENT:
                case MATCH_DOCUMENT_TREE:
                    enforceTree(uri);
                    return queryDocument(getDocumentId(uri), projection);
                case MATCH_CHILDREN:
                case MATCH_CHILDREN_TREE:
                    enforceTree(uri);
                    if (DocumentsContract.isManageMode(uri)) {
                        return queryChildDocumentsForManage(
                                getDocumentId(uri), projection, sortOrder);
                    } else {
                        return queryChildDocuments(getDocumentId(uri), projection, sortOrder);
                    }
                default:
                    throw new UnsupportedOperationException("Unsupported Uri " + uri);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during query", e);
            return null;
        }
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #getDocumentType(String)
     */
    @Override
    public final String getType(Uri uri) {
        try {
            switch (mMatcher.match(uri)) {
                case MATCH_ROOT:
                    return DocumentsContract.Root.MIME_TYPE_ITEM;
                case MATCH_DOCUMENT:
                case MATCH_DOCUMENT_TREE:
                    enforceTree(uri);
                    return getDocumentType(getDocumentId(uri));
                default:
                    return null;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during getType", e);
            return null;
        }
    }

    /**
     * Implementation is provided by the parent class. Can be overridden to
     * provide additional functionality, but subclasses <em>must</em> always
     * call the superclass. If the superclass returns {@code null}, the subclass
     * may implement custom behavior.
     * <p>
     * This is typically used to resolve a subtree URI into a concrete document
     * reference, issuing a narrower single-document URI permission grant along
     * the way.
     *
     * @see android.provider.DocumentsContract#buildDocumentUriUsingTree(android.net.Uri, String)
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri canonicalize(Uri uri) {
        final Context context = getContext();
        switch (mMatcher.match(uri)) {
            case MATCH_DOCUMENT_TREE:
                enforceTree(uri);

                final Uri narrowUri = buildDocumentUri(uri.getAuthority(), getDocumentId(uri));

                // Caller may only have prefix grant, so extend them a grant to
                // the narrow URI.
                final int modeFlags = getCallingOrSelfUriPermissionModeFlags(context, uri);
                if(Utils.hasKitKat()) {
                    context.grantUriPermission(getCallingPackage(), narrowUri, modeFlags);
                }
                return narrowUri;
        }
        return null;
    }

    private static int getCallingOrSelfUriPermissionModeFlags(Context context, Uri uri) {
        // TODO: move this to a direct AMS call
        int modeFlags = 0;
        if (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            modeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
        }
        if (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            modeFlags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }
        if (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            modeFlags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        }
        return modeFlags;
    }

    /**
     * Implementation is provided by the parent class. Throws by default, and
     * cannot be overriden.
     *
     * @see #createDocument(String, String, String)
     */
    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert not supported");
    }

    /**
     * Implementation is provided by the parent class. Throws by default, and
     * cannot be overriden.
     *
     * @see #deleteDocument(String)
     */
    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete not supported");
    }

    /**
     * Implementation is provided by the parent class. Throws by default, and
     * cannot be overriden.
     */
    @Override
    public final int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }

    /**
     * Implementation is provided by the parent class. Can be overridden to
     * provide additional functionality, but subclasses <em>must</em> always
     * call the superclass. If the superclass returns {@code null}, the subclass
     * may implement custom behavior.
     */
    @CallSuper
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!method.startsWith("android:")) {
            // Ignore non-platform methods
            return super.call(method, arg, extras);
        }

        try {
            return callUnchecked(method, arg, extras);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Failed call " + method, e);
        }
    }

    private Bundle callUnchecked(String method, String arg, Bundle extras)
            throws FileNotFoundException {

        final Context context = getContext();
        final Uri documentUri = extras.getParcelable(DocumentsContract.EXTRA_URI);
        final String authority = documentUri.getAuthority();
        final String documentId = DocumentsContract.getDocumentId(documentUri);
        //final String documentId = extras.getString(Document.COLUMN_DOCUMENT_ID);
        //final Uri documentUri = DocumentsContract.buildDocumentUri(mAuthority, documentId);

        if (!mAuthority.equals(authority)) {
            throw new SecurityException(
                    "Requested authority " + authority + " doesn't match provider " + mAuthority);
        }

        final Bundle out = new Bundle();

        // If the URI is a tree URI performs some validation.
        enforceTree(documentUri);

        if (METHOD_IS_CHILD_DOCUMENT.equals(method)) {
            enforceReadPermissionInner(documentUri, null);

            final Uri childUri = extras.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            final String childAuthority = childUri.getAuthority();
            final String childId = DocumentsContract.getDocumentId(childUri);

            out.putBoolean(DocumentsContract.EXTRA_RESULT,
                    mAuthority.equals(childAuthority)
                            && isChildDocument(documentId, childId));

        } else if (METHOD_CREATE_DOCUMENT.equals(method)) {
            enforceWritePermissionInner(documentUri, null);

            final String mimeType = extras.getString(Document.COLUMN_MIME_TYPE);
            final String displayName = extras.getString(Document.COLUMN_DISPLAY_NAME);
            final String newDocumentId = createDocument(documentId, mimeType, displayName);

            // No need to issue new grants here, since caller either has
            // manage permission or a prefix grant. We might generate a
            // tree style URI if that's how they called us.
            final Uri newDocumentUri = buildDocumentUriMaybeUsingTree(documentUri,
                    newDocumentId);
            out.putParcelable(DocumentsContract.EXTRA_URI, newDocumentUri);

        } else if (METHOD_RENAME_DOCUMENT.equals(method)) {
            enforceWritePermissionInner(documentUri, null);

            final String displayName = extras.getString(Document.COLUMN_DISPLAY_NAME);
            final String newDocumentId = renameDocument(documentId, displayName);

            if (newDocumentId != null) {
                final Uri newDocumentUri = buildDocumentUriMaybeUsingTree(documentUri,
                        newDocumentId);

                // If caller came in with a narrow grant, issue them a
                // narrow grant for the newly renamed document.
                if (!isTreeUri(newDocumentUri)) {
                    final int modeFlags = getCallingOrSelfUriPermissionModeFlags(context,
                            documentUri);
                    if (Utils.hasKitKat()) {
                        context.grantUriPermission(getCallingPackage(), newDocumentUri, modeFlags);
                    }
                }

                out.putParcelable(DocumentsContract.EXTRA_URI, newDocumentUri);

                // Original document no longer exists, clean up any grants.
                revokeDocumentPermission(documentId);
            }

        } else if (METHOD_DELETE_DOCUMENT.equals(method)) {
            enforceWritePermissionInner(documentUri, null);
            deleteDocument(documentId);

            // Document no longer exists, clean up any grants.
            revokeDocumentPermission(documentId);
        } else if (METHOD_COPY_DOCUMENT.equals(method)) {
            final Uri targetUri = extras.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            final String targetId = DocumentsContract.getDocumentId(targetUri);

            enforceReadPermissionInner(documentUri, null);
            enforceWritePermissionInner(targetUri, null);

            final String newDocumentId = copyDocument(documentId, targetId);

            if (newDocumentId != null) {
                final Uri newDocumentUri = buildDocumentUriMaybeUsingTree(documentUri,
                        newDocumentId);

                if (!isTreeUri(newDocumentUri)) {
                    final int modeFlags = getCallingOrSelfUriPermissionModeFlags(context,
                            documentUri);
                    if (Utils.hasKitKat()) {
                        context.grantUriPermission(getCallingPackage(), newDocumentUri, modeFlags);
                    }
                }

                out.putParcelable(DocumentsContract.EXTRA_URI, newDocumentUri);
            }

        } else if (METHOD_MOVE_DOCUMENT.equals(method)) {
            final Uri parentSourceUri = extras.getParcelable(DocumentsContract.EXTRA_PARENT_URI);
            String parentSourceId = null;
            if(null != parentSourceUri) {
                parentSourceId = DocumentsContract.getDocumentId(parentSourceUri);
            }
            final Uri targetUri = extras.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            final String targetId = DocumentsContract.getDocumentId(targetUri);

            enforceWritePermissionInner(documentUri, null);
            enforceReadPermissionInner(parentSourceUri, null);
            enforceWritePermissionInner(targetUri, null);

            final String newDocumentId = moveDocument(documentId, parentSourceId, targetId);

            if (newDocumentId != null) {
                final Uri newDocumentUri = buildDocumentUriMaybeUsingTree(documentUri,
                        newDocumentId);

                if (!isTreeUri(newDocumentUri)) {
                    final int modeFlags = getCallingOrSelfUriPermissionModeFlags(context,
                            documentUri);
                    if (Utils.hasKitKat()) {
                        context.grantUriPermission(getCallingPackage(), newDocumentUri, modeFlags);
                    }
                }

                out.putParcelable(DocumentsContract.EXTRA_URI, newDocumentUri);
            }
        } else if (METHOD_REMOVE_DOCUMENT.equals(method)) {
            final Uri parentSourceUri = extras.getParcelable(DocumentsContract.EXTRA_PARENT_URI);
            final String parentSourceId = DocumentsContract.getDocumentId(parentSourceUri);

            enforceReadPermissionInner(parentSourceUri, null);
            enforceWritePermissionInner(documentUri, null);
            removeDocument(documentId, parentSourceId);

            // It's responsibility of the provider to revoke any grants, as the document may be
            // still attached to another parents.

        } else if (METHOD_COMPRESS_DOCUMENT.equals(method)) {
            enforceWritePermissionInner(documentUri, null);
            final String documentIdTo = extras.getString(DocumentsContract.EXTRA_DOCUMENT_TO);
            final ArrayList<String> documentIdToCompress = extras.getStringArrayList(DocumentsContract.EXTRA_DOCUMENTS_COMPRESS);
            compressDocument(documentId, documentIdToCompress);

            // Document no longer exists, clean up any grants
                /*context.revokeUriPermission(documentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);*/
            revokeDocumentPermission(documentId);
        } else if (METHOD_UNCOMPRESS_DOCUMENT.equals(method)) {
            enforceWritePermissionInner(documentUri, null);
            final String documentIdTo = extras.getString(DocumentsContract.EXTRA_DOCUMENT_TO);
            final String documentIdToCompress = extras.getString(DocumentsContract.EXTRA_DOCUMENTS_COMPRESS);
            uncompressDocument(documentId);

            // Document no longer exists, clean up any grants
            revokeDocumentPermission(documentId);

        } else {
            throw new UnsupportedOperationException("Method not supported " + method);
        }
        return out;
    }

    /**
     * Revoke any active permission grants for the given
     * {@link DocumentsContract.Document#COLUMN_DOCUMENT_ID}, usually called when a document
     * becomes invalid. Follows the same semantics as
     * {@link android.content.Context#revokeUriPermission(android.net.Uri, int)}.
     */
    public final void revokeDocumentPermission(String documentId) {
        final Context context = getContext();
        context.revokeUriPermission(buildDocumentUri(mAuthority, documentId), ~0);
        context.revokeUriPermission(buildTreeDocumentUri(mAuthority, documentId), ~0);
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocument(String, String, CancellationSignal)
     */
    @Override
    public final ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(getDocumentId(uri), mode, null);
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocument(String, String, CancellationSignal)
     */
    @Override
    public final ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(getDocumentId(uri), mode, signal);
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocumentThumbnail(String, Point, CancellationSignal)
     */
    @Override
    public final AssetFileDescriptor openAssetFile(Uri uri, String mode)
            throws FileNotFoundException {
        enforceTree(uri);
        final ParcelFileDescriptor fd = openDocument(getDocumentId(uri), mode, null);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocument(String, String, CancellationSignal)
     */
    @Override
    public final AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        enforceTree(uri);
        final ParcelFileDescriptor fd = openDocument(getDocumentId(uri), mode, signal);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocumentThumbnail(String, Point, CancellationSignal)
     * @see #openTypedDocument(String, String, Bundle, CancellationSignal)
     * @see #getDocumentStreamTypes(String, String)
     */
    @Override
    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, mimeTypeFilter, opts, null);
    }

    /**
     * Implementation is provided by the parent class. Cannot be overriden.
     *
     * @see #openDocumentThumbnail(String, Point, CancellationSignal)
     * @see #openTypedDocument(String, String, Bundle, CancellationSignal)
     * @see #getDocumentStreamTypes(String, String)
     */
    @Override
    public final AssetFileDescriptor openTypedAssetFile(
            Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal)
            throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, mimeTypeFilter, opts, signal);
    }

    /**
     * Return a list of streamable MIME types matching the filter, which can be passed to
     * {@link #openTypedDocument(String, String, Bundle, CancellationSignal)}.
     *
     * <p>The default implementation returns a MIME type provided by
     * {@link #queryDocument(String, String[])} as long as it matches the filter and the document
     * does not have the {@link Document#FLAG_VIRTUAL_DOCUMENT} flag set.
     *
     * @see #getStreamTypes(Uri, String)
     * @see #openTypedDocument(String, String, Bundle, CancellationSignal)
     */
    public String[] getDocumentStreamTypes(String documentId, String mimeTypeFilter) {
        Cursor cursor = null;
        try {
            cursor = queryDocument(documentId, null);
            if (cursor.moveToFirst()) {
                final String mimeType =
                    cursor.getString(cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE));
                final long flags =
                    cursor.getLong(cursor.getColumnIndexOrThrow(Document.COLUMN_FLAGS));
                if ((flags & Document.FLAG_VIRTUAL_DOCUMENT) == 0 && mimeType != null &&
                        mimeTypeMatches(mimeTypeFilter, mimeType)) {
                    return new String[] { mimeType };
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        // No streamable MIME types.
        return null;
    }

    /**
     * Called by a client to determine the types of data streams that this content provider
     * support for the given URI.
     *
     * <p>Overriding this method is deprecated. Override {@link #openTypedDocument} instead.
     *
     * @see #getDocumentStreamTypes(String, String)
     */
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        enforceTree(uri);
        return getDocumentStreamTypes(getDocumentId(uri), mimeTypeFilter);
    }

    private final AssetFileDescriptor openTypedAssetFileImpl(
            Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal)
            throws FileNotFoundException {
        enforceTree(uri);
        final String documentId = getDocumentId(uri);
        if (opts != null && opts.containsKey(ContentResolver.EXTRA_SIZE)) {
            final Point sizeHint = opts.getParcelable(ContentResolver.EXTRA_SIZE);
            return openDocumentThumbnail(documentId, sizeHint, signal);
        }
        if ("*/*".equals(mimeTypeFilter)) {
             // If they can take anything, the untyped open call is good enough.
             return openAssetFile(uri, "r");
        }
        final String baseType = getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
            // Use old untyped open call if this provider has a type for this
            // URI and it matches the request.
            return openAssetFile(uri, "r");
        }
        // For any other yet unhandled case, let the provider subclass handle it.
        return openTypedDocument(documentId, mimeTypeFilter, opts, signal);
    }

    public static boolean mimeTypeMatches(String filter, String test) {
        if (test == null) {
            return false;
        } else if (filter == null || "*/*".equals(filter)) {
            return true;
        } else if (filter.equals(test)) {
            return true;
        } else if (filter.endsWith("/*")) {
            return filter.regionMatches(0, test, 0, filter.indexOf('/'));
        } else {
            return false;
        }
    }

    private int enforceReadPermission(Uri uri, IBinder callerToken)
            throws SecurityException {
        final int mode = enforceReadPermissionInner(uri, callerToken);
        if (mode != MODE_ALLOWED) {
            return mode;
        }

        return AppOpsManager.MODE_ALLOWED;
    }

    private int enforceWritePermission(Uri uri, IBinder callerToken)
            throws SecurityException {
        final int mode = enforceWritePermissionInner(uri, callerToken);
        if (mode != MODE_ALLOWED) {
            return mode;
        }

        return AppOpsManager.MODE_ALLOWED;
    }

    protected int enforceReadPermissionInner(Uri uri, IBinder callerToken)
            throws SecurityException {
        return MODE_ALLOWED;
    }

    protected int enforceWritePermissionInner(Uri uri, IBinder callerToken)
            throws SecurityException {
        return MODE_ALLOWED;
    }

    public void updateRoots(){

    }

    public static String getParentRootIdForDocId(String docId){
        final int splitIndex = docId.indexOf(':', 1);
        final String rootId = docId.substring(0, splitIndex);
        final String path = docId.substring(splitIndex + 1);
        final int pathSplitIndex = path.lastIndexOf(DIRECTORY_SEPERATOR);
        String parentPath = "";
        if (pathSplitIndex >= 0) {
            parentPath = path.substring(0, pathSplitIndex);
        }
        return rootId + ROOT_SEPERATOR + parentPath;
    }

    public static String getRootIdForDocId(String docId){
        final int splitIndex = docId.indexOf(':', 1);
        final String rootId = docId.substring(0, splitIndex);
        return rootId;
    }
}
