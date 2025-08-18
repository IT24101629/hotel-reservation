// Hotel Reservation System - Main JavaScript

document.addEventListener('DOMContentLoaded', function() {
    console.log('Main.js loading...');
    
    // Check current page to avoid conflicts
    const currentPath = window.location.pathname;
    console.log('Current page:', currentPath);
    
    // Initialize components
    initializeFormValidation();
    initializeDatePickers();
    
    // Only initialize room search on pages that need it
    if (currentPath === '/rooms' || currentPath === '/' || currentPath === '/index' || currentPath === '/dashboard') {
        initializeRoomSearch();
    }
    
    // Only initialize booking form on booking page
    if (currentPath === '/booking') {
        initializeBookingForm();
    }
    
    // Only initialize payment form on payment page
    if (currentPath === '/payment') {
        initializePaymentForm();
    }
    
    initializeAlerts();
});

// Form Validation
function initializeFormValidation() {
    const forms = document.querySelectorAll('form[data-validate="true"]');

    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!validateForm(form)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
}

function validateForm(form) {
    let isValid = true;
    const requiredFields = form.querySelectorAll('[required]');

    requiredFields.forEach(field => {
        if (!field.value.trim()) {
            showFieldError(field, 'This field is required');
            isValid = false;
        } else {
            clearFieldError(field);
        }

        // Email validation
        if (field.type === 'email' && field.value) {
            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(field.value)) {
                showFieldError(field, 'Please enter a valid email address');
                isValid = false;
            }
        }

        // Password validation
        if (field.type === 'password' && field.value) {
            if (field.value.length < 6) {
                showFieldError(field, 'Password must be at least 6 characters long');
                isValid = false;
            }
        }

        // Phone validation
        if (field.type === 'tel' && field.value) {
            const phonePattern = /^[+]?[0-9]{10,15}$/;
            if (!phonePattern.test(field.value.replace(/\s/g, ''))) {
                showFieldError(field, 'Please enter a valid phone number');
                isValid = false;
            }
        }
    });

    return isValid;
}

function showFieldError(field, message) {
    clearFieldError(field);

    field.classList.add('error');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    errorDiv.style.color = '#e74c3c';
    errorDiv.style.fontSize = '0.9rem';
    errorDiv.style.marginTop = '0.25rem';

    field.parentNode.appendChild(errorDiv);
}

function clearFieldError(field) {
    field.classList.remove('error');
    const existingError = field.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
}

// Date Picker Initialization
function initializeDatePickers() {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    const today = new Date().toISOString().split('T')[0];

    dateInputs.forEach(input => {
        if (input.id === 'checkInDate' || input.id === 'checkOutDate') {
            input.min = today;
        }
    });

    // Check-in/Check-out date validation
    const checkInInput = document.getElementById('checkInDate');
    const checkOutInput = document.getElementById('checkOutDate');

    if (checkInInput && checkOutInput) {
        checkInInput.addEventListener('change', function() {
            checkOutInput.min = this.value;
            if (checkOutInput.value && checkOutInput.value <= this.value) {
                const nextDay = new Date(this.value);
                nextDay.setDate(nextDay.getDate() + 1);
                checkOutInput.value = nextDay.toISOString().split('T')[0];
            }
            calculateStayDuration();
        });

        checkOutInput.addEventListener('change', function() {
            calculateStayDuration();
        });
    }
}

function calculateStayDuration() {
    const checkInDate = document.getElementById('checkInDate');
    const checkOutDate = document.getElementById('checkOutDate');
    const durationDisplay = document.getElementById('stayDuration');

    if (checkInDate && checkOutDate && durationDisplay && checkInDate.value && checkOutDate.value) {
        const checkIn = new Date(checkInDate.value);
        const checkOut = new Date(checkOutDate.value);
        const diffTime = Math.abs(checkOut - checkIn);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        durationDisplay.textContent = `${diffDays} night${diffDays !== 1 ? 's' : ''}`;

        // Update total price if room price is available
        updateTotalPrice(diffDays);
    }
}

function updateTotalPrice(nights) {
    const pricePerNight = document.getElementById('roomPricePerNight');
    const totalPriceDisplay = document.getElementById('totalPrice');

    if (pricePerNight && totalPriceDisplay) {
        const price = parseFloat(pricePerNight.textContent.replace(/[^0-9.]/g, ''));
        const total = price * nights;
        totalPriceDisplay.textContent = `LKR ${total.toLocaleString('en-US', {minimumFractionDigits: 2})}`;
    }
}

