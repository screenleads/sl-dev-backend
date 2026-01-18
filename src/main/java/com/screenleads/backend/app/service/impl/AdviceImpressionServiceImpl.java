package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.AdviceImpression;
import com.screenleads.backend.app.domain.repository.AdviceImpressionRepository;
import com.screenleads.backend.app.service.AdviceImpressionService;
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
public class AdviceImpressionServiceImpl implements AdviceImpressionService {

    private final AdviceImpressionRepository adviceImpressionRepository;

    @Override
    public AdviceImpression createImpression(AdviceImpression impression) {
        log.debug("Creating new impression for advice ID: {}", impression.getAdvice().getId());
        return adviceImpressionRepository.save(impression);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdviceImpression> getImpressionById(Long id) {
        return adviceImpressionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceImpression> getImpressionsByAdviceId(Long adviceId) {
        return adviceImpressionRepository.findByAdvice_Id(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceImpression> getImpressionsByDeviceId(Long deviceId) {
        return adviceImpressionRepository.findByDevice_Id(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceImpression> getImpressionsByCustomerId(Long customerId) {
        return adviceImpressionRepository.findByCustomer_Id(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceImpression> getImpressionsBySessionId(String sessionId) {
        return adviceImpressionRepository.findBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceImpression> getImpressionsByAdviceIdAndDateRange(
            Long adviceId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return adviceImpressionRepository.findByAdviceIdAndDateRange(adviceId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countImpressionsByAdviceId(Long adviceId) {
        return adviceImpressionRepository.countByAdvice_Id(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countInteractiveImpressionsByAdviceId(Long adviceId) {
        return adviceImpressionRepository.countByAdvice_IdAndWasInteractiveTrue(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUniqueCustomerCountByAdviceId(Long adviceId) {
        List<Long> customerIds = adviceImpressionRepository.findUniqueCustomerIdsByAdviceId(adviceId);
        return (long) customerIds.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateAverageDurationByAdviceId(Long adviceId) {
        Double avgDuration = adviceImpressionRepository.calculateAverageDurationByAdviceId(adviceId);
        return avgDuration != null ? avgDuration : 0.0;
    }
}
