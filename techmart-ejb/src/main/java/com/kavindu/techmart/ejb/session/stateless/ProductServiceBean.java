package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.CategoryDTO;
import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.entity.Category;
import com.kavindu.techmart.common.entity.Product;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.interfaces.ProductServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.util.Mappers;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@Local(ProductServiceLocal.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ProductServiceBean implements ProductServiceLocal {

    private static final Logger LOG = Logger.getLogger(ProductServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private PerformanceMetricsBean metrics;

    @Override
    public List<ProductDTO> findAllActive(int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        List<Product> products = em.createNamedQuery("Product.findAllActive", Product.class)
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();
        return toDtos(products);
    }

    @Override
    public long countActive() {
        return em.createNamedQuery("Product.countActive", Long.class).getSingleResult();
    }

    @Override
    public ProductDTO findById(Long id) {
        Product p = em.find(Product.class, id);
        if (p == null) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return Mappers.toProductDTO(p, totalStock(id));
    }

    @Override
    public ProductDTO findBySku(String sku) {
        try {
            Product p = em.createNamedQuery("Product.findBySku", Product.class)
                    .setParameter("sku", sku)
                    .getSingleResult();
            return Mappers.toProductDTO(p, totalStock(p.getId()));
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("Product not found for sku: " + sku);
        }
    }

    @Override
    public List<ProductDTO> findByCategory(Long categoryId) {
        List<Product> products = em.createNamedQuery("Product.findByCategory", Product.class)
                .setParameter("categoryId", categoryId)
                .getResultList();
        return toDtos(products);
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword, Long categoryId,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("active")));
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("brand")), like),
                    cb.like(cb.lower(root.get("sku")), like)));
        }
        if (categoryId != null) {
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("id")));

        List<Product> products = em.createQuery(cq)
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();
        return toDtos(products);
    }

    @Override
    @Asynchronous
    public Future<List<ProductDTO>> searchProductsAsync(String keyword) {
        long start = System.currentTimeMillis();
        List<ProductDTO> result = searchProducts(keyword, null, null, null, 0, 50);
        metrics.recordRequest("ProductService.searchAsync", System.currentTimeMillis() - start);
        LOG.fine(() -> "Async search for '" + keyword + "' returned " + result.size() + " products");
        return new AsyncResult<>(result);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ProductDTO createProduct(ProductDTO dto) {
        Product p = new Product();
        applyDto(p, dto);
        p.setActive(true);
        em.persist(p);
        LOG.info("Created product '" + p.getName() + "' (sku=" + p.getSku() + ")");
        return Mappers.toProductDTO(p, 0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product p = em.find(Product.class, id);
        if (p == null) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        applyDto(p, dto);
        return Mappers.toProductDTO(p, totalStock(id));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteProduct(Long id) {
        Product p = em.find(Product.class, id);
        if (p == null) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        p.setActive(false);
        LOG.info("Soft-deleted product " + id);
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = em.createNamedQuery("Category.findAll", Category.class).getResultList();
        return categories.stream()
                .map(c -> Mappers.toCategoryDTO(c, countProductsInCategory(c.getId())))
                .collect(Collectors.toList());
    }

    private void applyDto(Product p, ProductDTO dto) {
        if (dto.getName() != null) {
            p.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            p.setDescription(dto.getDescription());
        }
        if (dto.getSku() != null) {
            p.setSku(dto.getSku());
        }
        if (dto.getPrice() != null) {
            p.setPrice(dto.getPrice());
        }
        p.setDiscountedPrice(dto.getDiscountedPrice());
        if (dto.getBrand() != null) {
            p.setBrand(dto.getBrand());
        }
        if (dto.getImageUrl() != null) {
            p.setImageUrl(dto.getImageUrl());
        }
        if (dto.getCategoryId() != null) {
            Category c = em.find(Category.class, dto.getCategoryId());
            p.setCategory(c);
        }
    }

    private List<ProductDTO> toDtos(List<Product> products) {
        List<ProductDTO> dtos = new ArrayList<>(products.size());
        for (Product p : products) {
            dtos.add(Mappers.toProductDTO(p, totalStock(p.getId())));
        }
        return dtos;
    }

    private int totalStock(Long productId) {
        Long sum = em.createNamedQuery("Inventory.totalAvailableForProduct", Long.class)
                .setParameter("productId", productId)
                .getSingleResult();
        return sum == null ? 0 : sum.intValue();
    }

    private long countProductsInCategory(Long categoryId) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.category.id = :cid", Long.class);
        q.setParameter("cid", categoryId);
        return q.getSingleResult();
    }
}
