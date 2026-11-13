package com.indoornav.backend.repository;

import com.indoornav.backend.entity.ConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionTypeRepository extends JpaRepository<ConnectionType, String> {
}
