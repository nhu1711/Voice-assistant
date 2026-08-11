#VOICE ASSISTANT FOR ELDERLY AND VISUALLY IMPAIRED PEOPLE
An Android voice assistant application designed to help elderly and visually impaired users interact with their smartphones through speech. The app reduces dependency on touch operations and enhances accessibility to modern technology.

#MOTIVATION
Elderly and visually impaired people often face significant difficulties when using smartphones. Small text sizes, complex touch operations, and difficulty locating applications on the screen create barriers to accessing modern technology. According to the World Health Organization, approximately 2.2 billion people worldwide have some form of visual impairment. This project aims to bridge the accessibility gap by providing a simple, voice-driven interface that allows users to perform daily tasks with minimal visual interaction.

#FEATURES
Voice Interaction
Users tap the microphone button to activate speech recognition. The system converts speech to text using Android SpeechRecognizer, displays the recognized text on screen, and parses the command using CommandParser. All responses are read aloud using TextToSpeech in both Vietnamese and English.

Phone Calling
Users can say commands such as "Call Mom" or "Gọi cho mẹ". The application searches Android contacts and initiates a phone call after the required permissions are granted. Voice feedback confirms the action before the call is placed.

Time and Battery Information
Voice commands can request the current time or battery percentage. The result is shown on screen and read aloud, including charging status or low-battery warnings when the battery falls below 20 percent.

Emergency Contact Management
Users can add, edit, and delete multiple emergency contacts. Each contact can be assigned a priority value from 1 to 5, with lower numbers indicating higher priority. Data is stored locally in Room Database through repository and DAO pattern.

Emergency SOS
SOS can be activated by voice commands such as "Help me" or "Cứu tôi", or through the interface button. A five-second countdown allows cancellation before the emergency workflow begins. If not cancelled, the system calls the highest-priority available contact, obtains the current GPS location, and sends an emergency SMS with a Google Maps link to all registered emergency contacts.

Object Detection
CameraX supplies real-time frames to the on-device object detector using the bundled EfficientDet Lite model with TensorFlow Lite. Detected objects are displayed on screen with confidence scores and spoken aloud to help users understand their surroundings. This feature is particularly useful for visually impaired users navigating unfamiliar environments.

Background Assistance
VoiceAssistantService runs as a foreground service when background mode is enabled. It keeps system alerts active and displays a persistent notification. The service monitors battery level and network connectivity through a dynamically registered BroadcastReceiver, announcing low battery and network loss or restoration according to user settings.

Notification Reading
NotificationListenerService can announce notifications from applications selected by the user. Two reading modes are available: full content reading and brief announcement mode. This feature requires notification-access permission to be enabled in Android system settings.

Accessibility Interface
The application uses large buttons, clear labels, high-contrast colors, bottom navigation, listening animation, and bilingual settings to reduce visual and interaction difficulty. All critical actions provide both visual and audio feedback.

#SUPPORTED COMMANDS
Call Commands
Example: "Call Mom", "Gọi cho mẹ"
System Response: Find contact and start call

Information Commands
Example: "What time is it?", "Mấy giờ rồi?"
System Response: Read current time aloud

Battery Commands
Example: "Battery level", "Pin còn bao nhiêu?"
System Response: Read battery percentage aloud

SOS Commands
Example: "Help me", "Cứu tôi"
System Response: Start cancellable SOS workflow

Detection Commands
Example: "Detect objects", "Nhận diện vật thể"
System Response: Open camera and announce detected objects

Navigation Commands
Example: "Open settings", "Go home", "Mở cài đặt"
System Response: Navigate to corresponding screen

Help Commands
Example: "Help", "Hướng dẫn"
System Response: Read available command list

Repeat Commands
Example: "Repeat", "Nhắc lại"
System Response: Repeat the last response

Notification Commands
Example: "Read notifications", "Đọc thông báo"
System Response: Read saved recent notifications

#SYSTEM ARCHITECTURE
The application follows a three-layer architecture:

Presentation Layer
Contains Activities, Fragments, and Adapters responsible for displaying the user interface and handling user interactions. Main components include MainActivity, ContactsActivity, DetectionActivity, SettingsActivity, and HomeFragment.

Business Logic Layer
Contains Managers, Services, and Command Handlers that implement core functionality. Key components include SpeechRecognizerManager for speech-to-text, TTSManager for text-to-speech, CallManager for phone calls, ContactManager for contact lookup, SOSManager for emergency workflows, LocationManagerHelper for GPS, and ObjectDetectionManager for camera-based object recognition.

