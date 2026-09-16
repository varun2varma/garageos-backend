package com.garageos.modules.identity.mapper;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in that an employee's garage-scoped business code
 * (User.employeeCode, mirrored from GarageMembership.employeeCode -
 * see GarageMembershipServiceImplTest) survives a profile/role update.
 * UserMapper.updateEntity already ignores employeeCode by design; this
 * test exists so that guarantee can't silently regress.
 */
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void updateEntity_doesNotChangeEmployeeCode() {

        User user = new User();
        user.setEmployeeCode("G006-EMP001");
        user.setFirstName("Old");
        user.setStatus(UserStatus.ACTIVE);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");

        mapper.updateEntity(request, user);

        assertThat(user.getEmployeeCode()).isEqualTo("G006-EMP001");
        assertThat(user.getFirstName()).isEqualTo("New");
    }
}
