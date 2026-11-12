# AI Journal

## Initial project understanding

**Assessment source:** [`Apac_Assessment.md`](./Apac_Assessment.md)  
**Project:** Backend-for-Frontend (BFF) for ABC's APAC Policy Overview Dashboard

### What we are building

A production-quality REST service between the dashboard frontend and downstream systems. It will retrieve and aggregate insurance policy data, apply dashboard filters and pagination, support bulk review flagging, and provide summary statistics. The service is expected to have a documented API contract, persistent data, automated tests, operational concerns (including health checks), and configuration suitable for local development and deployment.

### Requirements

#### Required

- Define an OpenAPI 3.x contract before implementing endpoints; use it as the source of truth and drive or generate the server implementation from it.
- Provide these endpoints:
  - `GET /api/v1/policies`: paginated, sortable, filterable, searchable policy list.
  - `GET /api/v1/policies/{id}`: retrieve one policy by ID.
  - `PATCH /api/v1/policies/flag`: bulk-flag policies for review using an array of policy IDs.
  - `GET /api/v1/policies/summary`: counts by status, total premium by line of business, and count of policies expiring soon.
- Support list query parameters: `page`, `size`, `sort` (field and direction), `status`, `lineOfBusiness`, `region`, `effectiveDateFrom`, `effectiveDateTo`, and `search`. Search across policy number, policyholder name, and underwriter.
- Use a relational database with schema migrations. SQL Server is preferred; PostgreSQL or SQLite are acceptable for local development.
- Seed at least 200 realistic policies, covering all statuses and lines of business, APAC regions, varied dates, and premium amounts.
- Keep API, service, domain, and infrastructure concerns separated, with dependencies pointing inward and infrastructure details kept out of domain and service layers.
- Meet the list and detail performance target: p95 below 300 ms with 50 concurrent sessions.
- Provide production-quality automated tests.
- Address logging, error handling, health checks, externalized configuration, API documentation, and a runnable local setup.
- Apply DRY, SOLID, and 12-factor configuration principles.

#### Policy data

| Field | Type / constraints |
| --- | --- |
| `id` | UUID primary key |
| `policyNumber` | Unique string, format `POL-XXXXXX` |
| `policyholderName` | String; realistic APAC names |
| `lineOfBusiness` | `Property`, `Casualty`, `A&H`, or `Marine` |
| `status` | `Active`, `Expired`, `Pending`, or `Cancelled` |
| `premiumAmount` | Decimal, 1,000–5,000,000 |
| `currency` | `USD`, `SGD`, `HKD`, `AUD`, `JPY`, or `THB` |
| `effectiveDate`, `expiryDate` | Dates |
| `region` | Singapore, Hong Kong, Australia, Japan, Thailand, Indonesia, Malaysia, or Philippines |
| `underwriter` | String |
| `flaggedForReview` | Boolean, defaults to `false` |
| `createdAt`, `updatedAt` | Timestamps |

#### Bonus

- Cache commonly requested listings or summary statistics and define how cached values are invalidated.
- Publish an event when policies are flagged and consume policy status-change events through Kafka. Define event schemas and ensure consumer processing is idempotent.

### Database and API understanding

- **Database:** The user selected Microsoft SQL Server. The assessment requires a relational store, migrations, and seed data.
- **Likely persistence needs:** a policies table matching the fields above, uniqueness on `policyNumber`, and indexes chosen to support ID lookup and the list filters/sorting. Exact database, index set, and migration/seeding tooling are implementation decisions.
- **Public API:** The four endpoints explicitly listed in the assessment, described in OpenAPI 3.x. Details not supplied by the assessment are open questions, not additional requirements.
- **Health checks:** Required as a cross-cutting concern. The assessment does not specify a health-check API path; do not add one to the business API endpoint list without approval.
- **Optional integrations:** Kafka and a cache are bonuses, not prerequisites for the required BFF.

### Technology decisions

- **Backend:** Java with Spring Boot.
- **Database:** Microsoft SQL Server.

These are the user's selected stack choices and supersede the earlier open stack/database decisions.

