package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;
import com.squareup.picasso.Picasso;

public class ImageFragment extends SherlockFragment {
	private String mImageUrl;
	private ImageView mImageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mImageUrl = intent.getStringExtra(ImageActivity.KEY_IMAGE_URL);

		if (TextUtils.isEmpty(mImageUrl)) {
			return;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image, null);

		mImageView = (ImageView) rootView.findViewById(R.id.image);
		Picasso.with(getActivity()).load(HttpUtils.ensureScheme(mImageUrl))
			.placeholder(R.drawable.thumbnail_image_empty).error(R.drawable.thumbnail_image_empty).into(mImageView);

		return rootView;
	}
}
