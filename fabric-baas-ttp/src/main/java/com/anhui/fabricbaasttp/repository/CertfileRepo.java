package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.CertfileEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CertfileRepo extends MongoRepository<CertfileEntity, String> {
}
