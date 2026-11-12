# Background

ABC's APAC operations team manages insurance policies across multiple regions. The front-end team is building a Policy Overview Dashboard and needs a Backend-for-Frontend (BFF) service to aggregate, transform, and serve policy data. Your task is to design and implement this BFF service.

The BFF will sit between the front-end application and downstream systems (database, cache, message broker). It must be production-quality — the kind of service you would confidently put through a pull request review with senior engineers on your team.

# Technology Choice

You may implement the service using either:

- C# / .NET — the current stack for the CDP platform
- Java / Spring Boot — the stack for the Commercial Insurance platform

Choose the stack you are strongest in. Both are equally valid and both are actively used across ABC APAC engineering.

# Requirements

## Contract-First API Design

The service must expose a RESTful API. You are expected to take a contract-first approach:

- OpenAPI 3.x (Swagger) specification defined before implementing endpoints
- Generate server stubs or use the contract to drive your implementation
- The contract should be the single source of truth for the API shape

## Core API Endpoints

| Endpoint                       | Description                                                                                                              |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| `GET /api/v1/policies`         | List policies with pagination, sorting, filtering (by status, line of business, date range, region) and free-text search |
| `GET /api/v1/policies/{id}`    | Get a single policy by ID                                                                                                |
| `PATCH /api/v1/policies/flag`  | Bulk flag policies for review (accepts array of policy IDs)                                                              |
| `GET /api/v1/policies/summary` | Aggregated statistics — counts by status, total premium by line of business, expiring-soon count                         |

## Pagination and Filtering Contract

The `GET /api/v1/policies` endpoint should support:

- `page` and `size` query parameters (with sensible defaults)
- `sort` parameter (field and direction, e.g. `sort=premiumAmount,desc`)
- `status` filter (enum: `Active`, `Expired`, `Pending`, `Cancelled`)
- `lineOfBusiness` filter (enum: `Property`, `Casualty`, `A&H`, `Marine`)
- `region` filter
- `effectiveDateFrom` / `effectiveDateTo` range filter
- `search` free-text parameter (searches across `policyNumber`, `policyholderName`, `underwriter`)

## Database Integration (Required)

Use a relational database with proper schema management via migrations. SQL Server is preferred (it matches the CDP production stack on Azure SQL); PostgreSQL or SQLite are acceptable for local development.

Seed the database with 200+ realistic policy records covering all status values, lines of business, APAC regions, and a realistic spread of dates and premium amounts.

## Policy Data Schema

| Field              | Type      | Notes                                                                                              |
| ------------------ | --------- | -------------------------------------------------------------------------------------------------- |
| `id`               | UUID      | Primary key                                                                                        |
| `policyNumber`     | String    | Unique, format: `POL-XXXXXX`                                                                       |
| `policyholderName` | String    | Realistic APAC names                                                                               |
| `lineOfBusiness`   | Enum      | `Property`, `Casualty`, `A&H`, `Marine`                                                            |
| `status`           | Enum      | `Active`, `Expired`, `Pending`, `Cancelled`                                                        |
| `premiumAmount`    | Decimal   | Range: 1,000 – 5,000,000                                                                           |
| `currency`         | String    | `USD`, `SGD`, `HKD`, `AUD`, `JPY`, `THB`                                                           |
| `effectiveDate`    | Date      |                                                                                                    |
| `expiryDate`       | Date      |                                                                                                    |
| `region`           | String    | `Singapore`, `Hong Kong`, `Australia`, `Japan`, `Thailand`, `Indonesia`, `Malaysia`, `Philippines` |
| `underwriter`      | String    |                                                                                                    |
| `flaggedForReview` | Boolean   | Default: `false`                                                                                   |
| `createdAt`        | Timestamp |                                                                                                    |
| `updatedAt`        | Timestamp |                                                                                                    |

## Clean Architecture (Required)

Demonstrate clear architectural layering with dependencies pointing inward. API, service, domain, and infrastructure concerns should be properly separated. Infrastructure details should not leak into domain or service layers.

## Performance (Required)

The BFF service must meet defined performance targets under realistic concurrent load:

- All list and detail endpoints must respond at p95 < 300ms under 50 concurrent sessions.

## Caching (Bonus)

A caching layer for frequently accessed data such as summary statistics or policy listings, with a clear invalidation strategy.

## Kafka Integration (Bonus)

Implement a Kafka producer that publishes events when policies are flagged for review, and a consumer that listens for policy status change events. Idempotent consumer handling and a well-defined event schema are expected.

## Principles We Value

We expect the service to reflect the principles a senior engineer would naturally apply: DRY, SOLID, 12-factor configuration, and contract-first thinking — not as a checklist, but evident in how the code is structured.

## Test Automation (Required)

We expect production-quality engineering standards for testing. Think about what a senior engineer would expect to see in a pull request. Consider performance, maintainability, and confidence in deployment.

## Cross-Cutting Concerns

A production service needs more than just endpoints. Consider:

- Logging
- Error handling
- Health checks
- Externalised configuration
- API documentation
- A runnable local setup

## Clean Architecture (Required)

Demonstrate clear architectural layering with dependencies pointing inward. API, service, domain, and infrastructure concerns should be properly separated. Infrastructure details should not leak into domain or service layers.

## Performance (Required)

The BFF service must meet defined performance targets under realistic concurrent load:

- All list and detail endpoints must respond at p95 < 300ms under 50 concurrent sessions.

## Caching (Bonus)

A caching layer for frequently accessed data such as summary statistics or policy listings, with a clear invalidation strategy.

## Kafka Integration (Bonus)

Implement a Kafka producer that publishes events when policies are flagged for review, and a consumer that listens for policy status change events. Idempotent consumer handling and a well-defined event schema are expected.
