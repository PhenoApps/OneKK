package org.wheatgenetics.imageprocess.watershed;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Arrays;

public class MaximumFinder {
    private static final String TAG = "Arunachala";
    private int[] dirOffset;
    private int intEncodeXMask;
    private int intEncodeYMask;
    private int intEncodeShift;
    final static int[] DIR_X_OFFSET = new int[] {  0,  1,  1,  1,  0, -1, -1, -1 };
    final static int[] DIR_Y_OFFSET = new int[] { -1, -1,  0,  1,  1,  1,  0, -1 };
    final static byte MAXIMUM = (byte)1;
    int width;
    int height;
    final static float SQRT2 = 1.4142135624f;
    final static byte PROCESSED = (byte)4;// marks local maxima (irrespective of noise tolerance)
    final static byte LISTED = (byte)2;
    final static byte EQUAL = (byte)16;
    final static byte MAX_POINT = (byte)32;         // marks a single point standing for a maximum
    final static byte ELIMINATED = (byte)64;
    final static byte MAX_AREA = (byte)8;
    public final static int SEGMENTED=2;

    public void run(Bitmap bitmap){
        boolean invertedLut = false;         //check this functionality (isInvertedLut())
    }

    public int[][] findMaxima(float[][] floatedm, int width, int height, double tolerance, double threshold,
                                    int outputType, boolean excludeOnEdges, boolean isEDM){
   // print("findMaxima at start of function\n");
        //EDM e1 = new EDM();
        //enteredThreshold =e1.getThreshold();

        if(dirOffset == null){
            makeDirectionOffsets(width, height);
           // print("DirOffset is null\n");//
        }

        byte[] mask = null;
        int[][] typeP = new int[width][height];
        float globalMin = Float.MAX_VALUE;
        float globalMax = -Float.MAX_VALUE;
        for(int y=0; y<height; y++){
            for(int x=0; x<width;x++){
                float v= floatedm[x][y];
                if (globalMin>v) globalMin = v;
                if (globalMax<v) globalMax = v;
            }
        }
        boolean excludeEdgesNow = excludeOnEdges;
        print("Tolerance set is "+tolerance);
   // print("FindMaxima before calling getSortedMaxpoints\n");
        long[] maxPoints = getSortedMaxPoints(floatedm,width, height, typeP, excludeEdgesNow, isEDM, globalMin, globalMax, threshold);

        //int offset0 = (int)maxPoints[maxPoints.length-1];
       // int x0= offset0%width;
      //  int y0= offset0/width;
     //   print("max edm value is "+floatedm[x0][y0]+"Its coods are "+x0+", "+y0);
      //  print("highest edm value is "+maxPoints[maxPoints.length-1]);
        //print("FindMaxima after calling getSortedMaxPoints\n");
        float maxSortingError =  1.1f * (isEDM ? SQRT2/2f : (globalMax-globalMin)/2e9f);
      //  print("FindMaxima before clling analyzeandmarkmaxima\n");
        analyzeAndMarkMaxima(floatedm, typeP, maxPoints, excludeEdgesNow, isEDM, globalMin, tolerance, outputType, maxSortingError);
   // print("FindMAxima after calling analyzeandmarkmaxima\n");
        int[][] outIp = null;
          //  print("Fatel Error output type mismatch\n");
        if (outputType == SEGMENTED) {
            // Segmentation required, convert to 8bit (also for 8-bit images, since the calibration
            // may have a negative slope). outIp has background 0, maximum areas 255
          //  print("findMaxima beforre calling max8bit\n");
            outIp = make8bit(floatedm, typeP, isEDM, globalMin, globalMax, threshold);
         //   print("FindMAxima after calling max8bnit\n");
                //if (IJ.debugMode) new ImagePlus("pixel types precleanup", typeP.duplicate()).show();
        //    print("FindMaxima befiore calling cleanuopmaxima\n");
           // print("OutIP Print1\n");
           // print("outip at 1 0 "+outIp[1][0]);
            //print("Outip after print\n");
            cleanupMaxima(outIp, typeP, maxPoints);     //eliminate all the small maxima (i.e. those outside MAX_AREA)
        //    print("FindMaxima after calliung cleanupmaxima\n");
            //if (IJ.debugMode) new ImagePlus("pixel types postcleanup", typeP).show();
        //   print("FindMaxima before calling watershedsegment\n");
            //if (IJ.debugMode) new ImagePlus("pre-watershed", outIp.duplicate()).show();
           // print("OutIP Print2\n");
           // print("outip at 1 0 "+outIp[1][0]);
           // print("Outip after print\n");
            if (!watershedSegment(outIp)) {// print("FindMAxima returning null due to watershedsegment retunred null\n");             //do watershed segmentation
                return null;               }             //if user-cancelled, return
            //if (!isEDM) cleanupExtraLines(outIp);       //eliminate lines due to local minima (none in EDM)
  //  print("FindMAxima before calling watershedPostProcess\n");
          //  print("OutIP Print3\n");
          //  print("outip at 1 0 "+outIp[1][0]);
          //  print("Outip after print\n");
            watershedPostProcess(outIp);                //levels to binary image
         //   print("FIndmaxima after calling watershedpostprocess\n");
            //if (excludeOnEdges) deleteEdgeParticles(outIp, typeP);
        }

            for (int y=0, i=0; y<height; y++) { //delete everything outside roi
                for (int x=0; x<width; x++, i++) {
                    if (x<0 || x>=width || y<0 || y>=height) outIp[x][y] = (byte)0;
                    else if (mask !=null && (mask[x + width*(y)]==0)) outIp[x][y] = (byte)0;
                }
            }
     //   print("OutIP Print4\n");
     //   print("outip at 1 0 "+outIp[1][0]);
    //    print("Outip after print\n");
        return outIp; //check this
    }

