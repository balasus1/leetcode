# 07: API Strategy, Integration Patterns & Security (OAuth2, OIDC, JWT, SSO, IAM) (Basic → Advanced)

> JD §4: *"RESTful APIs, event-driven integrations, streaming; API management and governance; enterprise integration patterns; third-party and SaaS integration."*
> JD §5: *"Enterprise security standards and compliance; secure authN/authZ frameworks; IAM, OAuth2, OIDC, JWT, SSO."*

| Section | Questions |
|---|---|
| API: basic / medium / advanced | API-B1–B8, API-M1–M8, API-A1–A3 |
| Integration patterns | INT-M1–M5 |
| Security: basic | SEC-B1–B8 |
| Security: medium | SEC-M1–M12 |
| Security: advanced | SEC-A1–A6 |
| Authentication libraries cheat sheet | §LIB |

---

## API: Basic

### API-B1. What makes an API RESTful?
🎤 "Resources identified by URIs, standard HTTP methods with correct semantics, stateless requests, representations (JSON) with content negotiation, proper status codes, and cacheability. HATEOAS is the purist extra; most enterprise APIs stop at level 2 of the Richardson maturity model. Nouns, not verbs: `POST /bookings`, not `/createBooking`."

### API-B2. Which HTTP methods are idempotent and safe?
🎤 "**Safe** (no side effects): GET, HEAD, OPTIONS. **Idempotent**: GET, PUT, DELETE, HEAD, OPTIONS. **POST** and **PATCH** are not idempotent by default, which is why payments and job triggers use an `Idempotency-Key` header."

### API-B3. Status codes you use deliberately?
🎤 "200 OK, 201 Created (with Location), 202 Accepted (async: return a status URL), 204 No Content, 400 validation, 401 unauthenticated, 403 forbidden, 404, 409 conflict (seat taken, invalid state transition), 412 precondition failed (ETag mismatch), 422 semantically invalid, 429 rate limited (with Retry-After), 500, 502/503/504 for upstream issues. Errors use RFC 9457 problem details."

### API-B4. API versioning strategies?
🎤 "URI (`/v1/`), header (`Accept: application/vnd.x.v2+json`), or query parameter. I prefer **URI major versions** for clarity in gateways and docs, with additive non-breaking changes inside a version, and a deprecation policy using `Deprecation` and `Sunset` headers plus consumer notification. Versioning is a last resort; compatibility discipline comes first."

### API-B5. Pagination: offset vs cursor?
🎤 "Offset/limit is simple but slow at deep pages and inconsistent under concurrent inserts. **Cursor (keyset)** pagination, `WHERE (created_at, id) < (?, ?) ORDER BY … LIMIT n` with an opaque cursor, is stable and fast. I use cursors for large or real-time collections like job runs and audit logs."

### API-B6. REST vs gRPC vs GraphQL?
🎤 "**REST**: universal, cacheable, great for public and partner APIs. **gRPC**: binary Protobuf over HTTP/2, fast, strongly typed, with streaming. Great for internal service-to-service calls with high throughput. **GraphQL**: clients shape the response, avoiding over- and under-fetching for complex UIs, but caching, authorization per field and query-cost control are harder. I use REST externally, gRPC internally where latency matters, and GraphQL as a BFF for rich front ends when justified."

### API-B7. What's contract-first vs code-first? ✅
🎤 "**Contract-first**: design the OpenAPI spec first, review it with consumers, then generate stubs and clients. That's the philosophy behind getConsenso. **Code-first**: annotate code and generate the spec. Contract-first enables parallel development, mocks for consumers, and automated breaking-change detection. Code-first is faster for internal prototypes but tends to leak implementation details."

### API-B8. How do you secure a public REST API at minimum?
🎤 "TLS only; OAuth2 access tokens validated at the gateway and service; scopes or roles per operation; input validation; rate limiting and quotas; no sensitive data in URLs; CORS configured narrowly; security headers; audit logging; and protection against the OWASP API Top 10, especially broken object-level authorization (always check that the caller owns the resource ID)."

---

## API: Medium

