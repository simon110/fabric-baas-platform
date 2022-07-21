package com.anhui.fabricbaasttp.service;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.annotation.CacheClean;
import com.anhui.fabricbaascommon.constant.ApplStatus;
import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import com.anhui.fabricbaascommon.exception.OrganizationException;
import com.anhui.fabricbaascommon.exception.RegistrationException;
import com.anhui.fabricbaascommon.repository.UserRepo;
import com.anhui.fabricbaascommon.response.PageResult;
import com.anhui.fabricbaascommon.service.MailService;
import com.anhui.fabricbaasttp.entity.OrganizationEntity;
import com.anhui.fabricbaasttp.entity.RegistrationEntity;
import com.anhui.fabricbaasttp.repository.OrganizationRepo;
import com.anhui.fabricbaasttp.repository.RegistrationRepo;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import com.anhui.fabricbaasweb.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@CacheConfig(cacheNames = "ttp")
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

    public void setPassword(String organizationName, String newPassword) throws OrganizationException {
        Optional<UserEntity> optional = userRepo.findById(organizationName);
        if (optional.isEmpty()) {
            throw new OrganizationException("不存在组织：" + organizationName);
        }
        UserEntity organization = optional.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        organization.setPassword(encodedPassword);
        log.info("正在修改系统管理员密码...");
        userRepo.save(organization);
    }

    public UserDetails findOrganizationUserDetailsOrThrowEx(String organizationName) throws OrganizationException {
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(organizationName);
        if (userDetails == null) {
            throw new OrganizationException("未找到相应的组织，请检查是否已注册成功");
        }
        return userDetails;
    }

    /**
     * 1. 根据组织名查询UserDetails
     * 2. 使用PasswordEncoder检查密码是否正常
     * 3. 如果密码正确则生成对应的Token并返回
     */
    public String login(String organizationName, String password) throws Exception {
        UserDetails userDetails = findOrganizationUserDetailsOrThrowEx(organizationName);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new IncorrectPasswordException("密码错误");
        }
        String token = jwtUtils.generateToken(userDetails);
        log.info("用户{}登录成功：{}", userDetails.getUsername(), token);
        return token;
    }

    public Optional<RegistrationEntity> findUnhandledRegistration(String organizationName) {
        List<RegistrationEntity> registrations = registrationRepo.findAllByOrganizationNameAndStatus(organizationName, ApplStatus.UNHANDLED);
        if (!registrations.isEmpty()) {
            Assert.isTrue(registrations.size() == 1);
            // 判断最后一次注册申请的状态
            RegistrationEntity registration = registrations.get(0);
            if (registration.getStatus() == ApplStatus.UNHANDLED) {
                return Optional.of(registration);
            }
        }
        return Optional.empty();
    }

    /**
     * 提交注册申请主要包括以下流程：
     * 1. 检查组织名称是否已经被注册
     * 2. 检查组织名称是否还包含未处理的申请
     * 3. 将注册申请保存至数据库
     */
    @CacheClean(patterns = "'OrganizationService:queryRegistrations:*'")
    @Transactional
    public void applyRegistration(
            String organizationName,
            String password,
            String description,
            String email,
            String apiServer)
            throws Exception {
        if (userRepo.existsById(organizationName)) {
            throw new DuplicatedOperationException("该组织已经成功注册，请勿重复申请");
        }
        if (findUnhandledRegistration(organizationName).isPresent()) {
            throw new DuplicatedOperationException("该组织的注册申请仍在审核当中，请勿重复提交");
        }

        RegistrationEntity registration = new RegistrationEntity();
        registration.setOrganizationName(organizationName);
        registration.setApiServer(apiServer);
        registration.setDescription(description);
        registration.setEmail(email);
        registration.setPassword(passwordEncoder.encode(password));
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
    @CacheClean(patterns = {
            "'OrganizationService:queryOrganizations:*'",
            "'OrganizationService:queryRegistrations:*'",
    })
    @Transactional
    public void handleRegistration(String organizationName, boolean isAllowed) throws Exception {
        Optional<RegistrationEntity> registrationOptional = findUnhandledRegistration(organizationName);
        if (registrationOptional.isEmpty()) {
            throw new RegistrationException("该组织不存在未处理的注册申请");
        }
        RegistrationEntity registration = registrationOptional.get();

        if (isAllowed) {
            registration.setStatus(ApplStatus.ACCEPTED);
            Assert.isFalse(organizationRepo.existsById(organizationName));
            Assert.isFalse(userRepo.existsById(organizationName));

            OrganizationEntity organization = new OrganizationEntity();
            organization.setName(registration.getOrganizationName());
            organization.setEmail(registration.getEmail());
            organization.setApiServer(registration.getApiServer());
            organizationRepo.save(organization);
            log.info("生成组织信息：" + organization);

            UserEntity user = new UserEntity();
            user.setAuthorities(Collections.singletonList(Authority.USER));
            user.setPassword(registration.getPassword());
            user.setUsername(registration.getOrganizationName());
            userRepo.save(user);
            log.info("生成账户信息：" + user);
        } else {
            registration.setStatus(ApplStatus.REJECTED);
        }
        registrationRepo.save(registration);
        log.info("更新注册信息：" + registration);

        try {
            String messageText = isAllowed ? "组织申请加入Fabric BaaS平台的申请已通过" : "组织申请加入Fabric BaaS平台的申请被拒绝";
            mailService.send(registration.getEmail(), "Fabric BaaS Platform 注册申请处理结果", messageText);
        } catch (MailException e) {
            log.warn("发送邮件失败：" + registration.getEmail());
        }
    }

    @Cacheable(keyGenerator = "redisKeyGenerator")
    public PageResult<RegistrationEntity> queryRegistrations(int status, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return new PageResult<>(registrationRepo.findAllByStatus(status, pageable));
    }

    @Cacheable(keyGenerator = "redisKeyGenerator")
    public PageResult<OrganizationEntity> queryOrganizations(String organizationNameKeyword, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        if (StringUtils.isBlank(organizationNameKeyword)) {
            return new PageResult<>(organizationRepo.findAll(pageable));
        } else {
            return new PageResult<>(organizationRepo.findAllByNameLike(organizationNameKeyword, pageable));
        }
    }
}
