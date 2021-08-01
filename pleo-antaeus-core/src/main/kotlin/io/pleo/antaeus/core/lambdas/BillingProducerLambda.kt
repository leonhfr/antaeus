package io.pleo.antaeus.core.lambdas

import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingProducerLambda (
    private val invoiceService: InvoiceService
) {
    fun handler(): List<Invoice> {
        return invoiceService.fetchAll(InvoiceStatus.PENDING)
    }
}