package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.CommittedChaincodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommittedChaincodeRepo extends MongoRepository<CommittedChaincodeEntity, String> {
}
