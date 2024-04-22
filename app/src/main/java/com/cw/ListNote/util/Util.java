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
	
	public static boolean isYouTubeLink(String strLink)
	{
		boolean is = false;
		
		if(Util.isEmptyString(strLink) || (!strLink.startsWith("http")))
			return is;
		
//		//check if single string
//		String strArr[] = strLink.split("\\s"); // \s: A whitespace character, short for [ \t\n\x0b\r\f]
//		int cnt = 0;
//		for(int i=0; i < strArr.length; i++ )
//		{
//			System.out.println("strArr [" + i + "] = " + strArr[i]);
//			cnt++;
//		}
//		
//		//check if youTube keyword
//		if( (cnt == 1) &&
//			(strLink.contains("youtube") ||
//			 strLink.contains("youtu.be")  )&&
//			strLink.contains("//")) 
//		{
//			is = true;
//		}
		
		//check if single string
		String strArr[] = strLink.split("/");
//		for(int i=0; i < strArr.length; i++ )
//		{
//			System.out.println("YouTube strArr [" + i + "] = " + strArr[i]);
//		}
		
		if(strArr.length >= 2)
		{
			if(	strArr[2].contains("youtube") ||
				strArr[2].contains("youtu.be")  ) 		
			{
				is = true;
			}		
		}
		
		return is;
	}
	
    @TargetApi(Build.VERSION_CODES.KITKAT)
	public static Intent chooseMediaIntentByType(Activity act,String type)
    {
	    // set multiple actions in Intent 
	    // Refer to: http://stackoverflow.com/questions/11021021/how-to-make-an-intent-with-multiple-actions
        PackageManager pkgMgr = act.getPackageManager();
		Intent intentSaf;
		Intent intent;
        Intent openInChooser;
        List<ResolveInfo> resInfoSaf;
		List<ResolveInfo> resInfo;
		List<LabeledIntent> intentList = new ArrayList<>();

        // SAF support starts from Kitkat
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
			// BEGIN_INCLUDE (use_open_document_intent)
	        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        	intentSaf = new Intent(Intent.ACTION_OPEN_DOCUMENT);

	        // Filter to only show results that can be "opened", such as a file (as opposed to a list
	        // of contacts or time zones)
        	intentSaf.addCategory(Intent.CATEGORY_OPENABLE);
        	intentSaf.setType(type);

        	// get extra SAF intents
	        resInfoSaf = pkgMgr.queryIntentActivities(intentSaf, 0);

	        for (int i = 0; i < resInfoSaf.size(); i++)
	        {
	            // Extract the label, append it, and repackage it in a LabeledIntent
	            ResolveInfo ri = resInfoSaf.get(i);
	            String packageName = ri.activityInfo.packageName;
				intentSaf.setComponent(new ComponentName(packageName, ri.activityInfo.name));

				// add span (CLOUD)
		        Spannable saf_span = new SpannableString(" (CLOUD)");
		        saf_span.setSpan(new ForegroundColorSpan(android.graphics.Color.RED), 0, saf_span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        CharSequence newSafLabel = TextUtils.concat(ri.loadLabel(pkgMgr), saf_span.toString());
//	        	System.out.println("Util / _chooseMediaIntentByType / SAF label " + i + " = " + newSafLabel );
//				extraIntentsSaf[i] = new LabeledIntent(intentSaf, packageName, newSafLabel, ri.icon);

				intentList.add(new LabeledIntent(intentSaf,packageName,newSafLabel,ri.icon));
	        }
        }
        
        // get extra non-SAF intents
		intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(type);
        resInfo = pkgMgr.queryIntentActivities(intent, 0);
        for (int i = 0; i < resInfo.size(); i++)
        { ResolveInfo ri = resInfo.get(i);
			String packageName = ri.activityInfo.packageName;
			intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
			intentList.add(new LabeledIntent(intent,packageName,ri.loadLabel(pkgMgr),ri.icon));
        }


        // remove duplicated item
        for(int i=0;i<intentList.size();i++)
		{
			ComponentName name1 = intentList.get(i).getComponent();
//			System.out.println("---> intentList.size() = " + intentList.size());
//			System.out.println("---> name1 = " + name1);
			for(int j=i+1;j<intentList.size();j++)
			{
				ComponentName name2 = intentList.get(j).getComponent();
//				System.out.println("---> intentList.size() = " + intentList.size());
//				System.out.println("---> name2 = " + name2);
				if( name1.equals(name2)) {
//					System.out.println("---> will remove");
					intentList.remove(j);
					j=intentList.size();
				}
			}
		}

		// check
		for(int i=0; i<intentList.size() ; i++)
		{
			System.out.println("--> intent list ("+ i +")" + intentList.get(i).toString());
		}
        
        // OK to put extra
        CharSequence charSeq = "";
        
        if(type.startsWith("image"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_image);
        else if(type.startsWith("video"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_video);
        else if(type.startsWith("audio"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_audio);

		openInChooser = Intent.createChooser(intentList.remove(intentList.size()-1), charSeq);//remove duplicated item
		LabeledIntent[] extraIntentsFinal = intentList.toArray(new LabeledIntent[intentList.size()]);
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntentsFinal);
                	
        return openInChooser;
    }

    public static void setScrollThumb(Context context, View view)
    {
		// Change scroll thumb by reflection
		// ref: http://stackoverflow.com/questions/21806852/change-the-color-of-scrollview-programmatically?lq=1
		try
		{
		    Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
		    mScrollCacheField.setAccessible(true);
		    Object mScrollCache = mScrollCacheField.get(view);

		    Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
		    scrollBarField.setAccessible(true);
		    Object scrollBar = scrollBarField.get(mScrollCache);

		    Method method = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
		    method.setAccessible(true);
		    // Set drawable
		    method.invoke(scrollBar, context.getResources().getDrawable(R.drawable.fastscroll_thumb_default_holo));
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		}	    	
    }
    
    // Get YouTube Id
	public static String getYoutubeId(String url) {

	    String videoId = "";

		// format 1: https://www.youtube.com/watch?v=_sQSXwdtxlY
		// format 2: https://youtu.be/V7MfPD7kZuQ (notice: start with V)
	    if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
//			String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*";
            String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??(v=)?([^#\\&\\?]*).*";
	        CharSequence input = url;
	        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
	        Matcher matcher = pattern.matcher(input);
	        if (matcher.matches()) {
//				String groupIndex1 = matcher.group(7);
				String groupIndex1 = matcher.group(8);
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(0) = " + matcher.group(0));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(1) = " + matcher.group(1));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(2) = " + matcher.group(2));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(3) = " + matcher.group(3));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(4) = " + matcher.group(4));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(5) = " + matcher.group(5));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(6) = " + matcher.group(6));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(7) = " + matcher.group(7));
//				System.out.println("Util / _getYoutubeId 1 / matcher.group(8) = " + matcher.group(8));
	            if (groupIndex1 != null && groupIndex1.length() == 11)
	                videoId = groupIndex1;
	        }
	    }
