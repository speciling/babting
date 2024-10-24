package org.cookieandkakao.babting.domain.meeting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cookieandkakao.babting.domain.calendar.service.TalkCalendarClientService;
import org.cookieandkakao.babting.domain.food.service.FoodRepositoryService;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.response.MeetingGetResponse;
import org.cookieandkakao.babting.domain.meeting.entity.Location;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;
import org.cookieandkakao.babting.domain.meeting.entity.MeetingEvent;
import org.cookieandkakao.babting.domain.meeting.entity.MemberMeeting;
import org.cookieandkakao.babting.domain.meeting.repository.LocationRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingEventRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MeetingRepository;
import org.cookieandkakao.babting.domain.meeting.repository.MemberMeetingRepository;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.repository.MemberRepository;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingEventRepository meetingEventRepository;
    private final MemberMeetingRepository memberMeetingRepository;
    private final LocationRepository locationRepository;
    private final FoodRepositoryService foodRepositoryService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final TalkCalendarClientService talkCalendarClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MeetingService(MeetingRepository meetingRepository,
        MeetingEventRepository meetingEventRepository,
        MemberMeetingRepository memberMeetingRepository,
        LocationRepository locationRepository, FoodRepositoryService foodRepositoryService,
        MemberService memberService, MemberRepository memberRepository,
        TalkCalendarClientService talkCalendarClientService) {
        this.meetingRepository = meetingRepository;
        this.meetingEventRepository = meetingEventRepository;
        this.memberMeetingRepository = memberMeetingRepository;
        this.locationRepository = locationRepository;
        this.foodRepositoryService = foodRepositoryService;
        this.memberService = memberService;
        this.memberRepository = memberRepository;
        this.talkCalendarClientService = talkCalendarClientService;
    }

    // 모임 생성(주최자)
    public void createMeeting(Long memberId, MeetingCreateRequest meetingCreateRequest){
        Member member = memberService.findMember(memberId);
        Meeting meeting = meetingCreateRequest.toEntity();
        Location baseLocation = meetingCreateRequest.baseLocation().toEntity();
        locationRepository.save(baseLocation);
        meetingRepository.save(meeting);
        memberMeetingRepository.save(new MemberMeeting(member, meeting, true));
    }

    // 모임 참가(초대받은사람)
    public void joinMeeting(Long memberId, Long meetingId){
        Member member = memberService.findMember(memberId);
        Meeting meeting = findMeeting(meetingId);

        boolean isJoinMeeting = memberMeetingRepository.existsByMemberAndMeeting(member, meeting);
        if (isJoinMeeting){
            throw new IllegalStateException("이미 모임에 참가한 상태입니다.");
        }

        memberMeetingRepository.save(new MemberMeeting(member, meeting, false));
    }

    // 모임 탈퇴(주최자, 초대받은 사람)
    public void exitMeeting(Long memberId, Long meetingId){
        Member member = memberService.findMember(memberId);
        Meeting meeting = findMeeting(meetingId);
        MemberMeeting memberMeeting = findMemberMeeting(member, meeting);

        // 모임 확정 전
        if (meeting.getConfirmDateTime() != null){
            if (memberMeeting.isHost()){
                // 해당 모임에 속하는 회원 모임 전부 삭제
                memberMeetingRepository.deleteAllByMeeting(meeting);
                // 모임 삭제
                meetingRepository.delete(meeting);
            } else {
                // 해당 모임에 속하는 회원 모임 삭제
                memberMeetingRepository.delete(memberMeeting);
            }
        // 모임 확정 후
        } else{
            //Todo 모임 삭제 시 해당 모임의 모임 일정도 전부 삭제해야함.
            //만약 톡 캘린더에서 일정을 삭제했을 시 모임 일정이 없을 수도 있음.
            if (memberMeeting.isHost()){
                // 해당 모임의 모든 모임 일정 삭제
                // 해당 모임에 속하는 회원 모임 전부 삭제
                // 모임 삭제
            } else {
                // 해당 모임의 일정 삭제
                // 해당 모임에 속하는 회원 모임 전부 삭제
            }
        }

    }

    // 내가 참여한 모임 목록 조회
    public List<MeetingGetResponse> getAllMeetings(Long memberId){
        Member member = memberService.findMember(memberId);
        List<Meeting> meetingList = memberMeetingRepository.findMeetingsByMember(member);
        return meetingList.stream()
            .map(MeetingGetResponse::from)
            .collect(Collectors.toList());
    }
  
    public Meeting findMeeting(Long meetingId){
        return meetingRepository.findById(meetingId)
            .orElseThrow(() -> new NoSuchElementException("해당 모임이 존재하지 않습니다."));
    }

    public MemberMeeting findMemberMeeting(Member member, Meeting meeting){
        return memberMeetingRepository.findByMemberAndMeeting(member, meeting)
            .orElseThrow(() -> new NoSuchElementException("해당 모임에 회원이 존재하지 않습니다."));
    }

    public List<MemberMeeting> findAllMemberMeeting(Meeting meeting){
        return memberMeetingRepository.findByMeeting(meeting);
    }

    // MeetingEvent는 없을 수 있기 때문에 Optional로 반환
    private Optional<MeetingEvent> findMeetingEvent(MemberMeeting memberMeeting){
        return meetingEventRepository.findByMemberMeeting(memberMeeting);
    }

}
