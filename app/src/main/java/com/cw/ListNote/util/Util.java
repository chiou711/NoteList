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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.page.Checked_notes_option;
import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.preferences.Pref;
import com.cw.ListNote.define.Define;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Util 
{
    SharedPreferences mPref_vibration;
    private static Context mContext;
	private Activity mAct;
	private String mEMailString;
    private static DB_folder mDbFolder;
    public static String NEW_LINE = "\r" + System.getProperty("line.separator");

	private static int STYLE_DEFAULT = 1;

	private int defaultBgClr;
	private int defaultTextClr;

	public static final int PERMISSIONS_REQUEST_STORAGE = 13;
	public static final int PERMISSIONS_REQUEST_STORAGE_IMPORT = 14;
	public static final int PERMISSIONS_REQUEST_STORAGE_EXPORT = 15;

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
	
	// export to SD card: for checked pages
	public String exportToSdCard(String filename, List<Boolean> checkedTabs)
	{   
		//first row text
		String data ="";

		//get data from DB
		data = queryDB(data,checkedTabs);
		
		// sent data
		data = addXmlTag(data);
		mEMailString = data;

        exportToSdCardFile(filename,data);

		return mEMailString;
	}
	
	// Export data to be SD Card file
	public void exportToSdCardFile(String filename,String data)
	{
	    // SD card path + "/" + directory path
	    String dirString = Environment.getExternalStorageDirectory().toString() +
	    		              "/" +
	    		              Util.getStorageDirName(mContext);

		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();
		File file = new File(dir, filename);
		file.setReadOnly();

//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(file);
//		} catch (IOException e1) {
//			System.out.println("_FileWriter error");
//			e1.printStackTrace();
//		}
//		BufferedWriter bw = new BufferedWriter(fw);

		BufferedWriter bw = null;
		OutputStreamWriter osw = null;

		int BUFFER_SIZE = 8192;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8");
			bw = new BufferedWriter(osw,BUFFER_SIZE);

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		try {
			bw.write(data);
			bw.flush();
			osw.close();
			bw.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
    /**
     * Query current data base
     *
     */
    private String queryDB(String data, List<Boolean> checkedTabs)
    {
    	String curData = data;
    	
    	// folder
    	int folderTableId = Pref.getPref_focusView_folder_tableId(mContext);
    	mDbFolder = new DB_folder(mContext, folderTableId);

    	// page
    	int tabCount = checkedTabs.size();
    	for(int i=0;i<tabCount;i++)
    	{
            if(checkedTabs.get(i))
				curData = curData.concat(getStringWithXmlTag(i, ID_FOR_TABS));
    	}
    	return curData;
    	
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
    
//    void deleteAttachment(String mAttachmentFileName)
//    {
//		// delete file after sending
//		String attachmentPath_FileName = Environment.getExternalStorageDirectory().getPath() + "/" +
//										 mAttachmentFileName;
//		File file = new File(attachmentPath_FileName);
//		boolean deleted = file.delete();
//		if(deleted)
//			System.out.println("delete file is OK");
//		else
//			System.out.println("delete file is NG");
//    }
    
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
	
	// get App default storage directory name
	static public String getStorageDirName(Context context)
	{
//		return context.getResources().getString(R.string.app_name);

		Resources currentResources = context.getResources();
		Configuration conf = new Configuration(currentResources.getConfiguration());
		conf.locale = Locale.ENGLISH; // apply English to avoid reading directory error
		Resources newResources = new Resources(context.getAssets(), 
											   currentResources.getDisplayMetrics(),
											   conf);
		String appName = newResources.getString(R.string.app_name);

		// restore locale
		new Resources(context.getAssets(), 
					  currentResources.getDisplayMetrics(), 
					  currentResources.getConfiguration());
		
//		System.out.println("Util / _getStorageDirName / appName = " + appName);
		return appName;		
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",STYLE_DEFAULT);
	}
	
	
	// set button color
	private static String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
    public static void setButtonColor(RadioButton rBtn,int iBtnId)
    {
    	if(iBtnId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(ColorSet.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(ColorSet.mText_ColorArray[iBtnId]);
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

    private static int ID_FOR_TABS = -1;
    public static int ID_FOR_NOTES = -2;
    /**
     * Get string with XML tags
     * @param tabPos tab position
     * @param noteId: ID_FOR_TABS for checked tabs(pages), ID_FOR_NOTES for checked notes
     * @return string with tags
     */
	public static String getStringWithXmlTag(int tabPos,long noteId)
	{
		String PAGE_TAG_B = "<page>";
		String PAGE_NAME_TAG_B = "<page_name>";
		String PAGE_NAME_TAG_E = "</page_name>";
		String NOTE_ITEM_TAG_B = "<note>";
		String NOTE_ITEM_TAG_E = "</note>";
		String TITLE_TAG_B = "<title>";
		String TITLE_TAG_E = "</title>";
		String BODY_TAG_B = "<body>";
		String BODY_TAG_E = "</body>";
		String PAGE_TAG_E = "</page>";

		String sentString = NEW_LINE;

		int pageTableId = TabsHost.mTabsPagerAdapter.getItem(tabPos).page_tableId;
		List<Long> noteIdArray = new ArrayList<>();

		DB_page dbPage = new DB_page(MainAct.mAct, pageTableId);
        dbPage.open();

        int count = dbPage.getNotesCount(false);

		if(noteId == ID_FOR_TABS)
		{
			for (int i = 0; i < count; i++)
                noteIdArray.add(i, dbPage.getNoteId(i, false));
		}
        else if(noteId == ID_FOR_NOTES)
        {
            // for checked notes
            int j=0;
            for (int i = 0; i < count; i++)
            {
                if(dbPage.getNoteMarking(i,false) == 1) {
                    noteIdArray.add(j, dbPage.getNoteId(i, false));
                    j++;
                }
            }
        }
		else
			noteIdArray.add(0, noteId);//only one for View note case

        dbPage.close();

		// when page has page name only, no notes
		if(noteIdArray.size() == 0)
		{
			sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
			sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + mDbFolder.getCurrentPageTitle() + PAGE_NAME_TAG_E);
			sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
			sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + TITLE_TAG_E);
			sentString = sentString.concat(NEW_LINE + BODY_TAG_B +  BODY_TAG_E);
			sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
			sentString = sentString.concat(NEW_LINE + PAGE_TAG_E );
			sentString = sentString.concat(NEW_LINE);
		}
		else
		{
			for(int i=0;i< noteIdArray.size();i++)
			{
				dbPage.open();
				Cursor cursorNote = dbPage.queryNote(noteIdArray.get(i));
                String title = cursorNote.getString(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_TITLE));
				title = replaceEscapeCharacter(title);

				String body = cursorNote.getString(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_BODY));
				body = replaceEscapeCharacter(body);

				int mark = cursorNote.getInt(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_MARKING));
				String srtMark = (mark == 1)? "[s]":"[n]";
				dbPage.close();

				if(i==0)
				{
					DB_folder db_folder = new DB_folder(MainAct.mAct, Pref.getPref_focusView_folder_tableId(MainAct.mAct));
					sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
					sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + db_folder.getCurrentPageTitle() + PAGE_NAME_TAG_E );
				}

				sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
				sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + srtMark + title + TITLE_TAG_E);
				sentString = sentString.concat(NEW_LINE + BODY_TAG_B + body + BODY_TAG_E);
				sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
				sentString = sentString.concat(NEW_LINE);
				if(i==noteIdArray.size()-1)
					sentString = sentString.concat(NEW_LINE +  PAGE_TAG_E);

			}
		}
		return sentString;
	}


    // replace special character (e.q. amp sign) for avoiding XML paring exception 
	//      &   &amp;
	//      >   &gt;
	//      <   &lt;
	//      '   &apos;
	//      "   &quot;
	private static String replaceEscapeCharacter(String str)
	{
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll("\"", "&quot;");
        return str;
	}
	
	// add XML tag
	public static String addXmlTag(String str)
	{
		String ENCODING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String XML_TAG_B = NEW_LINE + "<ListNote>";
        String XML_TAG_E = NEW_LINE + "</ListNote>";
        
        String data = ENCODING + XML_TAG_B;
        
        data = data.concat(str);
		data = data.concat(XML_TAG_E);
		
		return data;
	}

	// trim XML tag
	public String trimXMLtag(String string) {
		string = string.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");
		string = string.replace("<ListNote>","");
		string = string.replace("<page>","");
		string = string.replace("<page_name>","=== Page: ");
		string = string.replace("</page_name>"," ===");
		string = string.replace("<note>","--- note ---");
		string = string.replace("[s]","");
		string = string.replace("[n]","");
		string = string.replace("<title></title>"+NEW_LINE,"");
        string = string.replace("<body></body>"+NEW_LINE,"");
		string = string.replace("<title>","Title: ");
		string = string.replace("</title>","");
		string = string.replace("<body>","Body: ");
		string = string.replace("</body>","");
		string = string.replace("</note>","");
		string = string.replace("</page>"," ");
		string = string.replace("</ListNote>","");
		string = string.trim();
		return string;
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
	
    static String mStringUrl;
    public static int mResponseCode;
    static String mResponseMessage;
	public static int oneSecond = 1000;
    
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


	static public void lockOrientation(Activity act) {
//	    if (act.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//	        act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
//	    } else {
//	        act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
//	    }
	    
	    int currentOrientation = act.getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	    }
	    else {
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	    }	    
	}

	static public void unlockOrientation(Activity act) {
	    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	// get time format string
	static public String getTimeFormatString(long duration)
	{
		long hour = TimeUnit.MILLISECONDS.toHours(duration);
		long min = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hour);
		long sec = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.HOURS.toSeconds(hour) - TimeUnit.MINUTES.toSeconds(min);
		String str = String.format(Locale.US,"%2d:%02d:%02d", hour, min, sec);
		return str;
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