### API-M1. What API governance standards would you establish for an enterprise? ✅
🎤 (~1.5 min) "From building getConsenso, I'd set standards that are **enforced by tooling, not PDFs**. **Design**: contract-first OpenAPI 3.1, a style guide (resource naming, plural nouns, casing, date formats in ISO 8601 UTC, pagination via cursors, filtering conventions, problem-details errors, idempotency keys for POSTs). **Linting** in CI (Spectral rulesets). **Review**: an API design review for new or changed public contracts, with reviewer approval workflows, as getConsenso automates. **Compatibility**: breaking-change detection on every PR (openapi-diff), semantic versioning of contracts, a deprecation policy with sunset dates. **Discovery**: a central API catalogue or developer portal. **Runtime**: gateway policies for auth, quotas and analytics; SLOs per API. **Generated SDKs** so consumers don't hand-write clients. getConsenso generates Java, TypeScript, Go and Kotlin stubs. Governance should make the right thing the easy thing."

### API-M2. API gateway vs API management platform?
🎤 (~1 min) "A **gateway** is the runtime: routing, authentication, rate limiting, transformation. **API management** (Apigee, Azure APIM, Kong Enterprise, AWS API Gateway with usage plans) adds a developer portal, API keys and subscriptions, monetization, analytics, a lifecycle and versioning catalogue. For partner and public APIs you want full management. For internal east-west traffic, a lightweight gateway or mesh is enough."

### API-M3. How do you design async APIs (long-running operations)?
🎤 (~1 min) "Return **202 Accepted** with a `Location: /operations/{id}` status resource. Clients poll with backoff, or subscribe to a **webhook** or **event** for completion. The operation resource exposes state, progress, result link and errors. Document events with **AsyncAPI**, as you'd document REST with OpenAPI. The scheduler's trigger API worked like this, and so do long AI tasks."

### API-M4. Designing webhooks reliably (getConsenso + GitHub) ✅
🎤 (~1.5 min) "**Consuming** webhooks (GitHub into getConsenso): verify the **HMAC signature** (`X-Hub-Signature-256`) with a constant-time compare, respond with 2xx quickly and **process asynchronously** via a queue, **de-duplicate** by delivery ID because providers retry, handle out-of-order deliveries, and respect the provider's API rate limits when calling back. **Providing** webhooks: sign payloads with a per-subscriber secret and a timestamp (to prevent replay), retry with exponential backoff and a dead-letter, give subscribers a delivery log and redelivery button, keep payloads small (an ID plus type, so they fetch details with auth), and allow secret rotation."

```java
boolean valid(String payload, String signatureHeader, byte[] secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(UTF_8)));
    return MessageDigest.isEqual(expected.getBytes(UTF_8), signatureHeader.getBytes(UTF_8)); // constant-time
}
```

### API-M5. Rate limiting and quotas: algorithms and placement?
🎤 (~1 min) "Algorithms: **token bucket** (allows bursts; my usual choice), leaky bucket (smooth output), fixed window (simple, but boundary bursts), sliding window log or counter (accurate). Placement: at the **gateway** per API key, user or tenant; in the **service** for expensive operations, like the per-user limit in my RAG agent; and **outbound**, to respect partner quotas. Distributed limits use Redis with Lua or Bucket4j. Return 429 with `Retry-After` and rate-limit headers."

### API-M6. ETags and optimistic concurrency over HTTP?
🎤 (~45 s) "GET returns an `ETag` (the version). The client sends `If-Match` on PUT or PATCH. If the resource changed, return 412 Precondition Failed. It maps to the `@Version` column. Also `If-None-Match` for caching (304 Not Modified)."

### API-M7. BFF (Backend for Frontend): when?
🎤 (~1 min) "When different clients (web, mobile, partner) need different shapes and aggregation. The BFF composes downstream calls for one UI, so the front end doesn't make ten calls. It's also the right place to **hold OAuth tokens server-side** for SPAs, using httpOnly session cookies. Risks: duplicated logic across BFFs, so keep business rules in domain services."

