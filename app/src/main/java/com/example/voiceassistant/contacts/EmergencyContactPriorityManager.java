package com.example.voiceassistant.contacts;

import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmergencyContactPriorityManager {

    public List<EmergencyContact> buildUpdatedContacts(
            List<EmergencyContact> contacts,
            EmergencyContact targetContact,
            String relationship,
            int newPriority
    ) {
        return buildUpdatedContacts(
                contacts,
                targetContact,
                targetContact.getName(),
                targetContact.getPhoneNumber(),
                relationship,
                newPriority
        );
    }

    public List<EmergencyContact> buildUpdatedContacts(
            List<EmergencyContact> contacts,
            EmergencyContact targetContact,
            String name,
            String phoneNumber,
            String relationship,
            int newPriority
    ) {
        List<EmergencyContact> sortedContacts = copyAndSortContacts(contacts);
        EmergencyContact targetCopy = null;
        String originalName = targetContact.getName();
        String originalPhoneNumber = targetContact.getPhoneNumber();
        String originalRelationship = targetContact.getRelationship();

        for (int i = sortedContacts.size() - 1; i >= 0; i--) {
            EmergencyContact contact = sortedContacts.get(i);
            if (contact.getId() == targetContact.getId()) {
                targetCopy = contact;
                sortedContacts.remove(i);
                break;
            }
        }

        if (targetCopy == null) {
            return Collections.emptyList();
        }

        targetCopy.setName(name);
        targetCopy.setPhoneNumber(phoneNumber);
        targetCopy.setRelationship(relationship);
        int safePriority = Math.max(1, Math.min(newPriority, sortedContacts.size() + 1));
        sortedContacts.add(safePriority - 1, targetCopy);

        List<EmergencyContact> updates = new ArrayList<>();
        for (int i = 0; i < sortedContacts.size(); i++) {
            EmergencyContact contact = sortedContacts.get(i);
            int expectedPriority = i + 1;
            boolean isTarget = contact.getId() == targetContact.getId();
            boolean priorityChanged = contact.getPriority() != expectedPriority;
            boolean nameChanged = isTarget && !areEqual(originalName, name);
            boolean phoneNumberChanged = isTarget && !areEqual(originalPhoneNumber, phoneNumber);
            boolean relationshipChanged = isTarget && !areEqual(originalRelationship, relationship);
            if (priorityChanged || nameChanged || phoneNumberChanged || relationshipChanged) {
                contact.setPriority(expectedPriority);
                updates.add(contact);
            }
        }

        return updates;
    }

    public int clampPriority(int priority, int contactCount) {
        if (contactCount <= 0) {
            return 1;
        }
        return Math.max(1, Math.min(priority, contactCount));
    }

    public List<EmergencyContact> normalizeAfterDelete(List<EmergencyContact> contacts) {
        List<EmergencyContact> sortedContacts = copyAndSortContacts(contacts);
        List<EmergencyContact> updates = new ArrayList<>();

        for (int i = 0; i < sortedContacts.size(); i++) {
            EmergencyContact contact = sortedContacts.get(i);
            int expectedPriority = i + 1;
            if (contact.getPriority() != expectedPriority) {
                contact.setPriority(expectedPriority);
                updates.add(contact);
            }
        }

        return updates;
    }

    private List<EmergencyContact> copyAndSortContacts(List<EmergencyContact> contacts) {
        List<EmergencyContact> copies = new ArrayList<>();
        if (contacts == null) {
            return copies;
        }

        for (EmergencyContact contact : contacts) {
            if (contact != null) {
                copies.add(copyContact(contact));
            }
        }

        Collections.sort(copies, (first, second) -> Integer.compare(first.getPriority(), second.getPriority()));
        return copies;
    }

    private EmergencyContact copyContact(EmergencyContact source) {
        EmergencyContact copy = new EmergencyContact();
        copy.setId(source.getId());
        copy.setContactId(source.getContactId());
        copy.setName(source.getName());
        copy.setPhoneNumber(source.getPhoneNumber());
        copy.setRelationship(source.getRelationship());
        copy.setPriority(source.getPriority());
        return copy;
    }

    private boolean areEqual(String first, String second) {
        if (first == null) {
            return second == null || second.isEmpty();
        }
        if (second == null) {
            return first.isEmpty();
        }
        return first.equals(second);
    }
}
