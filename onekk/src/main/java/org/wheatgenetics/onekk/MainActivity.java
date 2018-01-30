package org.wheatgenetics.onekk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.*;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.wheatgenetics.database.MySQLiteHelper;
import org.wheatgenetics.imageprocess.HueThreshold.HueThreshold;
import org.wheatgenetics.imageprocess.ImgProcess1KK.ImgProcess1KK;
import org.wheatgenetics.imageprocess.ImgProcess1KK.ImgProcess1KK.Seed;
import org.wheatgenetics.imageprocess.WatershedLB.*;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import org.wheatgenetics.onekkUtils.oneKKUtils;

import static org.wheatgenetics.onekk.Data.getAllData;
import static org.wheatgenetics.onekkUtils.oneKKUtils.*;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    public final static String TAG = "OneKK";
    private SharedPreferences ep;

    private ArrayList<Seed> seeds;
    private TableLayout lastSampleTable;
    private Data data;
    private static UsbDevice mDevice;

    private EditText mWeightEditText;
    private EditText inputText;

    int seedCount = 0;
    int notificationCounter = 1;
    private String firstName = "";
    private String lastName = "";


    FrameLayout preview;

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private CameraPreview mPreview;
    private String picName = "";
    private String photoName;
    private String photoPath;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private Bitmap outputBitmap;
    private Mat finalMat;
    private LinearLayout parent;
    private ScrollView changeContainer;
    private String sampleName;
    private String weight;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Random r;
    private ImageButton cameraButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.bringToFront();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView nvDrawer = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);
        setupDrawer();

        ep = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        lastSampleTable = (TableLayout)findViewById(R.id.lastSampleTable);
        inputText = (EditText) findViewById(R.id.etInput);
        mWeightEditText = (EditText) findViewById(R.id.etWeight);
        mWeightEditText.setText(getResources().getString(R.string.not_connected));

        preview = (FrameLayout) findViewById(R.id.camera_preview);
        parent = new LinearLayout(this);
        changeContainer = new ScrollView(this);
        changeContainer.removeAllViews();
        changeContainer.addView(parent);

        Intent intent = getIntent();
        mDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        cameraButton = (ImageButton) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new ImageButton.OnClickListener(
        ){
            @Override
            public void onClick(View view) {
                picName = inputText.getText().toString();
                takePic();
            }
        });

        startCamera();
        createDirs();
        data = new Data(MainActivity.this,lastSampleTable);
        data.getLastData();


        /**
         * uncomment to show the settings preferences at the start of the app
         */
        //final Intent settingsIntent = new Intent(this,SettingsActivity.class);
        //startActivity(settingsIntent);

        //makeToast(String.valueOf(preview.getHeight()) + " " + String.valueOf(preview.getWidth()));
        //OpenCVLoader.initAsync()
        if (!OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.INIT_FAILED);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            finalMat = new Mat();
        }

        if (ep.getString(SettingsFragment.FIRST_NAME, "").length() == 0) {
            setPersonDialog();
        }

        if (!ep.getBoolean("ignoreScale", false)) {
            findScale();
        }

        Editor ed = ep.edit();
        if (ep.getInt("UpdateVersion", -1) < getVersion()) {
            ed.putInt("UpdateVersion", getVersion());
            ed.apply();
            changelog();
        }

        if (ep.getInt(SettingsFragment.COIN_DB, -1) == -1) {
            ed.putInt(SettingsFragment.COIN_DB, 1);
            MySQLiteHelper mySQLiteHelper = new MySQLiteHelper(this);
            InputStream inputStream = null;
            try {
                inputStream   = this.getAssets().open("coin_database.csv");
            }
            catch(Exception ex) {
                Log.e("Coin DB file error : ", ex.getMessage());
            }
            mySQLiteHelper.importCoinData(inputStream);
        }

        FrameLayout measuringStick = (FrameLayout) findViewById(R.id.measureStick);
        measuringStick.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            int count = 0;
            double ratio = 0.0;
            int height1 = 0;
            int width1 = 0;

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                if (preview.getWidth() != 0 && preview.getHeight() != 0) {
                    if (preview.getWidth() > width1 && preview.getHeight() > height1) {
                        width1 = preview.getWidth();
                        height1 = preview.getHeight();
                        count++;
                    }

                    if(count!=0) {
                        ratio = ((double) height1)/((double) width1);
                    }
                }

                if(bottom<oldBottom) {
                    int newWidth = (int) (bottom/ratio);
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(newWidth,bottom);
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    lp.addRule(RelativeLayout.ALIGN_TOP);
                    preview.setLayoutParams(lp);
                }

                if(oldBottom<bottom) {
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width1,height1);
                    preview.setLayoutParams(lp);
                }

                //makeToast(String.valueOf(bottom) + " " + String.valueOf(oldBottom));
                //makeToast(String.valueOf(preview.getHeight()));
            }
        });
    }

    private void createDirs() {
        createDir(Constants.MAIN_PATH);
        createDir(Constants.EXPORT_PATH);
        createDir(Constants.PHOTO_PATH);
        createDir(Constants.ANALYZED_PHOTO_PATH);
    }

    public int getVersion() {
        int v = 0;
        try {
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Field Book", "" + e.getMessage());
        }
        return v;
    }

    private void createDir(File path) {
        File blankFile = new File(path, ".onekk");

        if (!path.exists()) {
            path.mkdirs();

            try {
                blankFile.getParentFile().mkdirs();
                blankFile.createNewFile();
                oneKKUtils.makeFileDiscoverable(blankFile, this);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @SuppressWarnings("deprecation")
    private void startCamera() {
        mCamera = getCameraInstance();

        PackageManager pm = getPackageManager();
        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)){
            Camera.Parameters params = mCamera.getParameters();

            if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            mCamera.setParameters(params);
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);

        guideBox gb = new guideBox(this,Integer.parseInt(ep.getString(SettingsFragment.COIN_SIZE,"4")));
        preview.addView(gb,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //addContentView(gb,new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @SuppressWarnings("deprecation")
    public static Camera getCameraInstance() {
        @SuppressWarnings("deprecation")
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return c;
    }

    private void changelog() {
        parent.setOrientation(LinearLayout.VERTICAL);
        parseLog(R.raw.changelog_releases);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.updatemsg));
        builder.setView(changeContainer)
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void parseLog(int resId) {
        try {
            InputStream is = getResources().openRawResource(resId);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr, 8192);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(20, 5, 20, 0);

            String curVersionName = null;
            String line;

            while ((line = br.readLine()) != null) {
                TextView header = new TextView(this);
                TextView content = new TextView(this);
                TextView spacer = new TextView(this);
                View ruler = new View(this);

                header.setLayoutParams(lp);
                content.setLayoutParams(lp);
                spacer.setLayoutParams(lp);
                ruler.setLayoutParams(lp);

                spacer.setTextSize(5);

                ruler.setBackgroundColor(getResources().getColor(R.color.main_colorAccent));
                header.setTextAppearance(getApplicationContext(), R.style.ChangelogTitles);
                content.setTextAppearance(getApplicationContext(), R.style.ChangelogContent);

                if (line.length() == 0) {
                    curVersionName = null;
                    spacer.setText("\n");
                    parent.addView(spacer);
                } else if (curVersionName == null) {
                    final String[] lineSplit = line.split("/");
                    curVersionName = lineSplit[1];
                    header.setText(curVersionName);
                    parent.addView(header);
                    parent.addView(ruler);
                } else {
                    content.setText("•  " + line);
                    parent.addView(content);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                TextView person = (TextView) findViewById(R.id.nameLabel);
                person.setText(ep.getString(SettingsFragment.FIRST_NAME, "") + " " + ep.getString(SettingsFragment.LAST_NAME, ""));
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_settings:
                final Intent settingsIntent = new Intent(this,SettingsActivity.class);
                startActivity(settingsIntent);
                break;

            case R.id.view_data:
                final Intent viewTableIntent = new Intent(this,ViewDataActivity.class);
                startActivity(viewTableIntent);
                break;

            case R.id.nav_scaleConnect:
                findScale();
                break;

            case R.id.nav_help:
                makeToast(getResources().getString(R.string.coming_soon));
                break;

            case R.id.nav_about:
                aboutDialog();
                break;
        }

        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCamera == null) {
            startCamera(); // Local method to handle camera initialization
        }
        Log.v(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera == null) {
            startCamera(); // Local method to handle camera initialization
        }
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return true;
    }

    private void takePic() {
        //inputText.setEnabled(false); //TODO fix camera preview and enable this
        mCamera.takePicture(null, null, mPicture);
    }

    @SuppressWarnings("deprecation")
    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            String fileName;
            if (picName.length() > 0) {
                fileName = picName + "_";
            } else {
                fileName = "temp_";
            }
            File pictureFile = oneKKUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE,fileName);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Uri outputFileUri = Uri.fromFile(pictureFile);
            makeFileDiscoverable(pictureFile, MainActivity.this);

            if(ep.getBoolean(SettingsFragment.ASK_PROCESSING_TECHNIQUE,true))
                processingTechniqueDialog();

            String input = inputText.getText().toString();
            r = new Random();
            if(input.charAt(0) == '$'){
                outputFileUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/"+input.substring(3)+".jpg"));
                inputText.setText(input.substring(3) + r.nextInt(200));
                switch (input.substring(1,3)) {
                    case "kk":
                        imageAnalysis(outputFileUri);
                        mCamera.startPreview();
                        break;
                    case "ht":
                        hueThreshold(outputFileUri);
                        mCamera.startPreview();
                        break;
                    default:
                        imageAnalysisLB(outputFileUri);
                        mCamera.startPreview();
                }
            }
            else{
                //imageAnalysis(outputFileUri);
                //hueThreshold(outputFileUri);
                imageAnalysisLB(outputFileUri);
                mCamera.startPreview();
            }
        }
    };


    /************************************************************************************
     * displays a dialogue after capturing the image, prompting the user to select a
     * processing technique, only if "Processing" -> "Always Ask" setting is checked
     * else proceed with the default technique set in the settings panel
     ************************************************************************************/

    // TODO: check the user selection and call the respective processing technique
    // TODO: complete the function implementation

    public void processingTechniqueDialog(){
        final ArrayList mSelectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Processing technique")

                .setSingleChoiceItems(R.array.processing_techniques, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.v(TAG,which+"");
                            }
                        })

                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG,"clicked");
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG,"clicked negative");
                    }
                });

        builder.create().show();
    }

    /************************************************************************************
     * image analysis method call to perform the default processing ImgProcess1KK
     ************************************************************************************/
    private void imageAnalysis(Uri photo) {
        photoName = photo.getLastPathSegment();
        makeToast(photoName);

        double refDiam = Double.valueOf(ep.getString(SettingsFragment.COIN_SIZE, "1")); // Wheat default

        ImgProcess1KK imgP = new ImgProcess1KK(Constants.PHOTO_PATH.toString() + "/" + photoName, refDiam, true, Double.valueOf(ep.getInt(SettingsFragment.MIN_SEED_VALUE, 0)), Double.valueOf(ep.getInt(SettingsFragment.MAX_SEED_VALUE, 0))); //TODO the min/max sizes are bad


        writeMat2File(imgP.getProcessedMat(),Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg");
        makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), this);

        seedCount = imgP.getSeedCount();

        seeds = imgP.getList();
        //addRecord(); // Add the current record to the table

        //if (ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false)) {
        //    postImageDialog(photoName,seedCount);
        //}
    }

    /************************************************************************************
     * image analysis method call to perform WatershedLB, the algorithmName parameter can
     * be used to extend different algorithm implementations using the same function call
     ************************************************************************************/

    // TODO: post analysis store the data in the database with the new format

    private void hueThreshold(final Uri photo) {
        photoPath = photo.getPath();
        photoName = photo.getLastPathSegment();

        makeToast(photoName);
        final HueThreshold hueThreshold;
        double refDiam = Double.valueOf(ep.getString(SettingsFragment.COIN_SIZE, "1")); // Wheat default
        double thresh = ep.getInt(SettingsFragment.THRESHOLD, 1);
        int minHue = ep.getInt(SettingsFragment.MIN_HUE_VALUE, 30);
        int maxHue = ep.getInt(SettingsFragment.MAX_HUE_VALUE, 255);
        final HueThreshold.HueThresholdParams params = new HueThreshold.HueThresholdParams(thresh,minHue,maxHue);
        hueThreshold = new HueThreshold(params);

        /*//after processing the image on a separate thread this handler is called
        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg){

                w.writeProcessedImg(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg");
                makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), MainActivity.this);
                seedCount = w.getSeedCount();

                //update the seed count in the database

                //old format
                //myDB.updateData(seedCount);

                //new format
                //addRecord();

                if(ep.getBoolean(SettingsFragment.DISPLAY_SEED_COUNT,false))
                    seedCountDialog(seedCount);

                if (ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false)) {
                    postImageDialog(photoName);
                }
                //can be used to ask the user if he/she wants to process another sample
                *//*this.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        feedbackDialog();
                    }
                }, 3000);*//*
            }
        };*/

        final Bitmap inputBitmap = BitmapFactory.decodeFile(photoPath);
       /* // creates a new thread and starts processing the image using watershed
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Processing", "Please wait .. ");
        new Thread(new Runnable() {
            @Override
            public void run() {*/
        File imageFile = hueThreshold.process(inputBitmap);
        Uri imageUri = Uri.fromFile(imageFile);
        imageAnalysisLB(imageUri);
                /*progressDialog.dismiss();
                handler.sendEmptyMessage(0);
            }
        }).start();*/
    }

    private void imageAnalysisLB(final Uri photo) {
        photoPath = photo.getPath();
        photoName = photo.getLastPathSegment();

        makeToast(photoName);

        //seed counter utility
        final WatershedLB mSeedCounter;
        final int areaLow = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_LOW, "400"));
        final int areaHigh = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_HIGH, "160000"));
        final int defaultRate = Integer.valueOf(ep.getString(SettingsFragment.PARAM_DEFAULT_RATE, "34"));
        final double sizeLowerBoundRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_SIZE_LOWER_BOUND_RATIO, "0.25"));
        final double newSeedDistRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_NEW_SEED_DIST_RATIO, "4.0"));
        final Bitmap inputBitmap = BitmapFactory.decodeFile(photoPath);
        final WatershedLB.WatershedParams params = new WatershedLB.WatershedParams(areaLow, areaHigh, defaultRate, sizeLowerBoundRatio, newSeedDistRatio);
        mSeedCounter = new WatershedLB(params);

        sampleName = inputText.getText().toString();

        if (mDevice == null
                && mWeightEditText.getText().toString().equals("Not connected")) {
            weight = "null";
        } else {
            weight = mWeightEditText.getText().toString();
        }

        inputText.setText("");
        mWeightEditText.setText("0");
        inputText.requestFocus();

        Boolean showAnalysis = ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false);
        Boolean backgroundProcessing = ep.getBoolean(SettingsFragment.ASK_BACKGROUND_PROCESSING,false);

        WatershedLBActivity watershedLBActivity = new WatershedLBActivity(MainActivity.this,mSeedCounter,photoName,showAnalysis,sampleName,weight,r.nextInt(20000),backgroundProcessing);
        watershedLBActivity.execute(inputBitmap);
        data.getLastData();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    private void aboutDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.about, new LinearLayout(this), false);
        TextView version = (TextView) personView.findViewById(R.id.tvVersion);
        TextView otherApps = (TextView) personView.findViewById(R.id.tvOtherApps);


        final PackageManager packageManager = this.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
            version.setText(getResources().getString(R.string.versiontitle) + " " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }

        version.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changelog();
            }
        });

        otherApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOtherAppsDialog();
            }
        });


        alert.setCancelable(true);
        alert.setTitle(getResources().getString(R.string.about));
        alert.setView(personView);
        alert.setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void showOtherAppsDialog() {
        final AlertDialog.Builder otherAppsAlert = new AlertDialog.Builder(this);

        ListView myList = new ListView(this);
        myList.setDivider(null);
        myList.setDividerHeight(0);
        String[] appsArray = new String[3];

        appsArray[0] = "Field Book";
        appsArray[1] = "Inventory";
        appsArray[2] = "Coordinate";
        //appsArray[3] = "Intercross";
        //appsArray[4] = "Rangle";

        Integer app_images[] = {R.drawable.other_ic_field_book, R.drawable.other_ic_inventory, R.drawable.other_ic_coordinate};
        final String[] links = {"https://play.google.com/store/apps/details?id=com.fieldbook.tracker",
                "https://play.google.com/store/apps/details?id=org.wheatgenetics.inventory",
                "http://wheatgenetics.org/apps"}; //TODO update these links

        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View arg1, int which, long arg3) {
                Uri uri = Uri.parse(links[which]);
                Intent intent;

                switch (which) {
                    case 0:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                }
            }
        });

        CustomListAdapter adapterImg = new CustomListAdapter(this, app_images, appsArray);
        myList.setAdapter(adapterImg);

        otherAppsAlert.setCancelable(true);
        otherAppsAlert.setTitle(getResources().getString(R.string.otherapps));
        otherAppsAlert.setView(myList);
        otherAppsAlert.setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        otherAppsAlert.show();
    }

    public class CustomListAdapter extends ArrayAdapter<String> {
        String[] color_names;
        Integer[] image_id;
        Context context;

        public CustomListAdapter(Activity context, Integer[] image_id, String[] text) {
            super(context, R.layout.appline, text);
            this.color_names = text;
            this.image_id = image_id;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View single_row = inflater.inflate(R.layout.appline, null, true);
            TextView textView = (TextView) single_row.findViewById(R.id.txt);
            ImageView imageView = (ImageView) single_row.findViewById(R.id.img);
            textView.setText(color_names[position]);
            imageView.setImageResource(image_id[position]);
            return single_row;
        }
    }

    public void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setPersonDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.person, new LinearLayout(this), false);

        final EditText fName = (EditText) personView
                .findViewById(R.id.firstName);
        final EditText lName = (EditText) personView
                .findViewById(R.id.lastName);

        fName.setText(ep.getString(SettingsFragment.FIRST_NAME, ""));
        lName.setText(ep.getString(SettingsFragment.LAST_NAME, ""));

        alert.setCancelable(false);
        alert.setTitle(getResources().getString(R.string.set_person));
        alert.setView(personView);
        alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                firstName = fName.getText().toString().trim();
                lastName = lName.getText().toString().trim();

                if (firstName.length() == 0 | lastName.length() == 0) {
                    makeToast(getResources().getString(R.string.no_blank));
                    setPersonDialog();
                    return;
                }

                makeToast(getResources().getString(R.string.person_set) + " " + firstName + " " + lastName);
                Editor ed = ep.edit();
                ed.putString(SettingsFragment.FIRST_NAME, firstName);
                ed.putString(SettingsFragment.LAST_NAME, lastName);
                ed.apply();
            }
        });
        alert.show();
    }



    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera(); // release the camera immediately on pause event
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }



    public void findScale() {
        if (mDevice == null) {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            for (UsbDevice usbDevice : deviceList.values()) {
                mDevice = usbDevice;
                Log.v(TAG,
                        String.format(
                                "name=%s deviceId=%d productId=%d vendorId=%d deviceClass=%d subClass=%d protocol=%d interfaceCount=%d",
                                mDevice.getDeviceName(), mDevice.getDeviceId(),
                                mDevice.getProductId(), mDevice.getVendorId(),
                                mDevice.getDeviceClass(),
                                mDevice.getDeviceSubclass(),
                                mDevice.getDeviceProtocol(),
                                mDevice.getInterfaceCount()));
                break;
            }
        }

        if (mDevice != null) {
            mWeightEditText.setText("0");
            //new ScaleListener().execute();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getResources().getString(R.string.no_scale))
                    .setMessage(
                            getResources().getString(R.string.connect_scale))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.try_again),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    findScale();
                                }

                            })
                    .setNegativeButton(getResources().getString(R.string.ignore),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Editor ed = ep.edit();
                                    ed.putBoolean("ignoreScale", true);
                                    ed.apply();
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    private class ScaleListener extends AsyncTask<Void, Double, Void> {
        private double mLastWeight = 0;

        @Override
        protected Void doInBackground(Void... arg0) {

            byte[] data = new byte[128];
            int TIMEOUT = 2000;

            Log.v(TAG, "start transfer");

            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (mDevice == null) {
                Log.e(TAG, "no device");
                return null;
            }
            UsbInterface intf = mDevice.getInterface(0);

            Log.v(TAG,
                    String.format("endpoint count = %d",
                            intf.getEndpointCount()));
            UsbEndpoint endpoint = intf.getEndpoint(0);
            Log.v(TAG, String.format(
                    "endpoint direction = %d out = %d in = %d",
                    endpoint.getDirection(), UsbConstants.USB_DIR_OUT,
                    UsbConstants.USB_DIR_IN));
            UsbDeviceConnection connection = usbManager.openDevice(mDevice);
            Log.v(TAG, "got connection:" + connection.toString());
            connection.claimInterface(intf, true);
            while (true) {

                int length = connection.bulkTransfer(endpoint, data,
                        data.length, TIMEOUT);

                if (length != 6) {
                    Log.e(TAG, String.format("invalid length: %d", length));
                    return null;
                }

                byte report = data[0];
                byte status = data[1];
                //byte exp = data[3];
                short weightLSB = (short) (data[4] & 0xff);
                short weightMSB = (short) (data[5] & 0xff);

                // Log.v(TAG, String.format(
                // "report=%x status=%x exp=%x lsb=%x msb=%x", report,
                // status, exp, weightLSB, weightMSB));

                if (report != 3) {
                    Log.v(TAG, String.format("scale status error %d", status));
                    return null;
                }

                double mWeightGrams;
                if (mDevice.getProductId() == 519) {
                    mWeightGrams = (weightLSB + weightMSB * 256.0) / 10.0;
                } else {
                    mWeightGrams = (weightLSB + weightMSB * 256.0);
                }
                double mZeroGrams = 0;
                double zWeight = (mWeightGrams - mZeroGrams);

                switch (status) {
                    case 1:
                        Log.w(TAG, "Scale reports FAULT!\n");
                        break;
                    case 3:
                        Log.i(TAG, "Weighing...");
                        if (mLastWeight != zWeight) {
                            publishProgress(zWeight);
                        }
                        break;
                    case 2:
                    case 4:
                        if (mLastWeight != zWeight) {
                            Log.i(TAG, String.format("Final Weight: %f", zWeight));
                            publishProgress(zWeight);
                        }
                        break;
                    case 5:
                        Log.w(TAG, "Scale reports Under Zero");
                        if (mLastWeight != zWeight) {
                            publishProgress(0.0);
                        }
                        break;
                    case 6:
                        Log.w(TAG, "Scale reports Over Weight!");
                        break;
                    case 7:
                        Log.e(TAG, "Scale reports Calibration Needed!");
                        break;
                    case 8:
                        Log.e(TAG, "Scale reports Re-zeroing Needed!\n");
                        break;
                    default:
                        Log.e(TAG, "Unknown status code");
                        break;
                }

                mLastWeight = zWeight;
            }
        }

        @Override
        protected void onProgressUpdate(Double... weights) {
            Double weight = weights[0];
            Log.i(TAG, "update progress");
            String weightText = String.format("%.1f", weight);
            Log.i(TAG, weightText);
            mWeightEditText.setText(weightText);
            mWeightEditText.invalidate();
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.scale_disconnect),
                    Toast.LENGTH_LONG).show();
            mDevice = null;
            mWeightEditText.setText("Not connected");
        }
    }

    public void onInit(int status) {
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}