package com.ares.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AuctionViewController {

    @GetMapping("/auctions")
    public String auctionsPage() {
        return "auctions";
    }

    @GetMapping("/auctions/{id}")
    public String auctionDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("auctionId", id);
        return "auction-detail";
    }

    @GetMapping("/admin/auctions")
    public String adminAuctionsPage() {
        return "admin-auctions";
    }
}
