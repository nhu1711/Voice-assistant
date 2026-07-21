package com.example.voiceassistant.contacts;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactPickerManager {

    public PickedContact resolvePickedContact(ContentResolver contentResolver, Uri selectedContactUri)
            throws ContactPickException {
        if (contentResolver == null || selectedContactUri == null) {
            throw new ContactPickException(ContactPickError.INVALID_SELECTION);
        }

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    selectedContactUri,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );

            if (cursor == null || !cursor.moveToFirst()) {
                throw new ContactPickException(ContactPickError.INVALID_SELECTION);
            }

            int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (contactIdIndex == -1 || nameIndex == -1 || phoneNumberIndex == -1) {
                throw new ContactPickException(ContactPickError.INVALID_SELECTION);
            }

            String contactId = trim(cursor.getString(contactIdIndex));
            String name = trim(cursor.getString(nameIndex));
            String phoneNumber = trim(cursor.getString(phoneNumberIndex));
            if (isBlank(phoneNumber)) {
                throw new ContactPickException(ContactPickError.MISSING_PHONE_NUMBER);
            }

            return new PickedContact(contactId, name, phoneNumber);
        } catch (SecurityException exception) {
            throw new ContactPickException(ContactPickError.PERMISSION_DENIED);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum ContactPickError {
        INVALID_SELECTION,
        MISSING_PHONE_NUMBER,
        PERMISSION_DENIED
    }

    public static class ContactPickException extends Exception {
        private final ContactPickError error;

        public ContactPickException(ContactPickError error) {
            this.error = error;
        }

        public ContactPickError getError() {
            return error;
        }
    }
}
