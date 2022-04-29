package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaasorg.request.CertGenerateRequest;
import com.anhui.fabricbaasorg.request.CertQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CertService {
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private CertfileRepo certfileRepo;

    public void generate(CertGenerateRequest req) throws Exception {
        caClientService.register(req.getCaUsername(), req.getCaPassword(), req.getCaUsertype());
    }

    public PaginationQueryResult<CertfileEntity> query(CertQueryRequest request) throws Exception {
        Sort sort = Sort.by(Sort.Direction.ASC, "caUsername");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize(), sort);
        Page<CertfileEntity> certfileEntities = certfileRepo.findAllByCaUsertype(request.getCertType(), pageable);

        PaginationQueryResult<CertfileEntity> result = new PaginationQueryResult<>();
        result.setItems(certfileEntities.getContent());
        result.setTotalPages(certfileEntities.getTotalPages());
        return result;
    }
}
