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

import java.util.Calendar;
import java.util.Locale;

import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.page.Checked_notes_option;
import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.preferences.Pref;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

public class Util
{
    SharedPreferences mPref_vibration;
    private static Context mContext;
	private Activity mAct;
    private static DB_folder mDbFolder;
    public static String NEW_LINE = "\r" + System.getProperty("line.separator");

	private static int STYLE_DEFAULT = 1;

	private int defaultBgClr;
	private int defaultTextClr;

	public Util(){}
    
	public Util(AppCompatActivity activity) {
		mContext = activity;
		mAct = activity;
	}
	
	public Util(Context context) {
		mContext = context;
	}
	
	// set vibration time
	public void vibrate()
	{
		mPref_vibration = mContext.getSharedPreferences("vibration", 0);
    	if(mPref_vibration.getString("KEY_ENABLE_VIBRATION","yes").equalsIgnoreCase("yes"))
    	{
			Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			if(!mPref_vibration.getString("KEY_VIBRATION_TIME","25").equalsIgnoreCase(""))
			{
				int vibLen = Integer.valueOf(mPref_vibration.getString("KEY_VIBRATION_TIME","25"));
				mVibrator.vibrate(vibLen); //length unit is milliseconds
				System.out.println("vibration len = " + vibLen);
			}
    	}
	}
	
    // get current time string
    public static String getCurrentTimeString()
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		
//		int hour = cal.get(Calendar.HOUR);//12h 
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int mSec = cal.get(Calendar.MILLISECOND);
		
		String strTime = year 
				+ "" + String.format(Locale.US,"%02d", month)
				+ "" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "_" + String.format(Locale.US,"%02d", hour)
				+ "" + String.format(Locale.US,"%02d", min)
				+ "" + String.format(Locale.US,"%02d", sec) 
				+ "_" + String.format(Locale.US,"%03d", mSec);
//		System.out.println("time = "+  strTime );
		return strTime;
    }
    
    // get time string
    public static String getTimeString(Long time)
    {
    	if(time == null)
    		return "";

		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		int hour = cal.get(Calendar.HOUR);//12h 
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "    " + String.format(Locale.US,"%02d", hour)
				+ ":" + String.format(Locale.US,"%02d", min)
				+ ":" + String.format(Locale.US,"%02d", sec) ;
//		System.out.println("time = "+  strTime );
		
		return strTime;
    }
    
    // add mark to current page
	public void addMarkToCurrentPage(DialogInterface dialogInterface,final int action)
	{
		mDbFolder = new DB_folder(MainAct.mAct, Pref.getPref_focusView_folder_tableId(MainAct.mAct));
	    ListView listView = ((AlertDialog) dialogInterface).getListView();
	    final ListAdapter originalAdapter = listView.getAdapter();
	    final int style = Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());
        CheckedTextView textViewDefault = new CheckedTextView(mAct) ;
        defaultBgClr = textViewDefault.getDrawingCacheBackgroundColor();
        defaultTextClr = textViewDefault.getCurrentTextColor();

	    listView.setAdapter(new ListAdapter()
	    {
	        @Override
	        public int getCount() {
	            return originalAdapter.getCount();
	        }
	
	        @Override
	        public Object getItem(int id) {
	            return originalAdapter.getItem(id);
	        }
	
	        @Override
	        public long getItemId(int id) {
	            return originalAdapter.getItemId(id);
	        }
	
	        @Override
	        public int getItemViewType(int id) {
	            return originalAdapter.getItemViewType(id);
	        }
	
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view = originalAdapter.getView(position, convertView, parent);
	            //set CheckedTextView in order to change button color
	            CheckedTextView textView = (CheckedTextView)view;
	            if(mDbFolder.getPageTableId(position,true) == TabsHost.getCurrentPageTableId())
	            {
		            textView.setTypeface(null, Typeface.BOLD_ITALIC);
		            textView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		            textView.setTextColor(ColorSet.mText_ColorArray[style]);
			        if(style%2 == 0)
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
			        else
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_light);

                    if(action == Checked_notes_option.MOVE_TO)
                        textView.setCheckMarkDrawable(null);
	            }
	            else
	            {
		        	textView.setTypeface(null, Typeface.NORMAL);
		            textView.setBackgroundColor(defaultBgClr);
		            textView.setTextColor(defaultTextClr);
		            textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
	            }
	            return view;
	        }

	        @Override
	        public int getViewTypeCount() {
	            return originalAdapter.getViewTypeCount();
	        }

	        @Override
	        public boolean hasStableIds() {
	            return originalAdapter.hasStableIds();
	        }
	
	        @Override
	        public boolean isEmpty() {
	            return originalAdapter.isEmpty();
	        }

	        @Override
	        public void registerDataSetObserver(DataSetObserver observer) {
	            originalAdapter.registerDataSetObserver(observer);
	
	        }
	
	        @Override
	        public void unregisterDataSetObserver(DataSetObserver observer) {
	            originalAdapter.unregisterDataSetObserver(observer);
	
	        }
	
	        @Override
	        public boolean areAllItemsEnabled() {
	            return originalAdapter.areAllItemsEnabled();
	        }
	
	        @Override
	        public boolean isEnabled(int position) {
	            return originalAdapter.isEnabled(position);
	        }
	    });
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",STYLE_DEFAULT);
	}
	
	
	// set button color
    public static void setButtonColor(RadioButton rBtn,int styleId)
    {
    	if(styleId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(ColorSet.mBG_ColorArray[styleId]);
	    rBtn.setText(String.valueOf(styleId+1));
		rBtn.setTextColor(ColorSet.mText_ColorArray[styleId]);
    }
	
    // get current page style
	static public int getCurrentPageStyle(int page_pos)
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(MainAct.mAct);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
        return db.getPageStyle(page_pos, true);
	}

	// get style count
	static public int getStyleCount()
	{
		return ColorSet.mBG_ColorArray.length;
	}

	// is Empty string
	public static boolean isEmptyString(String str)
	{
		boolean empty = true;
		if( str != null )
		{
			if(str.length() > 0 )
				empty = false;
		}
		return empty;
	}
	
	static public boolean isLandscapeOrientation(Activity act)
	{
		int currentOrientation = act.getResources().getConfiguration().orientation;

		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
			return true;
		else
			return false;
	}

	static public boolean isPortraitOrientation(Activity act)
	{
		int currentOrientation = act.getResources().getConfiguration().orientation;

		if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
			return true;
		else
			return false;
	}


	// set full screen for no immersive sticky
	public static void setFullScreen_noImmersive(Activity act)
	{
//		System.out.println("Util / _setFullScreen_noImmersive");
		Window win = act.getWindow();

		if (Build.VERSION.SDK_INT < 16)
		{
			win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		else
		{
			// show the status bar and navigation bar
			View decorView = act.getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;//full screen
			decorView.setSystemUiVisibility(uiOptions);
		}
	}

}
