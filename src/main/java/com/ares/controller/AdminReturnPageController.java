package com.ares.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReturnPageController {
    @GetMapping("/admin/returns")
    public String page() {
        return "admin-returns";
    }
}
