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

    public long createPlayer(String name) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, name);
        values.put(DatabaseHelper.COLUMN_UPDATE_STATUS, "no");

        long insertId = database.insert(DatabaseHelper.TABLE_PLAYERS, null, values);

        return insertId;
    }

    public Player getPlayer(long player_id) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_PLAYERS + " WHERE "
                + DatabaseHelper.COLUMN_ID + " = " + player_id;

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

        //Set Fines
        Type type = new TypeToken<ArrayList<Fine>>() {
        }.getType();
        Gson gson = new Gson();
        ArrayList<Fine> finesList = gson.fromJson(fines, type);
        if (finesList != null) {
            player.setFines(finesList);
        }

        //Set photo
        if(photo != null){
            player.setPhoto(getImage(photo));
        }

        return player;
    }

    public int updatePlayer(Player player) throws JSONException {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        Gson gson = new Gson();
        String fines = gson.toJson(player.getFines());
        byte[] photo = getBytes(player.getPhoto());
        long id = player.getId();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_FINES, fines);
        values.put(DatabaseHelper.COLUMN_PHOTO, photo);
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
                Type type = new TypeToken<ArrayList<Fine>>() {
                }.getType();
                Gson gson = new Gson();
                ArrayList<Fine> finesList = gson.fromJson(fines, type);
                if (finesList != null) {
                    player.setFines(finesList);
                }

                //Set photo
                if(photo != null){
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

    public void updateSyncStatus(String id, String status) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String updateQuery = "Update players SET updateStatus = '" + status + "' WHERE _id=" + "'" + id + "'";
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
        if(bitmap != null){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            return stream.toByteArray();
        }else {
            return null;
        }
    }

    // convert from byte array to bitmap
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }
}
