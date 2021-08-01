plugins {
    application
    kotlin("jvm")
}

kotlinProject()

dataLibs()

application {
    mainClassName = "io.pleo.antaeus.app.AntaeusApp"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation(project(":pleo-antaeus-rest"))
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")

    // kjob
    implementation("it.justwrote:kjob-core:0.2.0")
    implementation("it.justwrote:kjob-kron:0.2.0")
    implementation("it.justwrote:kjob-inmem:0.2.0" )

    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.9.0")
}