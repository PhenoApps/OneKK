package org.wheatgenetics.onekk;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.wheatgenetics.database.Data;
import org.wheatgenetics.database.MySQLiteHelper;
import org.wheatgenetics.imageprocess.CoinRecognitionTask;
import org.wheatgenetics.imageprocess.ImageProcess;
import org.wheatgenetics.utils.Constants;
import org.wheatgenetics.utils.Utils;
import org.wheatgenetics.ui.CameraPreview;
import org.wheatgenetics.ui.TouchImageView;
import org.wheatgenetics.ui.GuideBox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;

import static org.wheatgenetics.utils.Utils.makeFileDiscoverable;
import static org.wheatgenetics.utils.Utils.postImageDialog;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    public final static String TAG = "OneKK";
    public static final int MEDIA_TYPE_IMAGE = 1;
    //request message of getting the image path
    public static final int GET_PATH_REQUEST = 3;
    public static final int GET_WEIGHT_REQUEST = 4;
    public static final int GET_BLUETOOTH_DEVICE_REQUEST = 5;
    //requests for permissions
    private final int REQ_CAMERA_PERM = 101;

    //setting information handler
    private SharedPreferences ep;

    private Data data;

    private EditText mWeightEditText;
    private EditText inputText;

    int previousSize = 0;
    int seedCount = 0;
    int notificationCounter = 1;
    private String firstName = "";
    private String lastName = "";
    private String picName = "";
    private String photoName;
    private String photoPath;

    private GuideBox gb;
    private CoinRecognitionTask coinRecognitionTask;
    private FrameLayout preview;

    //private Camera mCamera;
    private CameraPreview mPreview;

    private LinearLayout parent;
    private ScrollView changeContainer;
    private String sampleName;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Random r;

    String mBluetoothDeviceName;
    String mBluetoothDeviceAddress;
    //default value is 1 which means one step
    private int mScaleSteps = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.bringToFront();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView nvDrawer = findViewById(R.id.nvView);
        /* setup navigation click listener*/
        setupDrawerContent(nvDrawer);
        setupDrawer();

        ep = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        //TableLayout lastSampleTable = findViewById(R.id.lastSampleTable);
        //inputText = findViewById(R.id.etInput);
        //mWeightEditText = findViewById(R.id.etWeight);
        //mWeightEditText.setText(getResources().getString(R.string.not_connected));

        //preview = findViewById(R.id.camera_preview);
        parent = new LinearLayout(this);
        changeContainer = new ScrollView(this);
        changeContainer.removeAllViews();
        changeContainer.addView(parent);

       // ImageButton cameraButton = findViewById(R.id.picture);


        //Todo move logic to camera2
        /*cameraButton.setOnClickListener(new ImageButton.OnClickListener(
        ) {
            @Override
            public void onClick(View view) {
                if (ep.getString(SettingsFragment.COIN_NAME, "-1").compareTo("-1") == 0)
                    Toast.makeText(getApplicationContext(), "Please select the Coin Name in Settings Panel!", Toast.LENGTH_LONG).show();
                else {
                    picName = inputText.getText().toString();
                    takePic();
                }
            }
        });*/

        createDirs();
        //data = new Data(MainActivity.this, lastSampleTable);
        //data.getLastData();


        /**
         * uncomment to show the settings preferences at the hueProcess of the app
         */
        //final Intent settingsIntent = new Intent(this,SettingsActivity.class);
        //startActivity(settingsIntent);

        //makeToast(String.valueOf(preview.getLength()) + " " + String.valueOf(preview.getWidth()));
        //OpenCVLoader.initAsync()

        if (!OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.INIT_FAILED);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (ep.getString(SettingsFragment.FIRST_NAME, "").length() == 0) {
            //setPersonDialog();
        }

        Editor ed = ep.edit();
        if (ep.getInt("UpdateVersion", -1) < getVersion()) {
            ed.putInt("UpdateVersion", getVersion());
            ed.apply();
            changelog();
        }

        if (ep.getInt(SettingsFragment.COIN_DB, -1) == -1) {
            ed.putInt(SettingsFragment.COIN_DB, 1);
            ed.apply();
            MySQLiteHelper mySQLiteHelper = new MySQLiteHelper(this);
            InputStream inputStream = null;
            try {
                inputStream = this.getAssets().open("coin_database.csv");
            } catch (Exception ex) {
                Log.e("Coin DB file error : ", ex.getMessage());
            }
            mySQLiteHelper.importCoinData(inputStream);
        }

        if (!ep.getBoolean("onlyLoadTutorialOnce", false)) {
            ed.putBoolean("onlyLoadTutorialOnce", true);
            ed.apply();
        }

        mScaleSteps = Integer.parseInt(ep.getString("scale_steps", "1"));

        //TODO get bluetooth to work with Camera2API
        //scaleBluetoothInit();

        startCamera();

    }


    public int getVersion() {
        int v = 0;
        try {
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "" + e.getMessage());
        }
        return v;
    }

    private void createDirs() {
        createDir(Constants.MAIN_PATH);
        createDir(Constants.EXPORT_PATH);
        createDir(Constants.PHOTO_PATH);
        createDir(Constants.PHOTO_SAMPLES_PATH);
        createDir(Constants.ANALYZED_PHOTO_PATH);
    }

    private void createDir(File path) {
        File blankFile = new File(path, ".onekk");

        if (!path.exists()) {
            path.mkdirs();

            try {
                blankFile.getParentFile().mkdirs();
                blankFile.createNewFile();
                Utils.makeFileDiscoverable(blankFile, this);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            /* copy sample images when the directory is created */
            if (path.compareTo(Constants.PHOTO_SAMPLES_PATH) == 0)
                copySampleImages();
        }
    }

    /**
     * copy sample images into the directory OneKK/Photos/Samples
     */
    private void copySampleImages() {

        AssetManager assetManager = getAssets();

        String[] files = {"soybean.jpg", "wheat.jpg", "maize.jpg"};

        for (String filename : files) {
            InputStream in;
            OutputStream out;

            try {
                in = assetManager.open(filename);

                String outDir = Constants.PHOTO_SAMPLES_PATH.toString();

                File outFile = new File(outDir, filename);

                out = new FileOutputStream(outFile);
                copyImage(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy asset file: " + filename, e);
            }
        }
    }

    /**
     * used by copySampleImages method
     */

    //TODO fix constant buffer size
    private void copyImage(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
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

    private void startCamera() {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.camera_preview, Camera2BasicFragment.newInstance())
                    .commit();

        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQ_CAMERA_PERM);
        }

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

            //TODO fix constant buffer size
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
                TextView person = findViewById(R.id.nameLabel);
                person.setText(ep.getString(SettingsFragment.FIRST_NAME, "") + " " + ep.getString(SettingsFragment.LAST_NAME, ""));
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    /* set navigation click listener*/
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

    public String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == GET_PATH_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                String path = getRealPathFromURI(imageUri);
                int index = path.lastIndexOf('/');
                String sampleName = path.substring(index + 1, path.length() - 4);
                analysisChoosingPhoto(path, sampleName);

            }
        } else if (requestCode == GET_BLUETOOTH_DEVICE_REQUEST) {

            if (resultCode == RESULT_OK) {

                mBluetoothDevice = data.getExtras().getParcelable("BluetoothDevice");
                mBluetoothDeviceName = mBluetoothDevice.getName();
                mBluetoothDeviceAddress = mBluetoothDevice.getAddress();

                if (mConnected) {
                    try {
                        unbindService(mServiceConnection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    mHandler.removeCallbacks(mRunnable);
                    if (mBluetoothLeService != null) {
                        mBluetoothLeService.disconnect();
                        mBluetoothLeService = null;
                    }

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }

                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            }
        }
        /*else if (requestCode == GET_WEIGHT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    weight = data.getStringExtra("Weight");
                    mWeightEditText.setText(weight);
                }
            }
        }*/
    }

    /* Set navigation click action*/
    public void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_settings:
                final Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;

            case R.id.nav_samples:
                samplesDialog();
                break;

            case R.id.view_data:
                final Intent viewTableIntent = new Intent(this, ViewDataActivity.class);
                startActivity(viewTableIntent);
                break;

            case R.id.choose_photo:
                /*final Intent choosePhotoIntent = new Intent(this, ChoosePhotoActivity.class);
                //startActivity(choosePhotoIntent);
                startActivityForResult(choosePhotoIntent, GET_PATH_REQUEST);*/

                // invoke the image gallery using an implict intent.
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

                // where do we want to find the data?
                File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String pictureDirectoryPath = pictureDirectory.getPath();
                // finally, get a URI representation
                Uri data = Uri.parse(pictureDirectoryPath);

                // set the data and type.  Get all image types.
                photoPickerIntent.setDataAndType(data, "image/*");

                // we will invoke this activity, and get something back from it.
                startActivityForResult(photoPickerIntent, GET_PATH_REQUEST);

                break;

            case R.id.nav_scale:
                final Intent scaleIntent = new Intent(this, ScaleActivity.class);
                scaleIntent.putExtra("DeviceName", mBluetoothDeviceName);
                scaleIntent.putExtra("DeviceAddress", mBluetoothDeviceAddress);
                if (mConnected) {
                    scaleIntent.putExtra("ConnectionState", "Connected");
                } else {
                    scaleIntent.putExtra("ConnectionState", "No connection");
                }

                scaleIntent.putExtra("WeightData", weightData);
                startActivityForResult(scaleIntent, GET_BLUETOOTH_DEVICE_REQUEST);
                break;

            case R.id.nav_about:
                aboutDialog();
                break;
        }

        mDrawerLayout.closeDrawers();
    }

    public void onRequestPermissionsResult(int result, String[] permissions, int[] granted) {

        int i = 0;
        for (String p : permissions) {
            if (p.equals(Manifest.permission.CAMERA) && granted[i] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //handle bluetooth connection
        try {
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.removeCallbacks(mRunnable);
        mBluetoothLeService = null;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    public void onResume() {
        super.onResume();

        mScaleSteps = Integer.parseInt(ep.getString("scale_steps", "1"));

        //handle bluetooth connection
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
            //Log.d(TAG, "Connect request result=" + result);
        }
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

        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return true;
    }


//    /*PictureCallback mPicture = new PictureCallback() {
//        @Override
//        public void onPictureTaken(byte[] picData, Camera camera) {
//            //Naive custom real time coin recognition
//            /*
//            ArrayList<Point> cornerArrayList = null;
//
//            Camera.Parameters parameters = camera.getParameters();
//
//            int h = parameters.getPreviewSize().height;
//            int w = parameters.getPreviewSize().width;
//
//            coinRecognitionTask = new CoinRecognitionTask(w,h);
//            coinRecognitionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,data);
//
//            try {
//                cornerArrayList = coinRecognitionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,data).get();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                Log.e("AsyncTask",e.getMessage());
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//                Log.e("AsyncTask",e.getMessage());
//            }
//
//            Log.d("Corner Array list",cornerArrayList.toString());
//            if(cornerArrayList.size() != 4){
//                Toast.makeText(MainActivity.this,"Couldn't detect all the coins, adjust and try again",Toast.LENGTH_LONG).show();
//                mCamera.startPreview();
//            }
//            else {
//            */
//
//            /*if (ep.getBoolean(SettingsFragment.ASK_PROCESSING_TECHNIQUE, true))
//                processingTechniqueDialog();*/
//
//            Uri outputFileUri;
//            String input;
//
//            if (inputText.getText().length() != 0) {
//                input = inputText.getText().toString();
//
//                /* This section of code is just a hack to run already stored sample images for UI testing
//                 *
//                 *  In the sample name input box the developer can enter $ followed by,
//                 *  either kk or lb to run different algorithms, followed by
//                 *  the name of the image that is already present on the device in the Samples directory
//                 *
//                 *  Example : $lbsoybeans, will run a watershed light box algorithm on a soybeans
//                 *            sample image that is present in the OneKK/Photos/Samples directory
//                 */
//
//                if (input.charAt(0) == '$') {
//                    outputFileUri = Uri.fromFile(new File(Constants.PHOTO_SAMPLES_PATH.toString() + "/" + input.substring(3) + ".jpg"));
//                    inputText.setText(input.substring(3) + r.nextInt(200));
//                    switch (input.substring(1, 3)) {
//                        case "kk":
//                            imageAnalysis(outputFileUri);
//                            mCamera.startPreview();
//                            break;
//                        default:
//                            imageAnalysisLB(outputFileUri);
//                            mCamera.startPreview();
//                    }
//                } else {
//                    outputFileUri = storeRawPicture(picData);
//                    if (mScaleSteps == 2) {
//                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
//                        alertDialog.setTitle("Weight Capture");
//                        alertDialog.setMessage("Please put seeds on the scale to update the seeds weight");
//                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        data.updateSampleWeight(sampleName, weightData);
//                                        dialog.dismiss();
//                                    }
//                                });
//                        alertDialog.show();
//                    }
//                    imageAnalysisLB(outputFileUri);
//                    mCamera.startPreview();
//                }
//            } else {
//
//                outputFileUri = storeRawPicture(picData);
//                imageAnalysisLB(outputFileUri);
//                mCamera.startPreview();
//            }
//        }
//    };*/

    private Uri storeRawPicture(byte[] data) {
        String fileName;
        if (picName.length() > 0) {
            fileName = picName + "_";
        } else {
            fileName = "temp_";
        }
        //Generates a Media file with specified type and name
        File pictureFile = Utils.getOutputMediaFile(MEDIA_TYPE_IMAGE, fileName);
        try {
            //write image file
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        //Makes the saved file discoverable in Gallery/File Manager
        makeFileDiscoverable(pictureFile, MainActivity.this);

        return Uri.fromFile(pictureFile);
    }

    /************************************************************************************
     * image analysis method call to perform the default processing MeasureSeeds
     ************************************************************************************/
   /* private void imageAnalysis(Uri photo) {
        photoPath = photo.getPath();
        photoName = photo.getLastPathSegment();

        sampleName = inputText.getText().toString();

        if (mConnected) {
            weightData = mWeightEditText.getText().toString();
        } else {
            weightData = "null";
        }

        inputText.setText("");
        mWeightEditText.setText("0");

        double refDiam = Double.valueOf(ep.getString(SettingsFragment.COIN_SIZE, "1")); // Wheat default

        ImageProcess imgP = new ImageProcess(photoPath, photoName, refDiam, true, Double.valueOf(ep.getInt(SettingsFragment.MIN_SEED_VALUE, 0)), Double.valueOf(ep.getInt(SettingsFragment.MAX_SEED_VALUE, 0))); //TODO the min/max sizes are bad

        makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), this);

        seedCount = imgP.getSeedCount();

        data.addRecords(sampleName, photoName, firstName, lastName, seedCount, weightData, imgP.getSeedList());// Add the current record to the table
        data.createNewTableEntry(sampleName, String.valueOf(seedCount));

        data.getLastData();

        if (ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false)) {
            postImageDialog(this, photoName, seedCount);
        }
    }
*/
    /************************************************************************************
     * image analysis method call to perform WatershedLB, the algorithmName parameter can
     * be used to extend different algorithm implementations using the same function call
     ***********************************************************************************
     */
//    private void imageAnalysisLB(final Uri photo) {
//        photoPath = photo.getPath();
//        photoName = photo.getLastPathSegment();
//        r = new Random();
//
//        sampleName = inputText.getText().toString();
//
//        if (mConnected) {
//            weightData = mWeightEditText.getText().toString();
//        } else {
//            weightData = "null";
//        }
//
//        inputText.setText("");
//        //mWeightEditText.setText("0");
//        inputText.requestFocus();
//
//        /* get user settings from shared preferences */
//        final String firstName = ep.getString(SettingsFragment.FIRST_NAME, "first_name");
//        final String lastName = ep.getString(SettingsFragment.LAST_NAME, "last_name");
//        final double coinSize = Double.valueOf(ep.getString(SettingsFragment.COIN_NAME, "-1"));
//        final Boolean showAnalysis = ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false);
//        final Boolean backgroundProcessing = ep.getBoolean(SettingsFragment.ASK_BACKGROUND_PROCESSING, false);
//        final Boolean multiProcessing = ep.getBoolean(SettingsFragment.ASK_MULTI_PROCESSING, false);
//        /*final int areaLow = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_LOW, "400"));
//        final int areaHigh = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_HIGH, "160000"));
//        final int defaultRate = Integer.valueOf(ep.getString(SettingsFragment.PARAM_DEFAULT_RATE, "34"));
//        final double sizeLowerBoundRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_SIZE_LOWER_BOUND_RATIO, "0.25"));
//        final double newSeedDistRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_NEW_SEED_DIST_RATIO, "4.0"));
//        */
//
//        final Bitmap inputBitmap = BitmapFactory.decodeFile(photoPath);
//        Log.d("CoreProcessing : Begin", Utils.getDate());
//
//        /* set Watershed parameters to be passed to the actual algorithm */
//        //final WatershedLB.WatershedParams params = new WatershedLB.WatershedParams(areaLow, areaHigh, defaultRate, sizeLowerBoundRatio, newSeedDistRatio);
//        //mSeedCounter = new WatershedLB(params);
//
//        final CoreProcessingTask coreProcessingTask = new CoreProcessingTask(MainActivity.this,
//                photoName, showAnalysis, sampleName, firstName, lastName, weightData, r.nextInt(20000), backgroundProcessing, coinSize);
//
//        if (multiProcessing)
//            coreProcessingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputBitmap);
//        else
//            coreProcessingTask.execute(inputBitmap);
//        data.getLastData();
//    }

    /*private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }*/

    private void analysisChoosingPhoto(final String path, final String name) {
        final AlertDialog.Builder samplePreviewAlert = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        final View personView = inflater.inflate(R.layout.post_image, new LinearLayout(MainActivity.this), false);
        final TextView tv = (TextView) personView.findViewById(R.id.tvSeedCount);
        Typeface myTypeFace = Typeface.createFromAsset(MainActivity.this.getAssets(), "AllerDisplay.ttf");
        tv.setTypeface(myTypeFace);
        tv.setText(name);
        //File imgFile = new File(Constants.PHOTO_SAMPLES_PATH, sampleName + ".jpg");
        File imgFile = new File(path);

        if (imgFile.exists()) {
            TouchImageView imgView = (TouchImageView) personView.findViewById(R.id.postImage);
            Bitmap bmImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rbmImg = Bitmap.createBitmap(bmImg, 0, 0, bmImg.getWidth(), bmImg.getHeight(), matrix, true);
            imgView.setImageBitmap(rbmImg);
        }

        samplePreviewAlert.setCancelable(true);
        samplePreviewAlert.setView(personView);
        samplePreviewAlert.setPositiveButton(MainActivity.this.getResources().getString(R.string.analyze), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Uri outputFileUri = Uri.fromFile(new File(Constants.PHOTO_SAMPLES_PATH.toString() + "/" + name + ".jpg"));
                Uri outputFileUri = Uri.fromFile(new File(path));
                //imageAnalysisLB(outputFileUri);
            }
        });
        samplePreviewAlert.setNegativeButton(MainActivity.this.getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        samplePreviewAlert.show();
    }

    /**
     * This method lets the user run the analysis on some sample images that come along with the app
     */
    private void samplesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Select a Sample");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.select_dialog_item);
        arrayAdapter.add("Soybean");
        arrayAdapter.add("Maize");
        arrayAdapter.add("Wheat");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String sampleName = arrayAdapter.getItem(which).toLowerCase();
                String path = Constants.PHOTO_SAMPLES_PATH + "/" + sampleName + ".jpg";
                analysisChoosingPhoto(path, sampleName);
                /*final AlertDialog.Builder samplePreviewAlert = new AlertDialog.Builder(MainActivity.this);

                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                final View personView = inflater.inflate(R.layout.post_image, new LinearLayout(MainActivity.this), false);
                final TextView tv = (TextView)personView.findViewById(R.id.tvSeedCount);
                Typeface myTypeFace = Typeface.createFromAsset(MainActivity.this.getAssets(), "AllerDisplay.ttf");
                tv.setTypeface(myTypeFace);
                tv.setText(sampleName);
                File imgFile = new File(Constants.PHOTO_SAMPLES_PATH, sampleName + ".jpg");

                if (imgFile.exists()) {
                    TouchImageView imgView = (TouchImageView) personView.findViewById(R.id.postImage);
                    Bitmap bmImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap rbmImg = Bitmap.createBitmap(bmImg, 0, 0, bmImg.getWidth(), bmImg.getHeight(), matrix, true);
                    imgView.setImageBitmap(rbmImg);
                }

                samplePreviewAlert.setCancelable(true);
                samplePreviewAlert.setView(personView);
                samplePreviewAlert.setPositiveButton(MainActivity.this.getResources().getString(R.string.analyze), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Uri outputFileUri = Uri.fromFile(new File(Constants.PHOTO_SAMPLES_PATH.toString() + "/" + sampleName + ".jpg"));
                        imageAnalysisLB(outputFileUri);
                    }
                });
                samplePreviewAlert.setNegativeButton(MainActivity.this.getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                samplePreviewAlert.show();*/
            }
        });
        builder.show();
    }

    private void aboutDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.about, new LinearLayout(this), false);
        TextView version = personView.findViewById(R.id.tvVersion);
        TextView otherApps = personView.findViewById(R.id.tvOtherApps);


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
                return;
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
            TextView textView = single_row.findViewById(R.id.txt);
            ImageView imageView = single_row.findViewById(R.id.img);
            textView.setText(color_names[position]);
            imageView.setImageResource(image_id[position]);
            return single_row;
        }
    }

    public void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void setPersonDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View personView = inflater.inflate(R.layout.person, new LinearLayout(this), false);

        final EditText fName = personView
                .findViewById(R.id.firstName);
        final EditText lName = personView
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
        //releaseCamera(); // release the camera immediately on pause event

        //deal bluetooth callback function
       // unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public void onInit(int status) {
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        /* THIS IS TO DISABLE back button to close MainActivity */
    }

    //start scale part
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothLeService mBluetoothLeService;

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new MyRunnable();
    private final long mDelayTime = 1000;

    private void scaleBluetoothInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Not Support LE Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not support", Toast.LENGTH_SHORT).show();
        }

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device != null) {
                                String deviceName = device.getName();
                                Log.i(TAG, "=====device name=====" + deviceName);
                                if (deviceName != null && deviceName.toLowerCase().startsWith("ohbt")) {
                                    Log.i(TAG, "Find device name start with ohbt");

                                    mBluetoothDeviceName = deviceName;
                                    mBluetoothDeviceAddress = device.getAddress();

                                    mBluetoothDevice = device;
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                                }
                            }
                        }
                    });
                }
            };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mBluetoothDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                mConnected = true;
                //updateConnectionState("Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState("Disconnected");
                //mDataField.setText("No data");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                try {
                    Thread.currentThread();
                    Thread.sleep(mDelayTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNotifyCharacteristic = mBluetoothLeService.enableNotifications(BluetoothLeService.UUID_CHARACTERISTIC, true);

                try {
                    Thread.currentThread();
                    Thread.sleep(mDelayTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mNotifyCharacteristic != null) {
                    mNotifyCharacteristic.setValue("0P\r\nI2\r\nPM\r\nHD11\r\n");
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic);

                    try {
                        Thread.currentThread();
                        Thread.sleep(mDelayTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.postDelayed(mRunnable, mDelayTime);
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    /*private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }*/

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private StringBuffer weightDataBuffer = new StringBuffer(256);
    private int weightDataIndex;
    private String weightUnit;
    private int spaceIndex;
    //private String weightPackage;
    private String weightData;

    private void displayData(String data) {
        if (data != null) {
            this.weightDataBuffer.append(data);
            this.weightDataIndex = this.weightDataBuffer.indexOf("\r\n");
            Log.d(TAG, "Scout:" + this.weightDataBuffer + this.weightDataBuffer.length());
            if (this.weightDataIndex >= 2) {
                if (this.weightDataIndex == 24 && this.weightDataBuffer.charAt(0) == '\u0002') {
                    this.weightData = this.weightDataBuffer.substring(this.weightDataIndex - 23, this.weightDataIndex - 11);
                    this.weightUnit = this.weightDataBuffer.substring(this.weightDataIndex - 11, this.weightDataIndex - 6);
                    this.spaceIndex = this.weightData.indexOf(32);
                    while (this.spaceIndex != -1) {
                        if (this.spaceIndex == 0) {
                            this.weightData = this.weightData.substring(1, this.weightData.length());
                        } else {
                            this.weightData = this.weightData.substring(0, this.weightData.length() - 1);
                        }
                        this.spaceIndex = this.weightData.indexOf(32);
                    }
                    this.mWeightEditText.setText(this.weightData);
                    this.spaceIndex = this.weightUnit.indexOf(32);
                    while (this.spaceIndex != -1) {
                        if (this.spaceIndex == 0) {
                            this.weightUnit = this.weightUnit.substring(1, this.weightUnit.length());
                        } else {
                            this.weightUnit = this.weightUnit.substring(0, this.weightUnit.length() - 1);
                        }
                        this.spaceIndex = this.weightData.indexOf(32);
                    }
                    /*this.mUnitField.setText(this.weightUnit);
                    this.weightPackage = this.weightDataBuffer.substring(this.weightDataIndex - 22, this.weightDataIndex - 1);
                    this.errorStrIndex = this.weightDataBuffer.indexOf("?");
                    if (this.errorStrIndex >= 0) {
                        this.mStable.setVisibility(4);
                        this.bLatestStableState = false;
                    } else {
                        this.mStable.setVisibility(0);
                        if (!(this.outStreamOhaus == null || this.bLatestStableState)) {
                            try {
                                this.outStreamOhaus.write(this.weightDataBuffer.toString().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        this.bLatestStableState = true;
                    }
                    this.errorStrIndex = this.weightDataBuffer.indexOf("Z");
                    if (this.errorStrIndex > 0) {
                        this.mZeroPointMark.setText(">0<");
                    } else {
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightPackage.indexOf("N") >= 0) {
                        this.mNet.setText("NET");
                    } else if (this.weightPackage.indexOf("PT") >= 0) {
                        this.mNet.setText("PT");
                    } else {
                        this.mNet.setText("");
                    }
                } else {
                    if (this.weightDataBuffer.indexOf("Err 8.3") >= 0) {
                        this.mDataField.setText("Err 8.3");
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightDataBuffer.indexOf("Err 8.4") >= 0) {
                        this.mDataField.setText("Err 8.4");
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightDataBuffer.indexOf("ES") >= 0) {
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                        this.mUnitField.setText("");
                    }*/
                    if (this.weightDataBuffer.indexOf("I2 A ") >= 0) {
                        if (this.weightDataBuffer.length() > 17) {
                            this.weightData = this.weightDataBuffer.substring(this.weightDataBuffer.indexOf("\"") + 1, this.weightDataBuffer.length());
                            this.weightData = this.weightData.substring(0, this.weightData.indexOf(" "));
                            //this.mScaleModel.setText(this.weightData);
                            //this.mScaleModel.setTextColor(-7829368);
                            if (this.mNotifyCharacteristic != null) {
                                this.mNotifyCharacteristic.setValue("I4\r\n");
                                this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                                if (this.weightDataIndex >= 0) {
                                    this.weightDataBuffer.setLength(0);
                                    return;
                                }
                                return;
                            }
                        } else if (this.mNotifyCharacteristic != null) {
                            this.mNotifyCharacteristic.setValue("I2\r\n");
                            this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                        }
                    }
                    if (this.weightDataBuffer.indexOf("I4 A ") >= 0) {
                        if (this.weightDataBuffer.length() > 9) {
                            this.weightData = this.weightDataBuffer.substring(this.weightDataBuffer.indexOf("\"") + 1, this.weightDataBuffer.length());
                            this.weightData = this.weightData.substring(0, this.weightData.indexOf("\""));
                            //this.mScaleSN.setTextColor(-7829368);
                            //this.mScaleSN.setText(this.weightData);
                        } else if (this.mNotifyCharacteristic != null) {
                            this.mNotifyCharacteristic.setValue("I4\r\n");
                            this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                        }
                    }
                }
                if (this.mNotifyCharacteristic != null) {
                    this.mNotifyCharacteristic.setValue("BP\r\n");
                    this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                }
                this.mHandler.removeCallbacks(this.mRunnable);
                this.mHandler.postDelayed(this.mRunnable, mDelayTime);
            }
            if (this.weightDataIndex >= 0) {
                this.weightDataBuffer.setLength(0);
            }
        }
    }

    class MyRunnable implements Runnable {

        public void run() {
            if (mNotifyCharacteristic != null) {
                mNotifyCharacteristic.setValue("HD11\r\nPM\r\n");
                mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic);
            }
            mHandler.postDelayed(this, mDelayTime);
        }
    }
}