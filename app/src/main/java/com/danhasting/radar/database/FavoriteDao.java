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

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Query("SELECT * FROM favorite")
    List<Favorite> getAll();

    @Query("SELECT * FROM favorite WHERE uid = :uid LIMIT 1")
    Favorite loadById(int uid);

    @Query("SELECT * FROM favorite WHERE location LIKE :location AND type LIKE :type AND " +
            "loop = :loop AND enhanced = :enhanced AND distance = :distance AND source = :source")
    List<Favorite> findByData(int source, String location, String type, Boolean loop,
                              Boolean enhanced, Integer distance);

    @Query("SELECT * FROM favorite WHERE name LIKE :name LIMIT 1")
    Favorite findByName(String name);

    @Insert
    void insertAll(Favorite... favorites);

    @Update
    void updateFavorites(Favorite... favorites);

    @Delete
    void delete(Favorite favorite);
}
