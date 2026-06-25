package com.zanchi.zanchi_backend.domain.member;

import com.zanchi.zanchi_backend.domain.member.dto.ChangeNameResponse;
import com.zanchi.zanchi_backend.domain.member.policy.MemberDeletionPolicy;
import com.zanchi.zanchi_backend.web.member.form.MemberForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberDeletionPolicy memberDeletionPolicy;

    public void signup(MemberForm form) {
        if (memberRepository.findByLoginId(form.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 로그인 ID입니다.");
        }

        Member member = Member.builder()
                .name(form.getName())
                .loginId(form.getLoginId())
                .password(form.getPassword()) //지인아 여기 암호화 해야해 나중에 시간나면 하자
                .build();

        memberRepository.save(member);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional
    public void deleteById(Long targetMemberId, Long requesterMemberId) {
        Member requester = memberRepository.findById(requesterMemberId)
                .orElseThrow(() -> new IllegalArgumentException("요청 회원을 찾을 수 없습니다."));
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!memberDeletionPolicy.canDelete(requester, target)) {
            throw new AccessDeniedException("회원 삭제 권한이 없습니다.");
        }

        memberRepository.delete(target);
    }

    @Transactional
    public ChangeNameResponse changeName(Long memberId, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.length() > 30) {
            throw new IllegalArgumentException("invalid name");
        }

        // (선택) 중복 금지 시
        // if (memberRepository.existsByNameIgnoreCase(name)) {
        //     throw new IllegalStateException("name already in use");
        // }

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("member not found"));
        m.setName(name); // JPA dirty checking으로 업데이트
        return new ChangeNameResponse(m.getId(), m.getName());
    }
}
