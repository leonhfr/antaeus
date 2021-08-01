package io.pleo.antaeus.core.lambdas

import com.rabbitmq.client.Channel
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

const val QUEUE_NAME = "billing-jobs"

class BillingProducerLambda(
    private val invoiceService: InvoiceService,
    private val channel: Channel
) {
    private val logger = KotlinLogging.logger {}

    fun handler() {
        invoiceService.fetchAll(InvoiceStatus.PENDING)
            .forEach {
                channel.basicPublish("", QUEUE_NAME, null, it.id.toString().toByteArray())
                logger.info { "Published invoice ${it.id} to queue" }
            }
    }
}