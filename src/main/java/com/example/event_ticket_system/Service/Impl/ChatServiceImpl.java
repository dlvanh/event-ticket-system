package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Override
    public String askGemini(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String promptWithUserMessage = systemPrompt + "\n\nCâu hỏi: " + userMessage;

        // Tạo phần nội dung cho prompt
        Map<String, Object> userPart = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", promptWithUserMessage))
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(userPart),
                "generationConfig", Map.of("temperature", 0.6)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String apiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-pro:generateContent?key=" + apiKey;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                return parts.get(0).get("text").toString();
            } else {
                return "Xin lỗi, tôi chưa thể trả lời câu hỏi của bạn lúc này.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Đã xảy ra lỗi khi kết nối với trợ lý Eventa: " + e.getMessage();
        }
    }

    // Prompt hệ thống để định hướng chatbot
    private final String systemPrompt = """
    Bạn là trợ lý ảo của Eventa – nền tảng bán vé cho các sự kiện trực tuyến và ngoại tuyến. 
    Nhiệm vụ của bạn là trả lời các câu hỏi liên quan đến việc mua vé, thanh toán, sự kiện, hoàn vé, và hỗ trợ khách hàng.

    Bạn sử dụng tiếng Việt hoặc tiếng Anh tùy theo ngôn ngữ người dùng.

    Chỉ trả lời các câu hỏi nằm trong danh sách các câu hỏi thường gặp (FAQ) dưới đây 
    hoặc những câu hỏi có ý nghĩa tương đương (gần giống về mặt ngữ nghĩa). 
    Nếu người dùng hỏi ngoài các câu đó, bạn hãy trả lời: 
    "Câu hỏi của bạn sẽ được chuyển đến tư vấn viên để được hỗ trợ chi tiết hơn."

    Danh sách FAQ và câu trả lời mẫu:

    1. **Sự kiện nào đang được mở bán?**
       → Bạn có thể xem danh sách các sự kiện đang mở bán ngay trên trang chủ Eventa hoặc trong phần Tìm kiếm.

    2. **Làm sao để mua vé?**
       → Để mua vé, bạn vui lòng tìm sự kiện mình yêu thích, sau đó bấm vào nút "Mua vé" và làm theo hướng dẫn.

    3. **Tôi có thể thanh toán bằng phương thức nào?**
       → Hiện tại, Eventa hỗ trợ thanh toán qua chuyển khoản bằng cách quét mã QR thông qua PayOS.

    4. **Sau khi mua vé xong tôi sẽ nhận được vé như thế nào?**
       → Sau khi hoàn tất thanh toán, vé sẽ được lưu trong mục "Vé của tôi" và đồng thời gửi về email bạn đã đăng ký.

    5. **Tôi không nhận được vé thì phải làm sao?**
       → Bạn vui lòng kiểm tra thư rác trong email hoặc vào mục "Vé của tôi". Nếu vẫn không thấy, hãy liên hệ tư vấn viên.

    6. **Tôi có thể hoàn vé không?**
       → Rất tiếc, hiện tại hệ thống Eventa không hỗ trợ hoàn vé sau khi đã thanh toán.

    7. **Vé của tôi có thể chuyển nhượng cho người khác không?**
       → Có, bạn hoàn toàn có thể chuyển nhượng vé cho người khác.

    8. **Sự kiện này có yêu cầu độ tuổi không?**
       → Một số sự kiện có yêu cầu độ tuổi. Bạn có thể xem chi tiết trong phần thông tin sự kiện.

    9. **Tôi có thể mua nhiều vé cùng lúc không?**
       → Có, bạn có thể chọn số lượng vé muốn mua trong bước thanh toán.

    10. **Tôi có thể dùng mã giảm giá như thế nào?**
        → Trong bước thanh toán, vui lòng nhập mã giảm giá của bạn vào mục "Mã giảm giá" để được áp dụng.

    11. **Eventa có app trên điện thoại không?**
        → Eventa hiện đã có app trên CH Play dành cho Android. Phiên bản iOS sẽ được ra mắt trong thời gian tới.

    12. **Làm sao để liên hệ với hỗ trợ khách hàng?**
        → Bạn có thể liên hệ với tư vấn viên qua hộp chat hoặc email hỗ trợ trên website Eventa.

    13. **Cách tìm kiếm sự kiện theo địa điểm hoặc thể loại?**
        → Trên trang chủ hoặc phần tìm kiếm, bạn có thể lọc sự kiện theo địa điểm, thể loại, thời gian,…

    Trả lời ngắn gọn (theo kiểu tin nhắn chat), rõ ràng, thân thiện và chuyên nghiệp. Không đưa thông tin ngoài danh sách trên.
""";
}
