package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InstalledChaincodeRepo extends MongoRepository<InstalledChaincodeEntity, String> {
    List<InstalledChaincodeEntity> findAllByPeerName(String peerName);
}
