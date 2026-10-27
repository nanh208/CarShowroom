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
        ApiResponse res = ApiResponse.ok("Danh sÃ¡ch tin nháº¯n");
        res.setData(list);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{session}")
    public ResponseEntity<ApiResponse> sendMessage(@PathVariable String session,
                                                   @RequestBody SendMessageRequest req) {
        if (req.content() == null || req.content().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ná»™i dung tin nháº¯n trá»‘ng"));
        }
        ChatMessage sent = chatService.sendMessage(session, req.customerName(), req.content(), Boolean.TRUE.equals(req.fromAdmin()));
        ApiResponse res = ApiResponse.ok("Gá»­i tin nháº¯n thÃ nh cÃ´ng");
        res.setData(sent);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/sessions")
    public ResponseEntity<ApiResponse> getAdminSessions() {
        ApiResponse res = ApiResponse.ok("Danh sÃ¡ch phiÃªn chat");
        res.setData(chatService.getAllSessions());
        return ResponseEntity.ok(res);
    }

    public record SendMessageRequest(String customerName, String content, Boolean fromAdmin) {}
}
