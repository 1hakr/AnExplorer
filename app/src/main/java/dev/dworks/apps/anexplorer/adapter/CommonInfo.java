package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;

import androidx.core.content.ContextCompat;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;

import static dev.dworks.apps.anexplorer.adapter.HomeAdapter.TYPE_RECENT;

public class CommonInfo {

    public int type;
    public DocumentInfo documentInfo;
    public RootInfo rootInfo;

    public static CommonInfo from(RootInfo rootInfo, int type){
        CommonInfo commonInfo = new CommonInfo();
        commonInfo.type = type;
        commonInfo.rootInfo = rootInfo;
        return commonInfo;
    }

    public static CommonInfo from(DocumentInfo documentInfo, int type){
        CommonInfo commonInfo = new CommonInfo();
        commonInfo.type = type;
        commonInfo.documentInfo = documentInfo;
        return commonInfo;
    }

    public static CommonInfo from(Cursor cursor){
        DocumentInfo documentInfo = DocumentInfo.fromDirectoryCursor(cursor);
        CommonInfo commonInfo = from(documentInfo, TYPE_RECENT);
        return commonInfo;
    }
}
