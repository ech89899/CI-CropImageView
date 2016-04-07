package net.ciapps.widget.demo;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showFragment();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
    }

    // Views
    //
    private void showFragment() {
        Fragment fragment = null;
        String tag = null;
        Bundle args = new Bundle();

        fragment = new CropImageFragment();
        tag = CropImageFragment.TAG;

        if (fragment != null) {
            fragment.setArguments(args);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, tag).commit();
            invalidateOptionsMenu();
        }
    }

}
