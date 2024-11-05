package org.cookieandkakao.babting.domain.meeting.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.cookieandkakao.babting.domain.calendar.dto.response.TimeGetResponse;
import org.cookieandkakao.babting.domain.meeting.entity.TimeZone;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class MeetingTimeCalculationService {
    // 겹치는 시간 병합
    public List<TimeGetResponse> mergeOverlappingTimes(List<TimeGetResponse> times) {
        List<TimeGetResponse> mergedTimes = new ArrayList<>();

        if (times.isEmpty()) {
            return mergedTimes;
        }

        TimeGetResponse currentTime = times.getFirst();

        for (int i = 1; i < times.size(); i++) {
            TimeGetResponse next = times.get(i);
            LocalDateTime currentEnd = LocalDateTime.parse(currentTime.endAt());
            LocalDateTime nextStart = LocalDateTime.parse(next.startAt());

            // 겹치는 시간대라면 병합
            if (!nextStart.isAfter(currentEnd)) {
                currentTime = new TimeGetResponse(
                    currentTime.startAt(),
                    maxEndTime(currentTime.endAt(), next.endAt()),
                    currentTime.timeZone(),
                    currentTime.allDay() || next.allDay()
                );
            } else {
                mergedTimes.add(currentTime);
                currentTime = next;
            }
        }

        mergedTimes.add(currentTime); // 마지막 시간대 추가
        return mergedTimes;
    }

    // 두 시간대 중 더 늦은 종료 시간을 반환
    public String maxEndTime(String end1, String end2) {
        LocalDateTime e1 = LocalDateTime.parse(end1);
        LocalDateTime e2 = LocalDateTime.parse(end2);
        if (e1.isAfter(e2)) {
            return end1;
        }
        return end2;
    }

    // 빈 시간대
    public List<TimeGetResponse> calculateAvailableTimes(List<TimeGetResponse> mergedTimes,
        String from, String to) {
        List<TimeGetResponse> availableTimes = new ArrayList<>();
        LocalDateTime searchStart = LocalDateTime.parse(from);
        LocalDateTime searchEnd = LocalDateTime.parse(to);

        // 첫 번째 시간대 이전의 빈 시간 확인
        // => 검색 시작일 ~ mergedTime 첫번째 일정의 시작시간까지 빈 시간
        if (searchStart.isBefore(LocalDateTime.parse(mergedTimes.getFirst().startAt()))) {
            availableTimes.add(new TimeGetResponse(
                from,
                mergedTimes.getFirst().startAt(),
                TimeZone.SEOUL.getArea(),
                false
            ));
        }

        // 두 시간대 사이의 빈 시간 계산
        for (int i = 0; i < mergedTimes.size() - 1; i++) {
            LocalDateTime endOfCurrent = LocalDateTime.parse(mergedTimes.get(i).endAt());
            LocalDateTime startOfNext = LocalDateTime.parse(mergedTimes.get(i + 1).startAt());

            // 첫 번째 time의 끝 시간 ~ 두 번째 time의 시작시간 => 빈 시간대
            if (endOfCurrent.isBefore(startOfNext)) {
                availableTimes.add(new TimeGetResponse(
                    mergedTimes.get(i).endAt(),
                    mergedTimes.get(i + 1).startAt(),
                    TimeZone.SEOUL.getArea(),
                    false
                ));
            }
        }

        // 마지막 시간대 이후의 빈 시간 확인
        if (searchEnd.isAfter(LocalDateTime.parse(mergedTimes.getLast().endAt()))) {
            availableTimes.add(new TimeGetResponse(
                mergedTimes.getLast().endAt(),
                to,
                TimeZone.SEOUL.getArea(),
                false
            ));
        }

        return availableTimes;
    }
}
