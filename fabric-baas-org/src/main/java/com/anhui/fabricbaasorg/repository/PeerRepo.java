package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.PeerEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PeerRepo extends MongoRepository<PeerEntity, String> {
    List<PeerEntity> findAllByKubeNodePortEqualsOrKubeEventNodePortEquals(int kubeNodePort, int kubeEventNodePort);
}

