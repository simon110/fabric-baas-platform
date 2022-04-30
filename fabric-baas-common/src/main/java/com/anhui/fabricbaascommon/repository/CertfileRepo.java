package com.anhui.fabricbaascommon.repository;

import com.anhui.fabricbaascommon.entity.CertfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CertfileRepo extends MongoRepository<CertfileEntity, String> {
    Page<CertfileEntity> findAllByCaUsertype(String usertype, Pageable pageable);

    Optional<CertfileEntity> findByCaUsernameAndCaUsertype(String username, String usertype);
}
