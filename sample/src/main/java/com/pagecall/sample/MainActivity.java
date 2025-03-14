package com.pagecall.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // handle file upload
        super.onActivityResult(requestCode, resultCode, intent);
        PagecallFragment pagecallFragment = getPagecallFragment();
        if (pagecallFragment != null) {
            pagecallFragment.processActivityResult(requestCode, resultCode, intent);
        }
    }

    private PagecallFragment getPagecallFragment() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof PagecallFragment) {
                return ((PagecallFragment) currentFragment);
            }
        }
        return null;
    }
}
