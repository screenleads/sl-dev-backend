package com.screenleads.backend.app.application.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        advice.setMedia(mediaRepository.findById(adviceDTO.media().getId()).get());
        advice.setPromotion(adviceDTO.promotion());

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
