🎙️ GIỚI THIỆU ỨNG DỤNG VOICE ASSISTANT
📱 TỔNG QUAN
Voice Assistant là một ứng dụng Android được phát triển nhằm hỗ trợ người cao tuổi và người khiếm thị trong việc sử dụng điện thoại thông minh. Ứng dụng cho phép người dùng thực hiện các tác vụ hàng ngày thông qua giọng nói và nhận phản hồi bằng âm thanh, từ đó giảm sự phụ thuộc vào thao tác cảm ứng và nâng cao khả năng tiếp cận công nghệ.

⚙️ CÁC TÍNH NĂNG CHÍNH
1. Tương tác bằng giọng nói
Người dùng nhấn nút Micro để kích hoạt nhận diện giọng nói. Hệ thống sử dụng Android SpeechRecognizer để chuyển giọng nói thành văn bản, hiển thị kết quả trên màn hình và phân tích lệnh bằng CommandParser. Mọi phản hồi đều được đọc lại bằng TextToSpeech bằng cả hai ngôn ngữ Tiếng Việt và Tiếng Anh.

2. Gọi điện thoại
Người dùng có thể nói câu lệnh như "Gọi cho mẹ" hoặc "Call Mom". Ứng dụng sẽ tìm kiếm liên hệ trong danh bạ thiết bị và thực hiện cuộc gọi sau khi được cấp quyền.

3. Đọc thời gian và pin
Các lệnh như "Mấy giờ rồi?", "What time is it?", "Pin còn bao nhiêu?" hoặc "Battery level" sẽ kích hoạt hệ thống đọc thời gian hiện tại hoặc phần trăm pin, kèm theo thông tin về trạng thái sạc hoặc pin yếu.

4. Quản lý liên hệ khẩn cấp
Người dùng có thể thêm, sửa hoặc xóa nhiều liên hệ khẩn cấp với mức độ ưu tiên. Dữ liệu được lưu trữ cục bộ trong Room Database thông qua Repository và DAO.

5. Tính năng SOS khẩn cấp
SOS có thể được kích hoạt bằng giọng nói (ví dụ: "Cứu tôi", "Help me") hoặc qua giao diện. Hệ thống hiển thị đếm ngược 5 giây để người dùng có thể hủy. Nếu không hủy, ứng dụng sẽ tự động gọi đến liên hệ có ưu tiên cao nhất, lấy vị trí GPS hiện tại và gửi tin nhắn SMS với đường dẫn Google Maps đến tất cả các liên hệ khẩn cấp.

6. Nhận diện vật thể xung quanh
Ứng dụng sử dụng CameraX và mô hình EfficientDet Lite (MediaPipe/TensorFlow Lite) để nhận diện vật thể trong khung hình camera. Kết quả được hiển thị và đọc thành tiếng, giúp người khiếm thị nhận biết các vật thể xung quanh.

7. Chạy nền và cảnh báo hệ thống
Khi bật chế độ nền, ứng dụng chạy Foreground Service để duy trì hoạt động và nhận các sự kiện hệ thống như pin yếu hoặc mất kết nối mạng thông qua BroadcastReceiver.

8. Đọc thông báo
Ứng dụng có thể đọc thông báo từ các ứng dụng được người dùng chọn (ví dụ: Zalo, Messenger) sau khi được cấp quyền Notification Access trong cài đặt hệ thống.

9. Giao diện thân thiện
Giao diện được thiết kế với nút bấm lớn, nhãn rõ ràng, màu sắc tương phản cao, thanh điều hướng dưới cùng, hiệu ứng hoạt hình khi lắng nghe và hỗ trợ song ngữ để giảm khó khăn về thị giác và thao tác.

🗂️ CÁC NHÓM LỆNH ĐƯỢC HỖ TRỢ
Nhóm lệnh	Ví dụ	Phản hồi hệ thống
Gọi điện	"Gọi cho mẹ" / "Call Mom"	Tìm liên hệ và thực hiện cuộc gọi
Thông tin	"Mấy giờ rồi?" / "Battery level"	Đọc thời gian hiện tại hoặc mức pin
SOS	"Cứu tôi" / "Help me"	Bắt đầu quy trình SOS có thể hủy
Nhận diện	"Nhận diện vật thể" / "Detect objects"	Mở camera và thông báo vật thể
Điều hướng	"Mở cài đặt" / "Trở về trang chủ"	Điều hướng đến màn hình tương ứng
Hỗ trợ	"Hướng dẫn" / "Help"	Đọc danh sách lệnh có thể sử dụng
Nhắc lại	"Nhắc lại" / "Repeat"	Đọc lại phản hồi cuối cùng
Thông báo	"Đọc thông báo" / "Read notifications"	Đọc các thông báo đã lưu gần đây
🏗️ KIẾN TRÚC ỨNG DỤNG
Ứng dụng được xây dựng theo mô hình ba tầng:

