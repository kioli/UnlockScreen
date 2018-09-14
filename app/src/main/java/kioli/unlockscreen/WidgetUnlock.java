package kioli.unlockscreen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Calendar;

public class WidgetUnlock extends AppWidgetProvider {
	
	private static final String SP_VALUE = "spValue";
	private static final String SP_NAME = "spWidget";
	private static final String ACTION_NEW_DAY = "newDay";
	private static final long INTERVAL = 24 * 60 * 60 * 1000L;
	
	private IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_PRESENT);
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
				int value = updateUnlocks(context);
				showText(context, value);
			} else if (ACTION_NEW_DAY.equals(intent.getAction())) {
				int value = resetNumLocks(context);
				showText(context, value);
			}
		}
	};
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
				setInitial(context);
			}
			context.getApplicationContext().registerReceiver(receiver, intentFilter);
		}
	}
	
	private void setInitial(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(SP_NAME, 0);
		final int numUnlocks = pref.getInt(SP_VALUE, 0);
		showText(context, numUnlocks);
		
		final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final Intent i = new Intent(context, WidgetUnlock.class);
		i.setAction(ACTION_NEW_DAY);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		
		final long firstTime = SystemClock.elapsedRealtime() + millisecondsToNextMidnight();
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, INTERVAL, pi);
	}
	
	private long millisecondsToNextMidnight() {
		final Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return (c.getTimeInMillis() - System.currentTimeMillis());
	}
	
	private void showText(Context context, int numUnlocks) {
		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock);
		final String songsFound = context.getResources().getQuantityString(R.plurals.numberUnlocks, numUnlocks, numUnlocks);
		views.setTextViewText(R.id.appwidget_text, songsFound);
		
		final ComponentName thisWidget = new ComponentName(context, WidgetUnlock.class);
		AppWidgetManager.getInstance(context).updateAppWidget(thisWidget, views);
	}
	
	private int updateUnlocks(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(SP_NAME, 0);
		int numUnlocks = pref.getInt(SP_VALUE, 0);
		numUnlocks++;
		pref.edit().putInt(SP_VALUE, numUnlocks).apply();
		return numUnlocks;
	}
	
	private int resetNumLocks(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(SP_NAME, 0);
		pref.edit().putInt(SP_VALUE, 0).apply();
		return 0;
	}
}