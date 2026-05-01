package com.udea.bancodigital.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainEvent implements Serializable {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("aggregate_id")
    private String aggregateId;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("saga_id")
    private String sagaId;

    @JsonProperty("occurred_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    @JsonProperty("source_service")
    private String sourceService;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("user_id")
    private String userId;
}
