package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InstalledChaincodeRepo extends MongoRepository<InstalledChaincodeEntity, String> {
}
