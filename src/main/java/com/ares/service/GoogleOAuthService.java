package com.ares.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleOAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Generates Google OAuth 2.0 authorization URL redirecting directly to https://accounts.google.com/
     */
    public String buildAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&prompt=select_account";
    }

    /**
     * Exchanges authorization code with Google for tokens and user profile
     */
    public Map<String, String> processCallback(String code) throws Exception {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        String requestBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret.trim(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) {
            throw new RuntimeException("Lỗi xác thực mã Token với Google: " + tokenResponse.body());
        }

        String tokenBody = tokenResponse.body();
        String accessToken = extractJsonField(tokenBody, "access_token");

        if (accessToken.isEmpty()) {
            throw new RuntimeException("Không tìm thấy access_token trong phản hồi của Google: " + tokenBody);
        }

        // Fetch user profile from Google UserInfo endpoint
        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userInfoResponse = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
        if (userInfoResponse.statusCode() != 200) {
            throw new RuntimeException("Không thể lấy thông tin người dùng từ Google: " + userInfoResponse.body());
        }

        String userBody = userInfoResponse.body();
        String email = extractJsonField(userBody, "email");
        String name = decodeUnicode(extractJsonField(userBody, "name"));
        String picture = extractJsonField(userBody, "picture");

        if (name.isEmpty()) {
            name = email;
        }

        Map<String, String> result = new HashMap<>();
        result.put("email", email);
        result.put("name", name);
        result.put("picture", picture);
        return result;
    }

    private String extractJsonField(String json, String fieldName) {
        String regex = "\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String decodeUnicode(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\' && i + 5 < str.length() && str.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(str.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