### API-M8. How do you document and test APIs?
🎤 (~45 s) "OpenAPI as the source of truth, rendered in a portal (Redoc or Swagger UI), with examples. Tests: contract validation of requests and responses against the spec in integration tests, consumer-driven contracts for key consumers, Postman/Newman or REST Assured suites, and fuzzing (Schemathesis) for robustness."

---

## API: Advanced

### API-A1. Designing an enterprise integration architecture: what's in the document? ✅
🎤 (~1.5 min) "An **integration inventory**: every interface with source, target, protocol (REST, gRPC, Kafka, SFTP, SOAP), pattern (request-reply, pub-sub, batch, streaming), data classification, volume and peak, SLA, error handling, retry and idempotency policy, owner, and security mechanism. An **integration context diagram** (C4 context plus container). **Canonical models or anti-corruption layers** at boundaries. **Cross-cutting standards**: correlation IDs, schema registry, versioning, DLQ handling, monitoring per partner. **Non-functional mapping**: which integrations are on the critical path and need breakers and fallbacks. For ticketing that included payment gateways, SMS and email providers, the CDN, and internal Kafka topics."

### API-A2. Migrating a large API estate with breaking changes: how?
🎤 (~1 min) "Inventory consumers using gateway analytics. Introduce v2 alongside v1 with an adapter (v1 implemented as a façade over v2 where possible). Communicate a deprecation timeline, add Sunset headers, reach out to the top consumers directly, monitor v1 traffic down to zero, then remove. Generated SDKs make migration cheap for consumers, which is a getConsenso benefit."

### API-A3. REST API design for the scheduler (show me)
🎤 (~1 min) "Resources: `/v1/jobs` (definitions), `/v1/jobs/{id}/runs`, `/v1/runs/{id}`. Actions on state are modelled as **state-transition sub-resources or commands**, for example `POST /v1/jobs/{id}:pause` (Google API style) or `POST /v1/jobs/{id}/state-transitions {"to":"PAUSED"}`, returning 409 for invalid transitions and requiring `If-Match` for concurrency and `Idempotency-Key` for retries."

```yaml
/v1/jobs/{jobId}/state-transitions:
  post:
    operationId: transitionJob
    security: [{ oauth2: [jobs:operate] }]
    parameters:
      - { name: If-Match, in: header, required: true, schema: { type: string } }
      - { name: Idempotency-Key, in: header, required: true, schema: { type: string, format: uuid } }
    requestBody:
      content: { application/json: { schema: { type: object, required: [to], properties: { to: { enum: [PAUSED, RESUMED, STOPPED, DELETED] }, reason: { type: string } } } } }
    responses:
      '200': { description: Transitioned }
      '409': { description: Invalid transition, content: { application/problem+json: {} } }
      '412': { description: ETag mismatch }
```

---

## Enterprise Integration Patterns (EIP)

### INT-M1. Which EIPs do you use most, and where? ✅
🎤 (~1.5 min) "**Message channel / pub-sub** (Kafka topics). **Content-based router** (routing scheduler commands to per-service topics). **Message translator** and **canonical data model** (adapters for payment gateways and Factiva). **Splitter/aggregator** (splitting a bulk job into chunks, aggregating results). **Claim check** (large payloads in S3, with a reference in the message). **Idempotent receiver** (inbox table). **Dead letter channel** (DLT/DLX). **Correlation identifier** (bookingId across Saga events). **Process manager** (the orchestrator, as in scheduler chains). **Competing consumers** (consumer groups). **Wire tap** (copying events to audit and analytics). **Normalizer** for the many inbound formats in CRM ingestion: email, SMS, voice and chat into one interaction model. Frameworks like Apache Camel or Spring Integration implement these, but often plain Spring Kafka plus clear code is enough."

### INT-M2. Integrating with a flaky third-party payment gateway ✅
🎤 (~1.5 min) "**Adapter per gateway** behind a `PaymentPort`. **Idempotency key** = bookingId on every charge, so a retry can't double-charge. **Timeouts** tuned to the gateway's P99, then treat the outcome as **UNKNOWN** (not failed) and **reconcile** by querying the gateway, or via its callback or webhook (signature-verified). **Circuit breaker per gateway** with failover to a secondary gateway when commercially possible. **Daily settlement reconciliation** batch against gateway reports. **PCI scope minimisation**: hosted payment pages or tokenization, so card data never touches our systems. **Observability per partner**: success rate and latency dashboards, so we can raise evidence-based tickets."

