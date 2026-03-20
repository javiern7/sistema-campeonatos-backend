package com.multideporte.backend.sport.service;

import com.multideporte.backend.sport.dto.response.SportResponse;
import java.util.List;

public interface SportService {

    List<SportResponse> getAll(Boolean activeOnly);
}
