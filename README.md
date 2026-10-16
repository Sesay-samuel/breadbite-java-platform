# Breadbite Java Platform

**Full-Stack E-Commerce Application | Java 21 | Spring Boot | PostgreSQL**

Breadbite is a Java-based bakery e-commerce platform developed to demonstrate full-stack software engineering, database integration, secure authentication, event-driven architecture, automated testing, and DevOps practices.

The project includes a customer-facing shopping application and a separate administration portal.

## Technology Stack

| Category | Technologies |
|---|---|
| Backend | Java 21, Spring Boot, Spring MVC |
| Frontend | Thymeleaf, HTML, CSS, JavaScript, Bootstrap |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Security | Spring Security, Keycloak, OAuth2 / OpenID Connect |
| Messaging | Apache Kafka |
| Event History | Apache Cassandra |
| Testing | JUnit, Maven, GitHub Actions |
| DevOps | Docker, Docker Compose, Kubernetes manifests |
| Additional Language | Kotlin |

## Key Features

### Customer Application
- Browse bakery products and view product details.
- Add products to the shopping cart.
- Manage cart quantities and checkout information.
- Enter shipping and billing addresses.
- Submit demonstration orders.
- View order confirmation details, including products, quantities, totals, and estimated delivery information.

### Administration Portal
- Manage bakery products.
- Configure product prices and inventory information.
- Upload product images.
- Maintain product catalogue information.

### Security
- Spring Security integration.
- Keycloak-based OpenID Connect authentication.
- Role-based access control within the application's security configuration.

### Event-Driven Architecture
- Apache Kafka integration for order-placement events.
- Transactional event handling after database commits.
- Kafka consumers for order notifications and event history.
- Apache Cassandra integration for event-history functionality.

### Testing and CI/CD
- Java unit testing with JUnit.
- Maven-based builds.
- GitHub Actions continuous integration.
- Docker-based application packaging.
- Kubernetes deployment configuration.

## Microsoft Azure and DevOps Experience

This project is part of my broader development portfolio, demonstrating skills relevant to cloud-native software engineering and DevOps.

### Azure and CI/CD Skills

- Microsoft Azure cloud platform fundamentals.
- Azure DevOps and Azure Pipelines configuration.
- YAML-based continuous integration workflows.
- Automated testing and dependency vulnerability scanning.
- Docker containerisation and Kubernetes deployment configuration.
- Secure management of application configuration using environment variables.

### Breadbite CI/CD Implementation

Breadbite uses GitHub Actions for automated build and test workflows and includes Docker and Kubernetes configuration.

My separate **CDR Analytics Dashboard** project demonstrates practical Azure DevOps pipeline configuration, backend Jest testing, and dependency security scanning.

**Deployment status:** Breadbite has not yet been deployed to Microsoft Azure or Render. Azure Pipelines should not be presented as an implemented Breadbite pipeline unless it is added and verified in this repository.

## Application Architecture

The project contains two Spring Boot applications:

**Customer application — `breadbitt`**

Handles product browsing, shopping cart functionality, checkout, customer interactions, and order processing.

**Administration application — `adminportal`**

Provides administrative product and catalogue management.

PostgreSQL stores relational application data. Kafka and Cassandra support additional event-driven functionality.

## Project Structure

```text
breadbite-java-platform/
├── .github/
│   └── workflows/
├── adminportal/
├── breadbitt/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── k8s/
├── docker-compose.cassandra.yml
├── docker-compose.kafka.yml
├── docker-compose.keycloak.yml
├── .gitignore
└── README.md
```

## Local Development

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Docker Desktop (for optional supporting services)
- Git

### Database Configuration

Create a PostgreSQL database named `breadbite` and configure the required environment variables:

```text
DB_URL=jdbc:postgresql://localhost:5432/breadbite
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
```

Additional environment variables may be required for Keycloak and other integrations.

Never commit passwords, tokens, or client secrets to source control.

### Run the Customer Application

From the `breadbitt` directory:

```bash
mvn spring-boot:run
```

Customer website:

`http://localhost:8098`

### Run the Administration Portal

From the `adminportal` directory, after configuring its required environment variables:

```bash
mvn spring-boot:run
```

Administration portal:

`http://localhost:8099/adminportal`

## Docker

The customer application includes a multi-stage Dockerfile using Maven and Eclipse Temurin Java 21.

Example build command from the repository root:

```bash
docker build -t breadbite-customer ./breadbitt
```

The repository also contains Docker Compose configurations for supporting infrastructure and Kubernetes manifests for deployment preparation.

## Project Status

**Status: Active development — local demonstration**

The shopping cart, checkout, and order confirmation workflows have been tested locally, including successful PostgreSQL order persistence.

Cloud hosting is not yet complete. Kafka, Keycloak, and Cassandra require appropriate service configuration for a cloud environment.

**Payment security notice:** The checkout is a demonstration implementation, not a production payment system. Do not enter real payment-card information. Production use would require a compliant payment provider, tokenisation, and removal of raw card-number and CVC storage.

## Future Improvements

- Deploy the applications to a managed cloud platform.
- Integrate a secure payment provider.
- Improve product-image storage and delivery.
- Add further automated integration and end-to-end tests.
- Implement production database migrations and observability.
- Harden security and deployment configuration.

## Repository

[GitHub — Breadbite Java Platform](https://github.com/Sesay-samuel/breadbite-java-platform)

## Author

Samuel Sesay

Software Developer | Java | Spring Boot | Full-Stack Development | Cloud & DevOps
