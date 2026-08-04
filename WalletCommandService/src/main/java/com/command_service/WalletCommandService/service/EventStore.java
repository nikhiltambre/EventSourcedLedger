package com.command_service.WalletCommandService.service;

import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EventStore {
  LedgerEvents appendEvent(LedgerEvents event);

  List<LedgerEvents> getEvents(String aggregateId);

  List<LedgerEvents> getEventsAfterVersion(String aggregateId,
                                           Integer afterVersion);

  BigDecimal getBalance(String aggregateId);

  Map<String, BigDecimal> getL1CacheContents();
}