### INT-M3. Integrating SaaS tools (Jira, Linear, GitHub) into a product ✅
🎤 (~1 min) "Use **OAuth apps** with minimal scopes per tenant, with tokens **encrypted at rest** (KMS/envelope encryption) and refreshed automatically. A per-tenant integration configuration. Webhooks in, API calls out via a queue with rate-limit-aware backoff (honour `X-RateLimit-Remaining` and Retry-After). Map their models through an anti-corruption layer (a 'ticket' abstraction over Jira issues and Linear issues). Audit every action with `tenant_id`, which is what I did in getConsenso."

### INT-M4. File-based / batch integration with legacy systems?
🎤 (~45 s) "Still common. Use SFTP or S3 with checksums and manifest files, idempotent processing (file ID plus row number), Spring Batch with restartability and skip reports, PGP encryption for sensitive files, and alerting on late or missing files (a 'heartbeat' SLA). Wrap the legacy system with an API or event façade when modernizing."

### INT-M5. ESB vs microservices integration?
🎤 (~1 min) "An ESB centralizes routing, transformation and orchestration, which often becomes a bottleneck and a single team's queue. The modern approach is **smart endpoints and dumb pipes**: services own their logic, a broker transports events, and integration logic lives in the domain services or in thin integration services per partner. I still use iPaaS (MuleSoft, Boomi) for SaaS-to-SaaS integration where business teams own flows."

---

## Security: Basic

### SEC-B1. Authentication vs authorization?
🎤 "**Authentication** proves who you are (login, MFA, certificate). **Authorization** decides what you may do (roles, scopes, policies). OIDC handles authentication; OAuth2 handles delegated authorization. A 401 means 'who are you?'; a 403 means 'I know who you are, and no'."

### SEC-B2. OAuth2 roles and main grant types?
🎤 "Roles: **resource owner** (user), **client** (app), **authorization server** (IdP), **resource server** (API). Grants: **Authorization Code + PKCE** (web, SPA and mobile apps with users), **Client Credentials** (service-to-service, no user), **Device Code** (TVs and CLIs), **Refresh Token**, and **Token Exchange** (RFC 8693, acting on behalf of a user). Implicit and Resource Owner Password grants are deprecated; don't use them."

### SEC-B3. OIDC vs OAuth2?
🎤 "OAuth2 is about **access**: the client gets an access token to call APIs. It doesn't define who the user is. **OpenID Connect** adds an **identity layer** on top: an **ID token** (a JWT with the user's identity claims), the `userinfo` endpoint, discovery (`.well-known/openid-configuration`) and standard scopes (`openid profile email`). Use the ID token for login in the client; use the access token for APIs. Never send ID tokens to APIs."

### SEC-B4. What's inside a JWT, and how is it validated?
🎤 "Three base64url parts: **header** (alg, kid), **payload** (claims: iss, sub, aud, exp, iat, scope, roles, tenant), **signature**. Validation: verify the signature with the issuer's public key from **JWKS** (matched by `kid`), check the **algorithm** is in an allow-list (reject `none` and HS/RS confusion), and check `iss`, `aud`, `exp`/`nbf` with small clock skew. JWTs are **signed, not encrypted**, so no secrets in claims (use JWE if you need encryption)."

### SEC-B5. Session cookies vs tokens?
🎤 "Server sessions (cookie with a session ID) are easy to revoke and keep tokens off the browser, but need shared session storage when scaled. Stateless JWTs scale easily, but are hard to revoke before expiry. A common enterprise pattern: **BFF with a session cookie** for the browser, and JWTs between the BFF and the APIs."

### SEC-B6. What is SSO, and how is it achieved?
🎤 "Single sign-on: log in once at the IdP, access many apps. Via **OIDC** (modern) or **SAML 2.0** (common in enterprises and legacy). The IdP session cookie lets the user get tokens for other apps without re-entering credentials. **Federation** connects your IdP to customers' corporate IdPs (Azure AD/Entra, Okta). Don't forget single logout and session lifetime policies."

