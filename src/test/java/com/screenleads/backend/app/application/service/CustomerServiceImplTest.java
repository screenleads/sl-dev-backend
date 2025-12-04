package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl Unit Tests")
class CustomerServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Company testCompany;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");

        testCustomer = Customer.builder()
                .id(1L)
                .company(testCompany)
                .identifierType(LeadIdentifierType.EMAIL)
                .identifier("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("create should create new customer with valid data")
    void whenCreateWithValidData_thenCreatesCustomer() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                anyLong(), any(LeadIdentifierType.class), any(String.class)))
                .thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        Customer result = customerService.create(1L, LeadIdentifierType.EMAIL, 
                "test@example.com", "John", "Doe");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompany()).isEqualTo(testCompany);
        verify(companyRepository, times(1)).findById(1L);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("create should throw exception when company not found")
    void whenCreateWithInvalidCompany_thenThrowsException() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.create(999L, LeadIdentifierType.EMAIL, 
                "test@example.com", "John", "Doe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    @DisplayName("create should throw exception when customer already exists")
    void whenCreateDuplicateCustomer_thenThrowsException() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                1L, LeadIdentifierType.EMAIL, "test@example.com"))
                .thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThatThrownBy(() -> customerService.create(1L, LeadIdentifierType.EMAIL, 
                "test@example.com", "John", "Doe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Customer already exists");
    }

    @Test
    @DisplayName("update should update existing customer")
    void whenUpdateExistingCustomer_thenUpdatesSuccessfully() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                anyLong(), any(LeadIdentifierType.class), any(String.class)))
                .thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        Customer result = customerService.update(1L, LeadIdentifierType.EMAIL, 
                "newemail@example.com", "Jane", "Smith");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIdentifier()).isEqualTo("newemail@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("update should throw exception when customer not found")
    void whenUpdateNonExistentCustomer_thenThrowsException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(999L, LeadIdentifierType.EMAIL, 
                "test@example.com", "John", "Doe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("update should throw exception when new identifier already exists")
    void whenUpdateToExistingIdentifier_thenThrowsException() {
        // Arrange
        Customer anotherCustomer = Customer.builder()
                .id(2L)
                .company(testCompany)
                .identifierType(LeadIdentifierType.EMAIL)
                .identifier("another@example.com")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                1L, LeadIdentifierType.EMAIL, "another@example.com"))
                .thenReturn(Optional.of(anotherCustomer));

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(1L, LeadIdentifierType.EMAIL, 
                "another@example.com", "John", "Doe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Another customer already uses this identifier");
    }

    @Test
    @DisplayName("get should return customer by id")
    void whenGetExistingCustomer_thenReturnsCustomer() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.get(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("get should throw exception when customer not found")
    void whenGetNonExistentCustomer_thenThrowsException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.get(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("list should return all customers when no companyId provided")
    void whenListWithNoCompanyId_thenReturnsAll() {
        // Arrange
        Customer customer2 = Customer.builder().id(2L).build();
        when(customerRepository.findAll()).thenReturn(Arrays.asList(testCustomer, customer2));

        // Act
        List<Customer> result = customerService.list(null, null);

        // Assert
        assertThat(result).hasSize(2);
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("list should return customers by company when companyId provided")
    void whenListWithCompanyId_thenReturnsCompanyCustomers() {
        // Arrange
        when(customerRepository.findByCompanyId(1L))
                .thenReturn(Arrays.asList(testCustomer));

        // Act
        List<Customer> result = customerService.list(1L, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testCustomer);
        verify(customerRepository, times(1)).findByCompanyId(1L);
    }

    @Test
    @DisplayName("list should return filtered customers when search provided")
    void whenListWithSearch_thenReturnsFilteredCustomers() {
        // Arrange
        when(customerRepository.findByCompanyIdAndIdentifierContainingIgnoreCase(1L, "test"))
                .thenReturn(Arrays.asList(testCustomer));

        // Act
        List<Customer> result = customerService.list(1L, "test");

        // Assert
        assertThat(result).hasSize(1);
        verify(customerRepository, times(1))
                .findByCompanyIdAndIdentifierContainingIgnoreCase(1L, "test");
    }

    @Test
    @DisplayName("delete should delete existing customer")
    void whenDeleteExistingCustomer_thenDeletesSuccessfully() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        customerService.delete(1L);

        // Assert
        verify(customerRepository, times(1)).findById(1L);
        verify(customerRepository, times(1)).delete(testCustomer);
    }

    @Test
    @DisplayName("delete should throw exception when customer not found")
    void whenDeleteNonExistentCustomer_thenThrowsException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.delete(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }
}
