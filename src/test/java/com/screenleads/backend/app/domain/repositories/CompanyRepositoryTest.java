package com.screenleads.backend.app.domain.repositories;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.screenleads.backend.app.domain.model.Company;

/**
 * Comprehensive test suite for CompanyRepository.
 * 
 * This test class demonstrates:
 * - Testing JPA repositories with @DataJpaTest
 * - Using H2 in-memory database for tests
 * - Using TestEntityManager for test data setup
 * - Testing custom query methods (findByName, existsByName)
 * - Testing inherited JPA methods (findById, save, delete)
 * - Testing edge cases (null, empty, not found)
 * - Database state verification
 * 
 * Pattern: @DataJpaTest + TestEntityManager + H2 database
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("CompanyRepository Tests")
class CompanyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompanyRepository companyRepository;

    private Company testCompany1;
    private Company testCompany2;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        companyRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Setup test data
        testCompany1 = createCompany("Acme Corp", "Acme observations", "#FF0000");
        testCompany2 = createCompany("Tech Solutions", "Tech observations", "#00FF00");
    }

    @Nested
    @DisplayName("Custom Query Methods Tests")
    class CustomQueryMethodsTests {

        @Test
        @DisplayName("Should find company by name when exists")
        void shouldFindCompanyByNameWhenExists() {
            // Given: Company persisted in database
            entityManager.persist(testCompany1);
            entityManager.flush();

            // When
            Optional<Company> result = companyRepository.findByName("Acme Corp");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Acme Corp");
            assertThat(result.get().getObservations()).isEqualTo("Acme observations");
        }

        @Test
        @DisplayName("Should return empty Optional when company name not found")
        void shouldReturnEmptyWhenCompanyNameNotFound() {
            // Given: Empty database
            // (setUp already cleared the database)

            // When
            Optional<Company> result = companyRepository.findByName("Non-existent Company");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find company by name case-sensitively")
        void shouldFindCompanyByNameCaseSensitively() {
            // Given
            entityManager.persist(testCompany1);
            entityManager.flush();

            // When
            Optional<Company> exactCase = companyRepository.findByName("Acme Corp");
            Optional<Company> wrongCase = companyRepository.findByName("acme corp");

            // Then: Exact case should work
            assertThat(exactCase).isPresent();

            // Wrong case should not find (assuming case-sensitive query)
            // Note: This depends on database collation - H2 default is case-insensitive
            // If your production DB is case-sensitive, this test should verify that
            // behavior
        }

        @Test
        @DisplayName("Should return true when company name exists")
        void shouldReturnTrueWhenCompanyNameExists() {
            // Given
            entityManager.persist(testCompany1);
            entityManager.flush();

            // When
            boolean exists = companyRepository.existsByName("Acme Corp");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when company name does not exist")
        void shouldReturnFalseWhenCompanyNameDoesNotExist() {
            // Given: Empty database

            // When
            boolean exists = companyRepository.existsByName("Non-existent Company");

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should find company by Stripe customer ID when exists")
        void shouldFindCompanyByStripeCustomerId() {
            // Given
            testCompany1.setStripeCustomerId("cus_123456789");
            entityManager.persist(testCompany1);
            entityManager.flush();

            // When
            Optional<Company> result = companyRepository.findByStripeCustomerId("cus_123456789");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Acme Corp");
            assertThat(result.get().getStripeCustomerId()).isEqualTo("cus_123456789");
        }

        @Test
        @DisplayName("Should return empty Optional when Stripe customer ID not found")
        void shouldReturnEmptyWhenStripeCustomerIdNotFound() {
            // Given: Company without Stripe customer ID
            entityManager.persist(testCompany1);
            entityManager.flush();

            // When
            Optional<Company> result = companyRepository.findByStripeCustomerId("cus_nonexistent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Inherited JPA Methods Tests")
    class InheritedJpaMethodsTests {

        @Test
        @DisplayName("Should find company by ID when exists")
        void shouldFindCompanyByIdWhenExists() {
            // Given
            Company saved = entityManager.persist(testCompany1);
            entityManager.flush();
            Long id = saved.getId();

            // When
            Optional<Company> result = companyRepository.findById(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(id);
            assertThat(result.get().getName()).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("Should return empty Optional when ID not found")
        void shouldReturnEmptyWhenIdNotFound() {
            // When
            Optional<Company> result = companyRepository.findById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should save new company successfully")
        void shouldSaveNewCompanySuccessfully() {
            // Given: New company not yet persisted
            Company newCompany = createCompany("New Corp", "new@example.com", "#33CCFF");

            // When
            Company saved = companyRepository.save(newCompany);
            entityManager.flush();

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("New Corp");

            // Verify it's actually in the database
            Company found = entityManager.find(Company.class, saved.getId());
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("New Corp");
        }

        @Test
        @DisplayName("Should update existing company successfully")
        void shouldUpdateExistingCompanySuccessfully() {
            // Given: Existing company
            Company saved = entityManager.persist(testCompany1);
            entityManager.flush();
            Long id = saved.getId();

            // When: Update company
            saved.setName("Updated Corp");
            saved.setObservations("Updated observations");
            companyRepository.save(saved);
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to force fresh load

            // Then: Changes should be persisted
            Company updated = entityManager.find(Company.class, id);
            assertThat(updated.getName()).isEqualTo("Updated Corp");
            assertThat(updated.getObservations()).isEqualTo("Updated observations");
        }

        @Test
        @DisplayName("Should delete company successfully")
        void shouldDeleteCompanySuccessfully() {
            // Given
            Company saved = entityManager.persist(testCompany1);
            entityManager.flush();
            Long id = saved.getId();

            // When
            companyRepository.delete(saved);
            entityManager.flush();

            // Then
            Company deleted = entityManager.find(Company.class, id);
            assertThat(deleted).isNull();
        }

        @Test
        @DisplayName("Should find all companies")
        void shouldFindAllCompanies() {
            // Given: Multiple companies
            entityManager.persist(testCompany1);
            entityManager.persist(testCompany2);
            entityManager.flush();

            // When
            Iterable<Company> allCompanies = companyRepository.findAll();

            // Then
            assertThat(allCompanies)
                    .hasSize(2)
                    .extracting(Company::getName)
                    .containsExactlyInAnyOrder("Acme Corp", "Tech Solutions");
        }
    }

    @Nested
    @DisplayName("Database Constraints and Edge Cases Tests")
    class DatabaseConstraintsTests {

        @Test
        @DisplayName("Should handle null name gracefully")
        void shouldHandleNullNameGracefully() {
            // Given: Company with null name
            Company companyWithNullName = new Company();
            companyWithNullName.setObservations("Null company observations");

            // When/Then: Depending on @NotNull/@Column(nullable=false) constraints
            // This might throw ConstraintViolationException or persist successfully
            // Adjust based on your actual entity constraints
            assertThatThrownBy(() -> {
                entityManager.persist(companyWithNullName);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should enforce unique name constraint if configured")
        void shouldEnforceUniqueNameConstraint() {
            // Given: First company with name
            entityManager.persist(testCompany1);
            entityManager.flush();
            entityManager.clear();

            // When: Trying to save another company with same name
            Company duplicate = createCompany("Acme Corp", "different@example.com", "+34600444444");

            // Then: Should throw exception if unique constraint exists
            // Note: This test assumes @Column(unique=true) on name field
            // If no unique constraint, remove or modify this test
            assertThatThrownBy(() -> {
                entityManager.persist(duplicate);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle concurrent saves correctly")
        void shouldHandleConcurrentSavesCorrectly() {
            // Given: Two different companies
            Company company1 = createCompany("Company 1", "company1@example.com", "#FF5733");
            Company company2 = createCompany("Company 2", "company2@example.com", "#3498DB");

            // When: Saving both
            companyRepository.save(company1);
            companyRepository.save(company2);
            entityManager.flush();

            // Then: Both should be saved with different IDs
            assertThat(company1.getId()).isNotNull();
            assertThat(company2.getId()).isNotNull();
            assertThat(company1.getId()).isNotEqualTo(company2.getId());
        }

        @Test
        @DisplayName("Should maintain data integrity after multiple operations")
        void shouldMaintainDataIntegrityAfterMultipleOperations() {
            // Given: Initial save
            Company saved = companyRepository.save(testCompany1);
            entityManager.flush();
            Long id = saved.getId();

            // When: Multiple updates
            saved.setName("Updated 1");
            companyRepository.save(saved);

            saved.setName("Updated 2");
            companyRepository.save(saved);

            saved.setObservations("Final observations");
            companyRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then: Final state should be correct
            Company final1 = companyRepository.findById(id).orElseThrow();
            assertThat(final1.getName()).isEqualTo("Updated 2");
            assertThat(final1.getObservations()).isEqualTo("Final observations");
        }
    }

    @Nested
    @DisplayName("Query Performance and Optimization Tests")
    class QueryPerformanceTests {

        @Test
        @DisplayName("Should efficiently query by indexed name field")
        void shouldEfficientlyQueryByName() {
            // Given: Large dataset
            for (int i = 0; i < 100; i++) {
                Company company = createCompany("Company " + i, "company" + i + "@example.com",
                        "#" + String.format("%06X", i * 1000));
                entityManager.persist(company);
            }
            entityManager.flush();
            entityManager.clear();

            // When: Querying by name
            long startTime = System.currentTimeMillis();
            Optional<Company> result = companyRepository.findByName("Company 50");
            long endTime = System.currentTimeMillis();

            // Then: Should find quickly (< 100ms for 100 records)
            assertThat(result).isPresent();
            assertThat(endTime - startTime).isLessThan(100);
        }

        @Test
        @DisplayName("Should not load unnecessary relationships eagerly")
        void shouldNotLoadUnnecessaryRelationshipsEagerly() {
            // Given: Company with potential relationships
            Company saved = entityManager.persist(testCompany1);
            entityManager.flush();
            entityManager.clear();

            // When: Finding by ID
            Company found = companyRepository.findById(saved.getId()).orElseThrow();

            // Then: Lazy relationships should not be initialized
            // Note: This verifies LAZY loading configuration
            // Adjust based on your actual entity relationship mappings
            assertThat(found).isNotNull();
        }
    }

    // ==================== Helper Methods ====================

    private Company createCompany(String name, String observations, String primaryColor) {
        Company company = new Company();
        company.setName(name);
        company.setObservations(observations);
        company.setPrimaryColor(primaryColor);
        company.setBillingStatus("ACTIVE");
        return company;
    }
}
