package com.hermann.bussenliste;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sebas on 07.09.2017.
 */

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "bussenliste.db";
    public static final int DB_VERSION = 8;

    public static final String TABLE_PLAYERS = "players";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FINES = "fines";
    public static final String COLUMN_UPDATE_STATUS = "udpateStatus";


    public static final String SQL_CREATE_TABLE_PLAYERS =
            "CREATE TABLE " + TABLE_PLAYERS +
                    "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " + COLUMN_FINES + " TEXT, " + COLUMN_UPDATE_STATUS + " TEXT);";


    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try {
            db.execSQL(SQL_CREATE_TABLE_PLAYERS);
        } catch (Exception ex) {

        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS players");
        this.onCreate(db);
    }
}
