package com.screenleads.backend.app.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;


@MappedSuperclass
@Getter
@Setter
public abstract class Auditable {
@CreationTimestamp
@Column(name = "created_at", updatable = false, nullable = false)
private Instant createdAt;


@UpdateTimestamp
@Column(name = "updated_at")
private Instant updatedAt;
}