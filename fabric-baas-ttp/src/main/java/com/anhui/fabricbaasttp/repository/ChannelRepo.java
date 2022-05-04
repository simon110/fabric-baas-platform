package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.ChannelEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChannelRepo extends MongoRepository<ChannelEntity, String> {
    List<ChannelEntity> findAllByNetworkName(String networkName);

    List<ChannelEntity> findAllByOrganizationNamesIsContaining(String organizationName);
}
