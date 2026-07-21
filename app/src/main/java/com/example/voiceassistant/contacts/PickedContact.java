package com.example.voiceassistant.contacts;

public final class PickedContact {

    private final String contactId;
    private final String name;
    private final String phoneNumber;

    public PickedContact(String contactId, String name, String phoneNumber) {
        this.contactId = contactId;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getContactId() {
        return contactId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
