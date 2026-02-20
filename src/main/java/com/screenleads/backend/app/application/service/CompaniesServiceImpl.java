package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.BillingStatus;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.CompanyDTO;
import com.screenleads.backend.app.web.dto.MediaSlimDTO;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@Service
public class CompaniesServiceImpl implements CompaniesService {

    private static final String MEDIA_NOT_FOUND_WITH_ID = "Media no encontrado con id: ";
    private static final String COMPANY_NOT_FOUND_WITH_ID = "Company not found with id: ";

    private final MediaTypeRepository mediaTypeRepository;
    private final CompanyRepository companyRepository;
    private final MediaRepository mediaRepository;
    private final StripeBillingService stripeBillingService;

    public CompaniesServiceImpl(CompanyRepository companyRepository,
            MediaRepository mediaRepository,
            MediaTypeRepository mediaTypeRepository,
            StripeBillingService stripeBillingService) {
        this.companyRepository = companyRepository;
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
        this.stripeBillingService = stripeBillingService;
    }

    // ===================== READ =====================

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(CompanyDTO::id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyDTO> getCompanyById(Long id) {
        return companyRepository.findById(id).map(this::convertToDTO);
    }

    // ===================== WRITE =====================

    @Override
    @Transactional
    public CompanyDTO saveCompany(CompanyDTO companyDTO) {
        // Construir entidad base sin logo para evitar cascadas raras
        Company company = convertToEntity(companyDTO);
        company.setLogo(null);

        // Idempotencia por nombre
        Optional<Company> exist = companyRepository.findByName(company.getName());
        if (exist.isPresent()) {
            return convertToDTO(exist.get());
        }

        // Guardar primero la company para obtener ID
        Company savedCompany = companyRepository.save(company);

        // Primero intentar con logoId (viene del frontend)
        if (companyDTO.logoId() != null) {
            Media media = mediaRepository.findById(companyDTO.logoId())
                    .orElseThrow(() -> new RuntimeException(MEDIA_NOT_FOUND_WITH_ID + companyDTO.logoId()));
            if (media.getCompany() == null) {
                media.setCompany(savedCompany);
                mediaRepository.save(media);
            }
            savedCompany.setLogo(media);
        } else if (companyDTO.logo() != null) {
            if (companyDTO.logo().id() != null) {
                Media media = mediaRepository.findById(companyDTO.logo().id())
                        .orElseThrow(
                                () -> new RuntimeException(MEDIA_NOT_FOUND_WITH_ID + companyDTO.logo().id()));
                if (media.getCompany() == null) {
                    media.setCompany(savedCompany);
                    mediaRepository.save(media);
                }
                savedCompany.setLogo(media);

            } else if (companyDTO.logo().src() != null && !companyDTO.logo().src().isBlank()) {
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().src());
                newLogo.setCompany(savedCompany);
                setMediaTypeFromSrc(newLogo, companyDTO.logo().src());

                Media savedLogo = mediaRepository.save(newLogo);
                savedCompany.setLogo(savedLogo);

            } else {
                savedCompany.setLogo(null);
            }
        } else {
            savedCompany.setLogo(null);
        }

        savedCompany = companyRepository.save(savedCompany);
        return convertToDTO(savedCompany);
    }