Tầng trình bày (Presentation Layer): Chứa các Activity, Fragment và Adapter để hiển thị giao diện và tương tác với người dùng.

Tầng logic nghiệp vụ (Business Logic Layer): Quản lý các chức năng chính như nhận diện giọng nói, TTS, gọi điện, SOS, định vị, thông báo và nhận diện vật thể.

Tầng dữ liệu (Data Layer): Quản lý lưu trữ dữ liệu cục bộ thông qua Room Database, Repository, SharedPreferences và Content Provider.

✅ KẾT QUẢ ĐẠT ĐƯỢC
Nhóm đã hoàn thành một bản prototype Android hoạt động, tích hợp đầy đủ các tính năng:

Nhận diện giọng nói và TTS song ngữ

Gọi điện từ danh bạ

Đọc thời gian và pin

Quản lý liên hệ khẩn cấp

Quy trình SOS với đếm ngược, gọi điện, GPS và SMS

Nhận diện vật thể thời gian thực

Cảnh báo hệ thống và đọc thông báo

Ứng dụng được kiểm thử trên thiết bị Android thực tế để đảm bảo các chức năng nhạy cảm như camera, micro, cuộc gọi, SMS, GPS và quyền truy cập thông báo hoạt động chính xác. Mã nguồn được tổ chức rõ ràng thành các Manager, Service, Repository, DAO, Activity và Fragment giúp dễ dàng bảo trì và mở rộng.

🎯 LỢI ÍCH VÀ HẠN CHẾ
Lợi ích
Giao diện đơn giản, dễ sử dụng cho người cao tuổi và người khiếm thị

Nút bấm lớn và phản hồi giọng nói giúp giảm khó khăn trong thao tác

Tính năng SOS tích hợp nhiều bước (ưu tiên, đếm ngược, gọi, GPS, SMS) chỉ trong một quy trình

Dữ liệu và mô hình nhận diện được lưu trữ cục bộ, không cần máy chủ

Kiến trúc phân tầng giúp dễ bảo trì và phát triển

Hạn chế
Chưa hỗ trợ kích hoạt qua nút trợ năng Accessibility hoặc từ khóa đánh thức "luôn lắng nghe"

Vẫn phải nhấn nút Micro để bắt đầu nhận diện giọng nói

Một số tính năng phụ thuộc vào quyền, SIM, GPS, camera và quyền truy cập thông báo

Nhận diện giọng nói và vật thể có thể bị ảnh hưởng bởi tiếng ồn, ánh sáng, góc camera, hiệu suất thiết bị và độ chính xác của mô hình

Tóm lại: Voice Assistant là một ứng dụng trợ lý giọng nói chuyên biệt, hướng đến việc hỗ trợ người cao tuổi và người khiếm thị trong các tác vụ hàng ngày, với các tính năng thiết thực và giao diện thân thiện. 🚀

viết cho tôi read me của voice assistant
🗣️ Voice Assistant for Elderly and Visually Impaired People
https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white
https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white
https://img.shields.io/badge/License-MIT-green.svg

📱 Giới thiệu
Voice Assistant là một ứng dụng Android được phát triển nhằm hỗ trợ người cao tuổi và người khiếm thị trong việc sử dụng điện thoại thông minh. Ứng dụng cho phép người dùng thực hiện các tác vụ hàng ngày thông qua giọng nói và nhận phản hồi bằng âm thanh, từ đó giảm sự phụ thuộc vào thao tác cảm ứng và nâng cao khả năng tiếp cận công nghệ.

🎯 Mục tiêu
Đơn giản hóa - Giao diện lớn, rõ ràng, dễ sử dụng

Trợ năng - Hỗ trợ người khiếm thị bằng giọng nói

An toàn - Tính năng SOS khẩn cấp với gửi vị trí GPS

