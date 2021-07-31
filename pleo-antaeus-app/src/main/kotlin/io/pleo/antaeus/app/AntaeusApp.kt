/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.rest.AntaeusRest
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.InMem
import it.justwrote.kjob.Job
import it.justwrote.kjob.job.JobExecutionType
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
import java.sql.Connection

private val kotlinLogger = KotlinLogging.logger {}

// Cron job expressions:
// - every 1st of the month at 3am: 0 0 3 ? 1 * *
// - every minute (testing): 0 */10 * ? * * *
object FirstOfMonth : KronJob("charge-invoices", "0 */1 * ? * * *")

object ProcessInvoiceJob : Job("process-invoice") {
    val id = integer("id")
}

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

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()

    // kjob
    val kjob = kjob(InMem) {
        nonBlockingMaxJobs = 3
        blockingMaxJobs = 3
        extension(KronModule)
    }.start()

    kjob.register(ProcessInvoiceJob) {
        executionType = JobExecutionType.NON_BLOCKING
        execute {
            billingService.processInvoice(props[it.id])
        }
    }

    kjob(Kron).kron(FirstOfMonth) {
        maxRetries = 3
        execute {
            kotlinLogger.info { "Scheduled task, charge pending invoices" }
            invoiceService.fetchAll(InvoiceStatus.PENDING).forEach {
                val id = it.id
                kjob.schedule(ProcessInvoiceJob) {
                    props[it.id] = id
                }
            }
        }
    }
}
