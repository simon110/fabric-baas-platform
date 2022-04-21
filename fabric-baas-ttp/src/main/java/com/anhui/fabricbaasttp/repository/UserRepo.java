package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepo extends MongoRepository<UserEntity, String> {
}
