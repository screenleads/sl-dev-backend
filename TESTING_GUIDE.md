# Testing Guide - ScreenLeads Backend

## Table of Contents
1. [Overview](#overview)
2. [Testing Infrastructure](#testing-infrastructure)
3. [Test Categories](#test-categories)
4. [Naming Conventions](#naming-conventions)
5. [Test Patterns and Examples](#test-patterns-and-examples)
6. [Running Tests](#running-tests)
7. [Code Coverage](#code-coverage)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Overview

This project uses a comprehensive testing framework with:
- **JUnit 5** as the testing framework
- **Mockito 5.x** for mocking
- **AssertJ 3.x** for fluent assertions
- **H2 Database** for in-memory testing
- **Testcontainers** for integration testing
- **Spring Security Test** for security testing
- **JaCoCo** for code coverage (60% minimum requirement)

### Testing Philosophy

We follow a **quality over quantity** approach:
- 5-10 exemplary tests are worth more than 100 auto-generated tests
- Tests should be **readable**, **maintainable**, and **reliable**
- Each test should demonstrate a **clear pattern** that can be replicated
- Tests are **living documentation** of how the system works

---

## Testing Infrastructure

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Mockito for mocking -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- H2 for in-memory database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers for Docker-based testing -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Test Configuration (application-test.properties)

```properties
# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Disable Firebase in tests
firebase.enabled=false

# Test JWT Secret (Base64 encoded)
jwt.secret.key=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5n

# Disable actuator endpoints in tests
management.endpoints.enabled-by-default=false
```

### JaCoCo Configuration

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/entities/**</exclude>
            <exclude>**/init/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Test Categories

### 1. Unit Tests

**Purpose**: Test individual components in isolation with mocked dependencies

**Location**: `src/test/java/.../` (mirrors production structure)

**Characteristics**:
- Fast execution (milliseconds)
- No database, no Spring context
- Use Mockito for mocking
- Focus on business logic

**Examples**:
- `JwtServiceTest` - Security component
- `CompaniesServiceImplTest` - Service layer with mocks
- `CompanyControllerTest` - Controller slice test

### 2. Integration Tests

**Purpose**: Test multiple components working together with real infrastructure

**Location**: `src/test/java/.../integration/`

**Characteristics**:
- Slower execution (seconds)
- Real database (H2), full Spring context
- Use `@SpringBootTest`
- Test end-to-end flows

**Examples**:
- `AuthenticationIntegrationTest` - Full auth flow

### 3. Repository Tests

**Purpose**: Test JPA repositories with real database

**Location**: `src/test/java/.../repositories/`

**Characteristics**:
- Medium speed (hundreds of ms)
- Use `@DataJpaTest` for slice testing
- H2 in-memory database
- Test custom queries

**Examples**:
- `CompanyRepositoryTest` - Custom queries and JPA methods

### 4. Security Tests

**Purpose**: Test authentication, authorization, and security configurations

**Characteristics**:
- Use `@WithMockUser`, `@WithAnonymousUser`
- Test `@PreAuthorize` annotations
- Test JWT token validation
- Test permission checks

**Examples**:
- `JwtServiceTest` - Token generation/validation
- `CompanyControllerTest` - Authorization tests

---

## Naming Conventions

### File Naming

```
[ClassName]Test.java          // Unit test
[ClassName]IntegrationTest.java  // Integration test
```

### Test Method Naming

Use **descriptive names** that read like sentences:

```java
@Test
@DisplayName("Should return 404 when company does not exist")
void shouldReturn404WhenCompanyNotFound() {
    // ...
}
```

**Pattern**: `should[ExpectedBehavior]When[Condition]`

### Examples

✅ **Good Names**:
- `shouldReturnCompanyWhenFound()`
- `shouldThrowExceptionWhenIdIsNull()`
- `shouldReturn403WhenUserLacksPermission()`

❌ **Bad Names**:
- `testGetCompany()`
- `test1()`
- `companyTest()`

---

## Test Patterns and Examples

### Pattern 1: Service Unit Test (with Mocks)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CompaniesServiceImpl Tests")
class CompaniesServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private MediaRepository mediaRepository;
    
    @InjectMocks
    private CompaniesServiceImpl companiesService;

    @Test
    @DisplayName("Should return all companies as DTOs")
    void shouldReturnAllCompaniesAsDTOs() {
        // Given: Setup test data and mock behavior
        Company company = createCompany(1L, "Acme Corp");
        when(companyRepository.findAll()).thenReturn(Arrays.asList(company));

        // When: Execute the method under test
        List<CompanyDTO> result = companiesService.getAllCompanies();

        // Then: Verify results and interactions
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Acme Corp");
        verify(companyRepository, times(1)).findAll();
    }
}
```

**Key Points**:
- `@ExtendWith(MockitoExtension.class)` - Enables Mockito
- `@Mock` - Creates mock dependencies
- `@InjectMocks` - Injects mocks into the service
- **Given-When-Then** structure for clarity
- `verify()` to ensure repository interactions

### Pattern 2: Controller Test (REST API)

```java
@WebMvcTest(CompanyController.class)
@DisplayName("CompanyController Tests")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompaniesService companiesService;

    @MockBean
    private PermissionService permissionService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should return 200 OK with list of companies")
    void shouldReturnCompaniesWhenAdmin() throws Exception {
        // Given
        when(companiesService.getAllCompanies())
            .thenReturn(Arrays.asList(testCompanyDTO));

        // When/Then
        mockMvc.perform(get("/companies"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name").value("Test Company"));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/companies"))
            .andExpect(status().isUnauthorized());
    }
}
```

**Key Points**:
- `@WebMvcTest` - Slice test (only web layer)
- `@MockBean` - Mock service layer
- `@WithMockUser` - Mock authentication
- `MockMvc` - Simulate HTTP requests
- Test both success and failure scenarios

### Pattern 3: Repository Test (JPA)

```java
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("CompanyRepository Tests")
class CompanyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Should find company by name when exists")
    void shouldFindCompanyByNameWhenExists() {
        // Given: Persist test data
        Company company = createCompany("Acme Corp");
        entityManager.persist(company);
        entityManager.flush();

        // When: Execute query
        Optional<Company> result = companyRepository.findByName("Acme Corp");

        // Then: Verify result
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Acme Corp");
    }
}
```

**Key Points**:
- `@DataJpaTest` - Slice test (only persistence layer)
- `TestEntityManager` - Manage test entities
- `@TestPropertySource` - Use H2 test database
- Test custom queries and JPA methods

### Pattern 4: Integration Test (End-to-End)

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should complete full authentication flow")
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        // Given: User in database
        userRepository.save(testUser);

        // When: Login
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String token = extractTokenFromResponse(loginResult);

        // Then: Use token to access protected endpoint
        mockMvc.perform(get("/companies")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

**Key Points**:
- `@SpringBootTest` - Full Spring context
- `@AutoConfigureMockMvc` - Enable MockMvc
- `@Transactional` - Rollback after each test
- Test realistic user scenarios

### Pattern 5: Security Component Test

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        setSecretKey(jwtService, "test-secret-key-base64");
        jwtService.init();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // Given
        User user = createTestUser("user@example.com", "ROLE_USER");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        Claims claims = parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("user@example.com");
    }
}
```

**Key Points**:
- Use **real implementation** for security components
- Test token generation, validation, expiration
- Test edge cases (expired, malformed, tampered tokens)

---

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=CompanyControllerTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

Coverage report: `target/site/jacoco/index.html`

### Run with Coverage Check (60% minimum)

```bash
mvn clean verify
```

This will **fail the build** if coverage is below 60%.

### Run Only Unit Tests

```bash
mvn test -Dtest=**/*Test
```

### Run Only Integration Tests

```bash
mvn test -Dtest=**/*IntegrationTest
```

### Run in IDE (IntelliJ IDEA)

1. Right-click on test class/method
2. Select **Run '[TestName]'**
3. View results in Run window

### Run with Debug

```bash
mvn test -Dmaven.surefire.debug
```

Then attach debugger on port 5005.

---

## Code Coverage

### Understanding JaCoCo Reports

After running `mvn test jacoco:report`, open `target/site/jacoco/index.html`:

- **Green**: Fully covered lines
- **Yellow**: Partially covered (e.g., if-else with only one branch tested)
- **Red**: Not covered

### Coverage Metrics

- **Line Coverage**: % of code lines executed during tests
- **Branch Coverage**: % of if/else branches tested
- **Method Coverage**: % of methods invoked

### Minimum Requirements

- **Overall**: 60% line coverage (enforced by JaCoCo check)
- **Critical Components**: Aim for 80%+ (services, security)
- **DTOs/Entities**: Excluded from coverage

### Improving Coverage

1. **Identify gaps**: Check JaCoCo report for red lines
2. **Prioritize**: Focus on business logic, not getters/setters
3. **Write tests**: Use exemplary patterns from this guide
4. **Verify**: Run `mvn verify` to check if 60% is met

---

## Best Practices

### 1. Use Given-When-Then Structure

```java
@Test
void shouldReturnCompanyWhenFound() {
    // Given: Setup test data and preconditions
    when(repository.findById(1L)).thenReturn(Optional.of(company));

    // When: Execute the behavior under test
    Optional<CompanyDTO> result = service.getCompanyById(1L);

    // Then: Verify the outcome
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Acme Corp");
}
```

### 2. Use AssertJ for Fluent Assertions

✅ **Good (AssertJ)**:
```java
assertThat(result)
    .isPresent()
    .get()
    .extracting(CompanyDTO::getName)
    .isEqualTo("Acme Corp");
```

❌ **Bad (JUnit)**:
```java
assertTrue(result.isPresent());
assertEquals("Acme Corp", result.get().getName());
```

### 3. Test One Thing Per Test

✅ **Good**:
```java
@Test
void shouldReturnCompanyWhenFound() { /* ... */ }

@Test
void shouldReturn404WhenCompanyNotFound() { /* ... */ }
```

❌ **Bad**:
```java
@Test
void testGetCompany() {
    // Tests both success and failure in same method
}
```

### 4. Use @Nested for Grouping

```java
@Nested
@DisplayName("Get Company By ID Tests")
class GetCompanyByIdTests {
    
    @Test
    void shouldReturnCompanyWhenFound() { /* ... */ }
    
    @Test
    void shouldReturn404WhenNotFound() { /* ... */ }
}
```

### 5. Mock External Dependencies Only

✅ **Mock**: Repositories, external APIs, file systems

❌ **Don't Mock**: DTOs, entities, value objects

### 6. Use @BeforeEach for Common Setup

```java
@BeforeEach
void setUp() {
    testCompany = createCompany(1L, "Acme Corp");
    testCompanyDTO = createCompanyDTO(1L, "Acme Corp");
}
```

### 7. Test Edge Cases

Always test:
- **Null inputs**
- **Empty collections**
- **Not found scenarios**
- **Unauthorized access**
- **Invalid data**

### 8. Use Meaningful Test Data

✅ **Good**:
```java
Company company = createCompany(1L, "Acme Corp", "acme@example.com");
```

❌ **Bad**:
```java
Company company = new Company();
company.setId(1L);
company.setName("test");
```

### 9. Verify Mock Interactions

```java
verify(repository, times(1)).findById(1L);
verify(repository, never()).delete(any(Company.class));
```

### 10. Use @Transactional for Integration Tests

```java
@SpringBootTest
@Transactional  // Rollback after each test
class MyIntegrationTest {
    // ...
}
```

---

## Troubleshooting

### Issue: Tests fail with "No suitable driver found"

**Solution**: Ensure H2 dependency is in `pom.xml` and `application-test.properties` has correct JDBC URL.

### Issue: JaCoCo reports 0% coverage

**Solution**: Run `mvn clean test jacoco:report` (not just `mvn test`).

### Issue: @Autowired fields are null in unit tests

**Solution**: Use `@ExtendWith(MockitoExtension.class)` or `@SpringBootTest` depending on test type.

### Issue: "Cannot find symbol: @DisplayName"

**Solution**: Use JUnit 5 imports: `import org.junit.jupiter.api.DisplayName;`

### Issue: Security tests always return 200 (ignoring @PreAuthorize)

**Solution**: Add `@Import(SecurityConfig.class)` to `@WebMvcTest` or mock `PermissionService`.

### Issue: H2 syntax error "Function not found"

**Solution**: Add `MODE=PostgreSQL` to H2 URL in `application-test.properties`.

### Issue: Testcontainers fails to start

**Solution**: Ensure Docker is running. For CI/CD, use Testcontainers Cloud or disable Testcontainers tests.

---

## Appendix: Quick Reference

### Common Annotations

| Annotation | Purpose | Layer |
|-----------|---------|-------|
| `@ExtendWith(MockitoExtension.class)` | Enable Mockito | Unit |
| `@Mock` | Create mock object | Unit |
| `@InjectMocks` | Inject mocks | Unit |
| `@WebMvcTest` | Test controllers | Unit |
| `@DataJpaTest` | Test repositories | Unit |
| `@SpringBootTest` | Full integration test | Integration |
| `@AutoConfigureMockMvc` | Enable MockMvc in @SpringBootTest | Integration |
| `@Transactional` | Rollback after test | Integration |
| `@WithMockUser` | Mock authenticated user | Security |
| `@Nested` | Group related tests | Any |

### Common Assertions (AssertJ)

```java
// Basic
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isInstanceOf(Company.class);

// Collections
assertThat(list).hasSize(5);
assertThat(list).isEmpty();
assertThat(list).containsExactly("a", "b", "c");
assertThat(list).extracting(Company::getName).contains("Acme Corp");

// Optionals
assertThat(optional).isPresent();
assertThat(optional).isEmpty();
assertThat(optional).contains(expected);

// Exceptions
assertThatThrownBy(() -> service.method())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("Invalid input");

// Numbers
assertThat(actual).isGreaterThan(5);
assertThat(actual).isLessThanOrEqualTo(10);
assertThat(actual).isBetween(1, 10);

// Strings
assertThat(actual).startsWith("prefix");
assertThat(actual).contains("substring");
assertThat(actual).matches("regex.*");
```

### Common Mockito Methods

```java
// Stubbing
when(repository.findById(1L)).thenReturn(Optional.of(entity));
when(repository.save(any(Company.class))).thenReturn(savedEntity);

// Verification
verify(repository, times(1)).findById(1L);
verify(repository, never()).delete(any());
verify(repository, atLeastOnce()).save(any());

// Argument Capture
ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
verify(repository).save(captor.capture());
Company captured = captor.getValue();

// Matchers
any(), anyLong(), anyString(), eq(value), isNull(), isNotNull()
```

---

## Next Steps

1. **Study the exemplary tests** in this project:
   - `JwtServiceTest` - Security component
   - `CompaniesServiceImplTest` - Service with mocks
   - `CompanyControllerTest` - REST controller
   - `CompanyRepositoryTest` - JPA repository
   - `AuthenticationIntegrationTest` - End-to-end

2. **Replicate the patterns** for other components:
   - Use the same structure (Given-When-Then)
   - Use the same annotations
   - Use the same assertion style (AssertJ)

3. **Run coverage report** and identify gaps:
   ```bash
   mvn clean test jacoco:report
   ```

4. **Write tests for critical components first**:
   - Security (authentication, authorization)
   - Core business logic (services)
   - Custom repository queries

5. **Aim for quality, not quantity**:
   - Better to have 50 excellent tests than 200 mediocre ones
   - Each test should be maintainable and readable
   - Tests should fail for the right reasons

---

**Happy Testing! 🚀**
