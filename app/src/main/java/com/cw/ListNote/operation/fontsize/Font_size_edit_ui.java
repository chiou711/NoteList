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

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.ColorSet;
import com.cw.ListNote.util.preferences.Pref;

public class Font_size_edit_ui {

	private TextView titleEditText;
	private TextView bodyEditText;
	private Activity act;
	private int style;
	Font_size_edit_ui(Activity act){
    	this.act = act;
    }

	void UI_init()
    {
		UI_init_text();

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

	private void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText = act.findViewById(R.id.edit_title);
		bodyEditText = act.findViewById(R.id.edit_body);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set body color
		bodyEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		bodyEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

    // populate text fields
	void populateFields_text(){
		// title
		String strTitleEdit = "text_title";
		titleEditText.setText(strTitleEdit);

		// body
		String strBodyEdit = "text_body";
		bodyEditText.setText(strBodyEdit);
	}

    // populate all fields
	void populateFields_all(){
			populateFields_text();
    }

}