    @Override
    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(COMPANY_NOT_FOUND_WITH_ID + id));
        company.setName(companyDTO.name());
        company.setObservations(companyDTO.observations());
        company.setPrimaryColor(companyDTO.primaryColor());
        company.setSecondaryColor(companyDTO.secondaryColor());
        company.setStripeCustomerId(companyDTO.stripeCustomerId());
        company.setStripeSubscriptionId(companyDTO.stripeSubscriptionId());
        company.setStripeSubscriptionItemId(companyDTO.stripeSubscriptionItemId());
        if (companyDTO.billingStatus() != null) {
            company.setBillingStatus(companyDTO.billingStatus().name());
        }

        // Primero intentar con logoId (viene del frontend)
        if (companyDTO.logoId() != null) {
            Media media = mediaRepository.findById(companyDTO.logoId())
                    .orElseThrow(() -> new RuntimeException(MEDIA_NOT_FOUND_WITH_ID + companyDTO.logoId()));
            if (media.getCompany() == null) {
                media.setCompany(company);
                mediaRepository.save(media);
            }
            company.setLogo(media);
        } else if (companyDTO.logo() != null) {
            if (companyDTO.logo().id() != null) {
                Media media = mediaRepository.findById(companyDTO.logo().id())
                        .orElseThrow(
                                () -> new RuntimeException(MEDIA_NOT_FOUND_WITH_ID + companyDTO.logo().id()));

                String newSrc = companyDTO.logo().src();
                if (newSrc != null && !newSrc.isBlank() && !java.util.Objects.equals(media.getSrc(), newSrc)) {
                    media.setSrc(newSrc);
                    setMediaTypeFromSrc(media, newSrc);
                }

                if (media.getCompany() == null) {
                    media.setCompany(company);
                }

                mediaRepository.save(media);
                company.setLogo(media);

            } else if (companyDTO.logo().src() != null && !companyDTO.logo().src().isBlank()) {
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().src());
                newLogo.setCompany(company);
                setMediaTypeFromSrc(newLogo, companyDTO.logo().src());

                Media savedLogo = mediaRepository.save(newLogo);
                company.setLogo(savedLogo);

            } else {
                company.setLogo(null);
            }
        } else {
            company.setLogo(null);
        }

        Company updatedCompany = companyRepository.save(company);
        return convertToDTO(updatedCompany);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CompanyDTO syncStripeData(Long companyId) throws BillingException {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException(COMPANY_NOT_FOUND_WITH_ID + companyId));

        Company updatedCompany = stripeBillingService.syncStripeData(company);
        return convertToDTO(updatedCompany);
    }

    @Override
    @Transactional
    public String createCheckoutSession(Long companyId) throws BillingException {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException(COMPANY_NOT_FOUND_WITH_ID + companyId));

        return stripeBillingService.createCheckoutSession(company);
    }

    @Override
    @Transactional
    public String createBillingPortalSession(Long companyId) throws BillingException {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException(COMPANY_NOT_FOUND_WITH_ID + companyId));

        return stripeBillingService.createBillingPortalSession(company);
    }

    // ===================== MAPPING =====================

    private CompanyDTO convertToDTO(Company company) {
        // Inicializa sólo lo necesario para evitar proxies LAZY en la serialización
        Media logo = company.getLogo();
        if (logo != null) {
            Hibernate.initialize(logo);
            MediaType type = logo.getType();
            if (type != null)
                Hibernate.initialize(type);
        }

        // Para no arrastrar colecciones LAZY (y evitar ByteBuddy proxies), devolvemos
        // vacías.
        // Si quieres devolverlas, mejor pásalas a DTOs o usa @EntityGraph y mapea.
        List<Device> devices = List.of();
        List<Advice> advices = List.of();

        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getObservations(),
                toMediaSlimDTO(company.getLogo()),
                company.getLogo() != null ? company.getLogo().getId() : null, // logoId
                devices,
                advices,
                company.getPrimaryColor(),
                company.getSecondaryColor(),
                company.getStripeCustomerId(),
                company.getStripeSubscriptionId(),
                company.getStripeSubscriptionItemId(),
                company.getBillingStatus());
    }

    private MediaSlimDTO toMediaSlimDTO(Media m) {
        if (m == null)
            return null;
        return new MediaSlimDTO(
                m.getId(),
                m.getSrc(),
                toMediaTypeDTO(m.getType()),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }

    private MediaTypeDTO toMediaTypeDTO(MediaType t) {
        if (t == null)
            return null;
        return new MediaTypeDTO(
                t.getId(),
                t.getExtension(), // <-- correcto
                t.getType(), // <-- correcto
                t.getEnabled());
    }

    private Company convertToEntity(CompanyDTO dto) {
        Company c = new Company();
        c.setId(dto.id());
        c.setName(dto.name());
        c.setObservations(dto.observations());
        c.setPrimaryColor(dto.primaryColor());
        c.setSecondaryColor(dto.secondaryColor());

        // Stripe & billing
        c.setStripeCustomerId(dto.stripeCustomerId());
        c.setStripeSubscriptionId(dto.stripeSubscriptionId());
        c.setStripeSubscriptionItemId(dto.stripeSubscriptionItemId());
        if (dto.billingStatus() != null) {
            c.setBillingStatus(dto.billingStatus().toString());
        } else {
            c.setBillingStatus(BillingStatus.INCOMPLETE.name());
        }

        // Logo: si viene id en el DTO slim, enlazamos; si no, se creará en save/update
        if (dto.logo() != null && dto.logo().id() != null) {
            mediaRepository.findById(dto.logo().id()).ifPresent(c::setLogo);
        } else {
            c.setLogo(null);
        }

        // No mapeamos devices/advices desde DTO para evitar inconsistencias (se
        // gestionan por sus endpoints)
        c.setDevices(List.of());
        c.setAdvices(List.of());

        return c;
    }

    // ===================== HELPERS =====================

    private void setMediaTypeFromSrc(Media media, String src) {
        if (src == null)
            return;
        String lower = src.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot != -1 && dot < lower.length() - 1) {
            String ext = lower.substring(dot + 1);
            mediaTypeRepository.findByExtension(ext).ifPresent(media::setType);
        }
    }
}
