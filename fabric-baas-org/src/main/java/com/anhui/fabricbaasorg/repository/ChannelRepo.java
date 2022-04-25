package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.ChannelEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChannelRepo extends MongoRepository<ChannelEntity, String> {
}

