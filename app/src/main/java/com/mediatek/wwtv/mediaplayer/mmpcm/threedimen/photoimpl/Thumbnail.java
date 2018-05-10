package com.mediatek.wwtv.mediaplayer.mmpcm.threedimen.photoimpl;

import com.mediatek.wwtv.mediaplayer.mmpcm.Info;

import android.graphics.Bitmap;

public class Thumbnail extends Info{
	static private Thumbnail  mThumbnail;
	
	public static Thumbnail getInstance() {
		if (mThumbnail == null) {
			synchronized (Thumbnail.class) {
				if (mThumbnail == null) {
					mThumbnail = new Thumbnail();
				}
			}
		}
		return mThumbnail;
	}
	
	public Bitmap getThumbnail(){
		return null;
	}
}
