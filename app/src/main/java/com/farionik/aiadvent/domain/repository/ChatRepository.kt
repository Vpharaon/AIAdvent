package com.farionik.aiadvent.domain.repository

import com.farionik.aiadvent.data.dto.CountryInfo

interface ChatRepository {
    suspend fun sendMessage(message: String, apiKey: String): Result<String>
    suspend fun getCountryInfo(countryName: String, apiKey: String): Result<CountryInfo>
    suspend fun validateCountryName(text: String, apiKey: String): Result<Boolean>
}