package org.cookieandkakao.babting.domain.meeting.controller;

import java.util.List;
import org.cookieandkakao.babting.common.annotaion.LoginMemberId;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseBody.SuccessBody;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseGenerator;
import org.cookieandkakao.babting.domain.calendar.dto.response.TimeGetResponse;
import org.cookieandkakao.babting.domain.meeting.dto.request.ConfirmMeetingGetRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.response.MeetingGetResponse;
import org.cookieandkakao.babting.domain.meeting.dto.response.TimeAvailableGetResponse;
import org.cookieandkakao.babting.domain.meeting.service.MeetingEventService;
import org.cookieandkakao.babting.domain.meeting.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {
    private final MeetingService meetingService;
    private final MeetingEventService meetingEventService;

    public MeetingController(MeetingService meetingService,
        MeetingEventService meetingEventService) {
        this.meetingService = meetingService;
        this.meetingEventService = meetingEventService;
    }

    // 모임 생성(주최자)
    @PostMapping
    public ResponseEntity<SuccessBody<Void>> createMeeting(
        @LoginMemberId Long memberId,
        @RequestBody MeetingCreateRequest meetingCreateRequest){
        meetingService.createMeeting(memberId, meetingCreateRequest);
        return ApiResponseGenerator.success(HttpStatus.CREATED, "모임 생성 성공");
    }

    // 모임 참가(초대받은사람)
    @PostMapping("/{meetingId}/join")
    public ResponseEntity<SuccessBody<Void>> joinMeeting(
        @PathVariable("meetingId") Long meetingId,
        @LoginMemberId Long memberId
    ){
        meetingService.joinMeeting(memberId, meetingId);
        return ApiResponseGenerator.success(HttpStatus.OK, "모임 참가 성공");
    }

    // 모임 확정(주최자)
    @PostMapping("/{meetingId}/confirm")
    public ResponseEntity<SuccessBody<Void>> confirmMeeting(
        @PathVariable("meetingId") Long meetingId,
        @RequestBody ConfirmMeetingGetRequest confirmMeetingGetRequest,
        @LoginMemberId Long memberId
    ) {
        meetingEventService.confirmMeeting(memberId, meetingId, confirmMeetingGetRequest);
        return ApiResponseGenerator.success(HttpStatus.OK, "모임 확정 성공");
    }

    // 모임 탈퇴(주최자, 초대받은 사람)
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<SuccessBody<Void>> exitMeeting(
        @PathVariable("meetingId") Long meetingId,
        @LoginMemberId Long memberId
    ){
        meetingService.exitMeeting(memberId, meetingId);
        return ApiResponseGenerator.success(HttpStatus.OK, "모임 탈퇴 성공");
    }

    // 내가 참여 모임 목록 조회
    @GetMapping
    public ResponseEntity<SuccessBody<List<MeetingGetResponse>>> getAllMeeting(
        @LoginMemberId Long memberId
    ){
        List<MeetingGetResponse> meetings = meetingService.getAllMeetings(memberId);
        return ApiResponseGenerator.success(HttpStatus.OK, "참여 모임 목록 조회 성공", meetings);
    }

    // 모임 id로 이름, 모임 시작과 끝 날짜 조회

    // 모임 주최자 확인

    // 모임 확정 날짜, 확정 음식 확인

    // 모임 공통 시간표 조회
    @GetMapping("/{meetingId}/calendar")
    public ResponseEntity<SuccessBody<TimeAvailableGetResponse>> getAvailableTime(
        @PathVariable("meetingId") Long meetingId
    ){
        TimeAvailableGetResponse timeAvailableGetResponse = meetingEventService.findAvailableTime(meetingId);
        return ApiResponseGenerator.success(HttpStatus.OK, "모임 공통 시간표 조회 성공", timeAvailableGetResponse);
    }
    // 모임 정보 수정
}
