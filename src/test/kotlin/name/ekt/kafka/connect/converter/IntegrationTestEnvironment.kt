package name.ekt.kafka.connect.converter

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.lifecycle.Startable
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Files.isRegularFile
import java.nio.file.Paths
import java.sql.DriverManager
import java.time.Duration

private const val SQLSERVER_CONNECTOR_PLUGIN_VERSION = "2.7.3.Final"

private const val JDBC_CONNECTOR_PLUGIN_VERSION = "10.7.12"

class IntegrationTestEnvironment(
    private val enableKafkaUI: Boolean = false,
    private val sqlServerTableName: String = "mytable",
    private val sqlServerTableCreateStmt: String,
    private val sqlServerInitialInserts: String
) : Startable {
    private var network: Network? = null

    private val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))

    private val sqlServerContainer =
        MSSQLServerContainer<Nothing>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest"))
            .apply {
                acceptLicense()
                withPassword("Password1")
            }

    val postgresSQLContainer = PostgreSQLContainer<Nothing>("postgres:13")
        .apply { withExposedPorts(POSTGRESQL_PORT) }

    private val schemaRegistryContainer =
        GenericContainer<Nothing>(DockerImageName.parse("confluentinc/cp-schema-registry"))
            .apply {
                withExposedPorts(8081)  // Schema Registry typically runs on port 8081
                withEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost")
                withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            }

    private val kafkaConnectContainer: GenericContainer<Nothing> =
        GenericContainer<Nothing>(
            ImageFromDockerfile()
                .withFileFromPath(
                    "kc-ssc.jar",
                    Files.list(Paths.get("build/libs"))
                        .filter { path -> isRegularFile(path) && path.toString().endsWith(".jar") }
                        .findFirst()
                        .orElseThrow { IllegalStateException("No jar file found") }
                )
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("confluentinc/cp-kafka-connect")
                        .run("confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:$JDBC_CONNECTOR_PLUGIN_VERSION")
                        .run(
                            "wget " +
                                    "https://repo1.maven.org/maven2/io/debezium/debezium-connector-sqlserver/" +
                                    "$SQLSERVER_CONNECTOR_PLUGIN_VERSION/" +
                                    "debezium-connector-sqlserver-$SQLSERVER_CONNECTOR_PLUGIN_VERSION-plugin.tar.gz " +
                                    "-O /tmp/debezium-connector-sqlserver.tar.gz && " +
                                    "tar -xzf /tmp/debezium-connector-sqlserver.tar.gz " +
                                    "-C /usr/share/java/"
                        )
                        .copy(
                            "kc-ssc.jar",
                            "/usr/share/java/debezium-connector-sqlserver/"
                        )
                        .build()
                }
        )
            .apply {
                withEnv("CONNECT_REST_PORT", "8083")
                withEnv("CONNECT_GROUP_ID", "kafka-connect-group")
                withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "connect-configs")
                withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "connect-offsets")
                withEnv("CONNECT_STATUS_STORAGE_TOPIC", "connect-statuses")
                withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
                withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "localhost")
                withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
                withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
                withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
                withExposedPorts(8083)
            }

    private val kafkaUIContainer: GenericContainer<Nothing> =
        GenericContainer<Nothing>(DockerImageName.parse("provectuslabs/kafka-ui"))
            .apply {
                withEnv("KAFKA_CLUSTERS_0_NAME", "default")
                withEnv("KAFKA_CLUSTERS_0_KAFKA_CONNECT_0_NAME", "connect-cluster-1")
                withEnv("DYNAMIC_CONFIG_ENABLED", "true")
                withExposedPorts(8080)
            }


    private fun createDataOnSqlServer(sqlServerContainer: MSSQLServerContainer<Nothing>) {
        DriverManager.getConnection(
            sqlServerContainer.jdbcUrl, sqlServerContainer.username, sqlServerContainer.password
        ).use { conn ->
            conn.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE DATABASE EKT;
                    """.trimIndent()
                )
            }
            conn.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    USE EKT;
                    EXEC sys.sp_cdc_enable_db;

                    ${sqlServerTableCreateStmt};

                    EXEC sys.sp_cdc_enable_table
                        @source_schema = 'dbo',
                        @source_name   = '${sqlServerTableName}',
                        @role_name     = NULL;

                    ${sqlServerInitialInserts}
                    COMMIT;
                    """.trimIndent()
                )
            }
        }
    }

    private fun registerSourceConnector(kafkaConnectContainer: GenericContainer<Nothing>) {
        val sqlServerHost = sqlServerContainer.containerInfo.config.hostName

        val sourceConnectorConfig = """
            {
                "name": "sqlserver-source-connector",
                "config": {
                    "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector",
                    "tasks.max": "1",
                    "database.hostname": "${sqlServerHost}",
                    "database.port": "1433",
                    "database.user": "${sqlServerContainer.username}",
                    "database.password": "${sqlServerContainer.password}",
                    "database.trustServerCertificate": "true",
                    "database.names": "EKT",

                    "topic.prefix": "sqlserver",
                    "topic.creation.default.partitions": "12",
                    "topic.creation.default.cleanup.policy": "delete",
                    "topic.creation.default.replication.factor": "1",

                    "transforms": "unwrap",
                    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",

                    "time.precision.mode": "connect",
                    "table.include.list": "dbo.${sqlServerTableName}",
                    
                    "snapshot.mode": "initial",
                    "snapshot.isolation.mode": "read_committed",

                    "schema.history.internal.kafka.bootstrap.servers": "PLAINTEXT://${kafkaContainer.containerInfo.config.hostName}:9092",
                    "schema.history.internal.kafka.topic": "schema-changes.${sqlServerTableName}", 
                    
                    "converters": "sql_variant_converter",
                    "sql_variant_converter.type": "name.ekt.kafka.connect.converter.SqlVariantConverter",
                    "sql_variant_converter.field": "data"
                }
            }
        """.trimIndent()

        registerConnector(kafkaConnectContainer, sourceConnectorConfig)
    }

    private fun registerConnector(
        kafkaConnectContainer: GenericContainer<Nothing>,
        connectorConfig: String
    ) {
        val kafkaConnectRestUrl =
            "http://${kafkaConnectContainer.host}:${kafkaConnectContainer.getMappedPort(8083)}/connectors"

        val client = OkHttpClient()
        val requestBody = connectorConfig.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder().url(kafkaConnectRestUrl).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to register connector: ${response.code}, ${response.body?.string()}")
            }
            println("Connector registered successfully")
        }
    }

    private fun registerSinkConnector(kafkaConnectContainer: GenericContainer<Nothing>) {
        val sinkConnectorConfig =
            """
                {
                    "name": "postgres-sink-connector",
                    "config": {
                        "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
        
                        "tasks.max": "1",
        
                        "connection.url": "jdbc:postgresql://${postgresSQLContainer.containerInfo.config.hostName}:$POSTGRESQL_PORT/${postgresSQLContainer.databaseName}",
                        "connection.user": "${postgresSQLContainer.username}",
                        "connection.password": "${postgresSQLContainer.password}",
                        "topics": "sqlserver.EKT.dbo.${sqlServerTableName}",
        
                        "insert.mode": "upsert",
                        "pk.mode": "record_key",
                        "pk.fields": "id",
                        
                        "auto.create": "true",
                        "delete.enabled": "true",
                        
                        "transforms": "extractTable",
                        "transforms.extractTable.type": "org.apache.kafka.connect.transforms.RegexRouter",
                        "transforms.extractTable.regex": ".*\\.dbo\\.(.*)",
                        "transforms.extractTable.replacement": "usrkafka.$1"
                    }
                }
            """.trimIndent()

        registerConnector(kafkaConnectContainer, sinkConnectorConfig)
    }


    override fun start() {
        startNetwork()
        startKafka()
        startSchemaRegistry()
        startSourceDatabase()
        startSinkDatabase()
        startKafkaConnect()
        startKafkaUi()
    }

    private fun startSchemaRegistry() {
        schemaRegistryContainer
            .apply {
                withEnv(
                    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                    "PLAINTEXT://${kafkaContainer.containerInfo.config.hostName}:9092"
                )
            }
            .also { it.withNetwork(network) }
            .also { it.start() }
    }

    private fun startKafkaUi() {
        if (enableKafkaUI) {
            kafkaUIContainer
                .apply {
                    withEnv(
                        "KAFKA_CLUSTERS_0_BOOTSTRAP_SERVERS", "${kafkaContainer.containerInfo.config.hostName}:9092"
                    )
                    withEnv(
                        "KAFKA_CLUSTERS_0_KAFKA_CONNECT_0_ADDRESS",
                        "http://${kafkaConnectContainer.containerInfo.config.hostName}:8083"
                    )
                }
                .also { it.withNetwork(network) }
                .also { it.start() }
                .also { println("Kafka UI available at: http://${it.host}:${it.getMappedPort(8080)}") }
        }
    }

    private fun startKafkaConnect() {
        kafkaConnectContainer
            .also {
                it.withEnv(
                    "CONNECT_BOOTSTRAP_SERVERS", "PLAINTEXT://${kafkaContainer.containerInfo.config.hostName}:9092"
                )
                it.withEnv("CONNECT_KEY_CONVERTER", "io.confluent.connect.avro.AvroConverter")
                it.withEnv(
                    "CONNECT_KEY_CONVERTER_SCHEMA_REGISTRY_URL",
                    "http://${schemaRegistryContainer.containerInfo.config.hostName}:8081"
                )
                it.withEnv("CONNECT_VALUE_CONVERTER", "io.confluent.connect.avro.AvroConverter")
                it.withEnv(
                    "CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL",
                    "http://${schemaRegistryContainer.containerInfo.config.hostName}:8081"
                )
            }
            .also { it.withNetwork(network) }
            .apply {
                waitingFor(
                    HttpWaitStrategy()
                        .forPath("/connectors")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(1))
                )
            }
            .also { it.start() }
            .also { registerSourceConnector(it) }
            .also { registerSinkConnector(it) }
    }

    private fun startSinkDatabase() {
        postgresSQLContainer
            .also { it.withNetwork(network) }
            .also { it.start() }
            .also { preparePostgresDatabase(it) }
    }

    private fun preparePostgresDatabase(postgresSQLContainer: PostgreSQLContainer<Nothing>) {
        DriverManager.getConnection(
            postgresSQLContainer.jdbcUrl, postgresSQLContainer.username, postgresSQLContainer.password
        ).use { conn ->
            conn.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE SCHEMA usrkafka;
                    """.trimIndent()
                )
            }
        }
    }

    private fun startSourceDatabase() {
        sqlServerContainer
            .also { it.withNetwork(network) }
            .also { it.start() }
            .also { createDataOnSqlServer(it) }
    }

    private fun startKafka() {
        kafkaContainer
            .also { it.withNetwork(network) }
            .start()
    }

    private fun startNetwork() {
        network = Network.newNetwork()
    }

    override fun stop() {
        kafkaUIContainer.stop()
        kafkaConnectContainer.stop()
        postgresSQLContainer.stop()
        sqlServerContainer.stop()
        kafkaContainer.stop()
        network?.close()
    }
}
