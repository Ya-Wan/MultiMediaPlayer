package com.mediatek.wwtv.mediaplayer.mmp.commonview;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mediatek.wwtv.mediaplayer.R;
import com.mediatek.wwtv.mediaplayer.mmp.util.MenuFatherObject;
import com.mediatek.wwtv.mediaplayer.util.ScreenConstant;
import com.mediatek.wwtv.util.MtkLog;

public class ListAdapterMenu extends BaseAdapter {
	private static final String TAG = "ListAdapterMenu";
	private List<MenuFatherObject> mDataList = new ArrayList<MenuFatherObject>();
	private Context mContext;
	private int mStyle;

	public static final int PAGE_SIZE = 6;

	private int mOffset = 0;

	@SuppressWarnings("unchecked")
	public ListAdapterMenu(Context context, List<?> list, int style) {
		mContext = context;
		mDataList = (List<MenuFatherObject>) list;
		mStyle = style;
	}

	public void initList(List<MenuFatherObject> mList) {
		mDataList = mList;
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		Object obj = getItem(position);
		if (obj == null || obj.toString().equals("false")) {
			return false;
		}
		// MtkLog.d(TAG, "isEnabled:" + getItem(position));
		return super.isEnabled(position);
	}

	public int getCount() {
		if (mDataList.size() > 6) {
			if (mDataList.size() - mOffset >= 6) {
				return PAGE_SIZE;
			}
			return mDataList.size() - mOffset;
		} else {
			return mDataList.size();
		}
	}

	public Object getItem(int position) {
		return mDataList.get(position + mOffset).enable;
	}

	public long getItemId(int arg0) {
		return arg0;
	}

	public View getView(int position, View view, ViewGroup arg2) {
		view = LayoutInflater.from(mContext).inflate(mStyle, null);
		//fix by tjs 
//		view.setLayoutParams(new ListView.LayoutParams(
//				LayoutParams.MATCH_PARENT, (int)(ScreenConstant.SCREEN_HEIGHT/18)));
	   //fix by tjs 
		TextView tv = (TextView) view.findViewById(R.id.mmp_menulist_tv);
		int pos = position+mOffset;
		pos = pos >= 0 ? pos: 0;
		tv.setText(mDataList.get(pos).content);
		if (!mDataList.get(pos).enable) {
			tv.setTextColor(Color.DKGRAY);
		}
		return view;
	}

	public void setOffset(int offset) {
		mOffset = offset;
		
		mMyOffsetListener.offset(this.mOffset);
	}
	public void setMyOffsetListener(MyOffsetListener mMyOffsetListener)
	{
		this.mMyOffsetListener=mMyOffsetListener;
	}
	private MyOffsetListener mMyOffsetListener;
	public interface MyOffsetListener
	{
		void offset(int offset);
	}
	public int getOffset() {
		return mOffset;
	}

	static class ViewHolder {
		TextView tv;
		ImageView img;
	}
}
