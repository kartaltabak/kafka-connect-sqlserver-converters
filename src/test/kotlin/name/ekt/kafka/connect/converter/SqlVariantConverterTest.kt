package name.ekt.kafka.connect.converter

import io.debezium.spi.converter.CustomConverter
import io.debezium.spi.converter.RelationalColumn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.connect.data.SchemaBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Properties

class SqlVariantConverterTest {
    private class TestConverterRegistration : CustomConverter.ConverterRegistration<SchemaBuilder?> {
        var converterFunction: CustomConverter.Converter? = null

        override fun register(fieldSchema: SchemaBuilder?, converter: CustomConverter.Converter?) {
            this.converterFunction = converter
        }
    }

    @Test
    fun `converterFor should register converter when field and type match`() {
        val converter = SqlVariantConverter()
        val column = mockk<RelationalColumn>()
        val registration = TestConverterRegistration()

        every { column.name() } returns "testField"
        every { column.typeName() } returns "sql_variant"

        converter.configure(Properties().apply {
            setProperty("field", "testField")
        })

        converter.converterFor(column, registration)
        val result = registration.converterFunction?.convert(Integer.valueOf(12))

        assertEquals("12", result)
    }

    @Test
    fun `converterFor should not register converter when field does not match`() {
        val converter = SqlVariantConverter()
        val column = mockk<RelationalColumn>()
        val registration = mockk<CustomConverter.ConverterRegistration<SchemaBuilder?>>()

        every { column.name() } returns "otherField"
        every { column.typeName() } returns "sql_variant"

        converter.configure(Properties().apply {
            setProperty("field", "testField")
        })

        converter.converterFor(column, registration)

        verify(exactly = 0) { registration.register(any(), any()) }
    }

    @Test
    fun `converterFor should not register converter when type does not match`() {
        val converter = SqlVariantConverter()
        val column = mockk<RelationalColumn>()
        val registration = mockk<CustomConverter.ConverterRegistration<SchemaBuilder?>>()

        every { column.name() } returns "testField"
        every { column.typeName() } returns "other_type"

        converter.configure(Properties().apply {
            setProperty("field", "testField")
        })

        converter.converterFor(column, registration)

        verify(exactly = 0) { registration.register(any(), any()) }
    }

}
