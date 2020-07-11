/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.Map;

import static com.google.android.exoplayer2.C.WIDEVINE_UUID;


/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity {

  private PlayerView playerView;
  private SimpleExoPlayer player;
  private boolean playWhenReady = true;
  private int currentWindow = 0;
  private long playbackPosition = 0;

  
  private String TAG = "PlayerActivity";
  private EventLogger eventLogger;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);
    playerView = findViewById(R.id.video_view);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT >= 24) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    hideSystemUi();
    if ((Util.SDK_INT < 24 || player == null)) {
      initializePlayer();
    }
  }

  @SuppressLint("InlinedApi")
  private void hideSystemUi() {
    playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT < 24) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT >= 24) {
      releasePlayer();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void initializePlayer() {
    if (player == null) {
      DefaultTrackSelector trackSelector = new DefaultTrackSelector();
      trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd());
      player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
      player.addAnalyticsListener(new EventLogger(trackSelector));
    }
//    player = ExoPlayerFactory.newSimpleInstance(this);
    playerView.setPlayer(player);

//    Uri uri = Uri.parse(getString(R.string.media_url_dash_mpd));
    Uri uri = Uri.fromFile(new File(NetflixManifestGenerator.getDashManifestPath()));
//    DashManifestParser manifestParser = new DashManifestParser();
//    manifestParser.
    MediaSource mediaSource = buildMediaSource(uri);

    player.setPlayWhenReady(playWhenReady);
    player.seekTo(currentWindow, playbackPosition);
    player.prepare(mediaSource, false, false);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private MediaSource buildMediaSource(Uri uri) {
//    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-netflix");
//    return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);

//    DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("ua");
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "ua");

//    DashMediaSource.Factory dashMediaSourceFactory = new DashMediaSource.Factory(
//            new DefaultDashChunkSource.Factory(dataSourceFactory),
//            manifestDataSourceFactory);
    DrmSessionManager<ExoMediaCrypto> drmSessionManager =
            new DefaultDrmSessionManager.Builder()
            .setMultiSession(true)
            .setUuidAndExoMediaDrmProvider(WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(new NflxWidevineDrmCallback());

    MediaSource mediaSource = new DashMediaSource.Factory(dataSourceFactory)
            .setDrmSessionManager(drmSessionManager)
            .createMediaSource(uri);
//    MediaSource mediaSource = new DashMediaSource.Factory(dataSourceFactory)
////            .setDrmSessionManager(drmSessionManager)
//            .createMediaSource(uri);

//    // Produces DataSource instances through which media data is loaded.
//    DataSource.Factory manifestDataSourceFactory = new DefaultDataSourceFactory(this, "ua");
//    // Produces Extractor instances for parsing the media data.
//    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
//
//    // This is the MediaSource representing the media to be played.
//    MediaSource createMediaSource = new ExtractorMediaSource(Uri.parse(path),
//            dataSourceFactory, extractorsFactory, null, null);
    return mediaSource;
  }

  private void releasePlayer() {
    if (player != null) {
      playWhenReady = player.getPlayWhenReady();
      playbackPosition = player.getCurrentPosition();
      currentWindow = player.getCurrentWindowIndex();
      player.release();
      player = null;
    }
  }
}
