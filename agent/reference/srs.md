
Group 08	23110065-Mai Trần Thùy Trang
23110051-Trần Thị Tố Như
23110004-Võ Nguyễn Ngọc Bích
Đề tài: Voice assistant (Ứng dụng trợ lý giọng nói cho người cao tuổi và người khiếm thị)


I. ĐỊNH NGHĨA VÀ THUẬT NGỮ (Definitions & Acronyms)
Speech Recognizer: Thành phần của Android dùng để chuyển đổi giọng nói thành văn bản (Speech-to-Text).
Text To Speech (TTS): Công nghệ chuyển đổi văn bản thành giọng nói để ứng dụng có thể phản hồi bằng âm thanh cho người dùng.
Accessibility Service: Dịch vụ trợ năng của Android cho phép ứng dụng hỗ trợ người khuyết tật và tương tác với hệ thống.
Foreground Service: Thành phần Android chạy nền với thông báo bắt buộc nhằm đảm bảo ứng dụng không bị hệ thống dừng.
Content Provider: Thành phần Android cho phép truy cập dữ liệu dùng chung như Danh bạ (Contacts).
Broadcast Receiver: Thành phần Android dùng để nhận các sự kiện từ hệ thống như pin yếu, mất kết nối mạng hoặc khởi động lại thiết bị.
Object Detection: Kỹ thuật nhận diện các vật thể xuất hiện trong khung hình camera.

II. GIỚI THIỆU VÀ PHẠM VI DỰ ÁN (Introduction & Scope)
1. Lý do chọn đề tài
Người cao tuổi và người khiếm thị thường gặp khó khăn khi sử dụng điện thoại thông minh do kích thước chữ nhỏ, thao tác phức tạp hoặc khó xác định vị trí các ứng dụng trên màn hình.
Ứng dụng Voice Assistant for Elderly & Visually Impaired được xây dựng nhằm giúp người dùng tương tác với điện thoại bằng giọng nói, từ đó giảm sự phụ thuộc vào thao tác cảm ứng và nâng cao khả năng tiếp cận công nghệ.
Ngoài ra, ứng dụng còn tích hợp AI để nhận diện các vật thể nằm trong khung hình sau đó phát ra âm thanh để hỗ trợ người khiếm thị.
2. Phạm vi dự án (Project Scope)
In-Scope (Các tính năng thực hiện)
Nhận diện giọng nói bằng Speech Recognizer.
Phản hồi bằng giọng nói sử dụng Text To Speech.
Thực hiện cuộc gọi từ danh bạ.
Đọc thời gian hiện tại.
Đọc mức pin thiết bị.
Đọc tin nhắn hoặc thông báo mới.
Hỗ trợ kích hoạt bằng Accessibility Service.
Chạy nền bằng Foreground Service.
Nhận sự kiện hệ thống bằng Broadcast Receiver.
Nhận diện vật thể xung quanh và đọc tên và hướng vật thể bằng Text To Speech.
Out-of-Scope (Không thực hiện)
Điều khiển toàn bộ hệ điều hành như Google Assistant.
Gọi điện hoặc gửi tin nhắn tự động không có xác nhận.
Điều khiển thiết bị IoT.
Nhận diện khuôn mặt.
Đánh giá mức độ nguy hiểm của vật thể.
Dẫn đường GPS thời gian thực.

