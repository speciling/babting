package org.cookieandkakao.babting.domain.calendar.service;

import org.cookieandkakao.babting.domain.calendar.entity.Event;
import org.cookieandkakao.babting.domain.calendar.entity.Time;
import org.cookieandkakao.babting.domain.calendar.repository.EventRepository;
import org.cookieandkakao.babting.domain.calendar.repository.TimeRepository;
import org.cookieandkakao.babting.domain.meeting.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final TimeRepository timeRepository;
    private final LocationRepository locationRepository;

    public EventService(EventRepository eventRepository, TimeRepository timeRepository,
        LocationRepository locationRepository) {
        this.eventRepository = eventRepository;
        this.timeRepository = timeRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public Event saveAvoidTimeEvent(Time avoidTime) {
        Event avoidTimeEvent = new Event(avoidTime);
        eventRepository.save(avoidTimeEvent);
        return avoidTimeEvent;
    }
}
