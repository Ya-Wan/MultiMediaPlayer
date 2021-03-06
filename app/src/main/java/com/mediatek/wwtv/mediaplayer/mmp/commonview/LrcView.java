package com.mediatek.wwtv.mediaplayer.mmp.commonview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mediatek.wwtv.mediaplayer.R;
import com.mediatek.wwtv.mediaplayer.mmp.util.LogicManager;
import com.mediatek.wwtv.mediaplayer.mmp.util.TextUtils;
import com.mediatek.wwtv.mediaplayer.mmpcm.audioimpl.LyricTimeContentInfo;
import com.mediatek.wwtv.util.MtkLog;

import java.util.List;
import java.util.Vector;

public class LrcView extends View {

    private static final String TAG = "LrcView";
    private static Vector<LyricTimeContentInfo> lrc_map;
    private Paint mWhitePaint;

    private Paint mRedPaint;
    private int[] lrcWidth;
    private int lrcHeight = 0;
    private int viewWidth = 0;
    private List<LyricTimeContentInfo> lrcarr;
    private int mCurrentLine = 0;
    private int mLrcLine;
    private int Lines = 10;
    private String noLrc;
    private int noLrcWidth = 0;

    private boolean isSetCurrentLine = false;
    private int lrcOffset = 10;

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        noLrc = context.getString(R.string.mmp_info_nolrc);
        mWhitePaint = new Paint();
        mRedPaint = new Paint();

        mWhitePaint.setAntiAlias(true);
        mWhitePaint.setTextSize(36);
        mWhitePaint.setColor(Color.WHITE);

        mRedPaint.setAntiAlias(true);
        mRedPaint.setTextSize(36);
        mRedPaint.setColor(0xff6eeeff);
    }

String tempContent;
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (lrcarr == null
                || LogicManager.getInstance(getContext()).lrcHide == true) {
            // canvas.drawText(noLrc, viewWidth / 3, lrcHeight / 2,
            // mWhitePaint);
            MtkLog.i(TAG, "-------- nolrc -------");
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.mmp_music_nolrc_bg);
            int width = getWidth();
            int height = getHeight();
            canvas.drawBitmap(bitmap, width / 2 - bitmap.getWidth() / 2,
                    160, new Paint());

            canvas.drawText(noLrc, width / 2 - noLrcWidth / 2, 250 + bitmap.getHeight(), mWhitePaint);
            return;
        }
        for (int i = 0; i < lrcarr.size(); i++) {
            if (lrcWidth.length > i && i != mCurrentLine) {
                canvas.drawText(lrcarr.get(i).getLyricContent(), lrcWidth[i], 40
                        + lrcHeight * i, mWhitePaint);
            }
        }
        // show current music word
        MtkLog.i(TAG, "lrcarr.size():" + lrcarr.size() + "mCurrentLine:" + mCurrentLine + "lrcWidth.length:" + lrcWidth.length);
        if (mCurrentLine < lrcarr.size() && mCurrentLine >= 0) {
            if (lrcWidth.length > mCurrentLine) {
                int currLrcWidth = (int) mWhitePaint.measureText(lrcarr.get(mCurrentLine).getLyricContent());
                if (currLrcWidth > getWidth()) {
                    if ((mAnimator == null || !mAnimator.isStarted())&& isSetCurrentLine) {
                        isSetCurrentLine = false;
                        /*if (!android.text.TextUtils.equals(tempContent, lrcarr.get(mCurrentLine).getLyricContent())) {
                            stopScrollLrc();
                        } else {

                        }*/
                        startScrollLrc(currLrcWidth - getWidth() + lrcOffset, lrcarr.get(mCurrentLine).getDuration());
                    }
                    Log.d("y.wan", "onDraw: " + lrcarr.get(mCurrentLine).getLyricContent() +
                            "*********: " + mCurTextXForHighLightLrc);
                    canvas.drawText(lrcarr.get(mCurrentLine).getLyricContent(),
                            mCurTextXForHighLightLrc, 40 + lrcHeight * mCurrentLine,
                            mRedPaint);
                } else {
                    canvas.drawText(lrcarr.get(mCurrentLine).getLyricContent(),
                            lrcWidth[mCurrentLine], 40 + lrcHeight * mCurrentLine,
                            mRedPaint);
                }
            }
            tempContent = lrcarr.get(mCurrentLine).getLyricContent();
        }
    }

    private void startScrollLrc(int endX, long duration) {
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofFloat(0, endX);
            mAnimator.addUpdateListener(updateListener);
        } else {
            mCurTextXForHighLightLrc = 0;
            mAnimator.cancel();
            mAnimator.setFloatValues(0, endX);
        }
        mAnimator.setDuration(duration);
