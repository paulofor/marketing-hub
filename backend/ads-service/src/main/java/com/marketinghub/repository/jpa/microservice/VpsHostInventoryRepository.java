package com.marketinghub.repository.jpa.microservice;

import com.marketinghub.microservice.VpsHostInventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Centraliza consultas JPA do inventário editável de hosts VPS. */
public interface VpsHostInventoryRepository extends JpaRepository<VpsHostInventory, Long> {
  /** Busca um host VPS pelo endereço operacional. */
  Optional<VpsHostInventory> findByHost(String host);

  /** Lista hosts VPS cadastrados para apresentação administrativa. */
  List<VpsHostInventory> findAllByOrderByHostAsc();
}
