package com.rodrigoleao.gramado2026.data.export

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.entity.*
import com.rodrigoleao.gramado2026.data.db.inMemoryDb
import com.rodrigoleao.gramado2026.data.db.tripEntity
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.import_trip.TravelImporter
import com.rodrigoleao.gramado2026.data.model.DuplicateTripException
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import com.rodrigoleao.gramado2026.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * O teste mais valioso do app: exportar uma viagem para `.travel` e reimportá-la
 * não pode perder nenhum campo. Qualquer campo novo adicionado ao banco que não
 * for adicionado ao `TravelExporter`/`TravelImporter` quebra este teste.
 *
 * Exercita o caminho real de produção: banco Room → `TravelExporter` (ZIP +
 * FileProvider em `cacheDir/exports/`) → `TravelImporter` (ContentResolver →
 * parse → repositórios → `filesDir/`). Só o banco é em memória.
 */
@RunWith(AndroidJUnit4::class)
class ExportImportRoundTripTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var db: TravelDatabase
    private lateinit var tripRepo: TripRepository
    private lateinit var dayRepo: DayRepository
    private lateinit var activityRepo: ActivityRepository
    private lateinit var contactRepo: ContactRepository
    private lateinit var voucherRepo: VoucherRepository
    private lateinit var boardingPassRepo: BoardingPassRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var exporter: TravelExporter
    private lateinit var importer: TravelImporter

    @Before
    fun setup() {
        db               = inMemoryDb()
        tripRepo         = TripRepository(db)
        dayRepo          = DayRepository(db)
        activityRepo     = ActivityRepository(db)
        contactRepo      = ContactRepository(db)
        voucherRepo      = VoucherRepository(db)
        boardingPassRepo = BoardingPassRepository(db)
        noteRepo         = NoteRepository(db)
        exporter         = TravelExporter(context, tripRepo, noteRepo)
        importer         = TravelImporter(
            context, tripRepo, dayRepo, activityRepo, contactRepo, voucherRepo, boardingPassRepo, noteRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
        // Limpa artefatos de arquivo criados pelos testes
        File(context.cacheDir, "exports").deleteRecursively()
        listOf("Arquivos", "Vouchers", "Passagens").forEach {
            File(context.filesDir, it).deleteRecursively()
        }
    }

    // ── Seed de viagem completa via repositórios (mesmo caminho do app) ─────────

    private suspend fun seedFullTrip(name: String): Long {
        val tripId = tripRepo.createTrip(
            name = name, destination = "Gramado, RS", coverEmoji = "⛰️",
            startDate = "2026-06-09", endDate = "2026-06-11",         // 3 dias
            latitude = -29.37, longitude = -50.87,
            hotelName = "Hotel San Lucas", hotelAddress = "Rua João Carniel, 73",
            hotelPhone = "(54) 3286-0000"
        )

        // Dia 1: título, alerta, link + 1 atividade com badges e walk stops
        val day1 = dayRepo.getDayEntity(tripId, 1)!!
        dayRepo.updateDay(day1.copy(
            title = "Chegada — Lago Negro", dayAlert = "Levar casaco",
            dayLinkUrl = "https://example.com/mapa", dayLinkLabel = "Mapa de Rotas"
        ))
        val actId = activityRepo.upsertActivity(
            dayEntityId = day1.id,
            entity = TravelActivityEntity(
                dayId = day1.id, position = 0, time = "14h00", emoji = "🏨",
                name = "Check-in", detail = "Hotel San Lucas",
                mapQuery = "Rua João Carniel, 73", uberDestination = "Rua João Carniel, 73"
            ),
            badges = listOf(
                ActivityBadgeEntity(activityId = 0L, badgeType = "BOOKED", label = "RESERVADO", color = null),
                ActivityBadgeEntity(activityId = 0L, badgeType = "CUSTOM", label = "Reserva 19h", color = "#FF5722")
            )
        )
        activityRepo.insertWalkStop(WalkStopEntity(
            activityId = actId, position = 1, emoji = "🚶", label = "Rua Coberta", sublabel = "200m", isLast = false
        ))
        activityRepo.insertWalkStop(WalkStopEntity(
            activityId = actId, position = 2, emoji = "🏨", label = "Hotel", sublabel = null, isLast = true
        ))

        // Contatos: um builtin favorito + um de categoria personalizada sem telefone
        contactRepo.upsertContact(tripId, ContactEntity(
            tripId = tripId, name = "Guia Ana", role = "Agência Brocker", phone = "54999990000",
            contactType = "AGENCY", hasWhatsApp = true, isEmergency = false,
            sortOrder = 0, isFavorite = true
        ))
        contactRepo.upsertContact(tripId, ContactEntity(
            tripId = tripId, name = "Chef Paulo", role = "Jantar especial", phone = null,
            contactType = "CUSTOM", customTypeName = "Gastronomia", hasWhatsApp = false,
            isEmergency = false, sortOrder = 1, isFavorite = false
        ))

        // Vouchers: grupos, sortOrder, isUsed e dayNumber distintos
        voucherRepo.upsertVoucher(tripId, VoucherEntity(
            tripId = tripId, dayNumber = 2, emoji = "🎫", groupName = "Passeios",
            name = "Bondinhos Aéreos", person = "Rodrigo",
            assetPath = "passeios/bondinho.pdf", sortOrder = 0, isUsed = true
        ))
        voucherRepo.upsertVoucher(tripId, VoucherEntity(
            tripId = tripId, dayNumber = null, emoji = "🍽️", groupName = "Alimentação",
            name = "Jantar Epopeia", person = null,
            assetPath = "https://example.com/reserva", sortOrder = 1, isUsed = false
        ))

        // Passagem com observações
        boardingPassRepo.upsertBoardingPass(tripId, BoardingPassEntity(
            tripId = tripId, transportType = "FLIGHT", origin = "REC", originCity = "Recife",
            destination = "POA", destinationCity = "Porto Alegre", flightNumber = "AD4104",
            date = "09/06", boardingTime = "05h30", passenger = "Rodrigo",
            walletUrl = null, notes = "Assento 12A"
        ))

        return tripId
    }

    /** Remove a viagem do banco — simula importar num aparelho que ainda não a tem
     *  (com F1, reimportar no mesmo banco dispararia DuplicateTripException). */
    private suspend fun clearTripFromDb(tripId: Long) {
        tripRepo.getTripEntity(tripId)?.let { tripRepo.deleteTrip(it) }
    }

    // ── Testes ──────────────────────────────────────────────────────────────────

    @Test
    fun roundTrip_preservaViagemDiasAtividadesBadgesEWalkStops() {
        runBlocking {
            val tripId = seedFullTrip("RoundTrip Roteiro")

            val orig = tripRepo.getTripData(tripId)!!   // captura antes de limpar
            val uri = exporter.export(tripId)
            clearTripFromDb(tripId)                      // device limpo
            val importedId = importer.import(uri)
            assertThat(importedId).isNotEqualTo(tripId)

            val imp  = tripRepo.getTripData(importedId)!!

            // Viagem
            with(imp.trip) {
                assertThat(name).isEqualTo(orig.trip.name)
                assertThat(destination).isEqualTo("Gramado, RS")
                assertThat(coverEmoji).isEqualTo("⛰️")
                assertThat(startDate).isEqualTo("2026-06-09")
                assertThat(endDate).isEqualTo("2026-06-11")
                assertThat(latitude).isEqualTo(-29.37)
                assertThat(longitude).isEqualTo(-50.87)
                assertThat(hotelName).isEqualTo("Hotel San Lucas")
                assertThat(hotelAddress).isEqualTo("Rua João Carniel, 73")
                assertThat(hotelPhone).isEqualTo("(54) 3286-0000")
            }

            // Dias
            assertThat(imp.days).hasSize(3)
            val impDay1 = imp.days.first { it.id == 1 }
            assertThat(impDay1.title).isEqualTo("Chegada — Lago Negro")
            assertThat(impDay1.dayAlert).isEqualTo("Levar casaco")
            assertThat(impDay1.dayLinkUrl).isEqualTo("https://example.com/mapa")
            assertThat(impDay1.dayLinkLabel).isEqualTo("Mapa de Rotas")
            assertThat(impDay1.date).isEqualTo(orig.days.first { it.id == 1 }.date)
            assertThat(imp.days.first { it.id == 2 }.title)
                .isEqualTo(orig.days.first { it.id == 2 }.title)

            // Atividade
            assertThat(impDay1.activities).hasSize(1)
            val impAct = impDay1.activities[0]
            with(impAct) {
                assertThat(time).isEqualTo("14h00")
                assertThat(emoji).isEqualTo("🏨")
                assertThat(name).isEqualTo("Check-in")
                assertThat(detail).isEqualTo("Hotel San Lucas")
                assertThat(mapQuery).isEqualTo("Rua João Carniel, 73")
                assertThat(uberDestination).isEqualTo("Rua João Carniel, 73")
            }

            // Badges (inclusive CUSTOM com cor)
            assertThat(impAct.badges).hasSize(2)
            val customBadge = impAct.badges.first { it.type.name == "CUSTOM" }
            assertThat(customBadge.label).isEqualTo("Reserva 19h")
            assertThat(customBadge.color).isEqualTo("#FF5722")
            val bookedBadge = impAct.badges.first { it.type.name == "BOOKED" }
            assertThat(bookedBadge.label).isEqualTo("RESERVADO")
            assertThat(bookedBadge.color).isNull()

            // Walk stops (ordem, sublabel nulo, isLast)
            assertThat(impAct.walkStops).hasSize(2)
            assertThat(impAct.walkStops.map { it.label }).containsExactly("Rua Coberta", "Hotel").inOrder()
            assertThat(impAct.walkStops[0].sublabel).isEqualTo("200m")
            assertThat(impAct.walkStops[1].sublabel).isNull()
            assertThat(impAct.walkStops[1].isLast).isTrue()
        }
    }

    @Test
    fun roundTrip_preservaContatosVouchersEPassagens() {
        runBlocking {
            val tripId = seedFullTrip("RoundTrip Anexos")
            val uri = exporter.export(tripId)
            clearTripFromDb(tripId)
            val importedId = importer.import(uri)
            val imp = tripRepo.getTripData(importedId)!!

            // Contatos
            assertThat(imp.contacts).hasSize(2)
            val ana = imp.contacts.first { it.name == "Guia Ana" }
            with(ana) {
                assertThat(role).isEqualTo("Agência Brocker")
                assertThat(phone).isEqualTo("54999990000")
                assertThat(type.name).isEqualTo("AGENCY")
                assertThat(hasWhatsApp).isTrue()
                assertThat(sortOrder).isEqualTo(0)
                assertThat(isFavorite).isTrue()
            }
            val paulo = imp.contacts.first { it.name == "Chef Paulo" }
            with(paulo) {
                assertThat(phone).isNull()
                assertThat(type.name).isEqualTo("CUSTOM")
                assertThat(customTypeName).isEqualTo("Gastronomia")
                assertThat(sortOrder).isEqualTo(1)
                assertThat(isFavorite).isFalse()
            }

            // Vouchers
            assertThat(imp.vouchers).hasSize(2)
            val bondinho = imp.vouchers.first { it.name == "Bondinhos Aéreos" }
            with(bondinho) {
                assertThat(emoji).isEqualTo("🎫")
                assertThat(groupName).isEqualTo("Passeios")
                assertThat(person).isEqualTo("Rodrigo")
                assertThat(dayId).isEqualTo(2)
                assertThat(sortOrder).isEqualTo(0)
                assertThat(isUsed).isTrue()
            }
            val jantar = imp.vouchers.first { it.name == "Jantar Epopeia" }
            with(jantar) {
                assertThat(person).isNull()
                assertThat(dayId).isNull()
                assertThat(isUsed).isFalse()
                // Sem arquivo no ZIP (é um link) → assetPath original preservado
                assertThat(assetPath).isEqualTo("https://example.com/reserva")
            }

            // Passagem
            assertThat(imp.boardingPasses).hasSize(1)
            with(imp.boardingPasses[0]) {
                assertThat(transportType).isEqualTo("FLIGHT")
                assertThat(origin).isEqualTo("REC")
                assertThat(originCity).isEqualTo("Recife")
                assertThat(destination).isEqualTo("POA")
                assertThat(destinationCity).isEqualTo("Porto Alegre")
                assertThat(flightNumber).isEqualTo("AD4104")
                assertThat(date).isEqualTo("09/06")
                assertThat(boardingTime).isEqualTo("05h30")
                assertThat(passenger).isEqualTo("Rodrigo")
                assertThat(walletUrl).isNull()
                assertThat(notes).isEqualTo("Assento 12A")
            }
        }
    }

    @Test
    fun roundTrip_preservaVoucherSortMode() {
        runBlocking {
            val tripId = seedFullTrip("RoundTrip SortMode")
            tripRepo.saveVoucherSortMode(tripId, "BY_DAY")

            val uri = exporter.export(tripId)
            clearTripFromDb(tripId)
            val importedId = importer.import(uri)

            assertThat(tripRepo.getTripData(importedId)!!.trip.voucherSortMode).isEqualTo("BY_DAY")
        }
    }

    @Test
    fun roundTrip_arquivosDeVoucherDocumentoDoDiaEPassagemViajamNoZip() {
        runBlocking {
            val tripId = tripRepo.createTrip(
                name = "RoundTrip Arquivos", destination = "Gramado, RS", coverEmoji = "⛰️",
                startDate = "2026-06-09", endDate = "2026-06-09"
            )

            val voucherBytes = "PDF-VOUCHER-CONTEUDO".toByteArray()
            val docBytes     = "PDF-DOCUMENTO-DIA".toByteArray()
            val passBytes    = "PDF-CARTAO-EMBARQUE".toByteArray()

            // Arquivo de voucher em filesDir/Vouchers/<assetPath relativo>
            val voucherFile = File(context.filesDir, "Vouchers/passeios/bondinho-teste.pdf")
            voucherFile.parentFile!!.mkdirs(); voucherFile.writeBytes(voucherBytes)
            voucherRepo.upsertVoucher(tripId, VoucherEntity(
                tripId = tripId, dayNumber = null, emoji = "🎫", groupName = "Passeios",
                name = "Bondinho", person = null, assetPath = "passeios/bondinho-teste.pdf"
            ))

            // Documento do dia em filesDir/Arquivos/
            val docFile = File(context.filesDir, "Arquivos/mapa-teste.pdf")
            docFile.parentFile!!.mkdirs(); docFile.writeBytes(docBytes)
            val day1 = dayRepo.getDayEntity(tripId, 1)!!
            dayRepo.updateDay(day1.copy(
                dayDocumentPath = docFile.absolutePath,
                dayDocumentName = "mapa-teste.pdf",
                dayDocumentTitle = "Mapa da cidade"
            ))

            // Arquivo da passagem em filesDir/Passagens/
            val passFile = File(context.filesDir, "Passagens/cartao-teste.pdf")
            passFile.parentFile!!.mkdirs(); passFile.writeBytes(passBytes)
            boardingPassRepo.upsertBoardingPass(tripId, BoardingPassEntity(
                tripId = tripId, origin = "REC", originCity = "Recife", destination = "POA",
                destinationCity = "Porto Alegre", flightNumber = "AD4104", date = "09/06",
                boardingTime = "05h30", passenger = "Rodrigo", walletUrl = null,
                documentPath = passFile.absolutePath, documentName = "cartao-teste.pdf"
            ))

            val uri = exporter.export(tripId)

            // Apaga os originais + a viagem do banco: prova que o import restaura a
            // partir do ZIP (não de sobras) e que roda num "device limpo"
            voucherFile.delete(); docFile.delete(); passFile.delete()
            clearTripFromDb(tripId)

            val importedId = importer.import(uri)
            val imp = tripRepo.getTripData(importedId)!!

            // Voucher: assetPath agora é caminho local absoluto e os bytes vieram do ZIP
            val impVoucher = imp.vouchers.first { it.name == "Bondinho" }
            assertThat(File(impVoucher.assetPath).exists()).isTrue()
            assertThat(File(impVoucher.assetPath).readBytes()).isEqualTo(voucherBytes)

            // Documento do dia
            val impDay1 = imp.days.first { it.id == 1 }
            assertThat(impDay1.dayDocumentName).isEqualTo("mapa-teste.pdf")
            assertThat(impDay1.dayDocumentTitle).isEqualTo("Mapa da cidade")
            assertThat(impDay1.dayDocumentPath).isNotNull()
            assertThat(File(impDay1.dayDocumentPath!!).readBytes()).isEqualTo(docBytes)

            // Documento da passagem
            val impPass = imp.boardingPasses[0]
            assertThat(impPass.documentName).isEqualTo("cartao-teste.pdf")
            assertThat(impPass.documentPath).isNotNull()
            assertThat(File(impPass.documentPath!!).readBytes()).isEqualTo(passBytes)
        }
    }

    // ── F4: notas ─────────────────────────────────────────────────────────────

    @Test
    fun roundTrip_preservaNotasComBlocosEItens() {
        runBlocking {
            val tripId = seedFullTrip("RoundTrip Notas")

            // Nota geral: título + heading + checklist (1 item marcado + 1 desmarcado)
            val generalId = noteRepo.createNote(tripId, null)
            noteRepo.updateNoteTitle(generalId, "Packing list")
            val headingId = noteRepo.addBlock(generalId, NoteBlockType.HEADING)
            noteRepo.updateBlockContent(headingId, "Documentos")
            val checkId = noteRepo.addBlock(generalId, NoteBlockType.CHECKLIST)  // nasce com 1 item
            val firstItem = (noteRepo.getNote(generalId)!!.blocks
                .first { it is NoteBlock.ChecklistBlock } as NoteBlock.ChecklistBlock).items.first()
            noteRepo.updateItemText(firstItem.id, "Passaporte")
            noteRepo.toggleChecklistItem(firstItem.id, true)
            val item2 = noteRepo.addChecklistItem(checkId)
            noteRepo.updateItemText(item2, "Carregador")

            // Nota de dia (dayId = 1) com um bloco de texto
            val dayNoteId = noteRepo.createNote(tripId, 1)
            val textId = noteRepo.addBlock(dayNoteId, NoteBlockType.TEXT)
            noteRepo.updateBlockContent(textId, "Chegar cedo à estação")

            val uri = exporter.export(tripId)
            clearTripFromDb(tripId)   // CASCADE remove as notas da original também
            val importedId = importer.import(uri)

            // Nota geral restaurada
            val general = noteRepo.getNotes(importedId, null)
            assertThat(general).hasSize(1)
            with(general[0]) {
                assertThat(title).isEqualTo("Packing list")
                assertThat(blocks).hasSize(2)
                assertThat((blocks[0] as NoteBlock.HeadingBlock).content).isEqualTo("Documentos")
                val checklist = blocks[1] as NoteBlock.ChecklistBlock
                assertThat(checklist.items.map { it.text }).containsExactly("Passaporte", "Carregador").inOrder()
                assertThat(checklist.items.first { it.text == "Passaporte" }.isChecked).isTrue()
                assertThat(checklist.items.first { it.text == "Carregador" }.isChecked).isFalse()
            }

            // Nota de dia restaurada com o dayId preservado
            val dayNotes = noteRepo.getNotes(importedId, 1)
            assertThat(dayNotes).hasSize(1)
            assertThat((dayNotes[0].blocks[0] as NoteBlock.TextBlock).content).isEqualTo("Chegar cedo à estação")
            // Nota de dia não vaza para as gerais
            assertThat(general.none { it.title == dayNotes[0].title && it.dayId == null }).isTrue()
        }
    }

    // ── F1: detecção de duplicata e sobrescrita ──────────────────────────────────

    @Test
    fun getTripData_curaUuidVazioDeViagemPreF1() {
        runBlocking {
            // Simula viagem criada antes da F1: uuid vazio inserido direto
            val tripId = db.tripDao().insert(
                tripEntity(name = "Viagem antiga").copy(tripUuid = "", lastEditedAt = 0L)
            )
            assertThat(db.tripDao().getById(tripId)!!.tripUuid).isEmpty()

            val data = tripRepo.getTripData(tripId)!!

            assertThat(data.trip.tripUuid).isNotEmpty()                       // curado no retorno
            assertThat(db.tripDao().getById(tripId)!!.tripUuid).isNotEmpty()  // e persistido
        }
    }

    @Test
    fun import_uuidExistente_lancaDuplicateException() {
        runBlocking {
            val tripId = seedFullTrip("RoundTrip Duplicado")
            tripRepo.touchLastEditedAt(tripId)          // garante lastEditedAt > 0
            val uri = exporter.export(tripId)

            // Reimportar SEM limpar → o UUID já existe no banco
            val ex = runCatching { importer.import(uri) }.exceptionOrNull()

            assertThat(ex).isInstanceOf(DuplicateTripException::class.java)
            val dup = ex as DuplicateTripException
            assertThat(dup.existingTripId).isEqualTo(tripId)
            assertThat(dup.existingTripName).isEqualTo("RoundTrip Duplicado")
            assertThat(db.tripDao().count()).isEqualTo(1)   // nada foi importado
        }
    }

    @Test
    fun import_arquivoV1SemUuid_importaNormalETripRecebeUuidNovo() {
        runBlocking {
            // .travel v1 forjado (schemaVersion 1, sem tripUuid)
            val json = """
                {"schemaVersion":1,"trip":{"name":"Viagem Antiga","destination":"Gramado, RS",
                "coverEmoji":"⛰️","startDate":"2026-06-09","endDate":"2026-06-09",
                "hotel":{"name":"","address":"","phone":""},"days":[],"contacts":[],
                "vouchers":[],"boardingPasses":[]}}
            """.trimIndent()
            val uri = writeFakeTravel("v1.travel", "trip.json", json)

            val importedId = importer.import(uri)   // não deve detectar duplicata

            val imp = tripRepo.getTripData(importedId)!!
            assertThat(imp.trip.name).isEqualTo("Viagem Antiga")
            assertThat(imp.trip.tripUuid).isNotEmpty()   // UUID gerado na importação
        }
    }

    @Test
    fun overwriteImport_substituiViagemAntigaEArquivos() {
        runBlocking {
            // Viagem local "antiga" com um voucher-arquivo próprio
            val oldId = tripRepo.createTrip(
                name = "Versão Antiga", destination = "Gramado, RS", coverEmoji = "⛰️",
                startDate = "2026-06-09", endDate = "2026-06-09"
            )
            val oldUuid = tripRepo.getTripEntity(oldId)!!.tripUuid
            val orphanFile = File(context.filesDir, "Vouchers/antigo/so-na-antiga.pdf")
            orphanFile.parentFile!!.mkdirs(); orphanFile.writeBytes("ANTIGO".toByteArray())
            voucherRepo.upsertVoucher(oldId, VoucherEntity(
                tripId = oldId, dayNumber = null, emoji = "🎫", groupName = "G",
                name = "Antigo", person = null, assetPath = orphanFile.absolutePath
            ))

            // Constrói um .travel com o MESMO uuid mas conteúdo novo (nome diferente, sem o voucher)
            val donorId = tripRepo.createTrip(
                name = "Versão Nova", destination = "Gramado, RS", coverEmoji = "🌲",
                startDate = "2026-06-09", endDate = "2026-06-09", tripUuid = oldUuid
            )
            val uri = exporter.export(donorId)
            clearTripFromDb(donorId)   // deixa só a "antiga" no banco antes do overwrite

            val newId = importer.overwriteImport(uri, oldId)

            // A antiga sumiu; a nova é a única com aquele uuid
            assertThat(tripRepo.getTripEntity(oldId)).isNull()
            val imp = tripRepo.getTripData(newId)!!
            assertThat(imp.trip.name).isEqualTo("Versão Nova")
            assertThat(imp.trip.tripUuid).isEqualTo(oldUuid)
            // Arquivo órfão da antiga (não usado pela nova) foi removido do disco
            assertThat(orphanFile.exists()).isFalse()
        }
    }

    @Test
    fun overwriteImport_falhaNaImportacaoPreservaViagemLocal() {
        runBlocking {
            val oldId = seedFullTrip("Local Intacta")
            val bad = writeFakeTravel("corrompido.travel", "leia-me.txt", "não é trip.json")

            val result = runCatching { importer.overwriteImport(bad, oldId) }

            assertThat(result.isFailure).isTrue()
            // UC-F1-10: a viagem local não foi deletada
            assertThat(tripRepo.getTripEntity(oldId)).isNotNull()
        }
    }

    // ── Rejeições ───────────────────────────────────────────────────────────────

    /** Grava um .travel forjado em cacheDir e devolve a Uri file:// dele. */
    private fun writeFakeTravel(name: String, entryName: String, content: String): Uri {
        val zipFile = File(context.cacheDir, name)
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return Uri.fromFile(zipFile)
    }

    @Test
    fun import_schemaVersionSuperior_recusaComMensagemDeAtualizacao() {
        runBlocking {
            val uri = writeFakeTravel("schema-futuro.travel", "trip.json", """{"schemaVersion": 4}""")

            val result = runCatching { importer.import(uri) }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()!!.message).contains("versão mais recente")
        }
    }

    @Test
    fun import_zipSemTripJson_recusa() {
        runBlocking {
            val uri = writeFakeTravel("sem-trip.travel", "leia-me.txt", "não sou um trip.json")

            val result = runCatching { importer.import(uri) }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()!!.message).contains("trip.json")
        }
    }
}
