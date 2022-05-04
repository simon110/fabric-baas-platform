package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApprovedChaincodeRepo extends MongoRepository<ApprovedChaincodeEntity, String> {
    List<ApprovedChaincodeEntity> findAllByChannelName(String channelName);
}