    long[] getSortedMaxPoints(float[][] floatedm, int width, int height, int[][] typeP, boolean excludeEdgesNow,
                              boolean isEDM, float globalMin, float globalMax, double threshold){
        boolean checkThreshold = false;
        int nMax = 0;
        for(int y=0; y<height;y++){
            for(int x=0; x<width; x++){
                float v = floatedm[x][y];
                float vTrue = isEDM ? trueEdmHeight(x, y, floatedm) : v;
                if (v==globalMin) continue;
                if (excludeEdgesNow && (x==0 || x==width-1 || y==0 || y==height-1)) continue;
                if (checkThreshold && v < threshold) continue;
                boolean isMax = true;
                boolean isInner = (y!=0 && y!=height-1) && (x!=0 && x!=width-1);
                for (int d=0; d<8; d++) {                         // compare with the 8 neighbor pixels
                    if (isInner || isWithin(x, y, d)) {
                        float vNeighbor = floatedm[x+DIR_X_OFFSET[d]][y+DIR_Y_OFFSET[d]];
                        float vNeighborTrue = isEDM ? trueEdmHeight(x+DIR_X_OFFSET[d], y+DIR_Y_OFFSET[d], floatedm) : vNeighbor;
                        if (vNeighbor > v && vNeighborTrue > vTrue) {
                            isMax = false;
                            break;
                        }
                    }
                }
                if (isMax) {
                    typeP[x][y] = MAXIMUM;
                    nMax++;
                }
            } // for x
        } // for y

        float z1 = -1 , z2 = -1;

        float vFactor = (float)(2e9/(globalMax-globalMin)); //for converting float values into a 32-bit int
        long[] maxPoints = new long[nMax];                  //value (int) is in the upper 32 bit, pixel offset in the lower
        int iMax = 0;
        for (int y=0; y<height; y++)           //enter all maxima into an array
            for (int x=0; x<width; x++)
                if (typeP[x][y]==MAXIMUM) {

                    float fValue = isEDM?trueEdmHeight(x,y,floatedm):floatedm[x][y];
                    int iValue = (int)((fValue-globalMin)*vFactor); //32-bit int, linear function of float value
                    maxPoints[iMax++] = (long)iValue<<32|x+y*width;
                }
        Arrays.sort(maxPoints);
       // System.out.println("z1 is: "+z1+" z2 is "+z2);
        return maxPoints;
    }

