package org.cookieandkakao.babting.domain.meeting.service;

import jakarta.transaction.Transactional;
import java.util.List;
import org.cookieandkakao.babting.domain.calendar.dto.request.EventCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.request.TimeCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.entity.Event;
import org.cookieandkakao.babting.domain.calendar.service.EventService;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarService;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingEventCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingTimeCreateRequest;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MeetingEvent;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingEventRepository;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.springframework.stereotype.Service;

@Transactional
@Service
public class MeetingEventCreateService {

    private final TalkCalendarService talkCalendarService;
    private final EventService eventService;
    private final MemberService memberService;
    private final MeetingService meetingService;
    private final MeetingEventRepository meetingEventRepository;

    public MeetingEventCreateService(TalkCalendarService talkCalendarService,
        EventService eventService, MemberService memberService,
        MeetingService meetingService,
        MeetingEventRepository meetingEventRepository) {
        this.talkCalendarService = talkCalendarService;
        this.eventService = eventService;
        this.memberService = memberService;
        this.meetingService = meetingService;
        this.meetingEventRepository = meetingEventRepository;
    }

    public EventCreateResponse addMeetingEvent(Long memberId,
        MeetingEventCreateRequest meetingEventCreateRequest) {
        EventCreateRequest eventCreateRequest = EventCreateRequest.from(meetingEventCreateRequest);
        return talkCalendarService.createEvent(eventCreateRequest, memberId);
    }

    @Transactional
    public void saveMeetingAvoidTime(Long memberId, Long meetingId,
        List<MeetingTimeCreateRequest> avoidTimeCreateRequests) {

        if (avoidTimeCreateRequests.isEmpty() || avoidTimeCreateRequests == null) {
            return;
        }

        Member member = memberService.findMember(memberId);
        Meeting meeting = meetingService.findMeeting(meetingId);
        MemberMeeting memberMeeting = meetingService.findMemberMeeting(member, meeting);

        List<MeetingEvent> meetingEvents = avoidTimeCreateRequests.stream()
            .map(avoidTimeCreateRequest -> createMeetingEvent(memberMeeting, avoidTimeCreateRequest))
            .toList();

        meetingEventRepository.saveAll(meetingEvents);
    }


    private MeetingEvent createMeetingEvent(MemberMeeting memberMeeting,
        MeetingTimeCreateRequest meetingTimeCreateRequest) {

        TimeCreateRequest timeCreateRequest = meetingTimeCreateRequest.toTimeCreateRequest();
        Event avoidEvent = eventService.saveAvoidTimeEvent(timeCreateRequest.toEntity());

        return new MeetingEvent(memberMeeting, avoidEvent);
    }

}