// Room Search
function initializeRoomSearch() {
    const searchForm = document.getElementById('roomSearchForm');
    const searchButton = document.getElementById('searchRoomsBtn');

    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            e.preventDefault();
            searchRooms();
        });
    }

    if (searchButton) {
        searchButton.addEventListener('click', function(e) {
            e.preventDefault();
            searchRooms();
        });
    }
}

function searchRooms() {
    const formData = new FormData(document.getElementById('roomSearchForm'));
    const searchParams = new URLSearchParams();

    for (let [key, value] of formData.entries()) {
        if (value) {
            searchParams.append(key, value);
        }
    }

    showLoading('Searching available rooms...');

    fetch(`/api/rooms/available?${searchParams.toString()}`)
        .then(response => response.json())
        .then(data => {
            hideLoading();
            displaySearchResults(data);
        })
        .catch(error => {
            hideLoading();
            showAlert('Error searching rooms. Please try again.', 'error');
            console.error('Search error:', error);
        });
}

function displaySearchResults(rooms) {
    const resultsContainer = document.getElementById('searchResults');

    if (!resultsContainer) return;

    if (rooms.length === 0) {
        resultsContainer.innerHTML = `
            <div class="text-center p-3">
                <h3>No rooms available</h3>
                <p>Please try different dates or adjust your search criteria.</p>
            </div>
        `;
        return;
    }

    resultsContainer.innerHTML = rooms.map(room => `
        <div class="room-card">
            <img src="${room.imageUrl || '/images/room-default.jpg'}" alt="${room.roomType}" class="room-image" onerror="this.src='/images/room-default.jpg'">
            <div class="room-info">
                <h3 class="room-title">${room.roomType}</h3>
                <p class="room-description">${room.description || ''}</p>
                <div class="room-amenities">
                    <span class="amenity-tag">📶 WiFi</span>
                    <span class="amenity-tag">❄️ AC</span>
                    <span class="amenity-tag">📺 TV</span>
                    <span class="amenity-tag">🛁 Private Bathroom</span>
                </div>
                <div class="room-price">LKR ${room.pricePerNight.toLocaleString('en-US', {minimumFractionDigits: 2})} / night</div>
                <div class="room-details">
                    <p><strong>Room Number:</strong> ${room.roomNumber}</p>
                    <p><strong>Floor:</strong> ${room.floorNumber}</p>
                    <p><strong>Max Occupancy:</strong> ${room.maxOccupancy} guests</p>
                </div>
                <button class="btn btn-primary select-room-btn"
                        data-room-id="${room.roomId}"
                        data-room-number="${room.roomNumber}"
                        data-price="${room.pricePerNight}"
                        data-room-type="${room.roomType}"
                        data-max-occupancy="${room.maxOccupancy}">
                    📅 Book This Room
                </button>
            </div>
        </div>
    `).join('');

    // Attach event listeners to new buttons
    attachRoomSelectionListeners();
}

function attachRoomSelectionListeners() {
    document.querySelectorAll('.select-room-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();

            const roomId = this.dataset.roomId;
            const roomNumber = this.dataset.roomNumber;
            const pricePerNight = this.dataset.price;
            const roomType = this.dataset.roomType;
            const maxOccupancy = this.dataset.maxOccupancy;

            selectRoom(roomId, roomNumber, pricePerNight, roomType, maxOccupancy);
        });
    });
}

// Main selectRoom function
function selectRoom(roomId, roomNumber, pricePerNight, roomType, maxOccupancy) {
    console.log('selectRoom called with:', {roomId, roomNumber, pricePerNight, roomType, maxOccupancy});

    try {
        // Get search form values, with defaults if empty
        const checkInDate = document.getElementById('checkInDate')?.value || getDefaultCheckInDate();
        const checkOutDate = document.getElementById('checkOutDate')?.value || getDefaultCheckOutDate();
        const numberOfGuests = document.getElementById('guests')?.value || 2;

        // Store selected room data
        const selectedRoomData = {
            roomId: parseInt(roomId),
            roomNumber: roomNumber,
            pricePerNight: parseFloat(pricePerNight),
            roomType: roomType,
            maxOccupancy: parseInt(maxOccupancy) || 4,
            checkInDate: checkInDate,
            checkOutDate: checkOutDate,
            numberOfGuests: parseInt(numberOfGuests)
        };

        console.log('Storing room data in sessionStorage:', selectedRoomData);
        sessionStorage.setItem('selectedRoomData', JSON.stringify(selectedRoomData));

        // Show success message
        showAlert('Room selected! Redirecting to booking page...', 'success');

        // Redirect to booking page
        setTimeout(() => {
            window.location.href = '/booking';
        }, 1000);

    } catch (error) {
        console.error('Error in selectRoom:', error);
        showAlert('Error selecting room. Please try again.', 'error');
    }
}

function getDefaultCheckInDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
}

function getDefaultCheckOutDate() {
    const dayAfterTomorrow = new Date();
    dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 2);
    return dayAfterTomorrow.toISOString().split('T')[0];
}

// Booking Form
function initializeBookingForm() {
    const bookingForm = document.getElementById('bookingForm');

    if (bookingForm) {
        // Load selected room data
        const selectedRoomData = JSON.parse(sessionStorage.getItem('selectedRoomData') || '{}');

        console.log('Loading booking form with room data:', selectedRoomData);

        if (selectedRoomData.roomId) {
            const selectedRoomNumber = document.getElementById('selectedRoomNumber');
            const selectedRoomType = document.getElementById('selectedRoomType');
            const roomPricePerNight = document.getElementById('roomPricePerNight');
            const roomIdInput = document.getElementById('roomId');

            if (selectedRoomNumber) selectedRoomNumber.textContent = selectedRoomData.roomNumber;
            if (selectedRoomType) selectedRoomType.textContent = selectedRoomData.roomType;
            if (roomPricePerNight) roomPricePerNight.textContent = `LKR ${selectedRoomData.pricePerNight.toLocaleString('en-US', {minimumFractionDigits: 2})}`;
            if (roomIdInput) roomIdInput.value = selectedRoomData.roomId;

            console.log('Booking form initialized with room:', selectedRoomData.roomType, selectedRoomData.roomNumber);
        } else {
            console.log('No room data found in sessionStorage');
        }

        bookingForm.addEventListener('submit', function(e) {
            e.preventDefault();
            submitBooking();
        });
    }
}

function submitBooking() {
    const formData = new FormData(document.getElementById('bookingForm'));
    const bookingData = {};

    for (let [key, value] of formData.entries()) {
        bookingData[key] = value;
    }

    showLoading('Creating your booking...');

    fetch('/api/bookings/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || document.querySelector('input[name="_csrf"]')?.value || ''
        },
        body: JSON.stringify(bookingData)
    })
    .then(response => response.json())
    .then(data => {
        hideLoading();
        if (data.bookingId) {
            // Store booking data for payment
            sessionStorage.setItem('bookingData', JSON.stringify(data));
            // Redirect to payment
            window.location.href = '/payment';
        } else {
            showAlert(data.error || 'Error creating booking', 'error');
        }
    })
    .catch(error => {
        hideLoading();
        showAlert('Error creating booking. Please try again.', 'error');
        console.error('Booking error:', error);
    });
}

// Payment Form
function initializePaymentForm() {
    const paymentForm = document.getElementById('paymentForm');
    const paymentMethods = document.querySelectorAll('.payment-method');

    if (paymentForm) {
        // Load booking data
        const bookingData = JSON.parse(sessionStorage.getItem('bookingData') || '{}');

        if (bookingData.bookingId) {
            const bookingRef = document.getElementById('bookingReference');
            const paymentAmount = document.getElementById('paymentAmount');

            if (bookingRef) bookingRef.textContent = bookingData.bookingReference;
            if (paymentAmount) paymentAmount.textContent = `LKR ${bookingData.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}`;
        }

        paymentMethods.forEach(method => {
            method.addEventListener('click', function() {
                paymentMethods.forEach(m => m.classList.remove('selected'));
                this.classList.add('selected');
                const paymentMethodInput = document.getElementById('paymentMethod');
                if (paymentMethodInput) {
                    paymentMethodInput.value = this.dataset.method;
                }
            });
        });

        paymentForm.addEventListener('submit', function(e) {
            e.preventDefault();
            processPayment();
        });
    }
}

function processPayment() {
    const paymentMethodInput = document.getElementById('paymentMethod');
    const paymentMethod = paymentMethodInput ? paymentMethodInput.value : '';
    const bookingData = JSON.parse(sessionStorage.getItem('bookingData') || '{}');

    if (!paymentMethod) {
        showAlert('Please select a payment method', 'warning');
        return;
    }

    if (paymentMethod === 'PAYHERE') {
        // Initialize PayHere payment
        initializePayHerePayment(bookingData);
    } else {
        // Handle other payment methods
        showAlert('Payment method not implemented yet', 'info');
    }
}

