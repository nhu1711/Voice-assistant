package com.example.voiceassistant.ui.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.provider.ContactsContract;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voiceassistant.R;
import com.google.android.material.button.MaterialButton;
import com.example.voiceassistant.permissions.PermissionHelper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voiceassistant.contacts.ContactPickerManager;
import com.example.voiceassistant.contacts.EmergencyContactPriorityManager;
import com.example.voiceassistant.contacts.EmergencyContactValidator;
import com.example.voiceassistant.contacts.PickedContact;
import com.example.voiceassistant.data.database.entity.EmergencyContact;
import com.example.voiceassistant.data.repository.EmergencyContactRepository;
import com.example.voiceassistant.speech.CommandParser;
import com.example.voiceassistant.speech.SpeechRecognizerManager;
import com.example.voiceassistant.speech.VoiceCommandDispatcher;
import com.example.voiceassistant.tts.TTSManager;
import com.example.voiceassistant.ui.adapters.EmergencyContactAdapter;

import java.util.List;

public class ContactsFragment extends Fragment {

    private android.widget.TextView tvEmptyContacts;
    private RecyclerView rvEmergencyContacts;
    private MaterialButton btnAddContact;
    private EmergencyContactRepository emergencyContactRepository;
    private EmergencyContactAdapter emergencyContactAdapter;
    private ContactPickerManager contactPickerManager;
    private EmergencyContactPriorityManager emergencyContactPriorityManager;
    private EmergencyContactValidator emergencyContactValidator;
    private SpeechRecognizerManager speechRecognizerManager;
    private VoiceCommandDispatcher voiceCommandDispatcher;
    private TTSManager ttsManager;
    private TextView tvCommand;
    private MaterialButton btnMicro;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ObjectAnimator pulseAnimator;
    private PickedContact selectedContact;
    private Dialog addContactDialog;
    private Dialog editContactDialog;
    private AlertDialog deleteContactDialog;
    private ActivityResultLauncher<String> contactPermissionLauncher;
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private boolean isViewDestroyed;
    private boolean isInsertInProgress;
    private boolean isUpdateInProgress;
    private boolean isDeleteInProgress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupContactPermissionLauncher();
        setupContactPickerLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        isViewDestroyed = false;
        tvEmptyContacts = view.findViewById(R.id.tv_empty_contacts);
        rvEmergencyContacts = view.findViewById(R.id.rv_emergency_contacts);
        btnAddContact = view.findViewById(R.id.btn_add_contact);
        emergencyContactRepository = new EmergencyContactRepository(requireActivity().getApplication());
        contactPickerManager = new ContactPickerManager();
        emergencyContactPriorityManager = new EmergencyContactPriorityManager();
        emergencyContactValidator = new EmergencyContactValidator();
        setupEmergencyContactList();
        loadEmergencyContacts();

        setupAddContactButton();
        setupVoiceAssistant(view);
        
