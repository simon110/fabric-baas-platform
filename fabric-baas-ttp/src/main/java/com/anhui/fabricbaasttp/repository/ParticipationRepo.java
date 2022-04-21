package com.anhui.fabricbaasttp.repository;

import com.anhui.fabricbaasttp.entity.ParticipationEntity;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepo extends MongoRepository<ParticipationEntity, ObjectId> {
    List<ParticipationEntity> findAllByNetworkNameAndOrganizationNameAndStatus(String networkName, String organizationName, int status);

    List<ParticipationEntity> findAllByOrganizationNameLikeAndStatus(String organizationName, int status);

    List<ParticipationEntity> findAllByNetworkNameLikeAndOrganizationNameLikeAndStatus(String networkName, String organizationName, int status);

    Page<ParticipationEntity> findAllByNetworkNameAndStatus(String networkName, int status, Pageable pageable);

    Optional<ParticipationEntity> findFirstByNetworkNameAndOrganizationNameAndStatus(String networkName, String organizationName, int status);
}

