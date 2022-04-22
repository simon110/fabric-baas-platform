package com.anhui.fabricbaascommon.repository;

import com.anhui.fabricbaascommon.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepo extends MongoRepository<UserEntity, String> {
}
