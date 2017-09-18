package com.hermann.bussenliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by sebas on 07.09.2017.
 */

public class DataSource {

    private SQLiteDatabase database;
    private DbHelper dbHelper;

    private String[] columnsPlayers = {
            DbHelper.COLUMN_ID,
            DbHelper.COLUMN_NAME,
            DbHelper.COLUMN_FINES,
            DbHelper.COLUMN_UPDATE_STATUS
    };

    private String[] columnsFines = {
            DbHelper.COLUMN_ID,
            DbHelper.COLUMN_DESCRIPTION,
            DbHelper.COLUMN_AMOUNT,
            DbHelper.COLUMN_DATE,
            DbHelper.COLUMN_UPDATE_STATUS
    };

    public DataSource(Context context) {
        dbHelper = new DbHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Player createPlayer(String name) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_NAME, name);
        values.put(DbHelper.COLUMN_UPDATE_STATUS, "no");

        long insertId = database.insert(DbHelper.TABLE_PLAYERS, null, values);

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columnsPlayers, DbHelper.COLUMN_ID + "=" + insertId,
                null, null, null, null);

        cursor.moveToFirst();
        Player player = cursorToPlayer(cursor);
        cursor.close();

        return player;
    }

    public Player updatePlayer(long id, List<Fine> newFairs) throws JSONException {

        Gson gson = new Gson();
        String inputString = gson.toJson(newFairs);

        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_FINES, inputString);
        values.put(DbHelper.COLUMN_UPDATE_STATUS, "no");

        database.update(DbHelper.TABLE_PLAYERS,
                values,
                DbHelper.COLUMN_ID + "=" + id,
                null);

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columnsPlayers, DbHelper.COLUMN_ID + "=" + id,
                null, null, null, null);

        cursor.moveToFirst();
        Player player = cursorToPlayer(cursor);
        cursor.close();

        return player;
    }

    private Player cursorToPlayer(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(DbHelper.COLUMN_ID);
        int idName = cursor.getColumnIndex(DbHelper.COLUMN_NAME);
        int idFines = cursor.getColumnIndex(DbHelper.COLUMN_FINES);

        long id = cursor.getLong(idIndex);
        String name = cursor.getString(idName);
        String fines = cursor.getString(idFines);

        Player player = new Player(id, name);

        Type type = new TypeToken<ArrayList<Fine>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<Fine> finesList = gson.fromJson(fines, type);
        if (finesList != null) {
            player.setFines(finesList);
        }

        return player;
    }

    public List<Player> getAllPlayers() {
        List<Player> playersList = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columnsPlayers, null, null, null, null, null);

        cursor.moveToFirst();
        Player player;

        while (!cursor.isAfterLast()) {
            player = cursorToPlayer(cursor);
            playersList.add(player);
            cursor.moveToNext();
        }

        cursor.close();

        return playersList;
    }


    public Fine createFine(String description, int amount) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_DESCRIPTION, description);
        values.put(DbHelper.COLUMN_AMOUNT, amount);
        values.put(DbHelper.COLUMN_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN).format(new Date()));
        values.put(DbHelper.COLUMN_UPDATE_STATUS, "no");

        long insertId = database.insert(DbHelper.TABLE_FINES, null, values);

        Cursor cursor = database.query(DbHelper.TABLE_FINES,
                columnsFines, DbHelper.COLUMN_ID + "=" + insertId,
                null, null, null, null);

        cursor.moveToFirst();
        Fine fine = cursorToFine(cursor);
        cursor.close();

        return fine;
    }

    private Fine cursorToFine(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(DbHelper.COLUMN_ID);
        int idDescription = cursor.getColumnIndex(DbHelper.COLUMN_DESCRIPTION);
        int idAmount = cursor.getColumnIndex(DbHelper.COLUMN_AMOUNT);
        int idDate = cursor.getColumnIndex(DbHelper.COLUMN_DATE);

        long id = cursor.getLong(idIndex);
        String description = cursor.getString(idDescription);
        int amount = cursor.getInt(idAmount);
        String date = cursor.getString(idDate);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        Date resultDate = null;
        try {
            resultDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Fine fine = new Fine(id, description, amount, resultDate);

        return fine;
    }

    public List<Fine> getAllFines() {
        List<Fine> finesList = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.TABLE_FINES,
                columnsFines, null, null, null, null, null);

        cursor.moveToFirst();
        Fine fine;

        while (!cursor.isAfterLast()) {
            fine = cursorToFine(cursor);
            finesList.add(fine);
            cursor.moveToNext();
        }

        cursor.close();

        return finesList;
    }


    /**
     * Compose JSON out of SQLite records
     *
     * @return
     */
    public String composeJSONfromSQLite() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM players where updateStatus = '" + "no" + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("playerId", cursor.getString(0));
                map.put("playerName", cursor.getString(1));
                map.put("playerFines", cursor.getString(2));
                wordList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        Gson gson = new GsonBuilder().create();
        return gson.toJson(wordList);
    }

    /**
     * Get Sync status of SQLite
     *
     * @return
     */
    public String getSyncStatus() {
        String msg = null;
        if (this.dbSyncCount() == 0) {
            msg = "SQLite and Remote MySQL DBs are in Sync!";
        } else {
            msg = "DB Sync needed";
        }
        return msg;
    }

    /**
     * Get SQLite records that are yet to be Synced
     *
     * @return
     */
    public int dbSyncCount() {
        int count = 0;
        String selectQuery = "SELECT  * FROM players where updateStatus = '" + "no" + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Update Sync status against each Player ID
     *
     * @param id
     * @param status
     */
    public void updateSyncStatus(String id, String status) {
        open();
        String updateQuery = "Update players set updateStatus = '" + status + "' where _id=" + "'" + id + "'";
        Log.d("query", updateQuery);
        database.execSQL(updateQuery);
        database.close();
    }
}
