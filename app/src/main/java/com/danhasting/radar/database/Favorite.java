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

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class Favorite {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "name")
    @NonNull
    private String name = "";

    @ColumnInfo(name = "location")
    @NonNull
    private String location = "";

    @ColumnInfo(name = "type")
    @NonNull
    private String type = "";

    @ColumnInfo(name = "loop")
    @NonNull
    private Boolean loop = false;

    @ColumnInfo(name = "enhanced")
    @NonNull
    private Boolean enhanced = false;

    @ColumnInfo(name = "distance")
    @NonNull
    private Integer distance = 50;

    @ColumnInfo(name = "source")
    @NonNull
    private String source = "";

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getLocation() {
        return location;
    }

    public void setLocation(@NonNull String location) {
        this.location = location;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public Boolean getLoop() {
        return loop;
    }

    public void setLoop(@NonNull Boolean loop) {
        this.loop = loop;
    }

    @NonNull
    public Boolean getEnhanced() {
        return enhanced;
    }

    public void setEnhanced(@NonNull Boolean enhanced) {
        this.enhanced = enhanced;
    }

    @NonNull
    public Integer getDistance() {
        return distance;
    }

    public void setDistance(@NonNull Integer distance) {
        this.distance = distance;
    }

    @NonNull
    public String getSource() {
        return source;
    }

    public void setSource(@NonNull String source) {
        this.source = source;
    }
}

