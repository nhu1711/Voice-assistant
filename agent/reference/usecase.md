III. ĐẶC TẢ USE CASE
Ghi chú quan trọng:

FR-07 (Phản hồi bằng giọng nói): Áp dụng cho tất cả các Use Case. Sau mỗi bước xử lý, hệ thống luôn đọc phản hồi bằng Text To Speech (TTS) và hiển thị văn bản trên màn hình. Đây là một ràng buộc cross-cutting áp dụng cho toàn bộ hệ thống.


FR-09 (Foreground Service) và FR-10 (Broadcast Receiver) là các cơ chế nền, không phải Use Case tương tác với người dùng. Chúng được mô tả trong phần Kiến trúc hệ thống và Thành phần Android sử dụng.


FR-01 (Kích hoạt trợ lý) và FR-02 (Chuyển đổi giọng nói thành văn bản) là các bước trong UC-02, không phải Use Case riêng.


UC-01: Nhận diện và xử lý lệnh thoại
Thuộc tính	Giá trị
Use Case ID	UC-01
Use Case Name	Nhận diện và xử lý lệnh thoại
Actor	Người dùng (Người cao tuổi / Người khiếm thị)
Trigger	Người dùng nhấn nút Micro hoặc nút Accessibility
Pre-conditions	Ứng dụng đã được cài đặt và đang chạy. Người dùng đã cấp quyền RECORD_AUDIO (BR-01).
Main Flow
Bước	Actor	Hệ thống
1	Người dùng nhấn nút Micro trên giao diện chính.	
	(Hoặc) Người dùng nhấn nút Accessibility trên thanh trạng thái.	
		2. Hệ thống hiển thị trạng thái "Đang lắng nghe..." trên màn hình.
		3. Hệ thống phát âm thanh "Bíp" báo hiệu bắt đầu lắng nghe.
		4. Hệ thống khởi tạo SpeechRecognizer và bắt đầu ghi âm.
5	Người dùng nói câu lệnh.	
		6. Hệ thống ghi nhận giọng nói và chuyển đổi thành văn bản.
		7. Hệ thống hiển thị văn bản đã nhận diện trên màn hình.
		8. Hệ thống phân tích văn bản để xác định loại lệnh.
		9. Hệ thống thực thi lệnh tương ứng (đi đến Sub-flow).
Sub-flow 1: Lệnh gọi điện (FR-04)
Bước	Actor	Hệ thống
		9.1. Hệ thống xác định đây là lệnh CALL (ví dụ: "Gọi cho mẹ").
		9.2. Hệ thống hiển thị "Đang tìm kiếm [tên liên hệ]...".
		9.3. Hệ thống truy cập danh bạ thiết bị qua Content Provider.
		9.4. Hệ thống tìm kiếm liên hệ phù hợp.
		9.5. Hệ thống hiển thị tên và số điện thoại tìm thấy.
		9.6. Hệ thống đọc "Đang gọi cho [tên liên hệ]".
		9.7. Hệ thống thực hiện cuộc gọi qua Intent.ACTION_CALL.
		9.8. Hệ thống chuyển sang màn hình cuộc gọi hệ thống.
Alternative Flow 9a: Không tìm thấy liên hệ
Bước	Actor	Hệ thống
		9a.1. Hệ thống hiển thị "Không tìm thấy [tên liên hệ]".
		9a.2. Hệ thống đọc "Không tìm thấy [tên liên hệ] trong danh bạ". (BR-03)
Exception Flow 9b: Chưa cấp quyền CALL_PHONE
Bước	Actor	Hệ thống
		9b.1. Hệ thống yêu cầu cấp quyền CALL_PHONE (BR-02).
Sub-flow 2: Lệnh đọc thời gian (FR-05)
Bước	Actor	Hệ thống
		9.1. Hệ thống xác định đây là lệnh TIME (ví dụ: "Mấy giờ rồi?").
		9.2. Hệ thống lấy thời gian hiện tại từ hệ thống (Calendar).
		9.3. Hệ thống định dạng thời gian: "HH:mm" hoặc "HH giờ mm phút".
		9.4. Hệ thống hiển thị thời gian trên màn hình.
		9.5. Hệ thống đọc "Bây giờ là [giờ] giờ [phút] phút".
Sub-flow 3: Lệnh đọc mức pin (FR-06)
Bước	Actor	Hệ thống
		9.1. Hệ thống xác định đây là lệnh BATTERY (ví dụ: "Pin còn bao nhiêu?").
		9.2. Hệ thống lấy phần trăm pin qua BatteryManager.
		9.3. Hệ thống hiển thị phần trăm pin trên màn hình.
		9.4. Hệ thống đọc "Pin còn [phần trăm] phần trăm".
		9.5. Nếu pin thấp (< 20%): Hệ thống đọc thêm "Pin yếu, vui lòng sạc thiết bị".
