package com.anhui.fabricbaasorg.repository;

import com.anhui.fabricbaasorg.entity.OrdererEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrdererRepo extends MongoRepository<OrdererEntity, String> {
    List<OrdererEntity> findAllByKubeNodePort(int kubeNodePort);
}