    float trueEdmHeight(int x, int y, float[][] floatedm) {
        int xmax = width - 1;
        int ymax = height - 1;
        int offset = x + y*width;
        if(x>=width || y>=height || x<0 || y<0)
            return 0;
       // print("trueEdmHeight at start of function accesing floatedm with x y \n"+ x+" " +y);
        float v =  floatedm[x][y];
      //  print("trueEdmHeight at start of function accesed floatedm with x y \n"+ x+" " +y);

        if (x==0 || y==0 || x==xmax || y==ymax || v==0) {
            return v;                               //don't recalculate for edge pixels or background
        } else {
            float trueH = v + 0.5f*SQRT2;           //true height can never by higher than this
            boolean ridgeOrMax = false;
            for (int d=0; d<4; d++) {               //for all directions halfway around:
                int d2 = (d+4)%8;                   //get the opposite direction and neighbors
             //   print("floatEdmHeight accsing floatedm after d2 with x y for v1 +"+offset+dirOffset[d]%width+" "+offset+dirOffset[d]/width);
                float v1 = floatedm[(offset+dirOffset[d])%width][(offset+dirOffset[d])/width];
             //   print("floatEdmHeight accsing floatedm after d2 with x y for v2 +"+offset+dirOffset[d2]%width+" "+offset+dirOffset[d2]/width);

                float v2 = floatedm[(offset+dirOffset[d2])%width][(offset+dirOffset[d2])/width];
                float h;
                if (v>=v1 && v>=v2) {
                    ridgeOrMax = true;
                    h = (v1 + v2)/2;
                } else {
                    h = Math.min(v1, v2);
                }
                h += (d%2==0) ? 1 : SQRT2;          //in diagonal directions, distance is sqrt2
                if (trueH > h) trueH = h;
            }
            if (!ridgeOrMax) trueH = v;

            return trueH;
        }
    }

