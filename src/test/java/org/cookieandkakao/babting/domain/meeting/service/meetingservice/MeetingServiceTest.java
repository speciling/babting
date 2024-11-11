package org.cookieandkakao.babting.domain.meeting.service.meetingservice;

import org.cookieandkakao.babting.domain.meeting.repository.LocationRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MemberMeetingRepository;
import org.cookieandkakao.babting.domain.meeting.service.MeetingService;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class MeetingServiceTest {
    @Mock
    protected MeetingRepository meetingRepository;
    @Mock
    protected MemberMeetingRepository memberMeetingRepository;
    @Mock
    protected LocationRepository locationRepository;
    @Mock
    protected MemberService memberService;
    @InjectMocks
    protected MeetingService meetingService;
}
