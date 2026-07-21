package com.example.voiceassistant.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.data.database.AppDatabase;
import com.example.voiceassistant.data.database.dao.EmergencyContactDao;
import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyContactRepository {

    public interface OperationCallback {
        void onSuccess();

        void onError(Exception exception);
    }

    public interface ContactsCallback {
        void onSuccess(List<EmergencyContact> contacts);

        void onError(Exception exception);
    }

    public interface ContactCallback {
        void onSuccess(EmergencyContact contact);

        void onError(Exception exception);
    }

    private final EmergencyContactDao emergencyContactDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public EmergencyContactRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        emergencyContactDao = db.emergencyContactDao();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
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

    public void getAll(ContactsCallback callback) {
        requireCallback(callback);

        executorService.execute(() -> {
            try {
                List<EmergencyContact> contacts = emergencyContactDao.getAll();
                postContactsSuccess(callback, contacts);
            } catch (Exception exception) {
                postContactsError(callback, exception);
            }
        });
    }

    public void getPrimaryContact(ContactCallback callback) {
        requireCallback(callback);

        executorService.execute(() -> {
            try {
                EmergencyContact contact = emergencyContactDao.getPrimaryContact();
                postContactSuccess(callback, contact);
            } catch (Exception exception) {
                postContactError(callback, exception);
            }
        });
    }

    public void insert(EmergencyContact contact, OperationCallback callback) {
        requireCallback(callback);

        Exception validationError = validateContactForWrite(contact);
        if (validationError != null) {
            postOperationError(callback, validationError);
            return;
        }

        executorService.execute(() -> {
            try {
                emergencyContactDao.insert(contact);
                postOperationSuccess(callback);
            } catch (Exception exception) {
                postOperationError(callback, exception);
            }
        });
    }

    public void update(EmergencyContact contact, OperationCallback callback) {
        requireCallback(callback);

        Exception validationError = validateContactForWrite(contact);
        if (validationError != null) {
            postOperationError(callback, validationError);
            return;
        }

        executorService.execute(() -> {
            try {
                emergencyContactDao.update(contact);
                postOperationSuccess(callback);
            } catch (Exception exception) {
                postOperationError(callback, exception);
            }
        });
    }

    public void delete(EmergencyContact contact, OperationCallback callback) {
        requireCallback(callback);

        if (contact == null) {
            postOperationError(callback, new IllegalArgumentException("Emergency contact is required."));
            return;
        }

        executorService.execute(() -> {
            try {
                emergencyContactDao.delete(contact);
                postOperationSuccess(callback);
            } catch (Exception exception) {
                postOperationError(callback, exception);
            }
        });
    }

    public List<EmergencyContact> getAll() {
        // Compatibility method retained for existing callers. New code should use getAll(ContactsCallback).
        return emergencyContactDao.getAll();
    }

    public EmergencyContact getPrimaryContact() {
        // Compatibility method retained for existing callers. New code should use getPrimaryContact(ContactCallback).
        return emergencyContactDao.getPrimaryContact();
    }

    private Exception validateContactForWrite(EmergencyContact contact) {
        if (contact == null) {
            return new IllegalArgumentException("Emergency contact is required.");
        }
        if (isBlank(contact.getName())) {
            return new IllegalArgumentException("Emergency contact name is required.");
        }
        if (isBlank(contact.getPhoneNumber())) {
            return new IllegalArgumentException("Emergency contact phone number is required.");
        }
        if (contact.getPriority() < 1 || contact.getPriority() > AppConstants.MAX_EMERGENCY_CONTACTS) {
            return new IllegalArgumentException("Emergency contact priority must be between 1 and "
                    + AppConstants.MAX_EMERGENCY_CONTACTS + ".");
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void requireCallback(Object callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback is required.");
        }
    }

    private void postOperationSuccess(OperationCallback callback) {
        mainHandler.post(callback::onSuccess);
    }

    private void postOperationError(OperationCallback callback, Exception exception) {
        mainHandler.post(() -> callback.onError(exception));
    }

    private void postContactsSuccess(ContactsCallback callback, List<EmergencyContact> contacts) {
        mainHandler.post(() -> callback.onSuccess(contacts));
    }

    private void postContactsError(ContactsCallback callback, Exception exception) {
        mainHandler.post(() -> callback.onError(exception));
    }

    private void postContactSuccess(ContactCallback callback, EmergencyContact contact) {
        mainHandler.post(() -> callback.onSuccess(contact));
    }

    private void postContactError(ContactCallback callback, Exception exception) {
        mainHandler.post(() -> callback.onError(exception));
    }
}
