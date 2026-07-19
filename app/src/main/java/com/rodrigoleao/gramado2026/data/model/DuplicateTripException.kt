package com.rodrigoleao.gramado2026.data.model

/**
 * Lançada pelo TravelImporter quando o `.travel` sendo importado tem um `tripUuid`
 * que já existe no banco (F1 — detecção de duplicata).
 *
 * O ViewModel captura e monta o diálogo de conflito, comparando os timestamps de
 * última edição. Se o usuário confirmar, chama `TravelImporter.overwriteImport()`.
 */
class DuplicateTripException(
    val existingTripId: Long,
    val existingTripName: String,
    val existingLastEditedAt: Long,
    val incomingLastEditedAt: Long
) : Exception("Viagem já importada: $existingTripName")
