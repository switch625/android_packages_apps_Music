/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.music;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class MediaAppWidgetProvider4 extends AppWidgetProvider {
    static final String TAG = "MusicAppWidgetProvider4";
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate4";
    
    static final ComponentName THIS_APPWIDGET =
        new ComponentName("com.android.music",
                "com.android.music.MediaAppWidgetProvider4");
    
    private static MediaAppWidgetProvider4 sInstance;
    
    static synchronized MediaAppWidgetProvider4 getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider4();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(MediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(MediaPlaybackService.CMDNAME,
                MediaAppWidgetProvider4.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    public static Bitmap getReflection(Bitmap bitmap){
 
        int bmwidth = bitmap.getWidth();
        int bmheight = bitmap.getHeight();

    Matrix matrix = new Matrix();
    matrix.preScale(1, -1);
 
    Bitmap reflect = Bitmap.createBitmap(bitmap, 0, bmheight/2, bmwidth, bmheight/2, matrix, false);
 
    Bitmap Reflection = Bitmap.createBitmap(bmwidth
        , (bmheight/2), Config.ARGB_8888);
 
    Canvas canvas = new Canvas(Reflection);
    canvas.drawBitmap(reflect, 0, 0, null);
 
    Paint paint = new Paint();
    LinearGradient shader = new LinearGradient(0, 0, 0,
    Reflection.getHeight(), 0xcfffffff, 0x00ffffff,
        TileMode.CLAMP);
    paint.setShader(shader);
    paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
    canvas.drawRect(0, 0, bmwidth, Reflection.getHeight(), paint);

    return Reflection;

    }

    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.album_appwidget4x4);
        
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.emptyplaylist));
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap noart =  BitmapFactory.decodeStream(
                context.getResources().openRawResource(R.drawable.albumart_mp_unknown), null, opts);
	Bitmap reflectnoart = getReflection(noart);
        views.setImageViewBitmap(R.id.albumart, noart);
	views.setImageViewBitmap(R.id.albumartreflect, reflectnoart);


        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(MediaPlaybackService service, String what) {
        if (hasInstances(service)) {
            if (MediaPlaybackService.PLAYBACK_COMPLETE.equals(what) ||
                    MediaPlaybackService.META_CHANGED.equals(what) ||
                    MediaPlaybackService.REPEAT_CHANGED.equals(what) ||
                    MediaPlaybackService.SHUFFLE_CHANGED.equals(what) ||
                    MediaPlaybackService.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
                
            } else if (MediaPlaybackService.PROGRESSBAR_CHANGED.equals(what)) {
            	progRunner(service, null);
                    
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes to progress bar only 
     */    
    void progRunner(MediaPlaybackService service, int[] appWidgetIds) {

        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget4x4);
	long pos = service.position();
	long dur = service.duration();

            views.setProgressBar(R.id.progress, 1000, (int) (1000 * pos / dur), false);
            
        pushUpdate(service, appWidgetIds, views);
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(MediaPlaybackService service, int[] appWidgetIds) {
        final Resources res = service.getResources();

        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget4x4);
        
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
	CharSequence albumName = service.getAlbumName();
	long albumId = service.getAlbumId();
	long songId = service.getAudioId();
	long pos = service.position();
	long dur = service.duration();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap noart =  BitmapFactory.decodeStream(
                service.getResources().openRawResource(R.drawable.albumart_mp_unknown), null, opts);
	Bitmap art = MusicUtils.getArtwork(service, songId, albumId);
        Bitmap reflectart = getReflection(art);
        Bitmap reflectnoart = getReflection(noart);
        CharSequence errorState = null;
        
        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
                status.equals(Environment.MEDIA_UNMOUNTED)) {
            errorState = res.getText(R.string.sdcard_busy_title);
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            errorState = res.getText(R.string.sdcard_missing_title);
        } else if (titleName == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }
        
        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setViewVisibility(R.id.albumname, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
            views.setImageViewBitmap(R.id.albumart, noart);
	    views.setImageViewBitmap(R.id.albumartreflect, reflectnoart);
            
        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.albumname, albumName);
            views.setTextViewText(R.id.artist, artistName);
            views.setProgressBar(R.id.progress, 1000, (int) (1000 * pos / dur), false);
	    if (art == null) {
		views.setImageViewBitmap(R.id.albumart, noart);
		views.setImageViewBitmap(R.id.albumartreflect, reflectnoart);
	} else {
		views.setImageViewBitmap(R.id.albumart, art);
		views.setImageViewBitmap(R.id.albumartreflect, reflectart);
          	}
	}
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.pause_button);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.play_button);
        }

	final int shuffle_mode = service.getShuffleMode();
        if (shuffle_mode == MediaPlaybackService.SHUFFLE_NONE) {
            views.setImageViewResource(R.id.shuffle, R.drawable.shuffle_off);
        } else if (shuffle_mode == MediaPlaybackService.SHUFFLE_AUTO) {
            views.setImageViewResource(R.id.shuffle, R.drawable.party_shuffle);
        } else {
            views.setImageViewResource(R.id.shuffle, R.drawable.shuffle_on);
	}

	final int repeat_mode = service.getRepeatMode();
        if (repeat_mode == MediaPlaybackService.REPEAT_ALL) {
            views.setImageViewResource(R.id.repeat, R.drawable.repeat_all);
        } else if (repeat_mode == MediaPlaybackService.REPEAT_CURRENT) {
            views.setImageViewResource(R.id.repeat, R.drawable.repeat_once);
        } else {
            views.setImageViewResource(R.id.repeat, R.drawable.repeat_off);
	}

        // Link actions buttons to intents
        linkButtons(service, views, playing);
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivityStarter},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, MediaPlaybackService.class);
        
        if (playerActive) {
            intent = new Intent(context, MediaPlaybackActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        } else {
            intent = new Intent(context, MusicBrowserActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        }
        
        intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        intent = new Intent(MediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);

        intent = new Intent(MediaPlaybackService.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);

        intent = new Intent(MediaPlaybackService.SHUFFLE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.shuffle, pendingIntent);

        intent = new Intent(MediaPlaybackService.REPEAT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.repeat, pendingIntent);

    }
}
