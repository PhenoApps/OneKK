// File: singlesource.rs

#pragma version(1)
#pragma rs java_package_name(org.wheatgenetics.onekk)

static const float4 weight = {0.299f, 0.587f, 0.114f, 0.0f};

uchar4 RS_KERNEL invert(uchar4 in, uint32_t x, uint32_t y) {
  uchar4 out = in;
  out.r = 255 - in.r;
  out.g = 255 - in.g;
  out.b = 255 - in.b;
  return out;
}

uchar4 RS_KERNEL greyscale(uchar4 in) {
  const float4 inF = rsUnpackColor8888(in);
  const float4 outF = (float4){ dot(inF, weight) };
  return rsPackColorTo8888(outF);
}

void process(rs_allocation inputImage, rs_allocation outputImage) {
  const uint32_t imageWidth = rsAllocationGetDimX(inputImage);
  const uint32_t imageHeight = rsAllocationGetDimY(inputImage);
  rs_allocation tmp = rsCreateAllocation_uchar4(imageWidth, imageHeight);
  rsForEach(invert, inputImage, tmp);
  rsForEach(greyscale, tmp, outputImage);
}