### SEC-B7. RBAC vs ABAC vs ReBAC?
🎤 "**RBAC**: permissions via roles (admin, operator). Simple, but roles explode with nuance. **ABAC**: policies on attributes (user department, resource owner, time, tenant), so it's fine-grained. **ReBAC**: based on relationships (user → member of → team → owns → job), as in Google Zanzibar, OpenFGA and SpiceDB. I use RBAC for coarse access and ABAC/ReBAC checks for resource-level rules, via a policy engine (OPA, Cedar) or `@PreAuthorize` with custom beans."

### SEC-B8. Encryption in transit and at rest?
🎤 "**In transit**: TLS 1.2+ everywhere, mTLS between services (mesh), HSTS at the edge. **At rest**: database and disk encryption with KMS-managed keys, envelope encryption for application-level secrets or fields, per-tenant keys for regulated tenants, and key rotation. HSMs or PKCS#11 for the most sensitive keys, which I've integrated at IBM."

---

## Security: Medium

### SEC-M1. Authorization Code + PKCE for a React SPA: walk me through it
🎤 (~1.5 min) "The SPA generates a random `code_verifier` and its SHA-256 `code_challenge`, plus a `state` (CSRF) and `nonce`. It redirects to the IdP's `/authorize` with `response_type=code`, client_id, redirect_uri, scope `openid profile api.read`, and the challenge. The user logs in (MFA, SSO). The IdP redirects back with a `code`. The app checks `state` and posts the code plus `code_verifier` to `/token`. The IdP verifies the challenge and returns an access token, ID token and refresh token. PKCE prevents an intercepted code from being redeemed. **Where tokens live** is the real architecture question. In-memory in the SPA is OK for short-lived tokens. Never `localStorage`, because of XSS. My preferred enterprise pattern is the **BFF**: the BFF is the confidential OAuth client, holds tokens server-side, and gives the browser an httpOnly, Secure, SameSite cookie."

### SEC-M2. Spring Security as an OAuth2 resource server: how do you configure it? ✅
🎤 (~1.5 min) "Add `spring-boot-starter-oauth2-resource-server`, set `issuer-uri`, and Spring fetches the JWKS and validates signature, issuer and expiry automatically. I add an **audience validator**, map claims to authorities with a custom converter (realm roles in Keycloak, groups in Entra), configure stateless sessions, and use `@EnableMethodSecurity` for `@PreAuthorize` checks on business rules."

```java
@Configuration @EnableMethodSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/jobs/**").hasAuthority("SCOPE_jobs:read")
                .requestMatchers("/v1/jobs/**").hasAuthority("SCOPE_jobs:operate")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(rolesConverter())))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)            // stateless bearer API (keep CSRF for cookie-based BFF)
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains("scheduler-api"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }

    Converter<Jwt, AbstractAuthenticationToken> rolesConverter() {
        var scopes = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            Collection<GrantedAuthority> auth = new ArrayList<>(scopes.convert(jwt));
            Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of())
                    .forEach(r -> auth.add(new SimpleGrantedAuthority("ROLE_" + r)));
            return new JwtAuthenticationToken(jwt, auth, jwt.getSubject());
        };
    }
}

// Resource-level rule: operators can pause only jobs owned by their team (ABAC)
@PreAuthorize("hasRole('OPERATOR') and @jobAccess.ownedByCallerTeam(#jobId, authentication)")
public void pause(String jobId) { ... }
```

### SEC-M3. JWT revocation: how do you handle logout or compromise?
🎤 (~1 min) "Keep access tokens **short-lived** (5–15 min) so revocation windows are small. Use **refresh-token rotation with reuse detection** (if an old refresh token is reused, revoke the whole family). For immediate revocation: a **denylist of `jti`** in Redis checked at the gateway, or **opaque tokens with introspection** for high-risk APIs, trading latency for control. Session revocation at the IdP plus back-channel logout to clients."

