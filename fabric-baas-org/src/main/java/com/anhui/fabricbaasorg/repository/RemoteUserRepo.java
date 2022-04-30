package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RemoteUserRepo extends MongoRepository<RemoteUserEntity, String> {
}
