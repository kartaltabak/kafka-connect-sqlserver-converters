package name.ekt.kafka.connect.converter

import io.debezium.spi.converter.CustomConverter
import io.debezium.spi.converter.RelationalColumn
import org.apache.kafka.connect.data.SchemaBuilder
import java.util.Properties


class SqlVariantConverter : CustomConverter<SchemaBuilder?, RelationalColumn> {
    companion object {
        private const val SQL_VARIANT_NAME = "sql_variant"
        private const val FIELD_PROPERTY = "field";
    }

    private lateinit var field: String
    override fun configure(props: Properties) {
        field = props.getProperty(FIELD_PROPERTY) ?: ""
    }

    override fun converterFor(
        column: RelationalColumn,
        registration: CustomConverter.ConverterRegistration<SchemaBuilder?>
    ) =
        if (field == column.name()) {
            coverterFor2(column, registration)
        } else {
            Unit
        }

    private fun coverterFor2(
        column: RelationalColumn,
        registration: CustomConverter.ConverterRegistration<SchemaBuilder?>
    ) {
        if (SQL_VARIANT_NAME.equals(column.typeName(), ignoreCase = true)) {
            registration.register(SchemaBuilder.string(), ::convert)
        } else {
            Unit
        }
    }

    private fun convert(input: Any?): String? = input?.toString()
}