package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.ChannelEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChannelRepo extends MongoRepository<ChannelEntity, String> {
}
