package name.ekt.kafka.connect.converter

import io.debezium.spi.converter.CustomConverter
import io.debezium.spi.converter.RelationalColumn
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.connect.data.Schema.STRING_SCHEMA
import org.apache.kafka.connect.data.SchemaBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties

class SqlVariantConverterTest {
    private lateinit var converter: SqlVariantConverter
    private lateinit var registration: TestConverterRegistration

    @BeforeEach
    fun setUp() {
        converter = SqlVariantConverter()
        registration = TestConverterRegistration()
    }

    @Test
    fun `converterFor should register converter when field matches`() {
        val column = mockk<RelationalColumn> {
            every { name() } returns "testField"
            every { typeName() } returns "sql_variant"
        }

        val properties = Properties().apply {
            setProperty("field", "testField")
        }
        converter.configure(properties)

        converter.converterFor(column, registration)
        val result = registration.converterFunction?.convert(123)

        assertNotNull(registration.schema)
        assertEquals(STRING_SCHEMA, registration.schema?.schema())
        assertEquals("123", result)
    }

    @Test
    fun `converterFor should not register converter when field does not match`() {
        val column = mockk<RelationalColumn> {
            every { name() } returns "otherField"
            every { typeName() } returns "sql_variant"
        }

        val properties = Properties().apply {
            setProperty("field", "testField")
        }
        converter.configure(properties)

        converter.converterFor(column, registration)

        assertEquals(null, registration.schema)
    }

    @Test
    fun `converterFor should not register converter when field is empty`() {
        val column = mockk<RelationalColumn> {
            every { name() } returns "anyField"
            every { typeName() } returns "sql_variant"
        }

        val properties = Properties()
        converter.configure(properties)

        converter.converterFor(column, registration)

        assertEquals(null, registration.schema)
    }

    @Test
    fun `converterFor should not register converter when field is not sql_variant`() {
        val column = mockk<RelationalColumn> {
            every { name() } returns "testField"
            every { typeName() } returns "other_type"
        }

        val properties = Properties().apply {
            setProperty("field", "testField")
        }
        converter.configure(properties)

        converter.converterFor(column, registration)

        assertEquals(null, registration.schema)
    }

    private class TestConverterRegistration : CustomConverter.ConverterRegistration<SchemaBuilder?> {
        var schema: SchemaBuilder? = null
        var converterFunction: CustomConverter.Converter? = null

        override fun register(fieldSchema: SchemaBuilder?, converter: CustomConverter.Converter?) {
            this.schema = fieldSchema
            this.converterFunction = converter
        }
    }

}
