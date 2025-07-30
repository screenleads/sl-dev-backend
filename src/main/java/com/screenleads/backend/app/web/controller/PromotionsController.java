package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionsController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public List<PromotionDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/{id}")
    public PromotionDTO getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id);
    }

    @PostMapping
    public PromotionDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.savePromotion(promotionDTO);
    }

    @PutMapping("/{id}")
    public PromotionDTO updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }
}
