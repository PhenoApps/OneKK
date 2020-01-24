package org.wheatgenetics.database;

/**
 * Created by sid on 1/23/18.
 */

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.wheatgenetics.imageprocess.DetectSeeds.Seed;
import org.wheatgenetics.imageprocess.RawSeed;
import org.wheatgenetics.utils.Constants;
import org.wheatgenetics.onekk.R;
import org.wheatgenetics.utils.CSVWriter;
import org.wheatgenetics.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
import static org.wheatgenetics.utils.Utils.adjustFontSize;
import static org.wheatgenetics.utils.Utils.getDate;
import static org.wheatgenetics.utils.Utils.makeFileDiscoverable;
import static org.wheatgenetics.utils.Utils.stringDecimal;

//TODO : REDO this class

public class Data {

    private static Context context;
    //handler of database
    private static MySQLiteHelper db;
    private static int currentItemNum = 1;
    private static TableLayout OneKKTable;
    private static String path;

    public Data(Context context, TableLayout tableLayout) {
        this.context = context;
        db = new MySQLiteHelper(context);
        OneKKTable = tableLayout;
    }

    public Data(Context context) {
        this.context = context;
        db = new MySQLiteHelper(context);
    }

    public void getLastData() {
        OneKKTable.removeAllViews();
        List<SampleRecord> list = db.getLastSample();
        db.close();
        parseListToLastSampleTable(list);
    }

    /**
     * Single method to get data from different tables in the Database
     *
     * <p>
     * {@link Data#getAllData(String)}
     * {@value "sample, seed, coins"}
     * </p>
     *
     * @param tableName takes the table name from which all the rows are fetched
     */
    public static void getAllData(String tableName) {

        OneKKTable.removeAllViews();
        switch (tableName) {
            case "sample":
            case "seed":
                parseListToTable(db.getAllSamples());
                break;
            case "coins":
                /* null as parameters means that all the columns are selected and no where clauses
                are used */
                parseCoinListToTable(db.getFromCoins(null, null, null, false));
                break;
        }
        db.close();

    }

    // FIXME: 1/23/18
    //Add data to table view
    private static void parseCoinListToTable(List<?> list) {

        int itemCount = list.size();

        for (int i = 0; i < itemCount; i++) {
            //String[] temp = list.get(i).toString().split(",");
            CoinRecord coin = (CoinRecord) list.get(i);

            createNewTableEntry(coin);
        }
    }

    // FIXME: 1/23/18
    //Add data to table view
    private static void parseListToTable(List<?> list) {

        int itemCount = list.size();

        for (int i = 0; i < itemCount; i++) {

            String[] temp = list.get(i).toString().split(",");
            SampleRecord r = (SampleRecord) list.get(i);

            /*if (temp.length == 7) {
                createNewTableEntry(temp[0], temp[6], stringDecimal(temp[5]));
            } else {*/
                //createNewTableEntry(temp[0], temp[5], stringDecimal(temp[7]), stringDecimal(temp[8]), stringDecimal(temp[6]));
            createNewTableEntry(r.getSampleId(), r.getSeedCount(), stringDecimal(r.getLengthAvg().toString()),
                    stringDecimal(r.getWidthAvg().toString()), stringDecimal(r.getWeight()));
            //}
        }
    }

    //add data to table view
    private static void parseListToLastSampleTable(List<?> list) {
        int itemCount = list.size();
        if (itemCount > 0) {
            String[] temp = list.get(0).toString().split(",");
            createNewTableEntry(temp[0], temp[4]);
        }
    }

