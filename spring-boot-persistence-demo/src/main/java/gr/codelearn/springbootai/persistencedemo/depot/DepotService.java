package gr.codelearn.springbootai.persistencedemo.depot;

import gr.codelearn.springbootai.persistencedemo.common.ConflictException;
import gr.codelearn.springbootai.persistencedemo.common.NotFoundException;
import gr.codelearn.springbootai.persistencedemo.depot.dto.DepotCreateRequest;
import gr.codelearn.springbootai.persistencedemo.depot.dto.DepotResponse;
import gr.codelearn.springbootai.persistencedemo.depot.dto.DepotUpdateRequest;
import gr.codelearn.springbootai.persistencedemo.depot.mapper.DepotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for {@link Depot}. Reads are {@code readOnly}; the write methods open their own read-write transaction. Everything returns
 * response records except {@link #requireByCode}, which the other features use to wire their associations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepotService {

	private final DepotRepository depotRepository;
	private final DepotMapper depotMapper;

	Page<DepotResponse> findAll(final Pageable pageable) {
		return depotRepository.findAll(pageable).map(depotMapper::toResponse);
	}

	DepotResponse getByCode(final String code) {
		return depotMapper.toResponse(requireByCode(code));
	}

	@Transactional
	DepotResponse create(final DepotCreateRequest request) {
		if (depotRepository.existsByCode(request.code())) {
			throw new ConflictException("Depot '%s' already exists".formatted(request.code()));
		}
		final Depot depot = new Depot(request.code(), request.name(), request.depotType(), request.city(), request.region(),
				request.addressLine(), request.postalCode(), request.openedOn());
		return depotMapper.toResponse(depotRepository.save(depot));
	}

	/**
	 * Updates the mutable fields. No {@code save}: the entity is managed, so the flush at commit writes the change (dirty checking).
	 */
	@Transactional
	DepotResponse update(final String code, final DepotUpdateRequest request) {
		final Depot depot = requireByCode(code);
		depot.setName(request.name());
		depot.setCity(request.city());
		depot.setRegion(request.region());
		depot.setAddressLine(request.addressLine());
		depot.setPostalCode(request.postalCode());
		return depotMapper.toResponse(depot);
	}

	public Depot requireByCode(final String code) {
		return depotRepository.findByCode(code).orElseThrow(() -> new NotFoundException("Depot", code));
	}
}
