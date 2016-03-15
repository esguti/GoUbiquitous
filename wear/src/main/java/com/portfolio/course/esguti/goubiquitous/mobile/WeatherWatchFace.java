/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.portfolio.course.esguti.goubiquitous.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.portfolio.course.esguti.goubiquitous.WeatherWatchConstants;
import com.portfolio.course.esguti.goubiquitous.WeatherWatchFaceConstants;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class WeatherWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }



    private class Engine extends CanvasWatchFaceService.Engine
            implements GoogleApiClient.ConnectionCallbacks  {

        private final String LOG_TAG = Engine.class.getSimpleName();

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;

        boolean mAmbient;
        Time mTime;
        int mTapCount;

        float mXOffset;
        float mYOffset;

        private GoogleApiClient m_apiClient;
        private Paint  m_TempPaint;
        private Bitmap m_WeatherIcon;
        private String m_TempHigh = "?";
        private String m_TempLow = "?";
        private String m_WeatherCond = WeatherWatchFaceConstants.KEY_WEATHER_NA;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;


        public class MessageReceiver extends BroadcastReceiver{
            @Override
            public void onReceive(Context context, Intent intent) {
                String path = intent.getStringExtra(getString(R.string.message_path));
                String message = intent.getStringExtra(getString(R.string.message_data));

                Log.d(LOG_TAG, "Intent Received." + " P:" + path + " M:" + message);

                if (path.equals(WeatherWatchConstants.MSG_HIGH_TEMP)) {
                    m_TempHigh = message;
                } else {
                    if (path.equals(WeatherWatchConstants.MSG_LOW_TEMP)) {
                        m_TempLow = message;
                    } else {
                        if (path.equals(WeatherWatchConstants.MSG_COND_WEATHER)) {
                            m_WeatherCond = message;
                        }else{
                            Log.w(LOG_TAG, "Message not recognized");
                        }
                    }
                }
            }
        };

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(LOG_TAG, "Connected Device");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(LOG_TAG, "Connection Suspended");
        }

        private void initGoogleApiClient(Context context) {
            m_apiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();

            if( m_apiClient != null && !( m_apiClient.isConnected() || m_apiClient.isConnecting() ) ) {
                Log.d(LOG_TAG, "Connected to API");
                m_apiClient.connect();
            }else{
                Log.e(LOG_TAG, "Problems connection to API");
            }
        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = WeatherWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

            MessageReceiver messageReceiver = new MessageReceiver();
            IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(messageReceiver, messageFilter);

            initGoogleApiClient(getApplicationContext());

            m_TempPaint = new Paint();
            m_TempPaint.setARGB(255, 255, 255, 255);
            m_TempPaint.setStrokeWidth(15.0f);
            m_TempPaint.setAntiAlias(true);
            m_TempPaint.setTextSize(35f);
            m_TempPaint.setStrokeCap(Paint.Cap.SQUARE);
            m_TempPaint.setTextAlign(Paint.Align.RIGHT);

            m_WeatherIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_clear);

            Log.d(LOG_TAG, "Created App");

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = WeatherWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = WeatherWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {


            final float center_x = (float) canvas.getWidth() / (float) 1.6;
            final int canvas_height = canvas.getHeight();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);


            //Draw weather
            float temp_y = canvas_height * 0.75f;
            float temp_y_high = canvas_height * 0.60f;
            float temp_y_low = canvas_height * 0.75f;
            float spaceLength = m_TempPaint.measureText(" ");
            String textLow  = "Low: "  + m_TempLow   + "° ";
            String textHigh = "High: " + m_TempHigh  + "° ";

            if( m_WeatherCond != WeatherWatchFaceConstants.KEY_WEATHER_NA ) {
                m_WeatherIcon = BitmapFactory.decodeResource(getResources(), getWearableIconResource(m_WeatherCond));
                canvas.drawBitmap(m_WeatherIcon, center_x + spaceLength, temp_y - m_WeatherIcon.getHeight(), m_TempPaint);
            }
            canvas.drawText(textHigh, center_x, temp_y_high, m_TempPaint);
            canvas.drawText(textLow , center_x, temp_y_low, m_TempPaint);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private int getWearableIconResource(String weatherId) {

            // Based on weather code data found at:
            // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
            if ( WeatherWatchFaceConstants.KEY_WEATHER_STORM.equals(weatherId) ){ return R.mipmap.ic_storm;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_LIGHT_RAIN.equals(weatherId) ){ return R.mipmap.ic_light_rain;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_RAIN.equals(weatherId) ){ return R.mipmap.ic_rain;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_SNOW.equals(weatherId) ){ return R.mipmap.ic_snow;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_FOG.equals(weatherId) ){ return R.mipmap.ic_fog;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_CLEAR.equals(weatherId) ){ return R.mipmap.ic_clear;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_LIGHT_CLOUDS.equals(weatherId) ){ return R.mipmap.ic_light_clouds;
            } else if ( WeatherWatchFaceConstants.KEY_WEATHER_CLOUDY.equals(weatherId) ){ return R.mipmap.ic_cloudy;
            }else {
                return R.mipmap.ic_launcher;
            }
        }

    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WeatherWatchFace.Engine> mWeakReference;

        public EngineHandler(WeatherWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WeatherWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
