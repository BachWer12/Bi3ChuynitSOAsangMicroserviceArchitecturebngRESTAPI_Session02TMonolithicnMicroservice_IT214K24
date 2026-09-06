# Bài 3 — Chuyển đổi từ SOA sang Microservice Architecture bằng REST API

## 1. Phân tích lỗi gọi IP cố định và cách khắc phục
### Vấn đề của việc "Hardcode" địa chỉ IP
Trong môi trường Microservices, tính năng tự động co giãn (auto-scaling) thường được sử dụng. Nếu `book-service` có nhiều instance (chẳng hạn 3 instance đang chạy song song), và địa chỉ IP liên tục thay đổi mỗi khi một instance mới được tạo ra hoặc bị sập đi, việc gọi thẳng IP cố định `192.168.1.15:8082` trong `BookClientService` mang lại những rủi ro cực lớn:
- **Nguy cơ sập hệ thống**: Nếu instance có IP đó bị tắt (down), toàn bộ truy vấn từ `borrowing-service` sẽ văng lỗi `Connection Refused` hoặc `Timeout`, làm ảnh hưởng nghiêm trọng tới chức năng hệ thống.
- **Không có cân bằng tải (Load Balancing)**: Ngay cả khi có hàng chục instance `book-service` rảnh rỗi, mọi request vẫn dồn vào đúng một IP `192.168.1.15`, gây nghẽn cổ chai cục bộ.

### Sửa lỗi bằng LoadBalanced và Service Discovery
Để sửa vấn đề này, code Java đã được thiết lập sử dụng **tên logic** (`http://book-service/...`) thay cho IP cứng, kết hợp Annotation `@LoadBalanced` để cấu hình cho `RestTemplate` khả năng tự động truy vấn địa chỉ IP thật thông qua máy chủ Service Discovery (như Eureka Server) và phân bổ tải thông minh theo thuật toán Round Robin. 

### Sửa lỗi thiếu cơ chế dự phòng (Fault Tolerance)
Đồng thời, hàm `getBookTitle` cũng đã được bổ sung khối `try-catch (RestClientException)` để xử lý việc gọi API thất bại. Nếu `book-service` không phản hồi, `borrowing-service` sẽ tự "nuốt" lỗi và trả về dữ liệu fallback mặc định (Ví dụ: "Unknown Title"), tránh gây crash dây chuyền (Cascading Failure). 

*(Source code chi tiết đã được cập nhật tại thư mục `src/main/java/` của dự án).*

---

## 2. Phân tích Đánh đổi giữa SOA và MSA trong bối cảnh LibraX

Trong quá trình tiến hóa từ Monolithic qua SOA (dùng ESB) rồi đến Microservice Architecture (MSA, dùng REST API), đội ngũ LibraX phải đối mặt với một loạt bài toán đánh đổi về mặt kỹ thuật lẫn tổ chức:

**Về tốc độ phát triển (Development Speed):** 
Với SOA trước đây, mọi liên lạc đều bị tập trung hóa ở trục ESB (Enterprise Service Bus). Khi phát triển chức năng `NotificationService`, việc thêm logic định tuyến (routing) bắt buộc đội phát triển phải phụ thuộc, chờ đợi sự phê duyệt từ đội vận hành hệ thống quản lý ESB, kìm hãm tốc độ ra mắt tính năng. Ngược lại, khi áp dụng MSA với phương châm *"Smart endpoints, dumb pipes"* (Endpoint thông minh, đường ống ngu ngốc), các dịch vụ gọi nhau trực tiếp qua REST API (HTTP). Đội `Borrowing` có thể phát hành mã gọi sang đội `Notification` ngay tắp lự chỉ bằng một giao thức JSON đơn giản, giúp quá trình nâng cấp ứng dụng độc lập, linh hoạt và Time-to-market cực kỳ nhanh.

**Về độ phức tạp vận hành (Operational Complexity):** 
Đây là điểm yếu chí mạng của MSA so với SOA. Khi đập bỏ ESB trung tâm, LibraX mất đi một "bảng điều khiển" duy nhất để giám sát luồng đi của dữ liệu. Độ phức tạp mạng (Network complexity) tăng theo cấp số nhân vì hệ thống giờ đây bị phân mảnh. Việc gọi API giữa hàng chục service bắt buộc đội ngũ LibraX phải đưa vào các công cụ giám sát vô cùng phức tạp: triển khai Service Discovery để tự động quản lý IP, sử dụng Distributed Tracing (như Zipkin, Sleuth) để truy vết xem luồng dữ liệu bị đứt đoạn tại đoạn nào. Điều này đòi hỏi chi phí cho đội ngũ DevOps lớn hơn rất nhiều so với thời kỳ dùng ESB.

**Về khả năng chịu lỗi (Fault Tolerance):**
Ở mô hình SOA, bản thân cái ESB chính là điểm mù và điểm gây chết chùm cục bộ (Single Point of Failure). Nếu ESB gặp sự cố sập hoặc treo, toàn bộ hoạt động giao tiếp giữa các service trong thư viện sẽ tê liệt hoàn toàn. Khi chuyển qua MSA, nhờ mạng lưới không tập trung, nếu một service sập (ví dụ `book-service`), các service khác vẫn tiếp tục chạy. Tuy nhiên, nếu như thiếu những giải pháp chặn lỗi thông minh (như `try-catch` đã triển khai ở trên, hay các pattern như Circuit Breaker), sự cố trễ mạng (Network Latency) hoặc Timeout có thể tạo ra hiệu ứng Domino quật ngã hệ thống. Có thể nói, khả năng chịu lỗi trong MSA linh hoạt hơn nhưng lại đòi hỏi lập trình viên phải chủ động lường trước và thiết kế kỹ càng từ tầng code.
