package com.hermann.bussenliste;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String PRODUCTION_SERVER_ADDRESS = "https://bussenliste.000webhostapp.com/";
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private ProgressDialog uploadingProgressDialog;
    private ProgressDialog downloadingProgressDialog;
    private PlayersAdapter playersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);

        uploadingProgressDialog = new ProgressDialog(this);
        uploadingProgressDialog.setMessage("Uploading Data to remote Server. Please wait...");
        uploadingProgressDialog.setCancelable(false);

        downloadingProgressDialog = new ProgressDialog(this);
        downloadingProgressDialog.setMessage("Downloading Data from remote Server. Please wait...");
        downloadingProgressDialog.setCancelable(false);

        if (noPlayers() || noFines()) {
            showImportDataDialog();
        }

        //initialize players view
        final GridView gridView = (GridView) findViewById(R.id.players);
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
                                dataSourcePlayer.deletePlayer(selectedItem.getId());
                                playersAdapter.refresh(dataSourcePlayer.getAllPlayers());
                                Toast.makeText(getApplicationContext(), R.string.deleted_players, Toast.LENGTH_SHORT).show();
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
                uploadDataToServer();
                downloadDataFromServer();
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
                // if EditText is empty disable closing on positive button
                String playerName = String.valueOf(editText.getText());

                if (!dataSourcePlayer.hasPlayer(playerName) && !isEmptyText) {
                    dataSourcePlayer.createPlayer(playerName);
                    playersAdapter.refresh(dataSourcePlayer.getAllPlayers());
                    Toast.makeText(getApplicationContext(), R.string.added_player, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else if (isEmptyText) {
                    Toast.makeText(getApplicationContext(), R.string.empty_player, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.already_added_player, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deletePlayerOnServer(Player player){
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("PlayersJSON",player.getName());
        client.post(PRODUCTION_SERVER_ADDRESS + "deleteplayer.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void uploadDataToServer() {
        uploadPlayersToServer();
        uploadFinesToServer();

    }

    private void uploadPlayersToServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        List<Player> playerList = dataSourcePlayer.getAllPlayers();

        if (playerList.size() != 0) {
            if (dataSourcePlayer.dbSyncCount() != 0) {
                uploadingProgressDialog.show();
                params.put("playersJSON", dataSourcePlayer.composeJSONfromSQLite());
                client.post(PRODUCTION_SERVER_ADDRESS + "insertplayer.php", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        uploadingProgressDialog.hide();
                        try {
                            JSONArray arr = new JSONArray(new String(responseBody));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = (JSONObject) arr.get(i);
                                dataSourcePlayer.updateSyncStatus(obj.get("id").toString(), obj.get("status").toString());
                            }
                            Toast.makeText(getApplicationContext(), "Players uploaded!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error occurred [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        uploadingProgressDialog.hide();
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unexpected Error occurred! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "No new Players to upload", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "No data in SQLite DB", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadFinesToServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        List<Fine> finesList = dataSourceFine.getAllFines();
        if (finesList.size() != 0) {
            if (dataSourceFine.dbSyncCount() != 0) {
                uploadingProgressDialog.show();
                params.put("finesJSON", dataSourceFine.composeJSONfromSQLite());
                client.post(PRODUCTION_SERVER_ADDRESS + "insertfine.php", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            uploadingProgressDialog.hide();
                            JSONArray arr = new JSONArray(new String(responseBody));
                            System.out.println(arr.length());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = (JSONObject) arr.get(i);
                                System.out.println(obj.get("id"));
                                System.out.println(obj.get("status"));
                                dataSourceFine.updateSyncStatus(obj.get("id").toString(), obj.get("status").toString());
                            }
                            Toast.makeText(getApplicationContext(), "Fines uploaded successfully!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            uploadingProgressDialog.hide();
                            Toast.makeText(getApplicationContext(), "Error occurred [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        uploadingProgressDialog.hide();
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unexpected Error occurred! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else {
                Toast.makeText(getApplicationContext(), "No Fines to upload!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No data in SQLite DB", Toast.LENGTH_LONG).show();
        }
    }

    // Method to Sync online MySQL DB with local SQLite DB
    public void downloadDataFromServer() {
        downloadPlayersFromServer();
        downloadFinesFromServer();
    }


    private void downloadPlayersFromServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        downloadingProgressDialog.show();
        // Make Http call to getplayers.php
        client.post(PRODUCTION_SERVER_ADDRESS + "getplayers.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // Update SQLite DB with response sent by getplayers.php
                downloadingProgressDialog.hide();
                updatePlayersSQLite(new String(responseBody));
            }

            // When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                downloadingProgressDialog.hide();
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updatePlayersSQLite(String response) {
        Gson gson = new GsonBuilder().create();
        try {
            JSONArray array = new JSONArray(response);

            if (array.length() != 0) {
                // Loop through each array element
                for (int i = 0; i < array.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) array.get(i);
                    // Insert Player into local SQLite DB
                    String playerId = obj.get("playerId").toString();
                    String playerName = obj.get("playerName").toString();
                    String playerFines = obj.get("playerFines").toString();


                    if (!dataSourcePlayer.hasPlayer(playerName)) {
                        dataSourcePlayer.createPlayer(playerName);
                        if (playerFines != null) {
                            Type type = new TypeToken<ArrayList<Fine>>() {
                            }.getType();
                            ArrayList<Fine> finesList = gson.fromJson(playerFines, type);
                            dataSourcePlayer.updatePlayer(Integer.parseInt(playerId), finesList);
                        }
                    }
                }
                // Reload the Main Activity
                this.recreate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void downloadFinesFromServer() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        downloadingProgressDialog.show();
        // Make Http call to getfines.php
        client.post(PRODUCTION_SERVER_ADDRESS + "getfines.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // Update SQLite DB with response sent by getfines.php
                downloadingProgressDialog.hide();
                updateFinesSQLite(new String(responseBody));
            }

            // When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                downloadingProgressDialog.hide();
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateFinesSQLite(String response) {
        Gson gson = new GsonBuilder().create();
        try {
            // Extract JSON array from the response
            JSONArray arr = new JSONArray(response);
            System.out.println(arr.length());
            // If no of array elements is not zero
            if (arr.length() != 0) {
                // Loop through each array element, get JSON object which has userid and username
                for (int i = 0; i < arr.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) arr.get(i);
                    // Insert Fine into SQLite DB
                    String fineId = obj.get("fineId").toString();
                    String fineDescription = obj.get("fineDescription").toString();
                    String fineAmount = obj.get("fineAmount").toString();

                    if (!dataSourceFine.hasFine(fineDescription)) {
                        dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
                    }

                }
                // Reload the Main Activity
                this.recreate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Navigation
    private void goToPlayerDetailsPage(Player selectedPlayer) {
        Intent intent = new Intent(this, PlayerDetailsActivity.class);
        intent.putExtra("SelectedPlayer", selectedPlayer);
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
