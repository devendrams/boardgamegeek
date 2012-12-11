package com.boardgamegeek;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

import com.boardgamegeek.pref.ListPreferenceMultiSelect;

public class BggApplication extends Application {
	private static final String TAG = makeLogTag(BggApplication.class);

	public final static String siteUrl = "http://www.boardgamegeek.com/";
	public static final String HELP_COLLECTION_KEY = "help.collection";
	public static final String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static final String HELP_LOGPLAY_KEY = "help.logplay";
	public static final String HELP_COLORS_KEY = "help.colors";

	public static final String AUTHTOKEN_TYPE = "com.boardgamegeek";
	public static final String ACCOUNT_TYPE = "com.boardgamegeek";

	private static final String SHARED_PREFERENCES_NAME = "com.boardgamegeek";
	private static final String SYNC_TICKS_KEY = "sync_ticks";
	private static final String KEY_SYNC_COLLECTION_FULL_TICKS = "collection_full_sync_ticks";
	private static final String KEY_SYNC_COLLECTION_PART_TICKS = "collection_part_sync_ticks";
	private static final String KEY_LAST_COLLECTION_SYNC = "LAST_COLLECTION_SYNC";
	private static final String KEY_LAST_PLAYS_SYNC = "LAST_PLAYS_SYNC";
	private static final String KEY_LAST_BUDDIES_SYNC = "LAST_BUDDIES_SYNC";

	private static final String MAX_PLAY_DATE_KEY = "max_play_date";
	private static final String MIN_PLAY_DATE_KEY = "min_play_date";
	private static final String DEFAULT_MAX_PLAY_DATE = "9999-99-99";
	private static final String DEFAULT_MIN_PLAY_DATE = "0000-00-00";

	private static BggApplication singleton;

	public static BggApplication getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}

	public static String getVersionDescription(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return "Version " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			LOGE(TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}

	public void clearSyncTimestamps() {
		putSyncTimestamp(0);
		putCollectionFullSyncTimestamp(0);
		putCollectionPartSyncTimestamp(0);
	}

	public void clearSyncPlaysSettings() {
		putMaxPlayDate(DEFAULT_MAX_PLAY_DATE);
		putMinPlayDate(DEFAULT_MIN_PLAY_DATE);
	}

	public void putSyncTimestamp(long startTime) {
		putTimestamp(startTime, SYNC_TICKS_KEY);
	}

	public void putCollectionFullSyncTimestamp(long startTime) {
		putTimestamp(startTime, KEY_SYNC_COLLECTION_FULL_TICKS);
	}

	public void putCollectionPartSyncTimestamp(long startTime) {
		putTimestamp(startTime, KEY_SYNC_COLLECTION_PART_TICKS);
	}

	private void putTimestamp(long startTime, String key) {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		Editor e = sp.edit();
		e.putLong(key, startTime);
		if (!e.commit()) {
			LOGW(TAG, "Error saving time the collection last synced.");
		}
	}

	public long getSyncTimestamp() {
		return getTimestamp(SYNC_TICKS_KEY);
	}

	public long getCollectionFullSyncTimestamp() {
		return getTimestamp(KEY_SYNC_COLLECTION_FULL_TICKS);
	}

	public long getCollectionPartSyncTimestamp() {
		return getTimestamp(KEY_SYNC_COLLECTION_PART_TICKS);
	}

	private long getTimestamp(String key) {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		return sp.getLong(key, 0);
	}

	public String getMaxPlayDate() {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		return sp.getString(MAX_PLAY_DATE_KEY, DEFAULT_MAX_PLAY_DATE);
	}

	public void putMaxPlayDate(String maxPlayDate) {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		Editor e = sp.edit();
		e.putString(MAX_PLAY_DATE_KEY, maxPlayDate);
		if (!e.commit()) {
			LOGW(TAG, "Error saving max play date.");
		}
	}

	public String getMinPlayDate() {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		return sp.getString(MIN_PLAY_DATE_KEY, DEFAULT_MIN_PLAY_DATE);
	}

	public void putMinPlayDate(String minPlayDate) {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		Editor e = sp.edit();
		e.putString(MIN_PLAY_DATE_KEY, minPlayDate);
		if (!e.commit()) {
			LOGW(TAG, "Error saving min play date.");
		}
	}

	public long getLastCollectionSync() {
		return getTimestamp(KEY_LAST_COLLECTION_SYNC);
	}

	public void putLastCollectionSync() {
		putTimestamp(KEY_LAST_COLLECTION_SYNC);
	}

	public long getLastPlaysSync() {
		return getTimestamp(KEY_LAST_PLAYS_SYNC);
	}

	public void putLastPlaysSync() {
		putTimestamp(KEY_LAST_PLAYS_SYNC);
	}

	public long getLastBuddiesSync() {
		return getTimestamp(KEY_LAST_BUDDIES_SYNC);
	}

	public void putLastBuddiesSync() {
		putTimestamp(KEY_LAST_BUDDIES_SYNC);
	}

	private void putTimestamp(String key) {
		SharedPreferences sp = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
			Context.MODE_PRIVATE);
		Editor e = sp.edit();
		e.putLong(key, System.currentTimeMillis());
		if (!e.commit()) {
			LOGW(TAG, "Error saving current time.");
		}
	}

	public boolean getImageLoad() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("imageLoad", true);
	}

	public boolean getExactSearch() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("exactSearch", true);
	}

	public boolean getSkipResults() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("skipResults", true);
	}

	public String[] getSyncStatuses() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String statuses = preferences.getString("syncStatuses", "");
		return ListPreferenceMultiSelect.parseStoredValue(statuses);
	}

	public boolean getSyncPlays() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("syncPlays", false);
	}

	public boolean getSyncBuddies() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("syncBuddies", false);
	}

	public boolean getPlayLoggingHideMenu() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLog", false);
	}

	public boolean getPlayLoggingHideQuickMenu() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideQuickLog", false);
	}

	public boolean getPlayLoggingHideLength() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLength", false);
	}

	public boolean getPlayLoggingHideLocation() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLocation", false);
	}

	public boolean getPlayLoggingHideIncomplete() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideIncomplete", false);
	}

	public boolean getPlayLoggingHideNoWinStats() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideNoWinStats", false);
	}

	public boolean getPlayLoggingHideComments() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideComments", false);
	}

	public boolean getPlayLoggingHidePlayerList() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHidePlayerList", false);
	}

	public boolean getPlayLoggingEditPlayer() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logEditPlayer", false);
	}

	public boolean getPlayLoggingHidePlayerTeamColor() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideTeamColor", false);
	}

	public boolean getPlayLoggingHidePlayerPosition() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHidePosition", false);
	}

	public boolean getPlayLoggingHidePlayerScore() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideScore", false);
	}

	public boolean getPlayLoggingHidePlayerRating() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideRating", false);
	}

	public boolean getPlayLoggingHidePlayerNew() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideNew", false);
	}

	public boolean getPlayLoggingHidePlayerWin() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideWin", false);
	}

	public boolean showHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	public boolean updateHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.edit().putInt(key, version).commit();
	}
}