    void analyzeAndMarkMaxima(float[][] floatedm, int[][] typeP, long[] maxPoints, boolean excludeEdgesNow,
                              boolean isEDM, float globalMin, double tolerance, int outputType, float maxSortingError) {
        int nMax = maxPoints.length;
       // print("Analyzeandmarkmaxima at start of function\n");
        int [] pList = new int[width*height];
        for (int iMax=nMax-1; iMax>=0; iMax--) {
            int offset0 = (int)maxPoints[iMax];
         //   print("Analyzeandmarkmaxima accesing typeP before continue\n");
            if((((byte)(typeP[offset0%width][offset0/width]))&PROCESSED)!=0){
           //     print("Analyzeandmarkmaxima accesed typeP inside iof before continue\n");
                continue;
            }
         //   print("Analyzeandmarkmaxima accesed typeP after if after continue\n");
            int x0 = offset0 % width;
            int y0 = offset0 / width;
        //    print("Analyzeandmarkmaxima accesing floatedm  for isedm\n");
            float v0 = isEDM?trueEdmHeight(x0,y0,floatedm):floatedm[x0][y0];
         //   print("Analyzeandmarkmaxima accesed floatedm for isedm\n");
            boolean sortingError;
            do{
                pList[0] = offset0;
                typeP[x0][y0] |= (EQUAL|LISTED);
                int listI = 0;
                int listLen = 1;
                boolean isEdgeMaximum = (x0==0 || x0==width-1 || y0==0 || y0==height-1);
                sortingError = false;       //if sorting was inaccurate: a higher maximum was not handled so far
                boolean maxPossible = true;         //it may be a true maximum
                double xEqual = x0;                 //for creating a single point: determine average over the
                double yEqual = y0;                 //  coordinates of contiguous equal-height points
                int nEqual = 1;
                do{//print("Analyzeandmarkmaxima accesing pList at do\n");
                    int offset = pList[listI];
                    //print("Analyzeandmarkmaxima accesed pList at do\n");
                    int x = offset % width;
                    int y = offset / width;
                    boolean isInner = (y!=0 && y!=height-1) && (x!=0 && x!=width-1);
                    for (int d=0; d<8; d++) {       //analyze all neighbors (in 8 directions) at the same level
                      //  print("Analyzeandmarkmaxima accesing dirOffet after for\n");
                        int offset2 = offset + dirOffset[d];
                        //print("Analyzeandmarkmaxima accesed dirOffset after for\n");
                       // print("Analyzeandmarkmaxima accesing typeP after for\n");
                        if ((isInner || isWithin(x, y, d)) && ((byte) (typeP[offset2 % width][offset2 / width]) & LISTED) == 0) {
                           // print("Analyzeandmarkmaxima accesed typeP after for\n");
                           // print("Analyzeandmarkmaxima accesing floatedm after for\n");
                            if (isEDM && floatedm[offset2 % width][offset2 / width] <= 0)
                            {
                             //   print("Analyzeandmarkmaxima accessed floaedm before continue after for\n");
                                continue; }  //ignore the background (non-particles)}
                            //print("Analyzeandmarkmaxima accesed floatedm after for\n");
                            //print("Analyzeandmarkmaxima accesing typeP inside for\n");
                            if ((((byte) typeP[offset2 % width][offset2 / width]) & PROCESSED) != 0) {
                                maxPossible = false;
                                //print("Analyzeandmarkmaxima accesed typeP inside for\n");
                                break;
                            }
                            //print("Analyzeandmarkmaxima accesed typeP inside for\n");
                            int x2 = x + DIR_X_OFFSET[d];
                            int y2 = y + DIR_Y_OFFSET[d];
                            //print("Analyzeandmarkmaxima accesing floatedm for v2\n");
                            float v2 = isEDM ? trueEdmHeight(x2, y2, floatedm) : floatedm[x2][y2];
                            //print("Analyzeandmarkmaxima accesed floatedm for v2\n");
                            if (v2 > v0 + maxSortingError) {
                                maxPossible = false;    //we have reached a higher point, thus it is no maximum
                                break;
                            } else if (v2 >= v0 - (float) tolerance) {
                                if (v2 > v0) {
                                    for (int d3 = 0; d3 < 8; d3++) {
                                        int x3 = x2 + DIR_X_OFFSET[d3];
                                        int y3 = y2 + DIR_Y_OFFSET[d3];
                                     //   print("Analyzeandmarkmaxima accesing floatedm for v3. x3, y3 "+x3+" "+y3+" x2 y2 "+x2+" "+y2+" dirx diry "+DIR_X_OFFSET[d3]+" "+DIR_Y_OFFSET[d3] );
                                     //   print("width height "+width+" "+height);
                                  //      if(isEDM==false)
                                  //          print("Analyzeandmarkmaxima fatel error as isedm is false\n");
                                        float v3 = isEDM ? trueEdmHeight(x3, y3, floatedm) : floatedm[x3][y3];
                                    //    print("Analyzeandmarkmaxima accesed floatedm for v3\n");

                                    }
                                    sortingError = true;
                                    offset0 = offset2;
                                    v0 = v2;
                                    x0 = x2;
                                    y0 = y2;
                                }
                              //  print("Analyzeandmarkmaxima accesing plist using offset2\n");
                                pList[listLen] = offset2;
                                //print("Analyzeandmarkmaxima accesed plist for offset2\n");
                                listLen++;              //we have found a new point within the tolerance
                                //print("Analyzeandmarkmaxima assigning typeP\n");
                                typeP[offset2 % width][offset2 / width] |= LISTED;
                                //print("Analyzeandmarkmaxima assigned typeP\n");
                                if (x2 == 0 || x2 == width - 1 || y2 == 0 || y2 == height - 1) {
                                    isEdgeMaximum = true;
                                    if (excludeEdgesNow) {
                                        maxPossible = false;
                                        break;          //we have an edge maximum;
                                    }
                                }
                                if (v2 == v0) {   //prepare finding center of equal points (in case single point needed)
                                  //  print("Analyzeandmarkmaxima assigned typeP when v2==v0\n");
                                    typeP[offset2 % width][offset2 / width] |= EQUAL;
                                    //print("Analyzeandmarkmaxima assigned typeP when v2==v0\n");
                                    xEqual += x2;
                                    yEqual += y2;
                                    nEqual++;
                                }
                            }
                        }
                    }
                    listI++;

                }while(listI<listLen);
                //print("Analyzeandmarkmaxima after do while\n");
                if (sortingError)  {				  //if x0,y0 was not the true maximum but we have reached a higher one
                    for (listI=0; listI<listLen; listI++)
                        typeP[(pList[listI])%width][(pList[listI])/width] = 0;	//reset all points encountered, then retry
                } else {
                    int resetMask = ~(maxPossible ? LISTED : (LISTED | EQUAL));
                    xEqual /= nEqual;
                    yEqual /= nEqual;
                    double minDist2 = 1e20;
                    int nearestI = 0;
                    for (listI = 0; listI < listLen; listI++) {
                        int offset = pList[listI];
                        int x = offset % width;
                        int y = offset / width;
                        typeP[offset % width][offset / width] &= resetMask;        //reset attributes no longer needed
                        typeP[offset % width][offset / width] |= PROCESSED;        //mark as processed
                        if (maxPossible) {
                            typeP[offset % width][offset / width] |= MAX_AREA;
                            if ((typeP[offset % width][offset / width] & EQUAL) != 0) {
                                double dist2 = (xEqual - x) * (double) (xEqual - x) + (yEqual - y) * (double) (yEqual - y);
                                if (dist2 < minDist2) {
                                    minDist2 = dist2;    //this could be the best "single maximum" point
                                    nearestI = listI;
                                }
                            }
                        }
                    } // for listI
                    if (maxPossible) {
                        int offset = pList[nearestI];
                        typeP[offset % width][offset / width] |= MAX_POINT;
                    }
                }
            }while(sortingError);
        }
    }

