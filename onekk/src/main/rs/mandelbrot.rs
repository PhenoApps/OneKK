// File: mandelbrot.rs

#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(org.wheatgenetics.imageprocess)

uint32_t gMaxIteration = 500;
uint32_t gDimX = 1024;
uint32_t gDimY = 1024;
float lowerBoundX = -2.f;
float lowerBoundY = -2.f;
float scaleFactor = 4.f;
uchar4 __attribute__((kernel)) root(uint32_t x, uint32_t y) {
  float2 p;
  p.x = lowerBoundX + ((float)x / gDimX) * scaleFactor;
  p.y = lowerBoundY + ((float)y / gDimY) * scaleFactor;
  float2 t = 0;
  float2 t2 = t * t;
  int iter = 0;
  while((t2.x + t2.y < 4.f) && (iter < gMaxIteration)) {
    float xtemp = t2.x - t2.y + p.x;
    t.y = 2 * t.x * t.y + p.y;
    t.x = xtemp;
    iter++;
    t2 = t * t;
  }
  if(iter >= gMaxIteration) {
    // write a non-transparent black pixel
    return (uchar4){0, 0, 0, 0xff};
  } else {
    float mi3 = gMaxIteration / 3.f;
    if (iter <= (gMaxIteration / 3))
      return (uchar4){0xff * (iter / mi3), 0, 0, 0xff};
    else if (iter <= (((gMaxIteration / 3) * 2)))
      return (uchar4){0xff - (0xff * ((iter - mi3) / mi3)),
                      (0xff * ((iter - mi3) / mi3)), 0, 0xff};
    else
      return (uchar4){0, 0xff - (0xff * ((iter - (mi3 * 2)) / mi3)),
                      (0xff * ((iter - (mi3 * 2)) / mi3)), 0xff};
  }
}