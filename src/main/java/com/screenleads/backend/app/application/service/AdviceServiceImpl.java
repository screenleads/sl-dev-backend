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
import com.screenleads.backend.app.web.dto.TimeRangeDTO;

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
    public AdviceDTO saveAdvice(AdviceDTO adviceDTO) {
        Advice advice = convertToEntity(adviceDTO);
        // Optional<Advice> exist = adviceRepository.findById(advice.getId());
        // if (exist.isPresent())
        // return convertToDTO(exist.get());
        Advice savedAdvice = adviceRepository.save(advice);
        return convertToDTO(savedAdvice);
    }

    @Override
    public AdviceDTO updateAdvice(Long id, AdviceDTO adviceDTO) {
        Advice advice = adviceRepository.findById(id).orElseThrow();
        advice.setCustomInterval(adviceDTO.customInterval());
        advice.setDescription(adviceDTO.description());
        advice.setInterval(adviceDTO.interval());
        // advice.setMedia(mediaRepository.findById(adviceDTO.media().getId()).get());
        if (adviceDTO.media() != null && adviceDTO.media().getId() != null) {
            Media media = mediaRepository.findById(adviceDTO.media().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada"));
            advice.setMedia(media);
        } else {
            advice.setMedia(null); // o mantenla como estaba, según tu lógica
        }
        advice.setPromotion(adviceDTO.promotion());
        // Limpia reglas actuales (esto activa el orphanRemoval)
        advice.getVisibilityRules().clear();

        // Si hay reglas nuevas en el DTO, reconstrúyelas manualmente
        List<AdviceVisibilityRule> newRules = new ArrayList<>();

        if (adviceDTO.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleDto : adviceDTO.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleDto.getDay());
                rule.setAdvice(advice); // ¡clave! relacionar con el padre

                List<TimeRange> newRanges = new ArrayList<>();
                if (ruleDto.getTimeRanges() != null) {
                    for (TimeRange rangeDto : ruleDto.getTimeRanges()) {
                        TimeRange range = new TimeRange();
                        range.setFromTime(rangeDto.getFromTime());
                        range.setToTime(rangeDto.getToTime());
                        range.setRule(rule); // ¡clave! relacionar con la regla
                        newRanges.add(range);
                    }
                }
                rule.setTimeRanges(newRanges);
                newRules.add(rule);
            }
        }

        advice.getVisibilityRules().addAll(newRules);
        logger.info("advice object: {}", advice);

        Advice updatedAdvice = adviceRepository.save(advice);
        return convertToDTO(updatedAdvice);
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
