package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.RetrofitError;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlayPostResponse;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

public class SyncPlaysUpload extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlaysUpload.class);
	private Context mContext;
	private List<CharSequence> mMessages;
	private LocalBroadcastManager mBroadcaster;
	private BggService mPostService;

	public SyncPlaysUpload(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		mContext = context;
		mPostService = Adapter.createForPost(mContext);
		mMessages = new ArrayList<CharSequence>();
		mBroadcaster = LocalBroadcastManager.getInstance(mContext);

		updatePendingPlays(account.name, syncResult);
		deletePendingPlays(syncResult);
		SyncService.hIndex(context);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays_upload;
	}

	private void updatePendingPlays(String username, SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_SIMPLE_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) }, null);
			LOGI(TAG, String.format("Updating %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor, mContext, true);

				PlayPostResponse response = postPlayUpdate(play);
				if (!response.hasError()) {
					setStatusToSynced(play);
					String error = syncGame(username, play, syncResult);

					if (TextUtils.isEmpty(error)) {
						increaseGamePlayCount(play);
						if (!play.hasBeenSynced()) {
							deletePlay(play);
						}

						String message = play.hasBeenSynced() ? mContext.getString(R.string.msg_play_updated)
							: mContext.getString(R.string.msg_play_added,
								getPlayCountDescription(response.getPlayCount(), play.quantity));
						notifyUser(StringUtils.boldSecondString(message, play.gameName));
					} else {
						notifyError(error);
					}
				} else if (response.hasInvalidIdError()) {
					notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_update_bad_id),
						String.valueOf(play.playId)));
				} else if (response.hasAuthError()) {
					syncResult.stats.numAuthExceptions++;
					Authenticator.clearPassword(mContext);
					break;
				} else {
					syncResult.stats.numIoExceptions++;
					notifyError(response.getErrorMessage());
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void deletePendingPlays(SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_SIMPLE_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
			LOGI(TAG, String.format("Deleting %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor);
				if (play.hasBeenSynced()) {
					PlayPostResponse response = postPlayDelete(play.playId);
					if (!response.hasError()) {
						decreaseGamePlayCount(play);
						deletePlay(play);
						notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_deleted),
							play.gameName));
					} else if (response.hasInvalidIdError()) {
						deletePlay(play);
						notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_deleted),
							play.gameName));
					} else if (response.hasAuthError()) {
						syncResult.stats.numAuthExceptions++;
						Authenticator.clearPassword(mContext);
					} else {
						syncResult.stats.numIoExceptions++;
						notifyError(response.getErrorMessage());
					}
				} else {
					deletePlay(play);
					notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_deleted_draft),
						play.gameName));
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void increaseGamePlayCount(Play play) {
		updateGamePlayCount(play, true);
	}

	private void decreaseGamePlayCount(Play play) {
		updateGamePlayCount(play, false);
	}

	private void updateGamePlayCount(Play play, boolean add) {
		ContentResolver resolver = mContext.getContentResolver();
		Uri uri = Games.buildGameUri(play.gameId);
		Cursor cursor = resolver.query(uri, new String[] { Games.NUM_PLAYS }, null, null, null);
		if (cursor.moveToFirst()) {
			int newPlayCount = cursor.getInt(0);
			if (add) {
				newPlayCount += play.quantity;
			} else {
				newPlayCount -= play.quantity;
				if (newPlayCount < 0) {
					newPlayCount = 0;
				}
			}
			ContentValues values = new ContentValues();
			values.put(Collection.NUM_PLAYS, newPlayCount);
			resolver.update(uri, values, null, null);
		}
	}

	private PlayPostResponse postPlayUpdate(Play play) {
		Map<String, String> form = new HashMap<String, String>();
		form.put("ajax", "1");
		form.put("action", "save");
		form.put("version", "2");
		form.put("objecttype", "thing");
		if (play.hasBeenSynced()) {
			form.put("playid", String.valueOf(play.playId));
		}
		form.put("objectid", String.valueOf(play.gameId));
		form.put("playdate", play.getDate());
		form.put("dateinput", play.getDate()); // TODO: ask Aldie what this is
		form.put("length", String.valueOf(play.length));
		form.put("location", play.location);
		form.put("quantity", String.valueOf(play.quantity));
		form.put("incomplete", play.Incomplete() ? "1" : "0");
		form.put("nowinstats", play.NoWinStats() ? "1" : "0");
		form.put("comments", play.comments);
		List<Player> players = play.getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			form.put(getMapKey(i, "playerid"), "player_" + i);
			form.put(getMapKey(i, "name"), player.name);
			form.put(getMapKey(i, "username"), player.username);
			form.put(getMapKey(i, "color"), player.color);
			form.put(getMapKey(i, "position"), player.startposition);
			form.put(getMapKey(i, "score"), player.score);
			form.put(getMapKey(i, "rating"), String.valueOf(player.rating));
			form.put(getMapKey(i, "new"), String.valueOf(player.new_));
			form.put(getMapKey(i, "win"), String.valueOf(player.win));
		}

		try {
			PlayPostResponse response = mPostService.geekPlay(form);
			return response;
		} catch (Exception e) {
			return new PlayPostResponse(e);
		}
	}

	private static String getMapKey(int index, String key) {
		return "players[" + index + "][" + key + "]";
	}

	private PlayPostResponse postPlayDelete(int playId) {
		Map<String, String> form = new HashMap<String, String>();
		form.put("ajax", "1");
		form.put("action", "delete");
		form.put("playid", String.valueOf(playId));

		try {
			PlayPostResponse response = mPostService.geekPlay(form);
			return response;
		} catch (Exception e) {
			return new PlayPostResponse(e);
		}
	}

	/**
	 * Marks the specified play as synced in the content provider
	 */
	private void setStatusToSynced(Play play) {
		play.syncStatus = Play.SYNC_STATUS_SYNCED;
		PlayPersister.save(mContext, play);
		// syncResult.stats.numUpdates++;
	}

	/**
	 * Deletes the specified play from the content provider
	 */
	private void deletePlay(Play play) {
		PlayPersister.delete(mContext.getContentResolver(), play);
	}

	/**
	 * Syncs the specified game from the 'Geek to the local DB.
	 * 
	 * @return An error message, or blank if no error.
	 */
	private String syncGame(String username, Play play, SyncResult syncResult) {
		try {
			long startTime = System.currentTimeMillis();
			PlaysResponse response = mService.plays(username, play.gameId, play.getDate(), play.getDate());
			if (!play.hasBeenSynced()) {
				int newPlayId = getTranslatedPlayId(play, response.plays);
				PreferencesUtils.putNewPlayId(mContext, play.playId, newPlayId);
				Intent intent = new Intent(SyncService.ACTION_PLAY_ID_CHANGED);
				mBroadcaster.sendBroadcast(intent);
			}
			PlayPersister.save(mContext, response.plays, startTime);
		} catch (Exception e) {
			if (e instanceof RetrofitError) {
				syncResult.stats.numIoExceptions++;
			} else {
				syncResult.stats.numParseExceptions++;
			}
			return e.toString();
		}
		return "";
	}

	private int getTranslatedPlayId(Play play, List<Play> parsedPlays) {
		if (parsedPlays == null || parsedPlays.size() == 0) {
			return BggContract.INVALID_ID;
		}

		int latestPlayId = BggContract.INVALID_ID;

		for (Play parsedPlay : parsedPlays) {
			if ((play.playId != parsedPlay.playId)
				&& (play.gameId == parsedPlay.gameId)
				&& (play.getDate().equals(parsedPlay.getDate()))
				&& ((play.location == null && parsedPlay.location == null) || (play.location
					.equals(parsedPlay.location))) && (play.length == parsedPlay.length)
				&& (play.quantity == parsedPlay.quantity) && (play.Incomplete() == parsedPlay.Incomplete())
				&& (play.NoWinStats() == parsedPlay.NoWinStats())
				&& (play.getPlayerCount() == parsedPlay.getPlayerCount())) {
				if (parsedPlay.playId > latestPlayId) {
					latestPlayId = parsedPlay.playId;
				}
			}
		}

		return latestPlayId;
	}

	private String getPlayCountDescription(int count, int quantity) {
		String countDescription = "";
		switch (quantity) {
			case 1:
				countDescription = StringUtils.getOrdinal(count);
				break;
			case 2:
				countDescription = StringUtils.getOrdinal(count - 1) + " & " + StringUtils.getOrdinal(count);
				break;
			default:
				countDescription = StringUtils.getOrdinal(count - quantity + 1) + " - " + StringUtils.getOrdinal(count);
				break;
		}
		return countDescription;
	}

	private void notifyUser(CharSequence message) {
		mMessages.add(message);

		NotificationCompat.Builder builder = createNotificationBuilder().setContentText(message);

		if (mMessages.size() == 1) {
			NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
			detail.bigText(message);
		} else {
			NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
			detail.setSummaryText(String.format(mContext.getString(R.string.sync_notification_upload_summary),
				mMessages.size()));
			for (int i = mMessages.size() - 1; i >= 0; i--) {
				detail.addLine(mMessages.get(i));
			}
		}
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_PLAY_UPLOAD, builder);
	}

	private void notifyError(String error) {
		NotificationCompat.Builder builder = createNotificationBuilder().setContentText(error);
		NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
		detail.bigText(error);
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_PLAY_UPLOAD_ERROR, builder);
	}

	private NotificationCompat.Builder createNotificationBuilder() {
		return NotificationUtils.createNotificationBuilder(mContext, R.string.sync_notification_title_play_upload,
			PlaysActivity.class);
	}
}
