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
    - Go to the [Releases](https://github.com/kartaltabak/kafka-connect-sqlserver-converters/releases) section of the repository.
    - Download the latest JAR file.

2. **Deploying the Converter JAR**:
   - To integrate this custom converter with a Kafka Connect connector, place the downloaded JAR file in the specific subdirectory of the connector's plugin path (e.g., `/usr/share/java/debezium-connector-sqlserver`). Make sure it's alongside other required JAR files.
   - For detailed instructions on deploying Debezium custom converters, visit the official documentation: [Debezium - Deploying Custom Converters](https://debezium.io/documentation/reference/stable/development/converters.html#deploying-a-debezium-custom-converter).

## Converters
This project aims to provide a variety of custom converters for Kafka Connect. 
Below is a list of currently available converters:
* [SqlVariantConverter](#sqlvariantconverter)

---

### SqlVariantConverter

Converts `sql_variant` fields from SQL Server into a string representation in Kafka Connect. 
This custom converter is used to handle the special `sql_variant` data type from SQL Server, 
enabling flexible data extraction and processing in Kafka Connect.

#### Configuration

- `field`: The name of the SQL field to be converted. 
   This field is required for the converter to know which column of type `sql_variant` it needs to process.

#### Example

```json
"transforms": "sql_variant_converter",
"transforms.sql_variant_converter.type": "name.ekt.kafka.connect.converter.SqlVariantConverter",
"transforms.sql_variant_converter.field": "variant_column"
```

---

## Contributing
We welcome contributions from the community. 
If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request.

## License
This project is licensed under the MIT License. See the LICENSE file for details.

Thank you for using Kafka Connect SQLServer Converters!






