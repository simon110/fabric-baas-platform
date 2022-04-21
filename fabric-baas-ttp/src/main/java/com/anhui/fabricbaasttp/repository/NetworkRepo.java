package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.NetworkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetworkRepo extends MongoRepository<NetworkEntity, String> {
    Page<NetworkEntity> findAllByNameLike(String keyword, Pageable pageable);
}
