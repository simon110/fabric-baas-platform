package com.anhui.fabricbaascommon.repository;

import com.anhui.fabricbaascommon.entity.CertfileEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CertfileRepo extends MongoRepository<CertfileEntity, String> {
}
