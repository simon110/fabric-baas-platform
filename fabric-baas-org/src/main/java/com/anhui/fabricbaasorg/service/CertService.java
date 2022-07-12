package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.response.PageResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@CacheConfig(cacheNames = "org")
public class CertService {
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private CertfileRepo certfileRepo;

    public void generate(CertfileEntity certfile) throws Exception {
        caClientService.register(certfile.getCaUsername(), certfile.getCaPassword(), certfile.getCaUsertype());
    }

    public PageResult<CertfileEntity> query(String usertype, int page, int pageSize) throws Exception {
        Sort sort = Sort.by(Sort.Direction.ASC, "caUsername");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        Page<CertfileEntity> result = certfileRepo.findAllByCaUsertype(usertype, pageable);
        result.getContent().forEach(item -> item.setCaPassword("Not Available"));
        return new PageResult<>(result);
    }
}
