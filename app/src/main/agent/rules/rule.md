 MỤC ĐÍCH
File này định nghĩa các quy tắc BẮT BUỘC mà mọi AI Agent (và lập trình viên) KHÔNG ĐƯỢC PHÉP VI PHẠM khi sinh mã nguồn cho dự án Voice Assistant. Mục tiêu là đảm bảo:

Code có cấu trúc rõ ràng, nhất quán


Tuân thủ kiến trúc đã thiết kế


Không vi phạm các quy tắc nghiệp vụ (Business Rules)


Dễ bảo trì và mở rộng


📁 QUY TẮC 1: CẤU TRÚC THƯ MỤC (Package Structure)
1.1. Thư mục gốc bắt buộc
text
com.group08.voiceassistant/├── ui/                    # CHỈ chứa UI components (Activities, Fragments, Adapters)├── data/                  # CHỈ chứa Data Layer (Database, Repository, Preferences)├── services/              # CHỈ chứa Android Services (Foreground, Accessibility)├── receivers/             # CHỈ chứa Broadcast Receivers├── accessibility/         # CHỈ chứa Accessibility helpers├── speech/                # CHỈ chứa Speech-to-Text logic├── tts/                   # CHỈ chứa Text-to-Speech logic├── contacts/              # CHỈ chứa Contact management├── call/                  # CHỈ chứa Call management├── sms/                   # CHỈ chứa SMS management├── location/              # CHỈ chứa Location management├── detection/             # CHỈ chứa Object Detection logic├── emergency/             # CHỈ chứa SOS/Emergency logic├── permissions/           # CHỈ chứa Permission handling├── utils/                 # CHỈ chứa Utility classes (không chứa business logic)├── constants/             # CHỈ chứa Constants└── VoiceAssistantApp.java # Application class duy nhất ở root
1.2. Quy tắc đặt file
Loại file	Thư mục bắt buộc	Ví dụ
Activity	ui/activities/	MainActivity.java
Fragment	ui/fragments/	HomeFragment.java
Adapter	ui/adapters/	EmergencyContactAdapter.java
Entity	data/database/entity/	EmergencyContact.java
DAO	data/database/dao/	EmergencyContactDao.java
Database	data/database/	AppDatabase.java
Repository	data/repository/	EmergencyContactRepository.java
Preferences	data/preferences/	AppPreferences.java
Service	services/	VoiceAssistantService.java
Broadcast Receiver	receivers/	SystemBroadcastReceiver.java
Manager	Package tương ứng	CallManager.java → call/
Helper	utils/ hoặc package tương ứng	PermissionHelper.java → permissions/
Constants	constants/	AppConstants.java
🚫 CẤM:

Đặt Activity trong ui/ mà không có thư mục con activities/


Đặt file Java ở root package com.group08.voiceassistant/ (ngoại trừ VoiceAssistantApp.java)


Đặt file logic nghiệp vụ trong ui/ hoặc utils/


Đặt nhiều loại file khác nhau trong cùng một thư mục (ví dụ: Activity và Adapter cùng folder)

✅ ĐÚNG:
text
data/database/entity/EmergencyContact.java        ✅data/database/dao/EmergencyContactDao.java        ✅ui/activities/MainActivity.java                   ✅services/VoiceAssistantService.java               ✅
❌ SAI:
text
data/EmergencyContact.java                        ❌ (sai thư mục)ui/MainActivity.java                              ❌ (thiếu thư mục con)VoiceAssistantService.java                        ❌ (ở root)MainActivity.java + EmergencyContact.java cùng 1 folder ❌ (lẫn lộn)

🏗️ QUY TẮC 2: KIẾN TRÚC PHÂN TẦNG (Layered Architecture)
2.1. Luồng gọi BẮT BUỘC
text
Activity/Fragment → Repository → DAO → Room Database                ↓Activity/Fragment → Manager → Android APIs                ↓Activity/Fragment → ViewModel (nếu dùng MVVM)
2.2. Quy tắc lớp (Class Rules)
Layer	Được phép gọi	KHÔNG được phép gọi
Activity/Fragment	Repository, Manager, ViewModel	DAO trực tiếp, Database trực tiếp
Repository	DAO	Activity, Manager (có thể nhận Manager qua dependency)
DAO	Database (Room)	Repository, Activity
Manager	Android APIs, Repository (nếu cần)	Activity trực tiếp (phải qua Controller/UseCase)
Service	Manager, Repository	Activity trực tiếp
🚫 CẤM:

