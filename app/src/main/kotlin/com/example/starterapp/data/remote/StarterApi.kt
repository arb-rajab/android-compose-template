package com.example.starterapp.data.remote

import com.example.starterapp.data.remote.dto.ActionRequestDto
import com.example.starterapp.data.remote.dto.ActionResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/** Retrofit surface for the backend. Add one suspend function per endpoint the app uses. */
interface StarterApi {
    @POST("api/v1/actions")
    suspend fun submitAction(@Body request: ActionRequestDto): ActionResponseDto
}
