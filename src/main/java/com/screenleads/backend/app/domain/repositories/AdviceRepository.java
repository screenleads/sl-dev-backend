package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Advice;

import java.util.List;

public interface AdviceRepository extends JpaRepository<Advice, Long> {
    List<Advice> findByCompany_Id(Long companyId);
}