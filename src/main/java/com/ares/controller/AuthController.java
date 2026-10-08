package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.RegisterRequest;
import com.ares.model.User;
import com.ares.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ares.service.GoogleOAuthService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final GoogleOAuthService googleOAuthService;

    @Autowired
    public AuthController(UserService userService, GoogleOAuthService googleOAuthService) {
        this.userService = userService;
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/home";
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session) {
        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            session.setAttribute("currentUser", userOpt.get());
            return "redirect:/home";
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegisterForm(@ModelAttribute RegisterRequest req, Model model) {
        ApiResponse response = userService.register(req);
        if (response.isSuccess()) {
            return "redirect:/login?registered=true";
        }
        model.addAttribute("errorMessage", response.getMessage());
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "forgot-password";
    }

    /**
     * Implements Google OAuth Workflow from Activity Diagram:
     * User selects Google -> System redirects to Google login ->
     * Google verifies and returns email -> System creates or links account to Gmail ->
     * System marks as verified with Gmail (as a base) -> Home
     */
    /**
     * Redirects directly to REAL Google OAuth 2.0 at https://accounts.google.com/
     */
    @GetMapping("/oauth2/google")
    public String redirectToRealGoogle() {
        return "redirect:" + googleOAuthService.buildAuthorizationUrl();
    }

    /**
     * Handles callback from https://accounts.google.com/ with real authorization code
     */
    @GetMapping("/login/oauth2/code/google")
    public String handleGoogleLoginCallback(@RequestParam(value = "code", required = false) String code,
                                           @RequestParam(value = "error", required = false) String error,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Đăng nhập Google bị hủy: " + error);
            return "redirect:/login";
        }
        if (code == null || code.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không nhận được mã xác thực từ Google!");
            return "redirect:/login";
        }

        try {
            Map<String, String> userInfo = googleOAuthService.processCallback(code);
            String email = userInfo.get("email");
            String name = userInfo.get("name");
            User oauthUser = userService.processGoogleOAuth(email, name);
            session.setAttribute("currentUser", oauthUser);
            return "redirect:/home";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xác thực Google: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/oauth2/callback")
    public String handleLegacyOAuthCallback(@RequestParam(value = "email", required = false, defaultValue = "manhmxtp000751@gmail.com") String email,
                                           @RequestParam(value = "name", defaultValue = "Google User") String name,
                                           HttpSession session) {
        if (email == null || email.trim().isEmpty()) {
            email = "manhmxtp000751@gmail.com";
        }
        User oauthUser = userService.processGoogleOAuth(email, name);
        session.setAttribute("currentUser", oauthUser);
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String homePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("user", currentUser);
        return "home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
