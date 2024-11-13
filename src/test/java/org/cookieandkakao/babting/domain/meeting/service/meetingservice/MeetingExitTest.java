package org.cookieandkakao.babting.domain.meeting.service.meetingservice;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.junit.jupiter.api.Test;

class MeetingExitTest extends MeetingServiceTest {

    @Test
    void 모임장_탈퇴_성공() {
        // given
        Member member = mock(Member.class);
        Meeting meeting = mock(Meeting.class);
        MemberMeeting memberMeeting = new MemberMeeting(member, meeting, true);

        when(memberService.findMember(1L)).thenReturn(member);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(memberMeetingRepository.findByMemberAndMeeting(member, meeting)).thenReturn(Optional.of(memberMeeting));

        // when
        meetingService.exitMeeting(1L, 1L);

        // then
        verify(memberMeetingRepository).deleteAllByMeeting(meeting);
        verify(meetingRepository).delete(meeting);
    }
    // 모임 참여자가 탈퇴할 경우 모임만 탈퇴
    // 가입하지 않은 모임 탈퇴 불가
    // 없는 모임 탈퇴 불가
}