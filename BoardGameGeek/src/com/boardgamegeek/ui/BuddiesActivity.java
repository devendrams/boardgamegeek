package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.UIUtils;

public class BuddiesActivity extends TopLevelActivity implements ActionBar.TabListener, ViewPager.OnPageChangeListener,
	BuddiesFragment.Callbacks, PlayersFragment.Callbacks {
	private static final String KEY_COUNT = "KEY_COUNT";
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private int mCount = -1;
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mCount = savedInstanceState.getInt(KEY_COUNT);
		}

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new BuddiesPagerAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setupActionBarTabs(actionBar);
	}

	private void setupActionBarTabs(final ActionBar actionBar) {
		actionBar.removeAllTabs();
		createTab(actionBar, R.string.title_buddies);
		createTab(actionBar, R.string.title_players);
	}

	private void createTab(final ActionBar actionBar, int textId) {
		Tab tab = actionBar.newTab().setText(textId).setTabListener(this);
		actionBar.addTab(tab);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COUNT, mCount);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_viewpager;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ActivityUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount));
		menu.findItem(R.id.menu_refresh).setVisible(!isDrawerOpen());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				SyncService.sync(this, SyncService.FLAG_SYNC_BUDDIES);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddies;
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_buddies;
	}

	@Override
	public boolean onBuddySelected(int buddyId, String name, String fullName) {
		Intent intent = new Intent(this, BuddyActivity.class);
		intent.putExtra(BuddyUtils.KEY_BUDDY_NAME, name);
		startActivity(intent);
		return false;
	}

	@Override
	public boolean onPlayerSelected(String name, String username) {
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.KEY_PLAYER_NAME, name);
		intent.putExtra(PlayerActivity.KEY_PLAYER_USERNAME, username);
		startActivity(intent);
		return true;
	}

	@Override
	public void onBuddyCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	private void setRefreshActionButtonState(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
			} else {
				refreshItem.setActionView(null);
			}
		}
	}

	private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		@Override
		public void onStatusChanged(int which) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setRefreshActionButtonState(SyncService.isActiveOrPending(BuddiesActivity.this));
				}
			});
		}
	};

	private class BuddiesPagerAdapter extends FragmentPagerAdapter {
		public BuddiesPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {
				case 0:
					fragment = new BuddiesFragment();
					break;
				case 1:
					fragment = new PlayersFragment();
					break;
			}
			if (fragment != null) {
				fragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		getSupportActionBar().setSelectedNavigationItem(position);
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}
}