Tiện lợi - Thực hiện cuộc gọi, kiểm tra thời gian và pin bằng giọng nói

Nhận diện - Phát hiện vật thể xung quanh để hỗ trợ di chuyển

✨ Tính năng chính
🎤 Tương tác bằng giọng nói
Nhận diện giọng nói bằng Android SpeechRecognizer

Phản hồi bằng giọng nói qua Text-to-Speech (TTS)

Hỗ trợ cả hai ngôn ngữ: Tiếng Việt và Tiếng Anh

Hiển thị văn bản nhận diện trên màn hình

📞 Gọi điện thoại
Ra lệnh bằng giọng nói: "Gọi cho mẹ" / "Call Mom"

Tìm kiếm liên hệ trong danh bạ thiết bị

Thực hiện cuộc gọi qua Intent.ACTION_CALL

⏰ Đọc thời gian và pin
"Mấy giờ rồi?" / "What time is it?" → Đọc thời gian hiện tại

"Pin còn bao nhiêu?" / "Battery level" → Đọc phần trăm pin

Thông báo khi pin yếu (<20%)

🚨 SOS Khẩn cấp
Kích hoạt bằng giọng nói: "Cứu tôi" / "Help me"

Đếm ngược 5 giây để hủy kích hoạt

Gọi đến liên hệ khẩn cấp có mức ưu tiên cao nhất

Gửi SMS với vị trí GPS hiện tại (Google Maps link)

Gửi đến tất cả liên hệ khẩn cấp đã đăng ký

📷 Nhận diện vật thể
Sử dụng CameraX và TensorFlow Lite (EfficientDet Lite)

Nhận diện vật thể xung quanh trong thời gian thực

Đọc tên vật thể bằng giọng nói

Hiển thị tên và độ tin cậy trên màn hình

📱 Quản lý liên hệ khẩn cấp
Thêm, sửa, xóa liên hệ khẩn cấp

Thiết lập mức độ ưu tiên (1: cao nhất)

Lưu trữ trong Room Database

Tối đa 5 liên hệ khẩn cấp

🔔 Đọc thông báo
Đọc thông báo từ các ứng dụng được chọn (Zalo, Messenger...)

Hai chế độ đọc: Đọc đầy đủ và Chỉ thông báo

Yêu cầu quyền Notification Access

⚙️ Chạy nền và cảnh báo
Foreground Service duy trì ứng dụng chạy nền

BroadcastReceiver nhận sự kiện hệ thống:

🔋 Pin yếu (<20%): Đọc cảnh báo

📶 Mất kết nối mạng: Đọc cảnh báo

🔄 Khởi động lại thiết bị: Tự động khởi động service

♿ Accessibility Service
Hỗ trợ người khiếm thị qua nút trợ năng hệ thống

Điều hướng bằng giọng nói

Giao diện chữ lớn, màu sắc tương phản cao

🏗️ Kiến trúc hệ thống
Ứng dụng được phát triển theo mô hình ba tầng (Layered Architecture):

text
┌─────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER (UI)                   │
│          Activities / Fragments / Adapters                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   BUSINESS LOGIC LAYER                      │
│    Services / Managers / Command Handlers / Use Cases       │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                             │
│     Repository / DAO / Room Database / Content Provider     │
└─────────────────────────────────────────────────────────────┘
📦 Cấu trúc thư mục
text
app/src/main/java/com/example/voiceassistant/
│
├── ui/                    # Giao diện người dùng
│   ├── activities/        # Activities (Main, Contacts, Detection...)
│   ├── fragments/         # Fragments (Home, Settings...)
│   └── adapters/          # Adapters
│
├── services/              # Android Services
│   ├── VoiceAssistantService.java       # Foreground Service
│   └── AppNotificationListenerService.java # Notification Listener
│
├── receivers/             # Broadcast Receivers
│   └── SystemBroadcastReceiver.java
│
├── speech/                # Speech-to-Text
│   ├── SpeechRecognizerManager.java
│   ├── CommandParser.java
│   └── VoiceCommandDispatcher.java
│
├── tts/                   # Text-to-Speech
│   └── TTSManager.java
│
├── call/                  # Call Manager
│   └── CallManager.java
│
├── contacts/              # Contact Manager
│   └── ContactManager.java
│
├── emergency/             # SOS Manager
│   └── EmergencyManager.java
│
├── detection/             # Object Detection
│   └── ObjectDetectionManager.java
│
├── location/              # Location Manager
│   └── LocationManagerHelper.java
│
├── battery/               # Battery Manager
│   └── BatteryManagerHelper.java
│
├── sms/                   # SMS Manager
│   └── SmsManagerHelper.java
│
├── data/                  # Data Layer
│   ├── database/          # Room Database
│   │   ├── AppDatabase.java
│   │   ├── dao/
│   │   └── entity/
│   ├── repository/        # Repository Pattern
│   └── preferences/       # SharedPreferences
│
├── permissions/           # Permission Helper
│   └── PermissionHelper.java
│
├── utils/                 # Utilities
│   └── LocaleHelper.java
│
└── constants/             # Constants
    └── AppConstants.java
