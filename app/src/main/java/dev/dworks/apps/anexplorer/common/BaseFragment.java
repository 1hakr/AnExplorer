package dev.dworks.apps.anexplorer.common;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {


    private AppCompatActivity mActivity;

    public AppCompatActivity getAppCompatActivity(){
        return mActivity;
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof AppCompatActivity)) {
            throw new IllegalStateException(getClass().getSimpleName() + " must be attached to a AppCompatActivity.");
        }
        mActivity = (AppCompatActivity) activity;

        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }
}
