package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PromotionDTO getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Override
    public PromotionDTO savePromotion(PromotionDTO dto) {
        Promotion entity = convertToEntity(dto);
        return convertToDto(promotionRepository.save(entity));
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion entity = convertToEntity(dto);
        entity.setId(id);
        return convertToDto(promotionRepository.save(entity));
    }

    @Override
    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    private PromotionDTO convertToDto(Promotion entity) {
        return new PromotionDTO(
                entity.getId(),
                entity.getLegal_url(),
                entity.getUrl(),
                entity.getDescription(),
                entity.getTemplateHtml());
    }

    private Promotion convertToEntity(PromotionDTO dto) {
        return Promotion.builder()
                .id(dto.id())
                .legal_url(dto.legal_url())
                .url(dto.url())
                .description(dto.description())
                .templateHtml(dto.templateHtml())
                .build();
    }
}