    private void watershedPostProcess(int[][] ip) {
        //MLN
        //new ImagePlus("before postprocess",ip.duplicate()).show();
        for (int i=0; i<height;i++){
            for(int j=0; j<width; j++){
                if ((ip[j][i]&255)<255)
                    ip[j][i] = (byte)0;
            }
        }
        //MLN
        //new ImagePlus("after postprocess",ip.duplicate()).show();
    }



    int[][] make8bit(float[][] floatedm, int[][] typeP, boolean isEDM, float globalMin, float globalMax, double threshold) {
        double minValue = 0.0;
        if(isEDM) {
            threshold = 0.5;
            minValue = 1.;
        }
        double offset = minValue - (globalMax-minValue)*(1./253/2-1e-6);
        double factor = 253/(globalMax-minValue);
        if (isEDM && factor>1)
            factor = 1;   // with EDM, no better resolution
        int[][] outIp = new int[width][height];
        long v;
        for (int y=0, i=0; y<height; y++) {
            for (int x=0; x<width; x++, i++) {
                float rawValue = floatedm[x][y];
                if (true && rawValue<threshold)
                    outIp[i%width][i/width] = (byte)0;
                else if ((typeP[i%width][i/width]&MAX_AREA)!=0)
                    //check this code*********************************************************************************************************
                    outIp[i%width][i/width] = (byte)255;  //prepare watershed by setting "true" maxima+surroundings to 255
                else {
                    v = 1 + Math.round((rawValue-offset)*factor);
                    if (v < 1) outIp[i%width][i/width] = (byte)1;
                    else if (v<=254) outIp[i%width][i/width] = (byte)(v&255);
                    else outIp[i%width][i/width] = (byte)254;
                }
            }
        }
        return outIp;
    }

