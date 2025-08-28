package com.screenleads.backend.app.application.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.TimeRange;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.AdviceVisibilityRuleDTO;
import com.screenleads.backend.app.web.dto.TimeRangeDTO;

import jakarta.transaction.Transactional;

@Service
public class AdviceServiceImpl implements AdviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final AdviceRepository adviceRepository;
    private final MediaRepository mediaRepository;

    public AdviceServiceImpl(AdviceRepository adviceRepository, MediaRepository mediaRepository) {
        this.adviceRepository = adviceRepository;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public List<AdviceDTO> getAllAdvices() {
        return adviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public List<AdviceDTO> getVisibleAdvicesNow() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        return adviceRepository.findAll().stream()
                .filter(advice -> advice.getVisibilityRules() != null &&
                        advice.getVisibilityRules().stream()
                                .anyMatch(rule -> rule.getDay() == today &&
                                        rule.getTimeRanges() != null &&
                                        rule.getTimeRanges().stream()
                                                .anyMatch(range -> !time.isBefore(range.getFromTime()) &&
                                                        !time.isAfter(range.getToTime()))))
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AdviceDTO> getAdviceById(Long id) {
        return adviceRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public AdviceDTO saveAdvice(AdviceDTO dto) {
        // 1) Crear Advice "limpio"
        Advice advice = new Advice();
        advice.setDescription(dto.description());
        advice.setCustomInterval(dto.customInterval());
        advice.setInterval(dto.interval());

        // 2) Resolver Media (si viene id)
        if (dto.media() != null && dto.media().getId() != null) {
            Media media = mediaRepository.findById(dto.media().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada"));
            advice.setMedia(media);
        } else {
            advice.setMedia(null);
        }

        // 3) Resolver Promotion (si trabajas por id; si no, ajusta a tu diseño)
        if (dto.promotion() != null && dto.promotion().getId() != null) {
            advice.setPromotion(dto.promotion()); // o promotionRepository.getReferenceById(dto.promotion().getId())
        } else {
            advice.setPromotion(null);
        }

        // 4) (Multi-tenant) Fijar compañía si no es admin
        // CompanyWriteGuards.enforceCompanyOnWrite(advice);

        // 5) Construir visibilityRules + timeRanges
        advice.setVisibilityRules(new ArrayList<>());
        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleIn : dto.visibilityRules()) { // <-- ENTIDAD, no DTO
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleIn.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleIn.getTimeRanges() != null) {
                    for (TimeRange rangeIn : ruleIn.getTimeRanges()) { // <-- ENTIDAD, no DTO
                        TimeRange tr = new TimeRange();
                        tr.setFromTime(rangeIn.getFromTime());
                        tr.setToTime(rangeIn.getToTime());
                        tr.setRule(rule);
                        ranges.add(tr);
                    }
                }
                rule.setTimeRanges(ranges);
                advice.getVisibilityRules().add(rule);
            }
        }

        // 6) Guardar
        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AdviceDTO updateAdvice(Long id, AdviceDTO dto) {
        Advice advice = adviceRepository.findById(id).orElseThrow();

        advice.setCustomInterval(dto.customInterval());
        advice.setDescription(dto.description());
        advice.setInterval(dto.interval());

        if (dto.media() != null && dto.media().getId() != null) {
            Media media = mediaRepository.findById(dto.media().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada"));
            advice.setMedia(media);
        } else {
            advice.setMedia(null);
        }

        advice.setPromotion(dto.promotion());

        // Reemplazo completo de reglas (orphanRemoval aplicado)
        advice.getVisibilityRules().clear();

        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleDto : dto.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleDto.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleDto.getTimeRanges() != null) {
                    for (TimeRange rangeDto : ruleDto.getTimeRanges()) {
                        TimeRange tr = new TimeRange();
                        tr.setFromTime(rangeDto.getFromTime());
                        tr.setToTime(rangeDto.getToTime());
                        tr.setRule(rule);
                        ranges.add(tr);
                    }
                }
                rule.setTimeRanges(ranges);

                // Añade a la colección MANAGED del padre
                advice.getVisibilityRules().add(rule);
            }
        }

        Advice saved = adviceRepository.save(advice); // asegura flush/commit
        return convertToDTO(saved);
    }

    @Override
    public void deleteAdvice(Long id) {
        adviceRepository.deleteById(id);
    }

    // Convert Advice Entity to AdviceDTO
    private AdviceDTO convertToDTO(Advice advice) {
        Media media = null;
        Optional<Media> aux = mediaRepository.findById(advice.getMedia().getId());
        if (aux.isPresent()) {
            media = aux.get();
        }
        return new AdviceDTO(advice.getId(), advice.getDescription(), advice.getCustomInterval(), advice.getInterval(),
                media, advice.getPromotion(), advice.getVisibilityRules());
    }

    // Convert AdviceDTO to Advice Entity
    private Advice convertToEntity(AdviceDTO adviceDTO) {
        Advice advice = new Advice();
        advice.setId(adviceDTO.id());
        advice.setDescription(adviceDTO.description());
        advice.setCustomInterval(adviceDTO.customInterval());
        advice.setInterval(adviceDTO.interval());
        advice.setMedia(mediaRepository.findById(adviceDTO.media().getId()).get());
        advice.setPromotion(adviceDTO.promotion());
        List<AdviceVisibilityRule> rules = new ArrayList<>();
        if (adviceDTO.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleDto : adviceDTO.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleDto.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleDto.getTimeRanges() != null) {
                    for (TimeRange rangeDto : ruleDto.getTimeRanges()) {
                        TimeRange range = new TimeRange();
                        range.setFromTime(rangeDto.getFromTime());
                        range.setToTime(rangeDto.getToTime());
                        range.setRule(rule);
                        ranges.add(range);
                    }
                }
                rule.setTimeRanges(ranges);
                rules.add(rule);
            }
        }
        advice.setVisibilityRules(rules);
        return advice;
    }
}
