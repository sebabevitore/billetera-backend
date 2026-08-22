package billetera.project.dto;

import billetera.project.model.TipoTransaccion;

public record CategoriaResponseDTO(
    Long id,
    String nombre,
    Long usuarioId,
    TipoTransaccion tipo,
    String icono
) {}
