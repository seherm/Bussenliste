package com.hermann.bussenliste;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sebas on 07.09.2017.
 */

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "bussenliste.db";
    public static final int DB_VERSION = 3;

    public static final String TABLE_PLAYERS = "players";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FINES = "fines";
    public static final String COLUMN_UPDATE_STATUS = "updateStatus";

    public static final String TABLE_FINES = "fines";

    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DATE = "date";

    public static final String SQL_CREATE_TABLE_PLAYERS =
            "CREATE TABLE " + TABLE_PLAYERS +
                    "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " + COLUMN_FINES + " TEXT, " + COLUMN_UPDATE_STATUS + " TEXT);";

    public static final String SQL_CREATE_TABLE_FINES =
            "CREATE TABLE " + TABLE_FINES +
                    "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " + COLUMN_AMOUNT + " INTEGER, " + COLUMN_DATE + " TEXT, " + COLUMN_UPDATE_STATUS + " TEXT);";


    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try {
            db.execSQL(SQL_CREATE_TABLE_PLAYERS);
            db.execSQL(SQL_CREATE_TABLE_FINES);
        } catch (Exception ex) {

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS players");
        db.execSQL("DROP TABLE IF EXISTS fines");
        this.onCreate(db);
    }
}
