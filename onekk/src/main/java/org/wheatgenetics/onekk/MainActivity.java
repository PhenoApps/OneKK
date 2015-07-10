package org.wheatgenetics.onekk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.wheatgenetics.imageprocess.ImgProcess1KK;
import org.wheatgenetics.imageprocess.ImgProcess1KK.Seed;
import org.wheatgenetics.onekk.CSVWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener {

	public final static String TAG = "OneKK";
	public final static String PREFS = "PREFS";

	private UsbDevice mDevice;
	private double mWeightGrams = 0;
	private double mZeroGrams = 0;

	private String personID;
	String photoName;
	String cropNameStr = "";
	
	private EditText mWeightEditText;
	private EditText inputText;

	ArrayList<Seed> seeds;
	
	TextView sampleName;
	TextView numSeeds;
	TextView avgLength;
	TextView avgWidth;
	TextView sampleWeight;

	TextView boxNumTV;
	TextView envIDTV;
	TextView weightTV;
	TextView boxHeader;
	TextView itemHeader;
	TextView idHeader;

	private SharedPreferences ep;
	
	TableLayout OneKKTable;
	MySQLiteHelper db;
	String firstName = "";
	String lastName = "";
	List<SampleRecord> list;
	int itemCount;
	ScrollView sv1;
	static int currentItemNum = 1;
	private Camera mCamera;
	private CameraPreview mPreview;
	private String picName = null;
	public static final int MEDIA_TYPE_IMAGE = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.v(TAG, "onCreate");

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		ep = getSharedPreferences("Settings", 0);
		
		inputText = (EditText) findViewById(R.id.etInput);

		mWeightEditText = (EditText) findViewById(R.id.text_weight);
		mWeightEditText.setText("Not connected");

		sv1 = (ScrollView) findViewById(R.id.svData);
		OneKKTable = (TableLayout) findViewById(R.id.tlInventory);

		db = new MySQLiteHelper(this);

		OneKKTable.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			}
		});

		Intent intent = getIntent();
		mDevice = (UsbDevice) intent
				.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		inputText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					mgr.showSoftInput(inputText,
							InputMethodManager.HIDE_IMPLICIT_ONLY);
					if (event.getAction() != KeyEvent.ACTION_DOWN)
						return true;
					picName = inputText.getText().toString();
					takePic();
					goToBottom();
					inputText.requestFocus(); // Set focus back to Enter box
				}

				if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					}
					if (event.getAction() == KeyEvent.ACTION_UP) {
						picName = inputText.getText().toString();
						takePic();
						goToBottom();
					}
					inputText.requestFocus(); // Set focus back to Enter box
				}
				return false;
			}
		});

		mWeightEditText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
					InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					mgr.showSoftInput(inputText,
							InputMethodManager.HIDE_IMPLICIT_ONLY);
					if (event.getAction() != KeyEvent.ACTION_DOWN)
						return true;

					goToBottom();

					if (mDevice != null) {
						mWeightEditText.setText("");
					}
					inputText.requestFocus(); // Set focus back to Enter box
				}

				if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					}
					if (event.getAction() == KeyEvent.ACTION_UP) {
						goToBottom();
					}
					inputText.requestFocus(); // Set focus back to Enter box
				}
				return false;
			}
		});

		startCamera();
		// TODO don't forget to undo this
		// setPersonDialog();
		// findScale();
		createDirectory("OneKK");
		createDirectory("OneKK/Photos");
		createDirectory("OneKK/Export");
		createDirectory("OneKK/AnalyzedPhotos");
		parseDbToTable();
		goToBottom();
		
		if(ep.getString("cropName","").equals("")) {
			settingsDialog();
		}
		
		if (!OpenCVLoader.initDebug()) {
	    	mLoaderCallback.onManagerConnected(LoaderCallbackInterface.INIT_FAILED);
	    } else {
	    	mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	    }
		
	}
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
	    @Override
	    public void onManagerConnected(int status){
	        switch(status){
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i("onekk", "OpenCV loaded");
	            }break;
	            default:
	            {
	                super.onManagerConnected(status);
	            }break;
	        }   
	    }
	};

	private void startCamera() {
		mCamera = getCameraInstance();
		mCamera.setDisplayOrientation(90);
		
		Camera.Parameters params = mCamera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		mCamera.setParameters(params);
		
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

	}

	private void goToBottom() {
		sv1.post(new Runnable() {
			public void run() {
				sv1.fullScroll(ScrollView.FOCUS_DOWN);
				inputText.requestFocus();
			}
		});
	}

	//TODO
	private void parseDbToTable() {
		OneKKTable.removeAllViews();
		list = db.getAllSamples();
		itemCount = list.size();
		if (itemCount != 0) {
			for (int i = 0; i < itemCount; i++) {
				String[] temp = list.get(i).toString().split(",");

				Log.d(TAG, temp[0] + " " + temp[1] + " " + temp[2] + " "
						+ temp[3] + " " + temp[4] + " " + temp[5] + " "
						+ temp[6] + " " + temp[7]);

				createNewTableEntry(temp[0], temp[5],
						String.format("%.2f", Double.parseDouble(temp[6])),
						String.format("%.2f", Double.parseDouble(temp[7])),
						String.format("%.2f", Double.parseDouble(temp[8])));

			}
		}
	}

	/**
	 * Adds a new record to the internal list of records
	 */
	private void addRecord() {
		
		// Data manipulation
		String ut;
		String date = getDate();

		ut = inputText.getText().toString();
		if (ut.equals("")) {
			return; // check for empty user input
		}

		String weight;
		if (mDevice == null
				&& mWeightEditText.getText().toString().equals("Not connected")) {
			weight = "null";
		} else {
			weight = mWeightEditText.getText().toString();
		}
		
		// Add all measured seeds to database
		for(int j = 0; j < seeds.size(); j++){
		    db.addSeedRecord(new SeedRecord(inputText.getText().toString(),seeds.get(j).getLength(),seeds.get(j).getWidth(),seeds.get(j).getCirc(),"","",""));
	    }
		
		// Calculate averages
		double avgLength = 0.0;
		double avgWidth = 0.0;
		double avgArea = 0.0;
		
		avgLength = db.averageSample(inputText.getText().toString(), "length");
		avgWidth = db.averageSample(inputText.getText().toString(), "width");
		avgArea = db.averageSample(inputText.getText().toString(), "area");
		
		// TODO Count seeds
		String seedCount = "0";
		
		// Add sample to database
		db.addSampleRecord(new SampleRecord(inputText.getText().toString(), photoName, personID, date, seedCount, weight, avgArea, avgLength, avgWidth));
		
		// Round values for UI
		String avgLengthStr = String.format("%.2f", avgLength);
		String avgWidthStr = String.format("%.2f", avgWidth);

		createNewTableEntry(inputText.getText().toString(),"0", avgLengthStr,avgWidthStr,weight);
		currentItemNum++;
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private String getDate() {
		String date = "";
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		date = Integer.toString(year) + "-" + Integer.toString(month) + "-"
				+ Integer.toString(day);
		return date;
	}

	/**
	 * Adds a new entry to the end of the TableView
	 */
	private void createNewTableEntry(String sample, String seedCount, String avgL, String avgW, String wt) {
		inputText.setText("");
		String tag = sample;
		
		/* Create a new row to be added. */
		TableRow tr = new TableRow(this);
		tr.setLayoutParams(new TableLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		/* Create the sample name field */
		sampleName = new TextView(this);
		sampleName.setGravity(Gravity.CENTER | Gravity.BOTTOM);
		sampleName.setTextColor(Color.BLACK);
		sampleName.setTextSize(20.0f);
		sampleName.setText(sample);
		sampleName.setTag(sample);
		sampleName.setLayoutParams(new TableRow.LayoutParams(0,
				LayoutParams.WRAP_CONTENT, 0.4f));

		/* Create the number of seeds field */
		numSeeds = new TextView(this);
		numSeeds.setGravity(Gravity.CENTER | Gravity.BOTTOM);
		numSeeds.setTextColor(Color.BLACK);
		numSeeds.setTextSize(20.0f);
		numSeeds.setText("5");
		numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
				LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the length field */
		avgLength = new TextView(this);
		avgLength.setGravity(Gravity.CENTER | Gravity.BOTTOM);
		avgLength.setTextColor(Color.BLACK);
		avgLength.setTextSize(20.0f);
		avgLength.setText(avgL);
		avgLength.setLayoutParams(new TableRow.LayoutParams(0,
				LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the width field */
		avgWidth = new TextView(this);
		avgWidth.setGravity(Gravity.CENTER | Gravity.BOTTOM);
		avgWidth.setTextColor(Color.BLACK);
		avgWidth.setTextSize(20.0f);
		avgWidth.setText(avgW);
		avgWidth.setLayoutParams(new TableRow.LayoutParams(0,
				LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the area field */
		sampleWeight = new TextView(this);
		sampleWeight.setGravity(Gravity.CENTER | Gravity.BOTTOM);
		sampleWeight.setTextColor(Color.BLACK);
		sampleWeight.setTextSize(20.0f);
		sampleWeight.setText(wt);
		sampleWeight.setLayoutParams(new TableRow.LayoutParams(0,
				LayoutParams.WRAP_CONTENT, 0.12f));

		/* Define the listener for the longclick event */
		sampleName.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				final String tag = (String) v.getTag();
				deleteDialog(tag);
				return false;
			}
		});

		/* Add UI elements to row and add row to table */
		tr.addView(sampleName);
		tr.addView(numSeeds);
		tr.addView(avgLength);
		tr.addView(avgWidth);
		tr.addView(sampleWeight);
		OneKKTable.addView(tr, 0, new LayoutParams( // Adds row to top of table
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.MATCH_PARENT));
	}

	private void deleteDialog(String tag) {
		final String sampleName = tag;

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Delete Entry");
		builder.setMessage("This will delete all samples and seeds associated with \"" + sampleName + "\". Do you wish to proceed?")
				.setCancelable(true)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								db.deleteSample(sampleName);
								parseDbToTable();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	private void createDirectory(String file) {
		try {
			File myFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + file + "/");
			if (!myFile.exists()) {
				if (!myFile.mkdirs()) {
					makeToast("Problem making directories");
					makeFileDiscoverable(myFile, MainActivity.this);
				}
			}
		} catch (Exception e) {
			Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT)
					.show();
		}
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
		getMenuInflater().inflate(R.menu.action_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scaleConnect:
			findScale();
			break;
		case R.id.person:
			setPersonDialog();
			break;
		case R.id.settings:
			settingsDialog();
			break;
		case R.id.export:
			exportDialog();
			break;
		case R.id.clearData:
			clearDialog();
			break;
		case R.id.help:
			helpDialog();
			break;
		case R.id.about:
			aboutDialog();
			break;
		}

		Log.i(TAG, String.format("item selected %s", item.getTitle()));
		return true;
	}

	private void settingsDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		final View settingsView = inflater.inflate(R.layout.settings, null);
		
		final Editor ed = ep.edit();
		
		final Spinner cropName = (Spinner) settingsView.findViewById(R.id.spCrops);
		final Switch analysisPreview = (Switch) settingsView.findViewById(R.id.swAnalysisPreview);
		final EditText refDiam = (EditText) settingsView.findViewById(R.id.etReferenceDiameter);
		final EditText expectLWR = (EditText) settingsView.findViewById(R.id.etExpectedLWR);
		final EditText minCirc = (EditText) settingsView.findViewById(R.id.etMinCirc);
		final EditText minSize = (EditText) settingsView.findViewById(R.id.etMinSize);
		
		refDiam.setText(ep.getString("refDiam", "1"));
		analysisPreview.setChecked(ep.getBoolean("analysisPreview", false));

		cropName.setSelection(getIndex(cropName, ep.getString("cropName","Wheat")));
		expectLWR.setText(ep.getString("expectLWR", ""));
		minCirc.setText(ep.getString("minCirc", ""));
		minSize.setText(ep.getString("minSize", ""));
		
		cropName.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				switch(arg2) {
				case 0: // Wheat
					expectLWR.setText("1.2");
					minCirc.setText("0.6");
					minSize.setText("30");
					break;
				case 1: // Maize
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 2: // Rice
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 3: // Sorghum
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 4: // Soybeans
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 5: // Canola
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 6:	// Canola
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 7: // Potato
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 8: // Cassava
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				case 9: // Custom
					expectLWR.setText("");
					minCirc.setText("");
					minSize.setText("");
					break;
				}
				
				cropNameStr = cropName.getItemAtPosition(arg2).toString();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
		
		alert.setCancelable(false);
		alert.setTitle("Settings");
		alert.setView(settingsView);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {		
				ed.putString("refDiam", refDiam.getText().toString());
				ed.putString("cropName", cropNameStr);
				ed.putString("expectLWR", expectLWR.getText().toString());
				ed.putString("minCirc", minCirc.getText().toString());
				ed.putString("minSize", minSize.getText().toString());
				ed.putBoolean("analysisPreview", analysisPreview.isChecked());
				ed.commit();
			}
		});
		alert.show();		
	}
	
	private int getIndex(Spinner spinner, String myString)
	 {
	  int index = 0;

	  for (int i=0;i<spinner.getCount();i++){
	   if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
	    index = i;
	    i=spinner.getCount();//will stop the loop, kind of break, by making condition false
	   }
	  }
	  return index;
	 } 

	private void takePic() {
		inputText.setEnabled(false);
		mCamera.takePicture(null, null, mPicture);
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

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
			makeFileDiscoverable(pictureFile,MainActivity.this);
			imageAnalysis(outputFileUri);
			
			mCamera.startPreview();
			
			inputText.setEnabled(true);
			inputText.requestFocus();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(inputText, InputMethodManager.SHOW_IMPLICIT);
		}
	};

	/** Create a File for saving an image or video */
	private File getOutputMediaFile(int type) {

		File mediaStorageDir = new File(
				Environment.getExternalStorageDirectory(), "OneKK/Photos");

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("OneKK", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;

		String fileName = "";
		
		if (picName.length() > 0) {
			fileName = picName + "_";
		} else {
			fileName = "temp_";
		}

		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ fileName + "IMG_" + timeStamp + ".jpg");

		return mediaFile;

	}

	private void imageAnalysis(Uri photo) {
		photoName = photo.getLastPathSegment().toString();
		
		double refDiam = Double.valueOf(ep.getString("refDiam","1")); // Wheat default
		double expLWR = Double.valueOf(ep.getString("expectLWR","1.2")); // Wheat default
		double minCirc = Double.valueOf(ep.getString("minCirc","0.6"));  // Wheat default
		double minSize = Double.valueOf(ep.getString("minSize", "30"));  // Wheat default
		
	    ImgProcess1KK imgP = new ImgProcess1KK(Environment.getExternalStorageDirectory().toString()+ "/OneKK/Photos/" + photoName, refDiam, true, ep.getBoolean("symmetric",true)); //TODO get crop setting
	    imgP.writeProcessedImg(Environment.getExternalStorageDirectory().toString() + "/OneKK/AnalyzedPhotos/" + photoName + "_new.jpg");
	    
	    seeds = imgP.getList();
	    addRecord(); // Add the current record to the table
	    
	    if(ep.getBoolean("analysisPreview", false) == true) {
	    	postImageDialog(photoName);
	    };
	}
	
	private void postImageDialog(String imageName) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		LayoutInflater inflater = this.getLayoutInflater();
		final View personView = inflater.inflate(R.layout.post_image, null);
		
		File imgFile = new File(Environment.getExternalStorageDirectory().toString() + "/OneKK/AnalyzedPhotos/" + imageName + "_new.jpg");
		
		if(imgFile.exists()) {
			TouchImageView imgView = (TouchImageView) personView.findViewById(R.id.postImage);
			Bitmap bmImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			
			Matrix matrix = new Matrix();
		    matrix.postRotate(90);
		    Bitmap rbmImg = Bitmap.createBitmap(bmImg, 0, 0, bmImg.getWidth(), bmImg.getHeight(), matrix, true);
		      
			imgView.setImageBitmap(rbmImg);
		}
		
		alert.setCancelable(true);
		alert.setTitle("Analysis Preview");
		alert.setView(personView);
		alert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});
		alert.show();
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

	private void helpDialog() {
	}

	private void aboutDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		LayoutInflater inflater = this.getLayoutInflater();
		final View personView = inflater.inflate(R.layout.about, null);

		alert.setCancelable(true);
		alert.setTitle("About OneKK");
		alert.setView(personView);
		alert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});
		alert.show();
	}

	public void makeToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private void setPersonDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		final View personView = inflater.inflate(R.layout.person, null);
		final EditText fName = (EditText) personView
				.findViewById(R.id.firstName);
		final EditText lName = (EditText) personView
				.findViewById(R.id.lastName);
		fName.setText(firstName);
		lName.setText(lastName);

		alert.setCancelable(false);
		alert.setTitle("Set Person");
		alert.setView(personView);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				firstName = fName.getText().toString().trim();
				lastName = lName.getText().toString().trim();

				if (firstName.length() == 0 | lastName.length() == 0) {
					makeToast("Names cannot be left blank.");
					setPersonDialog();
					return;
				}

				String input = firstName + "_" + lastName;
				makeToast("Person set as: " + input);
				input = input.replace(" ", "_");
				personID = input.toLowerCase();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(lName.getWindowToken(), 0);
			}
		});
		alert.show();
	}

	private void clearDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(
				"This will delete all data from the database. Proceed?")
				.setCancelable(false)
				.setTitle("Clear Data")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								makeToast("Data deleted");
								dropTables();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void exportDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage("Export raw data or sample summaries?")
				.setCancelable(false)

				.setPositiveButton("Raw data",
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
				.setNeutralButton("All data",
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
				.setNegativeButton("Sample summaries",
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
				.setTitle("Export data")
				.setNeutralButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera(); // release the camera immediately on pause event
	}

	public void makeFileDiscoverable(File file, Context context) {
		MediaScannerConnection.scanFile(context,
				new String[] { file.getPath() }, null, null);
		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.fromFile(file)));
	}

	private void dropTables() {
		db.deleteAll();
		OneKKTable.removeAllViews();
		currentItemNum = 1;
	}
	
	public void exportDatabase(Cursor cursorForExport,String type) throws Exception {

		File file = null;
		
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1; // Months are numbered 0-11
		int day = c.get(Calendar.DAY_OF_MONTH);
		String date = year + "-" + month + "-" + day;

		if (year == 0) { // database is empty
			makeToast("Database currently empty.");
			return;
		}
		
		try {
			file = new File(Environment.getExternalStorageDirectory() + "/OneKK/Export/export_" + type + "_" + date + "_1.csv");
			if (file.exists()) {
				// Add number suffix if file exists
				for (int i = 2; i < 100; i++) 
				{
					String newDate = date;
					newDate += "_" + Integer.toString(i);
					file = new File(Environment.getExternalStorageDirectory() + "/OneKK/Export/export_" + type + "_" + newDate + ".csv");
					if (!file.exists()) {
						break;
					}
				}
			} else {
				
			}
		} catch (Exception e) {
			Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT)
					.show();
		}

		Cursor curCSV = cursorForExport;
		File exportDir = new File(Environment.getExternalStorageDirectory() + "/OneKK/Export/", "");

		try {
			file.createNewFile();
			CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
			csvWrite.writeNext(curCSV.getColumnNames());
			
			while (curCSV.moveToNext()) {
				String arrStr[]	= new String[curCSV.getColumnCount()];
						
				for(int k=0; k< curCSV.getColumnCount(); k++) {
					arrStr[k] = curCSV.getString(k);
				}

				csvWrite.writeNext(arrStr);
			}
			csvWrite.close();
			curCSV.close();
		} catch (Exception sqlEx) {
			Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
		}
		
		makeFileDiscoverable(file, MainActivity.this);
		shareFile(file.toString());
	}
       

	private void shareFile(String filePath) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
		try {
			startActivity(Intent.createChooser(intent, "Sending File..."));
		} finally {
		}
	}

	public void findScale() {
		if (mDevice == null) {
			UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
			HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			while (deviceIterator.hasNext()) {
				mDevice = deviceIterator.next();
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
			new ScaleListener().execute();
		} else {
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("Scale Not Found")
					.setMessage(
							"Please connect scale with OTG cable and turn the scale on")
					.setCancelable(false)
					.setPositiveButton("Try Again",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									findScale();
								}

							})
					.setNegativeButton("Ignore",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}

							}).show();
		}
	}

	private class ScaleListener extends AsyncTask<Void, Double, Void> {
		private byte mLastStatus = 0;
		private double mLastWeight = 0;

		@Override
		protected Void doInBackground(Void... arg0) {

			byte[] data = new byte[128];
			int TIMEOUT = 2000;
			boolean forceClaim = true;

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
			connection.claimInterface(intf, forceClaim);
			while (true) {

				int length = connection.bulkTransfer(endpoint, data,
						data.length, TIMEOUT);

				if (length != 6) {
					Log.e(TAG, String.format("invalid length: %d", length));
					return null;
				}

				byte report = data[0];
				byte status = data[1];
				byte exp = data[3];
				short weightLSB = (short) (data[4] & 0xff);
				short weightMSB = (short) (data[5] & 0xff);

				// Log.v(TAG, String.format(
				// "report=%x status=%x exp=%x lsb=%x msb=%x", report,
				// status, exp, weightLSB, weightMSB));

				if (report != 3) {
					Log.v(TAG, String.format("scale status error %d", status));
					return null;
				}

				if (mDevice.getProductId() == 519) {
					mWeightGrams = (weightLSB + weightMSB * 256.0) / 10.0;
				} else {
					mWeightGrams = (weightLSB + weightMSB * 256.0);
				}
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
				mLastStatus = status;
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
			Toast.makeText(getApplicationContext(), "Scale Disconnected",
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
	}

}
