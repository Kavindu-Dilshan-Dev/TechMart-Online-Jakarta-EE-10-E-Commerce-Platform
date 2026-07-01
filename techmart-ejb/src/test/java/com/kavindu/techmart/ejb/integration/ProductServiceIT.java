package com.kavindu.techmart.ejb.integration;

import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.interfaces.ProductServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import com.kavindu.techmart.ejb.session.stateless.ProductServiceBean;
import com.kavindu.techmart.ejb.util.Mappers;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(ArquillianExtension.class)
class ProductServiceIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "techmart-it.war")
                .addPackages(true, "com.kavindu.techmart.common")
                .addClasses(ProductServiceBean.class, PerformanceMetricsBean.class,
                        SystemConfigBean.class, CircuitBreakerBean.class, Mappers.class)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private ProductServiceLocal productService;

    @Test
    void createAndRetrieveProduct() {
        ProductDTO dto = new ProductDTO();
        dto.setName("IT Test Widget");
        dto.setSku("IT-" + System.nanoTime());
        dto.setPrice(new BigDecimal("1999.00"));
        dto.setBrand("TestBrand");

        ProductDTO created = productService.createProduct(dto);
        Assertions.assertNotNull(created.getId());

        ProductDTO fetched = productService.findById(created.getId());
        Assertions.assertEquals("IT Test Widget", fetched.getName());
    }

    @Test
    void searchReturnsActiveProducts() {
        List<ProductDTO> results = productService.findAllActive(0, 10);
        Assertions.assertNotNull(results);
    }
}
