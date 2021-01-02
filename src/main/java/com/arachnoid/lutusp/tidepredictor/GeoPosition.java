/*
***************************************************************************
*   Copyright (C) 2017 by Paul Lutus                                      *
*   http://arachnoid.com/administration                                   *
*                                                                         *
*   This program is free software; you can redistribute it and/or modify  *
*   it under the terms of the GNU General Public License as published by  *
*   the Free Software Foundation; either version 2 of the License, or     *
*   (at your option) any later version.                                   *
*                                                                         *
*   This program is distributed in the hope that it will be useful,       *
*   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
*   GNU General Public License for more details.                          *
*                                                                         *
*   You should have received a copy of the GNU General Public License     *
*   along with this program; if not, write to the                         *
*   Free Software Foundation, Inc.,                                       *
*   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
***************************************************************************/


package com.arachnoid.lutusp.tidepredictor;

import android.support.annotation.NonNull;

/**
 * Created by lutusp on 9/20/17.
 */

final public class GeoPosition implements Comparable<GeoPosition> {
    int index = -1;
    String name = "";
    double latRadians = 0, lngRadians = 0, rad = 0, brg = 0;
    double degToRad = Math.PI / 180.0;

    public GeoPosition(double latDegrees, double lngDegrees) {
        this.latRadians = latDegrees * degToRad;
        this.lngRadians = lngDegrees * degToRad;
    }

    public GeoPosition(int index, String record) {
        this.index = index;
        if (record != null) {
            String[] fields = record.split("\t");
            if (fields.length > 5) {
                name = fields[3];
                this.lngRadians = Double.parseDouble(fields[4]) * degToRad;
                this.latRadians = Double.parseDouble(fields[5]) * degToRad;
            }
        }
    }

    // assign to argument instance
    // the result of a distance computation
    public void comp_radius(GeoPosition x) {
        double dLat = (latRadians - x.latRadians) * 0.5;
        double dLon = (lngRadians - x.lngRadians) * 0.5;

        double a = Math.pow(Math.sin(dLat), 2) +
                Math.pow(Math.sin(dLon), 2) * Math.cos(x.latRadians) * Math.cos(latRadians);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        rad = 3440 * c; // nautical miles
        // bearing from origin to destination, degrees
        brg = Math.atan2(Math.sin(dLon) * Math.cos(latRadians), Math.cos(x.latRadians) * Math.sin(latRadians) - Math.sin(x.latRadians) * Math.cos(latRadians) * Math.cos(dLon)) / degToRad;
        brg = (brg + 360) % 360;
    }

    public String toString() {
        return String.format("[%s]: %d,%.4f,%.4f,%4f,%.4f", name, index, latRadians, lngRadians, rad, brg);
    }

    @Override
    public int compareTo(@NonNull GeoPosition o) {
        return (rad > o.rad) ? 1 : (rad < o.rad) ? -1 : 0;
    }
}
