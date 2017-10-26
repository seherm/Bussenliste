package com.hermann.bussenliste;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PlayerDetailsActivity extends AppCompatActivity {

    private ArrayList<Fine> selectedItems;
    private Player selectedPlayer;
    private FinesAdapter finesAdapter;
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private TextView totalSumOfFines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_details);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFineSelectionDialog();
            }
        });

        selectedPlayer = (Player) getIntent().getSerializableExtra("SelectedPlayer");
        TextView playerName = (TextView) findViewById(R.id.player_name);
        playerName.setText(selectedPlayer.getName());

        updateTotalSumOfFinesView();

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);

        final ListView finesListView = (ListView) findViewById(R.id.finesListView);
        finesAdapter = new FinesAdapter(this, selectedPlayer.getFines());
        finesListView.setAdapter(finesAdapter);
        finesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        finesListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                int checkedCount = finesListView.getCheckedItemCount();
                actionMode.setTitle(Integer.toString(checkedCount));
                finesAdapter.toggleSelection(i);
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.delete_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.delete_mode:
                        SparseBooleanArray selected = finesAdapter.getSelectedIds();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Fine selectedItem = finesAdapter.getItem(selected.keyAt(i));
                                finesAdapter.remove(selectedItem);
                                selectedPlayer.getFines().remove(selectedItem);
                            }
                        }
                        String fineAmount = getString(R.string.fineAmount, selectedPlayer.getTotalSumOfFines());
                        totalSumOfFines.setText(fineAmount);
                        try {
                            dataSourcePlayer.updatePlayer(selectedPlayer.getId(), selectedPlayer.getFines());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        actionMode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    private void showFineSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        selectedItems = new ArrayList<>();
        final List<Fine> allFines = dataSourceFine.getAllFines();
        String[] namesStringArray = new String[allFines.size()];
        for (int i = 0; i < allFines.size(); i++) {
            namesStringArray[i] = allFines.get(i).getDescription();
        }
        builder.setTitle(R.string.add_fair)

                .setMultiChoiceItems(namesStringArray, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selectedItems.add(allFines.get(which));
                                } else if (selectedItems.contains(allFines.get(which))) {
                                    // Else, if the item is already in the array, remove it
                                    selectedItems.remove(allFines.get(which));
                                }
                            }
                        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                for (Fine fine : selectedItems) {
                    selectedPlayer.addFine(fine);
                    finesAdapter.notifyDataSetChanged();
                }
                try {
                    dataSourcePlayer.updatePlayer(selectedPlayer.getId(), selectedPlayer.getFines());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateTotalSumOfFinesView();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()

        {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateTotalSumOfFinesView() {
        if (totalSumOfFines == null) {
            totalSumOfFines = (TextView) findViewById(R.id.total_sum_of_fines);
        }
        String fineAmount = getString(R.string.fineAmount, selectedPlayer.getTotalSumOfFines());
        totalSumOfFines.setText(fineAmount);
    }

}
