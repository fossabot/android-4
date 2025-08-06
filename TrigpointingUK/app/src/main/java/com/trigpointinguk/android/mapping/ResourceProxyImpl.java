package com.trigpointinguk.android.mapping;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.trigpointinguk.android.R;

public class ResourceProxyImpl {

	private final Context mContext;

	public ResourceProxyImpl(final Context pContext) {
		mContext = pContext;
	}

	public String getString(final String pResId) {
		try {
			final int res = R.string.class.getDeclaredField(pResId).getInt(null);
			return mContext.getString(res);
		} catch (final Exception e) {
			return pResId;
		}
	}

	public Bitmap getBitmap(final String pResId) {
		try {
			final int res = R.drawable.class.getDeclaredField(pResId).getInt(null);
			return BitmapFactory.decodeResource(mContext.getResources(), res);
		} catch (final Exception e) {
			return null;
		}
	}

	public Drawable getDrawable(final String pResId) {
		try {
			final int res = R.drawable.class.getDeclaredField(pResId).getInt(null);
			return mContext.getResources().getDrawable(res);
		} catch (final Exception e) {
			return null;
		}
	}
}
