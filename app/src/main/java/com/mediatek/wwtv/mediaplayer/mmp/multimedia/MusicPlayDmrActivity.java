
package com.mediatek.wwtv.mediaplayer.mmp.multimedia;

import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.gamekit.GKView;
import com.mediatek.gamekit.GKView.MessageListener;
import com.mediatek.wwtv.mediaplayer.mmpcm.UIMediaPlayer;

import android.media.MediaPlayer;

import com.mediatek.mmp.MtkMediaPlayer;
import com.mediatek.mmp.MtkMediaPlayer.OnSeekCompleteListener;
import com.mediatek.mmp.MtkMediaPlayer.OnCompletionListener;
import com.mediatek.mmp.MtkMediaPlayer.OnErrorListener;
import com.mediatek.mmp.MtkMediaPlayer.OnInfoListener;
import com.mediatek.mmp.MtkMediaPlayer.OnPreparedListener;
import com.mediatek.wwtv.mediaplayer.mmpcm.audioimpl.AudioConst;
import com.mediatek.wwtv.mediaplayer.mmpcm.audioimpl.LyricTimeContentInfo;
import com.mediatek.wwtv.mediaplayer.mmpcm.fileimpl.FileConst;
import com.mediatek.wwtv.mediaplayer.mmpcm.mmcimpl.Const;
import com.mediatek.wwtv.mediaplayer.R;
import com.mediatek.wwtv.mediaplayer.mmp.MediaMainActivity;
import com.mediatek.wwtv.mediaplayer.mmp.commonview.ControlView.ControlPlayState;
import com.mediatek.wwtv.mediaplayer.mmp.commonview.LrcView;
import com.mediatek.wwtv.mediaplayer.mmp.commonview.ScoreView;
import com.mediatek.wwtv.mediaplayer.mmp.commonview.ShowInfoView;
import com.mediatek.wwtv.mediaplayer.mmp.gamekit.content.MutilMediaConst;
import com.mediatek.wwtv.mediaplayer.mmp.gamekit.filebrowse.MFlimBsActivity;
import com.mediatek.wwtv.mediaplayer.mmp.model.MultiFilesManager;
import com.mediatek.wwtv.mediaplayer.mmp.multimedia.MediaPlayActivity.DmrListener;
import com.mediatek.wwtv.mediaplayer.mmp.multimedia.MediaPlayActivity.PlayException;
import com.mediatek.wwtv.mediaplayer.mmp.util.AsyncLoader;
import com.mediatek.wwtv.mediaplayer.mmp.util.BitmapCache;
import com.mediatek.wwtv.mediaplayer.mmp.util.BitmapCache.DecodeInfo;
import com.mediatek.wwtv.mediaplayer.mmp.util.DmrHelper;
import com.mediatek.wwtv.mediaplayer.mmp.util.LogicManager;
import com.mediatek.wwtv.mediaplayer.mmp.util.MultiMediaConstant;
import com.mediatek.wwtv.mediaplayer.mmp.util.AsyncLoader.LoadWork;
import com.mediatek.wwtv.util.KeyMap;
import com.mediatek.wwtv.util.MtkLog;
import com.mediatek.wwtv.mediaplayer.util.Util;

import android.os.SystemProperties;

public class MusicPlayDmrActivity extends MediaPlayActivity implements GKView.LoadListener {

  private static final String TAG = "MusicPlayDmrActivity";

  private static final int PROGRESS_CHANGED = 0;

  private static final int PROGRESS_START = 1;
  // Spectrum
  private static final int PROGRESS_SCOREVIEW = 2;

  private static final int AUDIO_CHANGED = 3;

  private static final int AUDIO_RESET = 4;

  private static final int NOSUPPORT_PLAYNEXT = 5;
  private static final int SPEED_UPDATE = 6;
  private static final int FINISH_AUDIO = 7;
  private static final int CLEAR_LRC = 8;
  private static final int DISMISS_NOT_SUPPORT = 10;
  // for Gamekit start
  private static final int RUN_LUA_CHUNK = 11;
  private static final int RUN_LUA_ANIMA = 12;
  private static final int LOAD_GAMEKIT_VIEW = 13;
  private static final int HANDLE_ERROR_MSG = 14;
  // private static final int LOAD_GAMEKIT_RESUME = 14;
  private static final int DELAY_RUN_MILLIS = 2000;
  private static final int DELAY_LOADGAMEKIT_MILLS = 1;
  private GKView mEngineView;
  private boolean mIsClose3D = false;
  private static final String AIMAL_END = "AnimEnd";
  private static final String AIMAL_OPEN = "Open";
  private static final String AIMAL_CLOSE = "Close";
  // for Gamekit end
  private static final int DELAYMILLIS = 400;
  private static final int DELAYMILLIS_FOR_PROGRESS = 1000;
  private static final int DELAYMILLIS_DESTROY = 5000;

  private static final int SEEK_DURATION = 3000;

  // //add by xudong chen 20111204 fix DTV00379662
  public static final long SINGLINE = 1;
  public static final long MULTILINE = 8;
  public static final long OFFLINE = 0;
  // end
  private LinearLayout vLayout;

  private ImageView vThumbnail;

  private ScoreView mScoreView;

  private LrcView mLrcView;
  private boolean isDuration = false;

  private Vector<LyricTimeContentInfo> lrc_map;

  private boolean playFlag = true;
  private boolean isActivityLiving = true;
  private boolean retrunFromTipDismis = false;
  private boolean isCenterKey2Pause = false;

  private int mAudioSource = 0;

  private int mAudioFileType = 0;
  // private int onerrorSendWhat=0;

  private int mTotalTime;

