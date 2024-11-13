package org.cookieandkakao.babting.domain.meeting.service.meetingservice;


import static org.mockito.Mockito.*;

import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.junit.jupiter.api.Test;


class MeetingCreateTest extends MeetingServiceTest{

    @Test
    void 모임_생성_성공() {
        //given
        Meeting meeting = mock(Meeting.class);

        when(meetingRepository.save(meeting)).thenReturn(meetingCreateRequest.toEntity());
        //when
        meetingService.createMeeting(1L, meetingCreateRequest);
        //then
        verify(meetingRepository).save(meeting);
    }
}