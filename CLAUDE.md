# user-service — Claude Agent

## Read these first (via Notion MCP)
- DOP-001: https://app.notion.com/p/3412ba48f4878140a5ebf3df60896b9b
- IRD-001: https://app.notion.com/p/3412ba48f48781be8364d68a02f8bafd

---

## Scope
This repo contains user-service only.
Only implement what is defined in IRD-001. Nothing more.

---

## NEVER
- Generate code for task-service, api-gateway, or notification-service
- Add endpoints not defined in IRD-001
- Store plaintext passwords — BCrypt only
- Allow client to set `role` at registration
- Hardcode secrets — all config via env vars
- Return different 401 messages for wrong email vs wrong password
- Use `ddl-auto=create` or `update` — Flyway manages schema
- Call repository directly from controller — always go through service
- Return `@Entity` from controller — always map to DTO via MapStruct
- Use try/catch in controller or serviceImpl — throw exceptions, let GlobalExceptionHandler catch
- Write methods longer than 20 lines — split into private helpers
- Use `@Data` on JPA entities — use `@Getter` + `@Setter` + `@Builder` separately
- Use `@Autowired` for dependency injection — always use `@RequiredArgsConstructor` + `private final`
- Use `System.out.println` — always use `@Slf4j` + `log.info()`

---

## Spring Boot Code Conventions

---

### Directory Structure

```
src/main/java/com/proops2026/userservice/
├── controller/        HTTP layer only — receive request, return response, no logic
├── service/           Interfaces + implementations
│   ├── UserService.java          ← interface
│   ├── AuthService.java          ← interface
│   └── impl/
│       ├── UserServiceImpl.java  ← business logic
│       └── AuthServiceImpl.java
├── repository/        JpaRepository interfaces + custom queries
│   ├── UserRepository.java       ← extends JpaRepository
│   └── impl/
│       └── UserRepositoryImpl.java  ← complex queries
├── model/             JPA entities — maps to database tables
├── dto/
│   ├── request/       Input objects — what the client sends
│   └── response/      Output objects — what the client receives
├── mapper/            MapStruct interfaces — entity ↔ DTO conversion
├── util/              Stateless helpers — JWT, date, string utils
├── exception/         Custom exceptions + GlobalExceptionHandler
└── UserServiceApplication.java
```

---

### Lombok — Required Annotations

Use Lombok to eliminate boilerplate. Never write getters, setters, or constructors manually.

**Entity (`model/`):**
```java
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String email;
    private String passwordHash;
    private String role;
    private LocalDateTime createdAt;
}
```
> Do NOT use `@Data` on entities — causes issues with JPA lazy loading and equals/hashCode.

**Request DTO (`dto/request/`):**
```java
@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}
```

**Response DTO (`dto/response/`):**
```java
@Getter
@Builder
public class UserResponse {
    private String id;
    private String email;
    private LocalDateTime createdAt;
}
```

**ServiceImpl:**
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
}
```
> Use `@RequiredArgsConstructor` + `private final` — never use `@Autowired`.

---

### Layer Rules

**Controller** — HTTP only, no business logic
```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.register(request));
    }
}
```

**Service (interface)** — one method per use case
```java
public interface UserService {
    UserResponse register(CreateUserRequest request);
    LoginResponse login(LoginRequest request);
}
```

**ServiceImpl** — all business logic, extract private helpers
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(CreateUserRequest request) {
        validateEmailNotTaken(request.getEmail());
        User saved = userRepository.save(buildUser(request));
        log.info("User registered: {}", request.getEmail());
        return userMapper.toResponse(saved);
    }

    private void validateEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email already in use");
        }
    }

    private User buildUser(CreateUserRequest request) {
        return User.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role("member")
            .build();
    }
}
```

---

### Helper Rules
- If a block appears more than once — extract to private method
- If a method exceeds 20 lines — split it
- Name helpers after what they do: `validateEmailNotTaken`, `buildUser`, `extractClaims`
- Helpers are `private` in ServiceImpl — never expose through the interface

---

### MapStruct — Entity ↔ DTO
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
    User toEntity(CreateUserRequest request);
}
```
- One mapper per entity, lives in `mapper/`
- Never map manually — always use MapStruct
- Never call mapper from Controller — only from ServiceImpl
- If a field needs custom logic, use `@Mapping` or a `@Named` helper method

---

### Exception Handling
```java
// Custom exception
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

// Global handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(UserAlreadyExistsException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("validation error");
        return ResponseEntity.status(400).body(new ErrorResponse(message));
    }
}
```
- No try/catch in Controller or ServiceImpl — throw, let handler catch
- One exception class per error type
- All errors return `{ "message": "string" }` — no stack traces

---

### Validation — on Request DTOs only
```java
@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}
```
- Message must match exactly what IRD-001 defines
- Controller uses `@Valid` — never validate manually in ServiceImpl

---

### Naming Conventions

| Type | Pattern | Example |
|---|---|---|
| Class | PascalCase | `UserServiceImpl` |
| Method | camelCase, verb-first | `findByEmail`, `validateEmailNotTaken` |
| Variable | camelCase | `savedUser`, `hashedPassword` |
| Constant | UPPER_SNAKE_CASE | `JWT_SECRET_KEY` |
| DTO request | Action + Request | `CreateUserRequest`, `LoginRequest` |
| DTO response | Entity + Response | `UserResponse`, `LoginResponse` |
| Exception | Noun + Exception | `UserAlreadyExistsException` |
| Mapper | Entity + Mapper | `UserMapper` |

---

### Logging
```java
@Slf4j  // Lombok — generates: private static final Logger log = ...
public class UserServiceImpl implements UserService {
    log.info("User registered: {}", email);
    log.warn("Login failed for: {}", email);
    log.error("Unexpected error: {}", ex.getMessage());
}
```
- Every inbound request logged via `LoggingInterceptor`: `[timestamp] METHOD /path → STATUS (Xms)`
- Never log passwords or tokens
- Never use `System.out.println`

---

### Testing
- Integration tests only — real test database, no mocks
- Use `@SpringBootTest` + real test MySQL container
- One test class per controller: `UserControllerTest`, `AuthControllerTest`
- Method name: `methodName_condition_expectedResult`

```java
@Test
void register_duplicateEmail_returns409() { ... }

@Test
void login_wrongPassword_returns401() { ... }
```

---

### Database Rules (MySQL 8)
```sql
id    CHAR(36) PRIMARY KEY DEFAULT (UUID())
dates DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
```
- Schema changes in `db/migrations/V{n}__description.sql`
- `spring.jpa.hibernate.ddl-auto=validate`
- Never use `ddl-auto=create` or `update`