### Assessment-aligned API list — confirmed

Only the four business endpoints below are explicitly specified in the assessment. This list intentionally does not add an operational endpoint.

#### 1. `GET /api/v1/policies`

- **Purpose:** List policies with pagination, sorting, filtering, and free-text search.
- **Query parameters specified:** `page`, `size`, `sort`, `status`, `lineOfBusiness`, `region`, `effectiveDateFrom`, `effectiveDateTo`, `search`.
- **Specified parameter details:**
  - `sort` takes a field and direction; the assessment gives `sort=premiumAmount,desc` as an example.
  - `status`: `Active`, `Expired`, `Pending`, `Cancelled`.
  - `lineOfBusiness`: `Property`, `Casualty`, `A&H`, `Marine`.
  - `search` searches `policyNumber`, `policyholderName`, and `underwriter`.
  - The policy schema lists allowed regions: Singapore, Hong Kong, Australia, Japan, Thailand, Indonesia, Malaysia, Philippines.
- **Request body:** None specified.
- **Response structure:** Not specified. The assessment's policy schema lists the policy data fields, but does not define the list envelope or whether/how those fields appear in this endpoint's response.
- **Status codes:** Not specified.
- **Open questions:** pagination defaults and limits; allowed sort fields/directions beyond the example; exact filter parsing and date format/range-boundary behavior; response envelope; error response and status-code conventions; validation behavior.

#### 2. `GET /api/v1/policies/{id}`

- **Purpose:** Get a single policy by ID.
- **Path parameter:** `id` is present in the path; its wire format/validation behavior is not specified. The policy schema defines `id` as UUID.
- **Query parameters / request body:** None specified.
- **Response structure:** Not specified beyond returning a single policy by ID; refer to the assessment's policy schema for the defined data fields.
- **Status codes and validation behavior:** Not specified.
- **Open questions:** response shape; behavior for malformed or unknown IDs; status codes and error response format.

#### 3. `PATCH /api/v1/policies/flag`

- **Purpose:** Bulk-flag policies for review.
- **Request body:** An array of policy IDs is required by the description; the exact JSON encoding/wrapper is not specified.
- **Query/path parameters:** None specified beyond the fixed path.
- **Response structure:** Not specified.
- **Status codes and validation behavior:** Not specified.
- **Open questions:** exact body shape and ID format; empty arrays, duplicates, unknown IDs, maximum batch size, partial versus atomic processing; response shape; status codes and error response format.

#### 4. `GET /api/v1/policies/summary`

- **Purpose:** Return aggregated statistics: counts by status, total premium by line of business, and expiring-soon count.
- **Query parameters / request body:** None specified.
- **Response structure:** The three statistics are specified, but their JSON representation is not.
- **Status codes and validation behavior:** Not specified.
- **Open questions:** exact response shape; meaning/time window/status scope of “expiring soon”; how totals should handle policies with different currencies; status codes and error response format.

#### Assessment-specified data constraints relevant to the API

- Policy ID is UUID; policy number is unique and formatted `POL-XXXXXX`.
- Policy status, line of business, currency, premium range, and regions are as listed in the Policy Data Schema above.
- The list query's status and line-of-business filters use the enums specified above; the assessment also requires region filtering.
- The assessment does not define API response schemas, standard error payloads, HTTP status codes, or detailed request-validation rules. Those remain open contract questions.

Health checks are a required cross-cutting concern, but no health-check route is specified in the assessment. It is not included in this API list.

### OpenAPI specification status

- Draft specification: [`openapi.yaml`](./openapi.yaml), using OpenAPI 3.1.0.
- Includes only the four assessment-specified business endpoints, their stated methods, the list query parameter names and explicitly defined values, the bulk flag request as an array of policy IDs, and policy data fields/constraints from the assessment.
- Does not define response schemas, success/error status codes, or additional validation/business behavior because the assessment does not specify them. These are explicitly identified in the specification as open for review.
- No backend implementation has started.

### Recommended implementation order

