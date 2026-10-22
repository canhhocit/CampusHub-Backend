---
name: springboot-tdd
description: Test-driven development for Spring Boot using JUnit 5, Mockito, MockMvc, and integration tests. Use when adding features, fixing bugs, testing authorization, or refactoring backend services.
metadata:
  origin: ECC
---

# Spring Boot TDD Workflow

TDD guidance for Spring Boot services with comprehensive coverage across unit, integration, and authorization testing.

## When to Use

- New features, services, or endpoints
- Bug fixes, regression reproductions, or refactors
- Adding data access logic, transaction boundaries, or security/authorization rules

## Workflow

1) Write tests first (they should fail initially)
2) Implement minimal code to pass
3) Refactor with tests green
4) Verify regression and edge cases

## Unit Tests (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class MarketServiceTest {
  @Mock MarketRepository repo;
  @InjectMocks MarketService service;

  @Test
  void createsMarket() {
    CreateMarketRequest req = new CreateMarketRequest("name", "desc", Instant.now(), List.of("cat"));
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Market result = service.create(req);

    assertThat(result.name()).isEqualTo("name");
    verify(repo).save(any());
  }
}
```

Patterns:
- Arrange-Act-Assert
- Avoid partial mocks; prefer explicit stubbing
- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@MethodSource` for input variations

## Web Layer & Authorization Tests (MockMvc)

```java
@WebMvcTest(MarketController.class)
class MarketControllerTest {
  @Autowired MockMvc mockMvc;
  @MockBean MarketService marketService;

  @Test
  @WithMockUser(roles = "USER")
  void returnsMarketsWhenAuthorized() throws Exception {
    when(marketService.list(any())).thenReturn(Page.empty());

    mockMvc.perform(get("/api/markets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void returnsUnauthorizedWhenAnonymous() throws Exception {
    mockMvc.perform(post("/api/markets")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
```

## Integration Tests (SpringBootTest)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketIntegrationTest {
  @Autowired MockMvc mockMvc;
  @Autowired MarketRepository marketRepository;

  @Test
  @WithMockUser(username = "seller@test.com", roles = "USER")
  void createsMarketSuccessfully() throws Exception {
    mockMvc.perform(post("/api/markets")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"name":"Test","description":"Desc","endDate":"2030-01-01T00:00:00Z","categories":["general"]}
        """))
      .andExpect(status().isCreated());

    assertThat(marketRepository.findAll()).hasSize(1);
  }
}
```

## Persistence Tests (DataJpaTest)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MarketRepositoryTest {
  @Autowired MarketRepository repo;
  @Autowired TestEntityManager entityManager;

  @Test
  void savesAndFindsByStatus() {
    MarketEntity entity = new MarketEntity();
    entity.setName("Test");
    entity.setStatus(MarketStatus.ACTIVE);
    entityManager.persistAndFlush(entity);

    Optional<MarketEntity> found = repo.findByName("Test");
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(MarketStatus.ACTIVE);
  }
}
```

## Transaction Boundaries in Tests

- By default, `@Transactional` on test classes rolls back mutations at the end of each test method, keeping the database clean.
- When testing async callbacks, event listeners, or committed states, configure `@Commit` or use an explicit transaction template to verify boundary isolation.

## Assertions

- Prefer AssertJ (`assertThat`) for readable, fluent assertions
- For JSON responses: `jsonPath("$.status").value("SUCCESS")`
- For exception handling: `assertThatThrownBy(() -> service.process(null)).isInstanceOf(IllegalArgumentException.class)`

## Test Data Builders

```java
class MarketBuilder {
  private String name = "Test";
  MarketBuilder withName(String name) { this.name = name; return this; }
  Market build() { return new Market(null, name, MarketStatus.ACTIVE); }
}
```

## Execution Commands

- Run all unit and integration tests: `mvn test`
- Run a specific test class: `mvn test -Dtest=MarketServiceTest`
- Run a specific test method: `mvn test -Dtest=MarketServiceTest#createsMarket`
- Run full build and verify: `mvn verify`

**Remember**: Keep tests fast, isolated, and deterministic. Test real behavior, authorization boundaries, and failure modes, not just the happy path.
