package org.cookieandkakao.babting.domain.meeting.dto.response;

import org.cookieandkakao.babting.domain.food.dto.FoodGetResponse;
import org.cookieandkakao.babting.domain.meeting.entity.Meeting;

public record MeetingConfirmedInfo(
    String confirmedDateTime,
    FoodGetResponse confirmedFood
) {
    public static MeetingConfirmedInfo of(Meeting meeting) {
        return new MeetingConfirmedInfo(meeting.getConfirmDateTime().toString(),
            FoodGetResponse.from(meeting.getConfirmedFood()));
    }
}
