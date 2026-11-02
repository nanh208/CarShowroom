# setup_payloads_part2.ps1 - Generates modular payloads for Days 9 to 16
$base = "c:\Users\koreka\CarShowroom"
$payloadsRoot = Join-Path $base "scripts\payloads"

function Create-PayloadDir($dayNum) {
    $dir = Join-Path $payloadsRoot ("day{0:D2}" -f $dayNum)
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    return $dir
}

# --- DAY 09: ChatMessage & ChatRepository ---
$d9 = Create-PayloadDir 9
$d9Model = Join-Path $d9 "src\main\java\com\ares\model"
$d9Repo = Join-Path $d9 "src\main\java\com\ares\repository"
New-Item -ItemType Directory -Force -Path $d9Model | Out-Null
New-Item -ItemType Directory -Force -Path $d9Repo | Out-Null

@'
package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    private String customerName;

    private Boolean fromAdmin = false;

    @Column(nullable = false, length = 2000)
    private String content;

    private Boolean readByStaff = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(String sessionId, String customerName, Boolean fromAdmin, String content) {
        this.sessionId = sessionId;
        this.customerName = customerName;
        this.fromAdmin = fromAdmin;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Boolean getFromAdmin() { return fromAdmin; }
    public void setFromAdmin(Boolean fromAdmin) { this.fromAdmin = fromAdmin; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getReadByStaff() { return readByStaff; }
    public void setReadByStaff(Boolean readByStaff) { this.readByStaff = readByStaff; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
'@ | Set-Content (Join-Path $d9Model "ChatMessage.java") -Encoding UTF8

@'
package com.ares.repository;

import com.ares.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);
    List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByIdAsc(String sessionId, Long afterId);
    ChatMessage findFirstBySessionIdOrderByIdDesc(String sessionId);

    @Query("SELECT DISTINCT c.sessionId FROM ChatMessage c ORDER BY c.sessionId")
    List<String> findDistinctSessions();
}
'@ | Set-Content (Join-Path $d9Repo "ChatRepository.java") -Encoding UTF8

# --- DAY 10: ChatService & ChatApiController ---
$d10 = Create-PayloadDir 10
$d10Svc = Join-Path $d10 "src\main\java\com\ares\service"
$d10Ctrl = Join-Path $d10 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d10Svc | Out-Null
New-Item -ItemType Directory -Force -Path $d10Ctrl | Out-Null

@'
package com.ares.service;

import com.ares.model.ChatMessage;
import com.ares.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public List<ChatMessage> getMessages(String sessionId, Long afterId) {
        if (afterId != null && afterId > 0) {
            return chatRepository.findBySessionIdAndIdGreaterThanOrderByIdAsc(sessionId, afterId);
        }
        return chatRepository.findBySessionIdOrderByIdAsc(sessionId);
    }

    public ChatMessage sendMessage(String sessionId, String customerName, String content, boolean fromAdmin) {
        boolean first = chatRepository.findFirstBySessionIdOrderByIdDesc(sessionId) == null;

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setCustomerName(customerName != null && !customerName.isBlank() ? customerName : "Khách hàng");
        msg.setContent(content.trim());
        msg.setFromAdmin(fromAdmin);
        ChatMessage saved = chatRepository.save(msg);

        // Tin nhắn chào mừng tự động cho khách mở phiên đầu tiên
        if (first && !fromAdmin) {
            ChatMessage welcome = new ChatMessage();
            welcome.setSessionId(sessionId);
            welcome.setCustomerName("Ares Auto Assistant");
            welcome.setFromAdmin(true);
            welcome.setContent("Xin chào " + msg.getCustomerName() + "! Cảm ơn quý khách đã kết nối đến Ares Auto Showroom. Em là chuyên viên tư vấn hỗ trợ quý khách.");
            chatRepository.save(welcome);
        }

        return saved;
    }

    public List<String> getAllSessions() {
        return chatRepository.findDistinctSessions();
    }
}
'@ | Set-Content (Join-Path $d10Svc "ChatService.java") -Encoding UTF8

