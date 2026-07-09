package com.example.voiceassistant.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.List;

@Dao
public interface EmergencyContactDao {

    @Insert
    void insert(EmergencyContact contact);

    @Update
    void update(EmergencyContact contact);

    @Delete
    void delete(EmergencyContact contact);

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC")
    List<EmergencyContact> getAll();

    @Query("SELECT * FROM emergency_contacts WHERE priority = 1 LIMIT 1")
    EmergencyContact getPrimaryContact();
}
