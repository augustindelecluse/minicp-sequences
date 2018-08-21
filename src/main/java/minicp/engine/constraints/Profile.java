/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2017. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */


package minicp.engine.constraints;

import minicp.util.Entry;

import java.util.ArrayList;
import java.util.Arrays;

public class Profile {

    public static class Rectangle {

        final int start;
        final long dur;
        final int height;
        final int end;

        Rectangle(int start, int end, int height) {
            assert (end > start);
            this.start = start;
            this.end = end;
            this.dur = ((long) end) - start;
            this.height = height;
        }

        @Override
        public String toString() {
            return "[solve:" + start + " dur:" + dur + " end:" + (end) + "] h:" + height;
        }
    }

    final Rectangle[] profileRectangles;


    public Profile(Rectangle... rectangles) {
        ArrayList<Rectangle> profile = new ArrayList<Rectangle>();
        Entry[] points = new Entry[2 * rectangles.length + 2];
        for (int i = 0; i < rectangles.length; i++) {
            Rectangle r = rectangles[i];
            points[i] = new Entry(r.start, r.height);
            points[rectangles.length + i] = new Entry(r.end, -r.height);
        }
        points[2 * rectangles.length] = new Entry(Integer.MIN_VALUE, 0);
        points[2 * rectangles.length + 1] = new Entry(Integer.MAX_VALUE, 0);

        Arrays.sort(points);

        int sweep_h = 0;
        int sweep_t = points[0].key;
        for (Entry e : points) {
            int t = e.key;
            int h = e.value;
            if (t != sweep_t) {
                //System.out.println(sweep_t+" "+t);
                profile.add(new Rectangle(sweep_t, t, sweep_h));
                sweep_t = t;
            }
            sweep_h += h;
        }
        this.profileRectangles = profile.toArray(new Rectangle[0]);

    }

    /**
     * @param t
     * @return the rectangle r of the profileRectangles such that r.solve <= t and r.end > t
     */
    public int rectangleIndex(int t) {
        for (int i = 0; i < profileRectangles.length; i++) {
            if (profileRectangles[i].start <= t && profileRectangles[i].end > t)
                return i;
        }
        return -1;
    }

    /**
     * @return the number of rectangles in the profileRectangles
     */
    public int size() {
        return profileRectangles.length;
    }

    /**
     * @param i
     * @return the i_th rectangle of the profileRectangles
     */
    public Rectangle get(int i) {
        return profileRectangles[i];
    }


    @Override
    public String toString() {
        return Arrays.toString(profileRectangles);
    }
}
