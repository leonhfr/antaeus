package io.pleo.antaeus.core.lambdas

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

const val CONSUMER_TAG = "billing-consumer"

class BillingConsumerLambda(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun handler(id: Int): Invoice {
//        TODO: Exponential backoff on FAILED_NETWORK and FAILED_UNKNOWN statuses
        val invoice = invoiceService.fetch(id)
        return chargeInvoice(invoice).also { invoiceService.update(invoice.id, it.status) }
    }

    fun chargeInvoice(invoice: Invoice): Invoice {
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
            return invoice.copy(status = status)
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