### SEC-M4. Service-to-service authentication?
🎤 (~1 min) "**Client credentials** grant: each service has its own client ID with minimal scopes, and tokens are cached until near expiry. Or **mTLS** with workload identities (service mesh, SPIFFE/SPIRE). Often both: mTLS for the channel, JWT for authorization. When acting for a user, use **token exchange** so downstream services see the user's identity and scopes, which avoids the confused-deputy problem. Never share one 'god' service account across services."

### SEC-M5. Choosing an IdP: Keycloak vs Okta/Auth0 vs Entra ID vs Cognito?
🎤 (~1.5 min)

| IdP | Strengths | Watch out for |
|---|---|---|
| **Keycloak** (self-hosted / Red Hat build) | Open source, full OIDC/SAML, federation, fine control, runs on OpenShift | You operate it: HA, upgrades, DB, security patching |
| **Okta / Auth0** | Excellent developer experience, enterprise SSO connectors, MFA, B2B orgs | Cost per MAU at scale; data residency |
| **Microsoft Entra ID** | Natural for Microsoft-centric enterprises, conditional access, B2B | Customization limits for consumer (CIAM) flows |
| **AWS Cognito** | Cheap, AWS-native, integrates with API Gateway/ALB | Less flexible customization; migration constraints (password hashes) |

"For an enterprise on OpenShift with on-prem requirements, I'd pick Keycloak. For a SaaS with many B2B customers needing their own SSO, Auth0/Okta. For a Microsoft shop, Entra. For AWS-native consumer apps, Cognito. The application side stays standard OIDC with Spring Security, so the IdP stays swappable."

### SEC-M6. How do you design multi-tenant authorization? ✅
🎤 (~1.5 min) "The tenant comes from a **trusted token claim**, never from a request parameter. Enforce it at every layer. At the **gateway**, the token is valid for this tenant's routes. In the **service**, a filter or aspect binds the tenant context, and every repository query is scoped. In the **database**, PostgreSQL RLS as a backstop. Also in **caches and search** (tenant in keys and filters), **async messages** (tenant in headers, re-validated by consumers), and **audit** (`tenant_id` plus `api_key_id`, as in getConsenso). Roles are tenant-scoped (admin *of* tenant A). API keys for machine access are hashed at rest, scoped and rotatable. And an automated **cross-tenant access test** in CI tries to read tenant B's resources with tenant A's token."

### SEC-M7. OWASP API Security Top 10: which bite most often?
🎤 (~1 min) "**Broken object-level authorization** (changing an ID in the URL to see someone else's booking): fix with ownership checks on every resource access. **Broken authentication** (weak token validation, no audience check). **Broken object property-level authorization** (mass assignment or over-exposed fields): use explicit DTOs. **Unrestricted resource consumption**: rate limits, pagination caps, payload limits. **Broken function-level authorization** (admin endpoints reachable by users). **SSRF** in webhook or URL-fetch features: allow-lists and egress control. **Security misconfiguration** and **improper inventory** (forgotten v1 endpoints)."

### SEC-M8. Secrets management?
🎤 (~45 s) "Secrets live in Vault or AWS Secrets Manager, never in git, images or ConfigMaps. Inject at runtime (CSI driver or Spring Cloud Vault), rotate automatically (DB credentials as dynamic secrets), use **workload identity** (IRSA on EKS) instead of static cloud keys, and scan in CI with gitleaks. Audit secret access."

### SEC-M9. IAM on AWS: principles you apply?
🎤 (~1 min) "**Least privilege** with scoped policies (resource ARNs plus conditions); **roles, not users**, for workloads (IRSA or EKS Pod Identity for pods, instance profiles for EC2, OIDC federation for GitHub Actions, so CI has no long-lived keys); **SSO via IAM Identity Center** for humans with permission sets and MFA; multi-account isolation with **SCPs** as guardrails; permission boundaries for delegated admin; Access Analyzer to find unused or over-broad permissions; CloudTrail for everything."

