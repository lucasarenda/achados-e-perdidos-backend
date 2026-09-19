package com.lostandfound.backend.dto.request;

import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Tipo é obrigatório (PERDIDO or ECONTRADO)")
    private ItemType type;

    @NotNull(message = "Categoria é obrigatório")
    private ItemCategory category;

    private String color;

    @NotNull(message = "A data em que o item foi perdido/encontrado é obrigatório")
    private LocalDate occurredAt;


    private Double latitude;
    private Double longitude;


    private String locationAddress;

    private String locationDescription;
}
