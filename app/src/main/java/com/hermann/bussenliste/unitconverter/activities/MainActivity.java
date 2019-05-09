package com.hermann.bussenliste.unitconverter.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.hermann.bussenliste.repository.DataSourceFine;
import com.hermann.bussenliste.repository.DataSourcePlayer;
import com.hermann.bussenliste.common.OnServerTaskListener;
import com.hermann.bussenliste.unitconverter.adapters.PlayersAdapter;
import com.hermann.bussenliste.R;
import com.hermann.bussenliste.common.ServerTask;
import com.hermann.bussenliste.domain.Fine;
import com.hermann.bussenliste.domain.Player;

import org.json.JSONException;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnServerTaskListener {

    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private ProgressDialog uploadingProgressDialog;
    private ProgressDialog downloadingProgressDialog;
    private TextView totalFinesSumTextView;
    private PlayersAdapter playersAdapter;
    private List<Player> players;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);
        players = dataSourcePlayer.getAllPlayers();
        Collections.sort(players);

        uploadingProgressDialog = new ProgressDialog(this);
        uploadingProgressDialog.setTitle(getString(R.string.uploading_data_to_remote_server));
        uploadingProgressDialog.setMessage(getString(R.string.please_wait));
        uploadingProgressDialog.setCancelable(false);

        downloadingProgressDialog = new ProgressDialog(this);
        downloadingProgressDialog.setTitle(getString(R.string.downloading_data_from_remote_server));
        downloadingProgressDialog.setMessage(getString(R.string.please_wait));
        downloadingProgressDialog.setCancelable(false);

        if (noPlayers() || noFines()) {
            showImportDataDialog();
        }

        totalFinesSumTextView = findViewById(R.id.totalFinesSum);
        totalFinesSumTextView.setText(getString(R.string.fineAmount, getTotalFinesSum()));

        //initialize players view
        final GridView gridView = findViewById(R.id.players);
        playersAdapter = new PlayersAdapter(this, players);
        gridView.setAdapter(playersAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Player selectedPlayer = playersAdapter.getItem(i);
                goToPlayerDetailsPage(selectedPlayer);
            }
        });
        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        gridView.setMultiChoiceModeListener(new GridView.MultiChoiceModeListener() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.delete_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete_mode:
                        SparseBooleanArray selected = playersAdapter.getSelectedIds();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Player selectedItem = playersAdapter.getItem(selected.keyAt(i));
                                //Delete player in remote MySQL DB
                                deletePlayerOnServer(selectedItem);
                            }
                        }
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int checkedCount = gridView.getCheckedItemCount();
                mode.setTitle(Integer.toString(checkedCount));
                playersAdapter.toggleSelection(position);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                goToSettingsPage();
                return true;
            case R.id.action_create_player:
                showCreateNewPlayerDialog();
                return true;
            case R.id.action_edit_fines:
                goToEditFinesPage();
                return true;
            case R.id.action_sync:
                uploadDataToServer();
                return true;
            case R.id.action_import:
                goToImportDataPage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean noPlayers() {
        return dataSourcePlayer.getAllPlayers().isEmpty();
    }

    private boolean noFines() {
        return dataSourceFine.getAllFines().isEmpty();
    }

    private int getTotalFinesSum() {
        int sum = 0;
        for (Player player : players) {
            sum += player.getTotalSumOfFines();
        }
        return sum;
    }

    private void showImportDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_players_available);
        builder.setMessage(R.string.import_players_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                downloadDataFromServer();
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


    private void showCreateNewPlayerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_create_player);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog_new_player, null);
        builder.setView(promptView);
        final EditText editText = promptView.findViewById(R.id.create_player);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isEmptyText = (editText.getText().toString().trim().isEmpty());
                String playerName = String.valueOf(editText.getText());

                if (!dataSourcePlayer.hasPlayer(playerName) && !isEmptyText) {
                    Player player = new Player(playerName);
                    dataSourcePlayer.createPlayer(player);
                    playersAdapter.refresh(dataSourcePlayer.getAllPlayers());
                    Toast.makeText(getApplicationContext(), R.string.added_player, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else if (isEmptyText) {
                    Toast.makeText(getApplicationContext(), R.string.empty_player, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.already_added_player, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void deletePlayerOnServer(Player player) {
        ServerTask serverTask = new ServerTask(this, this);
        serverTask.deletePlayer(player);
    }

    private void downloadDataFromServer() {
        ServerTask serverTask = new ServerTask(this, this);
        downloadingProgressDialog.show();
        serverTask.getData();
    }

    private void uploadDataToServer() {
        ServerTask serverTask = new ServerTask(this, this);
        uploadingProgressDialog.show();
        serverTask.putData();
    }


    @Override
    public void deletePlayerTaskCompleted(Player player) {
        //Delete player in SQLite DB
        dataSourcePlayer.deletePlayer(player.getId());
        //Update UI
        playersAdapter.refresh(dataSourcePlayer.getAllPlayers());
        Toast.makeText(this, R.string.deleted_players, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deletePlayerTaskFailed(int statusCode) {
        Toast.makeText(this, R.string.error_player_deletion, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deleteFineTaskCompleted(Fine fine) {

    }

    @Override
    public void deleteFineTaskFailed(int statusCode) {

    }

    @Override
    public void uploadTaskCompleted(String response) {
        uploadingProgressDialog.hide();
        Toast.makeText(this, R.string.data_successfully_uploaded, Toast.LENGTH_LONG).show();
        downloadDataFromServer();
    }

    @Override
    public void uploadTaskFailed(int statusCode) {
        uploadingProgressDialog.hide();
        if (statusCode == 404) {
            Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
        } else if (statusCode == 500) {
            Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void updateSQLiteDataFailed(JSONException e) {
        Toast.makeText(this, R.string.json_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void downloadTaskCompleted(String response) {
        downloadingProgressDialog.hide();
        Toast.makeText(getApplicationContext(), R.string.data_downloaded, Toast.LENGTH_LONG).show();
        //Refresh UI
        players = dataSourcePlayer.getAllPlayers();
        playersAdapter.refresh(players);
        totalFinesSumTextView.setText(getString(R.string.fineAmount, getTotalFinesSum()));
    }

    @Override
    public void downloadTaskFailed(int statusCode) {
        downloadingProgressDialog.hide();
        if (statusCode == 404) {
            Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
        } else if (statusCode == 500) {
            Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void updateSyncStatusFailed(JSONException e) {
        Toast.makeText(getApplicationContext(), R.string.json_error, Toast.LENGTH_LONG).show();
    }


    //Navigation
    private void goToPlayerDetailsPage(Player selectedPlayer) {
        Intent intent = new Intent(this, PlayerDetailsActivity.class);
        intent.putExtra("SelectedPlayerName", selectedPlayer.getName());
        startActivity(intent);
    }

    private void goToEditFinesPage() {
        Intent intent = new Intent(this, FineListActivity.class);
        startActivity(intent);
    }

    private void goToImportDataPage() {
        Intent intent = new Intent(this, ImportDataActivity.class);
        startActivity(intent);
    }

    private void goToSettingsPage() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
