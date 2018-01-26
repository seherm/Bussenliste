package com.hermann.bussenliste;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;


/**
 * An activity representing a single Fine detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link FineListActivity}.
 */
public class FineDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fine_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);


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
                FineDetailFragment currentFragment = (FineDetailFragment) getSupportFragmentManager().findFragmentById(R.id.fine_detail_container);
                Fine currentFine =  currentFragment.getCurrentFine();
                if (!currentFine.getDescription().isEmpty() && currentFine.getAmount() != 0) {
                    DataSourceFine dataSourceFine = new DataSourceFine(this);
                    dataSourceFine.updateFine(currentFine);
                    NavUtils.navigateUpTo(this, new Intent(this, FineListActivity.class));
                }
                return true;
            case R.id.action_delete_fine:
                //TODO:delete fine
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

}