### SEC-M10. Compliance frameworks: how do you design for them? ✅
🎤 (~1.5 min) "Identify early which apply: **GDPR** (personal data: lawful basis, minimization, right to erasure, data residency), **PCI-DSS** (card data: minimize scope with tokenization or hosted pages, segment the network, encrypt), **SOC 2 / ISO 27001** (controls on access, change management, monitoring, incident response), plus sector rules like AML and KYC (from my Detica work) or capital-markets record-keeping (Morgan Stanley). Then I map each requirement to **architecture controls**: data classification and flow diagrams, encryption and key management, IAM and access reviews, audit logging (tamper-evident where required), retention policies, vulnerability management, and change management via CI/CD with approvals. I automate **evidence collection** (pipeline logs, config-as-code, AWS Config rules), so audits are a report, not a scramble. For getConsenso's GDPR needs I designed tenant-aware audit logs with `tenant_id` and `api_key_id`."

### SEC-M11. JCA/JCE and PKCS#11: what did you do, and what's the architect's takeaway? ✅
🎤 (~1 min) "At the IBM Java Technology Centre I implemented security API integration for **IBM Tivoli** using **PKCS#11** (the standard API to talk to HSMs and cryptographic tokens) and the **Java Cryptography Architecture/Extension** providers on **z/OS**. So cryptographic operations happened inside hardware-backed providers and keys never left the HSM in clear text. The takeaways: never roll your own crypto; use the provider model, so algorithms and hardware are pluggable; keep keys in HSM or KMS, with the app getting handles, not raw keys; and design for algorithm agility (key rotation, deprecating SHA-1 or RSA-1024). Today the same idea is AWS KMS or CloudHSM, with envelope encryption."

### SEC-M12. Securing Kafka-based event-driven services ✅
🎤 (~1 min) "TLS and authentication on brokers (mTLS or SASL/OAUTHBEARER with the same IdP), **topic ACLs** per service (producer-only or consumer-only rights), separate consumer groups per service, no PII in keys, sensitive fields encrypted or tokenized in payloads, schema-registry access control, and **event-level authorization context** (tenant and acting user in headers) so consumers can enforce rules. That matches the 'event-driven services on Kafka with OAuth2/JWT security and RBAC' on my resume."

---

## Security: Advanced

### SEC-A1. Design an enterprise SSO and identity architecture for multiple apps and tenants
🎤 (~2 min) "Central **IdP** (Keycloak, Okta or Entra) as the single authority. **Users** authenticate with OIDC (SAML for legacy apps), MFA and conditional access. **B2B customers federate** their own corporate IdPs (per-tenant identity-provider brokering), so their employees use their own SSO and the tenant is mapped from the federation source. **Apps**: SPAs use Auth Code + PKCE via a **BFF**; mobile uses Auth Code + PKCE with system browsers; server apps are confidential clients. **APIs**: resource servers validating JWTs (audience per API, scopes per operation). **Services**: client credentials or mTLS, with token exchange for on-behalf-of calls. **Authorization**: coarse roles in tokens; fine-grained decisions in a policy engine (OPA/Cedar or OpenFGA) to keep tokens small and decisions fresh. **Lifecycle**: SCIM provisioning and deprovisioning from HR or customer directories, access reviews, session policies, single logout. **Operations**: IdP highly available (multi-AZ), key rotation via JWKS with overlapping keys, and audit of all auth events into the SIEM."

### SEC-A2. Zero-trust architecture for microservices
🎤 (~1.5 min) "Never trust the network. Every request is authenticated and authorized. **Identity for every workload** (SPIFFE IDs or mesh certificates), **mTLS everywhere**, **authorization policies** per service-to-service path (mesh AuthorizationPolicies), user identity propagated end-to-end (token exchange), **least-privilege** IAM and network policies (default deny), **short-lived credentials**, continuous verification (device posture and risk signals at the edge via conditional access), and comprehensive telemetry for detection. Segment by sensitivity: PCI workloads in their own namespace, account or cluster."

### SEC-A3. Threat modelling: how do you run it?
🎤 (~1 min) "On the data-flow diagram of the design, identify trust boundaries, then walk **STRIDE** per element: Spoofing (authentication), Tampering (integrity, signatures), Repudiation (audit), Information disclosure (encryption, authorization), Denial of service (rate limits, quotas), Elevation of privilege (authorization checks, least privilege). Rate the risks, map mitigations to backlog items, and revisit when the design changes. For AI features add prompt injection, data poisoning and over-privileged agents (OWASP LLM Top 10)."

