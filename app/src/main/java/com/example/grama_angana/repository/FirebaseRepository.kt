package com.example.grama_angana.repository

import android.util.Log
import com.example.grama_angana.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Existing Firestore collections
    private val bookingsCol = db.collection("bookings")
    private val maintenanceCol = db.collection("maintenanceItems")
    private val usersCol = db.collection("users")
    private val TAG = "FirebaseRepository"

    // Remote Config for superuser management
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // Set to 3600 for production
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf("super_users" to ""))
    }

    // ----------------------------------------------------------------
    // Auth helpers
    // ----------------------------------------------------------------
    val currentUser get() = auth.currentUser
    fun isUserLoggedIn() = auth.currentUser != null
    fun getUserId() = auth.currentUser?.uid ?: ""

    fun authStateFlow(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ----------------------------------------------------------------
    // User Profile management
    // ----------------------------------------------------------------
    suspend fun saveUserProfile(user: User) = withContext(Dispatchers.IO) {
        usersCol.document(user.id).set(user, SetOptions.merge()).await()
    }

    fun getUserProfile(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = usersCol.document(userId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    // ----------------------------------------------------------------
    // Bookings operations (unchanged)
    // ----------------------------------------------------------------
    fun bookingsFor(date: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCol
            .whereEqualTo("date", date)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                }?.map { it.toDomain() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun userBookings(userId: String): Flow<List<Booking>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = bookingsCol
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                }?.map { it.toDomain() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun bookRoom(booking: Booking) = withContext(Dispatchers.IO) {
        db.runTransaction { txn ->
            val docId = "${booking.date}_${booking.timeSlot.name}"
            val docRef = bookingsCol.document(docId)

             val docSnapshot = txn.get(docRef)
            if (docSnapshot.exists()) {
                val existing = docSnapshot.toObject(BookingEntity::class.java)
                if (existing?.status != BookingStatus.CANCELLED.name) {
                    throw IllegalStateException("Time slot already occupied")
                }
            }
            val entity = booking.toEntity().copy(id = docId)
            txn.set(docRef, entity, SetOptions.merge())
            true
        }.await()
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) = withContext(Dispatchers.IO) {
        bookingsCol.document(bookingId).update("status", status.name).await()
    }

    suspend fun deleteBooking(bookingId: String) = withContext(Dispatchers.IO) {
        bookingsCol.document(bookingId).delete().await()
    }

    fun allMaintenanceItems(): Flow<List<MaintenanceItem>> = callbackFlow {
        val listener = maintenanceCol.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(MaintenanceItemEntity::class.java)?.copy(id = doc.id)
            }?.map { it.toDomain() } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun userMaintenanceRequests(userId: String): Flow<List<MaintenanceItem>> = callbackFlow {
        if (userId.isBlank()) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = maintenanceCol.whereEqualTo("requesterId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(MaintenanceItemEntity::class.java)?.copy(id = doc.id)
                }?.map { it.toDomain() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMaintenanceItem(item: MaintenanceItem, requesterId: String) = withContext(Dispatchers.IO) {
        val docRef = maintenanceCol.document()
        val entity = item.toEntity().copy(id = docRef.id, requesterId = requesterId)
        docRef.set(entity).await()
    }

    suspend fun updateMaintenanceItemCategory(itemId: String, category: String) = withContext(Dispatchers.IO) {
        maintenanceCol.document(itemId).update("category", category).await()
    }

    suspend fun deleteMaintenanceItem(itemId: String) = withContext(Dispatchers.IO) {
        maintenanceCol.document(itemId).delete().await()
    }

    suspend fun updateMaintenancePledgedAmount(itemId: String, newPledgedAmount: Double) = withContext(Dispatchers.IO) {
        maintenanceCol.document(itemId).update("pledgedAmount", newPledgedAmount).await()
    }

    fun logout() { auth.signOut() }

    // Superuser management
    suspend fun registerUserAsSuperUser(uid: String, email: String) = withContext(Dispatchers.IO) {
        usersCol.document(uid).set(
            mapOf("id" to uid, "email" to email, "role" to "superuser", "isSuperUser" to true, "canApprove" to true),
            SetOptions.merge()

        ).await()
    }

    suspend fun removeSuperUserPrivileges(uid: String) = withContext(Dispatchers.IO) {
        usersCol.document(uid).update(
            mapOf("role" to "user", "isSuperUser" to false, "canApprove" to false)
        ).await()
    }

    // ----------------------------------------------------------------
    // Superuser management via Remote Config
    // ----------------------------------------------------------------
    // Remote Config is read-only from the client. To add/remove superusers,
    // update the "super_users" parameter in the Firebase Console:
    //   Firebase Console → Remote Config → Add parameter "super_users"
    //   Value: comma-separated list of emails or UIDs, e.g. "admin1@example.com,admin2@example.com"
    // After updating, the app will fetch the new list on next launch (or when fetchAndActivate is called).

    // Fetch superusers list from Remote Config (comma-separated string)
    suspend fun getRemoteConfigSuperUsers(): List<String> = withContext(Dispatchers.IO) {
        val result = remoteConfig.fetchAndActivate().await()
        Log.d(TAG, "Remote Config fetch result: $result")
        val rawValue = remoteConfig.getString("super_users")
        Log.d(TAG, "Raw super_users value from Remote Config: '$rawValue'")
        val list = rawValue.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        Log.d(TAG, "Parsed super_users list: $list")
        list
    }

    // Check if the current user is a superuser according to Remote Config
    suspend fun isSuperUserFromRemoteConfig(): Boolean = withContext(Dispatchers.IO) {
        val superUsers = getRemoteConfigSuperUsers()
        val user = auth.currentUser ?: return@withContext false
        val isSuper = superUsers.contains(user.email) || superUsers.contains(user.uid)
        Log.d(TAG, "isSuperUserFromRemoteConfig: email=${user.email}, uid=${user.uid}, superUsers=$superUsers, result=$isSuper")
        isSuper
    }

    // Debug function: call this from anywhere to test Remote Config superuser setup
    suspend fun testRemoteConfigSuperUser(): String = withContext(Dispatchers.IO) {
        val superUsers = getRemoteConfigSuperUsers()
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: "No user logged in"
        val uid = currentUser?.uid ?: "N/A"
        val isSuper = if (currentUser != null) {
            superUsers.contains(email) || superUsers.contains(uid)
        } else false
        val result = """
            Remote Config Test:
            - super_users fetched: $superUsers
            - Current user email: $email
            - Current user uid: $uid
            - Is superuser: $isSuper
        """.trimIndent()
        Log.d(TAG, result)
        result
    }

    // Combined superuser check: Firestore role OR Remote Config list
    suspend fun isSuperUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        // Check Remote Config first
        val remoteConfigSuperUsers = getRemoteConfigSuperUsers()
        if (remoteConfigSuperUsers.contains(userId) || remoteConfigSuperUsers.contains(auth.currentUser?.email ?: "")) {
            return@withContext true
        }
        // Also check Firestore (existing method)
        val doc = usersCol.document(userId).get().await()
        val role = doc.getString("role")
        val isSuperUser = doc.getBoolean("isSuperUser") ?: false
        role == "superuser" || isSuperUser
    }
}
