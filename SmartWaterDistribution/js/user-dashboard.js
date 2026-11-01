/**
 * Smart Water Distribution Management System
 * File: js/user-dashboard.js
 * Description: Client-side logic for User Dashboard, Demand Requests, and Complaints.
 * Integrated with Phase 2 Java Backend via fetch() and LocalStorage fallback.
 */

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', async function () {
    // Check if user is logged in (session simulation)
    const storedName = sessionStorage.getItem('swd_user_name');
    if (storedName) {
        const userDisplay = document.getElementById('userDisplayName');
        if (userDisplay) {
            userDisplay.textContent = `👤 Logged in as: ${storedName}`;
        }
    }

    // Set default dates to today's date
    const today = new Date().toISOString().split('T')[0];
    const reqDateField = document.getElementById('reqDate');
    const cmpDateField = document.getElementById('cmpDate');
    if (reqDateField) reqDateField.value = today;
    if (cmpDateField) cmpDateField.value = today;

    // Check connectivity to Java Backend and sync latest state
    await checkAndSyncBackend();

    // Populate dropdowns and tables
    populateAreaDropdowns();
    generateNextComplaintId();
    refreshDashboardMetrics();
    renderRequestsTables();
    renderComplaintsTables();
});

/**
 * Check connectivity to Java Backend and show badge
 */
async function checkAndSyncBackend() {
    const isConnected = await BackendApi.checkConnection();
    const headerContainer = document.querySelector('.user-badge-header');
    
    if (headerContainer) {
        // Remove existing backend badge if any
        const existingBadge = document.getElementById('backendStatusBadge');
        if (existingBadge) existingBadge.remove();

        const badge = document.createElement('span');
        badge.id = 'backendStatusBadge';
        badge.style.marginLeft = '10px';
        badge.style.padding = '3px 8px';
        badge.style.borderRadius = '12px';
        badge.style.fontSize = '0.78rem';
        badge.style.fontWeight = 'bold';

        if (isConnected) {
            badge.style.background = '#e8f5e9';
            badge.style.color = '#2e7d32';
            badge.textContent = '🟢 Java Backend Active (Port 8080)';
            // Sync initial state from backend
            await BackendApi.syncFromBackend();
        } else {
            badge.style.background = '#e1f5fe';
            badge.style.color = '#0277bd';
            badge.textContent = '🔵 LocalStorage Cache';
        }
        headerContainer.appendChild(badge);
    }
}

/**
 * Switch visible section on User Dashboard (Single Page Navigation)
 * @param {string} sectionId - 'dashboard', 'demand', or 'complaint'
 */
function showUserSection(sectionId) {
    const sections = ['dashboard', 'demand', 'complaint'];
    
    sections.forEach(sec => {
        const secElement = document.getElementById(`section-${sec}`);
        const navBtn = document.getElementById(`nav-btn-${sec}`);
        if (secElement) {
            secElement.style.display = (sec === sectionId) ? 'block' : 'none';
        }
        if (navBtn) {
            if (sec === sectionId) {
                navBtn.classList.add('active');
            } else {
                navBtn.classList.remove('active');
            }
        }
    });

    // Refresh data whenever a section opens
    if (sectionId === 'dashboard') {
        refreshDashboardMetrics();
        renderRequestsTables();
        renderComplaintsTables();
    } else if (sectionId === 'demand') {
        populateAreaDropdowns();
        renderRequestsTables();
    } else if (sectionId === 'complaint') {
        populateAreaDropdowns();
        generateNextComplaintId();
        renderComplaintsTables();
    }
}

/**
 * Populate Demand Area dropdowns in forms from DataStore
 */
function populateAreaDropdowns() {
    const areas = DataStore.getDemandAreas();
    const reqAreaSelect = document.getElementById('reqDemandArea');
    const cmpAreaSelect = document.getElementById('cmpArea');

    const optionsHtml = areas.map(area => {
        return `<option value="${area.id}">${area.name} (${area.type})</option>`;
    }).join('');

    if (reqAreaSelect) reqAreaSelect.innerHTML = optionsHtml;
    if (cmpAreaSelect) cmpAreaSelect.innerHTML = optionsHtml;
}

/**
 * Generate Next Unique Complaint ID
 */
function generateNextComplaintId() {
    const complaints = DataStore.getComplaints();
    const nextNum = 201 + complaints.length;
    const cmpIdField = document.getElementById('cmpId');
    if (cmpIdField) {
        cmpIdField.value = `CMP-${nextNum}`;
    }
}

/**
 * Refresh top metric cards on the User Dashboard
 */