### SEC-A4. Securing a GenAI/LLM integration in an enterprise
🎤 (~1 min) "Private endpoints to the model provider (no public egress), **no training on our data** contractually, **PII redaction before prompts and embeddings**, tenant-scoped retrieval, input and output **guardrails** (prompt-injection detection, content filters, schema validation), agents acting with the **user's delegated permissions** via token exchange plus a policy engine and human approval for destructive actions, audit of prompts and responses (hashed or encrypted), and an approved-model inventory. Details are in the genai pack: [genai/05_sidecar_pattern_ai_governance.md](../genai/05_sidecar_pattern_ai_governance.md)."

### SEC-A5. A security decision you'd make differently ✅
🎤 (~1 min) "Early in one project we stored the SPA's tokens in `localStorage` for convenience; we moved to a **BFF with httpOnly cookies** after a security review. In another, services shared a single client credential for convenience, which made audit and blast radius worse. Now there's one client per service and token exchange for user context. And in getConsenso, I'd introduce Postgres **RLS** as a backstop to application-level tenant filtering earlier."

### SEC-A6. Data protection: tokenization vs encryption vs masking?
🎤 (~45 s) "**Encryption** is reversible with a key and protects data at rest and in transit. **Tokenization** replaces the value with a token, with the mapping in a secure vault; it removes systems from compliance scope (PCI). **Masking** shows partial values (`****1234`) for display or logs. **Hashing** is one-way, for matching without storing the value (with salt or HMAC). Use them in combination by data classification."

---

## §LIB: Authentication & security libraries cheat sheet

| Layer | Library / product | Use |
|---|---|---|
| Java resource server | **Spring Security OAuth2 Resource Server** (Nimbus JOSE + JWT under the hood) | JWT/opaque token validation, scopes → authorities |
| Java OAuth2 client | **Spring Security OAuth2 Client** | Login (OIDC), client credentials, token exchange (6.3+), token relay |
| Own authorization server | **Spring Authorization Server** | When you must host OAuth2/OIDC yourself (prefer a product IdP) |
| JWT low-level | **Nimbus JOSE + JWT**, **jjwt**, Auth0 java-jwt | Custom signing/validation (avoid unless needed) |
| Gateway | **Spring Cloud Gateway** (TokenRelay filter), Kong/Apigee/AWS API Gateway JWT authorizers | Edge validation |
| Keycloak | Use standard Spring Security OIDC (Keycloak's Java adapters are deprecated) | — |
| Policy engines | **OPA** (Rego), **AWS Cedar / Verified Permissions**, **OpenFGA/SpiceDB** (ReBAC), Spring `@PreAuthorize` | Fine-grained authorization |
| React / SPA | **oidc-client-ts / react-oidc-context**, **@azure/msal-react**, **@auth0/auth0-react**, **AWS Amplify Auth** | PKCE flows in the browser (prefer BFF) |
| Next.js / Node | **Auth.js (NextAuth)**, **openid-client**, **passport** strategies, **jose** | OIDC in Node/Next (getConsenso-style stack) |
| Crypto | **JCA/JCE providers**, **Bouncy Castle**, **PKCS#11 (SunPKCS11)**, AWS KMS / CloudHSM SDKs | Keys, signing, HSM integration |
| Secrets | **Spring Cloud Vault**, AWS Secrets Manager + CSI driver | Runtime secret injection |
| Scanning | OWASP Dependency-Check, Snyk, Trivy, gitleaks, SonarQube | CI security gates |

🎤 (~45 s) "In Java I stay on **Spring Security** for both resource server and client, because it handles JWKS rotation, validation and authority mapping correctly, and I avoid hand-rolled JWT parsing. In the browser I prefer a **BFF** so tokens never reach JavaScript. Where needed, oidc-client-ts or MSAL implement PKCE. For fine-grained rules, a policy engine keeps authorization out of tokens and out of scattered `if` statements."
