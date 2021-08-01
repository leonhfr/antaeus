plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))

    // Kafka
    implementation("io.streamthoughts:kafka-clients-kotlin:0.2.0")
    implementation("org.apache.kafka:kafka-clients:2.0.0")
}