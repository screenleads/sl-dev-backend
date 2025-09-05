package com.screenleads.backend.app.application.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.model.TimeRange;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;

@Service
public class AdviceServiceImpl implements AdviceService {
    private static final Logger logger = LoggerFactory.getLogger(AdviceServiceImpl.class);

    private final AdviceRepository adviceRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdviceServiceImpl(AdviceRepository adviceRepository,
            MediaRepository mediaRepository,
            UserRepository userRepository) {
        this.adviceRepository = adviceRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    // ======================= LECTURAS =======================

    @Override
    @Transactional
    public List<AdviceDTO> getAllAdvices() {
        enableCompanyFilterIfNeeded();
        return adviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AdviceDTO> getVisibleAdvicesNow() {
        enableCompanyFilterIfNeeded();

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
    @Transactional
    public Optional<AdviceDTO> getAdviceById(Long id) {
        enableCompanyFilterIfNeeded();
        return adviceRepository.findById(id).map(this::convertToDTO);
    }

    // ============================= ESCRITURAS =============================

    @Override
    @Transactional
    public AdviceDTO saveAdvice(AdviceDTO dto) {
        enableCompanyFilterIfNeeded();

        Advice advice = new Advice();
        advice.setDescription(dto.description());
        advice.setCustomInterval(dto.customInterval());
        advice.setInterval(dto.interval());

        // Media: id -> existente; solo src -> crear; null -> null
        advice.setMedia(resolveMediaFromDto(dto.media()));

        // Promotion: id>0 -> referencia; 0/null -> null
        advice.setPromotion(resolvePromotionFromDto(dto.promotion()));

        enforceCompanyOnWrite(advice);

        advice.setVisibilityRules(new ArrayList<>());
        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleIn : dto.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleIn.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleIn.getTimeRanges() != null) {
                    for (TimeRange rangeIn : ruleIn.getTimeRanges()) {
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

        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AdviceDTO updateAdvice(Long id, AdviceDTO dto) {
        enableCompanyFilterIfNeeded();

        Advice advice = adviceRepository.findById(id).orElseThrow();

        advice.setCustomInterval(dto.customInterval());
        advice.setDescription(dto.description());
        advice.setInterval(dto.interval());

        advice.setMedia(resolveMediaFromDto(dto.media()));
        advice.setPromotion(resolvePromotionFromDto(dto.promotion()));

        // Reemplazo completo de reglas (orphanRemoval en entidad)
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
                advice.getVisibilityRules().add(rule);
            }
        }

        enforceCompanyOnWrite(advice);

        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAdvice(Long id) {
        enableCompanyFilterIfNeeded();
        adviceRepository.findById(id).ifPresent(adviceRepository::delete);
    }

    // ============================= MAPPERS/HELPERS =============================

    private AdviceDTO convertToDTO(Advice advice) {
        return new AdviceDTO(
                advice.getId(),
                advice.getDescription(),
                advice.getCustomInterval(),
                advice.getInterval(),
                advice.getMedia(),
                advice.getPromotion(),
                advice.getVisibilityRules());
    }

    private Media resolveMediaFromDto(Media incoming) {
        if (incoming == null)
            return null;

        if (incoming.getId() != null && incoming.getId() > 0) {
            return mediaRepository.findById(incoming.getId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Media no encontrada (id=" + incoming.getId() + ")"));
        }

        String src = incoming.getSrc() != null ? incoming.getSrc().trim() : null;
        if (src != null && !src.isEmpty()) {
            Media m = new Media();
            m.setSrc(src);
            // Si tu Media requiere más campos (type, enabled, company...), setéalos aquí.
            return mediaRepository.save(m);
        }

        return null;
    }

    private Promotion resolvePromotionFromDto(Promotion incoming) {
        if (incoming == null)
            return null;
        Long id = incoming.getId();
        if (id == null || id <= 0)
            return null; // evita FK con 0
        // Referencia "perezosa" sin SELECT (válido si existe en BBDD)
        return entityManager.getReference(Promotion.class, id);
    }

    // ============================= MT Helpers =============================

    private void enableCompanyFilterIfNeeded() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
        if (isAdmin)
            return;

        Long companyId = resolveCompanyId(auth);
        if (companyId == null)
            return;

        Session session = entityManager.unwrap(Session.class);
        var filter = session.getEnabledFilter("companyFilter");
        if (filter == null) {
            session.enableFilter("companyFilter").setParameter("companyId", companyId);
        } else {
            filter.setParameter("companyId", companyId);
        }
    }

    private void enforceCompanyOnWrite(Advice advice) {
        if (advice == null || isCurrentUserAdmin())
            return;
        Long companyId = currentCompanyId();
        if (companyId == null)
            return;
        if (advice.getCompany() == null || advice.getCompany().getId() == null
                || !companyId.equals(advice.getCompany().getId())) {
            Company c = new Company();
            c.setId(companyId);
            advice.setCompany(c);
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return resolveCompanyId(auth);
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof com.screenleads.backend.app.domain.model.User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }

        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        return null;
    }
}
