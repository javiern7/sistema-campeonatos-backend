package com.multideporte.backend.player.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PlayerCreateRequest(
        @NotBlank(message = "firstName es obligatorio")
        @Size(max = 100, message = "firstName no puede superar 100 caracteres")
        String firstName,

        @NotBlank(message = "lastName es obligatorio")
        @Size(max = 100, message = "lastName no puede superar 100 caracteres")
        String lastName,

        @Size(max = 20, message = "documentType no puede superar 20 caracteres")
        String documentType,

        @Size(max = 30, message = "documentNumber no puede superar 30 caracteres")
        String documentNumber,

        @PastOrPresent(message = "birthDate no puede estar en el futuro")
        LocalDate birthDate,

        @Email(message = "email debe tener formato valido")
        @Size(max = 150, message = "email no puede superar 150 caracteres")
        String email,

        @Size(max = 30, message = "phone no puede superar 30 caracteres")
        String phone,

        @NotNull(message = "active es obligatorio")
        Boolean active
) {
}