@'
package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.ChatMessage;
import com.ares.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ChatApiController {

    private final ChatService chatService;

    public ChatApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{session}")
    public ResponseEntity<ApiResponse> getMessages(@PathVariable String session,
                                                   @RequestParam(required = false, defaultValue = "0") Long after) {
        List<ChatMessage> list = chatService.getMessages(session, after);
        ApiResponse res = ApiResponse.ok("Danh sách tin nhắn");
        res.setData(list);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{session}")
    public ResponseEntity<ApiResponse> sendMessage(@PathVariable String session,
                                                   @RequestBody SendMessageRequest req) {
        if (req.content() == null || req.content().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Nội dung tin nhắn trống"));
        }
        ChatMessage sent = chatService.sendMessage(session, req.customerName(), req.content(), Boolean.TRUE.equals(req.fromAdmin()));
        ApiResponse res = ApiResponse.ok("Gửi tin nhắn thành công");
        res.setData(sent);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/sessions")
    public ResponseEntity<ApiResponse> getAdminSessions() {
        ApiResponse res = ApiResponse.ok("Danh sách phiên chat");
        res.setData(chatService.getAllSessions());
        return ResponseEntity.ok(res);
    }

    public record SendMessageRequest(String customerName, String content, Boolean fromAdmin) {}
}
'@ | Set-Content (Join-Path $d10Ctrl "ChatApiController.java") -Encoding UTF8

# --- DAY 11: ChatIntent & ChatIntentRepository ---
$d11 = Create-PayloadDir 11
$d11Model = Join-Path $d11 "src\main\java\com\ares\model"
$d11Repo = Join-Path $d11 "src\main\java\com\ares\repository"
New-Item -ItemType Directory -Force -Path $d11Model | Out-Null
New-Item -ItemType Directory -Force -Path $d11Repo | Out-Null

@'
package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_intents")
public class ChatIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intentName;

    @Column(length = 1000)
    private String keywords;

    @Column(length = 3000, nullable = false)
    private String responseContent;

    @Column(length = 500)
    private String quickReplies;

    private Boolean active = true;

    private Integer hitCount = 0;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public ChatIntent() {}

    public ChatIntent(String intentName, String keywords, String responseContent, String quickReplies) {
        this.intentName = intentName;
        this.keywords = keywords;
        this.responseContent = responseContent;
        this.quickReplies = quickReplies;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIntentName() { return intentName; }
    public void setIntentName(String intentName) { this.intentName = intentName; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getResponseContent() { return responseContent; }
    public void setResponseContent(String responseContent) { this.responseContent = responseContent; }
    public String getQuickReplies() { return quickReplies; }
    public void setQuickReplies(String quickReplies) { this.quickReplies = quickReplies; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
'@ | Set-Content (Join-Path $d11Model "ChatIntent.java") -Encoding UTF8

@'
package com.ares.repository;

import com.ares.model.ChatIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatIntentRepository extends JpaRepository<ChatIntent, Long> {
    List<ChatIntent> findByActiveTrue();
}
'@ | Set-Content (Join-Path $d11Repo "ChatIntentRepository.java") -Encoding UTF8

# --- DAY 12: ChatbotService ---
$d12 = Create-PayloadDir 12
$d12Svc = Join-Path $d12 "src\main\java\com\ares\service"
New-Item -ItemType Directory -Force -Path $d12Svc | Out-Null

@'
package com.ares.service;

import com.ares.model.ChatIntent;
import com.ares.model.ChatMessage;
import com.ares.repository.ChatIntentRepository;
import com.ares.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private final ChatIntentRepository intentRepository;
    private final ChatRepository chatRepository;

    public ChatbotService(ChatIntentRepository intentRepository, ChatRepository chatRepository) {
        this.intentRepository = intentRepository;
        this.chatRepository = chatRepository;
    }

    public Optional<String> generateBotReply(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return Optional.empty();

        String normalized = removeDiacritics(userMessage.toLowerCase().trim());
        List<ChatIntent> intents = intentRepository.findByActiveTrue();

        for (ChatIntent intent : intents) {
            if (intent.getKeywords() == null) continue;
            String[] kwList = intent.getKeywords().split(",");
            boolean matched = Arrays.stream(kwList)
                    .map(String::trim)
                    .filter(kw -> !kw.isEmpty())
                    .anyMatch(kw -> normalized.contains(removeDiacritics(kw.toLowerCase())));

            if (matched) {
                intent.setHitCount((intent.getHitCount() == null ? 0 : intent.getHitCount()) + 1);
                intentRepository.save(intent);
                return Optional.of(intent.getResponseContent());
            }
        }

        // Mặc định phản hồi thông minh nếu chưa khớp từ khóa cụ thể
        if (normalized.contains("gia") || normalized.contains("bang gia")) {
            return Optional.of("Bảng giá xe Ares Auto luôn cập nhật ưu đãi mới nhất! Quý khách có thể xem trực tiếp tại showroom hoặc để lại SĐT để nhận catalog chi tiết.");
        } else if (normalized.contains("tra gop") || normalized.contains("vay")) {
            return Optional.of("Ares hỗ trợ trả góp lãi suất 0% liên kết với hơn 10 ngân hàng lớn, thời hạn linh hoạt đến 8 năm!");
        } else if (normalized.contains("lai thu") || normalized.contains("test drive")) {
            return Optional.of("Quý khách có thể đặt lịch lái thử miễn phí tận nhà hoặc tại Showroom Ares. Hãy gửi số điện thoại của bạn nhé!");
        }

        return Optional.empty();
    }

    private String removeDiacritics(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
'@ | Set-Content (Join-Path $d12Svc "ChatbotService.java") -Encoding UTF8

# --- DAY 13: CustomerTicket & CustomerTicketRepository ---
$d13 = Create-PayloadDir 13
$d13Model = Join-Path $d13 "src\main\java\com\ares\model"
$d13Repo = Join-Path $d13 "src\main\java\com\ares\repository"
New-Item -ItemType Directory -Force -Path $d13Model | Out-Null
New-Item -ItemType Directory -Force -Path $d13Repo | Out-Null

@'
package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_tickets")
public class CustomerTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticketCode;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String phone;

    private String email;

    private String interestCar;

    private String inquiryType = "CONSULTATION"; // CONSULTATION, TEST_DRIVE, COMPLAINT

    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    private String status = "NEW"; // NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED

    private String assignedStaff;

    @Column(length = 2000)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomerTicket() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInterestCar() { return interestCar; }
    public void setInterestCar(String interestCar) { this.interestCar = interestCar; }
    public String getInquiryType() { return inquiryType; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(String assignedStaff) { this.assignedStaff = assignedStaff; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
'@ | Set-Content (Join-Path $d13Model "CustomerTicket.java") -Encoding UTF8

@'
package com.ares.repository;

import com.ares.model.CustomerTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTicketRepository extends JpaRepository<CustomerTicket, Long> {
    List<CustomerTicket> findByStatusOrderByCreatedAtDesc(String status);
    List<CustomerTicket> findByAssignedStaffOrderByCreatedAtDesc(String staff);
}
'@ | Set-Content (Join-Path $d13Repo "CustomerTicketRepository.java") -Encoding UTF8

# --- DAY 14: CrmService & CrmApiController ---
$d14 = Create-PayloadDir 14
$d14Svc = Join-Path $d14 "src\main\java\com\ares\service"
$d14Ctrl = Join-Path $d14 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d14Svc | Out-Null
New-Item -ItemType Directory -Force -Path $d14Ctrl | Out-Null

@'
package com.ares.service;

import com.ares.model.CustomerTicket;
import com.ares.repository.CustomerTicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CrmService {

    private final CustomerTicketRepository ticketRepository;

    public CrmService(CustomerTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public CustomerTicket createInquiry(String name, String phone, String email, String car, String type, String notes) {
        CustomerTicket t = new CustomerTicket();
        String code = "TKT-" + DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now()) + "-" + (System.currentTimeMillis() % 10000);
        t.setTicketCode(code);
        t.setCustomerName(name);
        t.setPhone(phone);
        t.setEmail(email);
        t.setInterestCar(car);
        t.setInquiryType(type != null ? type : "CONSULTATION");
        t.setNotes(notes);
        t.setStatus("NEW");
        return ticketRepository.save(t);
    }

    public List<CustomerTicket> getAllTickets(String status) {
        if (status != null && !status.isBlank()) {
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return ticketRepository.findAll();
    }

    public CustomerTicket updateTicketStatus(Long id, String status, String staff, String notes) {
        CustomerTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ticket #" + id));
        if (status != null) t.setStatus(status);
        if (staff != null) t.setAssignedStaff(staff);
        if (notes != null) t.setNotes(notes);
        return ticketRepository.save(t);
    }
}
'@ | Set-Content (Join-Path $d14Svc "CrmService.java") -Encoding UTF8

@'
package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.CustomerTicket;
import com.ares.service.CrmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CrmApiController {

    private final CrmService crmService;

    public CrmApiController(CrmService crmService) {
        this.crmService = crmService;
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse> createInquiry(@RequestBody InquiryRequest req) {
        if (req.customerName() == null || req.phone() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng cung cấp họ tên và số điện thoại"));
        }
        CustomerTicket ticket = crmService.createInquiry(req.customerName(), req.phone(), req.email(), req.interestCar(), req.inquiryType(), req.notes());
        ApiResponse res = ApiResponse.ok("Gửi yêu cầu tư vấn thành công! Showroom sẽ liên hệ lại ngay.");
        res.setData(ticket);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<ApiResponse> getTickets(@RequestParam(required = false) String status) {
        ApiResponse res = ApiResponse.ok("Danh sách Ticket CRM");
        res.setData(crmService.getAllTickets(status));
        return ResponseEntity.ok(res);
    }

    @PutMapping("/admin/tickets/{id}")
    public ResponseEntity<ApiResponse> updateTicket(@PathVariable Long id, @RequestBody UpdateTicketRequest req) {
        try {
            CustomerTicket t = crmService.updateTicketStatus(id, req.status(), req.assignedStaff(), req.notes());
            ApiResponse res = ApiResponse.ok("Cập nhật ticket thành công");
            res.setData(t);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    public record InquiryRequest(String customerName, String phone, String email, String interestCar, String inquiryType, String notes) {}
    public record UpdateTicketRequest(String status, String assignedStaff, String notes) {}
}
'@ | Set-Content (Join-Path $d14Ctrl "CrmApiController.java") -Encoding UTF8

# --- DAY 15: Promotion & PromotionRepository ---
$d15 = Create-PayloadDir 15
$d15Model = Join-Path $d15 "src\main\java\com\ares\model"
$d15Repo = Join-Path $d15 "src\main\java\com\ares\repository"
New-Item -ItemType Directory -Force -Path $d15Model | Out-Null
New-Item -ItemType Directory -Force -Path $d15Repo | Out-Null

@'
package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    private String discountType = "PERCENTAGE"; // PERCENTAGE hoặc FIXED_AMOUNT

    private Long discountValue = 0L; // Số % hoặc số tiền VNĐ

    private Long minOrderValue = 0L;

    private Long maxDiscount = 0L;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Integer usageLimit = 100;

    private Integer usedCount = 0;

    private Boolean active = true;

    public Promotion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Long getDiscountValue() { return discountValue; }
    public void setDiscountValue(Long discountValue) { this.discountValue = discountValue; }
    public Long getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(Long minOrderValue) { this.minOrderValue = minOrderValue; }
    public Long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Long maxDiscount) { this.maxDiscount = maxDiscount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
'@ | Set-Content (Join-Path $d15Model "Promotion.java") -Encoding UTF8

@'
package com.ares.repository;

import com.ares.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeAndActiveTrue(String code);
    List<Promotion> findByActiveTrue();
}
'@ | Set-Content (Join-Path $d15Repo "PromotionRepository.java") -Encoding UTF8

# --- DAY 16: MarketingService & MarketingApiController ---
$d16 = Create-PayloadDir 16
$d16Model = Join-Path $d16 "src\main\java\com\ares\model"
$d16Repo = Join-Path $d16 "src\main\java\com\ares\repository"
$d16Svc = Join-Path $d16 "src\main\java\com\ares\service"
$d16Ctrl = Join-Path $d16 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d16Model | Out-Null
New-Item -ItemType Directory -Force -Path $d16Repo | Out-Null
New-Item -ItemType Directory -Force -Path $d16Svc | Out-Null
New-Item -ItemType Directory -Force -Path $d16Ctrl | Out-Null

@'
package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_messages")
public class BroadcastMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String message;

    private String targetAudience = "ALL"; // ALL, VIP, NEW

    private LocalDateTime createdAt = LocalDateTime.now();

    public BroadcastMessage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
'@ | Set-Content (Join-Path $d16Model "BroadcastMessage.java") -Encoding UTF8

@'
package com.ares.repository;

import com.ares.model.BroadcastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastRepository extends JpaRepository<BroadcastMessage, Long> {
    List<BroadcastMessage> findAllByOrderByCreatedAtDesc();
}
'@ | Set-Content (Join-Path $d16Repo "BroadcastRepository.java") -Encoding UTF8

@'
package com.ares.service;

import com.ares.model.BroadcastMessage;
import com.ares.model.Promotion;
import com.ares.repository.BroadcastRepository;
import com.ares.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketingService {

    private final PromotionRepository promotionRepository;
    private final BroadcastRepository broadcastRepository;

    public MarketingService(PromotionRepository promotionRepository, BroadcastRepository broadcastRepository) {
        this.promotionRepository = promotionRepository;
        this.broadcastRepository = broadcastRepository;
    }

    public Long calculateDiscount(String code, Long orderTotal) {
        Promotion promo = promotionRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại hoặc đã hết hạn"));

        LocalDateTime now = LocalDateTime.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            throw new IllegalArgumentException("Chương trình khuyến mãi chưa bắt đầu");
        }
        if (promo.getValidTo() != null && now.isAfter(promo.getValidTo())) {
            throw new IllegalArgumentException("Mã khuyến mãi đã hết hạn");
        }
        if (promo.getMinOrderValue() != null && orderTotal < promo.getMinOrderValue()) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + promo.getMinOrderValue() + " ₫");
        }

        long discount = 0;
        if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
            discount = (orderTotal * promo.getDiscountValue()) / 100;
            if (promo.getMaxDiscount() != null && promo.getMaxDiscount() > 0 && discount > promo.getMaxDiscount()) {
                discount = promo.getMaxDiscount();
            }
        } else {
            discount = promo.getDiscountValue();
        }

        return Math.min(discount, orderTotal);
    }

    public BroadcastMessage createBroadcast(String title, String message, String target) {
        BroadcastMessage bm = new BroadcastMessage();
        bm.setTitle(title);
        bm.setMessage(message);
        bm.setTargetAudience(target != null ? target : "ALL");
        return broadcastRepository.save(bm);
    }

    public List<BroadcastMessage> getRecentBroadcasts() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc();
    }
}
'@ | Set-Content (Join-Path $d16Svc "MarketingService.java") -Encoding UTF8

@'
package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.BroadcastMessage;
import com.ares.service.MarketingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class MarketingApiController {

    private final MarketingService marketingService;

    public MarketingApiController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @PostMapping("/validate-coupon")
    public ResponseEntity<ApiResponse> validateCoupon(@RequestBody CouponRequest req) {
        try {
            Long discount = marketingService.calculateDiscount(req.code(), req.orderTotal());
            ApiResponse res = ApiResponse.ok("Áp dụng mã ưu đãi thành công");
            res.setData(Map.of("code", req.code(), "discount", discount));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/broadcast")
    public ResponseEntity<ApiResponse> broadcast(@RequestBody BroadcastRequest req) {
        BroadcastMessage bm = marketingService.createBroadcast(req.title(), req.message(), req.target());
        ApiResponse res = ApiResponse.ok("Đã phát thông báo chiến dịch Marketing thành công");
        res.setData(bm);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/broadcasts")
    public ResponseEntity<ApiResponse> listBroadcasts() {
        ApiResponse res = ApiResponse.ok("Thông báo mới");
        res.setData(marketingService.getRecentBroadcasts());
        return ResponseEntity.ok(res);
    }

    public record CouponRequest(String code, Long orderTotal) {}
    public record BroadcastRequest(String title, String message, String target) {}
}
'@ | Set-Content (Join-Path $d16Ctrl "MarketingApiController.java") -Encoding UTF8

Write-Host "Setup Days 9-16 completed."
