package com.screenleads.backend.app.domain.model;

public enum CouponStatus {
    NEW,        // generado pero aún no validado
    VALID,      // validado / listo para canjear
    REDEEMED,   // ya canjeado
    EXPIRED,    // caducado por fecha o manualmente
    CANCELLED   // invalidado manualmente / antifraude
}
