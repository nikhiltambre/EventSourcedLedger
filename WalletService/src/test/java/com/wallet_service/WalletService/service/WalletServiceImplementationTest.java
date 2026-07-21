package com.wallet_service.WalletService.service;

import com.wallet_service.WalletService.exception.EventAlreadyExists;
import com.wallet_service.WalletService.exception.EventNotFound;
import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplementationTest {
    @Mock
    private WalletRepository walletRepository;
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

    //version does not exists -> saves
    @Test
    @DisplayName("appendEvent -> saves and returns event when slot is free")
    void appendEvent_shouldSaveAndReturnEvent_whenVersionSlotIsFree() {
        //arrange
        LedgerEvents tempEvent = new LedgerEvents();
        tempEvent.setAggregateId("ACC100");
        tempEvent.setVersion(2);
        tempEvent.setEventType("AccountOpened");

        when(walletRepository.existsByAggregateIdAndVersion("AC100", 2)).thenReturn(false);
        when(walletRepository.save(any())).thenReturn(tempEvent);
        //act
        LedgerEvents result = walletService.appendEvent(sampleEvent);
        //assert
        assertThat(result).isNotNull();
        assertThat(result.getAggregateId()).isEqualTo("ACC100");
        assertThat(result.getVersion()).isEqualTo(2);
        verify(walletRepository, times(1)).save(result);

    }

    //version does exists -> throws EventAlreadyExists
    @Test
    @DisplayName("appendEvent -> throws EventAlreadyExists when slot is taken ")
    void appendEvent_showThrowEventAlreadyExists_whenSlotIsTaken() {
        when(walletRepository.existsByAggregateIdAndVersion("ACC100", 2)).thenReturn(true);
        assertThatThrownBy(() -> walletService.appendEvent(sampleEvent))
                .isInstanceOf(EventAlreadyExists.class)
                .hasMessageContaining("ACC100");
    }


    //event exists
    @Test
    @DisplayName("getEvents -> fetches and returns event ")
    void getEvents_shouldReturnListOfEvents() {
        //arrange
        LedgerEvents event2 = new LedgerEvents();
        event2.setVersion(2);
        event2.setAggregateId("ACC100");

        when(walletRepository.findByAggregateIdOrderByVersionAsc("ACC100"))
                .thenReturn(List.of(sampleEvent, event2));

        //act
        List<LedgerEvents> result = walletService.getEvents("ACC100");
        //assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(1);
        assertThat(result.get(1).getVersion()).isEqualTo(2);
    }

    //event does not exist
    @Test
    @DisplayName("getEvents -> returns EventNotFound when event not exists")
    void getEvents_shouldReturnEventNotExists_whenListOfEventsIsEmpty() {
        when(walletRepository.findByAggregateIdOrderByVersionAsc("ACC999"))
                .thenReturn(List.of());
        assertThatThrownBy(() -> walletService.getEvents("ACC999"))
                .isInstanceOf(EventNotFound.class)
                .hasMessage("ACC999");
    }


}

