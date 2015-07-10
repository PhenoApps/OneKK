package org.wheatgenetics.imageprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.*;
import org.wheatgenetics.imageprocess.ImgProcess1KK.Seed;

public class ImgProcess1KK {

	private String imageFILE; 
	private org.opencv.core.Mat image;
	
	private Scalar filterGreen[] = {new Scalar(0,0,0), new Scalar(40,255,255)};
	private Scalar filterBlue[] = {new Scalar(100,0,0), new Scalar(150,255,255)};
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	private Boolean symmetric = true;

	private List<MatOfPoint> refContours = new ArrayList<MatOfPoint>();
	private List<MatOfPoint> goodContours = new ArrayList<MatOfPoint>();
	
	ArrayList<Double> seedMatL = new ArrayList<Double>(); // major axis
	ArrayList<Double> seedMatW = new ArrayList<Double>(); // minor axis
	ArrayList<Double> seedMatP = new ArrayList<Double>(); // perimeter
	ArrayList<Double> seedMatA = new ArrayList<Double>(); // area
	
	private String[] countingMethods = {"length","width","perimeter","area"};
	private String countMethodChoice = null;
	
	private double refDiam; 
	private double pixelSize = 0; // pixel size in mm
	public boolean cropImage = true;
	
	private double expLWR; // expected seed length to width ratio
	private double minCirc; // expected minimum circularity of the seed
	private double minSize;
	private ArrayList<Seed> seedArray = new ArrayList<Seed>();
	
	public ImgProcess1KK(String inputFILE){
		System.out.println("WARNING: Reference diameter has not been set. \n" + "Ref Diameter: "+refDiam);
		imageFILE = inputFILE;
		
		this.initialize();
		this.processImage();
	}
	
	public String getCountingMethodChoice() {
		return countMethodChoice;
	}
	
	public void setCountingMethodChoice(String choice) {
		if(Arrays.asList(countingMethods).contains(choice)) {
			countMethodChoice = choice;
		} else {
			System.out.println("Not a valid counting choice");
		}
	}
	
