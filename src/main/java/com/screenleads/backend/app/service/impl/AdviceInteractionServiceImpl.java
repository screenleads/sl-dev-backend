package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.AdviceInteraction;
import com.screenleads.backend.app.domain.model.InteractionType;
import com.screenleads.backend.app.domain.repository.AdviceInteractionRepository;
import com.screenleads.backend.app.service.AdviceInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdviceInteractionServiceImpl implements AdviceInteractionService {

    private final AdviceInteractionRepository adviceInteractionRepository;

    @Override
    public AdviceInteraction createInteraction(AdviceInteraction interaction) {
        log.debug("Creating new interaction type {} for impression ID: {}",
                interaction.getType(), interaction.getImpression().getId());
        return adviceInteractionRepository.save(interaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdviceInteraction> getInteractionById(Long id) {
        return adviceInteractionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceInteraction> getInteractionsByImpressionId(Long impressionId) {
        return adviceInteractionRepository.findByImpression_Id(impressionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceInteraction> getInteractionsByCustomerId(Long customerId) {
        return adviceInteractionRepository.findByCustomer_Id(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceInteraction> getInteractionsByType(InteractionType type) {
        return adviceInteractionRepository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceInteraction> getInteractionsByAdviceId(Long adviceId) {
        return adviceInteractionRepository.findByAdviceId(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceInteraction> getInteractionsByAdviceIdAndDateRange(
            Long adviceId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return adviceInteractionRepository.findByAdviceIdAndDateRange(adviceId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countInteractionsByAdviceId(Long adviceId) {
        return adviceInteractionRepository.countByAdviceId(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countInteractionsByAdviceIdAndType(Long adviceId, InteractionType type) {
        return adviceInteractionRepository.countByAdviceIdAndType(adviceId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countConversionsByAdviceId(Long adviceId) {
        return adviceInteractionRepository.countConversionsByAdviceId(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUniqueCustomerCountByAdviceId(Long adviceId) {
        List<Long> customerIds = adviceInteractionRepository.findUniqueCustomerIdsByAdviceId(adviceId);
        return (long) customerIds.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateAverageDurationByAdviceId(Long adviceId) {
        Double avgDuration = adviceInteractionRepository.calculateAverageDurationByAdviceId(adviceId);
        return avgDuration != null ? avgDuration : 0.0;
    }
}
