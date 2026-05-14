package com.example.grama_angana.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grama_angana.data.*
import com.example.grama_angana.repository.FirebaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repo: FirebaseRepository = FirebaseRepository()) : ViewModel() {

    // Auth & User State
    private val _isUserLoggedIn = MutableStateFlow(repo.isUserLoggedIn())
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Superuser State
    private val _isSuperUser = MutableStateFlow(false)
    val isSuperUser: StateFlow<Boolean> = _isSuperUser.asStateFlow()

    val currentUserProfile: StateFlow<User?> = isUserLoggedIn
        .flatMapLatest { loggedIn ->
            val uid = repo.getUserId()
            if (loggedIn && uid.isNotBlank()) {
                repo.getUserProfile(uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Observe real-time auth changes from Firebase
        viewModelScope.launch {
            repo.authStateFlow().collect { loggedIn ->
                Log.d("MainViewModel", "Auth state changed: loggedIn = $loggedIn")
                _isUserLoggedIn.value = loggedIn
                if (loggedIn) {
                    // Check Remote Config superuser status
                    viewModelScope.launch {
                        val isSuper = repo.isSuperUserFromRemoteConfig()
                        _isSuperUser.value = isSuper
                        Log.d("MainViewModel", "Remote Config superuser: $isSuper")
                    }
                } else {
                    _isSuperUser.value = false
                }
            }
        }
    }

    fun refreshAuthState() {
        _isUserLoggedIn.value = repo.isUserLoggedIn()
    }

    // Tracking State - Reactively filters based on login status and userId
    val myBookings: StateFlow<List<Booking>> = isUserLoggedIn
        .flatMapLatest { loggedIn ->
            val uid = repo.getUserId()
            if (loggedIn && uid.isNotBlank()) repo.userBookings(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myMaintenanceRequests: StateFlow<List<MaintenanceItem>> = isUserLoggedIn
        .flatMapLatest { loggedIn ->
            val uid = repo.getUserId()
            if (loggedIn && uid.isNotBlank()) repo.userMaintenanceRequests(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hall Schedule State
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val bookingsForSelectedDate: StateFlow<List<Booking>> =
        _selectedDate
            .flatMapLatest { date -> repo.bookingsFor(date.toString()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Maintenance State
    val maintenanceItems: StateFlow<List<MaintenanceItem>> =
        repo.allMaintenanceItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    // Actions
    fun updateProfile(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val uid = repo.getUserId()
            if (uid.isNotBlank()) {
                val updatedUser = User(id = uid, name = name, phoneNumber = phoneNumber, email = repo.currentUser?.email ?: "")
                runCatching { repo.saveUserProfile(updatedUser) }
                    .onSuccess { _statusMessage.value = "Profile updated!" }
                    .onFailure { _statusMessage.value = "Failed to update profile" }
            }
        }
    }

    fun getUserProfile(userId: String) = repo.getUserProfile(userId)

    fun bookSlot(slot: TimeSlot, purpose: String, userId: String, userName: String) {
        viewModelScope.launch {
            val booking = Booking(
                date = _selectedDate.value.toString(),
                timeSlot = slot,
                purpose = purpose,
                status = BookingStatus.PENDING,
                userId = userId,
                userName = userName
            )
            runCatching { 
                Log.d("MainViewModel", "Attempting to book slot: $booking")
                repo.bookRoom(booking) 
            }
                .onSuccess { 
                    _statusMessage.value = "Booked successfully! Pending approval."
                }
                .onFailure { 
                    _statusMessage.value = it.message ?: "Booking failed"
                }
        }
    }

    fun addMaintenanceRequest(name: String, goalAmount: Double) {
        viewModelScope.launch {
            val uid = repo.getUserId()
            val uName = currentUserProfile.value?.name ?: "Anonymous"
            if (uid.isNotBlank()) {
                val newItem = MaintenanceItem(
                    name = name,
                    goalAmount = goalAmount,
                    pledgedAmount = 0.0,
                    category = "REQUESTED",
                    requesterId = uid,
                    requesterName = uName
                )
                runCatching { repo.addMaintenanceItem(newItem, uid) }
                    .onSuccess { _statusMessage.value = "Request submitted successfully!" }
                    .onFailure { _statusMessage.value = "Failed to submit request" }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _isUserLoggedIn.value = false
        }
    }

    // Admin Actions (superuser only)
    fun approveBooking(bookingId: String) {
        if (!_isSuperUser.value) {
            _statusMessage.value = "Only superuser can approve bookings"
            return
        }
        viewModelScope.launch {
            runCatching { repo.updateBookingStatus(bookingId, BookingStatus.CONFIRMED) }
                .onSuccess { _statusMessage.value = "Booking approved!" }
        }
    }

    fun deleteBooking(bookingId: String) {
        if (!_isSuperUser.value) {
            _statusMessage.value = "Only superuser can delete bookings"
            return
        }
        viewModelScope.launch {
            runCatching { repo.deleteBooking(bookingId) }
                .onSuccess { _statusMessage.value = "Booking deleted!" }
        }
    }

    fun approveMaintenanceItem(itemId: String) {
        if (!_isSuperUser.value) {
            _statusMessage.value = "Only superuser can approve maintenance items"
            return
        }
        viewModelScope.launch {
            runCatching { repo.updateMaintenanceItemCategory(itemId, "EQUIPMENT") }
                .onSuccess { _statusMessage.value = "Maintenance item approved!" }
        }
    }

    fun cancelMaintenanceItem(itemId: String) {
        if (!_isSuperUser.value) {
            _statusMessage.value = "Only superuser can cancel maintenance requests"
            return
        }
        viewModelScope.launch {
            runCatching { repo.deleteMaintenanceItem(itemId) }
                .onSuccess { _statusMessage.value = "Maintenance request cancelled!" }
        }
    }

    fun updatePledgedAmount(itemId: String, currentPledged: Double, additionalAmount: Double) {
        viewModelScope.launch {
            runCatching { repo.updateMaintenancePledgedAmount(itemId, currentPledged + additionalAmount) }
                .onSuccess { _statusMessage.value = "Pledge updated!" }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }
}
