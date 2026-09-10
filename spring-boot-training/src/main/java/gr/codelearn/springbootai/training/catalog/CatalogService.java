package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.catalog.dto.CategoryResponse;
import gr.codelearn.springbootai.training.catalog.dto.CreateCategoryRequest;
import gr.codelearn.springbootai.training.catalog.dto.CreateProductRequest;
import gr.codelearn.springbootai.training.catalog.dto.ProductFilter;
import gr.codelearn.springbootai.training.catalog.dto.ProductResponse;
import gr.codelearn.springbootai.training.catalog.dto.UpdateProductRequest;
import gr.codelearn.springbootai.training.catalog.internal.CatalogDuplicateException;
import gr.codelearn.springbootai.training.catalog.mapper.CatalogWebMapper;
import gr.codelearn.springbootai.training.common.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use-case orchestration for the product and category resources (U4.1). {@code @Transactional} lives here, never on
 * {@code CatalogController}; reads are {@code readOnly = true}. The service returns {@code *Response} records only — {@link Product} and
 * {@link Category} entities never leave it (CLAUDE.md), and the mapping runs inside the transaction while the LAZY {@code category}
 * association is still reachable.
 *
 * <p>Validation is minimal on purpose: a duplicate business key or an unknown category throws, everything else is trusted until U4.2 adds
 * Bean Validation at the controller boundary.
 */
@Service
public class CatalogService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final CatalogWebMapper mapper;

	CatalogService(final ProductRepository productRepository, final CategoryRepository categoryRepository,
				   final CatalogWebMapper mapper) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> findProducts(final ProductFilter filter, final Pageable pageable) {
		Category category = null;
		if (filter.category() != null && !filter.category().isBlank()) {
			category = categoryRepository.findByName(filter.category()).orElse(null);
			if (category == null) {
				return Page.empty(pageable);
			}
		}
		final Specification<Product> spec = Specification.allOf(
				ProductSpecifications.nameContains(filter.name()),
				ProductSpecifications.inCategory(category),
				ProductSpecifications.priceAtLeast(filter.minPrice()),
				ProductSpecifications.priceAtMost(filter.maxPrice()));
		return mapper.toResponsePage(productRepository.findAll(spec, pageable));
	}

	@Transactional(readOnly = true)
	public ProductResponse getProduct(final String sku) {
		return productRepository.findBySku(sku)
								.map(mapper::toResponse)
								.orElseThrow(() -> new NotFoundException("No product with sku " + sku));
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> listCategories() {
		return categoryRepository.findAll().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse getCategory(final String name) {
		return categoryRepository.findByName(name)
								 .map(mapper::toResponse)
								 .orElseThrow(() -> new NotFoundException("No category named " + name));
	}

	@Transactional
	public ProductResponse createProduct(final CreateProductRequest request) {
		if (productRepository.existsBySku(request.sku())) {
			throw new CatalogDuplicateException("A product with sku " + request.sku() + " already exists");
		}
		final Category category = requireCategory(request.categoryName());
		final Product product = new Product(request.sku(), request.name(), request.price(), category);
		product.setDescription(request.description());
		return mapper.toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse replaceProduct(final String sku, final UpdateProductRequest request) {
		final Product product = productRepository.findBySku(sku)
												 .orElseThrow(() -> new NotFoundException("No product with sku " + sku));
		product.setName(request.name());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setCategory(requireCategory(request.categoryName()));
		return mapper.toResponse(productRepository.save(product));
	}

	@Transactional
	public void deleteProduct(final String sku) {
		final Product product = productRepository.findBySku(sku)
												 .orElseThrow(() -> new NotFoundException("No product with sku " + sku));
		productRepository.delete(product);
	}

	@Transactional
	public CategoryResponse createCategory(final CreateCategoryRequest request) {
		if (categoryRepository.existsByName(request.name())) {
			throw new CatalogDuplicateException("A category named " + request.name() + " already exists");
		}
		return mapper.toResponse(categoryRepository.save(new Category(request.name(), request.description())));
	}

	private Category requireCategory(final String name) {
		return categoryRepository.findByName(name)
								 .orElseThrow(() -> new NotFoundException("No category named " + name));
	}
}