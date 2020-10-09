/*
 * Copyright (c) 2018, Dan Hasting
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
package com.danhasting.radar.database;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import java.util.List;

public class FavoriteViewModel extends AndroidViewModel {

    private final LiveData<List<Favorite>> favorites;


    public FavoriteViewModel(@NonNull Application application) {
        super(application);

        favorites = AppDatabase.getAppDatabase(this.getApplication()).favoriteDao().getAll();
    }

    public LiveData<List<Favorite>> getFavorites() {
        return favorites;
    }
}
