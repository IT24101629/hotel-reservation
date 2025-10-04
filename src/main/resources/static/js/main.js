// Hotel Reservation System - Main JavaScript (Modernized, Dark-Theme Friendly)

(function () {
    console.log('Main.js loading...');

    // -------------------------------
    // Config (change in one place)
    // -------------------------------
    const API = {
        searchRooms: '/api/rooms/search',     // <-- use this by default
        availableRooms: '/api/rooms/available'
    };
    const ROOMS_ENDPOINT = API.searchRooms;  // switch to availableRooms if your backend uses that

    // -------------------------------
    // Helpers (dates, currency)
    // -------------------------------
    function formatCurrency(amount, currency = 'LKR') {
        return `${currency} ${Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    }

    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
    }

    function getDefaultCheckInDate() {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        return tomorrow.toISOString().split('T')[0];
    }

    function getDefaultCheckOutDate() {
        const after = new Date();
        after.setDate(after.getDate() + 2);
        return after.toISOString().split('T')[0];
    }

    // Map fallback images by room type (used if room.imageUrl is empty)
    function imageForRoom(room) {
        if (room?.imageUrl && String(room.imageUrl).trim()) return room.imageUrl;
        const map = {
            'Standard Single': '/images/rooms/standard-single.jpg',
            'Standard Double': '/images/rooms/standard-double.jpg',
            'Deluxe Room': '/images/rooms/deluxe.jpg',
            'Family Suite': '/images/rooms/family-suite.jpg',
            'Presidential Suite': '/images/rooms/presidential-suite.jpg'
        };
        return map[room?.roomType] || '/images/room-default.jpg';
    }

    // -------------------------------
    // UI: Loading & Alerts (dark)
    // -------------------------------
    function showLoading(message = 'Loading...') {
        const existing = document.getElementById('loadingOverlay');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.id = 'loadingOverlay';
        overlay.innerHTML = `
      <div style="
        position:fixed; inset:0; z-index:9999;
        background: rgba(0,0,0,0.6);
        display:flex; align-items:center; justify-content:center;">
        <div style="
          background:#121214; color:#fff; border:1px solid #2a2b30;
          padding:1.25rem 1.5rem; border-radius:16px; text-align:center;
          box-shadow: 0 16px 40px rgba(0,0,0,.45); min-width:280px;">
          <div class="spinner-border text-golden" style="width:2.25rem;height:2.25rem;border-width:.25rem" role="status"></div>
          <p style="margin:.9rem 0 0; color:#c8c8cf">${message}</p>
        </div>
      </div>
    `;
        document.body.appendChild(overlay);
    }

    function hideLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) overlay.remove();
    }

    function showAlert(message, type = 'info') {
        // Remove existing dynamic alerts
        document.querySelectorAll('.dynamic-alert').forEach(a => a.remove());

        const palette = {
            success: { bg: 'rgba(34,197,94,.12)', color: '#34d399' },
            danger:  { bg: 'rgba(239,68,68,.12)', color: '#fb7171' },
            error:   { bg: 'rgba(239,68,68,.12)', color: '#fb7171' },
            warning: { bg: 'rgba(255,215,0,.10)', color: '#ffd700' },
            info:    { bg: 'rgba(255,255,255,.06)', color: '#e5e7eb' }
        };
        const theme = palette[type] || palette.info;

        const alertDiv = document.createElement('div');
        alertDiv.className = 'dynamic-alert';
        alertDiv.style.cssText = `
      position:fixed; top:20px; right:20px; z-index:10000; min-width:300px;
      background:${theme.bg}; color:${theme.color};
      border:1px solid rgba(255,255,255,0.1); backdrop-filter: blur(6px);
      padding: .9rem 1rem; border-radius: 14px; box-shadow: 0 10px 30px rgba(0,0,0,0.35);
    `;
        alertDiv.innerHTML = `
      <span>${message}</span>
      <button type="button" aria-label="Close"
        style="float:right; background:none; border:none; font-size:1.2rem; cursor:pointer; color:inherit;">&times;</button>
    `;
        alertDiv.querySelector('button').onclick = () => alertDiv.remove();

        document.body.appendChild(alertDiv);
        setTimeout(() => alertDiv.remove(), 5000);
    }

    function initializeAlerts() {
        const alerts = document.querySelectorAll('.alert:not(.dynamic-alert)');
        alerts.forEach(alert => {
            if (!alert.querySelector('button')) {
                const closeBtn = document.createElement('button');
                closeBtn.innerHTML = '&times;';
                closeBtn.style.cssText = 'float:right;background:none;border:none;font-size:1.2rem;cursor:pointer;color:inherit;';
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

    // -------------------------------
    // Form Validation
    // -------------------------------
    function initializeFormValidation() {
        const forms = document.querySelectorAll('form[data-validate="true"]');
        forms.forEach(form => {
            if (form.dataset.validated === '1') return; // guard against double-binding
            form.dataset.validated = '1';
            form.addEventListener('submit', function (e) {
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
            if (!String(field.value || '').trim()) {
                showFieldError(field, 'This field is required');
                isValid = false;
            } else {
                clearFieldError(field);
            }

            // Email
            if (field.type === 'email' && field.value) {
                const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailPattern.test(field.value)) {
                    showFieldError(field, 'Please enter a valid email address');
                    isValid = false;
                }
            }

            // Password
            if (field.type === 'password' && field.value && field.value.length < 6) {
                showFieldError(field, 'Password must be at least 6 characters long');
                isValid = false;
            }

            // Phone
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
        errorDiv.style.color = '#fb7171';
        errorDiv.style.fontSize = '0.9rem';
        errorDiv.style.marginTop = '0.25rem';
        field.parentNode.appendChild(errorDiv);
    }

    function clearFieldError(field) {
        field.classList.remove('error');
        const existingError = field.parentNode?.querySelector('.field-error');
        if (existingError) existingError.remove();
    }

    // -------------------------------
    // Date pickers & stay duration
    // -------------------------------
    function initializeDatePickers() {
        const dateInputs = document.querySelectorAll('input[type="date"]');
        const today = new Date().toISOString().split('T')[0];

        dateInputs.forEach(input => {
            if (input.id === 'checkInDate' || input.id === 'checkOutDate') {
                input.min = today;
            }
        });

        const checkInInput = document.getElementById('checkInDate');
        const checkOutInput = document.getElementById('checkOutDate');

        if (checkInInput && checkOutInput) {
            checkInInput.addEventListener('change', function () {
                checkOutInput.min = this.value;
                if (checkOutInput.value && checkOutInput.value <= this.value) {
                    const nextDay = new Date(this.value);
                    nextDay.setDate(nextDay.getDate() + 1);
                    checkOutInput.value = nextDay.toISOString().split('T')[0];
                }
                calculateStayDuration();
            });

            checkOutInput.addEventListener('change', function () {
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
            updateTotalPrice(diffDays);
        }
    }

    function updateTotalPrice(nights) {
        const pricePerNight = document.getElementById('roomPricePerNight');
        const totalPriceDisplay = document.getElementById('totalPrice');

        if (pricePerNight && totalPriceDisplay) {
            const price = parseFloat(String(pricePerNight.textContent).replace(/[^0-9.]/g, '')) || 0;
            const total = price * (nights || 0);
            totalPriceDisplay.textContent = formatCurrency(total);
        }
    }

    // -------------------------------
    // Rooms: search and results
    // -------------------------------
    function initializeRoomSearch() {
        const searchForm = document.getElementById('roomSearchForm');
        const searchButton = document.getElementById('searchRoomsBtn');

        if (searchForm && !searchForm.dataset.bound) {
            searchForm.dataset.bound = '1';
            searchForm.addEventListener('submit', function (e) {
                e.preventDefault();
                searchRooms();
            });
        }

        if (searchButton && !searchButton.dataset.bound) {
            searchButton.dataset.bound = '1';
            searchButton.addEventListener('click', function (e) {
                e.preventDefault();
                searchRooms();
            });
        }
    }

    function searchRooms() {
        const form = document.getElementById('roomSearchForm');
        if (!form) return;

        const formData = new FormData(form);
        const searchParams = new URLSearchParams();
        for (let [key, value] of formData.entries()) {
            if (value) searchParams.append(key, value);
        }

        showLoading('Searching available rooms...');
        fetch(`${ROOMS_ENDPOINT}?${searchParams.toString()}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                hideLoading();
                displaySearchResults(Array.isArray(data) ? data : []);
            })
            .catch(err => {
                hideLoading();
                showAlert('Error searching rooms. Please try again.', 'error');
                console.error('Search error:', err);
            });
    }

    function displaySearchResults(rooms) {
        const resultsContainer = document.getElementById('searchResults');
        if (!resultsContainer) return;

        if (!rooms || rooms.length === 0) {
            resultsContainer.innerHTML = `
        <div class="text-center p-3">
          <h3 class="text-golden">No rooms available</h3>
          <p>Please try different dates or adjust your search criteria.</p>
        </div>
      `;
            return;
        }

        resultsContainer.innerHTML = rooms.map(room => `
      <div class="room-card-wrapper fade-in">
        <div class="room-card">
          <div class="room-image-container">
            <img src="${imageForRoom(room)}"
                 alt="${room.roomType}"
                 class="room-image"
                 onerror="this.src='/images/room-default.jpg'">
            <div class="room-overlay"></div>
            <div class="room-price-badge">
              <span class="price-amount">${formatCurrency(room.pricePerNight)}</span>
              <span class="price-period">/night</span>
            </div>
            <div class="room-capacity-badge">
              <i class="fas fa-users me-2"></i>${room.maxOccupancy}
            </div>
          </div>

          <div class="room-content">
            <div class="room-header">
              <h3 class="room-title">${room.roomType}</h3>
              <p class="room-description">${room.description || ''}</p>
            </div>

            <div class="room-details">
              <div class="detail-item">
                <i class="fas fa-door-open detail-icon"></i>
                <span class="detail-label">Room</span>
                <span class="detail-value">${room.roomNumber}</span>
              </div>
              <div class="detail-item">
                <i class="fas fa-building detail-icon"></i>
                <span class="detail-label">Floor</span>
                <span class="detail-value">${room.floorNumber}</span>
              </div>
            </div>

            <div class="room-amenities">
              <div class="amenities-header">
                <i class="fas fa-star me-2"></i>
                <span>Room Amenities</span>
              </div>
              <div class="amenities-grid">
                <div class="amenity-item"><i class="fas fa-wifi"></i><span>WiFi</span></div>
                <div class="amenity-item"><i class="fas fa-snowflake"></i><span>AC</span></div>
                <div class="amenity-item"><i class="fas fa-tv"></i><span>TV</span></div>
                <div class="amenity-item"><i class="fas fa-bath"></i><span>Bathroom</span></div>
              </div>
            </div>

            <div class="room-actions">
              <button class="btn-book-room select-room-btn"
                      data-room-id="${room.roomId}"
                      data-room-number="${room.roomNumber}"
                      data-price="${room.pricePerNight}"
                      data-room-type="${room.roomType}"
                      data-max-occupancy="${room.maxOccupancy}">
                <div class="btn-content">
                  <i class="fas fa-calendar-check me-2"></i>
                  <span>Book This Room</span>
                </div>
                <div class="btn-shimmer"></div>
              </button>
            </div>
          </div>
        </div>
      </div>
    `).join('');

        attachRoomSelectionListeners();
    }

    function attachRoomSelectionListeners() {
        document.querySelectorAll('.select-room-btn').forEach(btn => {
            if (btn.dataset.bound === '1') return;
            btn.dataset.bound = '1';
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                const { roomId, roomNumber, price, roomType, maxOccupancy } = this.dataset;
                selectRoom(roomId, roomNumber, price, roomType, maxOccupancy);
            });
        });
    }

    function selectRoom(roomId, roomNumber, pricePerNight, roomType, maxOccupancy) {
        try {
            const checkInDate = document.getElementById('checkInDate')?.value || getDefaultCheckInDate();
            const checkOutDate = document.getElementById('checkOutDate')?.value || getDefaultCheckOutDate();
            const numberOfGuests = document.getElementById('guests')?.value || 2;

            const selectedRoomData = {
                roomId: parseInt(roomId, 10),
                roomNumber: roomNumber,
                pricePerNight: parseFloat(pricePerNight),
                roomType: roomType,
                maxOccupancy: parseInt(maxOccupancy, 10) || 4,
                checkInDate,
                checkOutDate,
                numberOfGuests: parseInt(numberOfGuests, 10)
            };

            sessionStorage.setItem('selectedRoomData', JSON.stringify(selectedRoomData));
            showAlert('Room selected! Redirecting to booking page...', 'success');
            setTimeout(() => { window.location.href = '/booking'; }, 800);
        } catch (err) {
            console.error('Error in selectRoom:', err);
            showAlert('Error selecting room. Please try again.', 'error');
        }
    }

    // -------------------------------
    // Booking
    // -------------------------------
    function initializeBookingForm() {
        const bookingForm = document.getElementById('bookingForm');
        if (!bookingForm) return;

        // Pre-fill from selection
        const selectedRoomData = JSON.parse(sessionStorage.getItem('selectedRoomData') || '{}');
        if (selectedRoomData?.roomId) {
            const selectedRoomNumber = document.getElementById('selectedRoomNumber');
            const selectedRoomType = document.getElementById('selectedRoomType');
            const roomPricePerNight = document.getElementById('roomPricePerNight');
            const roomIdInput = document.getElementById('roomId');

            if (selectedRoomNumber) selectedRoomNumber.textContent = selectedRoomData.roomNumber || '';
            if (selectedRoomType) selectedRoomType.textContent = selectedRoomData.roomType || '';
            if (roomPricePerNight) roomPricePerNight.textContent = formatCurrency(selectedRoomData.pricePerNight);
            if (roomIdInput) roomIdInput.value = selectedRoomData.roomId;
        }

        if (bookingForm.dataset.bound !== '1') {
            bookingForm.dataset.bound = '1';
            bookingForm.addEventListener('submit', function (e) {
                e.preventDefault();
                submitBooking();
            });
        }
    }

    function submitBooking() {
        const form = document.getElementById('bookingForm');
        if (!form) return;

        const formData = new FormData(form);
        const bookingData = {};
        for (let [k, v] of formData.entries()) bookingData[k] = v;

        showLoading('Creating your booking...');
        fetch('/api/bookings/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
                    || document.querySelector('input[name="_csrf"]')?.value || ''
            },
            body: JSON.stringify(bookingData)
        })
            .then(r => r.json())
            .then(data => {
                hideLoading();
                if (data?.bookingId) {
                    sessionStorage.setItem('bookingData', JSON.stringify(data));
                    window.location.href = '/payment';
                } else {
                    showAlert(data?.error || 'Error creating booking', 'error');
                }
            })
            .catch(err => {
                hideLoading();
                console.error('Booking error:', err);
                showAlert('Error creating booking. Please try again.', 'error');
            });
    }

    // -------------------------------
    // Payment
    // -------------------------------
    function initializePaymentForm() {
        const paymentForm = document.getElementById('paymentForm');
        if (!paymentForm) return;

        const bookingData = JSON.parse(sessionStorage.getItem('bookingData') || '{}');
        if (bookingData?.bookingId) {
            const bookingRef = document.getElementById('bookingReference');
            const paymentAmount = document.getElementById('paymentAmount');
            if (bookingRef) bookingRef.textContent = bookingData.bookingReference || '';
            if (paymentAmount) paymentAmount.textContent = formatCurrency(bookingData.totalAmount);
        }

        const paymentMethods = document.querySelectorAll('.payment-method');
        paymentMethods.forEach(method => {
            if (method.dataset.bound === '1') return;
            method.dataset.bound = '1';
            method.addEventListener('click', function () {
                paymentMethods.forEach(m => m.classList.remove('selected'));
                this.classList.add('selected');
                const input = document.getElementById('paymentMethod');
                if (input) input.value = this.dataset.method;
            });
        });

        if (paymentForm.dataset.bound !== '1') {
            paymentForm.dataset.bound = '1';
            paymentForm.addEventListener('submit', function (e) {
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
            initializePayHerePayment(bookingData);
        } else {
            showAlert('Payment method not implemented yet', 'info');
        }
    }

    function initializePayHerePayment(bookingData) {
        showLoading('Initializing PayHere payment...');
        fetch('/api/payment/payhere/init', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bookingId: bookingData?.bookingId,
                amount: bookingData?.totalAmount
            })
        })
            .then(r => r.json())
            .then(data => {
                hideLoading();
                if (data?.paymentUrl) {
                    window.location.href = data.paymentUrl;
                } else {
                    showAlert(data?.error || 'Error initializing payment', 'error');
                }
            })
            .catch(err => {
                hideLoading();
                console.error('Payment error:', err);
                showAlert('Error initializing payment. Please try again.', 'error');
            });
    }

    // -------------------------------
    // Dashboard (optional)
    // -------------------------------
    function refreshDashboard() {
        fetch('/api/dashboard/stats')
            .then(r => r.json())
            .then(data => updateDashboardStats(data))
            .catch(err => console.error('Error refreshing dashboard:', err));
    }

    function updateDashboardStats(stats) {
        if (stats?.totalBookings !== undefined) {
            const el = document.getElementById('totalBookings');
            if (el) el.textContent = stats.totalBookings;
        }
        if (stats?.todayCheckIns !== undefined) {
            const el = document.getElementById('todayCheckIns');
            if (el) el.textContent = stats.todayCheckIns;
        }
        if (stats?.availableRooms !== undefined) {
            const el = document.getElementById('availableRooms');
            if (el) el.textContent = stats.availableRooms;
        }
        if (stats?.revenue !== undefined) {
            const el = document.getElementById('revenue');
            if (el) el.textContent = formatCurrency(stats.revenue);
        }
    }

    // -------------------------------
    // Boot
    // -------------------------------
    document.addEventListener('DOMContentLoaded', function () {
        const currentPath = window.location.pathname || '';
        console.log('Current page:', currentPath);

        initializeFormValidation();
        initializeDatePickers();
        initializeAlerts();

        // Pages
        if (['/rooms', '/', '/index', '/dashboard'].includes(currentPath)) {
            initializeRoomSearch();
        }
        if (currentPath === '/booking') {
            initializeBookingForm();
        }
        if (currentPath === '/payment') {
            initializePaymentForm();
        }
    });

    // Expose some helpers if you need them elsewhere
    window.selectRoom = selectRoom;
    window.selectRoomForBooking = selectRoom; // alias
    window.showAlert = showAlert;
    window.formatCurrency = formatCurrency;
    window.formatDate = formatDate;
    window.refreshDashboard = refreshDashboard;
    window.showLoading = showLoading;
    window.hideLoading = hideLoading;

    console.log('Main.js loaded. selectRoom function available:', typeof window.selectRoom);
})();
