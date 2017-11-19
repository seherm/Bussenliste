package com.hermann.bussenliste;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
                        Toast.makeText(getApplicationContext(),R.string.deleted_fines,Toast.LENGTH_LONG).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_create_fine:
                showCreateNewFineDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCreateNewFineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText descriptionBox = new EditText(this);
        descriptionBox.setHint(R.string.fineDescription);
        layout.addView(descriptionBox);
        final EditText amountBox = new EditText(this);
        amountBox.setHint(R.string.fineAmountText);
        amountBox.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(amountBox);
        builder.setTitle(R.string.action_create_fine);
        builder.setView(layout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String fineDescription = String.valueOf(descriptionBox.getText());
                String fineAmount = String.valueOf(amountBox.getText());
                dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }


    private void showFineSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        selectedItems = new ArrayList<>();
        final List<Fine> allFines = dataSourceFine.getAllFines();
        String[] namesStringArray = new String[allFines.size()];
        for (int i = 0; i < allFines.size(); i++) {
            namesStringArray[i] = allFines.get(i).getDescription() + " (" + allFines.get(i).getAmount() + ".-)";
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
