package com.fazbear.productos.controller;

import com.fazbear.productos.model.Producto;
import com.fazbear.productos.config.SecurityConfig;
import com.fazbear.productos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductoController.class, properties = "logging.level.org.springframework.security=WARN")
@Import(SecurityConfig.class)
class ProductoControllerStockTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductoService service;
    @MockitoBean JwtDecoder jwtDecoder;

    private Producto existing() {
        Producto producto = new Producto("Pizza", "Test", new BigDecimal("12.99"), "PIZZA", null);
        producto.setId(1L);
        producto.setStock(7);
        return producto;
    }

    @Test
    void createsNewProductWithTwentyUnitsIfStockIsOmitted() throws Exception {
        when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/productos").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Pizza\",\"precio\":12.99,\"categoria\":\"PIZZA\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(20))
            .andExpect(jsonPath("$.stockInformado").doesNotExist());
    }

    @Test
    void rejectsNegativeStockAtTheHttpBoundary() throws Exception {
        mvc.perform(post("/api/productos").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Pizza\",\"precio\":12.99,\"categoria\":\"PIZZA\",\"stock\":-1}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void updatingDescriptionWithoutStockDoesNotResetInventoryToTwenty() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(existing()));
        when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(put("/api/productos/1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Pizza\",\"precio\":12.99,\"categoria\":\"PIZZA\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(service).save(captor.capture());
        assertEquals(7, captor.getValue().getStock());
    }

    @Test
    void anExplicitInventoryAdjustmentIsHonored() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(existing()));
        when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(put("/api/productos/1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Pizza\",\"precio\":12.99,\"categoria\":\"PIZZA\",\"stock\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void authenticatedProductReadIncludesCurrentStock() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(existing()));
        mvc.perform(get("/api/productos/1").with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
    }
}
