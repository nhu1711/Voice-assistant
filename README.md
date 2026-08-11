# 🗣️ Voice Assistant for Elderly and Visually Impaired People

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📱 Giới thiệu

**Voice Assistant** là một ứng dụng Android được phát triển nhằm hỗ trợ **người cao tuổi** và **người khiếm thị** trong việc sử dụng điện thoại thông minh. Ứng dụng cho phép người dùng thực hiện các tác vụ hàng ngày thông qua giọng nói và nhận phản hồi bằng âm thanh, từ đó giảm sự phụ thuộc vào thao tác cảm ứng và nâng cao khả năng tiếp cận công nghệ.

---

## ✨ Tính năng chính

| Tính năng | Mô tả |
|-----------|-------|
| 🎤 **Tương tác giọng nói** | Nhận diện giọng nói bằng SpeechRecognizer, phản hồi bằng TTS, hỗ trợ Tiếng Việt và Tiếng Anh |
| 📞 **Gọi điện thoại** | Ra lệnh "Gọi cho mẹ", tìm kiếm danh bạ và thực hiện cuộc gọi |
| ⏰ **Thời gian và pin** | Hỏi giờ hoặc pin, đọc kết quả bằng giọng nói |
| 🚨 **SOS khẩn cấp** | Kích hoạt bằng giọng nói, đếm ngược 5 giây, gọi và gửi SMS vị trí GPS |
| 📷 **Nhận diện vật thể** | Dùng CameraX và TensorFlow Lite để nhận diện vật thể xung quanh |
| 📱 **Quản lý liên hệ khẩn cấp** | Thêm, sửa, xóa liên hệ, thiết lập mức ưu tiên, lưu trong Room Database |
| 🔔 **Đọc thông báo** | Đọc thông báo từ ứng dụng được chọn (Zalo, Messenger...) |
| ⚙️ **Chạy nền và cảnh báo** | Foreground Service, BroadcastReceiver, cảnh báo pin yếu và mất mạng |
| ♿ **Accessibility** | Hỗ trợ người khiếm thị qua nút trợ năng hệ thống |

---

## 🏗️ Kiến trúc hệ thống

Ứng dụng được phát triển theo mô hình **ba tầng (Layered Architecture)**:

```text
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
## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| **Ngôn ngữ** | Java |
| **IDE** | Android Studio |
| **Database cục bộ** | Room Database |
| **Lưu cấu hình** | SharedPreferences |
| **Speech-to-Text** | SpeechRecognizer API |
| **Text-to-Speech** | TextToSpeech API |
| **Danh bạ** | Content Provider |
| **Camera** | CameraX |
| **Object Detection** | TensorFlow Lite (EfficientDet Lite) |
| **GPS** | Fused Location Provider |
| **SMS** | SmsManager |
| **Trợ năng** | Accessibility Service |
| **Nền** | Foreground Service |
| **Broadcast** | Broadcast Receiver |
| **Notification** | NotificationListenerService |

---

## 📋 Yêu cầu hệ thống

- **Android 8.0 (API 26)** trở lên

- **Quyền cần thiết:**

| Quyền | Mục đích |
|-------|----------|
| `RECORD_AUDIO` | Ghi âm |
| `CALL_PHONE` | Gọi điện |
| `READ_CONTACTS` | Đọc danh bạ |
| `ACCESS_FINE_LOCATION` | Lấy vị trí GPS |
| `SEND_SMS` | Gửi tin nhắn |
| `CAMERA` | Camera |
| `INTERNET` | Kết nối mạng |
| `FOREGROUND_SERVICE` | Chạy nền |
| `RECEIVE_BOOT_COMPLETED` | Khởi động khi máy bật |
| `ACCESS_NETWORK_STATE` | Kiểm tra mạng |
| `POST_NOTIFICATIONS` | Hiển thị thông báo (Android 13+) |

---

## 🗂️ Nhóm lệnh hỗ trợ

| Nhóm lệnh | Ví dụ | Phản hồi hệ thống |
|-----------|-------|-------------------|
| **Gọi điện** | "Gọi cho mẹ" / "Call Mom" | Tìm liên hệ và thực hiện cuộc gọi |
| **Thông tin** | "Mấy giờ rồi?" / "What time is it?" | Đọc thời gian hiện tại |
| **Pin** | "Pin còn bao nhiêu?" / "Battery level" | Đọc phần trăm pin |
| **SOS** | "Cứu tôi" / "Help me" | Bắt đầu quy trình SOS |
| **Nhận diện** | "Nhận diện vật thể" / "Detect objects" | Mở camera và thông báo vật thể |
| **Điều hướng** | "Mở cài đặt" / "Open settings" | Mở màn hình cài đặt |
| **Trợ giúp** | "Hướng dẫn" / "Help" | Đọc danh sách lệnh |
| **Nhắc lại** | "Nhắc lại" / "Repeat" | Đọc lại phản hồi cuối |
| **Đọc thông báo** | "Đọc thông báo" / "Read notifications" | Đọc thông báo đã lưu |

---

## 👥 Thành viên nhóm

| STT | MSSV | Họ tên | Vai trò |
|-----|------|--------|---------|
| 1 | 23110065 | Mai Trần Thùy Trang | UI/UX, Presentation Layer |
| 2 | 23110051 | Trần Thị Tố Như | Business Logic, Services |
| 3 | 23110004 | Võ Nguyễn Ngọc Bích | Data Layer, Database |
