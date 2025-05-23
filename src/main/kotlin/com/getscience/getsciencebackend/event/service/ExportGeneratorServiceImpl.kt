package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import jakarta.annotation.PostConstruct
import jakarta.transaction.Transactional
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.RandomUidGenerator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*

@Service
class ExportGeneratorServiceImpl(
    @Value("\${frontend.base-url:http://localhost:3000}") private val frontendBaseUrl: String
) : ExportGeneratorService {

    private val uidGenerator = RandomUidGenerator()
    
    @PostConstruct
    fun init() {
        // Устанавливаем headless режим для Java AWT
        System.setProperty("java.awt.headless", "true")
    }

    /**
     * Генерирует iCalendar файл для указанного мероприятия.
     * Создает календарную запись с информацией о мероприятии: название, описание, даты, место.
     *
     * @param event Мероприятие для экспорта
     * @return Содержимое .ics файла в виде массива байтов
     */
    @LogBusinessOperation(operationType = "EXPORT_EVENT_ICAL", description = "Генерация iCalendar файла для события")
    override fun generateICalendarFile(event: Event): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val calendar = createCalendar(event)
        val outputter = CalendarOutputter()
        outputter.output(calendar, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Создает календарь с одним событием.
     * Формирует объект календаря в формате iCalendar с добавлением мероприятия
     * и всей необходимой информации о нем.
     *
     * @param event Событие для добавления в календарь
     * @return Объект календаря
     */
    private fun createCalendar(event: Event): Calendar {
        val calendar = Calendar()
        calendar.properties.add(ProdId("-//GetScience//Event Calendar//RU"))
        calendar.properties.add(Version.VERSION_2_0)
        calendar.properties.add(CalScale.GREGORIAN)

        // Создаем событие календаря
        val startDate = DateTime(Date.from(event.dateStart.toInstant()))
        
        val endDate = DateTime(Date.from(event.dateEnd.toInstant()))

        val vEvent = VEvent(startDate, endDate, event.title)
        
        // Добавляем уникальный идентификатор
        vEvent.properties.add(uidGenerator.generateUid())
        
        // Добавляем описание события
        val description = buildEventDescription(event)
        vEvent.properties.add(Description(description))
        
        // Добавляем место проведения, если указано
        if (event.location.isNotBlank()) {
            vEvent.properties.add(Location(event.location))
        }
        
        // Добавляем URL события, если есть
        vEvent.properties.add(Url(java.net.URI("${ensureHttpsUrl(frontendBaseUrl)}/events/${event.eventId}")))
        
        // Добавляем организатора
        val organizer = event.organizer
        val organizerName = "${organizer.firstName} ${organizer.lastName}"
        val organizerEmail = organizer.account.email
        
        val organizerProperty = Organizer("mailto:$organizerEmail")
        organizerProperty.parameters.add(net.fortuna.ical4j.model.parameter.Cn(organizerName))
        vEvent.properties.add(organizerProperty)
        
        // Добавляем событие в календарь
        calendar.components.add(vEvent)
        
        return calendar
    }

    /**
     * Формирует подробное описание события для экспорта в календарь.
     * Включает основное описание, тип, тему, формат, локацию и информацию об организаторе.
     *
     * @param event Событие для описания
     * @return Текстовое описание
     */
    private fun buildEventDescription(event: Event): String {
        val sb = StringBuilder()
        
        // Добавляем основное описание
        if (event.description.isNotBlank()) {
            sb.append(event.description)
            sb.append("\n\n")
        }
        
        // Добавляем метаданные события
        sb.append("Тип мероприятия: ${translateEventType(event.type)}\n")
        sb.append("Тема: ${event.theme ?: "Не указана"}\n")
        sb.append("Формат: ${translateEventFormat(event.format)}\n")
        
        if (event.location.isNotBlank()) {
            sb.append("Место проведения: ${event.location}\n")
        }
        
        // Добавляем информацию об организаторе
        val organizer = event.organizer
        sb.append("Организатор: ${organizer.firstName} ${organizer.lastName}\n")
        
        // Добавляем ссылку на страницу события
        sb.append("\nСтраница мероприятия: ${ensureHttpsUrl(frontendBaseUrl)}/events/${event.eventId}")
        
        return sb.toString()
    }
    
    /**
     * Переводит тип мероприятия на русский язык.
     *
     * @param type Тип мероприятия
     * @return Локализованное название типа
     */
    private fun translateEventType(type: EventType): String = when(type) {
        EventType.CONFERENCE -> "Конференция"
        EventType.SEMINAR -> "Семинар"
        // Если добавятся новые типы, их можно будет обработать здесь
    }
    
    /**
     * Переводит формат мероприятия на русский язык.
     *
     * @param format Формат мероприятия
     * @return Локализованное название формата
     */
    private fun translateEventFormat(format: EventFormat): String = when(format) {
        EventFormat.OFFLINE -> "Очно"
        EventFormat.ONLINE -> "Онлайн"
        EventFormat.HYBRID -> "Гибрид"
    }

    /**
     * Создает Excel файл со списком участников мероприятия.
     * Файл содержит имена, контактные данные, типы заявок, даты подачи и статусы.
     * Участники отсортированы: сначала принятые заявки, затем по имени.
     *
     * @param event Мероприятие с заявками
     * @return Содержимое Excel файла в виде массива байтов
     */
    @LogBusinessOperation(operationType = "EXPORT_EVENT_PARTICIPANTS_EXCEL", description = "Генерация Excel файла участников события")
    @Transactional
    override fun generateEventParticipantsExcel(event: Event): ByteArray {

        // Сортируем заявки: сначала по статусу (принятые вверху), затем по имени
        val sortedApplications = event.applications.sortedWith(
            compareBy(
                { it.status != "APPROVED" }, // Сначала APPROVED
                { it.profile.firstName },
                { it.profile.lastName }
            )
        )

        // Создаем Excel-документ
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Участники")

        // Создаем стили
        val headerFont = workbook.createFont()
        headerFont.bold = true
        
        val headerStyle = workbook.createCellStyle()
        headerStyle.setFont(headerFont)
        
        val dateStyle = workbook.createCellStyle()
        val dateFormat = workbook.createDataFormat()
        dateStyle.dataFormat = dateFormat.getFormat("dd.MM.yyyy HH:mm")
        
        val wrapTextStyle = workbook.createCellStyle()
        wrapTextStyle.wrapText = true

        // Создаем заголовки
        val header = sheet.createRow(0)
        val headers = listOf(
            "Имя", 
            "Фамилия", 
            "Email", 
            "Тип заявки", 
            "Дата подачи", 
            "Сообщение", 
            "Статус", 
            "Результат проверки"
        )
        
        headers.forEachIndexed { index, title ->
            val cell = header.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Заполняем данные
        sortedApplications.forEachIndexed { index, application ->
            val row = sheet.createRow(index + 1)
            
            // Имя
            row.createCell(0).setCellValue(application.profile.firstName)
            
            // Фамилия
            row.createCell(1).setCellValue(application.profile.lastName)
            
            // Email
            row.createCell(2).setCellValue(application.profile.account.email)
            
            // Тип заявки
            val applicationType = if (application.isObserver) "Наблюдатель" else "Участник"
            row.createCell(3).setCellValue(applicationType)
            
            // Дата подачи
            val dateCell = row.createCell(4)
            dateCell.setCellValue(Date(application.submissionDate.time))
            dateCell.cellStyle = dateStyle
            
            // Сообщение (с ограничением длины ячейки)
            val messageCell = row.createCell(5)
            messageCell.setCellValue(application.message ?: "")
            messageCell.cellStyle = wrapTextStyle
            
            // Статус
            row.createCell(6).setCellValue(application.status)
            
            // Результат проверки
            row.createCell(7).setCellValue(application.verdict ?: "")
        }

        // Устанавливаем фиксированную ширину столбцов вместо autoSizeColumn
        // для избежания использования AWT/X11 в headless окружении
        val columnWidths = intArrayOf(15, 20, 30, 15, 20, 30, 15, 25)
        for (i in headers.indices) {
            sheet.setColumnWidth(i, columnWidths[i] * 256) // 256 - множитель для конвертации в единицы POI
        }

        // Сохраняем в ByteArray
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        
        return outputStream.toByteArray()
    }

    /**
     * Обеспечивает корректный формат URL, добавляя протокол https://, если он отсутствует.
     *
     * @param url исходный URL
     * @return URL с добавленным протоколом https://, если он отсутствовал
     */
    private fun ensureHttpsUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
    }
} 