package org.cookieandkakao.babting.domain.meeting.service.meetingservice;


import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import org.cookieandkakao.babting.domain.meeting.dto.request.LocationCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingCreateRequest;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.repository.LocationRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MemberMeetingRepository;
import org.cookieandkakao.babting.domain.meeting.service.MeetingService;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MeetingCreateTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MemberMeetingRepository memberMeetingRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private MemberService memberService;
    @InjectMocks
    private MeetingService meetingService;

    //모임 생성
    @Test
    void 모임_생성_성공() {
        //given
        LocationCreateRequest baseLocation = new LocationCreateRequest("전대", "11", 1.1, 1.1);
        MeetingCreateRequest meetingCreateRequest = new MeetingCreateRequest(baseLocation, "밥팅",
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 3, LocalTime.of(14, 0),
            LocalTime.of(17, 0));

        when(meetingRepository.save(any(Meeting.class))).thenReturn(meetingCreateRequest.toEntity());

        //when
        meetingService.createMeeting(1L, meetingCreateRequest);
        //then
        verify(meetingRepository).save(any(Meeting.class));
    }
}