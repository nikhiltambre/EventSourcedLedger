package com.wallet_service.WalletService.model.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MoneyDebited.class, name = "MoneyDebited"),
        @JsonSubTypes.Type(value = MoneyCredited.class, name = "MoneyCredited")
})
public interface Event {

}