  private final Handler myHandler = new Handler() {

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      MtkLog.d(TAG, " music msg.what:" + msg.what);
      switch (msg.what) {

      // add for play 3D animal start
        case LOAD_GAMEKIT_VIEW: {
          if (mEngineView == null) {
            MtkLog.e(TAG, "LOAD_GAMEKIT_VIEW");
            findGameKitView();
          }
          break;
        }
        case HANDLE_ERROR_MSG:
          MtkLog.d(TAG, "HANDLE_ERROR_MSG ~~");
          mLogicManager.stopAudio();
          sendEmptyMessageDelayed(NOSUPPORT_PLAYNEXT, 3000);
          if (isNotSupport) {
            mScoreView.setVisibility(View.INVISIBLE);
            removeMessages(PROGRESS_SCOREVIEW);
          }
          DmrHelper.tellDmcState(getApplicationContext(), 2);
          myHandler.sendEmptyMessageDelayed(FINISH_AUDIO, DELAYMILLIS_DESTROY);// (FINISH_AUDIO);
          break;

        case RUN_LUA_CHUNK: {
          luaPlayAnim();
          break;
        }

        case RUN_LUA_ANIMA: {
          luaRotationAnim();
          break;
        }
        // by lei add for play 3D animal end
        case PROGRESS_CHANGED: {
          if (hasMessages(PROGRESS_CHANGED)) {
            removeMessages(PROGRESS_CHANGED);
          }
          MtkLog.d(TAG,
              "PROGRESS_CHANGED mLogicManager.getAudioStatus() = " + mLogicManager.getAudioStatus()
                  + "isDuration =" + isDuration);
          if (mLogicManager.getAudioStatus() < AudioConst.PLAY_STATUS_PREPARED) {
            break;
          }
          if (mControlView != null) {
            int progressTmp = 0;
            if (isDuration) {
              progressTmp = mLogicManager.getPlaybackProgress();
            } else {
              progressTmp = mLogicManager.getAudioBytePosition();
            }
            long progress = progressTmp & 0xffffffffL;
            if (progress >= 0) {
              mControlView.setCurrentTime(progress);
              mControlView.setProgress(progressTmp);
            }
            int totalTimeTmp =  mLogicManager.getTotalPlaybackTime();
            long totalTime = totalTimeTmp & 0xffffffffL;
            DmrHelper.tellDmcProgressState(totalTime / 1000,
                progress / 1000);
          }
          int enable = SystemProperties.getInt(ShowInfoView.PROPERTIES, 0);
          MtkLog.i(TAG, "enable:" + enable);
          if (0 == enable) {
            SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(1));
            sendBroadcast(new Intent(ShowInfoView.DURATION));
          }
          sendEmptyMessageDelayed(PROGRESS_CHANGED, DELAYMILLIS_FOR_PROGRESS);
          break;
        }
        case PROGRESS_START: {
          if (null == lrc_map || (lrc_map.size() == 0)
              || null == mLrcView) {
            return;
          }
          int line = mLogicManager.getLrcLine(mLogicManager
              .getPlaybackProgress());

          if (line != -1) {
            mLrcView.setlrc(line, false);
          }

          if (line == lrc_map.size() - 1) {
            return;
          }

          sendEmptyMessageDelayed(PROGRESS_START, DELAYMILLIS);
          break;
        }

        case PROGRESS_SCOREVIEW: {
          if (!isShowSpectrum() || mLogicManager.isMute()) {
            return;
          }
          if (hasMessages(PROGRESS_SCOREVIEW)) {
            removeMessages(PROGRESS_SCOREVIEW);
          }
          mScoreView.update(mLogicManager.getAudSpectrum());
          mScoreView.invalidate();
          sendEmptyMessageDelayed(PROGRESS_SCOREVIEW, DELAYMILLIS);

          break;
        }
        case AUDIO_CHANGED: {
          setMusicInfo();
          break;
        }
        case AUDIO_RESET: {
          resetMusicInfo();
          break;
        }
        case NOSUPPORT_PLAYNEXT:
          if (isActivityLiving) {
            MtkLog.i(TAG, "  NOSUPPORT_PLAYNEXT: dismissNotSupprot");
            dismissNotSupprot();
            dismissMenuDialog();
          }
          mLogicManager.playNextAudio();
          break;
        case SPEED_UPDATE:
          MtkLog.i(TAG, "  SPEED_UPDATE  speed:" + SPEED_UPDATE);
          // set play icon.
          if (mControlView != null) {
            mLogicManager.setAuidoSpeed(1);
            mControlView.onFast(1, 1, Const.FILTER_AUDIO);
            setMusicInfo();
            mControlView.play();
          }
          break;
        // add by keke 1215 fix DTV00380491
        case FINISH_AUDIO: {
          /* fix cr DTV00386326 by lei 1228 */
          mLogicManager.unbindService(MusicPlayDmrActivity.this);
          mLogicManager.stopAudio();
          MusicPlayDmrActivity.this.finish();
        }
          break;
        case CLEAR_LRC:
          clearLrc();
          break;

        case DISMISS_NOT_SUPPORT:
          if (isActivityLiving) {
            MtkLog.i(TAG, "  DISMISS_NOT_SUPPORT: dismissNotSupprot");
            dismissNotSupprot();
          }
          break;

        default:
          break;
      }

    }
  };

  // by lei add for play 3D animal start
  private void luaPlayAnim() {
    if (mIsClose3D)
      return;

    if (mEngineView != null) {
      String chunk = "openAnim();";
      mEngineView.runScript(chunk, false);
    }
  }

  private void luaStopAnim() {
    if (mIsClose3D)
      return;

    if (mEngineView != null) {
      String chunk = "closeAnim();";
      mEngineView.runScript(chunk, false);
    }
  }

  private void luaRotationAnim() {
    if (mIsClose3D)
      return;
    if (mEngineView != null) {
      String chunk = "rotationAnim();";
      mEngineView.runScript(chunk, false);
    }
  }

  private final MessageListener mGkListener = new MessageListener() {

    @Override
    public void onMessage(String from, String to, String subject,
        String body) {
      MtkLog.d(TAG, "subject : " + subject + ":" + body);
      if (subject != null && subject.equals(AIMAL_END)) {
        if (body != null) {
          if (body.equals(AIMAL_OPEN)) {
            myHandler.removeMessages(RUN_LUA_ANIMA);
            myHandler.sendEmptyMessage(RUN_LUA_ANIMA);
          } else if (body.equals(AIMAL_CLOSE)) {
            // by lei add.
            myHandler.removeMessages(RUN_LUA_CHUNK);
            myHandler.sendEmptyMessage(RUN_LUA_CHUNK);
          }
        }
      }

    }
  };

  // by lei add for play 3D animal end

  private void updateTime(int totalTime) {
    if (null != mControlView) {
      mControlView.setEndtime(totalTime);
      mControlView.setProgressMax(totalTime);
    }

    if (null != mInfo && mInfo.isShowing()) {
      mInfo.updateTime(totalTime);
    }
  }

  private final ControlPlayState mControlImp = new ControlPlayState() {

    @Override
    public void play() {
      Log.i(TAG, "start_play");
      /* add by lei for fix cr 386020 */

      if (isNotSupport) {
        Log.i(TAG, "start_play isNotSupport:" + isNotSupport);
        return;
      }
      if (null == mLogicManager.getAudioPlaybackService()) {
        Log.i(
            TAG,
            "start_play mLogicManager.getAudioPlaybackService():"
                + mLogicManager.getAudioPlaybackService());
        return;
      }

      Log.i(TAG, "playAudio");
      mLogicManager.playAudio();
      Log.i(TAG, "playAudio end");
      /* add by lei for fix cr DTV00381177&DTV00390959 */
      Log.i(TAG, "***********show Spectrum****************"
          + isShowSpectrum());
      if (isShowSpectrum()) {
        // add by keke 2.1 for DTV00393701
        Log.i(TAG, "clearTiles");
        mScoreView.clearTiles();
        Log.i(TAG, "setVisibility");
        mScoreView.setVisibility(View.VISIBLE);
        Log.i(TAG, "PROGRESS_SCOREVIEW");
        myHandler.sendEmptyMessage(PROGRESS_SCOREVIEW);
      }

      Log.i(TAG, "***********PROGRESS_CHANGED****************");
      myHandler.sendEmptyMessage(PROGRESS_CHANGED);
      Log.i(TAG, "***********PROGRESS_START****************");
      myHandler.sendEmptyMessage(PROGRESS_START);

    }

    @Override
    public void pause() {
      /* add by lei for fix cr 386020 */
      if (isNotSupport || null == mLogicManager.getAudioPlaybackService()) {
        return;
      }
      // change by shuming fix CR 00386020
      try {
        mLogicManager.pauseAudio();
      } catch (Exception e) {
        featureNotWork(getString(R.string.mmp_featue_notsupport));
        if ((AudioConst.MSG_ERR_CANNOTPAUSE).equals(e.getMessage()
            .toString())
            && isCenterKey2Pause) {
          isCenterKey2Pause = false;
          // displayErrorMessage(AudioConst.MSG_FILE_NOT_SUPPORT, 0);
          if (isActivityLiving) {
            // myHandler.sendEmptyMessageDelayed(DISMISS_NOT_SUPPORT,3000);
            throw new IllegalStateException(
                AudioConst.MSG_ERR_CANNOTPAUSE);
          }
        } else if (AudioConst.MSG_ERR_PAUSEEXCEPTION.equals(e.getMessage()
            .toString())) {
          throw new IllegalStateException(
              AudioConst.MSG_ERR_PAUSEEXCEPTION);
        } else {
          throw new IllegalStateException(e.getMessage());
        }

        throw new IllegalStateException(e.getMessage());
        // TODO: handle exception
      }
      // end

      mScoreView.clearTiles();
      mScoreView.setVisibility(View.INVISIBLE);

      /* add by lei for fix cr DTV00381177&DTV00390959 */
      myHandler.removeMessages(PROGRESS_SCOREVIEW);
      // change by shuming fix CR DTV00
      myHandler.removeMessages(PROGRESS_START);
      myHandler.removeMessages(PROGRESS_CHANGED);

    }
  };

  // MTK MEDIAPLAYER

  private final OnErrorListener mtkErrorListener = new OnErrorListener() {
    // @Override if add "override" P4 will build failure
    public boolean onError(MtkMediaPlayer arg0, final int what,
        final int extra) {
      MtkLog.i(TAG, "MtkMediaPlayer OnErrorListener  targ1:" + what + "  arg2" + extra
          + " " + System.currentTimeMillis());

      return handleError(what, extra);
    }
  };

  private final OnInfoListener mtkInfoListener = new OnInfoListener() {

    public boolean onInfo(MtkMediaPlayer arg0, int arg1, int arg2) {
      // TODO Auto-generated method stub
      return handleInfo(arg1);

    }
  };

  private final OnPreparedListener mtkPreparedListener = new OnPreparedListener() {

    public void onPrepared(MtkMediaPlayer mp) {
      handlePrepare();
    }

  };

  private final OnSeekCompleteListener mtkSeekCompletionListener = new OnSeekCompleteListener() {

    public void onSeekComplete(MtkMediaPlayer mp) {

      handleSeekComplete();
    }

  };

  /* true playing(ff, fr,fb), else stop or pause */
  // private boolean isPlay = true;
  private final OnCompletionListener mtkCompletionListener = new OnCompletionListener() {

    public void onCompletion(MtkMediaPlayer mp) {

      handleComplete();
    }

  };

  // MEDIAPLAYER

  private final MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
    // @Override if add "override" P4 will build failure
    @Override
    public boolean onError(MediaPlayer arg0, final int what,
        final int extra) {
      MtkLog.i(TAG, " MediaPlayer.OnErrorListener  OnErrorListener  targ1:" + what + "  arg2"
          + extra
          + " " + System.currentTimeMillis());

      return handleError(what, extra);
    }
  };

  private final MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {

    @Override
    public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
      // TODO Auto-generated method stub
      return handleInfo(arg1);

    }
  };

  private final MediaPlayer.OnPreparedListener mPreparedListener
  = new MediaPlayer.OnPreparedListener() {

    @Override
    public void onPrepared(MediaPlayer mp) {
      handlePrepare();
    }

  };

  /* true playing(ff, fr,fb), else stop or pause */
  // private boolean isPlay = true;
  private final MediaPlayer.OnCompletionListener mCompletionListener
  = new MediaPlayer.OnCompletionListener() {

    @Override
    public void onCompletion(MediaPlayer mp) {

      handleComplete();
    }

  };

  private final MediaPlayer.OnSeekCompleteListener
  mSeekCompletionListener = new MediaPlayer.OnSeekCompleteListener() {

    @Override
    public void onSeekComplete(MediaPlayer mp) {

      handleSeekComplete();
    }

  };

  private boolean handleError(final int what, final int extra) {
    // Runnable mErrorRunnable = null;

    SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(0));

    MtkLog.i(TAG, "handleError isNotSupport = " + isNotSupport + "what = " + what);
    if (isNotSupport || !isActivityLiving) {
      return true;
    }

    switch (what) {
      case com.mediatek.MtkMediaPlayer.MEDIA_ERROR_FILE_NOT_SUPPORT:
      case com.mediatek.MtkMediaPlayer.MEDIA_ERROR_OPEN_FILE_FAILED:
        isNotSupport = true;
        featureNotWork(getResources()
            .getString(R.string.mmp_file_notsupport));
        myHandler.sendEmptyMessage(HANDLE_ERROR_MSG);
        break;
      case com.mediatek.MtkMediaPlayer.MEDIA_ERROR_FILE_CORRUPT:
        isNotSupport = true;
        featureNotWork(getResources().getString(R.string.mmp_file_corrupt));
        myHandler.sendEmptyMessage(HANDLE_ERROR_MSG);
        break;
      default:

        MtkLog.i(TAG, "displayErrorMessage what = " + what + "extra = " + extra);
        // featureNotWork(getResources().getString(R.string.mmp_file_notsupport));
        break;
    }

    return true;

  }

  private boolean handleInfo(int arg1) {
    MtkLog.d(TAG, "handleInfo arg1 = " + arg1);
    switch (arg1) {
      case AudioConst.MSG_POSITION_UPDATE:
        int enable = SystemProperties.getInt(ShowInfoView.PROPERTIES, 0);
        MtkLog.i(TAG, "enable:" + enable);
        if (0 == enable) {
          SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(1));
          sendBroadcast(new Intent(ShowInfoView.DURATION));
        }
        ;
        myHandler.sendEmptyMessage(PROGRESS_CHANGED);
        DmrHelper.tellDmcState(this, 0);
        break;

      case AudioConst.MEDIA_INFO_METADATA_COMPLETE:
        SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(0));
        myHandler.sendEmptyMessage(FINISH_AUDIO);
        // isNotSupport = true;
        return false;
      case com.mediatek.MtkMediaPlayer.MEDIA_INFO_ON_REPLAY:
        // myHandler.sendEmptyMessage(SPEED_UPDATE);
        mLogicManager.replayAudio();
        break;
      case com.mediatek.MtkMediaPlayer.MEDIA_INFO_AUDIO_ENCODE_FORMAT_UNSUPPORT:
        if (isNotSupport) {
          return false;
        }
        isNotSupport = true;
        playExce = PlayException.AUDIO_NOT_SUPPORT;
        featureNotWork(getResources().getString(R.string.mmp_audio_notsupport));
        if (null != mControlView) {
          mControlView.setPauseIconGone();
        }
        myHandler.sendEmptyMessage(HANDLE_ERROR_MSG);

        break;
      case com.mediatek.MtkMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
        MtkLog.d(TAG, "enter onInfo: MEDIA_INFO_NOT_SEEKABLE");
        featureNotWork(getString(R.string.mmp_featue_notsupport));
        break;
      case AudioConst.MEDIA_INFO_FEATURE_NOT_SUPPORT:
        MtkLog.i(TAG, "AudioConst.MEDIA_INFO_FEATURE_NOT_SUPPORT");
        featureNotWork(getString(R.string.mmp_featue_notsupport));
        break;
      case AudioConst.MEDIA_INFO_DATA_BEFORE_SET_DATA_SOURCE_STATE:
        isNotSupport = false;
        playExce = PlayException.DEFAULT_STATUS;
        myHandler.sendEmptyMessage(AUDIO_RESET);
        // resetMusicInfo();
        break;
      case AudioConst.MEDIA_INFO_PLAY_RENDERING_START:
        myHandler.sendEmptyMessage(AUDIO_CHANGED);
        MtkLog.i(TAG, "MEDIA_INFO_PLAY_RENDERING_START");
        DmrHelper.tellDmcState(this, DmrHelper.DLNA_DMR_PLAYED);
        MtkLog.i(TAG, "MEDIA_INFO_PLAY_RENDERING_START END");
        break;
      default:
        MtkLog.d(TAG, "enter onInfo:" + arg1);
        break;
    }
    return false;

  }

  private void handlePrepare() {
    MtkLog.i(TAG, " audio  OnPrepared   -------------- ");
    /* add by lei for fix cr 386020 */

    myHandler.sendEmptyMessageDelayed(PROGRESS_CHANGED, DELAYMILLIS_FOR_PROGRESS);
    myHandler.sendEmptyMessage(PROGRESS_START);
    removeScore(isHideSperum);
    reSetController();

  }

  private void handleSeekComplete() {
    MtkLog.d(TAG, "handleSeekComplete!!!!");
    DmrHelper.tellDmcState(getApplicationContext(), 0);
    if (mControlView != null) {
      int progressTmp = 0;
      if (isDuration) {
        progressTmp = mLogicManager.getPlaybackProgress();
      } else {
        progressTmp = mLogicManager.getAudioBytePosition();
      }
      long progress = progressTmp & 0xffffffffL;
      if (progress >= 0) {
        mControlView.setCurrentTime(progress);
        mControlView.setProgress(progressTmp);
      }
      int totalTimeTmp =  mLogicManager.getTotalPlaybackTime();
      long totalTime = totalTimeTmp & 0xffffffffL;
      DmrHelper.tellDmcProgressState(totalTime / 1000,
          progress / 1000);
    }
    if (myHandler != null && !myHandler.hasMessages(PROGRESS_CHANGED)) {
      myHandler.sendEmptyMessage(PROGRESS_CHANGED);
    }
  }

  private void handleComplete() {
    /*
     * MtkLog.i(TAG, "-------------- Completion ----------------- flag=");
     * myHandler.sendEmptyMessage(CLEAR_LRC); removeMessages(); luaStopAnim();
     * myHandler.sendEmptyMessage(NOSUPPORT_PLAYNEXT);
     */
    this.finish();
  }

  private void setMusicInfo() {
    // Added by Dan for fix bug DTV00384892
    lrc_map = mLogicManager.getLrcInfo();
    isDuration = true;
    if (mControlView != null) {
      // getTotalPlaybackTime return 0 or minus,progressbar shows size/total-size DTV00595824
      int times = mLogicManager.getTotalPlaybackTime();
      // Modified by yongzheng for fix CR DTV00388558 12/1/12
      if (!isNotSupport) {
        mControlView.showProgress();
        mControlView.setCurrentTime(0);
        if (times <= 0) {
          // progressFlag = true;
          isDuration = false;
          mControlView.setTimeViewVisibility(false);
          // progressbar shows size/total-size,starttime && endtime hide
          times = (int) mLogicManager.getAudioFileSize();
        }
        mControlView.setProgressMax(times);
        if (isDuration) {
          SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(1));
        }
        mControlView.setEndtime(times);

      }
    }
    initLrc(mPerLine);

    if (null != mInfo && mInfo.isShowing()) {
      mInfo.setAudioView();
    }

    final String path = mLogicManager.getCurrentPath(Const.FILTER_AUDIO);
    if (null != path) {
      final BitmapCache cache = BitmapCache.createCache(false);
      BitmapCache.DecodeInfo cover = cache.getDecodeInfo(path);

      if (cover != null) {
        MtkLog.i("xxxxxxxxxxx", "-----------Hit Cache------- path: " + path);
        vThumbnail.setImageBitmap(cover.getBitmap());
      } else {

        new Thread(new Runnable() {

          @Override
          public void run() {
            final Bitmap bmp = mLogicManager.getAlbumArtwork(mAudioFileType,
                path, vThumbnail.getWidth(), vThumbnail.getHeight());

            MtkLog.i(TAG, "setMusicInfo load bmp= " + bmp);

            final boolean isFailed = (bmp == null ? false : true);
            BitmapCache.DecodeInfo info = cache.new DecodeInfo(bmp, isFailed);
            cache.putDecodeInfo(path, info);
            vThumbnail.post(new Runnable() {

              @Override
              public void run() {
                MtkLog.i(TAG, "setMusicInfo setbmp result = " + bmp);
                vThumbnail.setImageBitmap(bmp);
              }

            });
          }

        }).start();

      }

    }
  }

  public void resetMusicInfo() {
    MtkLog.d(TAG, "resetMusicInfo!~ dismissNotSupprot");
    dismissMenuDialog();
    dismissNotSupprot();
    isNotSupport = false;
    // onerrorSendWhat=0;
    if (mControlView != null) {
      mControlView.reSetAudio();
      mControlView.setProgress(0);
      mControlView.hideProgress();
      mControlView.setRepeat(Const.FILTER_AUDIO);
      mControlView.setVolumeMax(maxVolume);
      mControlView.setCurrentVolume(currentVolume);
      mControlView.setFileName(DmrHelper.getFileTitle(true));
      mControlView.setFilePosition(mLogicManager.getAudioPageSize());

    }

    removeMessages();

    if (null != vThumbnail) {
      vThumbnail.setImageBitmap(null);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mmp_musicplay);
    turnOfAnimation();
    findView();
    String dataStr = getIntent().getDataString();
    if ((0 != SystemProperties.getInt(AUTO_TEST_PROPERTY, 0) && dataStr != null)
        && !MediaMainActivity.mIsDlnaAutoTest && !MediaMainActivity.mIsSambaAutoTest) {
      autoTest(Const.FILTER_AUDIO, MultiFilesManager.CONTENT_AUDIO);
    }

    getIntentData();
    initData();
    // add by keke for fix DTV00380638
    mControlView.setRepeatVisibility(Const.FILTER_AUDIO);
    setRepeatMode();
    showPopUpWindow(vLayout);
    Util.LogLife(TAG, "onCreate");
  }

  private void turnOfAnimation() {
    // TODO Auto-generated method stub
    mIsClose3D = true;
  }

  /**//**
       * Set Spreum status, true hide, false display.
       */
  /*
   * private boolean mIsHideSperum = false;
   */

  @Override
  public void removeScore(boolean ishide) {

    if (ishide) {

      mScoreView.setVisibility(View.INVISIBLE);
      myHandler.removeMessages(PROGRESS_SCOREVIEW);
    } else {
      mScoreView.clearTiles();
      mScoreView.setVisibility(View.VISIBLE);
      myHandler.sendEmptyMessageDelayed(PROGRESS_SCOREVIEW, DELAYMILLIS);
    }

  }

  public void removeScorePause() {
    mScoreView.setVisibility(View.INVISIBLE);
  }

  @Override
  public boolean isShowSpectrum() {
    return !isHideSperum;
    // return mScoreView.isShown();
  }

  public void initLrc(int perline) {
    myHandler.removeMessages(PROGRESS_START);
    mLrcView.setVisibility(View.VISIBLE);
    if (null != lrc_map && lrc_map.size() > 0) {
      MtkLog.d(TAG, "perline:" + perline);
      mLrcView.init(lrc_map, perline);
      myHandler.sendEmptyMessageDelayed(PROGRESS_START, DELAYMILLIS);
    } else {
      mLrcView.noLrc(getString(R.string.mmp_info_nolrc));
    }
  }

  @Override
  public void setLrcLine(int perline) {
    mLrcView.setVisibility(View.VISIBLE);
    if (null != lrc_map && lrc_map.size() > 0) {
      if (null != mLrcView) {
        mLrcView.setLines(perline);
        int progress = mLogicManager.getPlaybackProgress();
        if (progress >= 0) {
          int currentline = mLogicManager.getLrcLine(progress);
          mLrcView.setlrc(currentline, true);
        }
        myHandler.sendEmptyMessageDelayed(PROGRESS_START, DELAYMILLIS);
      }
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void hideLrc() {
    myHandler.removeMessages(PROGRESS_START);
    mLrcView.setVisibility(View.INVISIBLE);
  }

  private void getIntentData() {
    // dmr_2014 musicplay source
    if (isDmrSource) {
      mAudioSource = AudioConst.PLAYER_MODE_HTTP;
      mDmrListener = new DmrListener();
      DmrHelper.setListener(mDmrListener);
      return;
    }

    mAudioSource = MultiFilesManager.getInstance(this)
        .getCurrentSourceType();

    switch (mAudioSource) {
      case MultiFilesManager.SOURCE_LOCAL:
        mAudioSource = AudioConst.PLAYER_MODE_LOCAL;
        mAudioFileType = FileConst.SRC_USB;
        break;
      case MultiFilesManager.SOURCE_SMB:
        mAudioSource = AudioConst.PLAYER_MODE_SAMBA;
        mAudioFileType = FileConst.SRC_SMB;
        break;
      case MultiFilesManager.SOURCE_DLNA:
        mAudioSource = AudioConst.PLAYER_MODE_DLNA;
        mAudioFileType = FileConst.SRC_DLNA;
        break;
      default:
        break;
    }
  }

  private void initData() {
    mLogicManager = LogicManager.getInstance(this);
    if (AudioConst.PLAYER_MODE_LOCAL == mAudioSource) {
      mLogicManager.setAudioPreparedListener(mPreparedListener);
      mLogicManager.setAudioSeekCompletionListener(mSeekCompletionListener);
      mLogicManager.setAudioCompletionListener(mCompletionListener);
      mLogicManager.setAudioErrorListener(mErrorListener);
      mLogicManager.setAudioInfoListener(mInfoListener);
    } else {
      mLogicManager.setAudioPreparedListener(mtkPreparedListener);
      mLogicManager.setAudioSeekCompletionListener(mtkSeekCompletionListener);
      mLogicManager.setAudioCompletionListener(mtkCompletionListener);
      mLogicManager.setAudioErrorListener(mtkErrorListener);
      mLogicManager.setAudioInfoListener(mtkInfoListener);
    }

    mLogicManager.initAudio(this, mAudioSource);

    initVulume(mLogicManager);
    // isNotSupport = true;
    isNotSupport = false;
    isActivityLiving = true;
    // add by xudong fix cr DTV00385993
    retrunFromTipDismis = false;
    isCenterKey2Pause = false;
    // end

  }

  private void findView() {
    vLayout = (LinearLayout) findViewById(R.id.mmp_music_top);
    vThumbnail = (ImageView) findViewById(R.id.mmp_music_img);
    mScoreView = (ScoreView) findViewById(R.id.mmp_music_tv);
    mLrcView = (LrcView) findViewById(R.id.mmp_music_lrc);

    getPopView(R.layout.mmp_popupmusic, MultiMediaConstant.AUDIO,
        mControlImp);

    mControlView.setFilePosition(mLogicManager.getAudioPageSize());
    mControlView.hideOrder();
    // Delay load gamekit view.
    myHandler.sendEmptyMessageDelayed(LOAD_GAMEKIT_VIEW, DELAY_LOADGAMEKIT_MILLS);
  }

  // For load gamekit view.
  private void findGameKitView() {
    if (!mIsClose3D) {
      // mSurfaceView = (MtkGameKitView)findViewById(R.id.gl_musicbox_view);
      mEngineView = new GKView(this, "/system/resource/MusicBox.blend");
      LinearLayout mLinear = (LinearLayout) findViewById(R.id.gl_musicbox_view);
      LayoutParams p = new LayoutParams();
      p.width = 170;
      p.height = 360;
      mLinear.addView(mEngineView, p);
      MtkLog.d(TAG, "mSurfaceView register listener");
      mEngineView.setLoadListener(this);

      mEngineView.setMessageListener(mGkListener);
      // myHandler.sendEmptyMessageDelayed(RUN_LUA_CHUNK, DELAY_RUN_MILLIS);
      MtkLog.d("Ogre", mEngineView.getClass().toString());
    }
  }

  /**
   * {@inheritDoc} fix bug DTV00365251 by lei add.
   */
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    /*
     * if (mLogicManager.isAudioOnly()) { // by lei add for fix cr DTV00390970 if (event.getAction()
     * == KeyEvent.ACTION_UP) { mLogicManager.setAudioOnly(false); } return true; }
     */

    return super.dispatchKeyEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // if(0 == SystemProperties.getInt(DmrHelper.DMR_KEY,0)){
    // return true;
    // }

    textToSpeech(keyCode);

    switch (keyCode) {

      case KeyMap.KEYCODE_DPAD_CENTER:
      case KeyMap.KEYCODE_MTKIR_PLAYPAUSE:
        // add by shuming fix CR 00386020
        // if (mControlView.isPlaying()) {
        if (mLogicManager.isAudioPlaying()) {
          isCenterKey2Pause = true;
        } else {
          isCenterKey2Pause = false;
        }
        // end
        // setPlayerStop(false);
        if (isNotSupport) {
          return true;
        }
        // Added by yognzheng for fix CR DTV00390968 16/1/12
        if (mTipsDialog != null
            && mTipsDialog.isShowing()
            && mTipsDialog.getTitle().equals(
                getResources().getString(
                    R.string.mmp_file_notsupport))) {
          mTipsDialog.dismiss();
        }
        break;
      // Added by yognzheng for fix CR DTV00390968 16/1/12
      /*
       * case KeyMap.KEYCODE_MTKIR_CHDN: case KeyMap.KEYCODE_MTKIR_PREVIOUS: { if
       * (isValid()&&!DmrHelper.isDmr()) { dismissNotSupprot();
       * myHandler.removeMessages(NOSUPPORT_PLAYNEXT); // add by xiaojie fix cr DTV00379650
       * myHandler.removeMessages(CLEAR_LRC); myHandler.sendEmptyMessage(CLEAR_LRC); // end
       * mLogicManager.playPrevAudio(); myHandler.removeMessages(PROGRESS_START); luaStopAnim();
       * //by lei add for play 3D animal start } return true; } case KeyMap.KEYCODE_MTKIR_CHUP: case
       * KeyMap.KEYCODE_MTKIR_NEXT: { if (isValid()&&!DmrHelper.isDmr()) { dismissNotSupprot();
       * myHandler.removeMessages(NOSUPPORT_PLAYNEXT); // add by xiaojie fix cr DTV00379650
       * myHandler.removeMessages(CLEAR_LRC); myHandler.sendEmptyMessage(CLEAR_LRC); // end
       * mLogicManager.playNextAudio(); myHandler.removeMessages(PROGRESS_START); luaStopAnim();
       * //by lei add for play 3D animal start } return true; } case KeyMap.KEYCODE_DPAD_LEFT: case
       * KeyMap.KEYCODE_DPAD_RIGHT: { if (isNotSupport || mLogicManager.isAudioFast()) { return
       * true; } // add by xiaojie fix cr DTV00381177 // if (mLogicManager.isAudioPause()) { //
       * mScoreView.setVisibility(View.INVISIBLE); // } // end // add by xiaojie fix cr DTV00381234
       * String fileNotSupport = this.getResources().getString( R.string.mmp_file_notsupport); if
       * (mTipsDialog != null && mTipsDialog.isShowing() &&
       * mTipsDialog.getTitle().equals(fileNotSupport)) { mTipsDialog.dismiss(); // add by xudong
       * fix cr DTV00385993 retrunFromTipDismis = true; // end return true; } // end // add by
       * xudong fix cr DTV00385993 retrunFromTipDismis = false; // end return seek(keyCode, event);
       * } case KeyMap.KEYCODE_MTKIR_REWIND: { if (mLogicManager.isAudioStoped()) { return true; }
       * if (isValid() && mLogicManager.getAudioStatus() < AudioConst.PLAY_STATUS_STOPPED) { if
       * (mLogicManager.getAudioStatus() < AudioConst.PLAY_STATUS_STARTED) {
       * featureNotWork(getString(R.string.mmp_featue_notsupport)); return true; } try {
       * mLogicManager.fastRewindAudio(); setFast(1); }catch(IllegalStateException e){ MtkLog.d(TAG,
       * "Exception" + e.getMessage()); featureNotWork(getString(R.string.mmp_featue_notsupport)); }
       * catch (Exception e) { MtkLog.d(TAG, "Exception" + e.getMessage());
       * featureNotWork(getString(R.string.mmp_featue_notsupport)); } } return true; } case
       * KeyMap.KEYCODE_MTKIR_FASTFORWARD: { MtkLog.i(TAG, "KEYCODE_MTKIR_FASTFORWARD"); if
       * (mLogicManager.isAudioStoped()) { MtkLog.i(TAG,
       * "KEYCODE_MTKIR_FASTFORWARD isAudioStoped() == TRUE"); return true; } if (isValid() &&
       * mLogicManager.getAudioStatus() < AudioConst.PLAY_STATUS_STOPPED) { MtkLog.i(TAG,
       * "KEYCODE_MTKIR_FASTFORWARD isValid() && mLogicManager.getAudioStatus()
       * < AudioConst.PLAY_STATUS_STOPPED"
       * ); if (mLogicManager.getAudioStatus() < AudioConst.PLAY_STATUS_STARTED) { MtkLog.i(TAG,
       * "KEYCODE_MTKIR_FASTFORWARD mLogicManager.getAudioStatus()
       * <  AudioConst.PLAY_STATUS_STARTED"
       * ); featureNotWork(getString(R.string.mmp_featue_notsupport)); return true; } try {
       * mLogicManager.fastForwardAudio(); MtkLog.i(TAG,
       * "KEYCODE_MTKIR_FASTFORWARD fastForwardAudio"); setFast(0); }catch(IllegalStateException e){
       * MtkLog.d(TAG, "Exception" + e.getMessage());
       * featureNotWork(getString(R.string.mmp_featue_notsupport)); } catch (Exception e) {
       * MtkLog.d(TAG, "Exception" + e.getMessage());
       * featureNotWork(getString(R.string.mmp_featue_notsupport)); } } return true; } case
       * KeyMap.KEYCODE_MTKIR_STOP: { if (isNotSupport) { return true; } // Added by yognzheng for
       * fix CR DTV00390968 16/1/12 if (mTipsDialog != null && mTipsDialog.isShowing() &&
       * mTipsDialog.getTitle().equals( getResources().getString( R.string.mmp_file_notsupport))) {
       * mTipsDialog.dismiss(); } mLogicManager.stopAudio(); stop(); return true; }
       */
      case KeyMap.KEYCODE_BACK: {
        removeControlView();
        finish();
        break;
      }
      default:
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }

    private void textToSpeech(int keyCode) {
        String textString = null;

        switch (keyCode) {
            case KeyMap.KEYCODE_DPAD_CENTER:
                if (null != mControlView && mControlView.isPlaying()){
                    textString = "pause";
                } else {
                    textString = "play";
                }
                break;
        }

        if (null != textString && null != MediaMainActivity.getInstance()){
            MtkLog.d(TAG,"musicPlayDmrActivity,textToSpeech,textString=="+textString);
            MediaMainActivity.getInstance().getTTSUtil().speak(textString);
        }
  }

  public void stop() {

    removeMessages();
    // Add by yongzheng for fix CR DTV00379673
    // setPlayerStop(true);
    if (null != mLrcView && null != lrc_map && lrc_map.size() > 0) {
      mLrcView.noLrc(null);
    }
    // end
    if (null != mControlView) {
      mControlView.setCurrentTime(0);
      mControlView.setProgress(0);
      mControlView.stop();
    }
    if (null != mScoreView) {
      mScoreView.setVisibility(View.INVISIBLE);
    }

  }

  @Override
  public void finish() {
    super.finish();
  }

  private void removeMessages() {
    myHandler.removeMessages(PROGRESS_START);
    myHandler.removeMessages(PROGRESS_CHANGED);
    myHandler.removeMessages(PROGRESS_SCOREVIEW);
    myHandler.removeMessages(AUDIO_CHANGED);
    myHandler.removeMessages(SPEED_UPDATE);
    myHandler.removeMessages(NOSUPPORT_PLAYNEXT);
  }

  private void removeLoadGamekitMessage() {
    MtkLog.e(TAG, "removeLoadGamekitMessage()");
    myHandler.removeMessages(LOAD_GAMEKIT_VIEW);
    // myHandler.removeMessages(LOAD_GAMEKIT_RESUME);
  }

  private void setFast(int isForward) {

    int speed = mLogicManager.getAudioSpeed();
    if (speed == 0) {
      return;
    }

    if (null == mControlView) {
      return;
    }
    // hideFeatureNotWork();

    if (!myHandler.hasMessages(PROGRESS_CHANGED)) {
      myHandler.sendEmptyMessage(PROGRESS_CHANGED);
    }
    if (!myHandler.hasMessages(PROGRESS_SCOREVIEW)) {
      myHandler.sendEmptyMessage(PROGRESS_SCOREVIEW);

    }
    if (!myHandler.hasMessages(PROGRESS_CHANGED)) {
      myHandler.sendEmptyMessage(PROGRESS_CHANGED);
    }
    mControlView.onFast(speed, isForward, Const.FILTER_AUDIO);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {

    if (keyCode == KeyMap.KEYCODE_DPAD_LEFT
        || keyCode == KeyMap.KEYCODE_DPAD_RIGHT) {

      if (mLogicManager.isAudioFast()) {
        return true;
      }
      // add by keke 2.1 for DTV00393701
      if (mLogicManager.getPlayStatus() == AudioConst.PLAY_STATUS_PAUSED) {
        removeScorePause();
      }
      MtkLog.i(TAG, "onkeyup");
      // add "&& !retrunFromTipDismis" by xudong fix cr DTV00385993
      if (playFlag && !retrunFromTipDismis) {
        if (null != mControlView) {
          mControlView.setMediaPlayState();
        }
      }
    }

    return super.onKeyUp(keyCode, event);
  }

  private boolean seek(int keyCode, KeyEvent event) {
    if (null == mControlView) {
      return true;
    }

    // if (mControlView.isPlaying()) {
    if (mControlView.isPlaying()) {
      playFlag = true;
      mControlView.setMediaPlayState();
      /*
       * if(mControlView.isPlaying()){ playFlag=false; return true; }
       */
    } else if (event.getRepeatCount() == 0) {
      playFlag = false;
    }

    if (!mLogicManager.canSeek()) {
      MtkLog.i(TAG, "!mLogicManager.canSeek()");
      featureNotWork(getString(R.string.mmp_featue_notsupport));
      return true;
    }

    int progressTmp = mLogicManager.getPlaybackProgress();
    long progress = progressTmp & 0xffffffffL;
    if (progress < 0) {
      return true;
    }

    if (keyCode == KeyMap.KEYCODE_DPAD_LEFT) {
      progress = progress - SEEK_DURATION;
      if (progress < 0) {
        progress = 0;
      }

    } else {
      progress = progress + SEEK_DURATION;

      int totalProgressTmp = mLogicManager.getTotalPlaybackTime();
      long totalProgress = totalProgressTmp & 0xffffffffL;
      if (progress > totalProgress) {
        progress = totalProgress;
      }
    }
    try {
      mLogicManager.seekToCertainTime(progress);
      // mControlView.setCurrentTime(progress);
      // mControlView.setProgress((int) progress);
    } catch (Exception e) {
      MtkLog.i(TAG, "Seek exception");
      featureNotWork(getString(R.string.mmp_featue_notsupport));
      return true;
    }

    // Added by yongzheng for fix CR DTV00379673
    if (getPlayerStop()) {
      removeMessages();
      return true;
    }
    // end
    // modified by keke for fix DTV00381199
    if (hasLrc()) {
      myHandler.sendEmptyMessage(PROGRESS_START);
    }
    return true;

  }

  @Override
  public boolean hasLrc() {
    if (null == lrc_map || (lrc_map.size() == 0) || null == mLrcView) {
      return false;
    }

    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    Util.LogLife(TAG, "onResume");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onPause() {
    super.onPause();
    // if (!mIsClose3D && mSurfaceView != null) mSurfaceView.onPause();
    MtkLog.e(TAG, "<----onPause----------------->");
    removeMessages();
    removeLoadGamekitMessage();
    Util.LogLife(TAG, "onPause");

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onDestroy() {
    luaStopAnim();
    removeMessages();
    isActivityLiving = false;
    if (mLogicManager.getAudioPlaybackService() != null) {
      mLogicManager.unbindService(this);
    }
    SystemProperties.set(ShowInfoView.PROPERTIES, String.valueOf(0));
    DmrHelper.handleStop();
    LogicManager.getInstance(getApplicationContext()).stopAudio();
    super.onDestroy();
    Util.LogLife(TAG, "onDestroy");
  }

  public void clearLrc() {
    if (mLrcView != null && mLrcView.getVisibility() == View.VISIBLE
        && null != lrc_map && lrc_map.size() > 0) {
      mLrcView.noLrc("");
    }
  }

  /**
   * Audio wheather stop
   */
  // Added by yongzheng for fix CR DTV00379673 and DTV00388521
  // private boolean isMusicStop = false;

  @Override
  protected boolean getPlayerStop() {
    // return false;
    return mLogicManager.getAudioStatus() == AudioConst.PLAY_STATUS_STOPPED;
  }

  @Override
  public void onLoaded() {
    // TODO Auto-generated method stub
    luaPlayAnim();
  }

  @Override
  protected void handleDmrPlayPause(int state) {
    super.handleDmrPlayPause(state);
  }

  @Override
  protected void handleDmrStop() {
    /*
     * if (isNotSupport) { return ; } // Added by yognzheng for fix CR DTV00390968 16/1/12 if
     * (mTipsDialog != null && mTipsDialog.isShowing() && mTipsDialog.getTitle().equals(
     * getResources().getString( R.string.mmp_file_notsupport))) { mTipsDialog.dismiss(); }
     * mLogicManager.stopAudio(); stop(); super.handleDmrStop();
     */
    this.finish();
  }

  @Override
  public long getCurrentTime() {
    long progress = LogicManager.getInstance(getApplicationContext()).getPlaybackProgress();
    if (progress < 0) {
      progress = 0;
    }
    return progress;
  }

  @Override
  public long getDurationTime() {
    int durTmp = LogicManager.getInstance(getApplicationContext()).getTotalPlaybackTime();
    long dur = durTmp & 0xffffffffL;
    if (dur < 0) {
      dur = 0;
    }
    return dur;
  }

  @Override
  public void handleDmrSeek(int progressTmp) {
    int totalProgressTmp = mLogicManager.getTotalPlaybackTime();
    long totalProgress = totalProgressTmp & 0xffffffffL;
    totalProgress = totalProgress / 1000;
    long progress = progressTmp & 0xffffffffL;
    Log.i(TAG, "progress:" + progress + "  total:" + totalProgress);
    if (progress > totalProgress) {
      progress = totalProgress;
    }
    if (progress < 0) {
      progress = 0;
    }

    try {
      mLogicManager.seekToCertainTime(progress * 1000);
      // mControlView.setCurrentTime(progress);
      // mControlView.setProgress((int) progress);
    } catch (Exception e) {
      MtkLog.i(TAG, "Seek exception");
      featureNotWork(getString(R.string.mmp_featue_notsupport));
      DmrHelper.tellDmcState(getApplicationContext(), 0);
    }
  }

  @Override
  protected void handleDmrSetVolume(int param) {
    super.handleDmrSetVolume(param);
  }

  @Override
  public void handleDmrSetMute(int param) {
    super.handleDmrSetMute(param);

  }

  @Override
  public void handleRootMenuEvent() {
    // TODO Auto-generated method stub
    super.handleRootMenuEvent();
    if (mLogicManager != null) {
      mLogicManager.stopAudio();
    }
  }
}
