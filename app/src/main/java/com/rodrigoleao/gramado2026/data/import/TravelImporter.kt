package com.rodrigoleao.gramado2026.data.import_trip

import android.content.Context
import android.net.Uri
import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity
import com.rodrigoleao.gramado2026.data.db.entity.WalkStopEntity
import com.rodrigoleao.gramado2026.data.model.BadgeType
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

// ── Estruturas de dados do export ─────────────────────────────────────────────

private data class ExportedTrip(
    val schemaVersion: Int,
    val name: String,
    val destination: String,
    val coverEmoji: String,
    val startDate: String?,
    val endDate: String?,
    val latitude: Double?,
    val longitude: Double?,
    val hotelName: String,
    val hotelAddress: String,
    val hotelPhone: String,
    val days: List<ExportedDay>,
    val contacts: List<ExportedContact>,
    val vouchers: List<ExportedVoucher>,
    val boardingPasses: List<ExportedBoardingPass>
)

private data class ExportedVoucher(
    val emoji: String,
    val groupName: String,
    val name: String,
    val person: String?,
    val assetPath: String,
    val dayId: Int?
)

private data class ExportedBoardingPass(
    val origin: String,
    val originCity: String,
    val destination: String,
    val destinationCity: String,
    val flightNumber: String,
    val date: String,
    val boardingTime: String,
    val passenger: String,
    val walletUrl: String?
)

private data class ExportedDay(
    val dayNumber: Int,
    val title: String,
    val dayAlert: String?,
    val linkUrl: String?,
    val linkLabel: String,
    val documentName: String?,
    val activities: List<ExportedActivity>
)

private data class ExportedWalkStop(
    val order: Int,
    val name: String,
    val detail: String,
    val emoji: String,
    val isLast: Boolean
)

private data class ExportedActivity(
    val position: Int,
    val time: String,
    val emoji: String,
    val name: String,
    val detail: String,
    val address: String?,
    val badges: List<ExportedBadge>,
    val walkStops: List<ExportedWalkStop>
)

private data class ExportedBadge(
    val type: String,
    val label: String,
    val color: String?
)

private data class ExportedContact(
    val name: String,
    val role: String,
    val phone: String?,
    val type: String,
    val hasWhatsApp: Boolean,
    val isEmergency: Boolean
)

// ── Importer ──────────────────────────────────────────────────────────────────

