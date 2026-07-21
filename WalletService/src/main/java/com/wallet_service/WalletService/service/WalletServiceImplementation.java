package com.wallet_service.WalletService.service;

import com.wallet_service.WalletService.exception.EventAlreadyExists;
import com.wallet_service.WalletService.exception.EventNotFound;
import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.repository.WalletRepository;
import com.wallet_service.WalletService.util.BalanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WalletServiceImplementation implements EventStore {

    private final WalletRepository walletRepository;

    public WalletServiceImplementation(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public LedgerEvents appendEvent(LedgerEvents event) {
        Integer nextVersion = event.getVersion() + 1;
        //if next version already exists then throw Exception
        if (walletRepository.existsByAggregateIdAndVersion(event.getAggregateId(), nextVersion)) {
            throw new EventAlreadyExists("Version " + nextVersion + " already exists for aggregate " + event.getAggregateId());
        }
        LedgerEvents newEvent = new LedgerEvents(
                null,
                event.getAggregateId(),
                nextVersion,
                event.getEventType(),
                event.getPayload(),
                null,
                event.getTraceId()
        );
        return walletRepository.save(newEvent);
    }

    @Override
    public List<LedgerEvents> getEvents(String aggregateId) {
        List<LedgerEvents> events = walletRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        if (events.isEmpty()) {
            throw new EventNotFound("No events found for aggregate " + aggregateId);
        }
        System.out.println("BALANCE=======>"+BalanceCalculator.calculateBalance(events));
        return events;
    }

    @Override
    public List<LedgerEvents> getEventsAfterVersion(String aggregateId, Integer afterVersion) {
        List<LedgerEvents> events = walletRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, afterVersion);
        if (events.isEmpty()) {
            throw new EventNotFound("No events found for aggregate " + aggregateId + " after version " + afterVersion);
        }
        return events;
    }

    @Override
    public Integer getCurrentVersion(String aggregateId) {
        Integer version = walletRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId)
                .map(LedgerEvents::getVersion).orElse(0);
        if (version == 0) {
            throw new EventNotFound("No events found for aggregate " + aggregateId);
        }
        return version;
    }
}
