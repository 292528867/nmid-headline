package cn.edu.cqupt.nmid.headline.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import cn.edu.cqupt.nmid.headline.R;
import cn.edu.cqupt.nmid.headline.support.Constant;
import cn.edu.cqupt.nmid.headline.support.api.headline.HeadlineService;
import cn.edu.cqupt.nmid.headline.support.db.DataBaseHelper;
import cn.edu.cqupt.nmid.headline.support.pref.ThemePref;
import cn.edu.cqupt.nmid.headline.support.pref.WebViewPref;
import cn.edu.cqupt.nmid.headline.support.task.WebContentGetTask;
import cn.edu.cqupt.nmid.headline.support.task.callback.WebContentGetTaskCallback;
import cn.edu.cqupt.nmid.headline.ui.widget.FloatingActionsMenuHidable;
import cn.edu.cqupt.nmid.headline.ui.widget.ProgressBarCircular;
import cn.edu.cqupt.nmid.headline.ui.widget.observerWebView.ObservableScrollViewCallbacks;
import cn.edu.cqupt.nmid.headline.ui.widget.observerWebView.ObservableWebView;
import cn.edu.cqupt.nmid.headline.ui.widget.observerWebView.ScrollState;
import cn.edu.cqupt.nmid.headline.ui.widget.swipebacklayout.SwipeBackActivity;
import cn.edu.cqupt.nmid.headline.utils.LogUtils;
import cn.edu.cqupt.nmid.headline.utils.NetworkUtils;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

/**
 * Useful @Link:http://developer.android.com/training/animation/crossfade.html
 */
public class DetailedActivity extends SwipeBackActivity {

  static final String MIME_TYPE = "text/html";
  static final String ENCODING = "utf-8";
  String url;
  /**
   * Activity views
   */

  @InjectView(R.id.detailed_webview) ObservableWebView mWebView;
  @InjectView(R.id.detailed_multiple_actions) FloatingActionsMenuHidable mFloatingActionsMenu;
  @InjectView(R.id.detailed_progressbar) ProgressBarCircular mProgressBarCircular;
  private String TAG = LogUtils.makeLogTag(DetailedActivity.class);
  /**
   * Intent extra uesd for ShareSDK
   */
  private int id;
  private int category;
  private String title;
  private String excerpt;
  private boolean isFabAnmation = false;

  @OnClick(R.id.detailed_action_share) void detailed_action_share() {
    mFloatingActionsMenu.toggle();
    OnekeyShare oks;
    ShareSDK.initSDK(this);
    oks = new OnekeyShare();
    // 分享时Notification的图标和文字
    oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
    // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
    oks.setTitle(title);
    // text是分享文本，所有平台都需要这个字段
    oks.setText(excerpt);
    // comment是我对这条分享的评论，仅在人人网和QQ空间使用
    oks.setComment("我在通信头条分享了文章");
    // site是分享此内容的网站名称，仅在QQ空间使用
    oks.setSite(getString(R.string.app_name));
    //TODO
    // url仅在微信（包括好友和朋友圈）中使用
    oks.setUrl(url);
    // siteUrl是分享此内容的网站地址，仅在QQ空间使用
    oks.setSiteUrl(url);
    // titleUrl是标题的网络链接，仅在人人网和QQ空间使用
    oks.setTitleUrl(url);
    oks.show(this);
  }

  @OnClick(R.id.detailed_action_favorite) void detailed_action_favorite(View v) {
    //mController.likeIt(id, category, true);
    DataBaseHelper.getInstance(this).setCollect(id,category,true);
  }

  @OnClick(R.id.detailed_action_settings) void detailed_action_settings() {
    startActivity(new Intent(this, SettingsActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detailed);
    ButterKnife.inject(this);

    tryGetIntent();
    trySetupWebview();
  }

  private void tryGetIntent() {
    id = getIntent().getIntExtra("id", 1);
    category = getIntent().getIntExtra("category", Constant.TYPE_CLASSMATE);
    title = getIntent().getStringExtra("title");
    excerpt = getIntent().getStringExtra("excerpt");
  }

  private void trySetupWebview() {

    url = HeadlineService.END_POINT + "/api/android/newscontent?id=" + id + "&category=" + category;

    WebSettings settings = mWebView.getSettings();
    settings.setTextZoom(WebViewPref.getWebViewTextZoom(this));
    switch (WebViewPref.isAutoLoadImages(this)) {
      case 0:
        settings.setLoadsImagesAutomatically(false);
        break;
      case 1:
        break;
      case 2:
        if (!NetworkUtils.isWifiAviliable(this)) {
          settings.setLoadsImagesAutomatically(false);
        }
        break;
    }

    mWebView.setScrollViewCallbacks(new ObservableScrollViewCallbacks() {
      //正在滚动时，包括drag滚动和惯性滚动
      @Override
      public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {

      }

      //当按下touch
      @Override
      public void onDownMotionEvent() {
        ///Log.i(TAG, "onDownMotionEvent");

      }

      //当松开touch后，计算相对y
      @Override
      public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        switch (scrollState) {
          case STOP:
            Log.i(TAG, "stop");
            mFloatingActionsMenu.show(true);
            break;
          case UP:
            Log.i(TAG, "UP");
            mFloatingActionsMenu.show(false);
            break;
          case DOWN:
            Log.i(TAG, "DOWN");
            mFloatingActionsMenu.show(true);
        }
      }
    });

    new WebContentGetTask(this, new WebContentGetTaskCallback() {
      @Override
      public void onPreExcute() {
        mProgressBarCircular.setVisibility(View.VISIBLE);
      }

      @Override
      public void onSuccess(Object o) {
        Log.d(TAG, o.toString());
        mProgressBarCircular.setVisibility(View.GONE);
        String htmlData;
        if (ThemePref.isNightMode(DetailedActivity.this)) {
          // Webview will use asserts/style_night.css
          htmlData =
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style_night.css\" /> <body class= \"gloable\"> "
                  + o
                  + "</body>";
        } else {
          // Webview will use asserts/style.css
          htmlData =
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" /> <body class= \"gloable\"> "
                  + o
                  + "</body>";
        }

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlData, MIME_TYPE, ENCODING, null);
      }
    }).execute(url);
  }

  @Override
  public void onBackPressed() {
    if (mFloatingActionsMenu.isExpanded()) {
      mFloatingActionsMenu.collapse();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      mFloatingActionsMenu.toggle();
    }
    return super.onKeyUp(keyCode, event);
  }
}