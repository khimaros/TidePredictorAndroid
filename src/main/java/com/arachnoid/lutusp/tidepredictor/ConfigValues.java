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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by lutusp on 9/15/17.
 */

final public class ConfigValues implements Serializable {

    // opened if no other site is opened
    public int lastDisplayedSite = -1;
    public String favorite_sites = "";
    public int currentTab = 0;

    // height units 0 = meters, 1 = feet
    // velocity units 2 = knots, 3 = mph, 4 = m/s
    public int displayUnits = 1;

    public int heightUnits = 1;
    public int velocityUnits = 2;

    // 0 = standard, 1 = compute, 2 = daylight
    public int daylightTime = 1;

    public double timeZone = -14; // -14 means use system timezone

    public boolean ampmFlag = true;
    public boolean thickLine = false;
    public boolean chartGrid = true;
    public boolean gridNums = true;
    public boolean listBackground = false;
    public boolean boldText = false;
    public boolean sunText = true;
    public boolean dateText = true;
    public boolean sunGraphic = true;
    public boolean sunGraphicDark = true;
    public boolean siteLabel = true;
    public boolean tideList = true;
    public boolean timeLine = true;
    public boolean dataFileCreation = false;
    public boolean useNearestGps = false;
    public ArrayList<String> decodedHarmonics = null;
}
