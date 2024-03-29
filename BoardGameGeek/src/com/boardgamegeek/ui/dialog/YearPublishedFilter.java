package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.YearPublishedFilterData;

public class YearPublishedFilter extends SliderFilter {
	private int mMinYear;
	private int mMaxYear;

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinYear = YearPublishedFilterData.MIN_RANGE;
			mMaxYear = YearPublishedFilterData.MAX_RANGE;
		} else {
			YearPublishedFilterData data = (YearPublishedFilterData) filter;
			mMinYear = data.getMin();
			mMaxYear = data.getMax();
		}
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_year_published;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new YearPublishedFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new YearPublishedFilterData(context, mMinYear, mMaxYear);
	}

	@Override
	protected int getMin() {
		return mMinYear;
	}

	@Override
	protected int getMax() {
		return mMaxYear;
	}

	@Override
	protected boolean isChecked() {
		return false;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
	}

	@Override
	protected int getAbsoluteMin() {
		return YearPublishedFilterData.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return YearPublishedFilterData.MAX_RANGE;
	}

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinYear = min;
		mMaxYear = max;
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min);
		if (min == YearPublishedFilterData.MIN_RANGE) {
			text = "<" + text;
		}
		text += " - " + String.valueOf(max);
		if (max == YearPublishedFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
