package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    private fun getInvoice(id: Int, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice {
        return Invoice(
            id = id,
            customerId = 1,
            amount = Money(BigDecimal(100), Currency.EUR),
            status = status
        )
    }

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(getInvoice(1)) } throws Exception()
        every { charge(getInvoice(1)) } throws CurrencyMismatchException(invoiceId = 0, customerId = 1)
        every { charge(getInvoice(2)) } throws CustomerNotFoundException(id = 1)
        every { charge(getInvoice(3)) } throws NetworkException()
        every { charge(getInvoice(4)) } returns false
        every { charge(getInvoice(5)) } returns true
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(1) } returns getInvoice(1)
        every { fetch(5) } returns getInvoice(5)
        every { update(1, InvoiceStatus.FAILED_NETWORK) } returns getInvoice(1, InvoiceStatus.FAILED_NETWORK)
        every { update(5, InvoiceStatus.PAID) } returns getInvoice(5)
    }

    private val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    @Test
    fun `processInvoice should update the invoice`() {
        billingService.processInvoice(5)
        verify(exactly = 1) { paymentProvider.charge(getInvoice(5)) }
        verify(exactly = 1) { invoiceService.update(5, InvoiceStatus.PAID) }
    }

    @Test
    fun `processInvoice should throw on select errors`() {
        assertThrows<Exception> {
            billingService.processInvoice(1)
        }
    }

    @Test
    fun `chargeInvoice should handle an unknown exception`() {
        val result = billingService.chargeInvoice(getInvoice(0))
        assertEquals(result.status, InvoiceStatus.FAILED_UNKNOWN)
    }

    @Test
    fun `chargeInvoice should handle a currency mismatch exception`() {
        val result = billingService.chargeInvoice(getInvoice(1))
        assertEquals(result.status, InvoiceStatus.FAILED_CURRENCY_MISMATCH)
    }

    @Test
    fun `chargeInvoice should handle a customer not found exception`() {
        val result = billingService.chargeInvoice(getInvoice(2))
        assertEquals(result.status, InvoiceStatus.FAILED_CUSTOMER_NOT_FOUND)
    }

    @Test
    fun `chargeInvoice should handle a network exception`() {
        val result = billingService.chargeInvoice(getInvoice(3))
        assertEquals(result.status, InvoiceStatus.FAILED_NETWORK)
    }

    @Test
    fun `chargeInvoice should handle a failed charge`() {
        val result = billingService.chargeInvoice(getInvoice(4))
        assertEquals(result.status, InvoiceStatus.FAILED_PAYMENT_METHOD)
    }

    @Test
    fun `chargeInvoice should handle a successful charge`() {
        val result = billingService.chargeInvoice(getInvoice(5))
        assertEquals(result.status, InvoiceStatus.PAID)
    }
}