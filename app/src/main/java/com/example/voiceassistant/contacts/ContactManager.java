package com.example.voiceassistant.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.voiceassistant.permissions.PermissionHelper;

/**
 * Quản lý danh bạ thiết bị qua Content Provider
 * FR-04: Truy cập danh bạ để tìm liên hệ
 */
public class ContactManager {
    
    private static final String TAG = "ContactManager";
    private final Context context;
    
    public ContactManager(Context context) {
        this.context = context;
    }
    
    /**
     * Tìm kiếm liên hệ theo tên
     * BR-03: Chỉ gọi khi tìm thấy liên hệ
     * 
     * @param contactName Tên liên hệ cần tìm (ví dụ: "Mẹ")
     * @return ContactInfo nếu tìm thấy, null nếu không
     */
    public ContactInfo findContactByName(String contactName) {
        if (contactName == null || contactName.trim().isEmpty()) {
            Log.w(TAG, "Contact name is empty");
            return null;
        }
        
        // Kiểm tra quyền READ_CONTACTS
        if (!PermissionHelper.hasReadContactsPermission(context)) {
            Log.w(TAG, "No READ_CONTACTS permission");
            return null;
        }
        
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        
        // Tìm kiếm theo tên (không phân biệt hoa thường)
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + contactName + "%"};
        
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                selection,
                selectionArgs,
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                
                if (idIndex != -1 && nameIndex != -1 && numberIndex != -1) {
                    String id = cursor.getString(idIndex);
                    String name = cursor.getString(nameIndex);
                    String phoneNumber = cursor.getString(numberIndex);
                    
                    Log.d(TAG, "Found contact directly: " + name + " - " + phoneNumber);
                    return new ContactInfo(id, name, phoneNumber);
                }
            }
        }
catch (SecurityException e) {
            Log.e(TAG, "Security exception when reading contacts", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        Log.w(TAG, "Contact not found: " + contactName);
        return null;
    }
    
    /**
     * Lấy số điện thoại từ contact ID
     */
    private String getPhoneNumber(String contactId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        
        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{contactId};
        
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                phoneUri,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                selection,
                selectionArgs,
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (numberIndex != -1) {
                    return cursor.getString(numberIndex);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when reading phone", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
    
    /**
     * Lớp chứa thông tin liên hệ
     */
    public static class ContactInfo {
        private final String id;
        private final String name;
        private final String phoneNumber;
        
        public ContactInfo(String id, String name, String phoneNumber) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhoneNumber() { return phoneNumber; }
        
        @Override
        public String toString() {
            return name + " - " + phoneNumber;
        }
    }
}
