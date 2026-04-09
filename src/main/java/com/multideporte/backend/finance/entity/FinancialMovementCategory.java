package com.multideporte.backend.finance.entity;

public enum FinancialMovementCategory {
    INSCRIPCION_EQUIPO(FinancialMovementType.INCOME),
    APORTE_SIMPLE(FinancialMovementType.INCOME),
    PATROCINIO_SIMPLE(FinancialMovementType.INCOME),
    OTRO_INGRESO_OPERATIVO(FinancialMovementType.INCOME),
    ARBITRAJE(FinancialMovementType.EXPENSE),
    CANCHA(FinancialMovementType.EXPENSE),
    LOGISTICA(FinancialMovementType.EXPENSE),
    PREMIOS(FinancialMovementType.EXPENSE),
    OTRO_GASTO_OPERATIVO(FinancialMovementType.EXPENSE);

    private final FinancialMovementType movementType;

    FinancialMovementCategory(FinancialMovementType movementType) {
        this.movementType = movementType;
    }

    public FinancialMovementType movementType() {
        return movementType;
    }
}
