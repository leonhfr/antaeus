package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
//    TODO: check var here, immutable is better?
    var status: InvoiceStatus
)
