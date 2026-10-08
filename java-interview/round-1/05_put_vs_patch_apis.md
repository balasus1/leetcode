# 05. PUT vs PATCH APIs

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In REST API design, **PUT** and **PATCH** are both HTTP methods used to update resources, but they differ fundamentally in **semantics, payload scope, and idempotency**:
>
> 1. **PUT represents Complete Replacement**: The client sends the entire updated resource representation. If any optional field is omitted in the PUT payload, the server typically overwrites it with `null` or its default value. PUT is **strictly Idempotent**, meaning sending identical PUT requests $N$ times results in the exact same server state.
> 2. **PATCH represents Partial Modification**: The client sends only the specific delta/fields that need updating (e.g., updating just the user's `email`). Non-specified fields remain untouched. By default, HTTP PATCH is **non-idempotent** (for example, if using JSON Patch append operations), though simple attribute updates can be designed idempotently.
>
> In production Spring Boot applications, we implement PUT with full DTO validation and entity replacement, and PATCH using either **JSON Merge Patch (RFC 7396)** or **JSON Patch (RFC 6902)** via Jackson's `JsonNode` or `JsonPatch` utilities to safely mutate only supplied fields."

---

## 🧠 Key Technical Comparison Matrix

| Feature | PUT (RFC 7231) | PATCH (RFC 5789) |
|---|---|---|
| **Semantic Meaning** | Replace the target resource entirely | Apply partial modifications |
| **Payload** | Complete resource representation | Only fields to change / Delta operations |
| **Missing Fields Behavior** | Reset to `null` or default values | Preserved unchanged |
| **Idempotency** | **Yes** ($f(f(x)) = f(x)$) | **No** (RFC spec says non-idempotent, though can be designed so) |
| **Safe (Read-Only)** | No | No |
| **HTTP Status on Success** | `200 OK` or `204 No Content` (or `201 Created` if creating) | `200 OK` or `204 No Content` |

---

## 💻 Spring Boot Production Implementation

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. PUT: Full Replacement (Idempotent)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> replaceUser(
            @PathVariable Long id,
            @Valid @RequestBody UserFullUpdateDto dto) {
        UserResponseDto updated = userService.replaceUser(id, dto);
        return ResponseEntity.ok(updated);
    }

    // 2. PATCH: Partial Modification (JSON Merge Patch)
    @PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
    public ResponseEntity<UserResponseDto> patchUser(
            @PathVariable Long id,
            @RequestBody JsonNode patchPayload) {
        UserResponseDto patched = userService.applyMergePatch(id, patchPayload);
        return ResponseEntity.ok(patched);
    }
}
```

### Applying JSON Merge Patch with Jackson Object Mapper:
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserResponseDto applyMergePatch(Long id, JsonNode patchPayload) {
        UserEntity existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        // Reader updates existing entity instance without wiping missing fields
        try {
            objectMapper.readerForUpdating(existing).readValue(patchPayload);
        } catch (IOException e) {
            throw new BadRequestException("Invalid patch payload", e);
        }

        UserEntity saved = userRepository.save(existing);
        return mapToDto(saved);
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can PUT be used to create a new resource?"
**Answer:**
> "Yes! If the client supplies the URI identifier (e.g., `PUT /api/orders/order-uuid-12345`), and that resource does not exist, the server can create it and return `201 Created`. If it exists, it replaces it and returns `200 OK` or `204 No Content`."

### 2. "How do you distinguish between a client wanting to set a field to `null` vs omitting the field in a PATCH request?"
**Answer:**
> "Standard Java DTOs deserialize omitted fields and explicit `null` fields both as `null`. To solve this:
> 1. Use `JsonNode` or `Map<String, Object>` to inspect key presence with `.has("fieldName")`.
> 2. Use `JsonNullable<T>` wrapper library (from Jackson / OpenAPI Tools) which differentiates between `undefined` (omitted) and `null`."
