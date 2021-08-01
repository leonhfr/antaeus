package io.pleo.antaeus.core.lambdas

import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.streamthoughts.kafka.clients.kafka
import io.streamthoughts.kafka.clients.producer.Acks
import io.streamthoughts.kafka.clients.producer.ProducerContainer
import mu.KotlinLogging
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.StringSerializer

class BillingProducerLambda(
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    private val producer: ProducerContainer<String, Int> = kafka("localhost:29092") {
        client {
            clientId("billing-producer-lambda")
        }

        producer {
            configure {
                Acks.InSyncReplicas
            }
            keySerializer(StringSerializer())
            valueSerializer(IntegerSerializer())
            defaultTopic("billing-jobs")

            onSendError { _, record, error -> logger.error { "Record ${record.value()} failed to send: $error" } }

            onSendSuccess { _, record, _ -> logger.info { "Record ${record.value()} sent successfully" } }
        }
    }

    init {
        producer.init()
    }

    fun handler(): List<Invoice> {
        val invoices = invoiceService.fetchAll(InvoiceStatus.PENDING)
        producer.use {
            invoices.forEach { producer.send(value = it.id) }
        }
        return invoices
    }
}