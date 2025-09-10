package com.screenleads.backend.app.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.MediaUpsertDTO;
import com.screenleads.backend.app.web.dto.PromotionRefDTO;

@Service
public class AdviceServiceImpl implements AdviceService {
    private static final Logger log = LoggerFactory.getLogger(AdviceServiceImpl.class);

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
    public List<AdviceDTO> getVisibleAdvicesNow(ZoneId zoneId) {
        enableCompanyFilterIfNeeded();

        ZoneId zone = (zoneId != null) ? zoneId : ZoneId.systemDefault();
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        DayOfWeek today = nowZ.getDayOfWeek();
        LocalTime time = nowZ.toLocalTime();
        LocalDate date = nowZ.toLocalDate();

        return adviceRepository.findAll().stream()
                .filter(advice -> advice.getVisibilityRules() != null && !advice.getVisibilityRules().isEmpty())
                .filter(advice -> advice.getVisibilityRules().stream().anyMatch(rule -> {
                    // día de semana
                    boolean dayOk = rule.getDay() == today;

                    // rango de fechas opcional (inclusive)
                    boolean dateOk = true;
                    if (rule.getStartDate() != null) {
                        dateOk = !date.isBefore(rule.getStartDate());
                    }
                    if (dateOk && rule.getEndDate() != null) {
                        dateOk = !date.isAfter(rule.getEndDate());
                    }

                    // rango horario dentro de ese día
                    boolean timeOk = rule.getTimeRanges() != null && rule.getTimeRanges().stream()
                            .anyMatch(range -> !time.isBefore(range.getFromTime()) &&
                                    !time.isAfter(range.getToTime()));

                    return dayOk && dateOk && timeOk;
                }))
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

        advice.setCompany(resolveCompanyForWrite(dto.company(), /* existing */ null));
        advice.setMedia(resolveMediaFromDto(dto.media(), advice.getCompany()));
        advice.setPromotion(resolvePromotionFromDto(dto.promotion()));

        // reglas
        advice.setVisibilityRules(new ArrayList<>());
        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleIn : dto.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleIn.getDay());
                // NUEVO: fechas opcionales
                rule.setStartDate(ruleIn.getStartDate());
                rule.setEndDate(ruleIn.getEndDate());
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

        advice.setDescription(dto.description());
        advice.setCustomInterval(dto.customInterval());
        advice.setInterval(dto.interval());

        advice.setCompany(resolveCompanyForWrite(dto.company(), advice.getCompany()));
        advice.setMedia(resolveMediaFromDto(dto.media(), advice.getCompany()));
        advice.setPromotion(resolvePromotionFromDto(dto.promotion()));

        // Reemplazo completo de reglas
        advice.getVisibilityRules().clear();
        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleDto : dto.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleDto.getDay());
                // NUEVO: fechas opcionales
                rule.setStartDate(ruleDto.getStartDate());
                rule.setEndDate(ruleDto.getEndDate());
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
        var mediaDto = (advice.getMedia() != null)
                ? new com.screenleads.backend.app.web.dto.MediaUpsertDTO(advice.getMedia().getId(),
                        advice.getMedia().getSrc())
                : null;

        var promoDto = (advice.getPromotion() != null)
                ? new com.screenleads.backend.app.web.dto.PromotionRefDTO(advice.getPromotion().getId())
                : null;

        var companyDto = (advice.getCompany() != null)
                ? new com.screenleads.backend.app.web.dto.CompanyRefDTO(advice.getCompany().getId(),
                        advice.getCompany().getName())
                : null;

        return new AdviceDTO(
                advice.getId(),
                advice.getDescription(),
                advice.getCustomInterval(),
                advice.getInterval(),
                mediaDto,
                promoDto,
                advice.getVisibilityRules(),
                companyDto);
    }

    private Media resolveMediaFromDto(MediaUpsertDTO incoming, Company ownerCompany) {
        if (incoming == null)
            return null;

        if (incoming.id() != null && incoming.id() > 0) {
            return mediaRepository.findById(incoming.id())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada (id=" + incoming.id() + ")"));
        }

        String src = incoming.src() != null ? incoming.src().trim() : null;
        if (src != null && !src.isEmpty()) {
            Media existing = mediaRepository.findBySrc(src).orElse(null);
            if (existing != null)
                return existing;

            Media m = new Media();
            m.setSrc(src);
            return mediaRepository.save(m);
        }

        return null;
    }

    private Promotion resolvePromotionFromDto(PromotionRefDTO incoming) {
        if (incoming == null)
            return null;
        Long id = incoming.id();
        if (id == null || id <= 0)
            return null;
        return entityManager.getReference(Promotion.class, id);
    }

    private Company resolveCompanyForWrite(CompanyRefDTO incoming, Company current) {
        boolean isAdmin = isCurrentUserAdmin();
        Long userCompanyId = currentCompanyId();

        if (!isAdmin) {
            if (userCompanyId != null) {
                Company c = new Company();
                c.setId(userCompanyId);
                return c;
            }
            return current;
        }

        Long desiredId = (incoming != null) ? incoming.id() : null;
        if (desiredId != null && desiredId > 0) {
            Company c = new Company();
            c.setId(desiredId);
            return c;
        }
        if (current != null)
            return current;
        if (userCompanyId != null) {
            Company c = new Company();
            c.setId(userCompanyId);
            return c;
        }
        return null;
    }

    /** Activa el filtro "companyFilter" si NO es admin. */
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
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
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
