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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lutusp on 9/16/17.
 */

public final class PlaceHolderFragment extends android.support.v4.app.Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    private final int[] fragment_array;
    //private final int[] label_array;

    public PlaceHolderFragment() {
        fragment_array = new int[]{R.layout.sites_fragment, R.layout.tools_fragment, R.layout.chart_fragment, R.layout.calendar_fragment};
        //label_array = new int[]{R.id.section_label_1, R.id.section_label_2, R.id.section_label_3};
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int index = getArguments().getInt("index");
        return inflater.inflate(fragment_array[index], container, false);
        //Log.e("onCreateView", "" + this + "," + index);
        //TextView textView = (TextView) rootView.findViewById(label_array[index]);
        //textView.setText(getString(R.string.section_format, index));
        //return rootView;
    }
}
