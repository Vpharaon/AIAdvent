package com.farionik.aiadvent.data.repository

import com.farionik.aiadvent.data.dto.CountryInfo
import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiClient: ApiClient,
) : ChatRepository {
    override suspend fun sendMessage(message: String, apiKey: String): Result<String> {
        return try {
            val response = apiClient.sendMessage(message, apiKey)
            val content = response.choices.firstOrNull()?.message?.content ?: "Нет ответа"
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCountryInfo(countryName: String, apiKey: String): Result<CountryInfo> {
        return try {
            val countryInfo = apiClient.getCountryInfo(countryName, apiKey)
            if (countryInfo != null) {
                Result.success(countryInfo)
            } else {
                Result.failure(Exception("Не удалось получить информацию о стране"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateCountryName(text: String, apiKey: String): Result<Boolean> {
        return try {
            val isValid = apiClient.validateCountryName(text, apiKey)
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}