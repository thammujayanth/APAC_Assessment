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
