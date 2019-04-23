package org.wheatgenetics.onekk;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
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
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.features2d.Params;
import org.wheatgenetics.database.Data;
import org.wheatgenetics.database.MySQLiteHelper;
import org.wheatgenetics.imageprocess.ColorThreshold.ColorThresholding;
import org.wheatgenetics.imageprocess.ImageProcess;
import org.wheatgenetics.onekkUtils.Constants;
import org.wheatgenetics.onekkUtils.oneKKUtils;
import org.wheatgenetics.ui.CameraPreview;
import org.wheatgenetics.ui.TouchImageView;
import org.wheatgenetics.ui.guideBox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static org.wheatgenetics.onekkUtils.oneKKUtils.makeFileDiscoverable;
import static org.wheatgenetics.onekkUtils.oneKKUtils.postImageDialog;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    public final static String TAG = "OneKK";
    public static final int MEDIA_TYPE_IMAGE = 1;
    //request message of getting the image path
    public static final int GET_PATH_REQUEST = 3;
    public static final int GET_WEIGHT_REQUEST = 4;
    //setting information handler
    private SharedPreferences ep;

    private Data data;
    private static UsbDevice mDevice;

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

    private guideBox gb;
    private CoinRecognitionTask coinRecognitionTask;
    private FrameLayout preview;

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private CameraPreview mPreview;

    private LinearLayout parent;
    private ScrollView changeContainer;
    private String sampleName;
    //private String weight;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Random r;

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
        /* setup navigation click listener*/
        setupDrawerContent(nvDrawer);
        setupDrawer();

        ep = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        TableLayout lastSampleTable = (TableLayout) findViewById(R.id.lastSampleTable);
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

        ImageButton cameraButton = (ImageButton) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new ImageButton.OnClickListener(
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
        });

        createDirs();
        data = new Data(MainActivity.this, lastSampleTable);
        data.getLastData();


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
            launchIntro();
            ed.putBoolean("onlyLoadTutorialOnce", true);
            ed.apply();
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

                    if (count != 0) {
                        ratio = ((double) height1) / ((double) width1);
                    }
                }

                if (bottom < oldBottom) {
                    int newWidth = (int) (bottom / ratio);
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(newWidth, bottom);
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    lp.addRule(RelativeLayout.ALIGN_TOP);
                    preview.setLayoutParams(lp);
                }

                if (oldBottom < bottom) {
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width1, height1);
                    preview.setLayoutParams(lp);
                }

                //makeToast(String.valueOf(bottom) + " " + String.valueOf(oldBottom));
                //makeToast(String.valueOf(preview.getLength()));
            }
        });

        scaleBluetoothInit();
    }

    private void launchIntro() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                //  Launch app intro
                final Intent i = new Intent(MainActivity.this, IntroActivity.class);

                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        startActivity(i);
                    }
                });


            }
        }).start();
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
                oneKKUtils.makeFileDiscoverable(blankFile, this);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            /* copy sample images when the directory is created */
            if(path.compareTo(Constants.PHOTO_SAMPLES_PATH) == 0)
                copySampleImages();
        }
    }

    /** copy sample images into the directory OneKK/Photos/Samples */
    private void copySampleImages(){

        AssetManager assetManager = getAssets();

            String[] files = {"gwheat.jpg","nsoybeans.jpg","soybeans.jpg","wheat.jpg","silphium.jpg","maize.jpg"};

            for(String filename : files) {
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
                } catch(IOException e) {
                    Log.e(TAG, "Failed to copy asset file: " + filename, e);
                }
            }
    }

    /** used by copySampleImages method */
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

    @SuppressWarnings("deprecation")
    private void startCamera() {
        mCamera = getCameraInstance();

        PackageManager pm = getPackageManager();
        Camera.Parameters params = mCamera.getParameters();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            //Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }

        //fix bug: the photo size is larger than preview size
        //Use the biggest preview size: 1920 X 1440
        Camera.Size previewSize = params.getSupportedPreviewSizes().get(0);
        for (Camera.Size size: params.getSupportedPreviewSizes()) {
            if (size.width >= 1024 && size.height > 1024) {
                previewSize = size;
                Log.i("MainActivity", "Preview Size: " + size.width + " " + size.height);
                break;
            }

        }
        params.setPreviewSize(previewSize.width, previewSize.height);

        //If set photo size as same as preview size, the photo size will smaller than expect, so we do not need to set photo size.
        /*Camera.Size picSize = params.getSupportedPictureSizes().get(0);
        for (Camera.Size size: params.getSupportedPictureSizes()) {
            if (size.width == previewSize.width && size.height == previewSize.height) {
                picSize = size;
                break;
            }
        }
        params.setPictureSize(picSize.width, picSize.height);*/

        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);
        gb = new guideBox(this, Integer.parseInt(ep.getString(SettingsFragment.COIN_SIZE, "4")));
        preview.addView(gb, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /* Uncomment the below line to enable naive custom real time coin recognition */
        //previewThread.start();
    }

    /* Naive custom real time coin recognition */
    //TODO see if there are other optimized possibilities to handle this operation
    /* A new thread to handle the camera preview callback.
     *
     *  This thread creates a new camera preview callback and processes the current frame every
     *  2 seconds and tries to determine the contours of the four coins and display the
     *  discovered coordinates on the preview
     *
     *  WARNING : If this feature is enabled make sure the processing is also done using
     *            THREAD POOL EXECUTOR
     */
    /*Thread previewThread = new Thread(new Runnable() {
        @Override
        public void run() {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {

                    Camera.Parameters parameters = camera.getParameters();

                    int h = parameters.getPreviewSize().height;
                    int w = parameters.getPreviewSize().width;

                    coinRecognitionTask = new CoinRecognitionTask(w, h, gb);

                    coinRecognitionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);

                    try {
                        Log.d("Camera Preview thread", "Sleeping for 1 sec");
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        Log.e("Camera Preview thread", ex.toString());
                    }
                }
            });
        }
    });*/

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
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == GET_PATH_REQUEST) {
            if (resultCode == RESULT_OK) {
                /*String path = data.getStringExtra("ImagePath");
                int index = path.lastIndexOf('/');
                String sampleName = path.substring(index + 1, path.length() - 4);
                analysisChoosingPhoto(path, sampleName);*/

                Uri imageUri = data.getData();
                String path = getRealPathFromURI(imageUri);
                Log.e("error", "======" + path);
                //Log.e("error", "choose====" + path);
                /*Intent intent = new Intent();
                intent.putExtra("ImagePath", path);
                setResult(RESULT_OK, intent);
                finish();*/
            }
        } /*else if (requestCode == GET_WEIGHT_REQUEST) {
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
            /* Real time coin recognition based on OpenCV camera */
            /*case R.id.nav_coinrecognition:
                final Intent cameraActivityIntent = new Intent(this,CameraActivity.class);
                startActivity(cameraActivityIntent);
                break;*/

            case R.id.nav_samples:
                samplesDialog();
                break;

            case R.id.nav_settings:
                final Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
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
            /*case R.id.nav_scaleConnect:
                findScale();
                break;

            case R.id.nav_report:
                final Intent reportActivityIntent = new Intent(this,ReportActivity.class);
                startActivity(reportActivityIntent);
                break;*/
            /*
            //we already move the RecordWeightActivity code to MainActivity
            case R.id.record_weight:
                final Intent recordWeight = new Intent(this, RecordWeightActivity.class);
                //startActivity(recordWeight);
                startActivityForResult(recordWeight, GET_WEIGHT_REQUEST);
                break;*/
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

        ////////////////////////////
        //deal bluetooth connection
        try {
            unbindService(mServiceConnection);
        } catch (Exception e){
            e.printStackTrace();
        }

        mHandler.removeCallbacks(mRunnable);
        mBluetoothLeService = null;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        ////////////////////////////
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

        ///////////////////////////
        //deal bluetooth connection
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
        ///////////////////////////

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


    PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //Naive custom real time coin recognition
            /*
            ArrayList<Point> cornerArrayList = null;

            Camera.Parameters parameters = camera.getParameters();

            int h = parameters.getPreviewSize().height;
            int w = parameters.getPreviewSize().width;

            coinRecognitionTask = new CoinRecognitionTask(w,h);
            coinRecognitionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,data);

            try {
                cornerArrayList = coinRecognitionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,data).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("AsyncTask",e.getMessage());
            } catch (ExecutionException e) {
                e.printStackTrace();
                Log.e("AsyncTask",e.getMessage());
            }

            Log.d("Corner Array list",cornerArrayList.toString());
            if(cornerArrayList.size() != 4){
                Toast.makeText(MainActivity.this,"Couldn't detect all the coins, adjust and try again",Toast.LENGTH_LONG).show();
                mCamera.startPreview();
            }
            else {
            */

            /*if (ep.getBoolean(SettingsFragment.ASK_PROCESSING_TECHNIQUE, true))
                processingTechniqueDialog();*/

            Uri outputFileUri;
            String input;

            if (inputText.getText().length() != 0) {
                input = inputText.getText().toString();

                /* This section of code is just a hack to run already stored sample images for UI testing
                 *
                 *  In the sample name input box the developer can enter $ followed by,
                 *  either kk or lb to run different algorithms, followed by
                 *  the name of the image that is already present on the device in the Samples directory
                 *
                 *  Example : $lbsoybeans, will run a watershed light box algorithm on a soybeans
                 *            sample image that is present in the OneKK/Photos/Samples directory
                 */

                if (input.charAt(0) == '$') {
                    outputFileUri = Uri.fromFile(new File(Constants.PHOTO_SAMPLES_PATH.toString() + "/" + input.substring(3) + ".jpg"));
                    inputText.setText(input.substring(3) + r.nextInt(200));
                    switch (input.substring(1, 3)) {
                        case "kk":
                            imageAnalysis(outputFileUri);
                            mCamera.startPreview();
                            break;
                        default:
                            imageAnalysisLB(outputFileUri);
                            mCamera.startPreview();
                    }
                } else {
                    outputFileUri = storeRawPicture(data);
                    imageAnalysisLB(outputFileUri);
                    mCamera.startPreview();
                }
            } else {
                outputFileUri = storeRawPicture(data);
                imageAnalysisLB(outputFileUri);
                mCamera.startPreview();
            }
        }
    };

    private Uri storeRawPicture(byte[] data) {
        String fileName;
        if (picName.length() > 0) {
            fileName = picName + "_";
        } else {
            fileName = "temp_";
        }
        //Generates a Media file with specified type and name
        File pictureFile = oneKKUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE, fileName);
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

    /** NOT IMPLEMENTED
     * displays a dialogue after capturing the image, prompting the user to select a
     * processing technique, only if "Processing" -> "Always Ask" setting is checked
     * else proceed with the default technique set in the settings panel
     */
    public void processingTechniqueDialog() {
        final ArrayList mSelectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Processing technique")

                .setSingleChoiceItems(R.array.processing_techniques, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.v(TAG, which + "");
                            }
                        })

                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "clicked");
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "clicked negative");
                    }
                });

        builder.create().show();
    }

    /************************************************************************************
     * image analysis method call to perform the default processing MeasureSeeds
     ************************************************************************************/
    private void imageAnalysis(Uri photo) {
        photoPath = photo.getPath();
        photoName = photo.getLastPathSegment();

        sampleName = inputText.getText().toString();

        if (mDevice == null
                && mWeightEditText.getText().toString().equals("Not connected")) {
            weightData = "null";
        } else {
            weightData = mWeightEditText.getText().toString();
        }

        inputText.setText("");
        mWeightEditText.setText("0");

        double refDiam = Double.valueOf(ep.getString(SettingsFragment.COIN_SIZE, "1")); // Wheat default

        ImageProcess imgP = new ImageProcess(photoPath, photoName, refDiam, true, Double.valueOf(ep.getInt(SettingsFragment.MIN_SEED_VALUE, 0)), Double.valueOf(ep.getInt(SettingsFragment.MAX_SEED_VALUE, 0))); //TODO the min/max sizes are bad

        makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), this);

        seedCount = imgP.getSeedCount();

        data.addRecords(sampleName,photoName,firstName,lastName,seedCount,weightData,imgP.getSeedList());// Add the current record to the table
        data.createNewTableEntry(sampleName,String.valueOf(seedCount));

        data.getLastData();

        if (ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false)) {
            postImageDialog(this,photoName,seedCount);
        }
    }

    /************************************************************************************
     * image analysis method call to perform WatershedLB, the algorithmName parameter can
     * be used to extend different algorithm implementations using the same function call
     ***********************************************************************************
     */
    private void imageAnalysisLB(final Uri photo) {
        photoPath = photo.getPath();
        photoName = photo.getLastPathSegment();
        r = new Random();

        sampleName = inputText.getText().toString();


        if (mDevice == null && mWeightEditText.getText().toString().equals("Not connected")) {
            weightData = "null";
        } else {
            weightData = mWeightEditText.getText().toString();
        }

        inputText.setText("");
        mWeightEditText.setText("0");
        inputText.requestFocus();

        /* get user settings from shared preferences */
        final ColorThresholding.ColorThresholdParams colorThresholdParams;
        final String firstName = ep.getString(SettingsFragment.FIRST_NAME, "first_name");
        final String lastName = ep.getString(SettingsFragment.LAST_NAME, "last_name");
        final int lowerBound = ep.getInt(SettingsFragment.MIN_VALUE, 116);
        final int upperBound = ep.getInt(SettingsFragment.MAX_VALUE, 255);
        final int threshold = ep.getInt(SettingsFragment.THRESHOLD, 20);
        final Boolean colorThresholding = ep.getBoolean(SettingsFragment.COLOR_THRESHOLD, false);
        final double coinSize = Double.valueOf(ep.getString(SettingsFragment.COIN_NAME, "-1"));
        final Boolean showAnalysis = ep.getBoolean(SettingsFragment.DISPLAY_ANALYSIS, false);
        final Boolean backgroundProcessing = ep.getBoolean(SettingsFragment.ASK_BACKGROUND_PROCESSING, false);
        final Boolean multiProcessing = ep.getBoolean(SettingsFragment.ASK_MULTI_PROCESSING, false);
        /*final int areaLow = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_LOW, "400"));
        final int areaHigh = Integer.valueOf(ep.getString(SettingsFragment.PARAM_AREA_HIGH, "160000"));
        final int defaultRate = Integer.valueOf(ep.getString(SettingsFragment.PARAM_DEFAULT_RATE, "34"));
        final double sizeLowerBoundRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_SIZE_LOWER_BOUND_RATIO, "0.25"));
        final double newSeedDistRatio = Double.valueOf(ep.getString(SettingsFragment.PARAM_NEW_SEED_DIST_RATIO, "4.0"));
        */

        final Bitmap inputBitmap = BitmapFactory.decodeFile(photoPath);
        Log.d("CoreProcessing : Begin", oneKKUtils.getDate());

        /* set ColorThreshold parameters to be passed if opted in Settings */
        colorThresholdParams = colorThresholding ? new ColorThresholding.ColorThresholdParams(threshold, lowerBound, upperBound) : null;

        /* set Watershed parameters to be passed to the actual algorithm */
        //final WatershedLB.WatershedParams params = new WatershedLB.WatershedParams(areaLow, areaHigh, defaultRate, sizeLowerBoundRatio, newSeedDistRatio);
        //mSeedCounter = new WatershedLB(params);

        final CoreProcessingTask coreProcessingTask = new CoreProcessingTask(MainActivity.this, colorThresholdParams,
                photoName, showAnalysis, sampleName, firstName, lastName, weightData, r.nextInt(20000), backgroundProcessing, coinSize);

        if (multiProcessing)
            coreProcessingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputBitmap);
        else
            coreProcessingTask.execute(inputBitmap);
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

    private void analysisChoosingPhoto(final String path, final String name) {
        final AlertDialog.Builder samplePreviewAlert = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        final View personView = inflater.inflate(R.layout.post_image, new LinearLayout(MainActivity.this), false);
        final TextView tv = (TextView)personView.findViewById(R.id.tvSeedCount);
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
                imageAnalysisLB(outputFileUri);
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
    private void samplesDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Select a Sample");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.select_dialog_item);
        arrayAdapter.add("Soybeans");
        arrayAdapter.add("Maize");
        arrayAdapter.add("Silphium");
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

    public void setPersonDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
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

        //deal bluetooth callback function
        unregisterReceiver(mGattUpdateReceiver);
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

            Log.v(TAG, "hueProcess transfer");

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

    @Override
    public void onBackPressed() {
        /* THIS IS TO DISABLE back button to close MainActivity */
    }

    /////////////////////////////////
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
                                Log.e(TAG, "=====device name=====" + deviceName);
                                if (deviceName != null && deviceName.toLowerCase().startsWith("ohbt")) {
                                    Log.i(TAG, "Find device name start with ohbt");
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
            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
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
                    Thread.sleep( mDelayTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNotifyCharacteristic = mBluetoothLeService.enableNotifications(BluetoothLeService.UUID_CHARACTERISTIC, true);

                try {
                    Thread.currentThread();
                    Thread.sleep( mDelayTime);
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
                    mHandler.postDelayed(mRunnable,  mDelayTime);
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
    ////////////////////////////////
}