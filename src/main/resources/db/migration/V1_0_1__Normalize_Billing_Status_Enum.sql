-- V1_0_1__Normalize_Billing_Status_Enum.sql
-- Normaliza el enum BillingStatus: CANCELED -> CANCELLED
-- Fecha: 2026-02-11
-- Descripción: 
--   - Cambia valores 'CANCELED' a 'CANCELLED' para consistencia con el enum standalone
--   - Actualiza la restricción CHECK si existe

-- Actualizar valores existentes de CANCELED a CANCELLED
UPDATE company 
SET billing_status = 'CANCELLED' 
WHERE billing_status = 'CANCELED';

-- Comentario informativo sobre los valores válidos del enum
COMMENT ON COLUMN company.billing_status IS 'Estados válidos: INCOMPLETE, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED';
