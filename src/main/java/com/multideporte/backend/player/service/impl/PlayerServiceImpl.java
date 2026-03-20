package com.multideporte.backend.player.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.player.dto.request.PlayerCreateRequest;
import com.multideporte.backend.player.dto.request.PlayerUpdateRequest;
import com.multideporte.backend.player.dto.response.PlayerResponse;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.mapper.PlayerMapper;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.player.repository.PlayerRosterRepository;
import com.multideporte.backend.player.repository.PlayerSpecifications;
import com.multideporte.backend.player.service.PlayerService;
import com.multideporte.backend.player.validation.PlayerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerRosterRepository playerRosterRepository;
    private final PlayerMapper playerMapper;
    private final PlayerValidator playerValidator;

    @Override
    @Transactional
    public PlayerResponse create(PlayerCreateRequest request) {
        playerValidator.validateForCreate(
                request.documentType(),
                request.documentNumber(),
                request.birthDate(),
                request.phone()
        );

        Player entity = playerMapper.toEntity(request);
        entity.setDocumentType(playerValidator.normalizeDocumentType(request.documentType()));
        entity.setDocumentNumber(playerValidator.normalizeDocumentNumber(request.documentNumber()));

        Player saved = playerRepository.save(entity);
        return playerMapper.toResponse(saved);
    }

    @Override
    public PlayerResponse getById(Long id) {
        return playerMapper.toResponse(findPlayer(id));
    }

    @Override
    public Page<PlayerResponse> getAll(
            String search,
            String documentType,
            String documentNumber,
            Boolean active,
            Pageable pageable
    ) {
        return playerRepository.findAll(
                        PlayerSpecifications.byFilters(search, documentType, documentNumber, active),
                        pageable
                )
                .map(playerMapper::toResponse);
    }

    @Override
    @Transactional
    public PlayerResponse update(Long id, PlayerUpdateRequest request) {
        Player entity = findPlayer(id);
        playerValidator.validateForUpdate(
                entity,
                request.documentType(),
                request.documentNumber(),
                request.birthDate(),
                request.phone()
        );

        playerMapper.updateEntity(entity, request);
        entity.setDocumentType(playerValidator.normalizeDocumentType(request.documentType()));
        entity.setDocumentNumber(playerValidator.normalizeDocumentNumber(request.documentNumber()));

        Player saved = playerRepository.save(entity);
        return playerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Player entity = findPlayer(id);

        if (playerRosterRepository.existsByPlayerId(id)) {
            throw new BusinessException("No se puede eliminar el jugador porque ya esta asociado a un roster");
        }

        playerRepository.delete(entity);
    }

    private Player findPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player no encontrado con id: " + id));
    }
}
