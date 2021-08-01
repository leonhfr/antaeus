plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))

    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.9.0")
}