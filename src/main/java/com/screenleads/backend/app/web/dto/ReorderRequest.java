package com.screenleads.backend.app.web.dto;

import java.util.List;

public record ReorderRequest(List<Long> ids) {
}
