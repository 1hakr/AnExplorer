package dev.dworks.apps.anexplorer.directory;


import dev.dworks.apps.anexplorer.common.RecyclerFragment;
import dev.dworks.apps.anexplorer.model.DirectoryResult;

public abstract class DirectoryFragmentFlavour extends RecyclerFragment {

    public void loadNativeAds(final DirectoryResult result) {
        showData(result);
    }

    private void insertNativeAds(DirectoryResult result) {
        showData(result);
    }

    public abstract void showData(DirectoryResult result);
}
