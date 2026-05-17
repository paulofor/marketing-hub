package com.marketinghub.experiment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.repository.MetricPresetRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MetricPresetServiceTest {

    @Mock
    private MetricPresetRepository repository;

    private MetricPresetService service;

    @BeforeEach
    void setUp() {
        service = new MetricPresetService(repository);
    }

    @Test
    void getThrowsBadRequestWhenIdIsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.get(" "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("metricPresetId required", ex.getReason());
    }

    @Test
    void getThrowsBadRequestWhenIdDoesNotExist() {
        when(repository.findById("legacy")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.get("legacy"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("metricPresetId not found: legacy", ex.getReason());
    }
}
