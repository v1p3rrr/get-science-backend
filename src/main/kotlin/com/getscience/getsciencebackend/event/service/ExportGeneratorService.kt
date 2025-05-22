package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.event.data.model.Event

/**
 * Сервис для генерации файлов экспорта событий в различные форматы календаря
 */
interface ExportGeneratorService {
    
    /**
     * Возвращает сгенерированный iCalendar файл в виде массива байтов
     *
     * @param event Мероприятие для экспорта
     * @return Содержимое .ics файла в виде массива байтов
     */
    fun generateICalendarFile(event: Event): ByteArray
    
    /**
     * Создает Excel файл со списком заявок на мероприятие
     *
     * @param event Мероприятие
     * @return Содержимое Excel файла в виде массива байтов
     */
    fun generateEventParticipantsExcel(event: Event): ByteArray
} 