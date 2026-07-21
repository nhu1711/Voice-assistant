package com.example.voiceassistant.emergency;

import android.os.CountDownTimer;

import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.data.database.entity.EmergencyContact;
import com.example.voiceassistant.data.repository.EmergencyContactRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmergencyManager {

    public interface EmergencyCallback {
        void onCountdownStarted(int totalSeconds);

        void onCountdownTick(int secondsRemaining);

        void onCancelled();

        void onCountdownCompleted();

        void onLoadingContacts();

        void onNoEmergencyContacts();

        void onContactsLoaded(List<EmergencyContact> contacts, EmergencyContact primaryContact);

        void onCallPermissionRequired(EmergencyContact contact);

        void onCallingContact(EmergencyContact contact);

        void onCallStarted(EmergencyContact contact);

        void onCallFailed();

        void onLocationPermissionRequired();

        void onLocationRequested();

        void onLocationReady(double latitude, double longitude);

        void onLocationUnavailable();

        void onSmsPermissionRequired();

        void onSmsSendingStarted(int totalContacts);

        void onSmsSendRequested(EmergencyContact contact);

        void onSmsPermissionDenied();

        void onSmsCompleted(int successCount, int failureCount);

        void onSmsFailed();

        void onError(String message);
    }

    private enum SOSState {
        IDLE,
        COUNTDOWN,
        LOADING_CONTACTS,
        READY,
        WAITING_CALL_PERMISSION,
        CALLING,
        WAITING_LOCATION_PERMISSION,
        GETTING_LOCATION,
        LOCATION_READY,
        WAITING_SMS_PERMISSION,
        SENDING_SMS,
        COMPLETED
    }

    private final EmergencyContactRepository emergencyContactRepository;
    private CountDownTimer countDownTimer;
    private SOSState state = SOSState.IDLE;
    private int sosRequestId;
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();
    private int currentCallIndex;
    private int currentSmsIndex;
    private int smsSuccessCount;
    private int smsFailureCount;

    public EmergencyManager(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    public void startSOS(EmergencyCallback callback) {
        if (callback == null) {
            return;
        }
        if (isSOSActive()) {
            callback.onError("SOS countdown is already active.");
            return;
        }

        int totalSeconds = AppConstants.SOS_CONFIRM_TIMEOUT;
        if (totalSeconds <= 0) {
            callback.onError("SOS countdown duration is invalid.");
            return;
        }

        sosRequestId++;
        state = SOSState.COUNTDOWN;
        callback.onCountdownStarted(totalSeconds);

        countDownTimer = new CountDownTimer(totalSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) Math.ceil(millisUntilFinished / 1000.0);
                callback.onCountdownTick(secondsRemaining);
            }

            @Override
            public void onFinish() {
                countDownTimer = null;
                state = SOSState.LOADING_CONTACTS;
                callback.onCountdownCompleted();
                loadEmergencyContacts(callback, sosRequestId);
            }
        };
        countDownTimer.start();
    }

    private void loadEmergencyContacts(EmergencyCallback callback, int requestId) {
        if (emergencyContactRepository == null) {
            state = SOSState.IDLE;
            callback.onError("Emergency contacts are unavailable.");
            return;
        }

        callback.onLoadingContacts();
        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (requestId != sosRequestId || state != SOSState.LOADING_CONTACTS) {
                    return;
                }

                List<EmergencyContact> sortedContacts = copyAndSortContacts(contacts);
                EmergencyContact primaryContact = findPrimaryContact(sortedContacts);
                if (sortedContacts.isEmpty() || primaryContact == null) {
                    state = SOSState.IDLE;
                    callback.onNoEmergencyContacts();
                    return;
                }

                state = SOSState.READY;
                callback.onContactsLoaded(sortedContacts, primaryContact);
                emergencyContacts = sortedContacts;
                currentCallIndex = sortedContacts.indexOf(primaryContact);
                requestCallPermission(callback);
            }

            @Override
            public void onError(Exception exception) {
                if (requestId != sosRequestId || state != SOSState.LOADING_CONTACTS) {
                    return;
                }
                state = SOSState.IDLE;
                callback.onError("Unable to load emergency contacts.");
            }
        });
    }

    public void cancelSOS() {
        if (!isSOSActive()) {
            return;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        state = SOSState.IDLE;
        sosRequestId++;
        clearSOSData();
    }

    public void cancelSOS(EmergencyCallback callback) {
        boolean wasActive = isSOSActive();
        cancelSOS();
        if (wasActive && callback != null) {
            callback.onCancelled();
        }
    }

    public boolean isSOSActive() {
        return state == SOSState.COUNTDOWN
                || state == SOSState.LOADING_CONTACTS
                || state == SOSState.READY
                || state == SOSState.WAITING_CALL_PERMISSION
                || state == SOSState.CALLING
                || state == SOSState.WAITING_LOCATION_PERMISSION
                || state == SOSState.GETTING_LOCATION
                || state == SOSState.LOCATION_READY
                || state == SOSState.WAITING_SMS_PERMISSION
                || state == SOSState.SENDING_SMS
                || state == SOSState.COMPLETED;
    }

    public void beginCallAttempt(EmergencyContact contact, EmergencyCallback callback) {
        if (callback == null || contact == null || state != SOSState.WAITING_CALL_PERMISSION) {
            return;
        }
        state = SOSState.CALLING;
        callback.onCallingContact(contact);
    }

    public void reportCallStarted(EmergencyContact contact, EmergencyCallback callback) {
        if (callback == null || contact == null || state != SOSState.CALLING) {
            return;
        }
        callback.onCallStarted(contact);
        requestLocationPermission(callback);
    }

    public void reportCallFailed(EmergencyCallback callback) {
        if (callback == null || state != SOSState.CALLING) {
            return;
        }
        currentCallIndex++;
        requestCallPermission(callback);
    }

    public void beginLocationRequest(EmergencyCallback callback) {
        if (callback == null || state != SOSState.WAITING_LOCATION_PERMISSION) {
            return;
        }
        state = SOSState.GETTING_LOCATION;
        callback.onLocationRequested();
    }

    public void reportLocationReady(double latitude, double longitude, EmergencyCallback callback) {
        if (callback == null || state != SOSState.GETTING_LOCATION) {
            return;
        }
        state = SOSState.LOCATION_READY;
        callback.onLocationReady(latitude, longitude);
        requestSmsPermission(callback);
    }

    public void reportLocationUnavailable(EmergencyCallback callback) {
        if (callback == null
                || (state != SOSState.GETTING_LOCATION && state != SOSState.WAITING_LOCATION_PERMISSION)) {
            return;
        }
        callback.onLocationUnavailable();
        requestSmsPermission(callback);
    }

    public void beginSmsSending(EmergencyCallback callback) {
        if (callback == null || state != SOSState.WAITING_SMS_PERMISSION) {
            return;
        }
        state = SOSState.SENDING_SMS;
        currentSmsIndex = 0;
        smsSuccessCount = 0;
        smsFailureCount = 0;
        callback.onSmsSendingStarted(emergencyContacts.size());
        requestNextSmsSend(callback);
    }

    public void reportSmsSent(boolean success, EmergencyCallback callback) {
        if (callback == null || state != SOSState.SENDING_SMS) {
            return;
        }
        if (success) {
            smsSuccessCount++;
        } else {
            smsFailureCount++;
        }
        currentSmsIndex++;
        requestNextSmsSend(callback);
    }

    public void reportSmsPermissionDenied(EmergencyCallback callback) {
        if (callback == null || state != SOSState.WAITING_SMS_PERMISSION) {
            return;
        }
        state = SOSState.IDLE;
        clearSOSData();
        callback.onSmsPermissionDenied();
    }

    private List<EmergencyContact> copyAndSortContacts(List<EmergencyContact> contacts) {
        List<EmergencyContact> sortedContacts = new ArrayList<>();
        if (contacts == null) {
            return sortedContacts;
        }

        for (EmergencyContact contact : contacts) {
            if (contact != null) {
                sortedContacts.add(contact);
            }
        }

        Collections.sort(sortedContacts, (first, second) -> {
            boolean firstPriorityValid = first.getPriority() > 0;
            boolean secondPriorityValid = second.getPriority() > 0;
            if (firstPriorityValid && secondPriorityValid) {
                return Integer.compare(first.getPriority(), second.getPriority());
            }
            if (firstPriorityValid) {
                return -1;
            }
            if (secondPriorityValid) {
                return 1;
            }
            return 0;
        });
        return sortedContacts;
    }

    private EmergencyContact findPrimaryContact(List<EmergencyContact> contacts) {
        for (EmergencyContact contact : contacts) {
            if (contact.getPriority() > 0) {
                return contact;
            }
        }
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    private void requestCallPermission(EmergencyCallback callback) {
        EmergencyContact nextContact = getCurrentCallContact();
        if (nextContact == null) {
            state = SOSState.IDLE;
            clearCallState();
            callback.onCallFailed();
            return;
        }
        state = SOSState.WAITING_CALL_PERMISSION;
        callback.onCallPermissionRequired(nextContact);
    }

    private void requestLocationPermission(EmergencyCallback callback) {
        state = SOSState.WAITING_LOCATION_PERMISSION;
        callback.onLocationPermissionRequired();
    }

    private void requestSmsPermission(EmergencyCallback callback) {
        state = SOSState.WAITING_SMS_PERMISSION;
        callback.onSmsPermissionRequired();
    }

    private void requestNextSmsSend(EmergencyCallback callback) {
        EmergencyContact contact = getCurrentSmsContact();
        if (contact == null) {
            state = SOSState.COMPLETED;
            callback.onSmsCompleted(smsSuccessCount, smsFailureCount);
            state = SOSState.IDLE;
            clearSOSData();
            return;
        }
        callback.onSmsSendRequested(contact);
    }

    private EmergencyContact getCurrentCallContact() {
        if (emergencyContacts == null || currentCallIndex < 0 || currentCallIndex >= emergencyContacts.size()) {
            return null;
        }
        return emergencyContacts.get(currentCallIndex);
    }

    private void clearCallState() {
        emergencyContacts = new ArrayList<>();
        currentCallIndex = 0;
    }

    private EmergencyContact getCurrentSmsContact() {
        if (emergencyContacts == null || currentSmsIndex < 0 || currentSmsIndex >= emergencyContacts.size()) {
            return null;
        }
        return emergencyContacts.get(currentSmsIndex);
    }

    private void clearSOSData() {
        emergencyContacts = new ArrayList<>();
        currentCallIndex = 0;
        currentSmsIndex = 0;
        smsSuccessCount = 0;
        smsFailureCount = 0;
    }
}
