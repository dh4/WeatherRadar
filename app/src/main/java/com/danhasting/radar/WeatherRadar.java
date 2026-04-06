/*
 * Copyright (c) 2026, Dan Hasting
 *
 * This file is part of WeatherRadar
 *
 * WeatherRadar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WeatherRadar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WeatherRadar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.danhasting.radar;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danhasting.radar.database.Source;

import java.lang.ref.WeakReference;


public class WeatherRadar extends Application {
    private static WeakReference<Activity> currentActivity;
    private static Source currentSource;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = new WeakReference<>(activity);
            }
            @Override public void onActivityPaused(@NonNull Activity activity) {
                Activity current = currentActivity == null ? null : currentActivity.get();
                if (current == activity) currentActivity = null;
            }

            public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {}
            public void onActivityStarted(@NonNull Activity activity) {}
            public void onActivityStopped(@NonNull Activity activity) {}
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    public static @Nullable Activity getCurrentActivity() {
        return currentActivity == null ? null : currentActivity.get();
    }

    public static void setCurrentSource(Source source) {
        currentSource = source;
    }

    public static Source getCurrentSource() {
        return currentSource;
    }
}