function initializePayHerePayment(bookingData) {
    showLoading('Initializing PayHere payment...');

    fetch('/api/payment/payhere/init', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            bookingId: bookingData.bookingId,
            amount: bookingData.totalAmount
        })
    })
    .then(response => response.json())
    .then(data => {
        hideLoading();
        if (data.paymentUrl) {
            // Redirect to PayHere
            window.location.href = data.paymentUrl;
        } else {
            showAlert(data.error || 'Error initializing payment', 'error');
        }
    })
    .catch(error => {
        hideLoading();
        showAlert('Error initializing payment. Please try again.', 'error');
        console.error('Payment error:', error);
    });
}

// Utility Functions
function showLoading(message = 'Loading...') {
    // Remove existing loading overlay
    const existingLoading = document.getElementById('loadingOverlay');
    if (existingLoading) {
        existingLoading.remove();
    }

    const loadingDiv = document.createElement('div');
    loadingDiv.id = 'loadingOverlay';
    loadingDiv.innerHTML = `
        <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 9999; display: flex; align-items: center; justify-content: center;">
            <div style="background: white; padding: 2rem; border-radius: 10px; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.3);">
                <div class="spinner"></div>
                <p style="margin-top: 1rem; color: #333;">${message}</p>
            </div>
        </div>
    `;
    document.body.appendChild(loadingDiv);
}

function hideLoading() {
    const loadingDiv = document.getElementById('loadingOverlay');
    if (loadingDiv) {
        loadingDiv.remove();
    }
}

function showAlert(message, type = 'info') {
    // Remove existing alerts
    document.querySelectorAll('.dynamic-alert').forEach(alert => alert.remove());

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} dynamic-alert`;
    alertDiv.style.position = 'fixed';
    alertDiv.style.top = '20px';
    alertDiv.style.right = '20px';
    alertDiv.style.zIndex = '10000';
    alertDiv.style.minWidth = '300px';
    alertDiv.style.boxShadow = '0 5px 15px rgba(0,0,0,0.3)';
    alertDiv.innerHTML = `
        <span>${message}</span>
        <button type="button" onclick="this.parentElement.remove()" style="float: right; background: none; border: none; font-size: 1.2rem; cursor: pointer; color: inherit;">&times;</button>
    `;

    document.body.appendChild(alertDiv);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

function initializeAlerts() {
    // Auto-hide existing alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.dynamic-alert)');
    alerts.forEach(alert => {
        // Add close button if not present
        if (!alert.querySelector('button')) {
            const closeBtn = document.createElement('button');
            closeBtn.innerHTML = '&times;';
            closeBtn.style.cssText = 'float: right; background: none; border: none; font-size: 1.2rem; cursor: pointer; color: inherit;';
            closeBtn.onclick = () => alert.remove();
            alert.appendChild(closeBtn);
        }

        setTimeout(() => {
            if (alert.parentNode) {
                alert.style.opacity = '0';
                alert.style.transition = 'opacity 0.3s ease';
                setTimeout(() => alert.remove(), 300);
            }
        }, 5000);
    });
}

// Format currency
function formatCurrency(amount, currency = 'LKR') {
    return `${currency} ${amount.toLocaleString('en-US', {minimumFractionDigits: 2})}`;
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

// Dashboard functionality
function refreshDashboard() {
    fetch('/api/dashboard/stats')
        .then(response => response.json())
        .then(data => {
            updateDashboardStats(data);
        })
        .catch(error => {
            console.error('Error refreshing dashboard:', error);
        });
}

function updateDashboardStats(stats) {
    if (stats.totalBookings !== undefined) {
        const totalBookingsEl = document.getElementById('totalBookings');
        if (totalBookingsEl) totalBookingsEl.textContent = stats.totalBookings;
    }
    if (stats.todayCheckIns !== undefined) {
        const todayCheckInsEl = document.getElementById('todayCheckIns');
        if (todayCheckInsEl) todayCheckInsEl.textContent = stats.todayCheckIns;
    }
    if (stats.availableRooms !== undefined) {
        const availableRoomsEl = document.getElementById('availableRooms');
        if (availableRoomsEl) availableRoomsEl.textContent = stats.availableRooms;
    }
    if (stats.revenue !== undefined) {
        const revenueEl = document.getElementById('revenue');
        if (revenueEl) revenueEl.textContent = formatCurrency(stats.revenue);
    }
}

// Export functions for global use
window.selectRoom = selectRoom;
window.selectRoomForBooking = selectRoom; // Alias for consistency
window.showAlert = showAlert;
window.formatCurrency = formatCurrency;
window.formatDate = formatDate;
window.refreshDashboard = refreshDashboard;
window.showLoading = showLoading;
window.hideLoading = hideLoading;

// Make sure selectRoom is available immediately
console.log('Main.js loaded. selectRoom function available:', typeof window.selectRoom);