        return view;
    }

    private void setupVoiceAssistant(View view) {
        tvCommand = view.findViewById(R.id.tv_command);
        btnMicro = view.findViewById(R.id.btn_micro);

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnMicro,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        );
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        ttsManager = TTSManager.getInstance(requireContext());
        voiceCommandDispatcher = new VoiceCommandDispatcher(
                requireActivity(),
                ttsManager,
                new com.example.voiceassistant.contacts.ContactManager(requireContext()),
                new com.example.voiceassistant.call.CallManager(requireContext(), ttsManager),
                new com.example.voiceassistant.battery.BatteryManagerHelper(requireContext()),
                text -> mainHandler.post(() -> {
                    if (isFragmentViewActive() && tvCommand != null) {
                        tvCommand.setText(text);
                    }
                })
        );

        speechRecognizerManager = new SpeechRecognizerManager(requireContext(), new SpeechRecognizerManager.RecognitionCallback() {
            @Override
            public void onReadyForSpeech() {
                mainHandler.post(() -> {
                    if (!isFragmentViewActive()) {
                        return;
                    }
                    tvCommand.setText(R.string.status_listening);
                    pulseAnimator.start();
                });
            }

            @Override
            public void onBeginningOfSpeech() {
                mainHandler.post(() -> {
                    if (ttsManager != null) {
                        ttsManager.setAssistantListening(true);
                    }
                });
            }

            @Override
            public void onEndOfSpeech() {
                mainHandler.post(() -> {
                    if (ttsManager != null) {
                        ttsManager.setAssistantListening(false);
                    }
                    resetMicroAnimation();
                });
            }

            @Override
            public void onResult(String text) {
                mainHandler.post(() -> {
                    if (!isFragmentViewActive()) {
                        return;
                    }
                    resetMicroAnimation();
                    tvCommand.setText(text);
                    voiceCommandDispatcher.execute(CommandParser.parse(text));
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    if (!isFragmentViewActive()) {
                        return;
                    }
                    resetMicroAnimation();

                    String currentText = tvCommand.getText().toString();
                    if (currentText.length() > 5) {
                        tvCommand.setText(currentText);
                        voiceCommandDispatcher.execute(CommandParser.parse(currentText));
                    } else if (currentText.isEmpty()) {
                        tvCommand.setText(error);
                    }
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                mainHandler.post(() -> {
                    if (isFragmentViewActive() && tvCommand != null) {
                        tvCommand.setText(partialText);
                    }
                });
            }
        });

        btnMicro.setOnClickListener(v -> {
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                PermissionHelper.requestRecordAudioPermission(requireActivity());
                return;
            }
            if (speechRecognizerManager.isListening()) {
                speechRecognizerManager.stopListening();
                resetMicroAnimation();
            } else {
                if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                    Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvCommand.setText("");
                ttsManager.stop();
                speechRecognizerManager.startListening();
            }
        });
    }

    private void setupContactPermissionLauncher() {
        contactPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isFragmentViewActive()) {
                        return;
                    }
                    if (isGranted) {
                        openContactPicker();
                    } else {
                        showContactPermissionDeniedMessage();
                    }
                }
        );
    }

    private void setupContactPickerLauncher() {
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleContactPickerResult
        );
    }

    private void setupEmergencyContactList() {
        emergencyContactAdapter = new EmergencyContactAdapter(
                this::loadContactForEdit,
                this::showDeleteEmergencyContactConfirmation
        );
        rvEmergencyContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEmergencyContacts.setAdapter(emergencyContactAdapter);
    }

    private void setupAddContactButton() {
        if (btnAddContact != null) {
            btnAddContact.setOnClickListener(v -> showAddContactMethodDialog());
        }
    }

    private void showAddContactMethodDialog() {
        if (!isFragmentViewActive() || addContactDialog != null) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_choose_contact_method, null);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);

        MaterialButton btnManualEntry = dialogView.findViewById(R.id.btn_enter_manually);
        MaterialButton btnChooseFromContacts = dialogView.findViewById(R.id.btn_choose_from_contacts);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnManualEntry.setOnClickListener(v -> {
            if (addContactDialog == dialog) {
                addContactDialog = null;
            }
            dialog.dismiss();
            showManualContactDialog();
        });
        btnChooseFromContacts.setOnClickListener(v -> {
            if (addContactDialog == dialog) {
                addContactDialog = null;
            }
            dialog.dismiss();
            requestContactPermissionOrOpenPicker();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(dialogInterface -> {
            if (addContactDialog == dialog) {
                addContactDialog = null;
            }
        });

        addContactDialog = dialog;
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void loadContactForEdit(EmergencyContact selectedContact) {
        if (selectedContact == null) {
            return;
        }

        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (!isFragmentViewActive()) {
                    return;
                }

                EmergencyContact currentContact = findContactById(contacts, selectedContact.getId());
                if (currentContact == null) {
                    Toast.makeText(requireContext(), R.string.unable_update_emergency_contact, Toast.LENGTH_SHORT).show();
                    loadEmergencyContacts();
                    return;
                }

                showEditEmergencyContactDialog(currentContact, contacts);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.unable_update_emergency_contact, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditEmergencyContactDialog(EmergencyContact contact, List<EmergencyContact> contacts) {
        if (!isFragmentViewActive()) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_emergency_contact, null);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(false);

        EditText etContactName = dialogView.findViewById(R.id.et_contact_name);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        etContactName.setText(contact.getName());
        etPhoneNumber.setText(contact.getPhoneNumber());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> saveEmergencyContactEdits(
                contact,
                etContactName,
                etPhoneNumber,
                btnSave,
                btnCancel
        ));
        dialog.setOnDismissListener(dialogInterface -> {
            if (editContactDialog == dialog) {
                editContactDialog = null;
            }
            isUpdateInProgress = false;
        });

        editContactDialog = dialog;
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void saveEmergencyContactEdits(
            EmergencyContact dialogContact,
            EditText etContactName,
            EditText etPhoneNumber,
            MaterialButton btnSave,
            MaterialButton btnCancel
    ) {
        if (isUpdateInProgress) {
            return;
        }

        String name = emergencyContactValidator.trim(etContactName.getText().toString());
        String phoneNumber = emergencyContactValidator.trim(etPhoneNumber.getText().toString());
        if (!validateContactFields(name, phoneNumber, etContactName, etPhoneNumber)) {
            return;
        }

        isUpdateInProgress = true;
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);
        if (editContactDialog != null) {
            editContactDialog.setCancelable(false);
        }

        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (!isFragmentViewActive()) {
                    return;
                }

                EmergencyContact currentContact = findContactById(contacts, dialogContact.getId());
                if (currentContact == null) {
                    finishFailedUpdate(btnSave, btnCancel, R.string.unable_update_emergency_contact);
                    dismissEditContactDialog();
                    loadEmergencyContacts();
                    return;
                }

                PickedContact editedContact = new PickedContact(
                        currentContact.getContactId(),
                        name,
                        phoneNumber
                );
                if (emergencyContactValidator.isDuplicate(editedContact, contacts, currentContact.getId())) {
                    finishFailedUpdate(btnSave, btnCancel, R.string.duplicate_emergency_contact_message);
                    return;
                }

                List<EmergencyContact> updates = emergencyContactPriorityManager.buildUpdatedContacts(
                        contacts,
                        currentContact,
                        name,
                        phoneNumber,
                        currentContact.getRelationship(),
                        currentContact.getPriority()
                );

                if (updates.isEmpty()) {
                    finishSuccessfulUpdate();
                    return;
                }

                updateContactsSequentially(updates, 0, btnSave, btnCancel);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedUpdate(btnSave, btnCancel, R.string.unable_update_emergency_contact);
            }
        });
    }

    private void updateContactsSequentially(
            List<EmergencyContact> updates,
            int updateIndex,
            MaterialButton btnSave,
            MaterialButton btnCancel
    ) {
        if (updateIndex >= updates.size()) {
            finishSuccessfulUpdate();
            return;
        }

        emergencyContactRepository.update(updates.get(updateIndex), new EmergencyContactRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isFragmentViewActive()) {
                    return;
                }
                updateContactsSequentially(updates, updateIndex + 1, btnSave, btnCancel);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedUpdate(btnSave, btnCancel, R.string.unable_update_emergency_contact);
                loadEmergencyContacts();
            }
        });
    }

    private void finishSuccessfulUpdate() {
        isUpdateInProgress = false;
        dismissEditContactDialog();
        loadEmergencyContacts();
        Toast.makeText(requireContext(), R.string.emergency_contact_updated_success, Toast.LENGTH_SHORT).show();
    }

    private void finishFailedUpdate(MaterialButton btnSave, MaterialButton btnCancel, int messageRes) {
        isUpdateInProgress = false;
        btnSave.setEnabled(true);
        btnCancel.setEnabled(true);
        if (editContactDialog != null) {
            editContactDialog.setCancelable(true);
        }
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private void dismissEditContactDialog() {
        if (editContactDialog != null && editContactDialog.isShowing()) {
            editContactDialog.dismiss();
        }
    }

    private void showDeleteEmergencyContactConfirmation(EmergencyContact contact) {
        if (!isFragmentViewActive() || contact == null || isDeleteInProgress || deleteContactDialog != null) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_emergency_contact_title)
                .setMessage(getString(R.string.confirm_delete_emergency_contact, contact.getName()))
                .setNegativeButton(R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.delete, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> deleteEmergencyContact(contact)));
        dialog.setOnDismissListener(dialogInterface -> {
            if (deleteContactDialog == dialog) {
                deleteContactDialog = null;
            }
            isDeleteInProgress = false;
        });

        deleteContactDialog = dialog;
        dialog.show();
    }

    private void deleteEmergencyContact(EmergencyContact contact) {
        if (isDeleteInProgress || !isFragmentViewActive()) {
            return;
        }

        isDeleteInProgress = true;
        setDeleteDialogButtonsEnabled(false);
        if (deleteContactDialog != null) {
            deleteContactDialog.setCancelable(false);
        }

        emergencyContactRepository.delete(contact, new EmergencyContactRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isFragmentViewActive()) {
                    return;
                }
                loadContactsAfterDelete();
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedDelete(true);
            }
        });
    }

    private void loadContactsAfterDelete() {
        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (!isFragmentViewActive()) {
                    return;
                }

                List<EmergencyContact> updates = emergencyContactPriorityManager.normalizeAfterDelete(contacts);
                if (updates.isEmpty()) {
                    finishSuccessfulDelete();
                    return;
                }

                updatePrioritiesAfterDeleteSequentially(updates, 0);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedDelete(false);
                loadEmergencyContacts();
            }
        });
    }

    private void updatePrioritiesAfterDeleteSequentially(List<EmergencyContact> updates, int updateIndex) {
        if (updateIndex >= updates.size()) {
            finishSuccessfulDelete();
            return;
        }

        emergencyContactRepository.update(updates.get(updateIndex), new EmergencyContactRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isFragmentViewActive()) {
                    return;
                }
                updatePrioritiesAfterDeleteSequentially(updates, updateIndex + 1);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedDelete(false);
                loadEmergencyContacts();
            }
        });
    }

    private void finishSuccessfulDelete() {
        isDeleteInProgress = false;
        dismissDeleteContactDialog();
        loadEmergencyContacts();
        Toast.makeText(requireContext(), R.string.emergency_contact_deleted_success, Toast.LENGTH_SHORT).show();
    }

    private void finishFailedDelete(boolean canRetryDelete) {
        isDeleteInProgress = false;
        if (canRetryDelete) {
            setDeleteDialogButtonsEnabled(true);
            if (deleteContactDialog != null) {
                deleteContactDialog.setCancelable(true);
            }
        } else {
            dismissDeleteContactDialog();
        }
        Toast.makeText(requireContext(), R.string.unable_delete_emergency_contact, Toast.LENGTH_SHORT).show();
    }

    private void setDeleteDialogButtonsEnabled(boolean enabled) {
        if (deleteContactDialog == null) {
            return;
        }
        android.widget.Button deleteButton = deleteContactDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button cancelButton = deleteContactDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (deleteButton != null) {
            deleteButton.setEnabled(enabled);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(enabled);
        }
    }

    private void dismissDeleteContactDialog() {
        if (deleteContactDialog != null && deleteContactDialog.isShowing()) {
            deleteContactDialog.dismiss();
        }
    }

    private EmergencyContact findContactById(List<EmergencyContact> contacts, int contactId) {
        if (contacts == null) {
            return null;
        }
        for (EmergencyContact contact : contacts) {
            if (contact != null && contact.getId() == contactId) {
                return contact;
            }
        }
        return null;
    }

    private void requestContactPermissionOrOpenPicker() {
        if (PermissionHelper.hasReadContactsPermission(requireContext())) {
            openContactPicker();
            return;
        }
        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
    }

    private void openContactPicker() {
        if (!isFragmentViewActive()) {
            return;
        }
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        );
        try {
            contactPickerLauncher.launch(intent);
        } catch (android.content.ActivityNotFoundException exception) {
            Toast.makeText(requireContext(), R.string.error_read_selected_contact, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleContactPickerResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        if (!isFragmentViewActive()) {
            return;
        }

        try {
            PickedContact contact = contactPickerManager.resolvePickedContact(
                    requireContext().getContentResolver(),
                    result.getData().getData()
            );
            onContactPicked(contact);
        } catch (ContactPickerManager.ContactPickException exception) {
            showContactPickError(exception);
        }
    }

    private void onContactPicked(PickedContact contact) {
        selectedContact = contact;
        showSelectedContactConfirmation(contact);
    }

    private void showManualContactDialog() {
        if (!isFragmentViewActive() || addContactDialog != null) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_manual_emergency_contact, null);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(false);

        EditText etContactName = dialogView.findViewById(R.id.et_contact_name);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> {
            clearSelectedContact();
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> {
            String name = emergencyContactValidator.trim(etContactName.getText().toString());
            String phoneNumber = emergencyContactValidator.trim(etPhoneNumber.getText().toString());
            if (!validateContactFields(name, phoneNumber, etContactName, etPhoneNumber)) {
                return;
            }
            PickedContact manualContact = new PickedContact("", name, phoneNumber);
            selectedContact = manualContact;
            saveEmergencyContact(manualContact, dialog, btnSave, btnCancel);
        });
        dialog.setOnCancelListener(dialogInterface -> clearSelectedContact());
        dialog.setOnDismissListener(dialogInterface -> {
            if (addContactDialog == dialog) {
                addContactDialog = null;
            }
            isInsertInProgress = false;
        });

        addContactDialog = dialog;
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showSelectedContactConfirmation(PickedContact contact) {
        if (!isFragmentViewActive()) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_emergency_contact, null);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(false);

        TextView tvSelectedName = dialogView.findViewById(R.id.tv_selected_contact_name);
        TextView tvSelectedPhoneNumber = dialogView.findViewById(R.id.tv_selected_phone_number);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        tvSelectedName.setText(emergencyContactValidator.trim(contact.getName()));
        tvSelectedPhoneNumber.setText(emergencyContactValidator.trim(contact.getPhoneNumber()));

        btnCancel.setOnClickListener(v -> {
            clearSelectedContact();
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> saveEmergencyContact(contact, dialog, btnSave, btnCancel));
        dialog.setOnCancelListener(dialogInterface -> clearSelectedContact());
        dialog.setOnDismissListener(dialogInterface -> {
            if (addContactDialog == dialog) {
                addContactDialog = null;
            }
            isInsertInProgress = false;
        });

        addContactDialog = dialog;
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void saveEmergencyContact(
            PickedContact contact,
            Dialog dialog,
            MaterialButton btnSave,
            MaterialButton btnCancel
    ) {
        if (isInsertInProgress) {
            return;
        }

        isInsertInProgress = true;
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);
        if (addContactDialog != null) {
            addContactDialog.setCancelable(false);
        }

        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (!isFragmentViewActive() || addContactDialog != dialog || contact != selectedContact) {
                    return;
                }

                if (!emergencyContactValidator.isValidPickedContact(contact)) {
                    finishFailedInsert(btnSave, btnCancel, R.string.error_read_selected_contact);
                    clearSelectedContact();
                    dismissAddContactDialog();
                    return;
                }
                if (emergencyContactValidator.isContactListFull(contacts)) {
                    finishFailedInsert(btnSave, btnCancel, R.string.max_emergency_contacts_message);
                    clearSelectedContact();
                    dismissAddContactDialog();
                    return;
                }
                if (emergencyContactValidator.isDuplicate(contact, contacts)) {
                    finishFailedInsert(btnSave, btnCancel, R.string.duplicate_emergency_contact_message);
                    clearSelectedContact();
                    dismissAddContactDialog();
                    return;
                }

                int priority = emergencyContactValidator.calculateNextPriority(contacts);
                EmergencyContact emergencyContact = buildEmergencyContact(contact, priority);
                insertEmergencyContact(emergencyContact, btnSave, btnCancel);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedInsert(btnSave, btnCancel, R.string.unable_add_emergency_contact);
            }
        });
    }

    private boolean validateContactFields(
            String name,
            String phoneNumber,
            EditText etContactName,
            EditText etPhoneNumber
    ) {
        etContactName.setError(null);
        etPhoneNumber.setError(null);

        boolean isValid = true;
        if (name.isEmpty()) {
            etContactName.setError(getString(R.string.contact_name_required));
            isValid = false;
        }
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError(getString(R.string.phone_number_required));
            isValid = false;
        } else if (!emergencyContactValidator.isValidPhoneNumber(phoneNumber)) {
            etPhoneNumber.setError(getString(R.string.invalid_phone_number));
            isValid = false;
        }
        return isValid;
    }

    private EmergencyContact buildEmergencyContact(PickedContact contact, int priority) {
        EmergencyContact emergencyContact = new EmergencyContact();
        emergencyContact.setContactId(emergencyContactValidator.trim(contact.getContactId()));
        emergencyContact.setName(emergencyContactValidator.trim(contact.getName()));
        emergencyContact.setPhoneNumber(emergencyContactValidator.trim(contact.getPhoneNumber()));
        emergencyContact.setRelationship("");
        emergencyContact.setPriority(priority);
        return emergencyContact;
    }

    private void insertEmergencyContact(
            EmergencyContact emergencyContact,
            MaterialButton btnSave,
            MaterialButton btnCancel
    ) {
        emergencyContactRepository.insert(emergencyContact, new EmergencyContactRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isFragmentViewActive()) {
                    return;
                }
                isInsertInProgress = false;
                clearSelectedContact();
                dismissAddContactDialog();
                loadEmergencyContacts();
                Toast.makeText(requireContext(), R.string.emergency_contact_added_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                finishFailedInsert(btnSave, btnCancel, R.string.unable_add_emergency_contact);
            }
        });
    }

    private void finishFailedInsert(MaterialButton btnSave, MaterialButton btnCancel, int messageRes) {
        isInsertInProgress = false;
        btnSave.setEnabled(true);
        btnCancel.setEnabled(true);
        if (addContactDialog != null) {
            addContactDialog.setCancelable(true);
        }
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private void dismissAddContactDialog() {
        if (addContactDialog != null && addContactDialog.isShowing()) {
            addContactDialog.dismiss();
        }
    }

    private void clearSelectedContact() {
        selectedContact = null;
    }

    private void showContactPermissionDeniedMessage() {
        if (!isFragmentViewActive()) {
            return;
        }
        int messageRes = shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                ? R.string.contact_permission_required
                : R.string.contact_permission_required_settings;
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private void showContactPickError(ContactPickerManager.ContactPickException exception) {
        if (!isFragmentViewActive()) {
            return;
        }
        int messageRes = exception.getError() == ContactPickerManager.ContactPickError.MISSING_PHONE_NUMBER
                ? R.string.contact_missing_phone_number
                : R.string.error_read_selected_contact;
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private void loadEmergencyContacts() {
        emergencyContactRepository.getAll(new EmergencyContactRepository.ContactsCallback() {
            @Override
            public void onSuccess(List<EmergencyContact> contacts) {
                if (!isFragmentViewActive()) {
                    return;
                }
                renderContacts(contacts);
            }

            @Override
            public void onError(Exception exception) {
                if (!isFragmentViewActive()) {
                    return;
                }
                showLoadingError();
            }
        });
    }

    private void renderContacts(List<EmergencyContact> contacts) {
        emergencyContactAdapter.submitList(contacts);
        if (contacts == null || contacts.isEmpty()) {
            showEmptyState();
        } else {
            showContentState();
        }
    }

    private void showEmptyState() {
        rvEmergencyContacts.setVisibility(View.GONE);
        tvEmptyContacts.setText(R.string.empty_contacts);
        tvEmptyContacts.setVisibility(View.VISIBLE);
    }

    private void showContentState() {
        tvEmptyContacts.setVisibility(View.GONE);
        rvEmergencyContacts.setVisibility(View.VISIBLE);
    }

    private void showLoadingError() {
        rvEmergencyContacts.setVisibility(View.GONE);
        tvEmptyContacts.setVisibility(View.VISIBLE);
        tvEmptyContacts.setText(R.string.error_load_emergency_contacts);
        Toast.makeText(requireContext(), R.string.error_load_emergency_contacts, Toast.LENGTH_SHORT).show();
    }

    private void resetMicroAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (btnMicro != null) {
            btnMicro.setScaleX(1f);
            btnMicro.setScaleY(1f);
        }
    }

    private boolean isFragmentViewActive() {
        return !isViewDestroyed && isAdded() && getView() != null;
    }
    
    @Override
    public void onDestroyView() {
        isViewDestroyed = true;
        dismissAddContactDialog();
        dismissEditContactDialog();
        dismissDeleteContactDialog();
        clearSelectedContact();
        if (speechRecognizerManager != null) {
            speechRecognizerManager.destroy();
            speechRecognizerManager = null;
        }
        if (ttsManager != null) {
            ttsManager.setAssistantListening(false);
        }
        mainHandler.removeCallbacksAndMessages(null);
        resetMicroAnimation();
        isInsertInProgress = false;
        isUpdateInProgress = false;
        isDeleteInProgress = false;
        rvEmergencyContacts = null;
        tvEmptyContacts = null;
        btnAddContact = null;
        tvCommand = null;
        btnMicro = null;
        pulseAnimator = null;
        voiceCommandDispatcher = null;
        ttsManager = null;
        super.onDestroyView();
    }
}
