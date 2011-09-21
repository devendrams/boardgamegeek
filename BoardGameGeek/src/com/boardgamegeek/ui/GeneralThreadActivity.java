package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class GeneralThreadActivity extends ListActivity {
	private final String TAG = "ThreadActivity";

	public static final String KEY_THREAD_ID = "THREAD_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	public static final String KEY_ARTICLES = "ARTICLES";

	private List<ThreadArticle> mArticles = new ArrayList<ThreadArticle>();

	private String mThreadId;
	private String mThreadSubject;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_thread);
		UIUtils.setTitle(this);
		findViewById(R.id.thread_game_header).setVisibility(View.GONE);
		findViewById(R.id.thread_header_divider).setVisibility(View.GONE);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mThreadId = intent.getExtras().getString(KEY_THREAD_ID);
			mThreadSubject = intent.getExtras().getString(KEY_THREAD_SUBJECT);
		} else {
			mThreadId = savedInstanceState.getString(KEY_THREAD_ID);
			mThreadSubject = savedInstanceState.getString(KEY_THREAD_SUBJECT);
			mArticles = savedInstanceState.getParcelableArrayList(KEY_ARTICLES);
		}
		((TextView) findViewById(R.id.thread_subject)).setText(mThreadSubject);

		if (mArticles == null || mArticles.size() == 0) {
			ForumsUtils.ThreadTask task =
				new ForumsUtils.ThreadTask(this, mArticles, HttpUtils.constructThreadUrl(mThreadId), mThreadSubject, TAG);
			task.execute();
		} else {
			setListAdapter(new ForumsUtils.ThreadAdapter(this, mArticles));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_THREAD_ID, mThreadId);
		outState.putString(KEY_THREAD_SUBJECT, mThreadSubject);
		outState.putParcelableArrayList(KEY_ARTICLES, (ArrayList<? extends Parcelable>) mArticles);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}
}
