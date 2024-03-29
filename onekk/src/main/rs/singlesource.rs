// File: singlesource.rs

#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(org.wheatgenetics.imageprocess)

static const float4 weight = {0.299f, 0.587f, 0.114f, 0.0f};

int32_t histo[256];

float remapArray[256];

int size;

uchar4 RS_KERNEL invert(uchar4 in, uint32_t x, uint32_t y) {
  uchar4 out = in;
  out.r = 0;//255 - in.r;
  out.g = 0;//255 - in.g;
  out.b = 0;//255 - in.b;
  return out;
}

uchar4 RS_KERNEL greyscale(uchar4 in) {
  const float4 inF = rsUnpackColor8888(in);
  const float4 outF = (float4){ dot(inF, weight) };
  return rsPackColorTo8888(outF);
}

uchar4 RS_KERNEL histogram(uchar4 in) {
    //Convert input uchar4 to float4
    float4 f4 = rsUnpackColor8888(in);

    //Get YUV channels values
    float Y = 0.299f * f4.r + 0.587f * f4.g + 0.114f * f4.b;
    float U = ((0.492f * (f4.b - Y))+1)/2;
    float V = ((0.877f * (f4.r - Y))+1)/2;

    //Get Y value between 0 and 255 (included)
    int32_t val = Y * 255;
    //Increment histogram for that value
    rsAtomicInc(&histo[val]);

    //Put the values in the output uchar4, note that we keep the alpha value
    return rsPackColorTo8888(Y, U, V, f4.a);
}

void process(rs_allocation inputImage, rs_allocation outputImage) {
  const uint32_t imageWidth = rsAllocationGetDimX(inputImage);
  const uint32_t imageHeight = rsAllocationGetDimY(inputImage);
  //rs_allocation tmp = rsCreateAllocation_uchar4(imageWidth, imageHeight);
  //rsForEach(invert, inputImage, tmp);
  //rsForEach(greyscale, inputImage, outputImage);
  rsForEach(histogram, inputImage, outputImage);
}


//Method to keep the result between 0 and 1
static float bound (float val) {
    float m = fmax(0.0f, val);
    return fmin(1.0f, m);
}

uchar4 RS_KERNEL remaptoRGB(uchar4 in) {
    //Convert input uchar4 to float4
    float4 f4 = rsUnpackColor8888(in);

    //Get Y value
    float Y = f4.r;
    //Get Y value between 0 and 255 (included)
    int32_t val = Y * 255;
    //Get Y new value in the map array
    Y = remapArray[val];

    //Get value for U and V channel (back to their original values)
    float U = (2*f4.g)-1;
    float V = (2*f4.b)-1;

    //Compute values for red, green and blue channels
    float red = bound(Y + 1.14f * V);
    float green = bound(Y - 0.395f * U - 0.581f * V);
    float blue = bound(Y + 2.033f * U);

    //Put the values in the output uchar4
    return rsPackColorTo8888(red, green, blue, f4.a);
}

void init() {
    //init the array with zeros
    for (int i = 0; i < 256; i++) {
        histo[i] = 0;
        remapArray[i] = 0.0f;
    }
}

void createRemapArray() {
    //create map for y
    float sum = 0;
    for (int i = 0; i < 256; i++) {
        sum += histo[i];
        remapArray[i] = sum / (size);
    }
}