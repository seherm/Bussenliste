package com.hermann.bussenliste;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String PRODUCTION_SERVER_ADDRESS = "https://bussenliste.000webhostapp.com/";

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);
        players = dataSourcePlayer.getAllPlayers();

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
        playersAdapter = new PlayersAdapter(this, dataSourcePlayer.getAllPlayers());
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
                                //Delete player in SQLite DB
                                dataSourcePlayer.deletePlayer(selectedItem.getId());
                                playersAdapter.refresh(dataSourcePlayer.getAllPlayers());
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
            case R.id.action_sync:
                syncDataWithServer();
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

    private int getTotalFinesSum(){
        int sum = 0;
        for(Player player : players){
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
        final EditText editText = new EditText(this);
        editText.setHint(R.string.name);
        builder.setView(editText);
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
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        Gson gson = new GsonBuilder().create();
        params.put("playersJSON", gson.toJson(player.getName()));
        client.post(PRODUCTION_SERVER_ADDRESS + "deleteplayer.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), R.string.deleted_players, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    //Method to first upload table entries from local SQLite DB to online MySQL DB if necessery and download entries afterwards
    private void syncDataWithServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        List<Player> playerList = dataSourcePlayer.getAllPlayers();
        List<Fine> finesList = dataSourceFine.getAllFines();

        if (playerList.isEmpty() && finesList.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.no_data_in_sqlite_db, Toast.LENGTH_LONG).show();
            downloadDataFromServer();
        }
        if (dataSourcePlayer.dbSyncCount() == 0 && dataSourceFine.dbSyncCount() == 0) {
            Toast.makeText(getApplicationContext(), R.string.no_data_to_upload, Toast.LENGTH_LONG).show();
            downloadDataFromServer();
        }
        if (dataSourcePlayer.dbSyncCount() != 0) {
            params.put("playersJSON", dataSourcePlayer.composeJSONfromSQLite());
        }
        if (dataSourceFine.dbSyncCount() != 0) {
            params.put("finesJSON", dataSourceFine.composeJSONfromSQLite());
        }

        if (params.has("playersJSON") || params.has("finesJSON")) {
            uploadingProgressDialog.show();
            client.post(PRODUCTION_SERVER_ADDRESS + "insertdata.php", params, new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseBody) {
                    uploadingProgressDialog.hide();
                    try {
                        JSONArray arr = new JSONArray(responseBody);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = (JSONObject) arr.get(i);
                            if (obj.get("table").toString().equals("players")) {
                                dataSourcePlayer.updateSyncStatus(obj.get("name").toString(), obj.get("status").toString());
                            } else if (obj.get("table").toString().equals("fines")) {
                                dataSourceFine.updateSyncStatus(obj.get("description").toString(), obj.get("status").toString());
                            }
                        }
                        Toast.makeText(getApplicationContext(), R.string.data_successfully_uploaded, Toast.LENGTH_LONG).show();
                        downloadDataFromServer();
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), R.string.json_error, Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable error) {
                    uploadingProgressDialog.hide();
                    if (statusCode == 404) {
                        Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
                    } else if (statusCode == 500) {
                        Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    //Method to download table entries from online MySQL DB and load it into local SQLite DB
    private void downloadDataFromServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        downloadingProgressDialog.show();
        // Make Http call to getdata.php
        client.post(PRODUCTION_SERVER_ADDRESS + "getdata.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // Update SQLite DB with response sent by getplayers.php
                downloadingProgressDialog.hide();
                updateSQLiteData(new String(responseBody));
                Toast.makeText(getApplicationContext(), R.string.data_downloaded, Toast.LENGTH_LONG).show();
            }

            //When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                downloadingProgressDialog.hide();
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateSQLiteData(String response) {
        try {
            JSONArray array = new JSONArray(response);
            if (array.length() != 0) {
                // Loop through each array element
                for (int i = 0; i < array.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) array.get(i);
                    String tableName = obj.get("table").toString();

                    switch (tableName) {
                        case "players":
                            // Insert player into local SQLite DB
                            String playerId = obj.get("playerId").toString();
                            String playerName = obj.get("playerName").toString();
                            String playerFines = obj.get("playerFines").toString().trim();
                            byte[] playerPhoto = Base64.decode(obj.get("playerPhoto").toString(),Base64.DEFAULT);

                            if (!dataSourcePlayer.hasPlayer(playerName)) {
                                Player player = new Player(playerName);
                                if (playerFines != null) {
                                   player.setFines(DataSourcePlayer.getFinesList(playerFines));
                                }

                                player.setPhoto(DataSourcePlayer.getImage(playerPhoto));

                                dataSourcePlayer.createPlayer(player);
                            }else{
                                Player currentPlayer = dataSourcePlayer.getPlayer(playerName);
                                if(playerFines != null){
                                    currentPlayer.setFines(DataSourcePlayer.getFinesList(playerFines));
                                }
                                currentPlayer.setPhoto(DataSourcePlayer.getImage(playerPhoto));
                                dataSourcePlayer.updatePlayer(currentPlayer);
                            }
                            break;
                        case "fines":
                            // Insert fine into local SQLite DB
                            String fineId = obj.get("fineId").toString();
                            String fineDescription = obj.get("fineDescription").toString();
                            String fineAmount = obj.get("fineAmount").toString();

                            if (!dataSourceFine.hasFine(fineDescription)) {
                                dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
                            }
                            break;
                    }
                }
                //Refresh UI
                players = dataSourcePlayer.getAllPlayers();
                playersAdapter.refresh(players);
                totalFinesSumTextView.setText(getString(R.string.fineAmount, getTotalFinesSum()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Navigation
    private void goToPlayerDetailsPage(Player selectedPlayer) {
        Intent intent = new Intent(this, PlayerDetailsActivity.class);
        intent.putExtra("SelectedPlayerName", selectedPlayer.getName());
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