    void cleanupMaxima(int[][] outIp, int[][] typeP, long[] maxPoints) {
        int nMax = maxPoints.length;
        int[] pList = new int[width*height];
        for (int iMax = nMax-1; iMax>=0; iMax--) {
            int offset0 = (int) maxPoints[iMax];     //type cast gets lower 32 bits where pixel offset is encoded
            if ((typeP[offset0 % width][offset0 / width] & (MAX_AREA | ELIMINATED)) != 0) continue;
            int level = outIp[offset0 % width][offset0 / width] & 255;
            int loLevel = level + 1;
            pList[0] = offset0;                     //we start the list at the current maximum
            //if (xList[0]==122) IJ.write("max#"+iMax+" at x,y="+xList[0]+","+yList[0]+"; level="+level);
            typeP[offset0 % width][offset0 / width] |= LISTED;               //mark first point as listed
            int listLen = 1;                        //number of elements in the list
            int lastLen = 1;
            int listI = 0;                          //index of current element in the list
            boolean saddleFound = false;
            while (!saddleFound && loLevel > 0) {
                loLevel--;
                lastLen = listLen;                  //remember end of list for previous level
                listI = 0;                          //in each level, start analyzing the neighbors of all pixels
                do {                                //for all pixels listed so far
                    int offset = pList[listI];
                    int x = offset % width;
                    int y = offset / width;
                    boolean isInner = (y != 0 && y != height - 1) && (x != 0 && x != width - 1); //not necessary, but faster than isWithin
                    for (int d = 0; d < 8; d++) {       //analyze all neighbors (in 8 directions) at the same level
                        int offset2 = offset + dirOffset[d];
                        if ((isInner || isWithin(x, y, d)) && (typeP[offset2 % width][offset2 / width] & LISTED) == 0) {
//MLN                            if ((types[offset2]&MAX_AREA)!=0 || (((types[offset2]&ELIMINATED)!=0) && (pixels[offset2]&255)>=loLevel)) {
                            if ((typeP[offset2 % width][offset2 / width] & MAX_AREA) != 0 || (((typeP[offset2 % width][offset2 / width] & ELIMINATED) != 0) && (outIp[offset2 % width][offset2 / width] & 255) >= loLevel)) {
                                saddleFound = true; //we have reached a point touching a "true" maximum...
                                break;              //...or a level not lower, but touching a "true" maximum
                            } else if ((outIp[offset2 % width][offset2 / width] & 255) >= loLevel && (typeP[offset2 % width][offset2 / width] & ELIMINATED) == 0) {
                                pList[listLen] = offset2;
                                //xList[listLen] = x+DIR_X_OFFSET[d];
                                //yList[listLen] = x+DIR_Y_OFFSET[d];
                                listLen++;          //we have found a new point to be processed
                                typeP[offset2 % width][offset2 / width] |= LISTED;
                            }
                        } // if isWithin & not LISTED
                    } // for directions d
                    if (saddleFound) break;         //no reason to search any further
                    listI++;
                } while (listI < listLen);
            } // while !levelFound && loLevel>=0
            for (listI = 0; listI < listLen; listI++)   //reset attribute since we may come to this place again
                typeP[(pList[listI]) % width][(pList[listI]) / width] &= ~LISTED;
            for (listI = 0; listI < lastLen; listI++) { //for all points higher than the level of the saddle point
                int offset = pList[listI];
                outIp[offset % width][offset / width] = (byte) loLevel;     //set pixel value to the level of the saddle point
                typeP[offset % width][offset / width] |= ELIMINATED;        //mark as processed: there can't be a local maximum in this area
            }
        }
    }

    private boolean watershedSegment(int[][] ip) {

        int[] myarray = new int[256];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (ip[j][i] == (byte) 255)
                    myarray[255]++;
                else
                    myarray[ip[j][i]]++;
            }
        }

        int arraySize = width * height - myarray[0] - myarray[255];
        int[] coordinates = new int[arraySize];    //from pixel coordinates, low bits x, high bits y
        int highestValue = 0;
        int maxBinSize = 0;
        int offset = 0;
        int[] levelStart = new int[256];
        for (int v = 1; v < 255; v++) {
            levelStart[v] = offset;
            offset += myarray[v];
            if (myarray[v] > 0) highestValue = v;
            if (myarray[v] > maxBinSize) maxBinSize = myarray[v];
        }
        System.out.println("highestValue = " + highestValue + "histogram[highestValue] = " + myarray[highestValue]);

