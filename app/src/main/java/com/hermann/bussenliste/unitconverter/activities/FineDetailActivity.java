package com.hermann.bussenliste.unitconverter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.hermann.bussenliste.repository.DataSourceFine;
import com.hermann.bussenliste.unitconverter.fragments.FineDetailFragment;
import com.hermann.bussenliste.common.OnServerTaskListener;
import com.hermann.bussenliste.R;
import com.hermann.bussenliste.common.ServerTask;
import com.hermann.bussenliste.domain.Fine;
import com.hermann.bussenliste.domain.Player;

import org.json.JSONException;


/**
 * An activity representing a single Fine detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link FineListActivity}.
 */
public class FineDetailActivity extends AppCompatActivity implements OnServerTaskListener {

    DataSourceFine dataSourceFine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fine_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        dataSourceFine = new DataSourceFine(this);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(FineDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(FineDetailFragment.ARG_ITEM_ID));
            FineDetailFragment fragment = new FineDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fine_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fine_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, FineListActivity.class));
                return true;
            case R.id.action_save_fine_change:
                saveFineChanges();
                return true;
            case R.id.action_delete_fine:
                deleteFineOnServer(getCurrentFine());
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void saveFineChanges() {
        Fine currentFine = getCurrentFine();
        if (!currentFine.getDescription().isEmpty() && currentFine.getAmount() != 0) {
            dataSourceFine.updateFine(currentFine);
            NavUtils.navigateUpTo(this, new Intent(this, FineListActivity.class));
        }
    }

    public Fine getCurrentFine() {
        FineDetailFragment currentFragment = (FineDetailFragment) getSupportFragmentManager().findFragmentById(R.id.fine_detail_container);
        return currentFragment.getCurrentFine();
    }

    public void deleteFineOnServer(Fine fine) {
        ServerTask serverTask = new ServerTask(this, this);
        serverTask.deleteFine(fine);
    }

    @Override
    public void deleteFineTaskCompleted(Fine fine) {
        dataSourceFine.deleteFine(fine.getId());
        NavUtils.navigateUpTo(this, new Intent(this, FineListActivity.class));
    }

    @Override
    public void deleteFineTaskFailed(int statusCode) {
        Toast.makeText(this,R.string.error_fine_deletion, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deletePlayerTaskCompleted(Player player) {
    }

    @Override
    public void deletePlayerTaskFailed(int statusCode) {
    }

    @Override
    public void downloadTaskCompleted(String response) {
    }

    @Override
    public void downloadTaskFailed(int statusCode) {
    }

    @Override
    public void updateSyncStatusFailed(JSONException e) {
    }

    @Override
    public void uploadTaskCompleted(String response) {
    }

    @Override
    public void uploadTaskFailed(int statusCode) {
    }

    @Override
    public void updateSQLiteDataFailed(JSONException e) {
    }
}