Sub-flow 4: Lệnh nhận diện vật thể (FR-03)
Bước	Actor	Hệ thống
		9.1. Hệ thống xác định đây là lệnh DETECT (ví dụ: "Nhận diện vật thể").
		9.2. Hệ thống mở Camera và hiển thị khung hình.
		9.3. Hệ thống hiển thị trạng thái "Đang phân tích...".
9.4	Người dùng hướng camera về phía vật thể.	
		9.5. Hệ thống phân tích khung hình bằng mô hình AI.
		9.6. Hệ thống nhận diện vật thể trong khung hình.
		9.7. Hệ thống hiển thị tên vật thể và độ tin cậy.
		9.8. Hệ thống đọc "Phát hiện [tên vật thể]".
9.9	Người dùng di chuyển camera để nhận diện vật thể khác.	
		9.10. Hệ thống tiếp tục phân tích và cập nhật kết quả.
Alternative Flow 9a: Không tìm thấy vật thể
Bước	Actor	Hệ thống
		9a.1. Hệ thống hiển thị "Không phát hiện vật thể".
		9a.2. Hệ thống đọc "Không tìm thấy vật thể nào".
Sub-flow 5: Lệnh SOS (FR-12, FR-13, FR-14)
Bước	Actor	Hệ thống
		9.1. Hệ thống xác định đây là lệnh SOS (ví dụ: "Cứu tôi").
		9.2. Hệ thống hiển thị "Đang kích hoạt chế độ khẩn cấp...".
		9.3. Hệ thống đọc "Đang kích hoạt chế độ khẩn cấp".
		9.4. Hệ thống hiển thị nút "Hủy" cho phép hủy trong 5 giây.
9.5	(Nếu không hủy trong 5 giây)	
		9.6. Hệ thống lấy danh sách liên hệ khẩn cấp từ Room Database.
		9.7. Hệ thống sắp xếp theo priority (ưu tiên thấp nhất là cao nhất).
		9.8. Hệ thống lấy liên hệ có priority = 1 (ưu tiên cao nhất).
		9.9. Hệ thống hiển thị "Đang gọi đến [tên liên hệ]".
		9.10. Hệ thống đọc "Đang gọi cho [tên liên hệ]".
		9.11. Hệ thống thực hiện cuộc gọi qua Intent.ACTION_CALL.
		9.12. Hệ thống chuyển sang màn hình cuộc gọi.
		(Đồng thời, song song)
		9.13. Hệ thống khởi tạo FusedLocationProviderClient.
		9.14. Hệ thống lấy vị trí GPS hiện tại.
		9.15. Hệ thống tạo liên kết Google Maps: https://maps.google.com/?q=lat,lng.
		9.16. Hệ thống tạo nội dung tin nhắn: "Tôi đang cần hỗ trợ khẩn cấp. Vị trí hiện tại của tôi: [link]"
		9.17. Hệ thống gửi SMS đến tất cả liên hệ khẩn cấp qua SmsManager. (BR-05)
		9.18. Hệ thống hiển thị "Đã gửi vị trí đến [số lượng] liên hệ".
Alternative Flow 9a: Người dùng hủy SOS
Bước	Actor	Hệ thống
9a.1	Người dùng nhấn nút "Hủy" trong vòng 5 giây.	
		9a.2. Hệ thống hủy quá trình SOS.
		9a.3. Hệ thống hiển thị "Đã hủy chế độ khẩn cấp".
		9a.4. Hệ thống đọc "Đã hủy, bạn an toàn".
Alternative Flow 9b: Không có liên hệ khẩn cấp
Bước	Actor	Hệ thống
		9b.1. Hệ thống đọc "Chưa có liên hệ khẩn cấp, vui lòng thiết lập trước".
		9b.2. Hệ thống hiển thị thông báo và dừng quá trình.
Alternative Flow 9c: Cuộc gọi thất bại
Bước	Actor	Hệ thống
		9c.1. Hệ thống gọi cho liên hệ tiếp theo trong danh sách.
		9c.2. Hệ thống đọc "Không thể gọi cho [tên], đang gọi cho [tên khác]".
Alternative Flow 9d: Không thể lấy vị trí
Bước	Actor	Hệ thống
		9d.1. Hệ thống gửi SMS không có vị trí.
		9d.2. Hệ thống đọc "Không thể lấy vị trí hiện tại".
