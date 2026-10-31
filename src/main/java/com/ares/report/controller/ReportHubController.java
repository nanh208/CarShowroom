package com.ares.report.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportHubController {
    @GetMapping("/reports")
    public String page(HttpSession session, Model model) {
        Object user = session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "reports/index";
    }
}
