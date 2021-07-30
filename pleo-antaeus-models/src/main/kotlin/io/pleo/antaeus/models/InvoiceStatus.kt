package io.pleo.antaeus.models

enum class InvoiceStatus {
    FAILED_CURRENCY_MISMATCH,
    FAILED_CUSTOMER_NOT_FOUND,
    FAILED_NETWORK,
    FAILED_PAYMENT_METHOD,
    FAILED_UNKNOWN,
    PAID,
    PENDING
}