Activity gọi DAO trực tiếp


Activity truy vấn Room Database trực tiếp


Business logic (gọi điện, lấy pin, v.v.) đặt trong Activity


Manager gọi Activity (gây phụ thuộc vòng)

✅ ĐÚNG:
java
// MainActivity.javarepository.getEmergencyContacts().observe(this, contacts -> {    // Update UI});// EmergencyContactRepository.javapublic LiveData<List<EmergencyContact>> getEmergencyContacts() {    return contactDao.getActiveContacts();}
❌ SAI:
java
// MainActivity.java - SAIAppDatabase db = AppDatabase.getInstance(this);db.contactDao().getActiveContacts(); // ❌ Activity gọi DAO trực tiếp// MainActivity.java - SAIcallManager.makeCall("mẹ"); // ❌ Nếu không qua UseCase/Controller

📦 QUY TẮC 3: DATABASE (Room)
3.1. Bảng DUY NHẤT được phép tạo
CHỈ TẠO 1 BẢNG: emergency_contacts
Trường	Kiểu	Bắt buộc	Mô tả
id	int	✅	Khóa chính, auto-increment
contactId	String	✅	ID từ Content Provider
name	String	✅	Tên liên hệ
phoneNumber	String	✅	Số điện thoại
relationship	String	❌	Quan hệ (Mẹ, Bố, ...)
priority	int	✅	1: cao nhất
🚫 CẤM:

Tạo bảng khác ngoài emergency_contacts


Lưu toàn bộ danh bạ thiết bị vào database


Lưu lịch sử lệnh thoại (không yêu cầu trong SRS)


Thêm các trường: isPrimary, enableCall, enableSms, isActive, createdAt (trừ khi có yêu cầu thay đổi từ giảng viên)

