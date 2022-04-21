package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.TTPEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TTPRepo extends MongoRepository<TTPEntity, String> {
    Optional<TTPEntity> findFirstByNameIsNotNull();
}
