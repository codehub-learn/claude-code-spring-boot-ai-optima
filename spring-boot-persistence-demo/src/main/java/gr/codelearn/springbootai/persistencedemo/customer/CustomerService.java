package gr.codelearn.springbootai.persistencedemo.customer;

import gr.codelearn.springbootai.persistencedemo.common.Address;
import gr.codelearn.springbootai.persistencedemo.common.ConflictException;
import gr.codelearn.springbootai.persistencedemo.common.NotFoundException;
import gr.codelearn.springbootai.persistencedemo.customer.dto.BusinessCustomerRequest;
import gr.codelearn.springbootai.persistencedemo.customer.dto.CustomerResponse;
import gr.codelearn.springbootai.persistencedemo.customer.dto.CustomerUpdateRequest;
import gr.codelearn.springbootai.persistencedemo.customer.dto.PrivateCustomerRequest;
import gr.codelearn.springbootai.persistencedemo.customer.mapper.CustomerMapper;
import gr.codelearn.springbootai.persistencedemo.depot.DepotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for {@link Customer}. Private and business customers are created through separate methods, which build them with the
 * {@link Customer} factories so the identity columns always match the customer type. {@link #requireByCode} is the entity-returning
 * method the other features use to wire their associations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final CustomerMapper customerMapper;
	private final DepotService depotService;

	Page<CustomerResponse> findByType(final CustomerType customerType, final Pageable pageable) {
		return customerRepository.findByCustomerType(customerType, pageable).map(customerMapper::toResponse);
	}

	CustomerResponse getByCode(final String customerCode) {
		return customerMapper.toResponse(requireByCode(customerCode));
	}

	@Transactional
	CustomerResponse createPrivate(final PrivateCustomerRequest request) {
		assertAvailable(request.customerCode(), request.email());
		final Customer customer = Customer.privateCustomer(request.customerCode(), request.firstName(), request.lastName(),
				request.email(), request.phone(), depotService.requireByCode(request.depotCode()),
				new Address(request.line1(), request.city(), request.postalCode()));
		return customerMapper.toResponse(customerRepository.save(customer));
	}

	@Transactional
	CustomerResponse createBusiness(final BusinessCustomerRequest request) {
		assertAvailable(request.customerCode(), request.email());
		final Customer customer = Customer.business(request.customerCode(), request.companyName(), request.vatNumber(), request.email(),
				request.phone(), depotService.requireByCode(request.depotCode()),
				new Address(request.line1(), request.city(), request.postalCode()));
		return customerMapper.toResponse(customerRepository.save(customer));
	}

	@Transactional
	CustomerResponse update(final String customerCode, final CustomerUpdateRequest request) {
		final Customer customer = requireByCode(customerCode);
		if (!customer.getEmail().equals(request.email()) && customerRepository.existsByEmail(request.email())) {
			throw new ConflictException("Email '%s' is already in use".formatted(request.email()));
		}
		customer.setEmail(request.email());
		customer.setPhone(request.phone());
		customer.setContractTier(request.contractTier());
		customer.setAddress(new Address(request.line1(), request.city(), request.postalCode()));
		return customerMapper.toResponse(customer);
	}

	public Customer requireByCode(final String customerCode) {
		return customerRepository.findByCustomerCode(customerCode).orElseThrow(() -> new NotFoundException("Customer", customerCode));
	}

	private void assertAvailable(final String customerCode, final String email) {
		if (customerRepository.existsByCustomerCode(customerCode)) {
			throw new ConflictException("Customer '%s' already exists".formatted(customerCode));
		}
		if (customerRepository.existsByEmail(email)) {
			throw new ConflictException("Email '%s' is already in use".formatted(email));
		}
	}
}
