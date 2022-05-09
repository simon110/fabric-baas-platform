package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.ChannelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChannelRepo extends MongoRepository<ChannelEntity, String> {
    List<ChannelEntity> findAllByNetworkName(String networkName);

    Page<ChannelEntity> findAllByOrganizationNamesIsContaining(String organizationName, Pageable pageable);
}
