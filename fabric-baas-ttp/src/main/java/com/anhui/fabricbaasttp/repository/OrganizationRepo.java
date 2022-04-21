package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.OrganizationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrganizationRepo extends MongoRepository<OrganizationEntity, String> {
    Page<OrganizationEntity> findAllByNameLike(String name, Pageable pageable);

    int countAllByNameLike(String name);
}
