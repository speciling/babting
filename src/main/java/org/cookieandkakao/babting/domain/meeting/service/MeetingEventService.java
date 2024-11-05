package org.cookieandkakao.babting.domain.meeting.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.cookieandkakao.babting.domain.calendar.dto.request.EventCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.request.TimeCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.TimeGetResponse;
import org.cookieandkakao.babting.domain.calendar.entity.Event;
import org.cookieandkakao.babting.domain.calendar.repository.EventRepository;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarService;
import org.cookieandkakao.babting.domain.meeting.dto.request.ConfirmMeetingGetRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingEventCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingTimeCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.response.TimeAvailableGetResponse;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MeetingEvent;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.meeting.entity.TimeZone;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingEventRepository;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.springframework.stereotype.Service;

@Transactional
@Service
public class MeetingEventService {

    private final MemberService memberService;
    private final TalkCalendarService talkCalendarService;
    private final MeetingService meetingService;
    private static final List<Integer> DEFAULT_REMINDER_TIMES = List.of(15, 30);
    private final EventRepository eventRepository;
    private final MeetingEventRepository meetingEventRepository;
    private final MeetingTimeCalculationService meetingTimeCalculationService;

    public MeetingEventService(MemberService memberService, TalkCalendarService talkCalendarService,
        MeetingService meetingService, EventRepository eventRepository,
        MeetingEventRepository meetingEventRepository,
        MeetingTimeCalculationService meetingTimeCalculationService) {
        this.memberService = memberService;
        this.talkCalendarService = talkCalendarService;
        this.meetingService = meetingService;
        this.eventRepository = eventRepository;
        this.meetingEventRepository = meetingEventRepository;
        this.meetingTimeCalculationService = meetingTimeCalculationService;
    }

    // 모임 확정되면 일정 생성
    public void confirmMeeting(Long memberId, Long meetingId,
        ConfirmMeetingGetRequest confirmMeetingGetRequest) {
        Member member = memberService.findMember(memberId);
        Meeting meeting = meetingService.findMeeting(meetingId);
        validateHostPermission(member, meeting);
        validateMeetingConfirmation(meeting);

        meeting.confirmDateTime(confirmMeetingGetRequest.confirmDateTime());

        MeetingTimeCreateRequest meetingTimeCreateRequest = createMeetingTimeRequest(meeting);
        MeetingEventCreateRequest meetingEventCreateRequest = createMeetingEventRequest(meeting,
            meetingTimeCreateRequest);

        List<Long> memberIds = meetingService.getMemberIdInMeetingId(meetingId);

        for (Long currentMemberId : memberIds) {
            addMeetingEvent(currentMemberId, meetingEventCreateRequest);
        }
    }

    private void validateHostPermission(Member member, Meeting meeting) {
        MemberMeeting memberMeeting = meetingService.findMemberMeeting(member, meeting);
        if (!memberMeeting.isHost()) {
            throw new IllegalStateException("권한이 없습니다.");
        }
    }

    private void validateMeetingConfirmation(Meeting meeting) {
        if (meeting.getConfirmDateTime() != null) {
            throw new IllegalStateException("이미 모임 시간이 확정되었습니다.");
        }
    }

    private MeetingTimeCreateRequest createMeetingTimeRequest(Meeting meeting) {
        ZonedDateTime startDateTime = meeting.getConfirmDateTime().atZone(ZoneId.of(TimeZone.SEOUL.getArea()));
        ZonedDateTime endDateTime = startDateTime.plusMinutes(meeting.getDurationTime());
        boolean allDay = false;
        return new MeetingTimeCreateRequest(startDateTime.toString(), endDateTime.toString(),
            TimeZone.SEOUL.getArea(), allDay);
    }

    private MeetingEventCreateRequest createMeetingEventRequest(Meeting meeting,
        MeetingTimeCreateRequest meetingTimeCreateRequest) {
        return new MeetingEventCreateRequest(meeting.getTitle(), meetingTimeCreateRequest,
            DEFAULT_REMINDER_TIMES);
    }

    // 일정 생성 후 캘린더에 일정 추가
    public EventCreateResponse addMeetingEvent(Long memberId,
        MeetingEventCreateRequest meetingEventCreateRequest) {
        EventCreateRequest eventCreateRequest = convertToEventCreateRequest(
            meetingEventCreateRequest);
        return talkCalendarService.createEvent(eventCreateRequest, memberId);
    }

    private EventCreateRequest convertToEventCreateRequest(
        MeetingEventCreateRequest meetingEventCreateRequest) {
        return new EventCreateRequest(
            meetingEventCreateRequest.title(),
            meetingEventCreateRequest.time().toTimeCreateRequest(), null,
            meetingEventCreateRequest.reminders(), null);
    }

    public TimeAvailableGetResponse findAvailableTime(Long meetingId) {
        return meetingTimeCalculationService.findAvailableTime(meetingId);
    }

    // 모임별 개인적으로 피하고 싶은 시간 저장하기
    public void saveMeetingAvoidTime(Long memberId, Long meetingId,
        List<MeetingTimeCreateRequest> avoidTimeCreateRequests) {

        // 피하고 싶은 시간 없으면 그냥 종료
        if (avoidTimeCreateRequests.isEmpty() || avoidTimeCreateRequests == null) {
            return;
        }

        Member member = memberService.findMember(memberId);
        Meeting meeting = meetingService.findMeeting(meetingId);
        MemberMeeting memberMeeting = meetingService.findMemberMeeting(member, meeting);

        for (MeetingTimeCreateRequest avoidTimeCreateRequest : avoidTimeCreateRequests) {
            createAndSaveMeetingEvent(memberMeeting, avoidTimeCreateRequest);
        }
    }

    // 피하고 싶은 시간을 기반으로 MeetingEvent 생성 및 저장
    private void createAndSaveMeetingEvent(MemberMeeting memberMeeting, MeetingTimeCreateRequest meetingTimeCreateRequest) {
        // 시간 정보를 Event로 생성 후 저장
        TimeCreateRequest timeCreateRequest = meetingTimeCreateRequest.toTimeCreateRequest();
        Event avoidEvent = new Event(timeCreateRequest.toEntity());
        eventRepository.save(avoidEvent);

        // MeetingEvent로 저장
        MeetingEvent meetingEvent = new MeetingEvent(memberMeeting, avoidEvent);
        meetingEventRepository.save(meetingEvent);
    }
}
