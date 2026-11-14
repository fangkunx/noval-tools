package com.noval.gateway.infrastructure.persistence.repository;

import com.noval.gateway.infrastructure.persistence.entity.CapabilityRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapabilityRouteRepository extends JpaRepository<CapabilityRouteEntity, Long> {

    List<CapabilityRouteEntity> findByCapabilityAndEnabledTrueOrderByPriorityAsc(String capability);

    List<CapabilityRouteEntity> findByEnabledTrueOrderByCapabilityAscPriorityAsc();
}