III. DANH SÁCH YÊU CẦU CHỨC NĂNG (Functional Requirements)
FR-01: Kích hoạt trợ lý
Người dùng nhấn nút Micro hoặc nút trợ năng.
Hệ thống phát âm thanh báo hiệu bắt đầu lắng nghe.
Speech Recognizer được kích hoạt.
FR-02: Chuyển đổi giọng nói thành văn bản
Hệ thống ghi nhận giọng nói.
Chuyển đổi thành văn bản.
Hiển thị nội dung đã nhận diện trên giao diện.
FR-03: Nhận diện vật thể
Sử dụng Object Detection để nhận diện vật thể.
Trả về danh sách vật thể tìm thấy.
FR-04: Gọi điện bằng giọng nói
Người dùng có thể nói:
Gọi cho mẹ
Gọi cho con trai
Hệ thống:
Truy cập danh bạ qua Content Provider.
Tìm liên hệ phù hợp.
Thực hiện cuộc gọi.
FR-05: Đọc thời gian
Người dùng: "Hiện tại là mấy giờ?"
Hệ thống đọc thời gian hiện tại bằng Text To Speech.
FR-06: Đọc mức pin
Người dùng: "Pin còn bao nhiêu?"
Hệ thống đọc phần trăm pin hiện tại.
FR-07: Phản hồi bằng giọng nói
Mọi kết quả xử lý phải được đọc bằng Text To Speech.
FR-08: Chế độ trợ năng
Hỗ trợ Accessibility Service.
Kích hoạt trợ lý từ nút trợ năng hệ thống.
Hỗ trợ điều hướng bằng giọng nói.
FR-09: Foreground Service
Ứng dụng phải duy trì dịch vụ nền để:
Lắng nghe kích hoạt trợ lý.
Đọc thông báo quan trọng.
Không bị hệ thống dừng.
FR-10: Broadcast Receiver
Hệ thống nhận các sự kiện:
Pin yếu.
Mất kết nối mạng.
Khởi động lại thiết bị.
Ví dụ:
Khi pin dưới 20%
Ứng dụng đọc: "Pin yếu, vui lòng sạc thiết bị."
FR-11: Thiết lập liên hệ khẩn cấp
Người dùng hoặc người thân có thể chọn một hoặc nhiều liên hệ khẩn cấp từ danh bạ.
Thông tin liên hệ được lưu trong Room Database.
Người dùng có thể chỉnh sửa hoặc xóa liên hệ khẩn cấp.
FR-12: Kích hoạt SOS bằng giọng nói
Người dùng có thể nói:
"Cứu tôi"
"Gọi khẩn cấp"
"Tôi cần giúp đỡ"
Hệ thống sẽ nhận diện đây là lệnh SOS và kích hoạt chế độ hỗ trợ khẩn cấp.
FR-13: Gọi điện khẩn cấp
Sau khi kích hoạt SOS:
Hệ thống đọc thông báo xác nhận.
Tự động gọi đến liên hệ khẩn cấp đã được thiết lập trước.
Ví dụ: "Đang gọi cho người liên hệ khẩn cấp."
FR-14: Gửi vị trí hiện tại
Khi SOS được kích hoạt:
Hệ thống lấy vị trí GPS hiện tại bằng Fused Location Provider.
Tạo liên kết Google Maps chứa tọa độ hiện tại.
Gửi tin nhắn SMS tới liên hệ khẩn cấp.
Nội dung mẫu:
Tôi đang cần hỗ trợ khẩn cấp.
Vị trí hiện tại:
https://maps.google.com/…

IV. QUY TẮC NGHIỆP VỤ (Business Rules)

BR-01: Người dùng phải cấp quyền RECORD_AUDIO trước khi sử dụng trợ lý giọng nói.


BR-02: Người dùng phải cấp quyền CALL_PHONE trước khi thực hiện cuộc gọi.


BR-03: Chỉ được thực hiện cuộc gọi khi tìm thấy liên hệ phù hợp trong danh bạ.


BR-04: Mọi phản hồi phải được đọc bằng Text To Speech.


BR-05: Chỉ gửi tin nhắn SOS đến liên hệ đã đăng ký

Ứng dụng không được gửi SMS đến các số điện thoại khác ngoài danh sách liên hệ khẩn cấp.

BR-06: Chỉ gửi vị trí khi SOS được kích hoạt

Dữ liệu GPS chỉ được sử dụng cho mục đích hỗ trợ khẩn cấp và không được lưu trữ hoặc chia sẻ ngoài phạm vi chức năng SOS.

V. YÊU CẦU PHI CHỨC NĂNG (Non-Functional Requirements)
NFR-01 (Bảo mật)

Chỉ truy cập danh bạ khi người dùng cấp quyền.


Chỉ gửi vị trí GPS khi người dùng kích hoạt SOS.

NFR-02 (Khả năng sử dụng)

Nút Micro phải lớn, dễ nhìn.


Hỗ trợ chữ lớn.


Hỗ trợ người khiếm thị.


Nút SOS phải nổi bật và dễ kích hoạt.





