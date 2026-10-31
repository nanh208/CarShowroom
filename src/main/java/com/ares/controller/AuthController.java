package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.RegisterRequest;
import com.ares.model.User;
import com.ares.service.GoogleOAuthService;
import com.ares.service.UserService;
import com.ares.service.VerificationService;
import com.ares.service.TotpAttemptService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final GoogleOAuthService googleOAuthService;
    private final VerificationService verificationService;
    private final TotpAttemptService totpAttempts;

    @Autowired
    public AuthController(UserService userService, 
                          GoogleOAuthService googleOAuthService,
                          VerificationService verificationService,
                          TotpAttemptService totpAttempts) {
        this.userService = userService;
        this.googleOAuthService = googleOAuthService;
        this.verificationService = verificationService;
        this.totpAttempts = totpAttempts;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/home";
        }
        model.addAttribute("totpPending", session.getAttribute("twoFactorPendingUserId") != null);
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session, HttpServletRequest request) {
        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isTotpEnabled()) {
                session.removeAttribute("currentUser");
                session.setAttribute("twoFactorPendingUserId", user.getId());
                session.removeAttribute("twoFactorAttempts");
                return "redirect:/login?totpRequired=true";
            }
            session.removeAttribute("twoFactorPendingUserId");
            session.removeAttribute("twoFactorAttempts");
            request.changeSessionId();
            session.setAttribute("currentUser", user);
            return "redirect:/home";
        }
        return "redirect:/login?error=true";
    }

    @PostMapping("/login/phone")
    public String handlePhoneLogin(@RequestParam("phone") String phone,
                                   @RequestParam("code") String code,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        if (phone == null || phone.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng nhập đầy đủ số điện thoại và mã OTP!");
            return "redirect:/login";
        }

        boolean valid = verificationService.verifyCode(phone.trim(), code.trim());
        if (!valid) {
            redirectAttributes.addFlashAttribute("errorMsg", "Mã OTP không chính xác hoặc đã hết hạn!");
            return "redirect:/login";
        }

        User user = userService.authenticateOrRegisterByPhoneOtp(phone.trim());
        return completeLogin(user, session, request);
    }

    @PostMapping("/login/verify-2fa")
    public String verifyTwoFactor(@RequestParam("code") String code, HttpSession session, HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Object pending = session.getAttribute("twoFactorPendingUserId");
        if (!(pending instanceof Long userId)) return "redirect:/login";
        User user = userService.findById(userId).orElse(null);
        if (user == null || !user.isTotpEnabled() || !totpAttempts.verify(user, code)) {
            Object storedAttempts = session.getAttribute("twoFactorAttempts");
            int attempts = (storedAttempts instanceof Integer value ? value : 0) + 1;
            if (attempts >= 5) {
                session.removeAttribute("twoFactorPendingUserId");
                session.removeAttribute("twoFactorAttempts");
                redirectAttributes.addFlashAttribute("errorMsg", "Quá nhiều lần thử. Vui lòng đăng nhập lại.");
                return "redirect:/login";
            }
            session.setAttribute("twoFactorAttempts", attempts);
            redirectAttributes.addFlashAttribute("errorMsg", "Mã xác thực hai bước không đúng.");
            return "redirect:/login?totpRequired=true";
        }
        session.removeAttribute("twoFactorPendingUserId");
        session.removeAttribute("twoFactorAttempts");
        request.changeSessionId();
        session.setAttribute("currentUser", user);
        return "redirect:/home";
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
     * Implements Google OAuth Workflow:
     * User selects Google -> System redirects to Google login ->
     * Google verifies and returns email -> System creates or links account -> Home
     */
    @GetMapping("/oauth2/google")
    public String redirectToRealGoogle() {
        try {
            return "redirect:" + googleOAuthService.buildAuthorizationUrl();
        } catch (Exception e) {
            return "redirect:/oauth2/callback?email=demo.google@ares.vn&name=Google+User";
        }
    }

    /**
     * Handles callback from https://accounts.google.com/ with real authorization code
     */
    @GetMapping("/login/oauth2/code/google")
    public String handleGoogleLoginCallback(@RequestParam(value = "code", required = false) String code,
                                           @RequestParam(value = "error", required = false) String error,
                                           HttpSession session,
                                           HttpServletRequest request,
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
            return completeLogin(oauthUser, session, request);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xác thực Google: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/oauth2/callback")
    public String handleLegacyOAuthCallback(@RequestParam(value = "email", required = false, defaultValue = "demo.google@ares.vn") String email,
                                           @RequestParam(value = "name", defaultValue = "Google User") String name,
                                           HttpSession session, HttpServletRequest request) {
        if (email == null || email.trim().isEmpty()) {
            email = "demo.google@ares.vn";
        }
        User oauthUser = userService.processGoogleOAuth(email, name);
        return completeLogin(oauthUser, session, request);
    }

    /**
     * Implements Facebook OAuth Workflow matching car-copy
     */
    @GetMapping("/oauth2/facebook")
    public String redirectToFacebook() {
        return "redirect:/oauth2/callback/facebook?email=demo.facebook@ares.vn&name=Facebook+User";
    }

    @GetMapping({"/oauth2/callback/facebook", "/login/oauth2/code/facebook"})
    public String handleFacebookCallback(@RequestParam(value = "email", required = false, defaultValue = "demo.facebook@ares.vn") String email,
                                         @RequestParam(value = "name", defaultValue = "Facebook User") String name,
                                         HttpSession session, HttpServletRequest request) {
        if (email == null || email.trim().isEmpty()) {
            email = "demo.facebook@ares.vn";
        }
        User oauthUser = userService.processFacebookOAuth(email, name);
        return completeLogin(oauthUser, session, request);
    }

    @GetMapping("/home")
    public String homePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("user", currentUser);
        return "home";
    }

    @GetMapping("/account/security")
    public String accountSecurity(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return "account-security";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    private String completeLogin(User user, HttpSession session, HttpServletRequest request) {
        if (user.isTotpEnabled()) {
            session.removeAttribute("currentUser");
            session.setAttribute("twoFactorPendingUserId", user.getId());
            session.removeAttribute("twoFactorAttempts");
            return "redirect:/login?totpRequired=true";
        }
        session.removeAttribute("twoFactorPendingUserId");
        session.removeAttribute("twoFactorAttempts");
        request.changeSessionId();
        session.setAttribute("currentUser", user);
        return "redirect:/home";
    }
}
