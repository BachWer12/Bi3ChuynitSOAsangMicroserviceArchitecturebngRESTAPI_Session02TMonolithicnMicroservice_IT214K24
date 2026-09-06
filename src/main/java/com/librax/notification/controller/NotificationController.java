package com.librax.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    // Đây là REST endpoint thay thế cho thao tác notifyOverdue gửi qua ESB trong hệ thống SOA cũ.
    // Thay vì ESB định tuyến, các service khác (như Borrowing) sẽ gọi trực tiếp HTTP POST tới endpoint này.
    @PostMapping("/overdue")
    public ResponseEntity<NotificationResponse> notifyOverdue(@RequestBody NotifyOverdueRequest request) {
        
        System.out.println("Đang xử lý thông báo tới độc giả ID: " + request.getMemberId() + 
                           " về cuốn sách ID: " + request.getBookId() + 
                           " (quá hạn từ ngày: " + request.getDueDate() + ")");
        
        // Logic truy vấn thông tin user và gửi Email/SMS sẽ diễn ra ở đây
        
        NotificationResponse response = new NotificationResponse("SUCCESS", "Đã gửi thông báo nhắc trả sách thành công.");
        return ResponseEntity.ok(response);
    }
}

class NotifyOverdueRequest {
    private int memberId;
    private int bookId;
    private String dueDate;
    
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}

class NotificationResponse {
    private String status;
    private String message;

    public NotificationResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