Exception Flow
Bước	Actor	Hệ thống
		4a.1. Nếu chưa cấp quyền RECORD_AUDIO: Hệ thống hiển thị thông báo yêu cầu cấp quyền.
		4a.2. Hệ thống đọc "Ứng dụng cần quyền ghi âm để hoạt động".
4a.3	Người dùng cấp quyền hoặc từ chối.	
		4a.4. Nếu được cấp quyền: Quay lại bước 2.
		4a.5. Nếu từ chối: Hệ thống hiển thị thông báo "Không thể sử dụng trợ lý" và thoát.
		8a.1. Nếu không xác định được loại lệnh: Hệ thống hiển thị "Tôi không hiểu lệnh này".
		8a.2. Hệ thống đọc "Tôi không hiểu lệnh của bạn".
Post-conditions

Lệnh đã được xử lý và phản hồi đã được gửi đến người dùng.


UC-02: Thiết lập liên hệ khẩn cấp
Thuộc tính	Giá trị
Use Case ID	UC-02
Use Case Name	Thiết lập liên hệ khẩn cấp
Actor	Người dùng hoặc người thân
Trigger	Người dùng mở màn hình quản lý liên hệ khẩn cấp
Pre-conditions	Người dùng đã cấp quyền READ_CONTACTS.
Main Flow - Thêm liên hệ khẩn cấp
Bước	Actor	Hệ thống
1	Người dùng mở màn hình "Liên hệ khẩn cấp" từ giao diện chính.	
		2. Hệ thống hiển thị danh sách liên hệ khẩn cấp đã lưu (nếu có).
		3. Hệ thống hiển thị nút "Thêm liên hệ".
4	Người dùng nhấn nút "Thêm liên hệ".	
		5. Hệ thống mở danh bạ thiết bị qua Content Provider.
		6. Hệ thống hiển thị danh sách danh bạ.
7	Người dùng chọn một liên hệ từ danh sách.	
		8. Hệ thống lấy thông tin: Tên, Số điện thoại, ID liên hệ.
		9. Hệ thống lưu thông tin vào Room Database.
		10. Hệ thống hiển thị thông báo "Đã thêm [tên liên hệ] vào danh sách khẩn cấp".
		11. Hệ thống cập nhật danh sách hiển thị.
Main Flow - Xóa liên hệ khẩn cấp
Bước	Actor	Hệ thống
1	Người dùng nhấn nút Xóa bên cạnh liên hệ trong danh sách.	
		2. Hệ thống hiển thị hộp thoại xác nhận.
3	Người dùng xác nhận "Có".	
		4. Hệ thống xóa liên hệ khỏi Room Database.
		5. Hệ thống cập nhật danh sách hiển thị.
Main Flow - Sửa liên hệ khẩn cấp
Bước	Actor	Hệ thống
1	Người dùng nhấn vào liên hệ trong danh sách.	
		2. Hệ thống hiển thị màn hình chỉnh sửa.
3	Người dùng thay đổi thông tin (ví dụ: cập nhật priority).	
4	Người dùng nhấn "Lưu".	
		5. Hệ thống cập nhật Room Database.
		6. Hệ thống thông báo "Đã cập nhật liên hệ".
Alternative Flow
Bước	Actor	Hệ thống
		9a.1. Nếu danh sách khẩn cấp đã có 5 liên hệ: Hệ thống thông báo "Danh sách đã đầy".
		9a.2. Hệ thống đọc "Chỉ có thể thêm tối đa 5 liên hệ khẩn cấp".
Post-conditions

Danh sách liên hệ khẩn cấp đã được cập nhật trong Room Database.


IV. QUY TẮC NGHIỆP VỤ (Business Rules)
Mã	Quy tắc	Mô tả
BR-01	Quyền ghi âm	Người dùng phải cấp quyền RECORD_AUDIO trước khi sử dụng trợ lý giọng nói.
BR-02	Quyền gọi điện	Người dùng phải cấp quyền CALL_PHONE trước khi thực hiện cuộc gọi.
BR-03	Tìm thấy liên hệ	Chỉ được thực hiện cuộc gọi khi tìm thấy liên hệ phù hợp trong danh bạ.
BR-04	Phản hồi bằng giọng nói	Mọi phản hồi của hệ thống phải được đọc bằng Text To Speech (FR-07).
BR-05	Gửi SMS đúng đối tượng	Chỉ gửi tin nhắn SOS đến liên hệ đã đăng ký trong danh sách khẩn cấp. Ứng dụng không gửi SMS đến số điện thoại khác.
BR-06	Chỉ gửi vị trí khi SOS	Dữ liệu GPS chỉ được sử dụng cho mục đích hỗ trợ khẩn cấp. Không lưu trữ hoặc chia sẻ ngoài phạm vi chức năng SOS.
