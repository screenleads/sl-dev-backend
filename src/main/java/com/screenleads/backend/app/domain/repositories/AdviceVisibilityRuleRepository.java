package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;

public interface AdviceVisibilityRuleRepository extends JpaRepository<AdviceVisibilityRule, Long> {
}