package com.wallet_service.WalletService.service;

import com.wallet_service.WalletService.exception.EventAlreadyExists;
import com.wallet_service.WalletService.exception.EventNotFound;
import com.wallet_service.WalletService.model.entries.AccountSnapshots;
import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.repository.SnapshotRepository;
import com.wallet_service.WalletService.repository.WalletRepository;
import com.wallet_service.WalletService.util.BalanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WalletServiceImplementation implements EventStore {

    private final WalletRepository walletRepository;
    private final SnapshotRepository snapshotRepository;

    public WalletServiceImplementation(WalletRepository walletRepository, SnapshotRepository snapshotRepository) {
        this.walletRepository = walletRepository;
        this.snapshotRepository = snapshotRepository;
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


        if (nextVersion % 50 == 0) {
            BigDecimal previousBalance = getBalance(event.getAggregateId());
            BigDecimal eventImpact = BalanceCalculator.calculateBalance(List.of(newEvent));
            BigDecimal currentBalance = previousBalance.add(eventImpact);
            AccountSnapshots entity = new AccountSnapshots(
                    event.getAggregateId(),
                    nextVersion,
                    currentBalance
            );
            LedgerEvents savedEvent = walletRepository.save(newEvent);
            snapshotRepository.save(entity);
            return savedEvent;
        }

        return walletRepository.save(newEvent);

    }

    @Override
    public List<LedgerEvents> getEvents(String aggregateId) {
        List<LedgerEvents> events = walletRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        if (events.isEmpty()) {
            throw new EventNotFound("No events found for aggregate " + aggregateId);
        }
        return events;
    }

    @Override
    public List<LedgerEvents> getEventsAfterVersion(String aggregateId, Integer afterVersion) {
        return walletRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, afterVersion);

    }

    @Override
    public Integer getCurrentVersion(String aggregateId) {
        return walletRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId)
                .map(LedgerEvents::getVersion).orElse(0);
    }

    @Override
    public BigDecimal getBalance(String aggregateId) {
        Optional<AccountSnapshots> snapshot = snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId);
        //no snapshot exists (full replay of events for aggregateId)
        if (snapshot.isEmpty()) {
            return BalanceCalculator.calculateBalance(getEvents(aggregateId));
        }
        //snapshot exists (snapshotBalance + balanceOfVersionsAfterSnapshots)
        List<LedgerEvents> eventsAfterVersion = getEventsAfterVersion(aggregateId, snapshot.get().getVersion());

        BigDecimal snapshotBalance = snapshot.get().getBalance();
        BigDecimal balanceAfterVersion = BalanceCalculator.calculateBalance(eventsAfterVersion);

        return snapshotBalance.add(balanceAfterVersion);
    }
}
