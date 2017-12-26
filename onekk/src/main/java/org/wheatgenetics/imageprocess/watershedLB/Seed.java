package org.wheatgenetics.imageprocess.watershedLB;

import android.util.Pair;

import java.util.ArrayList;

/**
 * Created by chaneylc on 8/16/2017.
 */

class Seed {

    ArrayList<Pair<Double, Double>> tracks;
    int sid, state, fc, age, updates, maxAge;
    double cx, cy, px, py;
    boolean done;

    Seed(int sid, double cx, double cy, int maxAge, int fc) {

        tracks = new ArrayList<>();
        this.px = 0;
        this.py = 0;
        this.sid = sid;
        this.cx = cx;
        this.cy = cy;
        this.state = 0; //1 = new, 2 = active, 3 = dead
        this.maxAge = maxAge;
        this.fc = fc;
        this.age = 0;
        this.updates = 0;
        this.done = false;
    }

    public void updateCoords(double xn, double yn) {

        this.age = 0;
        this.tracks.add(tracks.size(), new Pair<>(cx, cy));
        this.cx = xn;
        this.cy = yn;
        this.updates += 1;
    }

    public void updatePredictions(double xn, double yn) {
        tracks.add(new Pair<>(this.cx, this.cy));
        this.cx = xn;
        this.cy = yn;
    }

    public void setDone() {
        this.state = 3;
        this.done = true;
    }

    public boolean ageOne() {
        this.age += 1;
        return true;
    }
}