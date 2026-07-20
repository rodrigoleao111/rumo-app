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
import com.rodrigoleao.gramado2026.data.model.ChecklistItem
import com.rodrigoleao.gramado2026.data.model.DuplicateTripException
import com.rodrigoleao.gramado2026.data.model.Note
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import com.rodrigoleao.gramado2026.data.repository.ActivityRepository
import com.rodrigoleao.gramado2026.data.repository.BoardingPassRepository
import com.rodrigoleao.gramado2026.data.repository.ContactRepository
import com.rodrigoleao.gramado2026.data.repository.DayRepository
import com.rodrigoleao.gramado2026.data.repository.NoteRepository
import com.rodrigoleao.gramado2026.data.repository.TripData
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.data.repository.VoucherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── Estruturas de dados do export ─────────────────────────────────────────────

private data class ExportedTrip(
    val schemaVersion: Int,
    val tripUuid: String,          // F1 — vazio em arquivos v1
    val lastEditedAt: Long,        // F1 — 0 em arquivos v1
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
    val voucherSortMode: String,
    val days: List<ExportedDay>,
    val contacts: List<ExportedContact>,
    val vouchers: List<ExportedVoucher>,
    val boardingPasses: List<ExportedBoardingPass>,
    val notes: List<Note>          // F4 — domain models prontos p/ inserção
)

private data class ExportedVoucher(
    val emoji: String,
    val groupName: String,
    val name: String,
    val person: String?,
    val assetPath: String,
    val dayId: Int?,
    val sortOrder: Int,
    val isUsed: Boolean
)

private data class ExportedBoardingPass(
    val transportType: String,
    val origin: String,
    val originCity: String,
    val destination: String,
    val destinationCity: String,
    val flightNumber: String,
    val date: String,
    val boardingTime: String,
    val passenger: String,
    val walletUrl: String?,
    val documentName: String,
    val notes: String
)

private data class ExportedDay(
    val dayNumber: Int,
    val title: String,
    val dayAlert: String?,
    val linkUrl: String?,
    val linkLabel: String,
    val documentName: String?,
    val documentTitle: String,
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
    val isEmergency: Boolean,
    val customTypeName: String = "",
    val sortOrder: Int,
    val isFavorite: Boolean
)

// ── Importer ──────────────────────────────────────────────────────────────────

