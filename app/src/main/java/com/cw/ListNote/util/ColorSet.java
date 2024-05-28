/*
 * Copyright (C) 2019 CW Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.ListNote.util;
/**
 * Created by CW on 2016/7/16.
 */
import android.app.Activity;
import android.graphics.Color;

import com.cw.ListNote.R;

public class ColorSet{
    // style color array
    // 0,2,4,6,8: dark background, 1,3,5,7,9: light background
    public static int[] mBG_ColorArray = new int[]{
            // 0
            Color.rgb(34,34,34), //#222222
            Color.rgb(255,255,255),
            Color.rgb(38,87,51), //#265733
            Color.rgb(186,249,142),
            Color.rgb(87,38,51),//#572633
            // 5
            Color.rgb(249,186,142),
            Color.rgb(38,51,87),//#263357
            Color.rgb(142,186,249),
            Color.rgb(87,87,51),//#575733
            Color.rgb(249,249,140),

            // traditional China color pair
            // 10
            Color.parseColor("#66A9C9"),
            Color.parseColor("#F0C9CF"),

            Color.parseColor("#2C2F3B"),
            Color.parseColor("#FAC03D"),

            Color.parseColor("#D24735"),
            // 15
            Color.parseColor("#F7EEAD"),

            Color.parseColor("#00695A"),
            Color.parseColor("#BE9457"),

            Color.parseColor("#86D2ec"),
            Color.parseColor("#ee7934"),
            // note: must sync with select_style.xml
            };
    public static int[] mText_ColorArray = new int[]{
            // 0
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            // 5
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),

            // traditional China color pair
            // 10
            Color.parseColor("#F0C9CF"),
            Color.parseColor("#66A9C9"),

            Color.parseColor("#FAC03D"),
            Color.parseColor("#2C2F3B"),

            Color.parseColor("#F7EEAD"),
            // 15
            Color.parseColor("#D24735"),

            Color.parseColor("#BE9457"),
            Color.parseColor("#00695A"),

            Color.parseColor("#ee7934"),
            Color.parseColor("#86D2ec"),
            // note: must sync with select_style.xml
    };

    public static int color_white = Color.rgb(255,255,255);
    public static int color_black = Color.rgb(0,0,0);

    public static int getBarColor(Activity act)
    {
        return act.getResources().getColor(R.color.bar_color);
    }

    public static int getButtonColor(Activity act)
    {
        return act.getResources().getColor(R.color.button_color);
    }

    public static int getHighlightColor(Activity act)
    {
        return act.getResources().getColor(R.color.highlight_color);
    }

    public static int getPauseColor(Activity act)
    {
        return act.getResources().getColor(R.color.pause_color);
    }
}
