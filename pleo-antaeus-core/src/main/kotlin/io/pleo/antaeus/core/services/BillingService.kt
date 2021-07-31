package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun processInvoice(invoice: Invoice) {
//        val logTag = "[processInvoice]"
        val result = this.chargeInvoice(invoice)
//        TODO: Exponential backoff
//        TODO: Queue system
        invoiceService.update(invoice.id, result.status)
    }

    private fun chargeInvoice(invoice: Invoice): Invoice {
        val logTag = "[chargeInvoice]"
        try {
            val paid = paymentProvider.charge(invoice)
            val status = if (paid) {
                logger.info("$logTag Charge successful (invoice ${invoice.id})")
                InvoiceStatus.PAID
            } else {
                logger.info("$logTag Charge failed (invoice ${invoice.id})")
                InvoiceStatus.FAILED_PAYMENT_METHOD
            }
            return invoice.copy(status=status)
        } catch (e: CurrencyMismatchException) {
            logger.info("$logTag Currency mismatch exception (invoice ${invoice.id})")
            return invoice.copy(status = InvoiceStatus.FAILED_CURRENCY_MISMATCH)
        } catch (e: CustomerNotFoundException) {
            logger.info("$logTag Customer not found exception (invoice ${invoice.id})")
            return invoice.copy(status = InvoiceStatus.FAILED_CUSTOMER_NOT_FOUND)
        } catch (e: NetworkException) {
            logger.info("$logTag Network exception (invoice ${invoice.id})")
            return invoice.copy(status = InvoiceStatus.FAILED_NETWORK)
        } catch (e: Exception) {
            logger.info("$logTag Unknown exception (invoice ${invoice.id})", e)
            return invoice.copy(status = InvoiceStatus.FAILED_UNKNOWN)
        }
    }
}
