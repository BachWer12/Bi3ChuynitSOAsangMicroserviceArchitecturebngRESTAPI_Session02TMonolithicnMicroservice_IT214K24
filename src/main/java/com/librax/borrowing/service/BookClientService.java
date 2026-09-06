package com.librax.borrowing.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class BookClientService {

    private final RestTemplate restTemplate;

    // Inject RestTemplate đã được đánh dấu @LoadBalanced
    public BookClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getBookTitle(Long bookId) {
        // Thay thế việc gọi trực tiếp IP (192.168.1.15) bằng tên logic của service là "book-service"
        // Việc phân giải tên này ra IP thực tế sẽ do Service Discovery và LoadBalancer đảm nhiệm.
        String url = "http://book-service/api/books/" + bookId;
        
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            // Xử lý lỗi (Fault tolerance): Bắt các lỗi khi book-service không phản hồi (timeout, 500)
            // Tránh tình trạng Exception văng ra làm crash luôn borrowing-service
            System.err.println("[LỖI] Không thể kết nối tới book-service: " + e.getMessage());
            
            // Có thể trả về dữ liệu dự phòng (Fallback data)
            return "Unknown Title (Book Service Unavailable)";
        }
    }
}