class TravelImporter(
    private val context: Context,
    private val repo: TripRepository
) {

    /** Importa a viagem completa para o banco e retorna o novo tripId. */
    suspend fun import(uri: Uri): Long {
        val (exported, documents, voucherFiles) = parseZip(uri)

        if (exported.startDate == null || exported.endDate == null)
            throw Exception("Arquivo inválido: datas ausentes.")

        // Cria a viagem e os dias no banco
        val tripId = repo.createTrip(
            name         = exported.name,
            destination  = exported.destination,
            coverEmoji   = exported.coverEmoji,
            startDate    = exported.startDate,
            endDate      = exported.endDate,
            latitude     = exported.latitude,
            longitude    = exported.longitude,
            hotelName    = exported.hotelName,
            hotelAddress = exported.hotelAddress,
            hotelPhone   = exported.hotelPhone
        )

        // Copia documentos para filesDir/Arquivos/
        val arquivosDir = File(context.filesDir, "Arquivos").apply { mkdirs() }
        val docPaths    = mutableMapOf<String, String>() // documentName → localPath
        documents.forEach { (name, bytes) ->
            val dest = File(arquivosDir, name)
            dest.writeBytes(bytes)
            docPaths[name] = dest.absolutePath
        }

        // Atualiza cada dia com título, alerta, link e atividades
        for (expDay in exported.days) {
            val dayEntity = repo.getDayEntity(tripId, expDay.dayNumber) ?: continue

            val docPath = expDay.documentName?.let { docPaths[it] }
            val docName = expDay.documentName

            repo.updateDay(dayEntity.copy(
                title            = expDay.title,
                dayAlert         = expDay.dayAlert,
                dayLinkUrl       = expDay.linkUrl,
                dayLinkLabel     = expDay.linkLabel,
                dayDocumentPath  = docPath,
                dayDocumentName  = docName ?: ""
            ))

            for (expAct in expDay.activities.sortedBy { it.position }) {
                val actId = repo.upsertActivity(
                    dayEntityId = dayEntity.id,
                    entity      = TravelActivityEntity(
                        dayId           = dayEntity.id,
                        position        = expAct.position,
                        time            = expAct.time,
                        emoji           = expAct.emoji,
                        name            = expAct.name,
                        detail          = expAct.detail,
                        mapQuery        = expAct.address,
                        uberDestination = expAct.address
                    ),
                    badges = expAct.badges.mapNotNull { b ->
                        val type = runCatching { BadgeType.valueOf(b.type) }.getOrNull()
                            ?: return@mapNotNull null
                        ActivityBadgeEntity(
                            activityId = 0L,
                            badgeType  = type.name,
                            label      = b.label,
                            color      = b.color
                        )
                    }
                )
                expAct.walkStops.forEach { stop ->
                    repo.insertWalkStop(WalkStopEntity(
                        activityId = actId,
                        position   = stop.order,
                        emoji      = stop.emoji,
                        label      = stop.name,
                        sublabel   = stop.detail.takeIf { it.isNotBlank() },
                        isLast     = stop.isLast
                    ))
                }
            }
        }

        // Importa contatos
        for (expContact in exported.contacts) {
            val type = runCatching {
                com.rodrigoleao.gramado2026.data.model.ContactType.valueOf(expContact.type)
            }.getOrDefault(com.rodrigoleao.gramado2026.data.model.ContactType.AGENCY)

            repo.upsertContact(
                tripId = tripId,
                entity = ContactEntity(
                    tripId      = tripId,
                    name        = expContact.name,
                    role        = expContact.role,
                    phone       = expContact.phone,
                    contactType = type.name,
                    hasWhatsApp = expContact.hasWhatsApp,
                    isEmergency = expContact.isEmergency
                )
            )
        }

        // Importa vouchers
        val vouchersDir = File(context.filesDir, "Vouchers").apply { mkdirs() }
        for (expVoucher in exported.vouchers) {
            val assetPath = expVoucher.assetPath.trimStart('/')
            // Salva o arquivo se existir no ZIP; caso contrário mantém o assetPath original
            val localPath = voucherFiles[assetPath]?.let { bytes ->
                val dest = File(vouchersDir, assetPath).apply { parentFile?.mkdirs() }
                dest.writeBytes(bytes)
                dest.absolutePath
            } ?: expVoucher.assetPath

            repo.upsertVoucher(
                tripId = tripId,
                entity = VoucherEntity(
                    tripId    = tripId,
                    dayNumber = expVoucher.dayId,
                    emoji     = expVoucher.emoji,
                    groupName = expVoucher.groupName,
                    name      = expVoucher.name,
                    person    = expVoucher.person,
                    assetPath = localPath
                )
            )
        }

        // Importa boarding passes
        for (expPass in exported.boardingPasses) {
            repo.upsertBoardingPass(
                tripId = tripId,
                entity = BoardingPassEntity(
                    tripId          = tripId,
                    origin          = expPass.origin,
                    originCity      = expPass.originCity,
                    destination     = expPass.destination,
                    destinationCity = expPass.destinationCity,
                    flightNumber    = expPass.flightNumber,
                    date            = expPass.date,
                    boardingTime    = expPass.boardingTime,
                    passenger       = expPass.passenger,
                    walletUrl       = expPass.walletUrl
                )
            )
        }

        return tripId
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    /** Retorna (ExportedTrip, documentos, vouchers como Map<path, bytes>). */
    private fun parseZip(uri: Uri): Triple<ExportedTrip, Map<String, ByteArray>, Map<String, ByteArray>> {
        var exported: ExportedTrip? = null
        val documents = mutableMapOf<String, ByteArray>()
        val voucherFiles = mutableMapOf<String, ByteArray>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "trip.json" -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            exported = parseTripJson(json)
                        }
                        entry.name.startsWith("documents/") && !entry.isDirectory -> {
                            val name = entry.name.removePrefix("documents/")
                            if (name.isNotBlank()) documents[name] = zip.readBytes()
                        }
                        entry.name.startsWith("vouchers/") && !entry.isDirectory -> {
                            val path = entry.name.removePrefix("vouchers/")
                            if (path.isNotBlank()) voucherFiles[path] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw Exception("Não foi possível ler o arquivo.")

        return Triple(
            exported ?: throw Exception("Arquivo .travel inválido: trip.json não encontrado."),
            documents,
            voucherFiles
        )
    }

    private fun parseTripJson(json: String): ExportedTrip {
        val root      = JSONObject(json)
        val schemaVer = root.optInt("schemaVersion", 1)
        if (schemaVer > SUPPORTED_SCHEMA_VERSION)
            throw Exception("Este arquivo foi criado por uma versão mais recente do app. Atualize o app para importá-lo.")

        val trip  = root.getJSONObject("trip")
        val hotel = trip.optJSONObject("hotel")

        val daysArray = trip.getJSONArray("days")
        val days = (0 until daysArray.length()).map { i ->
            val day  = daysArray.getJSONObject(i)
            val acts = day.getJSONArray("activities")
            ExportedDay(
                dayNumber    = day.getInt("dayNumber"),
                title        = day.getString("title"),
                dayAlert     = day.optString("dayAlert").takeIf { it.isNotBlank() && it != "null" },
                linkUrl      = day.optString("linkUrl").takeIf   { it.isNotBlank() && it != "null" },
                linkLabel    = day.optString("linkLabel", ""),
                documentName = day.optString("documentName").takeIf { it.isNotBlank() && it != "null" },
                activities   = (0 until acts.length()).map { j ->
                    val act    = acts.getJSONObject(j)
                    val badges = act.optJSONArray("badges")
                    val stops  = act.optJSONArray("walkStops")
                    ExportedActivity(
                        position  = act.optInt("position", j),
                        time      = act.optString("time", ""),
                        emoji     = act.optString("emoji", "📍"),
                        name      = act.getString("name"),
                        detail    = act.optString("detail", ""),
                        address   = act.optString("address").takeIf { it.isNotBlank() && it != "null" },
                        badges    = if (badges != null)
                            (0 until badges.length()).map { k ->
                                val b = badges.getJSONObject(k)
                                ExportedBadge(
                                    type  = b.getString("type"),
                                    label = b.optString("label", b.getString("type")),
                                    color = b.optString("color").takeIf { it.isNotBlank() && it != "null" }
                                )
                            } else emptyList(),
                        walkStops = if (stops != null)
                            (0 until stops.length()).map { k ->
                                val s = stops.getJSONObject(k)
                                ExportedWalkStop(
                                    order  = s.optInt("order", k + 1),
                                    name   = s.getString("name"),
                                    detail = s.optString("detail", ""),
                                    emoji  = s.optString("emoji", "📍"),
                                    isLast = s.optBoolean("isLast", false)
                                )
                            } else emptyList()
                    )
                }
            )
        }

        val contactsArray = trip.optJSONArray("contacts")
        val contacts = if (contactsArray != null)
            (0 until contactsArray.length()).map { i ->
                val c = contactsArray.getJSONObject(i)
                ExportedContact(
                    name        = c.getString("name"),
                    role        = c.optString("role", ""),
                    phone       = c.optString("phone").takeIf { it.isNotBlank() && it != "null" },
                    type        = c.optString("type", "AGENCY"),
                    hasWhatsApp = c.optBoolean("hasWhatsApp", false),
                    isEmergency = c.optBoolean("isEmergency", false)
                )
            } else emptyList()

        val vouchersJsonArray = trip.optJSONArray("vouchers")
        val vouchers = if (vouchersJsonArray != null)
            (0 until vouchersJsonArray.length()).map { i ->
                val v = vouchersJsonArray.getJSONObject(i)
                ExportedVoucher(
                    emoji     = v.optString("emoji", "🎟️"),
                    groupName = v.optString("groupName", ""),
                    name      = v.getString("name"),
                    person    = v.optString("person").takeIf { it.isNotBlank() && it != "null" },
                    assetPath = v.getString("assetPath"),
                    dayId     = if (v.isNull("dayId")) null else v.optInt("dayId")
                )
            } else emptyList()

        val boardingJsonArray = trip.optJSONArray("boardingPasses")
        val boardingPasses = if (boardingJsonArray != null)
            (0 until boardingJsonArray.length()).map { i ->
                val p = boardingJsonArray.getJSONObject(i)
                ExportedBoardingPass(
                    origin          = p.getString("origin"),
                    originCity      = p.getString("originCity"),
                    destination     = p.getString("destination"),
                    destinationCity = p.getString("destinationCity"),
                    flightNumber    = p.getString("flightNumber"),
                    date            = p.getString("date"),
                    boardingTime    = p.getString("boardingTime"),
                    passenger       = p.getString("passenger"),
                    walletUrl       = p.optString("walletUrl").takeIf { it.isNotBlank() && it != "null" }
                )
            } else emptyList()

        return ExportedTrip(
            schemaVersion = schemaVer,
            name          = trip.getString("name"),
            destination   = trip.getString("destination"),
            coverEmoji    = trip.optString("coverEmoji", "✈️"),
            startDate     = trip.optString("startDate").takeIf { it.isNotBlank() && it != "null" },
            endDate       = trip.optString("endDate").takeIf   { it.isNotBlank() && it != "null" },
            latitude      = if (trip.isNull("latitude")) null else trip.optDouble("latitude"),
            longitude     = if (trip.isNull("longitude")) null else trip.optDouble("longitude"),
            hotelName     = hotel?.optString("name", "") ?: "",
            hotelAddress  = hotel?.optString("address", "") ?: "",
            hotelPhone    = hotel?.optString("phone", "") ?: "",
            days          = days,
            contacts      = contacts,
            vouchers      = vouchers,
            boardingPasses = boardingPasses
        )
    }

    companion object {
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