//MLN added
        int sum=0;
        for (int i=0; i<256; i++)
        {
            //System.out.println("histogram[" + i + "] = " + myarray[i]);
            sum += myarray[i];
        }
        //System.out.println("sum = " + sum);


        int[] levelOffset = new int[highestValue + 1];
        for (int y=0, i=0; y<height; y++) {
            for (int x=0; x<width; x++, i++) {
                int v = ip[x][y]&255;
                if (v>0 && v<255) {
                    offset = levelStart[v] + levelOffset[v];
                    coordinates[offset] = x | y<<intEncodeShift;
                    levelOffset[v] ++;
                }
            } //for x
        } //for y
        // Create an array of the points (pixel offsets) that we set to 255 in one pass.
        // If we remember this list we need not create a snapshot of the ImageProcessor.
        int[] setPointList = new int[Math.min(maxBinSize, (width*height+2)/3)];
        // now do the segmentation, starting at the highest level and working down.
        // At each level, dilate the particle (set pixels to 255), constrained to pixels
        // whose values are at that level and also constrained (by the fateTable)
        // to prevent features from merging.
        int[] table = makeFateTable();

        //for (int i=0; i<256; i++)
        //    System.out.println("FateTable [" + i + "] = " + table[i]);

        final int[] directionSequence = new int[] {7, 3, 1, 5, 0, 4, 2, 6}; // diagonal directions first
        for (int level=highestValue; level>=1; level--) {

       //     System.out.println("Level = " + level);

            int remaining = myarray[level];  //number of points in the level that have not been processed
            int idle = 0;
            while (remaining>0 && idle<8) {
                int sumN = 0;
                int dIndex = 0;
                do {                        // expand each level in 8 directions
                    int n = processLevel(directionSequence[dIndex%8], ip, table,
                            levelStart[level], remaining, coordinates, setPointList);
                    //IJ.log("level="+level+" direction="+directionSequence[dIndex%8]+" remain="+remaining+"-"+n);
//System.out.println("level="+level+" direction="+directionSequence[dIndex%8]+" remain="+remaining+"-"+n);
                    remaining -= n;         // number of points processed
                    sumN += n;
                    if (n > 0) idle = 0;    // nothing processed in this direction?  // MLN something processed in this direction
                    dIndex++;
                } while (remaining>0 && idle++<8);

            }
            if (remaining>0 && level>1) {   // any pixels that we have not reached?
                int nextLevel = level;      // find the next level to process
                do
                    nextLevel--;
                while (nextLevel>1 && myarray[nextLevel]==0);
                // in principle we should add all unprocessed pixels of this level to the
                // tasklist of the next level. This would make it very slow for some images,
                // however. Thus we only add the pixels if they are at the border (of the
                // image or a thresholded area) and correct unprocessed pixels at the very
                // end by CleanupExtraLines
                if (nextLevel > 0) {
                    int newNextLevelEnd = levelStart[nextLevel] + myarray[nextLevel];
                    for (int i=0, p=levelStart[level]; i<remaining; i++, p++) {
                        int xy = coordinates[p];
                        int x = xy&intEncodeXMask;
                        int y = (xy&intEncodeYMask)>>intEncodeShift;
                        int pOffset = x + y*width;
                        if ((ip[x][y]&255)==255) print("ERROR");
                        boolean addToNext = false;
                        if (x==0 || y==0 || x==width-1 || y==height-1)
                            addToNext = true;           //image border
                        else for (int d=0; d<8; d++)
                            if (isWithin(x, y, d) && ip[(pOffset+dirOffset[d])%width][(pOffset+dirOffset[d])/width]==0) {
                                addToNext = true;       //border of area below threshold
                                break;
                            }
                        if (addToNext)
                            coordinates[newNextLevelEnd++] = xy;
                    }
                    //IJ.log("level="+level+": add "+(newNextLevelEnd-levelStart[nextLevel+1])+" points to "+nextLevel);
                    //tasklist for the next level to process becomes longer by this:
                    myarray[nextLevel] = newNextLevelEnd - levelStart[nextLevel];
                }
            }
//MLN            if (debug && (level>170 || level>100 && level<110 || level<10))
        }
        return true;
    } // boolean watershedSegment

    private int[] makeFateTable() {
        int[] table = new int[256];
        boolean[] isSet = new boolean[8];

        //System.out.println("In makeFateTable()");

        for (int item=0; item<256; item++) {        //dissect into pixels
            for (int i=0, mask=1; i<8; i++) {
                isSet[i] = (item&mask)==mask;
                mask*=2;
            }
            for (int i=0, mask=1; i<8; i++) {       //we dilate in the direction opposite to the direction of the existing neighbors
                if (isSet[(i+4)%8]) table[item] |= mask;
                mask*=2;
            }
            for (int i=0; i<8; i+=2)                //if side pixels are set, for counting transitions it is as good as if the adjacent edges were also set
                if (isSet[i]) {
                    isSet[(i+1)%8] = true;
                    isSet[(i+7)%8] = true;
                }
            int transitions=0;
            for (int i=0, mask=1; i<8; i++) {
                if (isSet[i] != isSet[(i+1)%8])
                    transitions++;
            }
            if (transitions>=4) {        //if neighbors contain more than one region, dilation ito this pixel is forbidden
                table[item] = 0;
            } else {
            }
        }
        return table;
    } // int[] makeFateTable


    private int processLevel(int pass, int[][] ip, int[] fateTable,
                             int levelStart, int levelNPoints, int[] coordinates, int[] setPointList) {
        int xmax = width - 1;
        int ymax = height - 1;
        //byte[] pixels2 = (byte[])ip2.getPixels();
        int nChanged = 0;
        int nUnchanged = 0;
        for (int i=0, p=levelStart; i<levelNPoints; i++, p++) {
            int xy = coordinates[p];
            int x = xy&intEncodeXMask;
            int y = (xy&intEncodeYMask)>>intEncodeShift;
            int offset = x + y*width;
            int index = 0;      //neighborhood pixel ocupation: index in fateTable
            if (y>0 && (ip[(offset-width)%width][(offset-width)/width]&255)==255)
                index ^= 1;
            if (x<xmax && y>0 && (ip[(offset-width+1)%width][(offset-width+1)/width]&255)==255)
                index ^= 2;
            if (x<xmax && (ip[(offset+1)%width][(offset+1)/width]&255)==255)
                index ^= 4;
            if (x<xmax && y<ymax && (ip[(offset+width+1)%width][(offset+width+1)/width]&255)==255)
                index ^= 8;
            if (y<ymax && (ip[(offset+width)%width][(offset+width)/width]&255)==255)
                index ^= 16;
            if (x>0 && y<ymax && (ip[(offset+width-1)%width][(offset+width-1)/width]&255)==255)
                index ^= 32;
            if (x>0 && (ip[(offset-1)%width][(offset-1)/width]&255)==255)
                index ^= 64;
            if (x>0 && y>0 && (ip[(offset-width-1)%width][(offset-width-1)/width]&255)==255)
                index ^= 128;
            int mask = 1<<pass;
            if ((fateTable[index]&mask)==mask)
                setPointList[nChanged++] = offset;  //remember to set pixel to 255
            else
                coordinates[levelStart+(nUnchanged++)] = xy; //keep this pixel for future passes

        } // for pixel i

//        System.out.println("pass="+pass+", changed="+nChanged+" unchanged="+nUnchanged);

        //IJ.log("pass="+pass+", changed="+nChanged+" unchanged="+nUnchanged);
        for (int i=0; i<nChanged; i++)
            ip[(setPointList[i])%width][(setPointList[i])/width] = (byte)255;
        return nChanged;
    } //processLevel

    public void makeDirectionOffsets(int width1, int height1){
        width = width1;
        height= height1;
        int shift = 0, mult=1;
        do {
            shift++; mult*=2;
        }
        while (mult < width);
        intEncodeXMask = mult-1;
        intEncodeYMask = ~intEncodeXMask;
        intEncodeShift = shift;
        dirOffset  = new int[] {-width, -width+1, +1, +width+1, +width, +width-1,   -1, -width-1 };
    }

    boolean isWithin(int x, int y, int direction) {
        int xmax = width - 1;
        int ymax = height -1;
        switch(direction) {
            case 0:
                return (y>0);
            case 1:
                return (x<xmax && y>0);
            case 2:
                return (x<xmax);
            case 3:
                return (x<xmax && y<ymax);
            case 4:
                return (y<ymax);
            case 5:
                return (x>0 && y<ymax);
            case 6:
                return (x>0);
            case 7:
                return (x>0 && y>0);
        }
        return false;
    } // isWithin

    public void print(String s){
        Log.d(TAG, s);
    }
}
