package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.CommittedChaincodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommittedChaincodeRepo extends MongoRepository<CommittedChaincodeEntity, String> {
    List<CommittedChaincodeEntity> findAllByChannelName(String channelName);
}
