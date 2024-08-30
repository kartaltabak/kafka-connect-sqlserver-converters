# Kafka Connect SQL Server Variant Converters

[![codecov](https://codecov.io/gh/username/repository/branch/main/graph/badge.svg)](https://codecov.io/gh/username/repository)

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

## Configuration
1. **Kafka Connect Configuration**:
    - Configure your Kafka Connect connector to use the custom converter.

### Example Configuration
Add the following properties to your Kafka Connect configuration file or properties:

```properties
# Kafka Connect configuration properties
name=example-connector
connector.class=io.debezium.connector.sqlserver.SqlServerConnector
tasks.max=1
database.hostname=localhost
database.port=1433
database.user=sa
database.password=yourpassword
database.dbname=testDB
table.include.list=dbo.testTable

# Custom converter properties
converters=sqlVariantConverter
sqlVariantConverter.type=name.ekt.kafka.connect.converter.SqlVariantConverter
sqlVariantConverter.field=yourFieldName
Field Configuration
Specify the field you want to convert by setting the sqlVariantConverter.field property to the name of the SQL sql_variant field in your table.

Converters
This project aims to provide a variety of custom converters for Kafka Connect. Below is a list of currently available converters and those planned for future development:

Available Converters
SqlVariantConverter: Converts SQL sql_variant types to string representation.
Planned Converters
XmlConverter: Converts XML data types to string or JSON representation.
JsonConverter: Converts JSON data types to string or native JSON representation.
GeographyConverter: Converts SQL geography types to a standardized string or JSON format.
HierarchyIdConverter: Converts SQL hierarchyid types to a readable string format.
We encourage contributions and suggestions for new converters. If you have an idea for a new converter, please open an issue or submit a pull request.

Contributing
We welcome contributions from the community. If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request.

Steps to Contribute
Fork the repository.
Create a new branch for your feature or bug fix.
Implement your changes.
Commit and push your changes to your fork.
Open a pull request with a detailed description of your changes.
License
This project is licensed under the MIT License. See the LICENSE file for details.

Acknowledgements
This project uses the following libraries and tools:

Debezium
Kafka Connect
Contact
For more information, please contact the project maintainers at [your-email@example.com].

Thank you for using Kafka Connect SQL Variant Converter!

css
Copy code

This version includes instructions to download the JAR file from the releases section, making it easier for users to install the converter. Feel free to adjust as necessary.





