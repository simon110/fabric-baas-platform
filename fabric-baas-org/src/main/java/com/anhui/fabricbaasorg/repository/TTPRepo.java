package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.TTPEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TTPRepo extends MongoRepository<TTPEntity, String> {

}
