package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val invoice1 = Invoice(
        1,
        1,
        Money(BigDecimal(100), Currency.EUR),
        InvoiceStatus.PENDING
    )

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoices(InvoiceStatus.PENDING) } returns listOf(
            invoice1
        )
        every { updateInvoice(404, InvoiceStatus.PAID) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `fetch will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `update will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.update(404, InvoiceStatus.PAID)
        }
    }

    @Test
    fun `should return expected value`() {
        val result = invoiceService.fetchAll(InvoiceStatus.PENDING)
        assert(result == listOf(invoice1))
    }
}
