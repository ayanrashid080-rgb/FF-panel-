package com.example.data

import android.content.Context
import com.example.R
import com.example.model.LoginSession
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface LoginSessionRepository {
    fun observeSessions(currentUserId: String, isAdmin: Boolean): Flow<List<LoginSession>>
    suspend fun recordConsentedSession(
        activationKey: String,
        ipAddress: String,
        hardwareId: String
    ): Result<Unit>
    suspend fun deleteSession(sessionId: String): Result<Unit>
}

class FirestoreLoginSessionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : LoginSessionRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeSessions(currentUserId: String, isAdmin: Boolean): Flow<List<LoginSession>> = flow {
        val uid = requireUserId()
        require(uid == currentUserId) { "Authenticated user mismatch." }
        val collectionRef = db.collection("login_sessions")
        val query = if (isAdmin) {
            collectionRef.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            collectionRef.whereEqualTo("userId", uid)
        }

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LoginSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "login_sessions")
                    }
                    throw error
                }
        )
    }

    override suspend fun recordConsentedSession(
        activationKey: String,
        ipAddress: String,
        hardwareId: String
    ): Result<Unit> {
        var docPath = "login_sessions"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google before writing.")
            val uid = user.uid
            val email = user.email?.takeIf { it.length >= 3 } ?: "verified-client@nexus.app"
            val docRef = db.collection("login_sessions").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "userEmail" to email.take(254),
                "activationKey" to activationKey.trim().take(64),
                "ipAddress" to ipAddress.trim().take(64),
                "hardwareId" to hardwareId.trim().take(64),
                "consentStatus" to "ALLOWED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val docPath = "login_sessions/$sessionId"
        return try {
            requireUserId()
            require(sessionId.isNotBlank()) { "Session ID must not be blank." }
            db.collection("login_sessions").document(sessionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
