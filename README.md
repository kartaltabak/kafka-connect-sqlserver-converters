# Kafka Connect SQL Server Variant Converters

[![codecov](https://codecov.io/gh/kartaltabak/kafka-connect-sqlserver-converters/graph/badge.svg?token=1KBH26O0DT)](https://codecov.io/gh/kartaltabak/kafka-connect-sqlserver-converters)

## Overview
The `Kafka Connect SQLServer Converter` library provides custom converters for Kafka Connect, specifically designed to handle SQL `sql_variant` types. This project aims to extend the capabilities of Kafka Connect by offering additional converters, ensuring seamless integration and data transformation from databases to Kafka topics.

## Features
- **SQL Variant Conversion**: Convert SQL `sql_variant` types to string representation.
- **Configurable**: Easily configure the field to be converted using properties.

## Installation
To use this library, follow these steps:

1. **Download the Converter JAR**:
    - Go to the [Releases](https://github.com/your-repo/releases) section of the repository.
    - Download the latest JAR file.

2. **Deploy the JAR**:
    - Copy the downloaded JAR file to the `plugins` directory of your Kafka Connect installation.

## Converters

### SqlVariantConverter

The `SqlVariantConverter` is a custom converter designed for use with Debezium connectors in Kafka Connect. It handles SQL Server's `sql_variant` data type by converting it into a string format. This converter registers a string schema for any column that matches the specified field name and has a `sql_variant` type.

#### Configuration

- `field` (default: `""`): The name of the column that should be converted if it has the `sql_variant` type. 
This property must be set through the converter's configuration to match the column name in the database.

#### Example Configuration

To configure the `SqlVariantConverter` in your Debezium connector, 
you need to add the converter to the connector's configuration. Here's how you might configure it in a JSON format for a Debezium SQL Server connector:

```json
{
  "name": "your-connector-name",
  "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector",
  "tasks.max": "1",
  "database.hostname": "your-sqlserver-hostname",
  "database.port": "1433",
  "database.user": "your-username",
  "database.password": "your-password",
  "database.dbname": "your-database-name",
  "database.server.name": "your-server-name",
  "table.include.list": "your_table_name",

  "converters": "sqlVariantConverter",
  "sqlVariantConverter.type": "name.ekt.kafka.connect.converter.SqlVariantConverter",
  "sqlVariantConverter.field": "your_column_name",

  "transforms": "unwrap",
  "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState"
}
```

In this example:
- **`converters`**: Defines the converter to be used. 
Here, `sqlVariantConverter` is the name given to the custom converter.
- **`sqlVariantConverter.type`**: Specifies the fully-qualified class name of the `SqlVariantConverter`.
- **`sqlVariantConverter.field`**: Specifies the column name that should be converted if it matches the `sql_variant` type.

### Usage Details

The `SqlVariantConverter` is intended to be used in scenarios where you are working with SQL Server databases that include `sql_variant` columns. The converter ensures that these columns are correctly converted to a string format within the Kafka Connect data pipeline, allowing for consistent and reliable data handling in downstream systems.
## Contributing
We welcome contributions from the community. 
If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request.

## License
This project is licensed under the MIT License. See the LICENSE file for details.

Thank you for using Kafka Connect SQL Variant Converter!






