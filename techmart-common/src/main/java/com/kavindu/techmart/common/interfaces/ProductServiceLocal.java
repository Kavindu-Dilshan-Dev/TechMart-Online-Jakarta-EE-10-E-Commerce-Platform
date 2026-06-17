package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.CategoryDTO;
import com.kavindu.techmart.common.dto.ProductDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;

public interface ProductServiceLocal {

    List<ProductDTO> findAllActive(int page, int size);

    long countActive();

    ProductDTO findById(Long id);

    ProductDTO findBySku(String sku);

    List<ProductDTO> findByCategory(Long categoryId);

    List<ProductDTO> searchProducts(String keyword, Long categoryId,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    int page, int size);

    ProductDTO createProduct(ProductDTO dto);

    ProductDTO updateProduct(Long id, ProductDTO dto);

    void deleteProduct(Long id);

    Future<List<ProductDTO>> searchProductsAsync(String keyword);

    List<CategoryDTO> getAllCategories();
}
