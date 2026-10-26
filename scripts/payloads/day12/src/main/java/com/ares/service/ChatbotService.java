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

        // Máº·c Ä‘á»‹nh pháº£n há»“i thÃ´ng minh náº¿u chÆ°a khá»›p tá»« khÃ³a cá»¥ thá»ƒ
        if (normalized.contains("gia") || normalized.contains("bang gia")) {
            return Optional.of("Báº£ng giÃ¡ xe Ares Auto luÃ´n cáº­p nháº­t Æ°u Ä‘Ã£i má»›i nháº¥t! QuÃ½ khÃ¡ch cÃ³ thá»ƒ xem trá»±c tiáº¿p táº¡i showroom hoáº·c Ä‘á»ƒ láº¡i SÄT Ä‘á»ƒ nháº­n catalog chi tiáº¿t.");
        } else if (normalized.contains("tra gop") || normalized.contains("vay")) {
            return Optional.of("Ares há»— trá»£ tráº£ gÃ³p lÃ£i suáº¥t 0% liÃªn káº¿t vá»›i hÆ¡n 10 ngÃ¢n hÃ ng lá»›n, thá»i háº¡n linh hoáº¡t Ä‘áº¿n 8 nÄƒm!");
        } else if (normalized.contains("lai thu") || normalized.contains("test drive")) {
            return Optional.of("QuÃ½ khÃ¡ch cÃ³ thá»ƒ Ä‘áº·t lá»‹ch lÃ¡i thá»­ miá»…n phÃ­ táº­n nhÃ  hoáº·c táº¡i Showroom Ares. HÃ£y gá»­i sá»‘ Ä‘iá»‡n thoáº¡i cá»§a báº¡n nhÃ©!");
        }

        return Optional.empty();
    }

    private String removeDiacritics(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('Ä‘', 'd').replace('Ä', 'D');
    }
}
