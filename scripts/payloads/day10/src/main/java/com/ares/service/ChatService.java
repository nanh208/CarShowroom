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
        msg.setCustomerName(customerName != null && !customerName.isBlank() ? customerName : "KhÃ¡ch hÃ ng");
        msg.setContent(content.trim());
        msg.setFromAdmin(fromAdmin);
        ChatMessage saved = chatRepository.save(msg);

        // Tin nháº¯n chÃ o má»«ng tá»± Ä‘á»™ng cho khÃ¡ch má»Ÿ phiÃªn Ä‘áº§u tiÃªn
        if (first && !fromAdmin) {
            ChatMessage welcome = new ChatMessage();
            welcome.setSessionId(sessionId);
            welcome.setCustomerName("Ares Auto Assistant");
            welcome.setFromAdmin(true);
            welcome.setContent("Xin chÃ o " + msg.getCustomerName() + "! Cáº£m Æ¡n quÃ½ khÃ¡ch Ä‘Ã£ káº¿t ná»‘i Ä‘áº¿n Ares Auto Showroom. Em lÃ  chuyÃªn viÃªn tÆ° váº¥n há»— trá»£ quÃ½ khÃ¡ch.");
            chatRepository.save(welcome);
        }

        return saved;
    }

    public List<String> getAllSessions() {
        return chatRepository.findDistinctSessions();
    }
}