//        mAnimator.setStartDelay((long) (duration * 0.3));
        mAnimator.start();
    }

    private void stopScrollLrc() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mCurTextXForHighLightLrc = 0;
    }

    /**
     *
     **/
    private float mCurTextXForHighLightLrc;
    private ValueAnimator mAnimator;
    /***
     *
     */
    ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mCurTextXForHighLightLrc = - (Float) animation.getAnimatedValue();
            Log.d(TAG, "mCurTextXForHighLightLrc=" + mCurTextXForHighLightLrc);
            invalidate();
        }
    };

    public void noLrc(String lrc) {
        lrcarr = null;
        noLrc = lrc;
        viewWidth = this.getWidth();
        lrcHeight = this.getHeight();

        noLrcWidth = (int) mWhitePaint.measureText(noLrc);
        this.invalidate();
    }

    public void init(Vector<LyricTimeContentInfo> lrc, int line) {
        mCurrentLine = 0;
        lrc_map = lrc;
        // Modified by Dan for fix hide lrc bug
        if (line == 0) {
            Lines = 10;
            //			setVisibility(View.INVISIBLE);
        } else {
            Lines = line;
        }

        lrcarr = new Vector<LyricTimeContentInfo>(Lines);
        lrcWidth = new int[Lines];
        if (mLrcLine >= lrc_map.size() - 1) {
            mLrcLine = 0;
        }
        if (mLrcLine + Lines < lrc_map.size() && mLrcLine > -1) {
            lrcarr = lrc_map.subList(mLrcLine, mLrcLine + Lines);
        } else {
            if (mLrcLine > -1) {
                lrcarr = lrc_map.subList(mLrcLine, lrc_map.size());
            }
        }

        viewWidth = this.getWidth();
        for (int i = 0; i < lrcarr.size(); i++) {
            if (lrcWidth.length > i) {
                int lrcWidth = (int) mWhitePaint.measureText(lrcarr.get(i).getLyricContent());
                if (lrcWidth > viewWidth) {
                    this.lrcWidth[i] = 0;
                } else {
                    this.lrcWidth[i] = (viewWidth - lrcWidth) / 2;
                }
            }
        }
        noLrcWidth = (int) mWhitePaint.measureText(noLrc);
        lrcHeight = this.getHeight() / Lines;
    }

    public void setLines(int line) {
        Lines = line;
    }

    public void setlrc(int currentline, boolean isRefreshImmediately) {

        if (lrc_map == null) {
            return;
        }
        if (currentline > lrc_map.size() - 1) {
            return;
        }
        if (currentline != mLrcLine || isRefreshImmediately) {
            MtkLog.i(TAG, "--------  mLrcLine  :" + mLrcLine
                    + "    currentline:" + currentline);
            if (currentline != mLrcLine)
                isSetCurrentLine = true;
            mLrcLine = currentline;
            int endline;
            if (currentline % Lines == 0) {
                mCurrentLine = 0;
            } else {
                mCurrentLine = currentline % Lines;
                currentline = (currentline / Lines) * Lines;
            }

            if ((currentline + Lines) >= lrc_map.size()) {
                endline = lrc_map.size();
            } else {
                endline = currentline + Lines;
            }
            if (currentline >= 0 && endline >= 0) {
                lrcarr = lrc_map.subList(currentline, endline);
            }
            if (lrcarr == null)
                return;//add by yx for fix nullpointException
            for (int i = 0; i < lrcarr.size(); i++) {
                if (lrcWidth.length > i) {
                    int lrcWidth = (int) mWhitePaint.measureText(lrcarr.get(i).getLyricContent());
                    if (lrcWidth > viewWidth) {
                        this.lrcWidth[i] = 0;
                    } else {
                        this.lrcWidth[i] = (viewWidth - lrcWidth) / 2;
                    }
                }
            }
            this.invalidate();
        }
    }
}
