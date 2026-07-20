package com.rodrigoleao.gramado2026.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.rodrigoleao.gramado2026.data.db.contentText
import com.rodrigoleao.gramado2026.data.db.typeName
import com.rodrigoleao.gramado2026.data.model.*
import com.rodrigoleao.gramado2026.data.repository.NoteRepository
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TripRepository,
    private val noteRepo: NoteRepository
) {

    suspend fun export(tripId: Long): Uri {
        val data = repo.getTripData(tripId)
            ?: throw Exception("Viagem não encontrada.")

        val notes = noteRepo.getAllNotes(tripId)   // F4: notas gerais + de dia
        val json = buildJson(data, notes)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName  = data.trip.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val zipFile   = File(exportDir, "$safeName.travel")

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->

            // trip.json
            zip.putNextEntry(ZipEntry("trip.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // documents/ — arquivos anexados aos dias
            data.days.forEach { day ->
                if (day.dayDocumentPath != null) {
                    val doc = File(day.dayDocumentPath)
                    if (doc.exists()) {
                        zip.putNextEntry(ZipEntry("documents/${doc.name}"))
                        doc.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            // boarding/ — arquivos de passagens anexados
            data.boardingPasses.forEach { pass ->
                if (!pass.documentPath.isNullOrBlank()) {
                    val doc = File(pass.documentPath)
                    if (doc.exists() && pass.documentName.isNotBlank()) {
                        zip.putNextEntry(ZipEntry("boarding/${pass.documentName}"))
                        doc.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            // vouchers/ — tenta copiar o arquivo de cada voucher (assets ou filesDir)
            data.vouchers.forEach { voucher ->
                val assetPath = voucher.assetPath.trimStart('/')
                val bytes = tryReadVoucherFile(voucher.assetPath, assetPath)
                if (bytes != null) {
                    zip.putNextEntry(ZipEntry("vouchers/$assetPath"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    private fun buildJson(data: com.rodrigoleao.gramado2026.data.repository.TripData, notes: List<Note>): String {
        val trip = data.trip

        val hotelObj = JSONObject().apply {
            put("name",    trip.hotelName)
            put("address", trip.hotelAddress)
            put("phone",   trip.hotelPhone)
        }

        val daysArray = JSONArray()
        data.days.forEach { day ->
            daysArray.put(buildDayJson(day))
        }

        val contactsArray = JSONArray()
        data.contacts.forEach { contactsArray.put(buildContactJson(it)) }

        val vouchersArray = JSONArray()
        data.vouchers.forEach { vouchersArray.put(buildVoucherJson(it)) }

        val boardingArray = JSONArray()
        data.boardingPasses.forEach { boardingArray.put(buildBoardingPassJson(it)) }

        val notesArray = JSONArray()
        notes.forEach { notesArray.put(buildNoteJson(it)) }

        val tripObj = JSONObject().apply {
            put("tripUuid",         trip.tripUuid)
            put("lastEditedAt",     trip.lastEditedAt)
            put("name",             trip.name)
            put("destination",      trip.destination)
            put("coverEmoji",       trip.coverEmoji)
            put("startDate",        trip.startDate)
            put("endDate",          trip.endDate)
            put("latitude",         trip.latitude ?: JSONObject.NULL)
            put("longitude",        trip.longitude ?: JSONObject.NULL)
            put("voucherSortMode",  trip.voucherSortMode)
            put("hotel",            hotelObj)
            put("days",             daysArray)
            put("contacts",         contactsArray)
            put("vouchers",         vouchersArray)
            put("boardingPasses",   boardingArray)
            put("notes",            notesArray)
        }

        val ts = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        return JSONObject().apply {
            put("schemaVersion", 3)   // F4: presença do array notes[]
            put("exportedAt",    ts)
            put("trip",          tripObj)
        }.toString(2)
    }

    private fun buildDayJson(day: TravelDay): JSONObject {
        val activitiesArray = JSONArray()
        day.activities.forEachIndexed { index, act ->
            activitiesArray.put(buildActivityJson(act, index))
        }

        val docName = if (day.dayDocumentPath != null)
            File(day.dayDocumentPath).name else JSONObject.NULL

        return JSONObject().apply {
            put("dayNumber",    day.id)
            put("date",         day.date.toString())
            put("dayOfWeek",    day.dayOfWeek)
            put("title",        day.title)
            put("dayAlert",     day.dayAlert ?: JSONObject.NULL)
            put("linkUrl",      day.dayLinkUrl ?: JSONObject.NULL)
            put("linkLabel",    day.dayLinkLabel)
            put("documentName",  docName)
            put("documentTitle", day.dayDocumentTitle.ifEmpty { JSONObject.NULL })
            put("activities",    activitiesArray)
        }
    }

    private fun buildActivityJson(act: TravelActivity, position: Int): JSONObject {
        val badgesArray = JSONArray()
        act.badges.forEach { badge ->
            badgesArray.put(JSONObject().apply {
                put("type",  badge.type.name)
                put("label", badge.label)
                put("color", badge.color ?: JSONObject.NULL)
            })
        }

        val stopsArray = JSONArray()
        act.walkStops.forEachIndexed { i, stop ->
            stopsArray.put(JSONObject().apply {
                put("order",  i + 1)
                put("name",   stop.label)
                put("detail", stop.sublabel ?: "")
                put("emoji",  stop.emoji)
                put("isLast", stop.isLast)
            })
        }

        return JSONObject().apply {
            put("position",   position)
            put("time",       act.time)
            put("emoji",      act.emoji)
            put("name",       act.name)
            put("detail",     act.detail)
            put("address",    act.mapQuery ?: JSONObject.NULL)
            put("badges",     badgesArray)
            put("walkStops",  stopsArray)
        }
    }

    private fun buildContactJson(contact: Contact): JSONObject =
        JSONObject().apply {
            put("name",           contact.name)
            put("role",           contact.role)
            put("phone",          contact.phone ?: JSONObject.NULL)
            put("type",           contact.type.name)
            put("hasWhatsApp",    contact.hasWhatsApp)
            put("isEmergency",    contact.isEmergency)
            put("customTypeName", contact.customTypeName.ifEmpty { JSONObject.NULL })
            put("sortOrder",      contact.sortOrder)
            put("isFavorite",     contact.isFavorite)
        }

    private fun buildVoucherJson(voucher: Voucher): JSONObject =
        JSONObject().apply {
            put("emoji",     voucher.emoji)
            put("groupName", voucher.groupName)
            put("name",      voucher.name)
            put("person",    voucher.person ?: JSONObject.NULL)
            put("assetPath", voucher.assetPath)
            put("dayId",     voucher.dayId ?: JSONObject.NULL)
            put("sortOrder", voucher.sortOrder)
            put("isUsed",    voucher.isUsed)
        }

    private fun buildBoardingPassJson(pass: BoardingPass): JSONObject =
        JSONObject().apply {
            put("transportType",   pass.transportType)
            put("origin",          pass.origin)
            put("originCity",      pass.originCity)
            put("destination",     pass.destination)
            put("destinationCity", pass.destinationCity)
            put("flightNumber",    pass.flightNumber)
            put("date",            pass.date)
            put("boardingTime",    pass.boardingTime)
            put("passenger",       pass.passenger)
            put("walletUrl",       pass.walletUrl ?: JSONObject.NULL)
            put("documentName",    pass.documentName.ifEmpty { JSONObject.NULL })
            put("notes",           pass.notes.ifEmpty { JSONObject.NULL })
        }

    private fun buildNoteJson(note: Note): JSONObject {
        val blocksArray = JSONArray()
        note.blocks.forEach { block ->
            blocksArray.put(JSONObject().apply {
                put("type",      block.typeName())
                put("sortOrder", block.sortOrder)
                when (block) {
                    is NoteBlock.TextBlock,
                    is NoteBlock.HeadingBlock -> put("content", block.contentText())
                    is NoteBlock.ChecklistBlock -> {
                        val itemsArray = JSONArray()
                        block.items.forEach { item ->
                            itemsArray.put(JSONObject().apply {
                                put("text",      item.text)
                                put("isChecked", item.isChecked)
                                put("sortOrder", item.sortOrder)
                            })
                        }
                        put("items", itemsArray)
                    }
                }
            })
        }
        return JSONObject().apply {
            put("dayId",      note.dayId ?: JSONObject.NULL)
            put("title",      note.title)
            put("sortOrder",  note.sortOrder)
            put("createdAt",  note.createdAt)
            put("updatedAt",  note.updatedAt)
            put("blocks",     blocksArray)
        }
    }

    // Tenta ler o arquivo do voucher: primeiro como caminho absoluto (filesDir),
    // depois como asset (quando assetPath é um caminho relativo a assets/).
    private fun tryReadVoucherFile(absoluteOrRelative: String, assetPath: String): ByteArray? {
        val localFile = File(absoluteOrRelative)
        if (localFile.isAbsolute && localFile.exists()) return localFile.readBytes()
        val inVouchersDir = File(context.filesDir, "Vouchers/$assetPath")
        if (inVouchersDir.exists()) return inVouchersDir.readBytes()
        return runCatching { context.assets.open(assetPath).use { it.readBytes() } }.getOrNull()
    }
}
