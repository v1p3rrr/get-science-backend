package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import org.springframework.web.multipart.MultipartFile

/**
 * Сервис для управления профилями пользователей.
 * 
 * Предоставляет методы для получения, обновления профилей пользователей,
 * работы с аватарами и поиска профилей.
 */
interface ProfileService {
    /**
     * Находит профиль пользователя по email аккаунта.
     * 
     * @param email email пользователя
     * @return данные профиля или null, если профиль не найден
     * @throws IllegalArgumentException если профиль не найден
     */
    fun findByAccountEmail(email: String): ProfileResponse?
    
    /**
     * Находит профиль пользователя по его идентификатору.
     * 
     * @param profileId идентификатор профиля
     * @return данные профиля или null, если профиль не найден
     * @throws IllegalArgumentException если профиль не найден
     */
    fun findByProfileId(profileId: Long): ProfileResponse?
    
    /**
     * Обновляет профиль пользователя.
     * 
     * @param profile новые данные профиля
     * @param email email пользователя для идентификации
     * @return обновленные данные профиля
     * @throws IllegalArgumentException если профиль или аккаунт не найдены
     */
    fun updateProfile(profile: ProfileRequest, email: String): ProfileResponse
    
    /**
     * Ищет профили пользователей по заданным критериям.
     * 
     * Поиск выполняется по частичному совпадению email, имени и фамилии.
     * 
     * @param email часть email для поиска (опционально)
     * @param firstName часть имени для поиска (опционально)
     * @param lastName часть фамилии для поиска (опционально)
     * @return список найденных профилей
     */
    fun searchProfiles(email: String?, firstName: String?, lastName: String?): List<ProfileResponse>
    
    /**
     * Получает профиль пользователя вместе с информацией о его мероприятиях.
     * 
     * @param profileId идентификатор профиля
     * @return данные профиля с информацией о мероприятиях
     * @throws IllegalArgumentException если профиль не найден
     */
    fun getProfileWithEvents(profileId: Long): ProfileResponse
    
    /**
     * Загружает и устанавливает аватар для профиля пользователя.
     * 
     * Если у пользователя уже был аватар, предыдущий файл удаляется.
     * 
     * @param file файл аватара
     * @param email email пользователя для идентификации
     * @return обновленные данные профиля с URL аватара
     * @throws IllegalArgumentException если профиль не найден
     */
    fun uploadAvatar(file: MultipartFile, email: String): ProfileResponse
}