	public ImgProcess1KK(String inputFILE, double refDiameter, boolean crop, boolean sym){
    	double start = System.currentTimeMillis();

    	imageFILE = inputFILE;
		refDiam = refDiameter;
		cropImage = crop;
		symmetric = sym;

		this.initialize();
		this.processImage();
    	
		double time = (System.currentTimeMillis() - start) / 1000 ;
    	System.out.println("Processed in : " + time + " sec"); 
	}
	
	
	private void initialize(){
		checkOS();
		image = Highgui.imread(imageFILE);
		System.out.println(String.format("Processing %s", imageFILE) + "\n");
		Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2HSV);
	}
	

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}
 
	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}
	
	private void checkOS() { 
		if (isWindows() || isMac()) {
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load the native library.
		}
	}
	
	public void processImage(){
		if(cropImage==true) {
			this.cropImage();
		}
		
		if(this.setReference()==true){
			this.countSeeds();
		}
	}
	
	/**
	 * Filters the image based on the green background
	 * Crops image using largest observable contour
	 * Removes extra 1.5% of image to ensure all edges removed
	 * 
	 */
	private void cropImage() {
		System.out.println("Cropping image...");
		
		Mat uncropped = filterBackground(image);
		
		if(Core.countNonZero(uncropped.row(1)) > uncropped.rows()*.6 &&
		   Core.countNonZero(uncropped.col(1)) > uncropped.cols()*.6 &&
		   Core.countNonZero(uncropped.row(uncropped.rows()-1)) > uncropped.rows()*.6 &&
		   Core.countNonZero(uncropped.col(uncropped.cols()-1)) > uncropped.cols()*.6) {
			
			System.out.println("IMAGE DOES NOT NEED TO BE CROPPED" + "\n");
			return;
		}

		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat croppedImage = image;

		Imgproc.findContours(uncropped, contours, hierarchy,
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS);
		double largest_area = 0;
		int largest_index = 0;
		
		for(int i=0; i<contours.size(); i++ ) {
			MatOfPoint cnt = new MatOfPoint(contours.get(i).toArray());
			double area = Imgproc.contourArea(cnt);
			if(area > largest_area) {
				largest_area = area;
				largest_index = i;
			}
		}
		
		//System.out.println(contours.size());
		
		MatOfPoint cnt = new MatOfPoint(contours.get(largest_index).toArray());
		Rect rec = Imgproc.boundingRect(cnt);
		croppedImage = image.submat(rec.y, rec.y + rec.height, rec.x, rec.x	+ rec.width);

		Mat mask_image = new Mat( image.size(), CvType.CV_8U);
		
		Imgproc.drawContours(mask_image, contours, largest_index, new Scalar(255,255,255), Core.FILLED);

		image.copyTo(mask_image,mask_image);
	    
		image = mask_image;
		
		MatOfPoint2f mat = new MatOfPoint2f(contours.get(largest_index).toArray());
		
		RotatedRect rect = Imgproc.minAreaRect(mat);
		Size rectSize = rect.size;
		
		System.out.println("PHOTO CROPPED FROM " + image.rows() + " x " + image.cols() + " TO " + Math.round(rectSize.height) + " x " + Math.round(rectSize.width) + "\n");
	}
	
	
	/**
	 * Set reference circle diameter and pixel abs size
	 * Image must be initialized and converted to HSV  
	 *   
	 */	
	private boolean setReference(){
		System.out.println("Setting references...");
		Mat imgBLUE = this.filterBlue(image);
		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	    Imgproc.findContours(imgBLUE, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
	    	    
	    List<RotatedRect> minRect = new ArrayList<RotatedRect>();
	    List<RotatedRect> minEllipse = new ArrayList<RotatedRect>();
	    
	    for(int i=0; i<contours.size(); i++){
	    	MatOfPoint tmp = new MatOfPoint(contours.get(i).toArray());
	    	MatOfPoint2f tmp2f = new MatOfPoint2f(contours.get(i).toArray());
	    	
	    	if(tmp.toArray().length >10){
	    		RotatedRect rec = Imgproc.minAreaRect(tmp2f);
	    		RotatedRect elp = Imgproc.fitEllipse(tmp2f);
	    		double circ = 4*Math.PI*Imgproc.contourArea(tmp) / Math.pow(Imgproc.arcLength(tmp2f, true),2); // calculate circularity
	    		double h = rec.boundingRect().height;
	    		double w = rec.boundingRect().width;
	    		double h2w = Math.max(h,w)/Math.min(h,w);
	    		if (circ>0.85 & h2w<1.1){
	    			refContours.add(tmp);
	    		}
	    	}
	    }
	    
	    // find the average width and height // divide by reference circle size
	    double sum = 0;
	    for(int i=0; i<refContours.size(); i++){
	    	MatOfPoint2f ref2f = new MatOfPoint2f();
	    	refContours.get(i).convertTo(ref2f, CvType.CV_32FC2);
	    	RotatedRect rec = Imgproc.minAreaRect(ref2f);
	    	sum += rec.boundingRect().height + rec.boundingRect().width;
	    }
	    double avgRef = sum / refContours.size() / 2;
	    pixelSize = refDiam / avgRef ;  // TODO check if this is calculated correctly
	    
	    if(refContours.size()>0) {
	    	System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
		    System.out.println("REFERENCE PIXEL SIZE: " + pixelSize + "\n");
	    	
	    	return true;
	    } else {
	    	System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
		    System.out.println("REPOSITION CAMERA TO CAPTURE REFERENCE CIRCLES" + "\n");
	    	return false;
	    }
	    
	}

	//TODO minimum size option
	//TODO minimum number option
	
	/**
	 * Count seeds.  Color filter is followed by erosion and watershed. 
	 * Image must be initialized and converted to HSV  
	 *   
	 */	
	private void countSeeds(){	
		/**
		 * UW nonparametric approach
		 */
		System.out.println("Counting seeds...");

		Mat imgNG = this.filterGreen(image);
		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(imgNG, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE, new Point(0,0));

		if(contours.size()>=1500) {
			System.out.println("Too many objects detected. Use fewer seeds or check background colors");
			System.out.println(contours.size());
			return;
		}
		System.out.println("CONTOURS: " + contours.size());

		double[] dims = new double[2];
		
		for (int i = 0; i < contours.size(); i++) {
			MatOfPoint2f tmp = new MatOfPoint2f(contours.get(i).toArray());
			RotatedRect test = Imgproc.minAreaRect(tmp);
			Size testsz = test.size;
			dims[0] = testsz.width;
			dims[1] = testsz.height;

			double area = Imgproc.contourArea(contours.get(i));

			if (findMin(dims) / findMax(dims) == 0.0 || area == 0.0 || findMax(dims) / findMin(dims) >= 10.0 || findMax(dims) <= 10 || findMin(dims) <= 10) {
				contours.remove(i);
				i = i - 1;
				continue;
			}

			seedMatL.add(findMax(dims));
			seedMatW.add(findMin(dims));
			seedMatA.add(area);
			seedMatP.add(Imgproc.arcLength(tmp, true));
		}
		//System.out.println(seedMatW.size());
		//store all measurements in single variable
		double[][] seedMatValues = new double[4][seedMatW.size()];
		
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < seedMatW.size(); j++) {
				if (i == 0) {
					seedMatValues[i][j] = seedMatL.get(j);
				}
				if (i == 1) {
					seedMatValues[i][j] = seedMatW.get(j);
				}
				if (i == 2) {
					seedMatValues[i][j] = seedMatA.get(j);
				}
				if (i == 3) {
					seedMatValues[i][j] = seedMatP.get(j);
				}
			}
		}
		
		// Divide each element by the other element
		
		double[][][] seedMatCalc = new double[4][seedMatW.size()][seedMatW.size()];
		
		for (int k = 0; k < 4; k++) {
			for (int i = 0; i < seedMatW.size(); i++) {
				for (int j = 0; j < seedMatW.size(); j++) {
					seedMatCalc[k][i][j] = Math.round(seedMatValues[k][j] / seedMatValues[k][i]);
				}
			}
		}
		
		// Count number of 1s
		
		int[][] seedMatCalcMax = new int[4][seedMatW.size()];
		int count = 0;
		
		for (int k = 0; k < 4; k++) {
			for (int i = 0; i < seedMatW.size(); i++) {
				for (int j = 0; j < seedMatW.size(); j++) {
					if (seedMatCalc[k][j][i] == 1) {
						count++;
					}
				}
				
				seedMatCalcMax[k][i] = count;

				count = 0;
			}
		}
	
		
		// find mode for true seed count
		int[] trueSeedCount = new int[4];
		for(int i = 0; i< 4; i++) {
			trueSeedCount[i] = findMode(seedMatCalcMax[i]);
			//System.out.println(trueSeedCount[i]);
		}
		
		double seedSize = 0;
		double[] avgSeedSize = new double[4];
		int[] numClusters = new int[4];
		
		for(int j=0; j < 4; j++) {
			for(int i=0; i < seedMatW.size(); i++) {
				if(seedMatCalcMax[j][i]==trueSeedCount[j]) {
					seedSize = seedSize + seedMatValues[j][i];
					numClusters[j]++;
				}
			}
			avgSeedSize[j] = seedSize;
			//System.out.println(avgSeedSize[j]);
			seedSize = 0;
		}
		
		for(int i=0; i<4 ; i++) {
			avgSeedSize[i] = avgSeedSize[i]/numClusters[i];
			//System.out.println(avgSeedSize[i]);
		}
		
		
		// Count number of seeds using avg seed size 
		double[][] seedMatCount = new double[4][seedMatW.size()];
		
		for(int j=0; j < 4 ; j++) {
			for(int i=0; i < seedMatW.size(); i++) {
				seedMatCount[j][i] = Math.round(seedMatValues[j][i]/avgSeedSize[j]);
				//System.out.println(seedMatCount[j][i]);
			}
			//System.out.println("break");
		}
		
		int[] seedCount = new int[4];
		String[] method = {"Length","Width","Area","Perimeter"};
		
		for(int j = 0 ; j < 4; j++) {
			for(int i=0; i < seedMatW.size(); i++) {
				seedCount[j] = seedCount[j] + (int) seedMatCount[j][i];
			}
			System.out.println("SEED COUNT " + "(" + method[j] + "): " + seedCount[j]);
		}
		
		
		// identify all contours that only go to one for all four characteristics
		for(int i =0; i<contours.size();i++) {
			if(seedMatCount[0][i] == seedMatCount[1][i] && seedMatCount[1][i] == seedMatCount[2][i] && seedMatCount[2][i] == seedMatCount[3][i] && seedMatCount[0][i] == 1) {
				goodContours.add(contours.get(i));
			}
		}
		
		if(goodContours.size()<=12) {
			goodContours = contours;
			System.out.println("\n" + "UNABLE TO IDENTIFY FIVE INDIVIDUAL OBJECTS");
			System.out.println("YOUR SAMPLE IS LIKELY HETEROGENOUS OR THE IMAGE IS NOISY");
			System.out.println("USING ALL OBJECTS INSTEAD");
		}
		measureSeeds();
		
		/**
		 * OpenCV watershed - not working
		 */
		/*
        List<Mat> matList = new ArrayList<Mat>();
        matList.add(imgNG);
        matList.add(imgNG);
        matList.add(imgNG);
 
        Core.merge(matList, imgNG);
        System.out.println(imgNG.channels());
        
        Imgproc.threshold(imgNG, imgNG, 100, 255, Imgproc.THRESH_BINARY);

        Mat fg = new Mat(imgNG.size(),CvType.CV_8U);
        Imgproc.erode(imgNG,fg,new Mat(),new Point(-1,-1),5);

        Mat bg = new Mat(imgNG.size(),CvType.CV_8U);
        Imgproc.dilate(imgNG,bg,new Mat(),new Point(-1,-1),2);
        Imgproc.threshold(bg,bg,1, 128,Imgproc.THRESH_BINARY_INV);

        Mat markers = new Mat(imgNG.size(),CvType.CV_32S, new Scalar(0));
        Core.add(fg, bg, markers);
        
        Imgproc.cvtColor(markers, markers, Imgproc.COLOR_RGB2GRAY);
        markers.convertTo(markers, CvType.CV_32S);
		System.out.println(markers.channels());
        
		
        Imgproc.watershed(imgNG, markers);

        markers.convertTo(markers,CvType.CV_8U);
        
        Highgui.imwrite("C:\\Users\\Trevor\\Desktop\\photoB_erode_watershed.tif", markers);

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        
	    Imgproc.findContours(markers, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
	    */
   
	    
		/**
		 * distance - ~10-13% off
		 */
		/*
        Mat dt = new Mat();
        Imgproc.distanceTransform(imgNG, dt, Imgproc.CV_DIST_L2, 3); // Imgproc.CV_DIST_MASK_PRECISE
		Core.normalize(dt, dt, 0, 255, Core.NORM_MINMAX);
		Imgproc.threshold(dt, dt, 80, 255, Imgproc.THRESH_BINARY);
		
        dt.convertTo(dt,CvType.CV_8U);
        imgNG.convertTo(imgNG,CvType.CV_8U);
		Core.subtract(imgNG, dt, dt);

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        
	    Imgproc.findContours(dt, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        
        Highgui.imwrite("C:\\Users\\Trevor\\Desktop\\photoB_erode_watershed.tif", dt);

	    System.out.println("NUMBER OF SEEDS: " + contours.size());
		*/
        
		
		
	    /**
	     * pixel counting average, etc.
	     */
	    /*
        Mat pixels = imgNG.clone();
        Double d = new Double(seedSizeMedian);
        int i = d.intValue();
        
        int count = Core.countNonZero(pixels)/i;
	    System.out.println("NUMBER OF SEEDS COUNTED: " + count);
	    */
    
	}


	/**
	 * Measure the seeds that were identified as single seeds based on
	 * length, width, area, and perimeter  
	 * 
	 */	
	private void measureSeeds() {
		System.out.println("\n" + "Measuring seeds...");

		if (goodContours.size() >= 1500) {
			System.out.println("Too many objects detected. Use fewer seeds or check background colors");
			return;
		}

		for (int i = 0; i < goodContours.size(); i++) {
			MatOfPoint2f tmp = new MatOfPoint2f(goodContours.get(i).toArray());

			if (tmp.toArray().length > 10) {

				double[] dims = new double[2];
				RotatedRect test = Imgproc.minAreaRect(tmp);
				Size testsz = test.size;
				dims[0] = testsz.width;
				dims[1] = testsz.height;
				double area = Imgproc.contourArea(goodContours.get(i));
				
				if (findMin(dims) / findMax(dims) == 0.0 || area == 0.0 || findMax(dims) / findMin(dims) >= 10.0 || findMax(dims) <= 10 || findMin(dims) <= 10) {
					goodContours.remove(i);
					System.out.println(i);
					i = i - 1;
					continue;
				}

				Seed s = new Seed(tmp,symmetric);
				seedArray.add(s);
			}
		}

		System.out.println("NUMBER OF SEEDS USED FOR MEASUREMENTS: " + seedArray.size() + "\n");
	}
	
	private double findMax(double[] values) {
		double maxValue = values[0];
		for (int i = 1; i < values.length; i++) {
			if (values[i] > maxValue) {
				maxValue = values[i];
			}
		}
		return maxValue;
	}

	private double findMin(double[] values) {
		double minValue = values[0];
		for (int i = 1; i < values.length; i++) {
			if (values[i] < minValue) {
				minValue = values[i];
			}
		}
		return minValue;
	}
	
	private int findMode(int a[]) {
		int maxValue = 0;
		int maxCount = 0;

	    for (int i = 0; i < a.length; ++i) {
	        int count = 0;
	        for (int j = 0; j < a.length; ++j) {
	            if (a[j] == a[i]) ++count;
	        }
	        if (count > maxCount) {
	            maxCount = count;
	            maxValue = a[i];
	        }
	    }
	    return maxValue;
	}
	
	private double calculateMedian(List<MatOfPoint> contours) {
		double[] contSize = new double[contours.size()];
		
		if (!contours.isEmpty()) {
			for (int i = 0; i < contours.size(); i++) {
				contSize[i] = Imgproc.contourArea(contours.get(i));
			}
		}

		Arrays.sort(contSize);
		
		double median;
		if (contSize.length % 2 == 0) {
			median = ((double) contSize[contSize.length / 2] + (double) contSize[contSize.length / 2 - 1]) / 2;
		} else { 
			median = (double) contSize[contSize.length / 2];
		}
		
		return median;
	}
	
	
	/**
	 * Filters input image for blue
	 * 
	 * Scalar reference in HSB color space.  
	 *
	 * @param  img  input Mat to be filtered
	 */
	private Mat filterBlue(Mat img){   
		//Threshold the image	
		Mat imgFilter = img.clone();
	    Core.inRange(img, filterBlue[0], filterBlue[1], imgFilter); 
		return imgFilter;
	}
	
	/**
	 * Filters input image for green
	 * Scalar reference in HSB color space.  
	 *
	 * @param  img  input Mat to be filtered
	 */
	private Mat filterGreen(Mat img){
	    //Threshold the image	
		Mat imageFilter = img.clone();
	    Core.inRange(img, filterGreen[0], filterGreen[1], imageFilter); 
		return imageFilter;
	}
	
	/**
	 * Filters input image for the background
	 * Scalar reference in HSB color space.  
	 *
	 * @param  img  input Mat to be filtered
	 */
	private Mat filterBackground(Mat img){
		Scalar filterBackground[] = {new Scalar(40,0,0), new Scalar(85,255,255)};
		
		Mat imageFilter = img.clone();
	    Core.inRange(img, filterBackground[0], filterBackground[1], imageFilter); 
		return imageFilter;
	}
	
	
	
	/**
	 * Sets the blue level filter for masking image.  
	 * 
	 * Scalar reference in HSB color space.  
	 *
	 * @param  low	three value Scalar giving HSB values for lower filter threshold
	 * @param  high three value Scalar giving HSB values for upper filter threshold
	 */
	public void setBlueFilter(Scalar low, Scalar high){
		this.filterBlue[0] = low;
		this.filterBlue[1] = high;
	}
	
	/**
	 * Sets the green level filter for masking image.  
	 * 
	 * Scalar reference in HSB color space.  
	 *
	 * @param  low	three value Scalar giving HSB values for lower filter threshold
	 * @param  high three value Scalar giving HSB values for upper filter threshold
	 */
	public void setGreenFilter(Scalar low, Scalar high){
		this.filterGreen[0] = low;
		this.filterGreen[1] = high;
	}
	
	/**
	 * Returns a false color image with the reference and seeds colored  
	 *
	 * @return    OpenCV Mat
	 */
	
	public Mat getProcessedImg(){
		Mat pImg = image.clone();
		Imgproc.cvtColor(pImg, pImg, Imgproc.COLOR_HSV2BGR);
		//pImg.setTo(new Scalar(255,255,255));
		Scalar blue = new Scalar(255, 0, 0);
		Scalar purple = new Scalar(255, 0, 155);
		Scalar green = new Scalar(0, 255, 0);
		Scalar red = new Scalar(50, 0, 255);
		Scalar orange = new Scalar(0, 50, 255);
		Scalar white = new Scalar(255, 255, 255);
		Scalar black = new Scalar(0, 0, 0);
		//Scalar color = new Scalar(Math.random()*255, Math.random()*255, Math.random()*255);
		
		Imgproc.drawContours(pImg, refContours, -1, white, 4);
		
		
		List<MatOfPoint> seeds = new ArrayList<MatOfPoint>();
		for(int i=0; i<seedArray.size(); i++){
			seeds.add(seedArray.get(i).perm);
		}
		Imgproc.drawContours(pImg, seeds, -1, red, 3);	

		
		//for(int i=0; i<seedArray.size(); i++){
		//	Core.ellipse(pImg, seedArray.get(i).elp, green, 3);
		//}
		
		// Draw width vector, length vector and CG point
		for(int i=0; i<seedArray.size(); i++){
			
			Point points[] = new Point[4];
			seedArray.get(i).rect.points(points);
		    for(int j=0; j<4; ++j){
		        Core.line(pImg, points[j], points[(j+1)%4], blue, 2);
		    }
			
			
			// draw width vector
			//Core.line(pImg, seedArray.get(i).ptsW[0], seedArray.get(i).ptsW[1], blue, 2);
			
			// draw length vector
			//Core.line(pImg, seedArray.get(i).ptsL[0], seedArray.get(i).ptsL[1], green, 2);
			
			// draw CG point
			//Core.circle(pImg, seedArray.get(i).centgrav, 2, white, 2);
			
			// draw intLW point
			//Core.circle(pImg, seedArray.get(i).intLW, 2, black, 2);
		}
		
    	return pImg;
	}
	
	/**
	 * Writes a false color image with the reference and seeds colored to the specified image path 
	 *
	 * @param filename giving the full file path string of the file to write
	 */
	
	public void writeProcessedImg(String filename){
	    System.out.println(String.format("\nWriting %s", filename));
	    Highgui.imwrite(filename, this.getProcessedImg());
	    //Highgui.imwrite(filename, this.filterGreen(image));
	    //Highgui.imwrite(filename, filterBackground(image));
	}
	
	
	public class Seed{
		RotatedRect rect;
		private MatOfPoint2f seedMat = new MatOfPoint2f();
		private MatOfPoint perm = new MatOfPoint();

		private double length; 
		private double width; 
		private double circ;
		private double lwr; // length to width ratio
		private double area;
		private double perim;
		
		private Point centgrav = new Point();
		private Point intLW = new Point();
		private double ds; // distance between centgrav and intLW;
		private double tolerance = 0.2;
		private RotatedRect elp = new RotatedRect();
		private RotatedRect rec = new RotatedRect();
		private Point[] ptsL = new Point[2];
		private Point[] ptsW = new Point[2];
		
	    /**
	     * Class to hold seed matrix array and descriptors
	     *
	     */
		
		public double getLength() {
			return length;
		}
		
		public double getWidth() {
			return width;
		}
		
		public double getCirc() {
			return circ;
		}
		
		public double getArea() {
			return area;
		}
		
		public double getPerim() {
			return perim;
		}
		
		public Seed(MatOfPoint2f mat, Boolean symmetric){
			if(symmetric) {
				seedMat = mat;
				mat.convertTo(perm, CvType.CV_32S);

				rect = Imgproc.minAreaRect(mat);
				Size rectSize = rect.size;

				double[] dims = new double[2];

				dims[0] = rectSize.width;
				dims[1] = rectSize.height;

				length = findMax(dims)*pixelSize;
				width = findMin(dims)*pixelSize;
				area = Imgproc.contourArea(mat);
				perim = Imgproc.arcLength(mat,true);
				circ = 4*Math.PI*Imgproc.contourArea(mat) / Math.pow(Imgproc.arcLength(mat, true),2); // calculate circularity
			} else {
				seedMat = mat;
				mat.convertTo(perm, CvType.CV_32S);

				rec = Imgproc.minAreaRect(mat);
				elp = Imgproc.fitEllipse(mat);
				circ = 4*Math.PI*Imgproc.contourArea(mat) / Math.pow(Imgproc.arcLength(mat, true),2); // calculate circularity
				//length = Math.max(rec.boundingRect().height, rec.boundingRect().width);
				//width = Math.min(rec.boundingRect().height, rec.boundingRect().width);
				//lwr = length/width;
				this.calcCG();
				this.findMaxVec();
				this.findIS();
			}
		}
		
		/**
		 * Find the center of gravity by averaging all points in the perimeter   
		 * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
		 * 
		 */
		private void calcCG(){	
			double sumX = 0;
			double sumY = 0;
			Point[] permArray = perm.toArray();
			for(int i=0; i<permArray.length; i++){
				sumX += permArray[i].x;
				sumY += permArray[i].y;
		
			}
			centgrav.x = Math.round(sumX/permArray.length);
			centgrav.y = Math.round(sumY/permArray.length);
		}
		
		/**
		 * Find the end-point coordinates of the maxium vector in the seed  
		 * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
		 * 
		 */
		private void findMaxVec(){
			Point[] permArray = perm.toArray();
			for(int i=0; i<permArray.length; i++){
				for(int j=i; j<permArray.length; j++){
					Point p1 = permArray[i];
					Point p2 = permArray[j];
					double l = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
					if (l>length){
						length = l;
						ptsL[0]=p1;
						ptsL[1]=p2;
					}			
				}	
			}
			

			double slopeL = ((ptsL[1].y - ptsL[0].y) / (ptsL[1].x - ptsL[0].x));  // TODO not sure this works for infinite slope
			
			for(int i=0; i<permArray.length; i++){
				double d = 1;
				Point p1 = permArray[i];
				Point p2 = permArray[i];
				//double slp = 0;
				for(int j=0; j<permArray.length; j++){
					
					double s =  slopeL * ((p1.y - permArray[j].y) / (p1.x - permArray[j].x));  
					if (Math.abs(s+1)<d){
						d = Math.abs(s+1);  
						p2 = permArray[j];
					}	
				}	
				
				double w = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
				if(w>width){
					width = w;
					ptsW[0]=p1;
					ptsW[1]=p2;
				}
			}
			lwr = (length*pixelSize) / (width*pixelSize);
			
			
		}
		
		/**
		 * Find the intersection of max length and max width vector  
		 * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
		 * 
		 */
		private void findIS(){
			if(ptsW[0]==null){return;}; // exit method if width is null
			double S1 = 0.5*((ptsW[0].x - ptsW[1].x)*(ptsL[0].y - ptsW[1].y)-(ptsW[0].y - ptsW[1].y)*(ptsL[0].x - ptsW[1].x));
			double S2 = 0.5*((ptsW[0].x - ptsW[1].x)*(ptsW[1].y - ptsL[1].y)-(ptsW[0].y - ptsW[1].y)*(ptsW[1].x - ptsL[1].x));
			intLW.x = ptsL[0].x + S1*(ptsL[1].x -ptsL[0].x)/(S1+S2);
			intLW.y = ptsL[0].y + S1*(ptsL[1].y -ptsL[0].y)/(S1+S2); 
			
			ds = Math.sqrt(Math.pow(intLW.x - centgrav.x,2) + Math.pow(intLW.y - centgrav.y,2));
			
		}
		
		
		/**
		 * Returns the maximum vector length 
		 * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
		 * @return  double giving the maximum vector length
		 */
		private double getMaxVec(){	
			return Math.sqrt(Math.pow(ptsL[0].x - ptsL[1].x, 2) + Math.pow(ptsL[0].y - ptsL[1].y, 2));	
		}
		
		/**
		 * Runs multiple checks to determine if the shape blob represents a canonical seed shape
		 *
		 */
		private boolean checkCanonical(){
			/*
			if(this.checkCirc() & this.checkElp() & this.length>minSize & this.checkL2W()){
				return true;
			}
			else{return false;}
			*/
			if(this.length<minSize){
				return false;
			}
			if(!this.checkCirc()){
				return false;
			}
			if(!this.checkElp()){
				return false;
			}
			else{return true;}
			
		}
		
		/**
		 * Checks and expected circularity value is within the expected circularity range  
		 *
		 */
		private boolean checkCirc(){
			if(minCirc<circ){
				return true;
			}
			else{return false;}
		}
		
		/**
		 * Checks the object is roughly an eliptical shape.  Will filter blobs of two or more seeds
		 * 
		 * Not sure this is working correctly due to approximation formula for circumference of ellipse.
		 *
		 */
		private boolean checkElp(){
			
			double a = elp.boundingRect().height/2;
			double b = elp.boundingRect().width/2;
			double c = 2*Math.PI * Math.sqrt((Math.pow(a,2) + Math.pow(b,2))/ 2); // TODO this is the approximation formula for circumference of an ellipse - FIGURE OUT IF IT IS WORKING
			double p = Imgproc.arcLength(seedMat, true);
			//System.out.println(c + "\t" + p);
			
			if(p < 1.1*c){
				return true;
			}
			else{return false;}
		}
		
		
		/**
		 * Checks and expected length to width ratio is within a tolerance limit // DEFAULT SET TO 30%
		 *
		 */
		private boolean checkL2W(){
			if(lwr>expLWR*(1-tolerance) & lwr<expLWR*(1+tolerance)){
				return true;
			}
			else{return false;}
		}
		
		public void setTolerance(double t){
			tolerance = t;
		}
		
	}

	public ArrayList<Seed> getList() {
		return seedArray;
	}

}