//		System.out.println("Util / _getYoutubeId / video_id = " + videoId);

	    return videoId;
	}


    // Get YouTube list Id
    public static String getYoutubeListId(String url) {

        String videoId = "";

        if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
			String expression = "^.*((youtu.be/)|(v/)|(/u/w/)|(embed/)|(watch\\?))\\??v?=?([^#&?]*).*list?=?([^#&?]*).*";
            CharSequence input = url;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(8);
                if (groupIndex1 != null )
                    videoId = groupIndex1;
            }
        }

        System.out.println("Util / _getYoutubeListId / list_id = " + videoId);
        return videoId;
    }

	// Get YouTube playlist Id
	public static String getYoutubePlaylistId(String url) {

		String videoId = "";

		if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
			String expression = "^.*((youtu.be/)|(v/)|(/u/w/)|(embed/)|(playlist\\?))\\??v?=?([^#&?]*).*list?=?([^#&?]*).*";
			CharSequence input = url;
			Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(input);
			if (matcher.matches()) {
				String groupIndex1 = matcher.group(8);
				if (groupIndex1 != null )
					videoId = groupIndex1;
			}
		}
		System.out.println("Util / _getYoutubePlaylistId / playlist_id = " + videoId);
		return videoId;
	}

	// Get YouTube title
	public static String getYouTubeTitle(String youtubeUrl)
	{
    		URL embeddedURL = null;
    		if (youtubeUrl != null) 
    		{
    			try {
					embeddedURL = new URL("http://www.youtube.com/oembed?url=" +
										   youtubeUrl +
										   "&format=json");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
    		}

			JsonAsync jsonAsyncTask = new JsonAsync();
    		jsonAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,embeddedURL);
    		isTimeUp = false;
			setupLongTimeout(1000);

			while(Util.isEmptyString(jsonAsyncTask.title) && (!isTimeUp))
    		{
    			//Add for Android 7.1.1 hang up after adding YouTube link
				try { Thread.sleep(100); } catch (InterruptedException e) {}
    		}
    		isTimeUp = true;
	        return jsonAsyncTask.title;
	}


	// Set Http title
	static String httpTitle;
	public static void setHttpTitle(String httpUrl, Activity act, final EditText editText) {
		if (!isEmptyString(httpUrl)) {
			try {
				WebView wv = new WebView(act);
				wv.loadUrl(httpUrl);
				isTimeUp = false;
				setupLongTimeout(1000);

				//Add for non-stop showing of full screen web view
				wv.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						view.loadUrl(url);
						return true;
					}
				});

				wv.setWebChromeClient(new WebChromeClient() {
					@Override
					public void onReceivedTitle(WebView view, String title) {
						super.onReceivedTitle(view, title);
						httpTitle = title;
						editText.setHint(Html.fromHtml("<small style=\"text-color: gray;\"><i>" +
								httpTitle +
								"</i></small>"));
						editText.setSelection(0);

						editText.setOnTouchListener(new View.OnTouchListener() {
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								((EditText) v).setText(httpTitle);
								((EditText) v).setSelection(httpTitle.length());
								return false;
							}
						});
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Set Http title
	public static void setHttpTitle(String httpUrl, Activity act, final TextView textView)
	{
		if(!isEmptyString(httpUrl))
		{
			try
			{
				WebView wv = new WebView(act);
				wv.loadUrl(httpUrl);
				isTimeUp = false;
				setupLongTimeout(1000);

				//Add for non-stop showing of full screen web view
				wv.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url)
					{
						view.loadUrl(url);
						return true;
					}
				});

				wv.setWebChromeClient(new WebChromeClient() {
					@Override
					public void onReceivedTitle(WebView view, String title) {
						super.onReceivedTitle(view, title);
						textView.setText(title);
						textView.setTextColor(Color.GRAY);
						System.out.println("Util / _setHttpTitle / title = " +title);
					}
				});
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}


	public static boolean isTimeUp;
	public static Timer longTimer;
	public synchronized static void setupLongTimeout(long timeout)
	{
	  if(longTimer != null) 
	  {
	    longTimer.cancel();
	    longTimer = null;
	  }
	  
	  if(longTimer == null) 
	  {
	    longTimer = new Timer();
	    longTimer.schedule(new TimerTask() 	{
											  public void run(){
												longTimer.cancel();
												longTimer = null;
												//do your stuff, i.e. finishing activity etc.
												isTimeUp = true;
											  }
											}, timeout /*in milliseconds*/);
					  }
	}

	// set full screen
	public static void setFullScreen(Activity act)
	{
//		System.out.println("Util / _setFullScreen");
		Window win = act.getWindow();
		
		if (Build.VERSION.SDK_INT < 16) 
		{ 
			win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} 
		else 
		{
			//ref: https://stackoverflow.com/questions/28983621/detect-soft-navigation-bar-availability-in-android-device-progmatically
			Resources res = act.getResources();
			int id = res.getIdentifier("config_showNavigationBar", "bool", "android");
			boolean hasNavBar = ( id > 0 && res.getBoolean(id));

//			System.out.println("Util / _setFullScreen / hasNavBar = " + hasNavBar);

            // flags
            int uiOptions = //View.SYSTEM_UI_FLAG_LAYOUT_STABLE | //??? why this flag will add bottom offset
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

            if (Build.VERSION.SDK_INT >= 19)
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            //has navigation bar
			if(hasNavBar)
			{
				uiOptions = uiOptions
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			}

            View decorView = act.getWindow().getDecorView();
			decorView.setSystemUiVisibility(uiOptions);
		}
	}
	
	// set NOT full screen
	public static void setNotFullScreen(Activity act)
	{
//		System.out.println("Util / _setNotFullScreen");
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
//			int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//top is overlaid by action bar
			int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;//normal
//			int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;//the usual system chrome is deemed too distracting.
//			int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;//full screen
            decorView.setSystemUiVisibility(uiOptions);
		}
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

	// Create assets file
	public static File createAssetsFile(Activity act, String fileName)
	{
		System.out.println("Util / _createAssetsFile / fileName = " + fileName);

        File file = null;
		AssetManager am = act.getAssets();
		InputStream inputStream = null;
		try {
			inputStream = am.open(fileName);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// main directory
		String dirString = Environment.getExternalStorageDirectory().toString() +
				"/" + Util.getStorageDirName(act);

		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();

		String filePath = dirString + "/" + fileName;

        if((inputStream != null)) {
            try {
                file = new File(filePath);
                OutputStream outputStream = new FileOutputStream(file);
                byte buffer[] = new byte[1024];
                int length = 0;

                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
	            //Logging exception
            	e.printStackTrace();
            }
        }
		return file;
	}

	// Get external storage path for different devices
	public static String getDefaultExternalStoragePath(String path)
	{
		if(path.contains("/storage/emulated/0"))
			path = path.replace("/storage/emulated/0", Environment.getExternalStorageDirectory().getAbsolutePath() );
		else if( path.contains("/mnt/internal_sd"))
			path = path.replace("/mnt/internal_sd", Environment.getExternalStorageDirectory().getAbsolutePath());
		return path;
	}


}