Data Layer
Manages local data storage through Room Database for emergency contacts, SharedPreferences for user settings, and Content Provider for accessing system contacts. Repository pattern abstracts database operations through EmergencyContactRepository.

#TECHNOLOGY STACK
Programming Language: Java

IDE: Android Studio

Minimum SDK: Android 8.0 (API 26)

Local Database: Room Database

Configuration Storage: SharedPreferences

Speech-to-Text: SpeechRecognizer API

Text-to-Speech: TextToSpeech API

Contacts Access: Content Provider

Camera: CameraX

Object Detection: TensorFlow Lite (EfficientDet Lite)

GPS Location: Fused Location Provider

SMS: SmsManager

Accessibility: Accessibility Service

Background Service: Foreground Service

System Events: Broadcast Receiver

Notification Listening: NotificationListenerService

Permission Management: ActivityResultContracts

#PROJECT STRUCTURE
text
app/src/main/java/com/example/voiceassistant/
│
├── ui/
│   ├── activities/
│   │   ├── MainActivity.java
│   │   ├── ContactsActivity.java
│   │   ├── DetectionActivity.java
│   │   └── SettingsActivity.java
│   ├── fragments/
│   │   ├── HomeFragment.java
│   │   └── SettingsFragment.java
│   └── adapters/
│       └── EmergencyContactAdapter.java
│
├── services/
│   ├── VoiceAssistantService.java
│   └── AppNotificationListenerService.java
│
├── receivers/
│   └── SystemBroadcastReceiver.java
│
├── speech/
│   ├── SpeechRecognizerManager.java
│   ├── CommandParser.java
│   └── VoiceCommandDispatcher.java
│
├── tts/
│   └── TTSManager.java
│
├── call/
│   └── CallManager.java
│
├── contacts/
│   └── ContactManager.java
│
├── emergency/
│   └── EmergencyManager.java
│
├── detection/
│   └── ObjectDetectionManager.java
│
├── location/
│   └── LocationManagerHelper.java
│
├── battery/
│   └── BatteryManagerHelper.java
│
├── sms/
│   └── SmsManagerHelper.java
│
├── data/
│   ├── database/
│   │   ├── AppDatabase.java
│   │   ├── dao/
│   │   │   └── EmergencyContactDao.java
│   │   └── entity/
│   │       └── EmergencyContact.java
│   ├── repository/
│   │   └── EmergencyContactRepository.java
│   └── preferences/
│       └── AppPreferences.java
│
├── permissions/
│   └── PermissionHelper.java
│
├── utils/
│   └── LocaleHelper.java
│
└── constants/
    └── AppConstants.java
#SYSTEM REQUIREMENTS
Android 8.0 (API 26) or higher

Required Permissions:

RECORD_AUDIO for voice recording

CALL_PHONE for making calls

READ_CONTACTS for contact lookup

ACCESS_FINE_LOCATION for GPS location

SEND_SMS for emergency messages

CAMERA for object detection

INTERNET for network connectivity

FOREGROUND_SERVICE for background operation

RECEIVE_BOOT_COMPLETED for auto-start on boot

ACCESS_NETWORK_STATE for network monitoring

POST_NOTIFICATIONS for notifications (Android 13+)

#INSTALLATION AND SETUP
Clone the repository:
git clone https://github.com/nhu1711/Voice-assistant.git
cd Voice-assistant

Open with Android Studio:
Open Android Studio
Select "Open an Existing Project"
Choose the project directory

Build and run:
Build > Make Project
Run > Run 'app'

Permission setup:
The application requests necessary permissions automatically. Some permissions need to be granted manually:

Notification Access: Settings > Apps > Voice Assistant > Notification Access > Enable

Accessibility Service: Settings > Accessibility > Voice Assistant > Enable

#TEAM MEMBERS
Student ID: 23110004
Name: Vo Nguyen Ngoc Bich
Role: Data Layer, Database Design, Room Implementation, Repository Pattern

Student ID: 23110051
Name: Tran Thi To Nhu
Role: Business Logic Layer, Services, Command Processing, SOS Workflow

Student ID: 23110065
Name: Mai Tran Thuy Trang
Role: UI/UX Design, Presentation Layer, Activities, Fragments, Adapters

#ACKNOWLEDGMENT
This project was completed as the final assignment for the Mobile Programming course at Ho Chi Minh City University of Technology and Engineering. We would like to express our sincere gratitude to our instructor, MSc. Truong Thi Ngoc Phuong, for her invaluable guidance, enthusiastic support, and professional direction throughout the development process. Her dedication and extensive experience have been an important foundation, helping our group effectively apply theoretical knowledge to practice and meet the requirements of the topic.
