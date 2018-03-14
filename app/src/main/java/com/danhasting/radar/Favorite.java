package com.danhasting.radar;

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

    @ColumnInfo(name = "mosaic")
    @NonNull
    private Boolean mosaic = false;

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
    public Boolean getMosaic() {
        return mosaic;
    }

    public void setMosaic(@NonNull Boolean mosaic) {
        this.mosaic = mosaic;
    }
}

