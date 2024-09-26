package name.ekt.kafka.connect.converter

import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.sql.DriverManager

class SqlVariantConverterIntegrationTest {
    private val integrationTestEnvironment = IntegrationTestEnvironment(
        sqlServerTableName = "mytable",
        sqlServerTableCreateStmt = """
                                   CREATE TABLE mytable (
                                        id INT PRIMARY KEY,
                                        data sql_variant
                                   )
                                 """,
        sqlServerInitialInserts = """
                                     INSERT INTO mytable (id, data) VALUES (2, CAST('Test string' AS sql_variant)); 
                                  """,
        enableKafkaUI = true
    )

    @Test
    fun `test data transfer from SQL Server to Kafka`() {
        integrationTestEnvironment.use { ite ->
            ite.start()

            sleep(10000)

            DriverManager.getConnection(
                ite.postgresSQLContainer.jdbcUrl,
                ite.postgresSQLContainer.username,
                ite.postgresSQLContainer.password
            ).use { conn ->
                conn.createStatement().use { statement ->
                    val resultSet = statement.executeQuery("SELECT * FROM usrkafka.mytable")

                    assert(resultSet.next())
                    val id = resultSet.getInt("id")
                    val data = resultSet.getString("data")

                    assert(id == 2)
                    assert(data == "Test string")
                }
            }
        }
    }
}
