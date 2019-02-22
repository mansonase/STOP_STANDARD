package com.viseeointernational.stop.data.source.base.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.viseeointernational.stop.data.entity.State;

import java.util.List;

@Dao
public interface StateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertState(State... states);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertState(List<State> states);

    @Query("SELECT * FROM State WHERE address = :address AND time BETWEEN :from AND :to ORDER BY time ASC")
    List<State> getStateAsc(String address, long from, long to);

    @Query("SELECT * FROM State WHERE address = :address AND time BETWEEN :from AND :to ORDER BY time DESC")
    List<State> getStateDesc(String address, long from, long to);

    @Query("SELECT * FROM State WHERE address = :address AND type = :type AND time BETWEEN :from AND :to ORDER BY time DESC")
    List<State> getStateWithTypeDesc(String address, long from, long to, int type);

    @Query("SELECT * FROM State WHERE address = :address ORDER BY time DESC LIMIT 1")
    State getLastState(String address);

    @Query("SELECT * FROM State WHERE address = :address AND type != :type ORDER BY time DESC LIMIT 1")
    State getLastStateWithoutType(String address, int type);

    @Query("SELECT COUNT(*) FROM State WHERE address = :address AND indexH = :indexH AND indexL = :indexL AND reportId = :reportId AND time BETWEEN :from AND :to")
    long getStateCountWithIndex(String address, long from, long to, byte indexH, byte indexL, byte reportId);
}
