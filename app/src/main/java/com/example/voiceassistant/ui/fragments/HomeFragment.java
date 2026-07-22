package com.example.voiceassistant.ui.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.example.voiceassistant.battery.BatteryManagerHelper;
import com.example.voiceassistant.call.CallManager;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.data.database.entity.EmergencyContact;
import com.example.voiceassistant.data.repository.EmergencyContactRepository;
import com.example.voiceassistant.emergency.EmergencyManager;
import com.example.voiceassistant.location.LocationManagerHelper;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.sms.SmsManagerHelper;
import com.example.voiceassistant.speech.CommandParser;
import com.example.voiceassistant.speech.SpeechRecognizerManager;
import com.example.voiceassistant.speech.VoiceCommandDispatcher;
import com.example.voiceassistant.speech.VoiceIntent;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.utils.TimeFormatter;
import com.example.voiceassistant.ui.activities.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private MaterialButton btnMicro;
    private MaterialButton btnSOS;
    private TextView tvStatus;
    private TextView tvCommand;
    private TextView tvResponse;
    private TextView tvTime;
    private TextView tvBattery;

    private TTSManager ttsManager;
    private SpeechRecognizerManager speechRecognizerManager;
    private ContactManager contactManager;
    private CallManager callManager;
    private BatteryManagerHelper batteryManagerHelper;
    private EmergencyManager emergencyManager;
    private LocationManagerHelper locationManagerHelper;
    private SmsManagerHelper smsManagerHelper;
    private EmergencyManager.EmergencyCallback sosCallback;

    private ObjectAnimator pulseAnimator;
    private Dialog sosCountdownDialog;
    private TextView tvSosCountdown;
    private EmergencyContact pendingEmergencyCallContact;
    private ActivityResultLauncher<String> callPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> smsPermissionLauncher;
    private Double temporaryLatitude;
    private Double temporaryLongitude;
    private String pendingEmergencySmsMessage;
    private boolean isViewDestroyed;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            updateSystemInfo();
            handler.postDelayed(this, 60000);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCallPermissionLauncher();
        setupLocationPermissionLauncher();
        setupSmsPermissionLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        isViewDestroyed = false;
        initViews(view);
        initManagers();
        setupListeners();
        updateSystemInfo();
        handler.post(timeUpdater);
        return view;
    }

    private void initViews(View view) {
        btnMicro = view.findViewById(R.id.btn_micro);
        btnSOS = view.findViewById(R.id.btn_sos);
        tvStatus = view.findViewById(R.id.tv_status);
        tvCommand = view.findViewById(R.id.tv_command);
        tvResponse = view.findViewById(R.id.tv_response);
        tvTime = view.findViewById(R.id.tv_time);
        tvBattery = view.findViewById(R.id.tv_battery);
    }

    private VoiceCommandDispatcher voiceCommandDispatcher;
    private void initManagers() {
        Context context = requireContext();
        
        // Setup Pulse Animation
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnMicro,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        );
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        ttsManager = TTSManager.getInstance(context);
        ttsManager.setListener(new TTSManager.TTSListener() {
            @Override
            public void onSpeechStarted() {}

            @Override
            public void onSpeechFinished() {}

            @Override
            public void onSpeechError(String error) {
                Log.e(TAG, "TTS Error: " + error);
            }
        });

        speechRecognizerManager = new SpeechRecognizerManager(context, new SpeechRecognizerManager.RecognitionCallback() {
            @Override
            public void onReadyForSpeech() {
                updateUI(() -> {
                    tvStatus.setText(R.string.response_hint);
                    startPulseAnimation();
                });
            }

            @Override
            public void onBeginningOfSpeech() {
                updateUI(() -> tvStatus.setText(getString(R.string.analyzing)));
                ttsManager.setAssistantListening(true);
            }

            @Override
            public void onEndOfSpeech() {
                updateUI(() -> {
                    tvStatus.setText(getString(R.string.status_title));
                    stopPulseAnimation();
                });
                ttsManager.setAssistantListening(false);
            }

            @Override
            public void onResult(String recognizedText) {
                Log.d(TAG, "[VOICE] VOICE_RESULT: " + recognizedText);
                updateUI(() -> {
                    stopPulseAnimation();
                    tvCommand.setText(recognizedText);
                    tvStatus.setText(getString(R.string.status_title));
                    processVoiceCommand(recognizedText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Speech Error: " + errorMessage);
                updateUI(() -> {
                    stopPulseAnimation();
                    String currentText = tvCommand.getText().toString();
                    if (currentText.length() > 5) {
                        tvStatus.setText(getString(R.string.status_title));
                        processVoiceCommand(currentText);
                    } else if (currentText.isEmpty()) {
                        tvStatus.setText(errorMessage);
                    }
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                updateUI(() -> tvCommand.setText(partialText));
            }
        });

        contactManager = new ContactManager(context);
        callManager = new CallManager(context, ttsManager);
        batteryManagerHelper = new BatteryManagerHelper(context);
        locationManagerHelper = new LocationManagerHelper(context);
        smsManagerHelper = new SmsManagerHelper(context);
        emergencyManager = new EmergencyManager(new EmergencyContactRepository(requireActivity().getApplication()));

        voiceCommandDispatcher = new VoiceCommandDispatcher(requireActivity(), ttsManager, contactManager, callManager, batteryManagerHelper, new VoiceCommandDispatcher.EmergencyCommandCallback() {
            @Override
            public void onResponse(String text) {
                updateUI(() -> tvResponse.setText(text));
            }

            @Override
            public void onSOSRequested() {
                updateUI(() -> startSOSCountdown());
            }
        });
    }
    //su kien nhan nut micro
    private void setupListeners() {
        btnMicro.setOnClickListener(v -> {
            Log.d(TAG, "[VOICE] VOICE_START");
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                PermissionHelper.requestRecordAudioPermission(requireActivity());
                return;
            }
            
            if (speechRecognizerManager.isListening()) {
                stopVoiceRecognition();
            } else {
                startVoiceRecognition();
            }
        });

        btnSOS.setOnClickListener(v -> {
            startSOSCountdown();
        });
    }

    private void startSOSCountdown() {
        if (!isHomeViewActive()) {
            return;
        }
        if (emergencyManager.isSOSActive() || sosCountdownDialog != null) {
            Toast.makeText(requireContext(), R.string.sos_already_active_message, Toast.LENGTH_SHORT).show();
            return;
        }

        showSOSCountdownDialog();
        sosCallback = new EmergencyManager.EmergencyCallback() {
            @Override
            public void onCountdownStarted(int totalSeconds) {
                if (!isHomeViewActive()) {
                    return;
                }
                updateSOSCountdown(totalSeconds);
                tvResponse.setText(R.string.sos_countdown_started_message);
                ttsManager.speak(getString(R.string.sos_countdown_started_message));
            }

            @Override
            public void onCountdownTick(int secondsRemaining) {
                if (!isHomeViewActive()) {
                    return;
                }
                updateSOSCountdown(secondsRemaining);
            }

            @Override
            public void onCancelled() {
                if (!isHomeViewActive()) {
                    return;
                }
                dismissSOSCountdownDialog();
                tvResponse.setText(R.string.sos_cancelled_message);
                ttsManager.speak(getString(R.string.sos_cancelled_message));
                Toast.makeText(requireContext(), R.string.sos_cancelled_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCountdownCompleted() {
                if (!isHomeViewActive()) {
                    return;
                }
                dismissSOSCountdownDialog();
            }

            @Override
            public void onLoadingContacts() {
                if (!isHomeViewActive()) {
                    return;
                }
                tvResponse.setText(R.string.sos_loading_contacts_message);
            }

            @Override
            public void onNoEmergencyContacts() {
                if (!isHomeViewActive()) {
                    return;
                }
                tvResponse.setText(R.string.sos_no_emergency_contacts_message);
                ttsManager.speak(getString(R.string.sos_no_emergency_contacts_message));
                Toast.makeText(requireContext(), R.string.sos_no_emergency_contacts_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContactsLoaded(List<EmergencyContact> contacts, EmergencyContact primaryContact) {
                if (!isHomeViewActive()) {
                    return;
                }
                String message = getString(R.string.sos_contacts_ready_message, primaryContact.getName());
                tvResponse.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCallPermissionRequired(EmergencyContact contact) {
                if (!isHomeViewActive()) {
                    return;
                }
                requestEmergencyCallPermissionOrStartCall(contact);
            }

            @Override
            public void onCallingContact(EmergencyContact contact) {
                if (!isHomeViewActive()) {
                    return;
                }
                String message = getString(R.string.sos_calling_contact_message, contact.getName());
                tvResponse.setText(message);
                ttsManager.speak(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCallStarted(EmergencyContact contact) {
                if (!isHomeViewActive()) {
                    return;
                }
                pendingEmergencyCallContact = null;
            }

            @Override
            public void onCallFailed() {
                if (!isHomeViewActive()) {
                    return;
                }
                pendingEmergencyCallContact = null;
                tvResponse.setText(R.string.sos_call_failed_message);
                Toast.makeText(requireContext(), R.string.sos_call_failed_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationPermissionRequired() {
                if (!isHomeViewActive()) {
                    return;
                }
                requestLocationPermissionOrGetLocation();
            }

            @Override
            public void onLocationRequested() {
                if (!isHomeViewActive()) {
                    return;
                }
                tvResponse.setText(R.string.sos_location_requested_message);
            }

            @Override
            public void onLocationReady(double latitude, double longitude) {
                if (!isHomeViewActive()) {
                    return;
                }
                temporaryLatitude = latitude;
                temporaryLongitude = longitude;
                pendingEmergencySmsMessage = buildEmergencySmsMessage(true);
                tvResponse.setText(R.string.sos_location_ready_message);
                Toast.makeText(requireContext(), R.string.sos_location_ready_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationUnavailable() {
                if (!isHomeViewActive()) {
                    return;
                }
                pendingEmergencySmsMessage = buildEmergencySmsMessage(false);
                tvResponse.setText(R.string.sos_location_unavailable_message);
                Toast.makeText(requireContext(), R.string.sos_location_unavailable_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSmsPermissionRequired() {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                requestSmsPermissionOrStartSending();
            }

            @Override
            public void onSmsSendingStarted(int totalContacts) {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                tvResponse.setText(getString(R.string.sos_sms_sending_message, totalContacts));
            }

            @Override
            public void onSmsSendRequested(EmergencyContact contact) {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                sendEmergencySmsToContact(contact);
            }

            @Override
            public void onSmsPermissionDenied() {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                clearPendingSmsState();
                tvResponse.setText(R.string.sos_sms_permission_required_message);
                Toast.makeText(requireContext(), R.string.sos_sms_permission_required_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSmsCompleted(int successCount, int failureCount) {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                showSmsResult(successCount, failureCount);
                clearPendingSmsState();
            }

            @Override
            public void onSmsFailed() {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                clearPendingSmsState();
                tvResponse.setText(R.string.sos_sms_failed_message);
                Toast.makeText(requireContext(), R.string.sos_sms_failed_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isHomeViewActive()) {
                    return;
                }
                dismissSOSCountdownDialog();
                tvResponse.setText(R.string.sos_contact_loading_error_message);
                Toast.makeText(requireContext(), R.string.sos_contact_loading_error_message, Toast.LENGTH_SHORT).show();
            }
        };
        emergencyManager.startSOS(sosCallback);
    }

    private void setupCallPermissionLauncher() {
        callPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isHomeViewActive()) {
                        pendingEmergencyCallContact = null;
                        return;
                    }
                    if (isGranted) {
                        startPendingEmergencyCall();
                    } else {
                        pendingEmergencyCallContact = null;
                        if (emergencyManager != null) {
                            emergencyManager.cancelSOS();
                        }
                        tvResponse.setText(R.string.sos_call_permission_required_message);
                        Toast.makeText(requireContext(), R.string.sos_call_permission_required_message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupSmsPermissionLauncher() {
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isHomeViewActive()) {
                        clearPendingSmsState();
                        return;
                    }
                    if (isGranted) {
                        startEmergencySmsSending();
                    } else if (emergencyManager != null) {
                        emergencyManager.reportSmsPermissionDenied(sosCallback);
                    }
                }
        );
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    if (!isHomeViewActive()) {
                        clearTemporaryLocation();
                        return;
                    }
                    if (isLocationPermissionGranted(permissions)) {
                        getCurrentSOSLocation();
                    } else {
                        clearTemporaryLocation();
                        if (emergencyManager != null) {
                            emergencyManager.reportLocationUnavailable(sosCallback);
                        }
                    }
                }
        );
    }

    private void requestEmergencyCallPermissionOrStartCall(EmergencyContact contact) {
        if (contact == null) {
            handleEmergencyCallLaunchFailed();
            return;
        }

        pendingEmergencyCallContact = contact;
        if (PermissionHelper.hasCallPhonePermission(requireContext())) {
            Log.d(TAG, "CALL_PHONE permission granted");
            startPendingEmergencyCall();
        } else {
            Log.w(TAG, "CALL_PHONE permission missing");
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void startPendingEmergencyCall() {
        if (!isHomeViewActive() || pendingEmergencyCallContact == null) {
            return;
        }

        EmergencyContact contact = pendingEmergencyCallContact;
        emergencyManager.beginCallAttempt(contact, sosCallback);
        boolean callStarted = callManager.makeCall(contact.getPhoneNumber(), contact.getName());
        if (callStarted) {
            pendingEmergencyCallContact = null;
            emergencyManager.reportCallStarted(contact, sosCallback);
        } else {
            handleEmergencyCallLaunchFailed();
        }
    }

    private void handleEmergencyCallLaunchFailed() {
        Log.w(TAG, "Trying next emergency contact");
        pendingEmergencyCallContact = null;
        if (emergencyManager != null) {
            emergencyManager.reportCallFailed(sosCallback);
        }
    }

    private void requestLocationPermissionOrGetLocation() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            getCurrentSOSLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getCurrentSOSLocation() {
        if (!isHomeViewActive()) {
            return;
        }
        emergencyManager.beginLocationRequest(sosCallback);
        locationManagerHelper.getCurrentLocation(new LocationManagerHelper.LocationCallback() {
            @Override
            public void onLocationAvailable(double latitude, double longitude) {
                if (!isHomeViewActive()) {
                    clearTemporaryLocation();
                    return;
                }
                emergencyManager.reportLocationReady(latitude, longitude, sosCallback);
            }

            @Override
            public void onLocationUnavailable() {
                if (!isHomeViewActive()) {
                    clearTemporaryLocation();
                    return;
                }
                emergencyManager.reportLocationUnavailable(sosCallback);
            }

            @Override
            public void onError() {
                if (!isHomeViewActive()) {
                    clearTemporaryLocation();
                    return;
                }
                emergencyManager.reportLocationUnavailable(sosCallback);
            }
        });
    }

    private boolean isLocationPermissionGranted(Map<String, Boolean> permissions) {
        Boolean fineGranted = permissions.get(Manifest.permission.ACCESS_FINE_LOCATION);
        Boolean coarseGranted = permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION);
        return Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted);
    }

    private void clearTemporaryLocation() {
        temporaryLatitude = null;
        temporaryLongitude = null;
    }

    private void requestSmsPermissionOrStartSending() {
        if (PermissionHelper.hasSendSmsPermission(requireContext())) {
            startEmergencySmsSending();
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }

    private void startEmergencySmsSending() {
        if (!isHomeViewActive() || emergencyManager == null) {
            return;
        }
        if (pendingEmergencySmsMessage == null || pendingEmergencySmsMessage.trim().isEmpty()) {
            pendingEmergencySmsMessage = buildEmergencySmsMessage(false);
        }
        emergencyManager.beginSmsSending(sosCallback);
    }

    private void sendEmergencySmsToContact(EmergencyContact contact) {
        if (contact == null || pendingEmergencySmsMessage == null
                || emergencyManager == null || smsManagerHelper == null) {
            if (emergencyManager != null) {
                emergencyManager.reportSmsSent(false, sosCallback);
            }
            return;
        }

        smsManagerHelper.sendSms(contact.getPhoneNumber(), pendingEmergencySmsMessage, new SmsManagerHelper.SmsCallback() {
            @Override
            public void onSuccess() {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                emergencyManager.reportSmsSent(true, sosCallback);
            }

            @Override
            public void onFailure() {
                if (!isHomeViewActive()) {
                    clearPendingSmsState();
                    return;
                }
                emergencyManager.reportSmsSent(false, sosCallback);
            }
        });
    }

    private String buildEmergencySmsMessage(boolean includeLocation) {
        if (includeLocation && temporaryLatitude != null && temporaryLongitude != null) {
            String mapUrl = String.format(
                    Locale.US,
                    "https://maps.google.com/?q=%.6f,%.6f",
                    temporaryLatitude,
                    temporaryLongitude
            );
            return getString(R.string.sos_sms_message_with_location, mapUrl);
        }
        return getString(R.string.sos_sms_message_without_location);
    }

    private void showSmsResult(int successCount, int failureCount) {
        if (successCount > 0 && failureCount == 0) {
            tvResponse.setText(getString(R.string.sos_sms_success_message, successCount));
            Toast.makeText(requireContext(), getString(R.string.sos_sms_success_message, successCount), Toast.LENGTH_SHORT).show();
        } else if (successCount > 0) {
            tvResponse.setText(getString(R.string.sos_sms_partial_success_message, successCount, failureCount));
            Toast.makeText(requireContext(), getString(R.string.sos_sms_partial_success_message, successCount, failureCount), Toast.LENGTH_SHORT).show();
        } else {
            tvResponse.setText(R.string.sos_sms_failed_message);
            Toast.makeText(requireContext(), R.string.sos_sms_failed_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearPendingSmsState() {
        clearTemporaryLocation();
        pendingEmergencySmsMessage = null;
    }

    private void showSOSCountdownDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_sos_countdown, null);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(false);

        tvSosCountdown = dialogView.findViewById(R.id.tv_sos_countdown);
        MaterialButton btnCancelSOS = dialogView.findViewById(R.id.btn_cancel_sos);
        btnCancelSOS.setOnClickListener(v -> cancelSOSCountdown());
        dialog.setOnCancelListener(dialogInterface -> cancelSOSCountdown());
        dialog.setOnDismissListener(dialogInterface -> {
            if (sosCountdownDialog == dialog) {
                sosCountdownDialog = null;
                tvSosCountdown = null;
            }
        });

        sosCountdownDialog = dialog;
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void cancelSOSCountdown() {
        if (emergencyManager != null && emergencyManager.isSOSActive()) {
            emergencyManager.cancelSOS(sosCallback);
        } else {
            dismissSOSCountdownDialog();
        }
    }

    private void updateSOSCountdown(int secondsRemaining) {
        if (tvSosCountdown != null) {
            tvSosCountdown.setText(String.valueOf(secondsRemaining));
        }
    }

    private void dismissSOSCountdownDialog() {
        if (sosCountdownDialog != null && sosCountdownDialog.isShowing()) {
            sosCountdownDialog.dismiss();
        }
    }

    private boolean isHomeViewActive() {
        return !isViewDestroyed && isAdded() && getView() != null;
    }

    private void startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(getContext(), R.string.speech_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        // Làm sạch ô văn bản trước khi nghe mới
        tvCommand.setText("");
        tvResponse.setText("");
        
        ttsManager.stop();
        speechRecognizerManager.startListening();
    }

    private void stopVoiceRecognition() {
        speechRecognizerManager.stopListening();
        stopPulseAnimation();
        tvStatus.setText(getString(R.string.status_title));
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && !pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            btnMicro.setScaleX(1f);
            btnMicro.setScaleY(1f);
        }
    }

    private void processVoiceCommand(String text) {
        CommandParser.CommandResult result = CommandParser.parse(text);
        voiceCommandDispatcher.execute(result);
    }

    private void handleDetectCommand() {
        Log.d(TAG, "Handling DETECT command");
        String response = getString(R.string.response_opening_camera);
        tvResponse.setText(response);
        ttsManager.speak(response);
        
        handler.postDelayed(() -> {
            if (isAdded() && getActivity() instanceof MainActivity) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_object_detection);
                }
            }
        }, 1500);
    }

    private String getCurrentLanguage() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(AppConstants.PREF_LANGUAGE, AppConstants.DEFAULT_LANGUAGE);
    }

    private int getBatteryLevel() {
        return batteryManagerHelper.getBatteryLevel();
    }

    private void updateSystemInfo() {
        if (isAdded()) {
            tvTime.setText(TimeFormatter.getDisplayTime(requireContext()));
            tvBattery.setText(getString(R.string.battery_status, getBatteryLevel()));
        }
    }

    private void updateUI(Runnable runnable) {
        if (isAdded()) {
            handler.post(runnable);
        }
    }

    @Override
    public void onDestroyView() {
        isViewDestroyed = true;
        if (emergencyManager != null) {
            emergencyManager.cancelSOS();
        }
        dismissSOSCountdownDialog();
        sosCallback = null;
        pendingEmergencyCallContact = null;
        clearPendingSmsState();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizerManager != null) speechRecognizerManager.destroy();
        handler.removeCallbacks(timeUpdater);
    }
}
