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

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
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
    @GetMapping("/oauth2/google")
    public String handleGoogleOAuthMock(HttpSession session) {
        String mockGoogleEmail = "ares.customer@gmail.com";
        String mockGoogleName = "Ares VIP Member";

        User oauthUser = userService.processGoogleOAuth(mockGoogleEmail, mockGoogleName);
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