1. **Review the OpenAPI draft:** resolve open contract questions without adding unsupported endpoints or behavior.
2. **Finalize the OpenAPI 3.x specification:** document only approved request/response details for the four specified endpoint operations.
3. **Establish architecture and configuration:** create the API, application/service, domain, and infrastructure boundaries, keeping dependencies directed inward; establish externalized configuration and consistent error/logging conventions.
4. **Design persistence:** define the relational schema, migrations, constraints, and query-supporting indexes based on the contract.
5. **Add realistic seed data:** create 200+ records spanning every required enum and APAC region with representative dates and premiums.
6. **Implement domain and application behavior:** policy retrieval, list filtering/search/sorting/pagination, bulk flagging, and summary calculations, independently testable from infrastructure.
7. **Connect API and persistence:** implement the contract-driven endpoints and database adapters; document and verify health checks and local startup.
8. **Build the test suite:** cover domain/service rules, API validation and response contracts, persistence/migrations, and integration behavior; include repeatable test data.
9. **Measure and tune performance:** run a representative 50-concurrent-session load test and verify p95 below 300 ms for list and detail endpoints; tune queries/indexes or other bottlenecks based on evidence.
10. **Consider bonus capabilities:** add caching with invalidation and/or Kafka event production/consumption only after the required service works and is tested.
11. **Run final validation and document operation:** verify the required test/build checks, local setup, API docs, and operational configuration.

### Open decisions

- Resolve the API open questions listed under each assessment-specified endpoint before formalizing request/response details in OpenAPI.
- Decide whether to implement either bonus integration after completing and validating required scope.

## Work log

### 2026-11-12 — Initial assessment review

- Read `docs/Apac_Assessment.md`, `.github/copilot-instructions.md`, and `README.md`.
- Created this journal with the requirements, initial project understanding, recommended implementation order, and unresolved decisions.
- No application code was written or changed. No tests were run; this task was requirements documentation only.

### 2026-11-12 — Stack selection and API contract proposal

- Recorded the selected backend stack as Java/Spring Boot with Microsoft SQL Server.
- Initially drafted endpoint details and assumptions for review; the user identified that unsupported details and an extra operational route must not be included as API requirements.
- Rechecked the assessment and replaced that draft with only its four explicitly specified business endpoints. Unspecified request/response details, status codes, and behavior are now listed as open questions.
- Removed the unsupported operational route from the API list. Health checks remain an assessment cross-cutting requirement, with no route specified.
- No application code or formal OpenAPI file was written. No tests were run; this was a documentation correction.

### 2026-11-12 — OpenAPI draft created for review

- Created `docs/openapi.yaml` as an OpenAPI 3.1.0 draft based on the confirmed requirements.
- Included only the four specified business endpoints; no health-check route or other additional API was added.
- Left unspecified response bodies, status codes, and detailed validation/business rules open rather than assuming them.
- Updated this journal with the specification status. No backend code was changed and no tests were run.

### 2026-11-12 — Spring Boot backend scaffold

- After the user approved Spring Initializr, created the `backend/` Maven project with Java 21 and Spring Boot 4.1.2 using group ID `com.apac.assessment` and artifact ID `apac-policy-bff`.
- Included Spring MVC (not WebFlux), Spring Data JPA, validation, Flyway with SQL Server support, the Microsoft SQL Server JDBC driver, Lombok, and the generated Spring Boot test dependencies.
- Added Springdoc's Spring MVC Swagger UI starter (`springdoc-openapi-starter-webmvc-ui` 3.2.0).
- Configured SQL Server URL, username, and password through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; no connection values or credentials were added.
- The scaffold contains only the generated application entry point and test skeleton; no policy API implementation or migrations were added.
- `mvnw.cmd -DskipTests package` completed successfully. This compiled and packaged the project while skipping test execution; the generated context test requires a configured SQL Server connection.
- Confirmed the POM contains Spring MVC dependencies and no WebFlux dependencies.

### 2026-11-12 — Policy schema and Flyway migration

