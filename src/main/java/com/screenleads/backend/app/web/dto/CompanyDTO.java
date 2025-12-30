package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Device;

import com.screenleads.backend.app.domain.model.Company;

public record CompanyDTO(
        Long id,
        String name,
        String observations,
        MediaSlimDTO logo,
        Long logoId, // ID del logo para crear/actualizar
        List<Device> devices,
        List<Advice> advices,
        String primaryColor,
        String secondaryColor,
        String stripeCustomerId,
        String stripeSubscriptionId,
        String stripeSubscriptionItemId,
        Company.BillingStatus billingStatus) {
}
