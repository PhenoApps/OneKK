package org.wheatgenetics.database;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "onekkdb";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sample (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, photo TEXT, person TEXT, date TEXT, seed_count TEXT, weight TEXT, " +
                "length_avg TEXT, length_var TEXT, length_cv TEXT, width_avg TEXT, width_var TEXT, width_cv TEXT, area_avg TEXT, area_var TEXT, area_cv TEXT)");
        db.execSQL("CREATE TABLE seed (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, length TEXT, width TEXT, circularity TEXT, area TEXT, color TEXT, weight TEXT )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS sample");
        db.execSQL("DROP TABLE IF EXISTS seed");

        // create fresh tables
        this.onCreate(db);
    }

    // Table names
    private static final String TABLE_SAMPLE = "sample";
    private static final String TABLE_SEED = "seed";

    // Sample table columns names
    private static final String SAMPLE_SID = "sample_id";
    private static final String SAMPLE_PHOTO = "photo";
    private static final String SAMPLE_PERSON = "person";
    private static final String SAMPLE_TIME = "date";
    private static final String SAMPLE_NUMSEEDS = "seed_count";
    private static final String SAMPLE_WT = "weight";
    private static final String SAMPLE_LENGTHAVG = "length_avg";
    private static final String SAMPLE_LENGTHVAR = "length_var";
    private static final String SAMPLE_LENGTHCV = "length_cv";
    private static final String SAMPLE_WIDTHAVG = "width_avg";
    private static final String SAMPLE_WIDTHVAR = "width_var";
    private static final String SAMPLE_WIDTHCV = "width_cv";
    private static final String SAMPLE_AREAAVG = "area_avg";
    private static final String SAMPLE_AREAVAR = "area_var";
    private static final String SAMPLE_AREACV = "area_cv";

    // Sample table columns names
    private static final String SEED_SID = "sample_id";
    private static final String SEED_LEN = "length";
    private static final String SEED_WID = "width";
    private static final String SEED_CIRC = "circularity";
    private static final String SEED_AREA = "area";
    private static final String SEED_COL = "color";
    private static final String SEED_WT = "weight";

    public void addSampleRecord(SampleRecord sample) {
        Log.d("Add Sample: ", sample.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(SAMPLE_SID, sample.getSampleId());
        values.put(SAMPLE_PHOTO, sample.getPhoto());
        values.put(SAMPLE_PERSON, sample.getPersonId());
        values.put(SAMPLE_TIME, sample.getDate());
        values.put(SAMPLE_NUMSEEDS, sample.getSeedCount());
        values.put(SAMPLE_WT, sample.getWeight());
        values.put(SAMPLE_LENGTHAVG, sample.getLengthAvg());
        values.put(SAMPLE_LENGTHVAR, sample.getLengthVar());
        values.put(SAMPLE_LENGTHCV, sample.getLengthCV());
        values.put(SAMPLE_WIDTHAVG, sample.getWidthAvg());
        values.put(SAMPLE_WIDTHVAR, sample.getWidthVar());
        values.put(SAMPLE_WIDTHCV, sample.getWidthCV());
        values.put(SAMPLE_AREAAVG, sample.getAreaAvg());
        values.put(SAMPLE_AREAVAR, sample.getAreaVar());
        values.put(SAMPLE_AREACV, sample.getAreaCV());

        // 3. insert
        db.insert(TABLE_SAMPLE, null, values);

        // 4. close
        db.close();
    }

    public void addSeedRecord(SeedRecord seed) {
        // for logging
        Log.d("Add Seed: ", seed.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(SEED_SID, seed.getSampleId());
        values.put(SEED_LEN, seed.getLength());
        values.put(SEED_WID, seed.getWidth());
        values.put(SEED_CIRC, seed.getCircularity());
        values.put(SEED_AREA, seed.getArea());
        values.put(SEED_COL, seed.getColor());
        values.put(SEED_WT, seed.getWeight());

        // 3. insert
        db.insert(TABLE_SEED, null, values);

        // 4. close
        db.close();
    }

    // average seeds in a sample
    public double averageSample(String sampleName, String trait) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(" + trait
                + ") FROM (SELECT seed." + trait
                + ", seed.sample_id FROM seed WHERE seed.sample_id = \""
                + sampleName + "\")", null);

        double traitValue = 0;

        if (cursor != null) {
            cursor.moveToFirst();
            traitValue = cursor.getDouble(0);
            cursor.close();
        }
        return traitValue;
    }

    // sd of seeds in a sample TODO
    public double sdSample(String sampleName, String trait) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursorMean = db.rawQuery("SELECT AVG(" + trait
                + ") FROM (SELECT seed." + trait
                + ", seed.sample_id FROM seed WHERE seed.sample_id = \""
                + sampleName + "\")", null);

        Cursor cursor = db.rawQuery("SELECT (" + trait
                + ") FROM (SELECT seed." + trait
                + ", seed.sample_id FROM seed WHERE seed.sample_id = \""
                + sampleName + "\")", null);

        double traitMean = 1;

        if (cursorMean != null) {
            cursorMean.moveToFirst();
            traitMean = cursorMean.getDouble(0);
        }

        int size = cursor.getCount();
        double variance = 0;
        double temp = 0;

        if (cursor.moveToFirst()){
            do {
                double data = cursor.getDouble(0);
                temp += (traitMean - data)*(traitMean-data);
            } while(cursor.moveToNext());
        }

        variance = temp/(size-1);
        return Math.sqrt(variance);
    }

    // Export summary statistics
    public Cursor exportSummaryData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db
                .rawQuery(
                        "SELECT sample_id, photo, person, date, seed_count, weight, length_avg, length_var, length_cv, " +
                                "width_avg, width_var, width_cv, area_avg, area_var, area_cv FROM sample",
                        null);
        return cursor;
    }

    // Export raw data
    public Cursor exportRawData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db
                .rawQuery(
                        "SELECT seed.sample_id, photo, person, date, length, width, circularity, seed.weight, area, " +
                                "color, sample.weight, length_avg, width_avg, area_avg, seed_count FROM seed, sample " +
                                "WHERE seed.sample_id = sample.sample_id",
                        null);
        return cursor;
    }

    public List<SampleRecord> getLastSample() {
        List<SampleRecord> samples = new LinkedList<>();

        // 1. build the query
        String query = "SELECT * FROM " + TABLE_SAMPLE + " ORDER BY id desc limit 1";

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build sample and add it to list
        SampleRecord sample;

        if (cursor.moveToFirst()) {
            do {
                sample = new SampleRecord();

                sample.setSampleId(cursor.getString(1));
                sample.setPhoto(cursor.getString(2));
                sample.setPersonId(cursor.getString(3));
                sample.setDate(cursor.getString(4));
                sample.setWeight(cursor.getString(5));
                sample.setSeedCount(cursor.getString(6));
                sample.setLengthAvg(Double.parseDouble(cursor.getString(7)));
                sample.setWidthAvg(Double.parseDouble(cursor.getString(10)));
                samples.add(sample);
            } while (cursor.moveToNext());
        }
        Log.d("getLastSample()", samples.toString());
        cursor.close();
        // return samples
        return samples;
    }

    public List<SampleRecord> getAllSamples() {
        List<SampleRecord> samples = new LinkedList<>();

        // 1. build the query
        String query = "SELECT * FROM " + TABLE_SAMPLE;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build sample and add it to list
        SampleRecord sample;

        if (cursor.moveToFirst()) {
            do {
                sample = new SampleRecord();

                sample.setSampleId(cursor.getString(1));
                sample.setPhoto(cursor.getString(2));
                sample.setPersonId(cursor.getString(3));
                sample.setDate(cursor.getString(4));
                sample.setWeight(cursor.getString(5));
                sample.setSeedCount(cursor.getString(6));
                sample.setLengthAvg(Double.parseDouble(cursor.getString(7)));
                sample.setWidthAvg(Double.parseDouble(cursor.getString(10)));
                samples.add(sample);
            } while (cursor.moveToNext());
        }
        Log.d("getAllSamples()", samples.toString());
        cursor.close();
        // return samples
        return samples;
    }

    public void deleteSample(String sample) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("Delete sample: ", sample);

        try {
            db.execSQL("DELETE FROM sample WHERE sample_id = \"" + sample + "\"");
            db.execSQL("DELETE FROM seed WHERE sample_id = \"" + sample + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SAMPLE, null, null);
        db.delete(TABLE_SEED, null, null);
        db.close();
    }
}