package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.RegistrationEntity;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RegistrationRepo extends MongoRepository<RegistrationEntity, ObjectId> {
    List<RegistrationEntity> findAllByOrganizationName(String organizationName);

    Page<RegistrationEntity> findAllByStatus(int status, Pageable pageable);

    int countAllByStatus(int status);
}
