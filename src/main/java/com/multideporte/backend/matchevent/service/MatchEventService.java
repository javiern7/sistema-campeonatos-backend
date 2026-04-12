package com.multideporte.backend.matchevent.service;

import com.multideporte.backend.matchevent.dto.request.MatchEventAnnulRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventCreateRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventUpdateRequest;
import com.multideporte.backend.matchevent.dto.response.MatchEventResponse;
import java.util.List;

public interface MatchEventService {

    List<MatchEventResponse> getMatchEvents(Long matchId);

    MatchEventResponse create(Long matchId, MatchEventCreateRequest request);

    MatchEventResponse update(Long matchId, Long eventId, MatchEventUpdateRequest request);

    MatchEventResponse annul(Long matchId, Long eventId, MatchEventAnnulRequest request);
}
