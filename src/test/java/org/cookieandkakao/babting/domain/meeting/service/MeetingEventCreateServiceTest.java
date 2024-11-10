package org.cookieandkakao.babting.domain.meeting.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cookieandkakao.babting.domain.calendar.dto.request.EventCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.request.TimeCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.entity.Event;
import org.cookieandkakao.babting.domain.calendar.entity.Time;
import org.cookieandkakao.babting.domain.calendar.exception.EventCreationException;
import org.cookieandkakao.babting.domain.calendar.service.EventService;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarService;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingEventCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingTimeCreateRequest;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.meeting.exception.meeting.MeetingNotFoundException;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingEventRepository;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.exception.MemberNotFoundException;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MeetingEventCreateServiceTest {

    @Mock
    private TalkCalendarService talkCalendarService;
    @Mock
    private EventService eventService;
    @Mock
    private MemberService memberService;
    @Mock
    private MeetingService meetingService;
    @Mock
    private MeetingEventRepository meetingEventRepository;

    @InjectMocks
    private MeetingEventCreateService meetingEventCreateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_MEETING_ID = 2L;
    private static final Long INVALID_MEMBER_ID = -1L;
    private static final Long INVALID_MEETING_ID = -2L;

    @Nested
    class 캘린더에_일정_추가_테스트 {

        @Test
        void 성공() {
            // Given
            MeetingEventCreateRequest meetingEventCreateRequest = mock(MeetingEventCreateRequest.class);
            MeetingTimeCreateRequest meetingTimeCreateRequest = mock(MeetingTimeCreateRequest.class);
            TimeCreateRequest timeCreateRequest = mock(TimeCreateRequest.class);
            EventCreateResponse expectedResponse = mock(EventCreateResponse.class);

            // Mocking
            given(meetingEventCreateRequest.title()).willReturn("Meeting Title");
            given(meetingEventCreateRequest.time()).willReturn(meetingTimeCreateRequest);
            given(meetingTimeCreateRequest.toTimeCreateRequest()).willReturn(timeCreateRequest);
            given(talkCalendarService.createEvent(any(EventCreateRequest.class), eq(VALID_MEMBER_ID)))
                .willReturn(expectedResponse);

            // When
            EventCreateResponse result = meetingEventCreateService.addMeetingEvent(VALID_MEMBER_ID, meetingEventCreateRequest);

            // Then
            assertEquals(expectedResponse, result);
            assertEquals(result.eventId(), expectedResponse.eventId());
            verify(talkCalendarService).createEvent(any(EventCreateRequest.class), eq(VALID_MEMBER_ID));
        }

        @Test
        void 실패_EventCreationException예외() {
            // Given
            MeetingEventCreateRequest meetingEventCreateRequest = mock(MeetingEventCreateRequest.class);
            MeetingTimeCreateRequest meetingTimeCreateRequest = mock(MeetingTimeCreateRequest.class);
            TimeCreateRequest timeCreateRequest = mock(TimeCreateRequest.class);

            // Mocking
            given(meetingEventCreateRequest.title()).willReturn("Meeting Title");
            given(meetingEventCreateRequest.time()).willReturn(meetingTimeCreateRequest);
            given(meetingTimeCreateRequest.toTimeCreateRequest()).willReturn(timeCreateRequest);
            given(talkCalendarService.createEvent(any(EventCreateRequest.class), eq(VALID_MEMBER_ID)))
                .willThrow(new EventCreationException("Event 생성 중 오류 발생"));

            // When
            Exception e = assertThrows(EventCreationException.class,
                () -> meetingEventCreateService.addMeetingEvent(VALID_MEMBER_ID, meetingEventCreateRequest));

            // Then
            assertEquals(e.getClass(), EventCreationException.class);
            assertEquals(e.getMessage(),"Event 생성 중 오류 발생");
            verify(talkCalendarService).createEvent(any(EventCreateRequest.class), eq(VALID_MEMBER_ID));
        }
    }

    @Nested
    class 피하고_싶은_시간_일정_저장_테스트 {

        @Test
        void 성공_피하고_싶은_시간_없는_경우() {
            // Given
            List<MeetingTimeCreateRequest> emptyAvoidTimeRequests = List.of();

            // When & Then
            assertDoesNotThrow(() -> meetingEventCreateService.saveMeetingAvoidTime(VALID_MEMBER_ID, VALID_MEETING_ID, emptyAvoidTimeRequests));
        }

        @Test
        void 성공_피하고_싶은_시간_있는_경우() {
            // Given
            MeetingTimeCreateRequest avoidTimeCreateRequest = mock(MeetingTimeCreateRequest.class);
            TimeCreateRequest timeCreateRequest = mock(TimeCreateRequest.class);
            Time time = mock(Time.class);
            Event avoidEvent = mock(Event.class);
            List<MeetingTimeCreateRequest> avoidTimeCreateRequests = List.of(avoidTimeCreateRequest);
            Member member = mock(Member.class);
            Meeting meeting = mock(Meeting.class);
            MemberMeeting memberMeeting = mock(MemberMeeting.class);

            // Mocking
            given(memberService.findMember(VALID_MEMBER_ID)).willReturn(member);
            given(meetingService.findMeeting(VALID_MEETING_ID)).willReturn(meeting);
            given(meetingService.findMemberMeeting(member, meeting)).willReturn(memberMeeting);
            given(avoidTimeCreateRequest.toTimeCreateRequest()).willReturn(timeCreateRequest);
            given(timeCreateRequest.toEntity()).willReturn(time);
            given(eventService.saveAvoidTimeEvent(time)).willReturn(avoidEvent);

            // When
            assertDoesNotThrow(() -> meetingEventCreateService.saveMeetingAvoidTime(VALID_MEMBER_ID, VALID_MEETING_ID, avoidTimeCreateRequests));

            // Then
            verify(eventService).saveAvoidTimeEvent(time);
            verify(meetingEventRepository).saveAll(anyList());
        }

        @Test
        void 실패_잘못된_멤버ID인_경우() {
            // Given
            List<MeetingTimeCreateRequest> avoidTimeCreateRequests = List.of(mock(MeetingTimeCreateRequest.class));

            // Mocking
            given(memberService.findMember(INVALID_MEMBER_ID)).willThrow(new MemberNotFoundException("해당 사용자가 존재하지 않습니다."));

            // When
            Exception e = assertThrows(MemberNotFoundException.class,
                () -> meetingEventCreateService.saveMeetingAvoidTime(INVALID_MEMBER_ID, VALID_MEETING_ID, avoidTimeCreateRequests));

            // Then
            assertEquals(e.getClass(), MemberNotFoundException.class);
            assertEquals(e.getMessage(),"해당 사용자가 존재하지 않습니다.");
            verify(memberService).findMember(INVALID_MEMBER_ID);
        }

        @Test
        void 실패_잘못된_모임ID인_경우() {
            // Given
            List<MeetingTimeCreateRequest> avoidTimeCreateRequests = List.of(mock(MeetingTimeCreateRequest.class));
            Member member = mock(Member.class);

            // Mocking
            given(memberService.findMember(VALID_MEMBER_ID)).willReturn(member);
            given(meetingService.findMeeting(INVALID_MEETING_ID)).willThrow(new MeetingNotFoundException("해당 모임이 존재하지 않습니다."));

            // When
            Exception e = assertThrows(MeetingNotFoundException.class,
                () -> meetingEventCreateService.saveMeetingAvoidTime(VALID_MEMBER_ID, INVALID_MEETING_ID, avoidTimeCreateRequests));

            // Then
            assertEquals(e.getClass(), MeetingNotFoundException.class);
            assertEquals(e.getMessage(),"해당 모임이 존재하지 않습니다.");
            verify(memberService).findMember(VALID_MEMBER_ID);
            verify(meetingService).findMeeting(INVALID_MEETING_ID);
        }
    }
}