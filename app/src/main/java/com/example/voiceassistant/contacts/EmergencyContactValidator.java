package com.example.voiceassistant.contacts;

import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.List;

public class EmergencyContactValidator {

    public boolean isContactListFull(List<EmergencyContact> contacts) {
        return contacts != null && contacts.size() >= AppConstants.MAX_EMERGENCY_CONTACTS;
    }

    public boolean isDuplicate(PickedContact pickedContact, List<EmergencyContact> contacts) {
        return isDuplicate(pickedContact, contacts, 0);
    }

    public boolean isDuplicate(PickedContact pickedContact, List<EmergencyContact> contacts, int ignoredContactId) {
        if (pickedContact == null || contacts == null) {
            return false;
        }

        String pickedContactId = trim(pickedContact.getContactId());
        String pickedPhoneNumber = normalizePhoneNumber(pickedContact.getPhoneNumber());

        for (EmergencyContact contact : contacts) {
            if (contact == null) {
                continue;
            }
            if (ignoredContactId > 0 && contact.getId() == ignoredContactId) {
                continue;
            }

            String savedContactId = trim(contact.getContactId());
            if (!isBlank(pickedContactId) && pickedContactId.equals(savedContactId)) {
                return true;
            }

            String savedPhoneNumber = normalizePhoneNumber(contact.getPhoneNumber());
            if (!isBlank(pickedPhoneNumber) && pickedPhoneNumber.equals(savedPhoneNumber)) {
                return true;
            }
        }

        return false;
    }

    public int calculateNextPriority(List<EmergencyContact> contacts) {
        int currentSize = contacts == null ? 0 : contacts.size();
        return Math.min(currentSize + 1, AppConstants.MAX_EMERGENCY_CONTACTS);
    }

    public boolean isValidPickedContact(PickedContact pickedContact) {
        return pickedContact != null
                && !isBlank(pickedContact.getName())
                && isValidPhoneNumber(pickedContact.getPhoneNumber());
    }

    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String trimmedPhoneNumber = phoneNumber.trim();
        boolean hasLeadingPlus = trimmedPhoneNumber.startsWith("+");
        String normalizedDigits = trimmedPhoneNumber
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace("+", "");
        return hasLeadingPlus ? "+" + normalizedDigits : normalizedDigits;
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        String trimmedPhoneNumber = trim(phoneNumber);
        if (isBlank(trimmedPhoneNumber)) {
            return false;
        }

        int digitCount = 0;
        for (int i = 0; i < trimmedPhoneNumber.length(); i++) {
            char current = trimmedPhoneNumber.charAt(i);
            if (Character.isDigit(current)) {
                digitCount++;
                continue;
            }
            if (current == '+' && i == 0) {
                continue;
            }
            if (current == ' ' || current == '-' || current == '(' || current == ')') {
                continue;
            }
            return false;
        }
        return digitCount >= 7 && digitCount <= 15;
    }

    public String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
