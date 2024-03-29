package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;

public class PlayTimeFilter extends SliderFilter {
	private int mMinTime;
	private int mMaxTime;
	private boolean mUndefined;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinTime = min;
		mMaxTime = max;
		mUndefined = checkbox;
	}

	@Override
	protected boolean isChecked() {
		return mUndefined;
	}

	@Override
	protected int getMax() {
		return mMaxTime;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayTimeFilterData.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayTimeFilterData.MIN_RANGE;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new PlayTimeFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new PlayTimeFilterData(context, mMinTime, mMaxTime, mUndefined);
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_play_time;
	}

	@Override
	protected int getMin() {
		return mMinTime;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_time;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinTime = PlayTimeFilterData.MIN_RANGE;
			mMaxTime = PlayTimeFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			PlayTimeFilterData data = (PlayTimeFilterData) filter;
			mMinTime = data.getMin();
			mMaxTime = data.getMax();
			mUndefined = data.isUndefined();
		}
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayTimeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == PlayTimeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