@Singleton
class TravelImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tripRepo: TripRepository,
    private val dayRepo: DayRepository,
    private val activityRepo: ActivityRepository,
    private val contactRepo: ContactRepository,
    private val voucherRepo: VoucherRepository,
    private val boardingPassRepo: BoardingPassRepository,
    private val noteRepo: NoteRepository
) {

    /**
     * Importa a viagem como uma nova entrada no banco e retorna o novo tripId.
     *
     * F1: se o `tripUuid` do arquivo já existir no banco, lança [DuplicateTripException]
     * (o ViewModel decide entre manter local e sobrescrever via [overwriteImport]).
     * UUID vazio (arquivos v1) nunca casa — importação normal.
     */
    suspend fun import(uri: Uri): Long {
        val parsed = parseZip(uri)
        val exported = parsed.trip

        tripRepo.findByUuid(exported.tripUuid)?.let { existing ->
            throw DuplicateTripException(
                existingTripId       = existing.id,
                existingTripName     = existing.name,
                existingLastEditedAt = existing.lastEditedAt,
                incomingLastEditedAt = exported.lastEditedAt
            )
        }

        return writeToDb(parsed)
    }

    /**
     * F1: sobrescreve a viagem local `existingTripId` pelo conteúdo de `uri`.
     *
     * Ordem deliberada (mais segura que delete-antes-de-inserir): importa a nova
     * viagem PRIMEIRO; só depois remove a antiga. Se a importação falhar, a viagem
     * local permanece intacta (UC-F1-10). Ao final, remove do disco apenas os
     * arquivos da viagem antiga que a nova não reutiliza.
     */
    suspend fun overwriteImport(uri: Uri, existingTripId: Long): Long {
        val oldPaths = collectManagedFilePaths(tripRepo.getTripData(existingTripId))

        val newTripId = writeToDb(parseZip(uri))   // pode lançar → antiga preservada

        tripRepo.getTripEntity(existingTripId)?.let { tripRepo.deleteTrip(it) }

        val newPaths = collectManagedFilePaths(tripRepo.getTripData(newTripId))
        (oldPaths - newPaths).forEach { runCatching { File(it).delete() } }

        return newTripId
    }

    /** Insere a viagem parseada como nova entrada. Não faz detecção de duplicata. */
    private suspend fun writeToDb(parsed: ParsedZip): Long {
        val exported     = parsed.trip
        val documents    = parsed.documents
        val voucherFiles = parsed.voucherFiles
        val boardingFiles = parsed.boardingFiles

        if (exported.startDate == null || exported.endDate == null)
            throw Exception("Arquivo inválido: datas ausentes.")

        // Cria a viagem e os dias no banco.
        // F1: preserva UUID/timestamp do arquivo (v2); arquivos v1 (uuid vazio) geram novo UUID.
        val tripId = tripRepo.createTrip(
            name         = exported.name,
            destination  = exported.destination,
            coverEmoji   = exported.coverEmoji,
            startDate    = exported.startDate,
            endDate      = exported.endDate,
            latitude     = exported.latitude,
            longitude    = exported.longitude,
            hotelName    = exported.hotelName,
            hotelAddress = exported.hotelAddress,
            hotelPhone   = exported.hotelPhone,
            tripUuid     = exported.tripUuid.ifBlank { null },
            lastEditedAt = exported.lastEditedAt.takeIf { it > 0L }
        )

        // Grava preferência de agrupamento de vouchers
        if (exported.voucherSortMode != "BY_CATEGORY") {
            tripRepo.saveVoucherSortMode(tripId, exported.voucherSortMode)
        }

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
            val dayEntity = dayRepo.getDayEntity(tripId, expDay.dayNumber) ?: continue

            val docPath = expDay.documentName?.let { docPaths[it] }
            val docName = expDay.documentName

            dayRepo.updateDay(dayEntity.copy(
                title             = expDay.title,
                dayAlert          = expDay.dayAlert,
                dayLinkUrl        = expDay.linkUrl,
                dayLinkLabel      = expDay.linkLabel,
                dayDocumentPath   = docPath,
                dayDocumentName   = docName ?: "",
                dayDocumentTitle  = expDay.documentTitle
            ))

            for (expAct in expDay.activities.sortedBy { it.position }) {
                val actId = activityRepo.upsertActivity(
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
                    activityRepo.insertWalkStop(WalkStopEntity(
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
            val isBuiltinType = com.rodrigoleao.gramado2026.data.model.ContactType.entries
                .any { it.name == expContact.type && it != com.rodrigoleao.gramado2026.data.model.ContactType.CUSTOM }

            contactRepo.upsertContact(
                tripId = tripId,
                entity = ContactEntity(
                    tripId         = tripId,
                    name           = expContact.name,
                    role           = expContact.role,
                    phone          = expContact.phone,
                    contactType    = if (isBuiltinType) expContact.type else com.rodrigoleao.gramado2026.data.model.ContactType.CUSTOM.name,
                    customTypeName = if (isBuiltinType) "" else expContact.customTypeName,
                    hasWhatsApp    = expContact.hasWhatsApp,
                    isEmergency    = expContact.isEmergency,
                    sortOrder      = expContact.sortOrder,
                    isFavorite     = expContact.isFavorite
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

            voucherRepo.upsertVoucher(
                tripId = tripId,
                entity = VoucherEntity(
                    tripId    = tripId,
                    dayNumber = expVoucher.dayId,
                    emoji     = expVoucher.emoji,
                    groupName = expVoucher.groupName,
                    name      = expVoucher.name,
                    person    = expVoucher.person,
                    assetPath = localPath,
                    sortOrder = expVoucher.sortOrder,
                    isUsed    = expVoucher.isUsed
                )
            )
        }

        // Importa boarding passes
        val passagensDir = File(context.filesDir, "Passagens").apply { mkdirs() }
        for (expPass in exported.boardingPasses) {
            val localDocPath = if (expPass.documentName.isNotBlank()) {
                boardingFiles[expPass.documentName]?.let { bytes ->
                    val dest = File(passagensDir, expPass.documentName)
                    dest.writeBytes(bytes)
                    dest.absolutePath
                }
            } else null

            boardingPassRepo.upsertBoardingPass(
                tripId = tripId,
                entity = BoardingPassEntity(
                    tripId          = tripId,
                    transportType   = expPass.transportType,
                    origin          = expPass.origin,
                    originCity      = expPass.originCity,
                    destination     = expPass.destination,
                    destinationCity = expPass.destinationCity,
                    flightNumber    = expPass.flightNumber,
                    date            = expPass.date,
                    boardingTime    = expPass.boardingTime,
                    passenger       = expPass.passenger,
                    walletUrl       = expPass.walletUrl,
                    documentPath    = localDocPath,
                    documentName    = expPass.documentName,
                    notes           = expPass.notes
                )
            )
        }

        // Importa notas (F4) — cada nota preserva dayId, blocos, itens e timestamps
        for (note in exported.notes) {
            noteRepo.insertImportedNote(tripId, note)
        }

        return tripId
    }

    // Coleta os caminhos de arquivo gerenciados pelo app (dentro de filesDir) que
    // pertencem a uma viagem — documentos de dia, vouchers salvos e docs de passagem.
    // Ignora URLs/links e caminhos fora do sandbox, para nunca apagar algo externo.
    private fun collectManagedFilePaths(data: TripData?): Set<String> {
        if (data == null) return emptySet()
        val base = context.filesDir.absolutePath
        val paths = mutableSetOf<String>()
        data.days.forEach { d -> d.dayDocumentPath?.let { if (it.startsWith(base)) paths += it } }
        data.vouchers.forEach { v -> if (v.assetPath.startsWith(base)) paths += v.assetPath }
        data.boardingPasses.forEach { p -> p.documentPath?.let { if (it.startsWith(base)) paths += it } }
        return paths
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    /** Retorna (ExportedTrip, documentos, vouchers, arquivos de boarding como Map<name, bytes>). */
    private fun parseZip(uri: Uri): ParsedZip {
        var exported: ExportedTrip? = null
        val documents    = mutableMapOf<String, ByteArray>()
        val voucherFiles = mutableMapOf<String, ByteArray>()
        val boardingFiles = mutableMapOf<String, ByteArray>()

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
                        entry.name.startsWith("boarding/") && !entry.isDirectory -> {
                            val name = entry.name.removePrefix("boarding/")
                            if (name.isNotBlank()) boardingFiles[name] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw Exception("Não foi possível ler o arquivo.")

        return ParsedZip(
            exported ?: throw Exception("Arquivo .travel inválido: trip.json não encontrado."),
            documents,
            voucherFiles,
            boardingFiles
        )
    }

    private data class ParsedZip(
        val trip: ExportedTrip,
        val documents: Map<String, ByteArray>,
        val voucherFiles: Map<String, ByteArray>,
        val boardingFiles: Map<String, ByteArray>
    )

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
                documentName  = day.optString("documentName").takeIf { it.isNotBlank() && it != "null" },
                documentTitle = day.optString("documentTitle").takeIf { it.isNotBlank() && it != "null" } ?: "",
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
                    name           = c.getString("name"),
                    role           = c.optString("role", ""),
                    phone          = c.optString("phone").takeIf { it.isNotBlank() && it != "null" },
                    type           = c.optString("type", "AGENCY"),
                    hasWhatsApp    = c.optBoolean("hasWhatsApp", false),
                    isEmergency    = c.optBoolean("isEmergency", false),
                    customTypeName = c.optString("customTypeName", "").let { if (it == "null") "" else it },
                    sortOrder      = c.optInt("sortOrder", 0),
                    isFavorite     = c.optBoolean("isFavorite", false)
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
                    dayId     = if (v.isNull("dayId")) null else v.optInt("dayId"),
                    sortOrder = v.optInt("sortOrder", 0),
                    isUsed    = v.optBoolean("isUsed", false)
                )
            } else emptyList()

        val boardingJsonArray = trip.optJSONArray("boardingPasses")
        val boardingPasses = if (boardingJsonArray != null)
            (0 until boardingJsonArray.length()).map { i ->
                val p = boardingJsonArray.getJSONObject(i)
                ExportedBoardingPass(
                    transportType   = p.optString("transportType", "FLIGHT").ifBlank { "FLIGHT" },
                    origin          = p.getString("origin"),
                    originCity      = p.getString("originCity"),
                    destination     = p.getString("destination"),
                    destinationCity = p.getString("destinationCity"),
                    flightNumber    = p.getString("flightNumber"),
                    date            = p.getString("date"),
                    boardingTime    = p.getString("boardingTime"),
                    passenger       = p.getString("passenger"),
                    walletUrl       = p.optString("walletUrl").takeIf  { it.isNotBlank() && it != "null" },
                    documentName    = p.optString("documentName").takeIf { it.isNotBlank() && it != "null" } ?: "",
                    notes           = p.optString("notes").takeIf       { it.isNotBlank() && it != "null" } ?: ""
                )
            } else emptyList()

        // F4 — notas (parseadas direto para domain models, prontas p/ inserção)
        val notesJsonArray = trip.optJSONArray("notes")
        val notes = if (notesJsonArray != null)
            (0 until notesJsonArray.length()).map { i ->
                val n = notesJsonArray.getJSONObject(i)
                val blocksArr = n.optJSONArray("blocks")
                val blocks = if (blocksArr != null)
                    (0 until blocksArr.length()).map { j ->
                        val b         = blocksArr.getJSONObject(j)
                        val sortOrder = b.optInt("sortOrder", j)
                        when (b.optString("type", "TEXT")) {
                            NoteBlockType.CHECKLIST.name -> {
                                val itemsArr = b.optJSONArray("items")
                                val items = if (itemsArr != null)
                                    (0 until itemsArr.length()).map { k ->
                                        val it = itemsArr.getJSONObject(k)
                                        ChecklistItem(
                                            text = it.optString("text", ""),
                                            isChecked = it.optBoolean("isChecked", false),
                                            sortOrder = it.optInt("sortOrder", k)
                                        )
                                    } else emptyList()
                                NoteBlock.ChecklistBlock(items = items, sortOrder = sortOrder)
                            }
                            NoteBlockType.HEADING.name ->
                                NoteBlock.HeadingBlock(content = b.optString("content", ""), sortOrder = sortOrder)
                            else ->
                                NoteBlock.TextBlock(content = b.optString("content", ""), sortOrder = sortOrder)
                        }
                    } else emptyList()
                Note(
                    tripId    = 0L,   // definido no insert
                    dayId     = if (n.isNull("dayId")) null else n.optInt("dayId"),
                    title     = n.optString("title", ""),
                    blocks    = blocks,
                    sortOrder = n.optInt("sortOrder", i),
                    createdAt = n.optLong("createdAt", 0L),
                    updatedAt = n.optLong("updatedAt", 0L)
                )
            } else emptyList()

        return ExportedTrip(
            schemaVersion = schemaVer,
            tripUuid      = trip.optString("tripUuid").let { if (it == "null") "" else it },
            lastEditedAt  = trip.optLong("lastEditedAt", 0L),
            name          = trip.getString("name"),
            destination   = trip.getString("destination"),
            coverEmoji    = trip.optString("coverEmoji", "✈️"),
            startDate     = trip.optString("startDate").takeIf { it.isNotBlank() && it != "null" },
            endDate       = trip.optString("endDate").takeIf   { it.isNotBlank() && it != "null" },
            latitude      = if (trip.isNull("latitude")) null else trip.optDouble("latitude"),
            longitude     = if (trip.isNull("longitude")) null else trip.optDouble("longitude"),
            hotelName       = hotel?.optString("name", "") ?: "",
            hotelAddress    = hotel?.optString("address", "") ?: "",
            hotelPhone      = hotel?.optString("phone", "") ?: "",
            voucherSortMode = trip.optString("voucherSortMode", "BY_CATEGORY"),
            days            = days,
            contacts      = contacts,
            vouchers      = vouchers,
            boardingPasses = boardingPasses,
            notes          = notes
        )
    }

    companion object {
        private const val SUPPORTED_SCHEMA_VERSION = 3   // F4: array notes[]
    }
}
