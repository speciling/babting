package org.cookieandkakao.babting.domain.meeting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cookieandkakao.babting.common.exception.customexception.EventCreationException;
import org.cookieandkakao.babting.common.exception.customexception.JsonConversionException;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventListGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.TimeGetResponse;
import org.cookieandkakao.babting.domain.calendar.entity.Time;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarClientService;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingEventCreateRequest;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.member.entity.KakaoToken;
import org.cookieandkakao.babting.domain.member.entity.Member;
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
    private EventCreateResponse addMeetingEvent(Long memberId,Long meetingId, MeetingEventCreateRequest meetingEventCreateRequest) {
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

    private List<Long> getMemberIdInMeetingId(Long meetingId) {
        Meeting meeting = meetingService.findMeeting(meetingId);
        List<MemberMeeting> memberMeetings = meetingService.findAllMemberMeeting(meeting);

        return memberMeetings.stream()
            .map(memberMeeting -> memberMeeting.getMember().getMemberId())
            .toList();
    }
    /** 빈 시간대 조회 로직 설명
     *
     * 1. 모임의 모든 참여자들의 일정 중 Time을 allTimes에 추출
     * 2. allTimes의 시간을 시작 시간을 기준으로 오름차순 정렬한 값들을 sortedTimes에 저장
     * 3. sortedTimes에 겹치는 시간이 있다면 모든 시간들을 (2024-10-24T15:00 ~ 2024-10-24T16:00)
     *  ex) 1. 2024-10-24T12:00 ~ 2024-10-24T15:00
     *      2. 2024-10-24T14:00 ~ 2024-10-24T16:00
     *      1의 시간을 1의 시작 시간 ~ 2의 끝 시간으로 병합 (2024-10-24T12:00 ~ 2024-10-24T16:00)
     * 4. 이제 mergedTime에는 겹치지 않는 시간대만 존재
     * 5. mergedTime에 있는 시간들을 순회하면서 i번째 끝 시간 ~ i+1번째 시작 시간으로 시간 생성
     */
    public List<TimeGetResponse> findAvailableTime(Long meetingId, String from, String to) {
        List<Long> joinedMemberIds = getMemberIdInMeetingId(meetingId);

        // 참여자별 일정에서 필요한 시간 정보만 추출하여 리스트로 수집
        List<TimeGetResponse> allTimes = joinedMemberIds.stream()
            .flatMap(memberId ->
                talkCalendarClientService
                    .getEventList(getKakaoAccessToken(memberId), from, to)
                    .events()
                    .stream()
                    .map(EventGetResponse::time)
            )
            .toList();

        // 시간대 정렬 (시작 시간을 기준으로 오름차순 정렬)
        List<TimeGetResponse> sortedTimes = allTimes.stream()
            .sorted(Comparator.comparing(time -> LocalDateTime.parse(time.startAt())))
            .toList();

        // 겹치는 시간 병합
        List<TimeGetResponse> mergedTimes = mergeOverlappingTimes(sortedTimes);

        // 빈 시간대 계산
        return calculateAvailableTimes(mergedTimes, from, to);
    }

    // 겹치는 시간 병합
    private List<TimeGetResponse> mergeOverlappingTimes(List<TimeGetResponse> times) {

    }

    // 빈 시간대
    private List<TimeGetResponse> calculateAvailableTimes(List<TimeGetResponse> mergedTimes, String from, String to) {

    }



}
