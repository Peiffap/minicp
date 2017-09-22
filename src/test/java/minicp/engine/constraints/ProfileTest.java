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


import minicp.util.NotImplementedException;
import org.junit.Assume;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;


import minicp.engine.constraints.Profile.Rectangle;


public class ProfileTest {


    private boolean checkProfile(Rectangle ... rectangles) {
        Profile p = new Profile(rectangles);
        int [] discreteProfile_p = discreteProfile(p.profileRectangles);
        int [] discreteProfile_r = discreteProfile(rectangles);
        if (!Arrays.equals(discreteProfile_p,discreteProfile_r)) {
            System.out.println("not same profile");
            return false;
        }

        for (int i = 0; i < p.profileRectangles.length-1; i++) {
            if (p.profileRectangles[i].end != p.profileRectangles[i+1].start) {
                System.out.println("not continuous rectangles");
                return false;
            }
        }
        return p.size() <= 2* rectangles.length+2;
    }


    private int[] discreteProfile(Rectangle ... rectangles) {
        int min = Arrays.stream(rectangles).filter(r -> r.height > 0).map(r -> r.start).min(Integer::compare).get();
        int max = Arrays.stream(rectangles).filter(r -> r.height > 0).map(r -> r.end).max(Integer::compare).get();
        int[] heights = new int[max-min];
        // discrete profileRectangles of rectangles
        for (Rectangle r: rectangles) {
            if (r.height > 0) {
                for (int i = r.start; i < r.end; i++) {
                    heights[i-min] += r.height;
                }
            }
        }
        return heights;
    }



    @Test
    public void testProfile1() {
        try {

                Profile.Rectangle r1 = new Profile.Rectangle(7,11,3);
                Profile.Rectangle r2 = new Profile.Rectangle(2,10,1);
                Profile.Rectangle r3 = new Profile.Rectangle(3,4,2);
                assert(checkProfile(r1,r2,r3));

        } catch (NotImplementedException e) {
            Assume.assumeNoException(e);
        }
    }


    @Test
    public void testProfile2() {
        try {

            Profile.Rectangle r1 = new Profile.Rectangle(1,10,3);
            Profile.Rectangle r2 = new Profile.Rectangle(1,10,1);
            Profile.Rectangle r3 = new Profile.Rectangle(1,10,2);
            assert(checkProfile(r1,r2,r3));

        } catch (NotImplementedException e) {
            Assume.assumeNoException(e);
        }
    }


    @Test
    public void testProfileRandom() {
        try {

            Random r = new Random(0);

            for (int iter = 0; iter < 10; iter++) {
                Rectangle[] rects = IntStream.range(0, 10).mapToObj(i -> {
                    int start = r.nextInt(100);
                    int end = start + 1 + r.nextInt(30);
                    int height = 1 + r.nextInt(30);
                    return new Rectangle(start, end, height);
                }).toArray(Rectangle[]::new);
                assert (checkProfile(rects));

            }

        } catch (NotImplementedException e) {
            Assume.assumeNoException(e);
        }
    }



}
