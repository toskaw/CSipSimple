/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Colors;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.models.Filter;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.telephony.PhoneStateListener.*;

public class CallLogHelper {

	private static final String ACTION_ANNOUNCE_SIP_CALLLOG = "de.ub0r.android.callmeter.SAVE_SIPCALL";
	// Uri of call log entry
	private static final String EXTRA_CALL_LOG_URI = "uri";
	// Provider name
	public static final String EXTRA_SIP_PROVIDER = "provider";
    private static final String THIS_FILE = "CallLogHelper";
    private static final String GOOGLE_CONTACTS_SEARCH_URI ="https://contacts.google.com/search/";
    // プロジェクション配列。
	// 取得したいプロパティの一覧を指定する。
	private static final String[] CALENDAR_PROJECTION = new String[] {
			Calendars._ID,
			Calendars.NAME,
			Calendars.ACCOUNT_NAME,
			Calendars.ACCOUNT_TYPE,
	};

	// プロジェクション配列
	private static final String[] COLOR_PROJECTION = new String[] {
			Colors._ID,
			Colors.COLOR,
			Colors.COLOR_KEY,
			Colors.COLOR_TYPE,
			Colors.ACCOUNT_NAME,
			Colors.ACCOUNT_TYPE,
	};
	// プロジェクション配列のインデックス。
	// パフォーマンス向上のために、動的に取得せずに、静的に定義しておく。
	private static final int CALENDAR_PROJECTION_IDX_ID = 0;
	private static final int CALENDAR_PROJECTION_IDX_NAME = 1;
	private static final int CALENDAR_PROJECTION_IDX_ACCOUNT_NAME = 2;
	private static final int CALENDAR_PROJECTION_IDX_ACCOUNT_TYPE = 3;

	public static void addCallLog(Context context, ContentValues values, ContentValues extraValues) {
		ContentResolver contentResolver = context.getContentResolver();
		Uri result = null;
		try {
		    result = contentResolver.insert(CallLog.Calls.CONTENT_URI, values);
		}catch(IllegalArgumentException e) {
		    Log.w(THIS_FILE, "Cannot insert call log entry. Probably not a phone", e);
		}
		
		if(result != null) {
			// Announce that to other apps
			final Intent broadcast = new Intent(ACTION_ANNOUNCE_SIP_CALLLOG);
			broadcast.putExtra(EXTRA_CALL_LOG_URI, result.toString());
			String provider = extraValues.getAsString(EXTRA_SIP_PROVIDER);
			if(provider != null) {
				broadcast.putExtra(EXTRA_SIP_PROVIDER, provider);
			}
			context.sendBroadcast(broadcast);
			//Log.setLogLevel(4);
			// カレンダーに追加
			String query = "(" + Calendars.NAME + "= ?)";
			String[] args = new String[]{"通話履歴"};
			Cursor cur = contentResolver.query(Calendars.CONTENT_URI, CALENDAR_PROJECTION, query, args, null);
			cur.moveToFirst();
			String id = cur.getString(CALENDAR_PROJECTION_IDX_ID);
			String name = cur.getString(CALENDAR_PROJECTION_IDX_NAME);
			String accountName = cur.getString(CALENDAR_PROJECTION_IDX_ACCOUNT_NAME);
			String accountType = cur.getString(CALENDAR_PROJECTION_IDX_ACCOUNT_TYPE);
			ContentValues event = new ContentValues();
			String title = values.getAsString(CallLog.Calls.CACHED_NAME);
			long start = values.getAsLong(CallLog.Calls.DATE);
			long end = start + values.getAsLong(CallLog.Calls.DURATION);
			int type = values.getAsInteger(CallLog.Calls.TYPE);
			String number = values.getAsString(CallLog.Calls.NUMBER);
			event.put(CalendarContract.Events.CALENDAR_ID, id);
			event.put(CalendarContract.Events.TITLE, title);
			event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
			event.put(CalendarContract.Events.DTSTART, start);
			event.put(Events.EVENT_COLOR_KEY, String.valueOf(type));
			event.put(Events.DESCRIPTION,  "<a href=\"" +GOOGLE_CONTACTS_SEARCH_URI + number
				+ "\">" + number + "</a>");
			event.put(Events.DTEND, end);
			Log.d(THIS_FILE, "title = " + title);
			Log.d(THIS_FILE, "start = " + start);
			Log.d(THIS_FILE, "end = " + end);
			Log.d(THIS_FILE, "type = " + type);
			Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, event);
			Log.d(THIS_FILE, uri.toString());
		}
	}
	
	
	public static ContentValues logValuesForCall(Context context, SipCallSession call, long callStart) {
		ContentValues cv = new ContentValues();
		String remoteContact = call.getRemoteContact();
		
		cv.put(CallLog.Calls.NUMBER, remoteContact);
		
		Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*@[^>]*)(?:>)?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(remoteContact);
        String number = remoteContact;
        if (m.matches()) {
            number = m.group(2);
        }
        
        cv.put(CallLog.Calls.DATE, (callStart > 0) ? callStart : System.currentTimeMillis());
		int type = CallLog.Calls.OUTGOING_TYPE;
		int nonAcknowledge = 0; 
		if(call.isIncoming()) {
			type = CallLog.Calls.MISSED_TYPE;
			nonAcknowledge = 1;
			if(callStart > 0) {
				// Has started on the remote side, so not missed call
				type = CallLog.Calls.INCOMING_TYPE;
				nonAcknowledge = 0;
			}else if(call.getLastStatusCode() == SipCallSession.StatusCode.DECLINE ||
			        call.getLastStatusCode() == SipCallSession.StatusCode.BUSY_HERE ||
			        call.getLastReasonCode() == 200) {
				// We have intentionally declined this call or replied elsewhere
				type = CallLog.Calls.INCOMING_TYPE;
				nonAcknowledge = 0;
			}
		}


        int hasBeenAutoanswered = Filter.isAutoAnswerNumber(context, call.getAccId(), number, null);
        if(hasBeenAutoanswered == call.getLastStatusCode()) {
            nonAcknowledge = 0;
        }
        cv.put(CallLog.Calls.TYPE, type);
        cv.put(CallLog.Calls.NEW, nonAcknowledge);
        cv.put(CallLog.Calls.DURATION,
                (callStart > 0) ? (System.currentTimeMillis() - callStart) / 1000 : 0);
        cv.put(SipManager.CALLLOG_PROFILE_ID_FIELD, call.getAccId());
        cv.put(SipManager.CALLLOG_STATUS_CODE_FIELD, call.getLastStatusCode());
        cv.put(SipManager.CALLLOG_STATUS_TEXT_FIELD, call.getLastStatusComment());

		CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(context, remoteContact);
		if(callerInfo != null) {
			cv.put(CallLog.Calls.CACHED_NAME, callerInfo.name);
			cv.put(CallLog.Calls.CACHED_NUMBER_LABEL, callerInfo.numberLabel);
			cv.put(CallLog.Calls.CACHED_NUMBER_TYPE, callerInfo.numberType);
		}
		
		return cv;
	}
}
