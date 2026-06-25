package com.zanchi.zanchi_backend.domain.member.policy;

import com.zanchi.zanchi_backend.domain.member.Member;

public interface MemberDeletionPolicy {

    boolean canDelete(Member requester, Member target);
}
