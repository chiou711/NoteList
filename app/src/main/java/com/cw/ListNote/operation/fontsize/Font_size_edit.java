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

package com.cw.ListNote.operation.fontsize;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cw.ListNote.R;
import com.cw.ListNote.util.ColorSet;
import com.cw.ListNote.util.preferences.Pref;

public class Font_size_edit extends Activity {

    Font_size_edit_ui note_edit_ui;

    TextView titleText;
    TextView bodyText;
    private float titleFontSize,bodyFontSize;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_font_size_edit);
        setTitle(R.string.note_font_size);// set title
    	
        System.out.println("Note_edit / onCreate");
        titleText  = findViewById(R.id.edit_title);
        bodyText = findViewById(R.id.edit_body);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));

        //initialization
        note_edit_ui = new Font_size_edit_ui(this);
        note_edit_ui.UI_init();

    	// show view
		note_edit_ui.populateFields_all();

        float title_px = titleText.getTextSize();
        float title_density = getResources().getDisplayMetrics().scaledDensity;
        titleFontSize = title_px / title_density;

        float body_px = bodyText.getTextSize();
        float body_density = getResources().getDisplayMetrics().scaledDensity;
        bodyFontSize = body_px / body_density;

		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }

        });
        
        // up button
        Button title_size_up = (Button) findViewById(R.id.title_font_size_up);
        // delete
        title_size_up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                titleFontSize += 4f;
                titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleFontSize);
                Pref.setPref_title_font_size(Font_size_edit.this,(int) titleFontSize);
            }
        });
        
        // down button
        Button title_size_down = (Button) findViewById(R.id.title_font_size_down);
        title_size_down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                titleFontSize -= 4f;
                titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleFontSize);
                Pref.setPref_title_font_size(Font_size_edit.this,(int) titleFontSize);
            }
        });

        // body up button
        Button body_size_up = (Button) findViewById(R.id.body_font_size_up);
        // delete
        body_size_up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                bodyFontSize += 4f;
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyFontSize);
                Pref.setPref_body_font_size(Font_size_edit.this,(int) bodyFontSize);
            }
        });

        // body down button
        Button body_size_down = (Button) findViewById(R.id.body_font_size_down);
        body_size_down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                bodyFontSize -= 4f;
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyFontSize);
                Pref.setPref_body_font_size(Font_size_edit.this,(int) bodyFontSize);
            }
        });
    }
    
    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()){
		    case android.R.id.home:
		        finish();
		        return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
