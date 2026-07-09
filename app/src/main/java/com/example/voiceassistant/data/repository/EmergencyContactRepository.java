package com.example.voiceassistant.data.repository;

import android.app.Application;

import com.example.voiceassistant.data.database.AppDatabase;
import com.example.voiceassistant.data.database.dao.EmergencyContactDao;
import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyContactRepository {

    private final EmergencyContactDao emergencyContactDao;
    private final ExecutorService executorService;

    public EmergencyContactRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        emergencyContactDao = db.emergencyContactDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public void insert(EmergencyContact contact) {
        executorService.execute(() -> emergencyContactDao.insert(contact));
    }

    public void update(EmergencyContact contact) {
        executorService.execute(() -> emergencyContactDao.update(contact));
    }

    public void delete(EmergencyContact contact) {
        executorService.execute(() -> emergencyContactDao.delete(contact));
    }

    public List<EmergencyContact> getAll() {
        // Note: In a real app, you might return LiveData or use a callback/Future
        // For simplicity as requested, this is a synchronous-looking wrapper
        // but Room won't allow this on the main thread.
        return emergencyContactDao.getAll();
    }

    public EmergencyContact getPrimaryContact() {
        return emergencyContactDao.getPrimaryContact();
    }
}