function refreshDashboardMetrics() {
    const requests = DataStore.getWaterRequests();
    const complaints = DataStore.getComplaints();

    // 1. Current Water Demand: Sum of active requests
    const totalDemand = requests.reduce((sum, req) => sum + Number(req.quantity), 0);
    const demandElem = document.getElementById('userTotalDemand');
    if (demandElem) {
        demandElem.textContent = `${totalDemand.toLocaleString()} L`;
    }

    // 2. Latest Request Status
    const statusElem = document.getElementById('userLatestStatus');
    const reqIdElem = document.getElementById('userLatestReqId');
    const statusCard = document.getElementById('metricCardStatus');

    if (requests.length > 0) {
        const latestReq = requests[requests.length - 1];
        if (statusElem) statusElem.textContent = latestReq.status || 'Pending';
        if (reqIdElem) reqIdElem.textContent = `Request ${latestReq.id} (${latestReq.areaName})`;
        
        if (statusCard) {
            statusCard.className = 'metric-card ' + 
                (latestReq.status === 'Allocated' ? 'success' : 
                 latestReq.status === 'Partially Allocated' ? 'warning' : 'warning');
        }
    } else {
        if (statusElem) statusElem.textContent = 'None';
        if (reqIdElem) reqIdElem.textContent = 'No requests submitted yet';
    }

    // 3. Open Complaints
    const openComplaintsCount = complaints.filter(c => c.status === 'Open').length;
    const compElem = document.getElementById('userOpenComplaints');
    if (compElem) {
        compElem.textContent = openComplaintsCount;
    }
}

/**
 * Render Requests Tables (Both on Home Dashboard and Add Demand section)
 */
function renderRequestsTables() {
    const requests = DataStore.getWaterRequests();
    const dashboardTbody = document.getElementById('dashboardRequestsTbody');
    const allTbody = document.getElementById('allRequestsTbody');

    // Helper for table row HTML
    const generateRow = (req) => {
        const badgeClass = getStatusBadgeClass(req.status);
        const priorityBadge = getPriorityBadgeClass(req.priority);

        return `
            <tr>
                <td><strong>${req.id}</strong></td>
                <td>${req.date}</td>
                <td>${req.areaName || req.areaId}</td>
                <td><strong>${Number(req.quantity).toLocaleString()} L</strong></td>
                <td><span class="badge ${priorityBadge}">${req.priority}</span></td>
                ${allTbody ? `<td><small>${escapeHtml(req.notes || '-')}</small></td>` : ''}
                <td><span class="badge ${badgeClass}">${req.status}</span></td>
            </tr>
        `;
    };

    // Dashboard Home table (Latest 4)
    if (dashboardTbody) {
        if (requests.length === 0) {
            dashboardTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No water requests found.</td></tr>';
        } else {
            const recent = requests.slice().reverse().slice(0, 4);
            dashboardTbody.innerHTML = recent.map(req => `
                <tr>
                    <td><strong>${req.id}</strong></td>
                    <td>${req.date}</td>
                    <td>${req.areaName || req.areaId}</td>
                    <td><strong>${Number(req.quantity).toLocaleString()} L</strong></td>
                    <td><span class="badge ${getPriorityBadgeClass(req.priority)}">${req.priority}</span></td>
                    <td><span class="badge ${getStatusBadgeClass(req.status)}">${req.status}</span></td>
                </tr>
            `).join('');
        }
    }

    // All Requests table (on Add Demand section)
    if (allTbody) {
        if (requests.length === 0) {
            allTbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No water demand requests submitted yet.</td></tr>';
        } else {
            allTbody.innerHTML = requests.slice().reverse().map(generateRow).join('');
        }
    }
}

/**
 * Render Complaints Tables (Both on Home Dashboard and Complaints section)
 */
function renderComplaintsTables() {
    const complaints = DataStore.getComplaints();
    const dashboardTbody = document.getElementById('dashboardComplaintsTbody');
    const allTbody = document.getElementById('allComplaintsTbody');

    if (dashboardTbody) {
        if (complaints.length === 0) {
            dashboardTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No complaints logged.</td></tr>';
        } else {
            const recent = complaints.slice().reverse().slice(0, 4);
            dashboardTbody.innerHTML = recent.map(c => `
                <tr>
                    <td><strong>${c.id}</strong></td>
                    <td>${c.date}</td>
                    <td>${c.areaName || c.areaId}</td>
                    <td>${c.type}</td>
                    <td><small>${escapeHtml(c.description.substring(0, 50))}${c.description.length > 50 ? '...' : ''}</small></td>
                    <td><span class="badge ${getStatusBadgeClass(c.status)}">${c.status}</span></td>
                </tr>
            `).join('');
        }
    }

    if (allTbody) {
        if (complaints.length === 0) {
            allTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No complaints recorded yet.</td></tr>';
        } else {
            allTbody.innerHTML = complaints.slice().reverse().map(c => `
                <tr>
                    <td><strong>${c.id}</strong></td>
                    <td>${c.date}</td>
                    <td>${c.areaName || c.areaId}</td>
                    <td><strong>${c.type}</strong></td>
                    <td>${escapeHtml(c.description)}</td>
                    <td><span class="badge ${getStatusBadgeClass(c.status)}">${c.status}</span></td>
                </tr>
            `).join('');
        }
    }
}

