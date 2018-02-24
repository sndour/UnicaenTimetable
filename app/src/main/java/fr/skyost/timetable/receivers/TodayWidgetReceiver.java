package fr.skyost.timetable.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.skyost.timetable.R;
import fr.skyost.timetable.Timetable;
import fr.skyost.timetable.activities.MainActivity;
import fr.skyost.timetable.services.TodayWidgetService;
import fr.skyost.timetable.utils.Utils;

public class TodayWidgetReceiver extends AppWidgetProvider {

	public static final int CURRENT_DAY_REQUEST = 100;
	public static final int REFRESH_REQUEST = 200;
	public static final int SCHEDULE_REQUEST = 300;
	public static final int BACK_REQUEST = 400;
	public static final int NEXT_REQUEST = 500;

	public static final String INTENT_REFRESH_WIDGETS = "refresh-widgets";
	public static final String INTENT_RELATIVE_DAY = "relative-day";

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		if(intent.hasExtra(INTENT_REFRESH_WIDGETS)) {
			final AppWidgetManager manager = AppWidgetManager.getInstance(context);
			this.onUpdate(
					context,
					manager,
					intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) ? new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)} : manager.getAppWidgetIds(new ComponentName(context, this.getClass())),
					intent.getIntExtra(INTENT_RELATIVE_DAY, 0)
			);
		}

		super.onReceive(context, intent);
	}

	@Override
	public final void onUpdate(final Context context, final AppWidgetManager manager, final int[] ids) {
		onUpdate(context, manager, ids, 0);
	}

	public final void onUpdate(final Context context, final AppWidgetManager manager, final int[] ids, final int relativeDay) {
		final WidgetDateManager dateManager = WidgetDateManager.getInstance();
		dateManager.setRelativeDay(relativeDay);

		Timetable timetable = null;
		boolean nextAvailable = false;
		try {
			timetable = Timetable.loadFromDisk(context);

			if(timetable != null) {
				final List<DateTime> availableWeeks = timetable.getAvailableWeeks();
				if(!availableWeeks.isEmpty()) {
					nextAvailable = !availableWeeks.get(availableWeeks.size() - 1).withDayOfWeek(DateTimeConstants.THURSDAY).isBefore(new DateTime(dateManager.getAbsoluteDay()));
				}
			}
		}
		catch(final Exception ex) {
			ex.printStackTrace();
		}


		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today_layout);

		updateDrawables(context, views, dateManager, nextAvailable);
		updateTitle(context, views, dateManager);
		updateMessage(context, views);
		registerIntents(context, views, dateManager, nextAvailable);

		for(final int id : ids) {
			manager.notifyAppWidgetViewDataChanged(id, R.id.widget_today_content);
			manager.updateAppWidget(id, views);
		}

		scheduleNextUpdate(context, timetable);
		super.onUpdate(context, manager, ids);
	}

	/**
	 * Updates the drawables.
	 *
	 * @param context The context.
	 * @param views Widgets' RemoteViews.
	 * @param dateManager The date manager.
	 * @param nextAvailable If next button should be available.
	 */

	public final void updateDrawables(final Context context, final RemoteViews views, final WidgetDateManager dateManager, final boolean nextAvailable) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			views.setImageViewResource(R.id.widget_today_refresh, R.drawable.widget_today_refresh_drawable);
			views.setImageViewResource(R.id.widget_today_back, R.drawable.widget_today_back_drawable);
			views.setImageViewResource(R.id.widget_today_next, R.drawable.widget_today_next_drawable);
		}
		else {
			views.setImageViewBitmap(R.id.widget_today_refresh, Utils.drawableToBitmap(context, R.drawable.widget_today_refresh_drawable));
			views.setImageViewBitmap(R.id.widget_today_back, Utils.drawableToBitmap(context, R.drawable.widget_today_back_drawable));
			views.setImageViewBitmap(R.id.widget_today_next, Utils.drawableToBitmap(context, R.drawable.widget_today_next_drawable));
		}

		if(dateManager.getRelativeDay() <= 0) {
			views.setInt(R.id.widget_today_back, "setColorFilter", ContextCompat.getColor(context, R.color.color_widget_today_white_disabled));
		}
		else {
			views.setInt(R.id.widget_today_back, "setColorFilter", ContextCompat.getColor(context, R.color.color_widget_today_white));
		}

		if(nextAvailable) {
			views.setInt(R.id.widget_today_next, "setColorFilter", ContextCompat.getColor(context, R.color.color_widget_today_white));
		}
		else {
			views.setInt(R.id.widget_today_next, "setColorFilter", ContextCompat.getColor(context, R.color.color_widget_today_white_disabled));
		}
	}

	/**
	 * Update widgets' title.
	 *
	 * @param context The context.
	 * @param views Widgets' RemoteViews.
	 * @param dateManager The date manager.
	 */

	public final void updateTitle(final Context context, final RemoteViews views, final WidgetDateManager dateManager) {
		if(dateManager.getRelativeDay() == 0) {
			views.setTextViewText(R.id.widget_today_title, context.getString(R.string.widget_today_title));
			return;
		}

		final Date date = dateManager.getAbsoluteDay().getTime();
		views.setTextViewText(R.id.widget_today_title, new SimpleDateFormat("E", Locale.getDefault()).format(date).toUpperCase() + " " + DateFormat.getDateFormat(context).format(date));
	}

	/**
	 * Update widgets' message.
	 *
	 * @param context The context.
	 * @param views Widgets' RemoteViews.
	 */

	public final void updateMessage(final Context context, final RemoteViews views) {
		final Intent intent = new Intent(context, TodayWidgetService.class);
		views.setRemoteAdapter(R.id.widget_today_content, intent);
	}

	/**
	 * Attaches MainActivity intents to this widget_today_layout.
	 *
	 * @param context A context.
	 * @param views Widgets' RemoteViews.
	 * @param dateManager The date manager.
	 * @param nextAvailable If next button should be available.
	 */

	public final void registerIntents(final Context context, final RemoteViews views, final WidgetDateManager dateManager, final boolean nextAvailable) {
		final Calendar now = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(dateManager.getRelativeDay()));

		int day = now.get(Calendar.DAY_OF_WEEK);
		if(day == Calendar.SATURDAY) {
			now.setTimeInMillis(now.getTimeInMillis() + TimeUnit.DAYS.toMillis(2));
		}
		else if(day == Calendar.SUNDAY) {
			now.setTimeInMillis(now.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
		}

		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);

		final Intent currentFragment = new Intent(context, MainActivity.class);
		currentFragment.putExtra(MainActivity.INTENT_DATE, now.getTimeInMillis());
		views.setOnClickPendingIntent(R.id.widget_today_title, PendingIntent.getActivity(context, CURRENT_DAY_REQUEST, currentFragment, PendingIntent.FLAG_UPDATE_CURRENT));

		final Intent refresh = (Intent)currentFragment.clone();
		refresh.putExtra(MainActivity.INTENT_REFRESH_TIMETABLE, true);
		views.setOnClickPendingIntent(R.id.widget_today_refresh, PendingIntent.getActivity(context, REFRESH_REQUEST, refresh, PendingIntent.FLAG_UPDATE_CURRENT));

		final Intent next = new Intent(context, this.getClass());
		next.putExtra(INTENT_REFRESH_WIDGETS, true);

		if(nextAvailable) {
			next.putExtra(INTENT_RELATIVE_DAY, dateManager.getRelativeDay() + 1);
			views.setOnClickPendingIntent(R.id.widget_today_next, PendingIntent.getBroadcast(context, BACK_REQUEST, next, PendingIntent.FLAG_UPDATE_CURRENT));
		}
		else {
			views.setOnClickPendingIntent(R.id.widget_today_next, null);
		}

		if(dateManager.getRelativeDay() > 0) {
			final Intent back = (Intent)next.clone();
			back.putExtra(INTENT_RELATIVE_DAY, dateManager.getRelativeDay() - 1);
			views.setOnClickPendingIntent(R.id.widget_today_back, PendingIntent.getBroadcast(context, NEXT_REQUEST, back, PendingIntent.FLAG_UPDATE_CURRENT));
		}
		else {
			views.setOnClickPendingIntent(R.id.widget_today_back, null);
		}
	}

	/**
	 * Schedules widgets next update.
	 *
	 * @param context A context.
	 * @param timetable The timetable.
	 */

	public final void scheduleNextUpdate(final Context context, final Timetable timetable) {
		final AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		if(manager == null) {
			return;
		}

		final Timetable.Lesson nextLesson = timetable == null ? null : timetable.getNextLesson();

		final Intent intent = new Intent(context, this.getClass());
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		intent.putExtra(INTENT_REFRESH_WIDGETS, true);
		final PendingIntent pending = PendingIntent.getBroadcast(context, SCHEDULE_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		final Calendar calendar = nextLesson == null ? Utils.tomorrowMidnight() : nextLesson.getEnd();
		if(calendar.get(Calendar.SECOND) == 0) {
			calendar.add(Calendar.SECOND, 1);
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			manager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending);
		}
		else {
			manager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending);
		}
	}

	public static final class WidgetDateManager {

		private int relativeDay = 0;

		private static class Holder {

			private static final WidgetDateManager INSTANCE = new WidgetDateManager();

		}

		public static WidgetDateManager getInstance() {
			return Holder.INSTANCE;
		}

		public final int getRelativeDay() {
			return relativeDay;
		}

		public final void plusRelativeDay() {
			this.relativeDay++;
		}

		public final void minusRelativeDay() {
			this.relativeDay--;
		}

		public final void setRelativeDay(final int relativeDay) {
			this.relativeDay = relativeDay;
		}

		public final Calendar getAbsoluteDay() {
			final Calendar day = Calendar.getInstance();
			day.setTimeInMillis(day.getTimeInMillis() + TimeUnit.DAYS.toMillis(this.relativeDay));

			day.set(Calendar.HOUR_OF_DAY, 0);
			day.set(Calendar.MINUTE, 0);
			day.set(Calendar.SECOND, 0);
			day.set(Calendar.MILLISECOND, 0);

			return day;
		}

	}

}