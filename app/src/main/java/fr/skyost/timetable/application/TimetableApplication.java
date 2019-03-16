package fr.skyost.timetable.application;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.room.Room;
import fr.skyost.timetable.lesson.database.AppDatabase;

/**
 * The Application class.
 */

public class TimetableApplication extends Application {

	/**
	 * The database file.
	 */

	public static final String DATABASE = "app_data.db";

	/**
	 * The application database.
	 */

	private AppDatabase database;

	@Override
	public void onCreate() {
		super.onCreate();

		// We create our database.
		database = Room.databaseBuilder(this, AppDatabase.class, DATABASE)
				//.allowMainThreadQueries()
				.build();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	/**
	 * Returns the database.
	 *
	 * @return The database.
	 */

	public AppDatabase getDatabase() {
		return database;
	}

}