package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends TopLevelActivity {
	private static final int HELP_VERSION = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Authenticator.isSignedIn(this)) {
			if (Authenticator.isOldAuth(this)) {
				signOut();
			} else if (startUserActivity()) {
				return;
			}
		}

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		UIUtils.showHelpDialog(this, HelpUtils.HELP_HOME_KEY, HELP_VERSION, R.string.help_home);
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_home;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	protected void onSignInSuccess() {
		super.onSignInSuccess();
		startUserActivity();
	}

	private boolean startUserActivity() {
		Intent intent = null;
		String[] statuses = PreferencesUtils.getSyncStatuses(this);
		if (statuses != null && statuses.length > 0) {
			intent = new Intent(this, CollectionActivity.class);
		} else if (PreferencesUtils.getSyncPlays(this)) {
			intent = new Intent(this, PlaysActivity.class);
		} else if (PreferencesUtils.getSyncBuddies(this)) {
			intent = new Intent(this, BuddiesActivity.class);
		}
		if (intent != null) {
			startActivity(intent);
			finish();
			return true;
		}
		return false;
	}
}