package com.zanchi.zanchi_backend.domain.member.policy;

import com.zanchi.zanchi_backend.domain.member.Member;
import org.springframework.stereotype.Component;

@Component
public class SelfOrAdminMemberDeletionPolicy implements MemberDeletionPolicy {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    @Override
    public boolean canDelete(Member requester, Member target) {
        if (requester == null || target == null) {
            return false;
        }

        return requester.getId().equals(target.getId()) || ADMIN_ROLE.equals(requester.getRole());
    }
}