/**
 * Handle Add Demand Water Form Submission
 * Dispatches via fetch() to Java Backend POST /api/requests
 */
async function handleDemandSubmit(event) {
    event.preventDefault();

    const userName = document.getElementById('reqUserName').value.trim();
    const areaId = document.getElementById('reqDemandArea').value;
    const reqDate = document.getElementById('reqDate').value;
    const quantity = parseInt(document.getElementById('reqQuantity').value, 10);
    const priority = document.getElementById('reqPriority').value;
    const notes = document.getElementById('reqNotes').value.trim();
    const alertBox = document.getElementById('demandAlert');

    // Validation
    if (!userName || !areaId || !reqDate || isNaN(quantity) || quantity <= 0) {
        showAlert(alertBox, 'alert-danger', 'Please enter all mandatory fields with a valid water quantity.');
        return;
    }

    const areas = DataStore.getDemandAreas();
    const selectedArea = areas.find(a => a.id === areaId);
    const areaName = selectedArea ? selectedArea.name : areaId;

    // Generate Request ID
    const requests = DataStore.getWaterRequests();
    const newId = `REQ-${101 + requests.length}`;

    const newRequest = {
        id: newId,
        userName: userName,
        areaId: areaId,
        areaName: areaName,
        date: reqDate,
        quantity: quantity,
        priority: priority,
        notes: notes,
        status: 'Pending'
    };

    // 1. Send to Java Backend via fetch()
    const apiResult = await BackendApi.addWaterRequest(newRequest);

    // 2. Synchronize state with Java backend
    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        const assignedId = (apiResult && apiResult.id) ? apiResult.id : newId;

        // Show success message
        showAlert(alertBox, 'alert-success', `Water Demand Request ${assignedId} for ${areaName} (${quantity.toLocaleString()} L) submitted successfully! Status: Pending. [Saved to MySQL Database]`);

        // Reset Form
        document.getElementById('reqQuantity').value = '';
        document.getElementById('reqNotes').value = '';

        // Refresh displays
        renderRequestsTables();
        refreshDashboardMetrics();
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : 'Database connection or server error.';
        showAlert(alertBox, 'alert-danger', `Failed to submit Water Demand Request: ${errorMsg}`);
    }
}

/**
 * Handle Complaint Form Submission
 * Dispatches via fetch() to Java Backend POST /api/complaints
 */
async function handleComplaintSubmit(event) {
    event.preventDefault();

    const cmpId = document.getElementById('cmpId').value;
    const userName = document.getElementById('cmpUserName').value.trim();
    const areaId = document.getElementById('cmpArea').value;
    const cmpType = document.getElementById('cmpType').value;
    const cmpDate = document.getElementById('cmpDate').value;
    const description = document.getElementById('cmpDesc').value.trim();
    const alertBox = document.getElementById('complaintAlert');

    // Validation
    if (!userName || !areaId || !cmpType || !cmpDate || !description) {
        showAlert(alertBox, 'alert-danger', 'Please fill in all complaint details.');
        return;
    }

    const areas = DataStore.getDemandAreas();
    const selectedArea = areas.find(a => a.id === areaId);
    const areaName = selectedArea ? selectedArea.name : areaId;

    const complaints = DataStore.getComplaints();
    const newComplaint = {
        id: cmpId,
        userName: userName,
        areaId: areaId,
        areaName: areaName,
        type: cmpType,
        description: description,
        date: cmpDate,
        status: 'Open'
    };

    // 1. Send to Java Backend via fetch()
    const apiResult = await BackendApi.addComplaint(newComplaint);

    // 2. Synchronize state with Java backend
    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showAlert(alertBox, 'alert-success', `Complaint ${cmpId} registered successfully! Saved to MySQL Database.`);

        // Reset form and generate next complaint ID
        document.getElementById('cmpDesc').value = '';
        generateNextComplaintId();

        // Refresh displays
        renderComplaintsTables();
        refreshDashboardMetrics();
    } else {
        const errMsg = (apiResult && apiResult.error) ? apiResult.error : 'Database connection or server error.';
        showAlert(alertBox, 'alert-danger', `Failed to submit complaint: ${errMsg}`);
    }
    refreshDashboardMetrics();
}

/**
 * Logout Handler
 */
function handleLogout() {
    sessionStorage.removeItem('swd_user_logged_in');
    sessionStorage.removeItem('swd_user_name');
    window.location.href = 'index.html';
}

/**
 * Helper to display temporary alert messages
 */
function showAlert(element, alertClass, message) {
    if (!element) return;
    element.className = `alert ${alertClass}`;
    element.textContent = message;
    element.style.display = 'block';

    setTimeout(() => {
        element.style.display = 'none';
    }, 6000);
}
