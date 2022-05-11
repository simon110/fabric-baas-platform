package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.constant.ApplStatus;
import com.anhui.fabricbaascommon.constant.Authority;
import com.anhui.fabricbaascommon.entity.UserEntity;
import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import com.anhui.fabricbaascommon.exception.OrganizationException;
import com.anhui.fabricbaascommon.exception.RegistrationException;
import com.anhui.fabricbaascommon.repository.UserRepo;
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
import org.springframework.data.domain.Page;
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

    public RegistrationEntity findUnhandledRegistration(String organizationName) {
        List<RegistrationEntity> registrations = registrationRepo.findAllByOrganizationNameAndStatus(organizationName, ApplStatus.UNHANDLED);
        if (!registrations.isEmpty()) {
            assert registrations.size() == 1;
            // 判断最后一次注册申请的状态
            RegistrationEntity registration = registrations.get(0);
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
        if (findUnhandledRegistration(organizationName) != null) {
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
    @Transactional
    public void handleRegistration(String organizationName, boolean isAllowed) throws Exception {
        RegistrationEntity registration = findUnhandledRegistration(organizationName);
        if (registration == null) {
            throw new RegistrationException("该组织不存在未处理的注册申请");
        }

        if (isAllowed) {
            registration.setStatus(ApplStatus.ACCEPTED);
            assert !organizationRepo.existsById(organizationName);
            assert !userRepo.existsById(organizationName);

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

    public Page<RegistrationEntity> queryRegistrations(int status, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return registrationRepo.findAllByStatus(status, pageable);
    }

    public Page<OrganizationEntity> queryOrganizations(String organizationNameKeyword, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        if (StringUtils.isBlank(organizationNameKeyword)) {
            return organizationRepo.findAll(pageable);
        } else {
            return organizationRepo.findAllByNameLike(organizationNameKeyword, pageable);
        }
    }
}
