package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findByCompany_IdAndIsActiveTrue(Long companyId);

    List<FraudRule> findByCompany_IdAndRuleTypeAndIsActiveTrue(Long companyId, FraudRuleType ruleType);

    List<FraudRule> findByIsActiveTrue();

    List<FraudRule> findByCompany_Id(Long companyId);

    long countByCompany_Id(Long companyId);
}
