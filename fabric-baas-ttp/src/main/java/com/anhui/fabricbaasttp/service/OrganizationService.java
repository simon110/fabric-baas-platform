package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.constant.ApplStatus;
import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import com.anhui.fabricbaascommon.exception.OrganizationException;
import com.anhui.fabricbaascommon.exception.RegistrationException;
import com.anhui.fabricbaascommon.service.MailService;
import com.anhui.fabricbaasttp.entity.OrganizationEntity;
import com.anhui.fabricbaasttp.entity.RegistrationEntity;
import com.anhui.fabricbaasttp.entity.UserEntity;
import com.anhui.fabricbaasttp.repository.OrganizationRepo;
import com.anhui.fabricbaasttp.repository.RegistrationRepo;
import com.anhui.fabricbaasttp.repository.UserRepo;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasweb.response.LoginResult;
import com.anhui.fabricbaasweb.response.PaginationQueryResult;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import com.anhui.fabricbaasweb.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class OrganizationService {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RegistrationRepo registrationRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private OrganizationRepo organizationRepo;
    @Autowired
    private MailService mailService;

    /**
     * 1. 根据组织名查询UserDetails
     * 2. 使用PasswordEncoder检查密码是否正常
     * 3. 如果密码正确则生成对应的Token并返回
     */
    public LoginResult login(LoginRequest request) throws Exception {
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(request.getOrganizationName());
        if (userDetails == null) {
            throw new OrganizationException("未找到相应的组织，请检查是否已注册成功");
        }
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new IncorrectPasswordException("密码错误");
        }
        String token = jwtUtils.generateToken(userDetails);
        log.info(String.format("用户%s登录成功：%s", userDetails.getUsername(), token));
        return new LoginResult(token);
    }

    public RegistrationEntity findUnhandledRegistration(String organizationName) {
        List<RegistrationEntity> registrations = registrationRepo.findAllByOrganizationName(organizationName);
        if (!registrations.isEmpty()) {
            // 判断最后一次注册申请的状态
            RegistrationEntity registration = registrations.get(registrations.size() - 1);
            if (registration.getStatus() == ApplStatus.UNHANDLED) {
                return registration;
            }
        }
        return null;
    }

    /**
     * 提交注册申请主要包括以下流程：
     * 1. 检查组织名称是否已经被注册
     * 2. 检查组织名称是否还包含未处理的申请
     * 3. 将注册申请保存至数据库
     */
    public void applyRegistration(RegistrationApplyRequest request) throws Exception {
        if (userRepo.existsById(request.getOrganizationName())) {
            throw new DuplicatedOperationException("该组织已经成功注册，请勿重复申请");
        }
        if (findUnhandledRegistration(request.getOrganizationName()) != null) {
            throw new DuplicatedOperationException("该组织的注册申请仍在审核当中，请勿重复提交");
        }

        RegistrationEntity registration = new RegistrationEntity();
        registration.setOrganizationName(request.getOrganizationName());
        registration.setApiServer(request.getApiServer());
        registration.setDescription(request.getDescription());
        registration.setEmail(request.getEmail());
        registration.setPassword(passwordEncoder.encode(request.getPassword()));
        registration.setStatus(ApplStatus.UNHANDLED);
        registration.setTimestamp(System.currentTimeMillis());
        log.info("接收注册申请：" + registration);
        registrationRepo.save(registration);
    }

    /**
     * 主要执行三个任务：
     * 1. 根据处理结果向相应组织发送邮件通知
     * 2. 更新数据库中的申请状态
     * 3. 增加相应的组织和用户定义（如果需要）
     */
    public void handleRegistration(RegistrationHandleRequest request) throws Exception {
        RegistrationEntity registration = findUnhandledRegistration(request.getOrganizationName());
        if (registration == null) {
            throw new RegistrationException("该组织不存在未处理的注册申请");
        }

        if (request.isAllowed()) {
            mailService.send(registration.getEmail(), "注册申请处理结果", "通过");

            registration.setStatus(ApplStatus.ACCEPTED);
            assert !organizationRepo.existsById(request.getOrganizationName());
            assert !userRepo.existsById(request.getOrganizationName());

            OrganizationEntity organization = new OrganizationEntity();
            organization.setName(registration.getOrganizationName());
            organization.setEmail(registration.getEmail());
            organization.setApiServer(registration.getApiServer());

            UserEntity user = new UserEntity();
            user.setAuthorities(Collections.singletonList(Authority.USER));
            user.setPassword(registration.getPassword());
            user.setUsername(registration.getOrganizationName());

            organizationRepo.save(organization);
            userRepo.save(user);
            log.info("生成组织信息：" + organization);
            log.info("生成账户信息：" + user);
        } else {
            mailService.send(registration.getEmail(), "注册申请处理结果", "拒绝");
            registration.setStatus(ApplStatus.REJECTED);
        }
        registrationRepo.save(registration);
        log.info("更新注册信息：" + registration);
    }

    public PaginationQueryResult<RegistrationEntity> queryRegistrations(RegistrationQueryRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize(), sort);
        Page<RegistrationEntity> page = registrationRepo.findAllByStatus(request.getStatus(), pageable);

        PaginationQueryResult<RegistrationEntity> result = new PaginationQueryResult<>();
        result.setItems(page.toList());
        result.setTotalPages(page.getTotalPages());
        return result;
    }

    public PaginationQueryResult<OrganizationEntity> queryOrganizations(OrganizationQueryRequest request) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize(), sort);
        Page<OrganizationEntity> page;
        if (StringUtils.isBlank(request.getOrganizationNameKeyword())) {
            page = organizationRepo.findAll(pageable);
        } else {
            page = organizationRepo.findAllByNameLike(request.getOrganizationNameKeyword(), pageable);
        }
        PaginationQueryResult<OrganizationEntity> result = new PaginationQueryResult<>();
        result.setItems(page.toList());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
}
