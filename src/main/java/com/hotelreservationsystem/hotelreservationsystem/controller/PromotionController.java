package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Promotion;
import com.hotelreservationsystem.hotelreservationsystem.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public String showPromotions(Model model) {
        List<Promotion> activePromotions = promotionService.getAllActivePromotions();
        model.addAttribute("promotions", activePromotions);
        return "promotions";
    }

    @GetMapping("/{id}")
    public String showPromotionDetail(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.getPromotionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        model.addAttribute("promotion", promotion);
        return "promotion-detail";
    }
}
