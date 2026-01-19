package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.util.List;

import com.screenleads.backend.app.domain.model.UserActionType;
import com.screenleads.backend.app.web.dto.UserActionDTO;

public interface UserActionService {

    List<UserActionDTO> getAllActions();

    List<UserActionDTO> getActionsByCustomer(Long customerId);

    List<UserActionDTO> getActionsByDevice(Long deviceId);

    List<UserActionDTO> getActionsByType(UserActionType actionType);

    List<UserActionDTO> getActionsByCustomerAndDateRange(Long customerId, Instant startDate, Instant endDate);

    UserActionDTO createAction(UserActionDTO dto);

    void trackAction(Long customerId, Long deviceId, UserActionType actionType, String entityType, Long entityId,
            String details);
}
