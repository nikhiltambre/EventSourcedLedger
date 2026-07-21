package com.wallet_service.WalletService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wallet_service.WalletService.exception.EventAlreadyExists;
import com.wallet_service.WalletService.exception.EventNotFound;
import com.wallet_service.WalletService.model.entries.AccountSnapshots;
import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.model.events.MoneyCredited;
import com.wallet_service.WalletService.repository.SnapshotRepository;
import com.wallet_service.WalletService.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplementationTest {
    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private WalletServiceImplementation walletService;

    private LedgerEvents sampleEvent;

    @BeforeEach
    void setup() {
        sampleEvent = new LedgerEvents();
        sampleEvent = new LedgerEvents();
        sampleEvent.setAggregateId("ACC100");
        sampleEvent.setVersion(1);
        sampleEvent.setEventType("AccountOpened");
        sampleEvent.setTraceId("trace-001");
    }

    // version does not exists -> saves
    @Test
    @DisplayName("appendEvent -> saves and returns event when slot is free")
    void appendEvent_shouldSaveAndReturnEvent_whenVersionSlotIsFree() {
        // arrange
        LedgerEvents tempEvent = new LedgerEvents();
        tempEvent.setAggregateId("ACC100");
        tempEvent.setVersion(2);
        tempEvent.setEventType("AccountOpened");

        when(walletRepository.existsByAggregateIdAndVersion("ACC100", 2))
                .thenReturn(false);
        when(walletRepository.save(any())).thenReturn(tempEvent);
        // act
        LedgerEvents result = walletService.appendEvent(sampleEvent);
        // assert
        assertThat(result).isNotNull();
        assertThat(result.getAggregateId()).isEqualTo("ACC100");
        assertThat(result.getVersion()).isEqualTo(2);
        ArgumentCaptor<LedgerEvents> eventCaptor =
                ArgumentCaptor.forClass(LedgerEvents.class);
        verify(walletRepository, times(1)).save(eventCaptor.capture());
        LedgerEvents capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getAggregateId()).isEqualTo("ACC100");
        assertThat(capturedEvent.getVersion()).isEqualTo(2);
        assertThat(capturedEvent.getEventType()).isEqualTo("AccountOpened");
    }

    // version does exists -> throws EventAlreadyExists
    @Test
    @DisplayName("appendEvent -> throws EventAlreadyExists when slot is taken ")
    void appendEvent_showThrowEventAlreadyExists_whenSlotIsTaken() {
        when(walletRepository.existsByAggregateIdAndVersion("ACC100", 2))
                .thenReturn(true);
        assertThatThrownBy(() -> walletService.appendEvent(sampleEvent))
                .isInstanceOf(EventAlreadyExists.class)
                .hasMessageContaining("ACC100");
    }

    // event exists(getEvents)
    @Test
    @DisplayName("getEvents -> fetches and returns events ")
    void getEvents_shouldReturnListOfEvents() {
        // arrange
        LedgerEvents event2 = new LedgerEvents();
        event2.setVersion(2);
        event2.setAggregateId("ACC100");

        when(walletRepository.findByAggregateIdOrderByVersionAsc("ACC100"))
                .thenReturn(List.of(sampleEvent, event2));

        // act
        List<LedgerEvents> result = walletService.getEvents("ACC100");
        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(1);
        assertThat(result.get(1).getVersion()).isEqualTo(2);
    }

    // event does not exist(getEvents)
    @Test
    @DisplayName("getEvents -> returns EventNotFound when events not exists")
    void getEvents_shouldReturnEventNotExists_whenListOfEventsIsEmpty() {
        when(walletRepository.findByAggregateIdOrderByVersionAsc("ACC999"))
                .thenReturn(List.of());
        assertThatThrownBy(() -> walletService.getEvents("ACC999"))
                .isInstanceOf(EventNotFound.class)
                .hasMessageContaining("ACC999");
    }

    // events exist after version (getEventsAfterVersion)
    @Test
    @DisplayName(
            "getEventsAfterVersion -> fetches and returns events after given version")
    void
    getEventsAfterVersion_shouldReturnEvents_afterCertainVersion() {
        LedgerEvents event2 = new LedgerEvents();
        event2.setVersion(2);
        event2.setAggregateId("ACC100");

        LedgerEvents event3 = new LedgerEvents();
        event3.setVersion(3);
        event3.setAggregateId("ACC100");

        when(walletRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc("ACC100",
                        1))
                .thenReturn(List.of(event2, event3));

        List<LedgerEvents> result =
                walletService.getEventsAfterVersion("ACC100", 1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
        assertThat(result.get(1).getVersion()).isEqualTo(3);
    }

    // no newer events exist (getEventsAfterVersion)
    @Test
    @DisplayName("getEventsAfterVersion -> returns empty list when no event " +
            "exists after given version")
    void
    getEventsAfterVersion_shouldReturnEmptyList_whenNoNewerEventsExist() {
        when(walletRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc("ACC100",
                        99))
                .thenReturn(List.of());

        List<LedgerEvents> result =
                walletService.getEventsAfterVersion("ACC100", 99);

        assertThat(result).isEmpty();
    }

    // has history( getCurrentVersion)
    @Test
    @DisplayName("getCurrentVersion -> fetches and returns current version " +
            "when events exist")
    void
    getCurrentVersion_shouldReturnCurrentVersion_whenEventExists() {
        LedgerEvents latestEvent = new LedgerEvents();
        latestEvent.setAggregateId("ACC100");
        latestEvent.setVersion(3);

        when(walletRepository.findFirstByAggregateIdOrderByVersionDesc("ACC100"))
                .thenReturn(Optional.of(latestEvent));

        Integer version = walletService.getCurrentVersion("ACC100");

        assertThat(version).isEqualTo(3);
    }

    // brand-new aggregate (getCurrentVersion)
    @Test
    @DisplayName(
            "getCurrentVersion -> returns 0 when aggregate has no events yet")
    void
    getCurrentVersion_shouldReturnZero_whenNoEventsExist() {
        when(walletRepository.findFirstByAggregateIdOrderByVersionDesc("ACC999"))
                .thenReturn(Optional.empty());

        Integer version = walletService.getCurrentVersion("ACC999");

        assertThat(version).isEqualTo(0);
    }

    // snapshot does not exist (getBalance)
    @Test
    @DisplayName("getBalance -> calculates balance from full history when no " +
            "snapshot exists ")
    void
    getBalance_shouldCalculateFromFullHistory_whenNoSnapshotExist() {
        // arrange
        String aggregateId = sampleEvent.getAggregateId();
        when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(
                aggregateId))
                .thenReturn(Optional.empty());

        LedgerEvents event1 = new LedgerEvents();
        event1.setPayload(new MoneyCredited(new BigDecimal("100"), "Deposit"));
        when(walletRepository.findByAggregateIdOrderByVersionAsc(aggregateId))
                .thenReturn(List.of(event1));
        // act
        BigDecimal result = walletService.getBalance(aggregateId);
        // assert
        assertThat(result).isEqualByComparingTo("100");
        verify(walletRepository, times(1))
                .findByAggregateIdOrderByVersionAsc(aggregateId);
        verify(walletRepository, never())
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(anyString(),
                        anyInt());
    }

    // snapshot exists (getBalance)
    @Test
    @DisplayName(
            "getBalance -> calculates balance from snapshots when snapshots exists")
    void
    getBalance_shouldCalculateBalanceFromSnapshots_whenSnapshotsExists() {
        // arrange
        String aggregateId = sampleEvent.getAggregateId();
        AccountSnapshots snapshot =
                new AccountSnapshots(aggregateId, 50, new BigDecimal("500"));
        when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(
                aggregateId))
                .thenReturn(Optional.of(snapshot));

        LedgerEvents deltaEvent = new LedgerEvents();
        deltaEvent.setPayload(new MoneyCredited(new BigDecimal("50"), "Deposit"));
        when(walletRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                        aggregateId, 50))
                .thenReturn(List.of(deltaEvent));
        // act
        BigDecimal balance = walletService.getBalance(aggregateId);
        // assert
        assertThat(balance).isEqualByComparingTo("550");
        verify(walletRepository, times(1))
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId,
                        50);
        verify(walletRepository, never())
                .findByAggregateIdOrderByVersionAsc(anyString());
    }
}
