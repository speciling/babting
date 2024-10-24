package org.cookieandkakao.babting.domain.meeting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.cookieandkakao.babting.common.exception.customexception.EventCreationException;
import org.cookieandkakao.babting.common.exception.customexception.JsonConversionException;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarClientService;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingEventCreateRequest;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MemberMeetingRepository;
import org.cookieandkakao.babting.domain.member.entity.KakaoToken;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.repository.MemberRepository;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Transactional
@Service
public class MeetingEventService {
    private final MemberService memberService;
    private final TalkCalendarClientService talkCalendarClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeetingService meetingService;

    public MeetingEventService(MemberService memberService,
        TalkCalendarClientService talkCalendarClientService,
        MeetingService meetingService) {
        this.memberService = memberService;
        this.talkCalendarClientService = talkCalendarClientService;
        this.meetingService = meetingService;
    }

    // 모임 확정되면 일정 생성
    public void confirmMeeting(Long memberId, Long meetingId){
        Member member = memberService.findMember(memberId);
        Meeting meeting = meetingService.findMeeting(meetingId);
        MemberMeeting memberMeeting = meetingService.findMemberMeeting(member, meeting);

        if (!memberMeeting.isHost()){
            throw new IllegalStateException("권한이 없습니다.");
        }

        if (meeting.getConfirmDateTime() != null){
            throw new IllegalStateException("이미 모임 시간이 확정되었습니다.");
        }
        /*String startAt = "2024-10-11T06:00:00Z";
        String endAt = "2024-10-11T09:00:00Z";
        String timeZone = "Asia/Seoul";
        boolean allDay = false;

        MeetingTimeCreateRequest meetingTimeCreateRequest = new MeetingTimeCreateRequest(startAt, endAt, timeZone, allDay);

        */
        MeetingEventCreateRequest meetingEventCreateRequest
            = new MeetingEventCreateRequest(
            // conflict 해결하느라 null 값 입력
            meeting.getTitle(), null
            /*meetingTimeCreateRequest*/
        );

        List<Long> memberIds = getMemberIdInMeetingId(meetingId);

        for (Long currentMemberId : memberIds){
            addMeetingEvent(currentMemberId, meetingId, meetingEventCreateRequest);
        }
    }

    // 일정 생성 후 캘린더에 일정 추가
    public EventCreateResponse addMeetingEvent(Long memberId,Long meetingId, MeetingEventCreateRequest meetingEventCreateRequest) {
        String kakaoAccessToken = getKakaoAccessToken(memberId);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String eventJson = convertToJSONString(meetingEventCreateRequest);
        formData.add("event", eventJson);
        Map<String, Object> responseBody = talkCalendarClientService.createEvent(kakaoAccessToken, formData);
        if (responseBody != null && responseBody.containsKey("event_id")) {
            String eventId = responseBody.get("event_id").toString();
            // EventCreateResponseDto로 응답 반환
            return new EventCreateResponse(eventId);
        }
        throw new EventCreationException("Event 생성 중 오류 발생: 응답에서 event_id가 없습니다.");
    }

    private String getKakaoAccessToken(Long memberId) {
        KakaoToken kakaoToken = memberService.getKakaoToken(memberId);
        String kakaoAccessToken = kakaoToken.getAccessToken();
        return kakaoAccessToken;
    }

    private String convertToJSONString(MeetingEventCreateRequest eventCreateRequest) {
        try {
            return objectMapper.writeValueAsString(eventCreateRequest);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSON 변환 중 오류가 발생했습니다.");
        }
    }

    public List<Long> getMemberIdInMeetingId(Long meetingId) {
        Meeting meeting = meetingService.findMeeting(meetingId);
        List<MemberMeeting> memberMeetings = meetingService.findAllMemberMeeting(meeting);

        return memberMeetings.stream()
            .map(memberMeeting -> memberMeeting.getMember().getMemberId())
            .toList();
    }
}
