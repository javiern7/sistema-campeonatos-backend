package com.multideporte.backend.tournamentpublication.service.impl;

import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentpublication.entity.TournamentPublication;
import com.multideporte.backend.tournamentpublication.entity.TournamentPublicationStatus;
import com.multideporte.backend.tournamentpublication.repository.TournamentPublicationRepository;
import com.multideporte.backend.tournamentpublication.service.TournamentPublicationService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class TournamentPublicationServiceImpl implements TournamentPublicationService {
    private final TournamentRepository tournamentRepository;
    private final TournamentPublicationRepository publicationRepository;
    private final CurrentUserService currentUserService;

    @Override @Transactional
    public TournamentPublication publish(Long tournamentId) {
        requireTournament(tournamentId);
        TournamentPublication item = getOrCreate(tournamentId);
        item.setPublicationStatus(TournamentPublicationStatus.PUBLISHED);
        item.setPublishedAt(OffsetDateTime.now()); item.setUnpublishedAt(null);
        item.setUpdatedAt(OffsetDateTime.now()); item.setUpdatedByUserId(currentUserService.requireCurrentUserId());
        return publicationRepository.save(item);
    }

    @Override @Transactional
    public TournamentPublication unpublish(Long tournamentId) {
        requireTournament(tournamentId);
        TournamentPublication item = getOrCreate(tournamentId);
        item.setPublicationStatus(TournamentPublicationStatus.UNPUBLISHED);
        item.setUnpublishedAt(OffsetDateTime.now()); item.setUpdatedAt(OffsetDateTime.now());
        item.setUpdatedByUserId(currentUserService.requireCurrentUserId());
        return publicationRepository.save(item);
    }

    private TournamentPublication getOrCreate(Long tournamentId) {
        return publicationRepository.findById(tournamentId).orElseGet(() -> {
            TournamentPublication item = new TournamentPublication(); item.setTournamentId(tournamentId);
            item.setPublicationStatus(TournamentPublicationStatus.UNPUBLISHED); item.setAccessMode("UNLISTED"); return item;
        });
    }

    private void requireTournament(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) throw new ResourceNotFoundException("Tournament no encontrado con id: " + tournamentId);
    }
}
