package com.directdev.portal.ui.main.account.finance;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.directdev.portal.R;
import com.directdev.portal.tools.helper.MainViewPagerAdapter;
import com.directdev.portal.tools.model.Finance;
import com.directdev.portal.ui.access.WebappActivity;

import java.text.NumberFormat;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

public class FinanceActivity extends AppCompatActivity {
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        String totalCharge = "0";
        setContentView(R.layout.activity_finance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.finance_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.finance_collapsing_toolbar);
        RealmResults<Finance> finance = realm.where(Finance.class).findAll();
        if (!finance.isEmpty()){
            totalCharge = NumberFormat.getNumberInstance(Locale.US).format(finance.sum("ITEM_AMT"));
        }

        if (totalCharge.equals("0")) {
            collapsingToolbar.setTitle("No unpaid bill");
        } else {
            collapsingToolbar.setTitle("Rp. " + totalCharge);
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.finance_viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.finance_tab);
        tabLayout.setupWithViewPager(viewPager);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("View Finance")
                .putContentType("Activity")
                .putContentId("studentData"));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_finance, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_finance_webapp:
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();

                if (isConnected) {
                    Intent intent = new Intent(this, WebappActivity.class);
                    intent.putExtra("url", "https://newbinusmaya.binus.ac.id/newStudent/#/financial/financialStatus");
                    intent.putExtra("title", "Financial Status");
                    startActivity(intent);
                } else {
                    Toast connection = Toast.makeText(this, "You are offline, please find a connection", Toast.LENGTH_SHORT);
                    connection.show();
                }
                return true;
            case R.id.action_finance_info:
                Toast toast = Toast.makeText(FinanceActivity.this, "Financial data will be updated together with schedules", Toast.LENGTH_LONG);
                toast.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void setupViewPager(ViewPager viewPager) {
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new BillingFragment(), "BILLING");
        adapter.addFrag(new PaymentFragment(), "PAYMENT");
        viewPager.setAdapter(adapter);
    }
}