3.2. Quy tắc truy vấn
BẮT BUỘC có các phương thức DAO:
java
@Daopublic interface EmergencyContactDao {    @Insert    void insert(EmergencyContact contact);        @Update    void update(EmergencyContact contact);        @Delete    void delete(EmergencyContact contact);        @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC")    List<EmergencyContact> getAll();        @Query("SELECT * FROM emergency_contacts WHERE priority = 1 LIMIT 1")    EmergencyContact getPrimaryContact();}
KHÔNG THÊM các method khác trừ khi được yêu cầu cụ thể.

📋 QUY TẮC 4: TUÂN THỦ BUSINESS RULES (BR)
4.1. BR-01: Quyền ghi âm
BẮT BUỘC kiểm tra trước khi gọi SpeechRecognizer:
java
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)     != PackageManager.PERMISSION_GRANTED) {    // Yêu cầu cấp quyền, KHÔNG tự động gọi SpeechRecognizer    ActivityCompat.requestPermissions(activity, new String[]{RECORD_AUDIO}, REQUEST_CODE);    return; // Dừng xử lý}// Chỉ gọi SpeechRecognizer sau khi đã có quyền
4.2. BR-02: Quyền gọi điện
BẮT BUỘC kiểm tra trước khi gọi Intent.ACTION_CALL:
java
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)     != PackageManager.PERMISSION_GRANTED) {    // Yêu cầu cấp quyền, KHÔNG thực hiện cuộc gọi    ActivityCompat.requestPermissions(activity, new String[]{CALL_PHONE}, REQUEST_CODE);    return;}// Chỉ thực hiện cuộc gọi sau khi đã có quyền
4.3. BR-03: Chỉ gọi khi tìm thấy liên hệ
BẮT BUỘC kiểm tra kết quả tìm kiếm trước khi gọi:
java
Contact contact = findContactInSystem(contactName);if (contact == null) {    // Đọc: "Không tìm thấy [tên] trong danh bạ"    ttsManager.speak("Không tìm thấy " + contactName + " trong danh bạ");    return; // KHÔNG thực hiện cuộc gọi}// Chỉ gọi khi contact != nullmakeCall(contact.getPhoneNumber());
4.4. BR-04: Phản hồi bằng giọng nói
BẮT BUỘC mọi phản hồi đều phải đọc bằng TTS và hiển thị văn bản:
java
// SAU MỖI XỬ LÝ LỆNHString response = "Đã thực hiện lệnh thành công";ttsManager.speak(response);  // BẮT BUỘC đọctextViewResult.setText(response); // BẮT BUỘC hiển thị
4.5. BR-05: Chỉ gửi SMS đến liên hệ đã đăng ký
BẮT BUỘC kiểm tra danh sách trong Room Database:
java
List<EmergencyContact> contacts = repository.getAllContacts(); // TỪ ROOMfor (EmergencyContact contact : contacts) {    sendSms(contact.getPhoneNumber(), message); // CHỈ gửi đến contact trong Room}// KHÔNG gửi SMS đến số không có trong Room
4.6. BR-06: Chỉ gửi vị trí khi SOS kích hoạt
BẮT BUỘC chỉ lấy GPS khi SOS được kích hoạt:
java
public void triggerSOS() {    // CHỈ gọi getLocation() trong phương thức triggerSOS()    Location location = locationManager.getCurrentLocation();    sendLocationSMS(location);}// KHÔNG gọi getLocation() ở nơi khác

🚫 QUY TẮC 5: NHỮNG ĐIỀU CẤM TUYỆT ĐỐI
5.1. Cấm tạo các file không thuộc phạm vi
File bị cấm	Lý do
CommandHistory.java	Không có trong SRS
UserSettingsEntity.java	Dùng SharedPreferences, không lưu Room
VoiceCommand.java	Không có trong SRS
FeedbackLog.java	Không có trong SRS
SOSLog.java	Không có trong SRS
Bất kỳ file nào không được định nghĩa trong cấu trúc thư mục	Vi phạm phạm vi dự án
5.2. Cấm sử dụng các thư viện chưa được phê duyệt
CHỈ ĐƯỢC PHÉP DÙNG:

AndroidX libraries


Room Database


CameraX


TensorFlow Lite


Google Play Services (Location)


Dexter (permission)

CẤM DÙNG:

Firebase (không có server)


Retrofit/OkHttp (không có API)


Glide/Picasso (có thể dùng sau, nhưng chưa cần)


RxJava (có thể dùng sau, nhưng chưa cần)


Dagger/Hilt (có thể dùng sau, nhưng chưa cần)

5.3. Cấm đặt tên không nhất quán
Loại	Quy tắc	Ví dụ
Activity	Tên + Activity	MainActivity
Fragment	Tên + Fragment	HomeFragment
Adapter	Tên + Adapter	ContactAdapter
Repository	Tên + Repository	ContactRepository
DAO	Tên + Dao	ContactDao
Entity	Tên + Entity	ContactEntity
Manager	Tên + Manager	CallManager
Helper	Tên + Helper	PermissionHelper
Constants	AppConstants.java	AppConstants
Exception	Tên + Exception	ContactNotFoundException

🔐 QUY TẮC 6: PERMISSION
6.1. Danh sách permission phải khai báo trong AndroidManifest
xml
<uses-permission android:name="android.permission.RECORD_AUDIO" /><uses-permission android:name="android.permission.CALL_PHONE" /><uses-permission android:name="android.permission.READ_CONTACTS" /><uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /><uses-permission android:name="android.permission.SEND_SMS" /><uses-permission android:name="android.permission.FOREGROUND_SERVICE" /><uses-permission android:name="android.permission.CAMERA" /><uses-permission android:name="android.permission.INTERNET" />
6.2. Bắt buộc kiểm tra trước khi dùng
KHÔNG được phép gọi API mà không kiểm tra permission:
java
// ❌ SAIspeechRecognizer.startListening(); // Không kiểm tra RECORD_AUDIO// ✅ ĐÚNGif (hasPermission(Manifest.permission.RECORD_AUDIO)) {    speechRecognizer.startListening();} else {    requestPermission(Manifest.permission.RECORD_AUDIO);
QUY TẮC 10: KIỂM TRA TRƯỚC KHI COMMIT
Trước khi commit code, AI Agent PHẢI kiểm tra:
Hạng mục	Kiểm tra
Thư mục đúng	File có đúng thư mục không?
Tên file đúng	Có đúng naming convention không?
Không có file thừa	Có file nào không thuộc dự án không?
Tuân thủ BR	Có kiểm tra BR-01 đến BR-06 không?
Permission	Có kiểm tra quyền trước khi gọi API không?
Log	Có log error ở catch block không?
TTS	Mọi phản hồi đều có TTS không?
