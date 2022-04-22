package com.anhui.fabricbaascommon.repository;

import com.anhui.fabricbaascommon.entity.CAEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CARepo extends MongoRepository<CAEntity, String> {
    Optional<CAEntity> findFirstByNameIsNotNull();
}
