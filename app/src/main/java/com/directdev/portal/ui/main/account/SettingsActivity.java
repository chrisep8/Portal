package com.directdev.portal.ui.main.account;


import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.directdev.portal.R;
import com.directdev.portal.tools.helper.Portal;


public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingFragment()).commit();

        Portal application = (Portal) getApplication();
    }

    public static class SettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
