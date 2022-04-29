package com.anhui.fabricbaascommon.repository;

import com.anhui.fabricbaascommon.entity.CaEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CaRepo extends MongoRepository<CaEntity, String> {
    Optional<CaEntity> findFirstByOrganizationNameIsNotNull();
}