🛠️ Công nghệ sử dụng
Thành phần	Công nghệ
Ngôn ngữ	Java
IDE	Android Studio
Database cục bộ	Room Database
Lưu cấu hình	SharedPreferences
Speech-to-Text	SpeechRecognizer API
Text-to-Speech	TextToSpeech API
Danh bạ	Content Provider
Camera	CameraX
Object Detection	TensorFlow Lite (EfficientDet Lite)
GPS	Fused Location Provider
SMS	SmsManager
Trợ năng	Accessibility Service
Nền	Foreground Service
Broadcast	Broadcast Receiver
Notification	NotificationListenerService
📋 Yêu cầu hệ thống
Android 8.0 (API 26) trở lên

Quyền cần thiết:

RECORD_AUDIO - Ghi âm

CALL_PHONE - Gọi điện

READ_CONTACTS - Đọc danh bạ

ACCESS_FINE_LOCATION - Lấy vị trí GPS

SEND_SMS - Gửi tin nhắn

CAMERA - Camera

INTERNET - Kết nối mạng

FOREGROUND_SERVICE - Chạy nền

RECEIVE_BOOT_COMPLETED - Khởi động khi máy bật

ACCESS_NETWORK_STATE - Kiểm tra mạng

POST_NOTIFICATIONS - Hiển thị thông báo (Android 13+)

🚀 Cài đặt và chạy
1. Clone dự án
bash
git clone https://github.com/nhu1711/Voice-assistant.git
cd Voice-assistant
2. Mở bằng Android Studio
Mở Android Studio

Chọn Open an Existing Project

Chọn thư mục dự án

3. Build và chạy
bash
# Sync Gradle
Build → Make Project

# Chạy trên thiết bị
Run → Run 'app'
4. Cấp quyền
Ứng dụng sẽ tự động yêu cầu các quyền cần thiết. Một số quyền cần được cấp thủ công:

Notification Access: Vào Cài đặt → Ứng dụng → Voice Assistant → Quyền truy cập thông báo → Bật

Accessibility Service: Vào Cài đặt → Trợ năng → Voice Assistant → Bật

🗂️ Nhóm lệnh hỗ trợ
Nhóm lệnh	Ví dụ	Phản hồi hệ thống
Gọi điện	"Gọi cho mẹ" / "Call Mom"	Tìm liên hệ và thực hiện cuộc gọi
Thông tin	"Mấy giờ rồi?" / "What time is it?"	Đọc thời gian hiện tại
Pin	"Pin còn bao nhiêu?" / "Battery level"	Đọc phần trăm pin
SOS	"Cứu tôi" / "Help me"	Bắt đầu quy trình SOS
Nhận diện	"Nhận diện vật thể" / "Detect objects"	Mở camera và thông báo vật thể
Điều hướng	"Mở cài đặt" / "Open settings"	Mở màn hình cài đặt
Trợ giúp	"Hướng dẫn" / "Help"	Đọc danh sách lệnh
Nhắc lại	"Nhắc lại" / "Repeat"	Đọc lại phản hồi cuối
Đọc thông báo	"Đọc thông báo" / "Read notifications"	Đọc thông báo đã lưu
👥 Thành viên nhóm
STT	MSSV	Họ tên	Vai trò
1	23110065	Mai Trần Thùy Trang	UI/UX, Presentation Layer
2	23110051	Trần Thị Tố Như	Business Logic, Services
3	23110004	Võ Nguyễn Ngọc Bích	Data Layer, Database
