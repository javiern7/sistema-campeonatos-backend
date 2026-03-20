package com.multideporte.backend.sport.service.impl;

import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.mapper.SportMapper;
import com.multideporte.backend.sport.repository.SportRepository;
import com.multideporte.backend.sport.service.SportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final SportMapper sportMapper;

    @Override
    public List<SportResponse> getAll(Boolean activeOnly) {
        return sportRepository.findAll()
                .stream()
                .filter(sport -> !Boolean.TRUE.equals(activeOnly) || Boolean.TRUE.equals(sport.getActive()))
                .map(sportMapper::toResponse)
                .toList();
    }
}
