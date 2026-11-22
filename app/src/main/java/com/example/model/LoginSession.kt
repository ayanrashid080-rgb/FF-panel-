package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class LoginSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val activationKey: String = "",
    val ipAddress: String = "",
    val hardwareId: String = "",
    val consentStatus: String = "ALLOWED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
