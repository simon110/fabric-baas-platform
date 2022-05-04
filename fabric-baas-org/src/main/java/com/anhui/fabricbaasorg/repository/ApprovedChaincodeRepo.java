package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApprovedChaincodeRepo extends MongoRepository<ApprovedChaincodeEntity, String> {
    List<ApprovedChaincodeEntity> findAllByChannelNameAndCommitted(String channelName,boolean isCommitted);

    List<ApprovedChaincodeEntity> findAllByChannelNameAndNameAndSequenceAndVersion(String channelName, String chaincodeName, int sequence, String version);

    Page<ApprovedChaincodeEntity> findAllByCommitted(boolean isCommitted, Pageable pageable);
}
