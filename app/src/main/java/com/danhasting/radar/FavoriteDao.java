package com.danhasting.radar;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Query("SELECT * FROM favorite")
    List<Favorite> getAll();

    @Query("SELECT * FROM favorite WHERE uid IN (:favoriteIds)")
    List<Favorite> loadAllByIds(int[] favoriteIds);

    @Query("SELECT * FROM favorite WHERE uid = :uid LIMIT 1")
    Favorite loadById(int uid);

    @Query("SELECT * FROM favorite WHERE location LIKE :location AND "
            + "type LIKE :type AND loop = :loop")
    List<Favorite> findByData(String location, String type, Boolean loop);

    @Query("SELECT * FROM favorite WHERE name LIKE :name LIMIT 1")
    Favorite findByName(String name);

    @Insert
    void insertAll(Favorite... favorites);

    @Delete
    void delete(Favorite favorite);
}
