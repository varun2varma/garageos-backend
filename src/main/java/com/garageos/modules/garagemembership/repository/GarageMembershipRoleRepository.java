package com.garageos.modules.garagemembership.repository;

import com.garageos.modules.garagemembership.entity.GarageMembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarageMembershipRoleRepository
        extends JpaRepository<GarageMembershipRole, Long> {

    List<GarageMembershipRole> findByMembership_Id(Long membershipId);

    void deleteByMembership_Id(Long membershipId);

}