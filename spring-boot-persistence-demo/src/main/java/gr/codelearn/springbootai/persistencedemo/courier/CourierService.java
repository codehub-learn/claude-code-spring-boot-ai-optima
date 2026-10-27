package gr.codelearn.springbootai.persistencedemo.courier;

import gr.codelearn.springbootai.persistencedemo.common.ConflictException;
import gr.codelearn.springbootai.persistencedemo.common.NotFoundException;
import gr.codelearn.springbootai.persistencedemo.courier.dto.CourierCreateRequest;
import gr.codelearn.springbootai.persistencedemo.courier.dto.CourierResponse;
import gr.codelearn.springbootai.persistencedemo.courier.dto.CourierUpdateRequest;
import gr.codelearn.springbootai.persistencedemo.courier.mapper.CourierMapper;
import gr.codelearn.springbootai.persistencedemo.depot.DepotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for {@link Courier}. The depot is resolved through {@link DepotService}, never through its repository.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CourierService {

	private final CourierRepository courierRepository;
	private final CourierMapper courierMapper;
	private final DepotService depotService;

	Page<CourierResponse> findByDepot(final String depotCode, final Pageable pageable) {
		return courierRepository.findByDepotCode(depotCode, pageable).map(courierMapper::toResponse);
	}

	CourierResponse getByEmployeeCode(final String employeeCode) {
		return courierMapper.toResponse(requireByEmployeeCode(employeeCode));
	}

	@Transactional
	CourierResponse create(final CourierCreateRequest request) {
		if (courierRepository.existsByEmployeeCode(request.employeeCode())) {
			throw new ConflictException("Courier '%s' already exists".formatted(request.employeeCode()));
		}
		final Courier courier = new Courier(depotService.requireByCode(request.depotCode()), request.employeeCode(), request.firstName(),
				request.lastName(), request.phone(), request.vehicleType(), request.dailyCapacity(),
				request.hiredOn());
		return courierMapper.toResponse(courierRepository.save(courier));
	}

	@Transactional
	CourierResponse update(final String employeeCode, final CourierUpdateRequest request) {
		final Courier courier = requireByEmployeeCode(employeeCode);
		courier.setPhone(request.phone());
		courier.setVehicleType(request.vehicleType());
		courier.setDailyCapacity(request.dailyCapacity());
		courier.setActive(request.active());
		return courierMapper.toResponse(courier);
	}

	Courier requireByEmployeeCode(final String employeeCode) {
		return courierRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new NotFoundException("Courier", employeeCode));
	}
}
