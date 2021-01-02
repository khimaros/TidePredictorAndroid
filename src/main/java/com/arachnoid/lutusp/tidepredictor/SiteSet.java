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

/**
 * Created by lutusp on 9/15/17.
 */

final public class SiteSet {
    public String fullName;
    public String name;
    public String shortName;
    int indexNumber;
    int units; // 0 = meters, 1 = feet, 2 = knots, 3 = MPH, 4 = m/s
    int currentDisplayUnits;
    int currentYear;
    boolean daylightInEffect = false;
    boolean current; // site measures current
    boolean needRoot;
    boolean valid;
    double baseHeight;
    double tz;
    double lat;
    double lng;
    double gHiWater;
    double gLoWater;
    double mHiWater;
    double mLoWater;
    int constituentMax;
    int startYear;
    int equMax,nodeMax,endYear;
    int currentLoadedFile;
    long epochTime;
    String dataFileName;
    double constSpeeds[];
    double[][] nodeFacts,equArgs;
    Harmonic harmBase[];
    Harmonic harm[];
    public SiteSet() {
        indexNumber = -1;
        currentYear = -1;
        current = false;
        needRoot = false;
        valid = false;
        gHiWater = 10;
        gLoWater = -2;
        mHiWater = 10;
        mLoWater = -1;
        constituentMax = 0;
        currentLoadedFile = -1;
        currentDisplayUnits = -1;
    }
}