    private static void createNewTableEntry(CoinRecord coin) { //String country, final String coinName, String diameter) {
        //inputText.setText("");

        String country = coin.getCountry();
        String coinName = coin.getName();
        String diameter = coin.getDiameter();

        /* Create a new row to be added. */
        TableRow tr = new TableRow(context);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tr.setPadding(0, 30, 0, 30);
        tr.setTag(country + "~" + coinName + "~" + diameter);
        float fontSize = 20.0f; //min(adjustFontSize(country),adjustFontSize(currency));
        /* Create the country field */
        TextView tvCountry = new TextView(context);
        tvCountry.setGravity(Gravity.START | Gravity.BOTTOM);
        tvCountry.setTextColor(Color.BLACK);
        tvCountry.setTextSize(fontSize);
        tvCountry.setText(country);
        tvCountry.setTag(country);
        tvCountry.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

        /* Create the currency field */
        TextView tvCurrency = new TextView(context);
        tvCurrency.setGravity(Gravity.START | Gravity.BOTTOM);
        tvCurrency.setTextColor(Color.BLACK);
        tvCurrency.setTextSize(fontSize);
        tvCurrency.setText(coinName);
        tvCountry.setTag(coinName);
        tvCurrency.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

        /* Create the diameter field */
        TextView tvDiameter = new TextView(context);
        tvDiameter.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        tvDiameter.setTextColor(Color.BLACK);
        tvDiameter.setTextSize(fontSize);
        tvDiameter.setText(diameter);
        tvDiameter.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.14f));

        tr.addView(tvCountry);
        tr.addView(tvCurrency);
        tr.addView(tvDiameter);
        tr.setOnLongClickListener(new TableRow.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                final String tag = (String) view.getTag();
                deleteCoinDialog(tag);
                return true;
            }
        });

        tr.setOnTouchListener(new View.OnTouchListener() {

            private GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onDoubleTap(MotionEvent motionEvent) {
                    return true;
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    coinDialog(v.getTag().toString(), false);
                    return true;
                } else
                    return false;
            }
        });

        OneKKTable.addView(tr, new ViewGroup.LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    public static void createNewTableEntry(String sample, String seedCount) {
        //inputText.setText("");

        /* Create a new row to be added. */
        TableRow tr = new TableRow(context);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* Create the sample name field */
        TextView sampleName = new TextView(context);
        sampleName.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleName.setTextColor(Color.BLACK);
        sampleName.setTextSize(adjustFontSize(sample));
        sampleName.setText("Last Sample : " + sample);
        sampleName.setTag(sample);
        sampleName.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

        /* Create the number of seeds field */
        TextView numSeeds = new TextView(context);
        numSeeds.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        numSeeds.setTextColor(Color.BLACK);
        numSeeds.setTextSize(20.0f);
        numSeeds.setText("Count : " + seedCount);
        numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        tr.addView(sampleName);
        tr.addView(numSeeds);

        OneKKTable.addView(tr, 0, new ViewGroup.LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    // FIXME: 1/23/18

    /************************************************************************************
     * Adds a new entry to the end of the View Data
     ************************************************************************************/
    private static void createNewTableEntry(String sample, String seedCount, String avgL, String avgW, String wt) {

        /* Create a new row to be added. */
        TableRow tr = new TableRow(context);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tr.setPadding(0, 30, 0, 30);
        float fontSize = 20.0f;

        /* Create the sample name field */
        TextView sampleName = new TextView(context);
        sampleName.setGravity(Gravity.LEFT | Gravity.BOTTOM);
        sampleName.setTextColor(Color.BLACK);
        sampleName.setTextSize(fontSize);
        sampleName.setText(sample);
        sampleName.setTag(sample);
        sampleName.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

        /* Create the number of seeds field */
        TextView numSeeds = new TextView(context);
        numSeeds.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        numSeeds.setTextColor(Color.BLACK);
        numSeeds.setTextSize(fontSize);
        numSeeds.setText(seedCount);
        numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        /* Create the length field */
        TextView avgLength = new TextView(context);
        avgLength.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        avgLength.setTextColor(Color.BLACK);
        avgLength.setTextSize(fontSize);
        avgLength.setText(avgL);
        avgLength.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        /* Create the width field */
        TextView avgWidth = new TextView(context);
        avgWidth.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        avgWidth.setTextColor(Color.BLACK);
        avgWidth.setTextSize(fontSize);
        avgWidth.setText(avgW);
        avgWidth.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        /* Create the area field */
        TextView sampleWeight = new TextView(context);
        sampleWeight.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        sampleWeight.setTextColor(Color.BLACK);
        sampleWeight.setTextSize(fontSize);
        sampleWeight.setText(wt);
        sampleWeight.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        /* Define the listener for the longclick event */
        sampleName.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                final String tag = (String) v.getTag();
                Data.deleteDialog(tag);
                return false;
            }
        });

        /* Add UI elements to row and add row to table */
        tr.addView(sampleName);
        tr.addView(numSeeds);
        tr.addView(avgLength);
        tr.addView(avgWidth);
        tr.addView(sampleWeight);

        OneKKTable.addView(tr, 0, new ViewGroup.LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    /************************************************************************************
     * Adds a new record to the internal list of records
     ************************************************************************************/
    // FIXME: 1/23/18 change the parameters to a Seed Record and a Sample Record
    /*public void addRecords(String sampleName, String photoName, String firstName, String lastName, int seedCount, String weight, ArrayList<RawSeed> seedArrayList) {
        // Add all measured seeds to database
        for (RawSeed s : seedArrayList) {
            db.addSeedRecord(new SeedRecord(sampleName, s.getLength(), s.getWidth(), s.getPerimeter(), s.getArea(), "", null));
        }

        // Calculate averages
        double lengthAvg = db.averageSample(sampleName, "length");
        double lengthVar = Math.pow(db.sdSample(sampleName, "length"), 2);
        double lengthCV = (db.sdSample(sampleName, "length")) / (db.averageSample(sampleName, "length"));

        double widthAvg = db.averageSample(sampleName, "width");
        double widthVar = Math.pow(db.sdSample(sampleName, "width"), 2);
        double widthCV = (db.sdSample(sampleName, "width")) / db.averageSample(sampleName, "width");

        double areaAvg = db.averageSample(sampleName, "area");
        double areaVar = Math.pow(db.sdSample(sampleName, "area"), 2);
        double areaCV = (db.sdSample(sampleName, "area")) / (db.averageSample(sampleName, "area"));

        String seedCountString = String.valueOf(seedCount);
        String date = Utils.getDate();

        // Add sample to database
        db.addSampleRecord(new SampleRecord(sampleName, photoName,
                firstName.toLowerCase() + "_" + lastName.toLowerCase(),
                date, seedCountString, String.valueOf(weight), lengthAvg, lengthVar, lengthCV, widthAvg,
                widthVar, widthCV, areaAvg, areaVar, areaCV));

        // Round values for UI
        String avgLengthStr = String.format("%.2f", lengthAvg);
        String avgWidthStr = String.format("%.2f", widthAvg);

        //createNewTableEntry(sampleName, seedCountString);
        currentItemNum++;
    }*/

    public void addRecords(String sampleName, String photoName, String firstName, String lastName, int seedCount, String weight, ArrayList<Seed> seedArrayList) {
        // Add all measured seeds to database
        for (Seed s : seedArrayList) {
            db.addSeedRecord(new SeedRecord(sampleName, s.getLength(), s.getWidth(), s.getPerimeter(), s.getArea(), "", null));
        }

        // Calculate averages
        double lengthAvg = db.averageSample(sampleName, "length");
        double lengthVar = Math.pow(db.sdSample(sampleName, "length"), 2);
        double lengthCV = (db.sdSample(sampleName, "length")) / (db.averageSample(sampleName, "length"));

        double widthAvg = db.averageSample(sampleName, "width");
        double widthVar = Math.pow(db.sdSample(sampleName, "width"), 2);
        double widthCV = (db.sdSample(sampleName, "width")) / db.averageSample(sampleName, "width");

        double areaAvg = db.averageSample(sampleName, "area");
        double areaVar = Math.pow(db.sdSample(sampleName, "area"), 2);
        double areaCV = (db.sdSample(sampleName, "area")) / (db.averageSample(sampleName, "area"));

        String seedCountString = String.valueOf(seedCount);
        String date = Utils.getDate();

        // Add sample to database
        db.addSampleRecord(new SampleRecord(sampleName, photoName,
                firstName.toLowerCase() + "_" + lastName.toLowerCase(),
                date, seedCountString, String.valueOf(weight), lengthAvg, lengthVar, lengthCV, widthAvg,
                widthVar, widthCV, areaAvg, areaVar, areaCV));

        // Round values for UI
        String avgLengthStr = String.format("%.2f", lengthAvg);
        String avgWidthStr = String.format("%.2f", widthAvg);

        //createNewTableEntry(sampleName, seedCountString);
        currentItemNum++;
    }

    // FIXME: change the parameters to a Seed Record and a Sample Record
    public static void addSimpleRecord(String inputText, int seedCount, String weight) {

        String seedCountString = String.valueOf(seedCount);
        String date = getDate();

        // Add sample to database
        db.addSampleRecord(new SampleRecord(inputText, "photoName",
                "first_name" + "last_name",
                date, seedCountString, "", 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0));

        // Round values for UI
        String avgLengthStr = String.format("%.2f", 0.0);
        String avgWidthStr = String.format("%.2f", 0.0);

        createNewTableEntry(inputText, seedCountString);
        db.close();
        currentItemNum++;
    }

    // FIXME: 1/23/18
    public static void clearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.delete_database))
                .setCancelable(false)
                .setTitle("Clear Data")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //makeToast(context.getResources().getString(R.string.data_deleted));
                                dropTables();
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    public static void deleteDialog(String tag) {
        final String sampleName = tag;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.delete_entry));
        builder.setMessage(context.getResources().getString(R.string.delete_msg_1) + " \"" + sampleName + "\". " + context.getResources().getString(R.string.delete_msg_2))
                .setCancelable(true)
                .setPositiveButton(context.getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                db.deleteSample(sampleName);
                                getAllData("sample");
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    // FIXME: 1/23/18
    private static void deleteCoinDialog(String tag) {
        final String[] id = tag.split("~");
        final String countryId = id[0];
        final String nameId = id[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.delete_entry));
        builder.setMessage(context.getResources().getString(R.string.delete_msg_3) + " " + nameId + " of " + countryId + ". " + context.getResources().getString(R.string.delete_msg_2))
                .setCancelable(true)
                .setPositiveButton(context.getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                db.deleteCoin(countryId, nameId);
                                getAllData("coins");
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    // FIXME: 1/23/18
    public static void coinDialog(String tag, final boolean newRecord) {
        final String[] record;

        final String[] updates = new String[3];


        final AlertDialog.Builder alert = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        final View coinView = inflater.inflate(R.layout.add_update_coin_record, new LinearLayout(context), false);
        final EditText etCountry = (EditText) coinView.findViewById(R.id.newCountry);
        final EditText etCoinName = (EditText) coinView.findViewById(R.id.newCoinName);
        final EditText etDiameter = (EditText) coinView.findViewById(R.id.newDiameter);

        if (newRecord) {
            record = null;
            alert.setTitle("Add Coin");
        }
        //existing record
        else {
            record = tag.split("~");
            etCountry.setText(record[0]);  //country
            etCoinName.setText(record[1]); //name
            etDiameter.setText(record[2]); //diameter
            alert.setTitle("Edit Coin");
        }

        alert.setCancelable(true);
        alert.setView(coinView);

        alert.setPositiveButton(context.getResources().getString(R.string.coin_edit_save),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        updates[0] = etCountry.getText().toString();
                        updates[1] = etCoinName.getText().toString();
                        updates[2] = etDiameter.getText().toString();

                        db.coinData(record, updates, newRecord);

                        if (record != null)
                            Toast.makeText(context, "Coin updated", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(context, "Coin added", Toast.LENGTH_LONG).show();

                        getAllData("coins");
                    }
                });

        alert.setNegativeButton(context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

        alert.show();
    }

    // FIXME: 1/23/18
    public static void resetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.reset_coin_database))
                .setCancelable(false)
                .setTitle("Reset Coin Data")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //makeToast(context.getResources().getString(R.string.data_deleted));
                                MySQLiteHelper mySQLiteHelper = new MySQLiteHelper(context);
                                InputStream inputStream = null;
                                try {
                                    inputStream = context.getAssets().open("coin_database.csv");
                                } catch (Exception ex) {
                                    Log.e("Coin DB file error : ", ex.getMessage());
                                }
                                mySQLiteHelper.importCoinData(inputStream);
                                getAllData("coins");
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    private static void dropTables() {
        db.deleteAll();
        OneKKTable.removeAllViews();
        currentItemNum = 1;
    }

    // FIXME: 1/23/18
    /* get data from database get show them in dialog */
    public static void exportSeedSamplesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.export_choice))
                .setCancelable(false)

                .setPositiveButton(context.getResources().getString(R.string.export_raw),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportRawData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNeutralButton(context.getResources().getString(R.string.export_all),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.export_summaries),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setTitle(context.getResources().getString(R.string.export_data))
                .setNeutralButton(context.getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    public static void exportCoinsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.export_coins))
                .setCancelable(false)
                .setTitle(context.getResources().getString(R.string.export_data))
                .setPositiveButton(context.getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        //can be used to ask the user if he/she wants to process another sample
                                        this.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                shareFile(path);
                                            }
                                        }, 3000);
                                    }
                                };
                                Toast.makeText(context, "Exporting Coin Data", Toast.LENGTH_LONG).show();

                                // creates a new thread and starts processing export
                                final ProgressDialog progressDialog = ProgressDialog.show(context, "Exporting Coin Data", "Please wait .. ");
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Cursor exportCursor = db.exportCoinsData();
                                        try {
                                            path = exportDatabase(exportCursor, "CoinsData");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        progressDialog.dismiss();
                                        handler.sendEmptyMessage(0);
                                    }
                                }).start();
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    /* export database data to .csv file*/
    private static String exportDatabase(Cursor cursorForExport, String type) throws Exception {
        File file = null;

        try {
            file = new File(Constants.EXPORT_PATH, "export_" + type + "_" + getDate() + ".csv");
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            csvWrite.writeNext(cursorForExport.getColumnNames());

            while (cursorForExport.moveToNext()) {
                String arrStr[] = new String[cursorForExport.getColumnCount()];

                for (int k = 0; k < cursorForExport.getColumnCount(); k++) {
                    arrStr[k] = cursorForExport.getString(k);
                }

                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            cursorForExport.close();
            makeFileDiscoverable(file, context);
            //shareFile(file.getPath());
        } catch (Exception e) {
            Log.e("Data", e.getMessage());
        }

        return file.toString();
    }

    // FIXME: 1/23/18
    private static void shareFile(final String filePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.share_file))
                .setCancelable(false)
                .setTitle(context.getResources().getString(R.string.share))
                .setPositiveButton(context.getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_SEND);
                                intent.setType("text/*");
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
                                context.startActivity(Intent.createChooser(intent, "Sending File..."));
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void updateSampleWeight(String name, String weight) {
        db.updateSampleWeight(name, weight);
    }
}