- User request: inspect database/Flyway setup; propose required SQL Server table structure before changes; create correctly ordered Flyway migrations; avoid extra tables/columns and Hibernate schema updates; run the application to verify migrations; record progress in `docs/AI_Journal.md`.
- Read the assessment and approved OpenAPI draft. The assessment specifies one policy record schema; the OpenAPI leaves response details open and adds no additional persistence fields.
- Inspected backend configuration: SQL Server datasource values are environment based, Flyway dependencies are installed, and no prior migration or Flyway settings existed.
- Presented the one-table proposal and open questions before database implementation. User approved all columns `NOT NULL`, proposed field lengths, application-managed timestamps, and only the primary key plus unique policy-number constraint/index; user reported SQL Server is configured and reachable locally.
- Added `V1__create_policies_table.sql` with the assessment policy fields, SQL Server-native data types, key/unique constraints, and check constraints for the specified policy number format, enums, regions, and premium bounds. No seed data or additional table was included.
- Configured Flyway to read `classpath:db/migration` and Hibernate schema mode to `validate`; no `ddl-auto=update` is used.
- Attempted to launch the application using the Java debugger. The tool reported a Maven build failure before startup; no active debug session was created. The Problems check reported no source errors, but the available output did not identify the Maven failure cause.
- Since the application did not start, Flyway migration execution against SQL Server remains unverified. No retry was made. The shell did not expose `SPRING_DATASOURCE_*` environment variable names, though the user indicated local SQL Server is configured.

### 2026-11-12 — Sample policy migration clarification pending

- User requested a `V2__insert_sample_policies.sql` migration with 200 policy rows and specified using `./mvnw` for Maven commands.
- Workspace inspection found the actual existing migration is `V1__create_policies_table.sql` and creates `dbo.policies`; the user referenced a different V1 filename and singular table name. No seed script was written pending confirmation of the correct target.
- The current datasource properties contain hard-coded credentials. Their values were not copied into this journal.
- No Maven command, application launch, or database migration was run for this request.

### 2026-11-12 — Seed migration created

- User confirmed the V2 target is the existing `dbo.policies` schema and approved replacing datasource credentials with environment-variable placeholders.
- Added `V2__insert_sample_policies.sql`, a SQL Server set-based seed migration producing 200 records with values matching the current V1 schema and assessment enums/ranges.
- Externalized datasource URL, username, and password in `application.properties`; no credential values were retained in documentation.
- Static checks confirmed 200 generated records, coverage of the required status/line/region/currency values, 200 unique policy numbers, and premium values within range.
- No Maven command or SQL Server/Flyway execution was performed; the migration has not yet been applied to a database.
- The user specified `./mvnw` for any Maven command.

### 2026-11-12 — Backend package structure proposal

- User reports the database schema migration has completed successfully.
- Inspected the existing backend: Java 21 / Spring Boot 4.1.2 Maven project, base package `com.apac.assessment`, one application entry point, one generated context test, SQL Server datasource placeholders, Flyway V1/V2 migrations, and no policy implementation packages.
- Proposed a layered structure covering controller, DTO, entity, enum, exception, repository, and service packages. Use `enums` because `enum` is a Java reserved word. Do not add a `config` package unless custom configuration is needed.
- Deferred DTO response details and exception mappings because the approved OpenAPI draft does not define response or error schemas/status codes.
- No application code was changed and no Maven commands or tests were run.

### 2026-11-12 — Layer scaffolding created

- Created initial Java classes under `com.apac.assessment` for controller, request/response DTOs, entity, enums with JSON/JPA value mapping, exception advice, repository, and service.
- Mapped the JPA entity to the existing `dbo.policies` schema and added UTC lifecycle timestamp handling consistent with application-managed timestamps.
- Left endpoint methods and exception mappings unimplemented because OpenAPI response/error details remain unresolved. No config package or new tests were added.
- `.\mvnw.cmd -DskipTests package` completed successfully; main and test sources compiled and the Spring Boot JAR was packaged. Tests were skipped. Maven showed non-blocking 401 metadata warnings from optional remote repositories.
- Asked which feature or requirement to implement next; the user cancelled the response form, so no next task was selected.
