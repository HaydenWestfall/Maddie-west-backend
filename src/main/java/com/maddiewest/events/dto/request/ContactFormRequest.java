package com.maddiewest.rentalservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public website contact-form payload. Mirrors the contract of the standalone contact-api:
 * only {@code name} and {@code email} are required; any other string field the form posts is
 * captured into {@link #additionalFields} and forwarded into the notification email, so the
 * form can evolve without backend changes.
 */
@Getter
@Setter
public class ContactFormRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @Size(max = 2000)
    private String phone;

    @Size(max = 2000)
    private String message;

    private final Map<String, String> additionalFields = new LinkedHashMap<>();

    /** Collects any field beyond the named ones above, mirroring contact-api's dynamic forwarding. */
    @JsonAnySetter
    public void addAdditionalField(String key, Object value) {
        if (value != null) {
            additionalFields.put(key, value.toString());
        }
    }
}
