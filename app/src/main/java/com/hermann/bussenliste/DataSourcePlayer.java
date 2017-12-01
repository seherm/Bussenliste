package com.hermann.bussenliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataSourcePlayer {

    private final DatabaseHelper dbHelper;

    private final String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_NAME,
            DatabaseHelper.COLUMN_FINES,
            DatabaseHelper.COLUMN_PHOTO,
            DatabaseHelper.COLUMN_UPDATE_STATUS
    };

    public DataSourcePlayer(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long createPlayer(Player player) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, player.getName());
        values.put(DatabaseHelper.COLUMN_UPDATE_STATUS, "no");

        if (player.getFines() != null) {
            if (!player.getFines().isEmpty()) {
                String fines = getFinesJSON(player.getFines());
                values.put(DatabaseHelper.COLUMN_FINES, fines);
            }
        }

        if (player.getPhoto() != null) {
            byte[] photo = getBytes(player.getPhoto());
            values.put(DatabaseHelper.COLUMN_PHOTO, photo);
        }

        long insertId = database.insert(DatabaseHelper.TABLE_PLAYERS, null, values);

        return insertId;
    }

    public Player getPlayer(String playerName) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_PLAYERS + " WHERE "
                + DatabaseHelper.COLUMN_NAME + " = " + "'" + playerName + "'";

        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
        int idName = cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME);
        int idFines = cursor.getColumnIndex(DatabaseHelper.COLUMN_FINES);
        int idPhoto = cursor.getColumnIndex(DatabaseHelper.COLUMN_PHOTO);

        long id = cursor.getLong(idIndex);
        String name = cursor.getString(idName);
        String fines = cursor.getString(idFines);
        byte[] photo = cursor.getBlob(idPhoto);

        Player player = new Player(id, name);

        //Set fines
        if (fines != null) {
            player.setFines(getFinesList(fines));
        }

        //Set photo
        if (photo != null) {
            player.setPhoto(getImage(photo));
        }

        return player;
    }

    public int updatePlayer(Player player) throws JSONException {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        long id = player.getId();

        ContentValues values = new ContentValues();

        if (player.getFines() != null) {
            if (!player.getFines().isEmpty()) {
                String fines = getFinesJSON(player.getFines());
                values.put(DatabaseHelper.COLUMN_FINES, fines);
            }
        }

        if (player.getPhoto() != null) {
            byte[] photo = getBytes(player.getPhoto());
            values.put(DatabaseHelper.COLUMN_PHOTO, photo);
        }

        values.put(DatabaseHelper.COLUMN_UPDATE_STATUS, "no");

        return database.update(DatabaseHelper.TABLE_PLAYERS,
                values,
                DatabaseHelper.COLUMN_ID + "=" + id,
                null);
    }


    public List<Player> getAllPlayers() {
        List<Player> playersList = new ArrayList<>();

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = database.query(DatabaseHelper.TABLE_PLAYERS,
                columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int idName = cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME);
                int idFines = cursor.getColumnIndex(DatabaseHelper.COLUMN_FINES);
                int idPhoto = cursor.getColumnIndex(DatabaseHelper.COLUMN_PHOTO);

                long id = cursor.getLong(idIndex);
                String name = cursor.getString(idName);
                String fines = cursor.getString(idFines);
                byte[] photo = cursor.getBlob(idPhoto);

                Player player = new Player(id, name);

                //Set fines
                if (fines != null) {
                    player.setFines(getFinesList(fines));
                }

                //Set photo
                if (photo != null) {
                    player.setPhoto(getImage(photo));
                }

                playersList.add(player);
            } while (cursor.moveToNext());
        }
        return playersList;
    }

    public long deletePlayer(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_PLAYERS, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{Long.toString(id)});
    }

    public String composeJSONfromSQLite() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT  * FROM  players WHERE updateStatus = '" + "no" + "'";
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
        Gson gson = new GsonBuilder().create();
        return gson.toJson(wordList);
    }

    public int dbSyncCount() {
        int count;
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT  * FROM players WHERE updateStatus = '" + "no" + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        return count;
    }

    public void updateSyncStatus(String name, String status) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String updateQuery = "Update players SET updateStatus = '" + status + "' WHERE name=" + "'" + name + "'";
        database.execSQL(updateQuery);
    }

    public boolean hasPlayer(String name) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String Query = "SELECT * FROM players WHERE name =" + "'" + name + "'";
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }


    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public static ArrayList<Fine> getFinesList(String finesJSON) {
        Type type = new TypeToken<ArrayList<Fine>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<Fine> finesList = gson.fromJson(finesJSON, type);
        return finesList;
    }

    public static String getFinesJSON(ArrayList<Fine> fines) {
        Gson gson = new Gson();
        return gson.toJson(fines);
    }
}
