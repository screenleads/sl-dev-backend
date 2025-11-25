package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.CompanyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyTokenRepository extends JpaRepository<CompanyToken, Long> {
    List<CompanyToken> findByCompanyId(Long companyId);
    void deleteByToken(String token);
    CompanyToken findByToken(String token);
}
