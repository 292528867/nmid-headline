package cn.edu.cqupt.nmid.headline.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import cn.edu.cqupt.nmid.headline.R;
import cn.edu.cqupt.nmid.headline.support.api.headline.HeadlineService;
import cn.edu.cqupt.nmid.headline.support.api.image.ImageService;
import cn.edu.cqupt.nmid.headline.support.api.image.bean.ImageInfo;
import cn.edu.cqupt.nmid.headline.support.api.image.bean.ImageStream;
import cn.edu.cqupt.nmid.headline.support.pref.ThemePref;
import cn.edu.cqupt.nmid.headline.support.service.UploadService;
import cn.edu.cqupt.nmid.headline.ui.adapter.ImagesFeedAdapter;
import cn.edu.cqupt.nmid.headline.utils.FileUtils;
import cn.edu.cqupt.nmid.headline.utils.thirdparty.RetrofitUtils;
import cn.edu.cqupt.nmid.headline.utils.animation.SlideInOutBottomItemAnimator;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qzone.QZone;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import java.io.File;
import java.util.ArrayList;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static cn.edu.cqupt.nmid.headline.utils.LogUtils.makeLogTag;

public class ImagesFeedFragment extends Fragment {

  String TAG = makeLogTag(ImagesFeedFragment.class);
  public final static int REQUEST_IMAGE_CAPTURE = 1;
  Uri outputFileUri;
  AlertDialog dialog;

  /**
   * Injected Vies
   */
  @InjectView(R.id.feed_recyclerview) ObservableRecyclerView mRecyclerview;
  @InjectView(R.id.feed_swiperefreshlayout) SwipeRefreshLayout mSwipeRefreshLayout;
  //@InjectView(R.id.include_progressbar) RelativeLayout mIncludeProgressbar;
  @InjectView(R.id.stream_multiple_actions) FloatingActionButton mStreamMultipleActions;

  @OnClick(R.id.stream_multiple_actions) void stream_action_take_picture() {
    if (!ShareSDK.getPlatform(getActivity().getApplicationContext(), QZone.NAME).isValid()) {

      new AlertDialog.Builder(getActivity()).setTitle("Login Required")
          .setMessage("请先登录账号，这样才可以上传哦！").setPositiveButton("OK",
          new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {

            }
          }).create().show();

    } else {
      tryUploadImage();
    }
  }

  private void tryUploadImage() {
    File camaraCacheFile = FileUtils.createCachedFile();
    outputFileUri = Uri.fromFile(camaraCacheFile);
    Intent intent = new Intent(
        MediaStore.ACTION_IMAGE_CAPTURE);

    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(camaraCacheFile));
    startActivity(intent);
  }

  ImagesFeedAdapter adapter;
  LinearLayoutManager mLayoutManager;
  ArrayList<ImageInfo> images = new ArrayList<>();
  private int lastid = 0;

  private boolean isLoadingMore = false;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_stream, container, false);
    ButterKnife.inject(this, view);

    //mIncludeProgressbar.setVisibility(View.GONE);
    adapter = new ImagesFeedAdapter(images);
    mLayoutManager = new LinearLayoutManager(getActivity());

    mRecyclerview.setBackgroundResource(ThemePref.getBackgroundResColor(getActivity()));
    mStreamMultipleActions.setColorNormalResId(
        ThemePref.getToolbarBackgroundResColor(getActivity()));
    mSwipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN);
    mRecyclerview.setAdapter(adapter);
    mRecyclerview.setHasFixedSize(true);
    mRecyclerview.setLayoutManager(mLayoutManager);
    mRecyclerview.setItemAnimator(new SlideInOutBottomItemAnimator(mRecyclerview));
    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {

        loadNewFeeds();
        mRecyclerview.smoothScrollToPosition(0);
      }
    });
    //Endless RecyclerView
    mRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        int visibleItems = mLayoutManager.findLastVisibleItemPosition();
        int lastVisibleItems = adapter.getItemCount();
        if (dy > 0 && visibleItems == lastVisibleItems - 1) {
          loadOldImage();
        }
      }
    });

    loadNewFeeds();
    return view;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  void loadNewFeeds() {
    if (isLoadingMore) {
      Log.d(TAG, "ignore manually update");
      return;
    }
    isLoadingMore = true;
    RetrofitUtils.getCachedAdapter(HeadlineService.END_POINT)
        .create(ImageService.class)
        .getRefreshImage(0, 4, new Callback<ImageStream>() {
          @Override public void success(ImageStream imageStream, Response response) {
            isLoadingMore = false;
            mSwipeRefreshLayout.setRefreshing(false);
            images.clear();
            //bug fix when server return null
            if (imageStream.getStatus() == 1) {
              images.addAll(imageStream.getData());
              adapter.notifyDataSetChanged();
            }
          }

          @Override public void failure(RetrofitError error) {
            mSwipeRefreshLayout.setRefreshing(false);
            isLoadingMore = false;
          }
        });
  }

  void loadOldImage() {

    if (isLoadingMore) {
      Log.d(TAG, "ignore manually update");
      return;
    }
    isLoadingMore = true;
    lastid = images.get(images.size() - 1).getIdmember();
    RetrofitUtils.getCachedAdapter(HeadlineService.END_POINT)
        .create(ImageService.class)
        .getROldImage(lastid, 4, new Callback<ImageStream>() {
          @Override public void success(ImageStream imageStream, Response response) {
            isLoadingMore = false;
            //bug fix when server return null
            if (imageStream.getStatus() == 1) {
              images.addAll(imageStream.getData());
              adapter.notifyDataSetChanged();
            }
          }

          @Override public void failure(RetrofitError error) {
            isLoadingMore = false;
          }
        });
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != Activity.RESULT_OK) {
      Log.d(TAG, "canceled or other exception!");
      return;
    }

    if (requestCode == REQUEST_IMAGE_CAPTURE) {
      Log.d(TAG, "onActivityResult: REQUEST_IMAGE_CAPTURE");
      mRecyclerview.smoothScrollToPosition(0);
      tryUploadFromUri(getActivity(), outputFileUri);
    }
  }

  private void tryUploadFromUri(Context context, Uri mImageUri) {
    Intent intent = new Intent(context.getApplicationContext(), UploadService.class);
    intent.putExtra(UploadService.Key, mImageUri);
    getActivity().startService(intent);
  }
}