package com.viseeointernational.stop.data.source.base.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.viseeointernational.stop.data.entity.Device;

import java.util.List;

@Dao
public interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDevice(Device... devices);

    @Query("DELETE FROM Device WHERE address = :address")
    int deleteDeviceByAddress(String address);

    @Query("SELECT * FROM Device WHERE address = :address LIMIT 1")
    Device getDeviceByAddress(String address);

    @Query("SELECT * FROM Device WHERE pairId != '' ORDER BY time ASC")
    List<Device> getPairedDevice();
}
