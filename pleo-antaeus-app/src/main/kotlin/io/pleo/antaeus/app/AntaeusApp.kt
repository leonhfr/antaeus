/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import QUEUE_NAME
import buildChannel
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import getPaymentProvider
import io.pleo.antaeus.core.lambdas.BillingConsumerLambda
import io.pleo.antaeus.core.lambdas.BillingProducerLambda
import io.pleo.antaeus.core.lambdas.CONSUMER_TAG
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.InMem
import it.justwrote.kjob.kron.Kron
import it.justwrote.kjob.kron.KronModule
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.nio.charset.StandardCharsets
import java.sql.Connection

private val kotlinLogger = KotlinLogging.logger {}

// Cron job expressions:
// - every 1st of the month at 3am: 0 0 3 ? 1 * *
// - every minute (testing): 0 */10 * ? * * *
object FirstOfMonth : KronJob("charge-invoices", "0 */1 * ? * * *")

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = ""
        )
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // RabbitMQ channel
    val channel = buildChannel()

    // Lambdas (theoretically!)
    val billingConsumerLambda = BillingConsumerLambda(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService
    )
    val billingProducerLambda = BillingProducerLambda(
        invoiceService = invoiceService,
        channel = channel
    )

    // RabbitMQ set consumer
    val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
        val id = String(delivery.body, StandardCharsets.UTF_8).toInt()
        kotlinLogger.info { "[$consumerTag] Received invoice $id" }
        billingConsumerLambda.handler(id)
    }
    val cancelCallback = CancelCallback { consumerTag: String? ->
        kotlinLogger.info { "[$consumerTag] cancelled" }
    }

    channel.basicConsume(QUEUE_NAME, true, CONSUMER_TAG, deliverCallback, cancelCallback)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        billingProducerLambda = billingProducerLambda
    ).run()

    // kjob
    val kjob = kjob(InMem) {
        extension(KronModule)
    }.start()

    // TODO: cron job should be scheduled
    kjob(Kron).kron(FirstOfMonth) {
        maxRetries = 3
        execute {
            kotlinLogger.info { "Scheduled task, charge pending invoices" }
            billingProducerLambda.handler()
        }
    }
}
