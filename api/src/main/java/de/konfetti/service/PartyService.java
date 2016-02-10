package de.konfetti.service;

import de.konfetti.data.Party;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface PartyService {

    Party create(@NotNull Party party);

    Party update(@NotNull Party party);

    Party delete(@NotNull long listId);

    List<Party> getAllParties();

    Party findById(long partyId);
}