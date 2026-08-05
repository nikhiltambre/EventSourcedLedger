package com.command_service.WalletCommandService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.command_service.WalletCommandService.exception.EventAlreadyExists;
import com.command_service.WalletCommandService.exception.EventNotFound;
import com.command_service.WalletCommandService.model.entries.AccountSnapshots;
import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.model.events.MoneyCredited;
import com.command_service.WalletCommandService.repository.EventRepository;
import com.command_service.WalletCommandService.repository.SnapshotRepository;

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
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
public class CommandServiceImplementationTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<BigDecimal> rBucket;

    @Mock
    private RTopic rTopic;
    @InjectMocks
    private CommandServiceImplementation commandService;

    private LedgerEvents sampleEvent;

    @BeforeEach
    void setup() {
        sampleEvent = new LedgerEvents();
        sampleEvent = new LedgerEvents();
        sampleEvent.setAggregateId("ACC100");
        sampleEvent.setVersion(1);
        sampleEvent.setEventType("AccountOpened");
        sampleEvent.setTraceId("trace-001");
        lenient()
                .when(redissonClient.<BigDecimal>getBucket(anyString()))
                .thenReturn(rBucket);
        lenient().when(redissonClient.getTopic(anyString())).thenReturn(rTopic);
        lenient().when(rBucket.get()).thenReturn(null);
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

        when(eventRepository.existsByAggregateIdAndVersion("ACC100", 2))
                .thenReturn(false);
        when(eventRepository.save(any())).thenReturn(tempEvent);
        // act
        LedgerEvents result = commandService.appendEvent(sampleEvent, "").getData();
        // assert
        assertThat(result).isNotNull();
        assertThat(result.getAggregateId()).isEqualTo("ACC100");
        assertThat(result.getVersion()).isEqualTo(2);
        ArgumentCaptor<LedgerEvents> eventCaptor =
                ArgumentCaptor.forClass(LedgerEvents.class);
        verify(eventRepository, times(1)).save(eventCaptor.capture());
        LedgerEvents capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getAggregateId()).isEqualTo("ACC100");
        assertThat(capturedEvent.getVersion()).isEqualTo(2);
        assertThat(capturedEvent.getEventType()).isEqualTo("AccountOpened");
        verify(rBucket, times(1)).delete();
        verify(rTopic, times(1)).publish("ACC100");
    }

    // version does exists -> throws EventAlreadyExists
    @Test
    @DisplayName("appendEvent -> throws EventAlreadyExists when slot is taken ")
    void appendEvent_showThrowEventAlreadyExists_whenSlotIsTaken() {
        when(eventRepository.existsByAggregateIdAndVersion("ACC100", 2))
                .thenReturn(true);
        assertThatThrownBy(() -> commandService.appendEvent(sampleEvent, ""))
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

        when(eventRepository.findByAggregateIdOrderByVersionAsc("ACC100"))
                .thenReturn(List.of(sampleEvent, event2));

        // act
        List<LedgerEvents> result = commandService.getEvents("ACC100");
        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(1);
        assertThat(result.get(1).getVersion()).isEqualTo(2);
    }

    // event does not exist(getEvents)
    @Test
    @DisplayName("getEvents -> returns EventNotFound when events not exists")
    void getEvents_shouldReturnEventNotExists_whenListOfEventsIsEmpty() {
        when(eventRepository.findByAggregateIdOrderByVersionAsc("ACC999"))
                .thenReturn(List.of());
        assertThatThrownBy(() -> commandService.getEvents("ACC999"))
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

        when(
                eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                        "ACC100", 1))
                .thenReturn(List.of(event2, event3));

        List<LedgerEvents> result =
                commandService.getEventsAfterVersion("ACC100", 1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
        assertThat(result.get(1).getVersion()).isEqualTo(3);
    }

    // no newer events exist (getEventsAfterVersion)
    @Test
    @DisplayName("getEventsAfterVersion -> returns empty list when no event "
            + "exists after given version")
    void
    getEventsAfterVersion_shouldReturnEmptyList_whenNoNewerEventsExist() {
        when(
                eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                        "ACC100", 99))
                .thenReturn(List.of());

        List<LedgerEvents> result =
                commandService.getEventsAfterVersion("ACC100", 99);

        assertThat(result).isEmpty();
    }

    // snapshot does not exist (getBalance)
    @Test
    @DisplayName("getBalance -> calculates balance from full history when no "
            + "snapshot exists ")
    void
    getBalance_shouldCalculateFromFullHistory_whenNoSnapshotExist() {
        when(redissonClient.<BigDecimal>getBucket("balance:ACC100"))
                .thenReturn(rBucket);
        when(rBucket.get()).thenReturn(null); // cache miss
        // arrange
        String aggregateId = sampleEvent.getAggregateId();
        when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(
                aggregateId))
                .thenReturn(Optional.empty());

        LedgerEvents event1 = new LedgerEvents();
        event1.setPayload(new MoneyCredited(new BigDecimal("100"), "Deposit"));
        when(eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId))
                .thenReturn(List.of(event1));
        // act
        BigDecimal result = commandService.getBalance(aggregateId);
        // assert
        assertThat(result).isEqualByComparingTo("100");
        verify(eventRepository, times(1))
                .findByAggregateIdOrderByVersionAsc(aggregateId);
        verify(eventRepository, never())
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(anyString(),
                        anyInt());
        verify(rBucket).set(eq(result), anyLong(), any());
    }

    // snapshot exists (getBalance)
    @Test
    @DisplayName(
            "getBalance -> calculates balance from snapshots when snapshots exists")
    void
    getBalance_shouldCalculateBalanceFromSnapshots_whenSnapshotsExists() {
        // arrange
        when(redissonClient.<BigDecimal>getBucket("balance:ACC100"))
                .thenReturn(rBucket);
        when(rBucket.get()).thenReturn(null);

        String aggregateId = sampleEvent.getAggregateId();
        AccountSnapshots snapshot =
                new AccountSnapshots(aggregateId, 50, new BigDecimal("500"));
        when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(
                aggregateId))
                .thenReturn(Optional.of(snapshot));

        LedgerEvents deltaEvent = new LedgerEvents();
        deltaEvent.setPayload(new MoneyCredited(new BigDecimal("50"), "Deposit"));
        when(
                eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                        aggregateId, 50))
                .thenReturn(List.of(deltaEvent));
        // act
        BigDecimal result = commandService.getBalance(aggregateId);
        // assert
        assertThat(result).isEqualByComparingTo("550");
        verify(eventRepository, times(1))
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId,
                        50);
        verify(eventRepository, never())
                .findByAggregateIdOrderByVersionAsc(anyString());
        verify(rBucket).set(eq(result), anyLong(), any());
    }
}
