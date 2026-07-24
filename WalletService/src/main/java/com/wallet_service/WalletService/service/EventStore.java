package com.wallet_service.WalletService.service;

import com.wallet_service.WalletService.dto.DataObject;
import com.wallet_service.WalletService.model.entries.LedgerEvents;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EventStore {
    LedgerEvents appendEvent(LedgerEvents event);

    List<LedgerEvents> getEvents(String aggregateId);

    List<LedgerEvents> getEventsAfterVersion(String aggregateId, Integer afterVersion);

    Integer getCurrentVersion(String aggregateId);

    DataObject getBalance(String aggregateId);
    Map<String, DataObject> getL1CacheContents();
}