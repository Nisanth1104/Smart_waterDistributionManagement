/**
 * Smart Water Distribution Management System
 * File: js/admin-dashboard.js
 * Description: Municipal admin control center logic integrated with Phase 2 Java Backend via fetch().
 * Implements:
 * - Real-time synchronization with Java HttpServer REST API
 * - Adjacency List Graph & BFS Reachability checks
 * - FIFO Maintenance Queue (ArrayDeque in Java)
 * - 5-Tier Priority-Based Water Allocation (PriorityQueue in Java)
 * - Reservoir management & multi-reservoir failover
 */

// Global state for search and filter caches
let areaSearchQuery = '';
let complaintSearchQuery = '';
let complaintStatusFilterValue = 'ALL';

// Initialize on document ready
document.addEventListener('DOMContentLoaded', async function () {
    // Check if admin is logged in
    const storedAdminName = sessionStorage.getItem('swd_admin_name');
    if (storedAdminName) {
        const adminDisplay = document.getElementById('adminDisplayName');
        if (adminDisplay) {
            adminDisplay.textContent = `👤 Logged in as: ${storedAdminName}`;
        }
    }

    // Check connectivity to Java Backend and sync latest state
    await checkAndSyncBackendAdmin();

    // Initialize all components
    initAdminComponents();
});

/**
 * Check connectivity to Java Backend and display status badge in header
 */
async function checkAndSyncBackendAdmin() {
    const isConnected = await BackendApi.checkConnection();
    const headerContainer = document.querySelector('.user-badge-header');

    if (headerContainer) {
        const existingBadge = document.getElementById('adminBackendBadge');
        if (existingBadge) existingBadge.remove();

        const badge = document.createElement('span');
        badge.id = 'adminBackendBadge';
        badge.style.marginLeft = '10px';
        badge.style.padding = '3px 8px';
        badge.style.borderRadius = '12px';
        badge.style.fontSize = '0.78rem';
        badge.style.fontWeight = 'bold';

        if (isConnected) {
            badge.style.background = '#e8f5e9';
            badge.style.color = '#2e7d32';
            badge.textContent = '🟢 Java Backend Active (Port 8080)';
            // Sync all latest data from Java in-memory model
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
 * Master initializer for all Admin sections
 */
function initAdminComponents() {
    refreshAdminDashboardStats();
    renderRecentRequestsAdmin();
    generateNextAreaId();
    renderDemandAreasTable();
    renderAdminComplaintsTable();
    renderNetworkFlowGraph();
    renderPipelinesTable();
    generateNextMaintenanceId();
    populateMntPipelineDropdown();
    renderMaintenanceQueueTable();
    renderReservoirCards();
    loadSelectedReservoirData();
    calculateAndRenderAllocations(false); // Initial preview
}

/**
 * Switch visible section on Admin Dashboard
 */
function showAdminSection(sectionId) {
    const sections = ['dashboard', 'areas', 'complaints', 'network', 'maintenance', 'allocation', 'reservoirs'];

    sections.forEach(sec => {
        const secElement = document.getElementById(`section-admin-${sec}`);
        const navBtn = document.getElementById(`admin-nav-${sec}`);

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

    // Refresh specific section data when visited
    switch (sectionId) {
        case 'dashboard':
            refreshAdminDashboardStats();
            renderRecentRequestsAdmin();
            break;
        case 'areas':
            generateNextAreaId();
            renderDemandAreasTable();
            break;
        case 'complaints':
            renderAdminComplaintsTable();
            break;
        case 'network':
            renderNetworkFlowGraph();
            renderPipelinesTable();
            break;
        case 'maintenance':
            generateNextMaintenanceId();
            populateMntPipelineDropdown();
            renderMaintenanceQueueTable();
            break;
        case 'allocation':
            calculateAndRenderAllocations(false);
            break;
        case 'reservoirs':
            renderReservoirCards();
            loadSelectedReservoirData();
            break;
    }
}

// =============================================================================
// SECTION 1: ADMIN DASHBOARD HOME
// =============================================================================

function refreshAdminDashboardStats() {
    const areas = DataStore.getDemandAreas();
    const reservoirs = DataStore.getReservoirs();
    const pipelines = DataStore.getPipelines();
    const requests = DataStore.getWaterRequests();
    const complaints = DataStore.getComplaints();

    // 1. Total Demand Areas
    const totalAreasElem = document.getElementById('statTotalAreas');
    if (totalAreasElem) totalAreasElem.textContent = areas.length;

    // 2. Total Water Demand in Litres
    const totalDemand = requests.reduce((sum, r) => sum + Number(r.quantity), 0);
    const totalDemandElem = document.getElementById('statTotalDemand');
    if (totalDemandElem) totalDemandElem.textContent = `${totalDemand.toLocaleString()} L`;

    // 3. Available Reservoir Water (from Active reservoirs only)
    const activeWater = reservoirs
        .filter(r => r.status === 'Active')
        .reduce((sum, r) => sum + Number(r.available), 0);
    const availWaterElem = document.getElementById('statAvailableWater');
    if (availWaterElem) availWaterElem.textContent = `${activeWater.toLocaleString()} L`;

    // 4. Pending Complaints (Open or In Progress)
    const pendingComplaints = complaints.filter(c => c.status === 'Open' || c.status === 'In Progress').length;
    const pendingCompElem = document.getElementById('statPendingComplaints');
    if (pendingCompElem) pendingCompElem.textContent = pendingComplaints;

    // 5. Failed Pipelines
    const failedPipelines = pipelines.filter(p => p.status === 'Failed').length;
    const failedPipeElem = document.getElementById('statFailedPipelines');
    if (failedPipeElem) failedPipeElem.textContent = failedPipelines;

    // Dynamic warning alert for failed pipeline
    const gridAlert = document.getElementById('gridWarningAlert');
    if (gridAlert) {
        if (failedPipelines > 0) {
            gridAlert.style.display = 'flex';
            const failedNames = pipelines.filter(p => p.status === 'Failed').map(p => p.id).join(', ');
            gridAlert.innerHTML = `<span>⚠️ <strong>Network Alert:</strong> Pipeline(s) <code>${failedNames}</code> failed! Areas connected solely to these pipelines are disconnected.</span>`;
        } else {
            gridAlert.style.display = 'none';
        }
    }

    // 6. Areas Receiving Water (Graph reachability)
    const reachableCount = countReachableAreas(areas, pipelines, reservoirs);
    const receivingElem = document.getElementById('statReceivingWater');
    if (receivingElem) receivingElem.textContent = `${reachableCount} / ${areas.length}`;
}

function countReachableAreas(areas, pipelines, reservoirs) {
    const activeReservoirIds = new Set(reservoirs.filter(r => r.status === 'Active').map(r => r.id));
    let count = 0;

    areas.forEach(area => {
        const hasWorkingFeed = pipelines.some(p => 
            p.to === area.id && 
            p.status === 'Working' && 
            activeReservoirIds.has(p.from)
        );
        if (hasWorkingFeed) count++;
    });

    return count;
}

function isAreaReachable(areaId, pipelines, reservoirs) {
    const activeReservoirIds = new Set(reservoirs.filter(r => r.status === 'Active').map(r => r.id));
    return pipelines.some(p => 
        p.to === areaId && 
        p.status === 'Working' && 
        activeReservoirIds.has(p.from)
    );
}

function renderRecentRequestsAdmin() {
    const requests = DataStore.getWaterRequests();
    const tbody = document.getElementById('adminRecentRequestsTbody');
    if (!tbody) return;

    if (requests.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No water requests logged.</td></tr>';
        return;
    }

    tbody.innerHTML = requests.slice().reverse().slice(0, 5).map(req => {
        return `
            <tr>
                <td><strong>${req.id}</strong></td>
                <td>${req.date}</td>
                <td>${req.areaName || req.areaId}</td>
                <td><span class="badge ${getPriorityBadgeClass(req.priority)}">${req.priority}</span></td>
                <td><strong>${Number(req.quantity).toLocaleString()} L</strong></td>
                <td><span class="badge ${getStatusBadgeClass(req.status)}">${req.status}</span></td>
            </tr>
        `;
    }).join('');
}

// =============================================================================
// SECTION 2: ADD DEMAND AREA (CRUD & Validation)
// =============================================================================

function generateNextAreaId() {
    const areas = DataStore.getDemandAreas();
    const nextNum = areas.length + 1;
    const formatted = nextNum < 10 ? `AREA-0${nextNum}` : `AREA-${nextNum}`;
    const idField = document.getElementById('areaId');
    if (idField) idField.value = formatted;
}

function handleAreaTypeChange() {
    const type = document.getElementById('areaType').value;
    const prioritySelect = document.getElementById('areaPriority');
    if (type === 'Critical Institution') {
        prioritySelect.value = 'Critical';
    } else if (type === 'Residential') {
        prioritySelect.value = 'High';
    } else {
        prioritySelect.value = 'Medium';
    }
}

async function handleAddArea(event) {
    event.preventDefault();

    const areaId = document.getElementById('areaId').value;
    const name = document.getElementById('areaName').value.trim();
    const type = document.getElementById('areaType').value;
    const priority = document.getElementById('areaPriority').value;
    const minDemand = parseInt(document.getElementById('areaMinDemand').value, 10);
    const maxDemand = parseInt(document.getElementById('areaMaxDemand').value, 10);
    const reservoir = document.getElementById('areaReservoir').value;
    const description = document.getElementById('areaDesc').value.trim();
    const alertBox = document.getElementById('areaAlert');

    // Validation
    if (minDemand > maxDemand) {
        showInlineAlert(alertBox, 'alert-danger', 'Validation Error: Minimum water requirement cannot be greater than Maximum water requirement.');
        return;
    }

    const newArea = {
        id: areaId,
        name: name,
        type: type,
        minRequirement: minDemand,
        maxRequirement: maxDemand,
        priority: priority,
        connectedReservoir: reservoir,
        description: description
    };

    // 1. Dispatch via fetch() to Java Backend POST /api/areas
    const apiResult = await BackendApi.addDemandArea(newArea);

    // 2. Synchronize from backend
    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-success', `Demand Area "${name}" (${areaId}) created and saved to MySQL!`);

        // Reset inputs
        document.getElementById('areaName').value = '';
        document.getElementById('areaMinDemand').value = '';
        document.getElementById('areaMaxDemand').value = '';
        document.getElementById('areaDesc').value = '';
        generateNextAreaId();

        // Refresh views
        renderDemandAreasTable();
        refreshAdminDashboardStats();
        renderNetworkFlowGraph();
        renderPipelinesTable();
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : 'Database insert failed.';
        showInlineAlert(alertBox, 'alert-danger', `Failed to create Demand Area: ${errorMsg}`);
    }
}

function renderDemandAreasTable() {
    const areas = DataStore.getDemandAreas();
    const tbody = document.getElementById('demandAreasTbody');
    const badge = document.getElementById('areaCountBadge');
    if (!tbody) return;

    if (badge) badge.textContent = `${areas.length} Total Areas`;

    const filtered = areas.filter(a => {
        if (!areaSearchQuery) return true;
        const q = areaSearchQuery.toLowerCase();
        return a.name.toLowerCase().includes(q) || a.id.toLowerCase().includes(q) || a.type.toLowerCase().includes(q);
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">No matching demand areas found.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(area => {
        return `
            <tr>
                <td><strong>${area.id}</strong></td>
                <td><strong>${escapeHtml(area.name)}</strong></td>
                <td>${area.type}</td>
                <td><span class="badge ${getPriorityBadgeClass(area.priority)}">${area.priority}</span></td>
                <td>${Number(area.minRequirement).toLocaleString()} L</td>
                <td>${Number(area.maxRequirement).toLocaleString()} L</td>
                <td><code>${area.connectedReservoir}</code></td>
                <td>
                    <div style="display: flex; gap: 6px;">
                        <button class="btn btn-sm btn-secondary" onclick="openEditAreaModal('${area.id}')">✏️ Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteDemandArea('${area.id}')">🗑️ Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterDemandAreas() {
    areaSearchQuery = document.getElementById('areaSearchInput').value.trim();
    renderDemandAreasTable();
}

function clearAreaSearch() {
    document.getElementById('areaSearchInput').value = '';
    areaSearchQuery = '';
    renderDemandAreasTable();
}

function openEditAreaModal(areaId) {
    const areas = DataStore.getDemandAreas();
    const area = areas.find(a => a.id === areaId);
    if (!area) return;

    document.getElementById('editAreaId').value = area.id;
    document.getElementById('editAreaName').value = area.name;
    document.getElementById('editAreaType').value = area.type;
    document.getElementById('editAreaPriority').value = area.priority;
    document.getElementById('editMinReq').value = area.minRequirement;
    document.getElementById('editMaxReq').value = area.maxRequirement;
    document.getElementById('editAreaReservoir').value = area.connectedReservoir || 'RES-A';
    document.getElementById('editAreaDesc').value = area.description || '';

    document.getElementById('editAreaModal').style.display = 'flex';
}

function closeEditAreaModal() {
    document.getElementById('editAreaModal').style.display = 'none';
}

async function handleSaveEditedArea(event) {
    event.preventDefault();
    const areaId = document.getElementById('editAreaId').value;
    const name = document.getElementById('editAreaName').value.trim();
    const type = document.getElementById('editAreaType').value;
    const priority = document.getElementById('editAreaPriority').value;
    const minReq = parseInt(document.getElementById('editMinReq').value, 10);
    const maxReq = parseInt(document.getElementById('editMaxReq').value, 10);
    const reservoir = document.getElementById('editAreaReservoir').value;
    const desc = document.getElementById('editAreaDesc').value.trim();

    if (minReq > maxReq) {
        alert("Minimum demand requirement cannot exceed Maximum requirement!");
        return;
    }

    const updatedObj = {
        id: areaId,
        name: name,
        type: type,
        priority: priority,
        minRequirement: minReq,
        maxRequirement: maxReq,
        connectedReservoir: reservoir,
        description: desc
    };

    // Dispatch via fetch() to Java Backend PUT /api/areas
    const apiResult = await BackendApi.updateDemandArea(updatedObj);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        closeEditAreaModal();
        renderDemandAreasTable();
        refreshAdminDashboardStats();
        renderNetworkFlowGraph();
    } else {
        alert("Failed to update demand area in database: " + (apiResult && apiResult.error ? apiResult.error : "Database error"));
    }
}

async function deleteDemandArea(areaId) {
    if (!confirm(`Are you sure you want to delete Demand Area ${areaId}? This will remove associated pipelines and requests.`)) {
        return;
    }

    // Call Java Backend DELETE /api/areas
    const apiResult = await BackendApi.deleteDemandArea(areaId);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        renderDemandAreasTable();
        refreshAdminDashboardStats();
        renderNetworkFlowGraph();
        renderPipelinesTable();
    } else {
        alert("Failed to delete demand area from database: " + (apiResult && apiResult.error ? apiResult.error : "Database error"));
    }
}

// =============================================================================
// SECTION 3: COMPLAINT MANAGEMENT
// =============================================================================

function renderAdminComplaintsTable() {
    const complaints = DataStore.getComplaints();
    const tbody = document.getElementById('adminComplaintsTbody');
    if (!tbody) return;

    let filtered = complaints.filter(c => {
        if (complaintStatusFilterValue !== 'ALL' && c.status !== complaintStatusFilterValue) {
            return false;
        }
        if (complaintSearchQuery) {
            const q = complaintSearchQuery.toLowerCase();
            return c.id.toLowerCase().includes(q) || c.userName.toLowerCase().includes(q) ||
                   (c.areaName || c.areaId).toLowerCase().includes(q) || c.type.toLowerCase().includes(q) ||
                   c.description.toLowerCase().includes(q);
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">No complaints matching current filter.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.slice().reverse().map(c => {
        return `
            <tr>
                <td><strong>${c.id}</strong></td>
                <td>${escapeHtml(c.userName)}</td>
                <td>${escapeHtml(c.areaName || c.areaId)}</td>
                <td><strong>${c.type}</strong></td>
                <td>${c.date}</td>
                <td><span class="badge ${getStatusBadgeClass(c.status)}">${c.status}</span></td>
                <td>
                    <select class="form-control" style="padding: 4px 8px; font-size: 0.82rem;" onchange="updateComplaintStatus('${c.id}', this.value)">
                        <option value="Open" ${c.status === 'Open' ? 'selected' : ''}>Open</option>
                        <option value="In Progress" ${c.status === 'In Progress' ? 'selected' : ''}>In Progress</option>
                        <option value="Resolved" ${c.status === 'Resolved' ? 'selected' : ''}>Resolved</option>
                    </select>
                </td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="viewComplaintDetails('${c.id}')">
                        👁️ View Details
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterComplaints() {
    complaintSearchQuery = document.getElementById('complaintSearchInput').value.trim();
    complaintStatusFilterValue = document.getElementById('complaintStatusFilter').value;
    renderAdminComplaintsTable();
}

function resetComplaintFilters() {
    document.getElementById('complaintSearchInput').value = '';
    document.getElementById('complaintStatusFilter').value = 'ALL';
    complaintSearchQuery = '';
    complaintStatusFilterValue = 'ALL';
    renderAdminComplaintsTable();
}

async function updateComplaintStatus(complaintId, newStatus) {
    const alertBox = document.getElementById('adminComplaintAlert');
    // 1. Call Java Backend PUT /api/complaints/status
    const apiResult = await BackendApi.updateComplaintStatus(complaintId, newStatus);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-success', `Complaint ${complaintId} status updated to "${newStatus}" in MySQL.`);
        refreshAdminDashboardStats();
        renderAdminComplaintsTable();
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : 'Database update failed.';
        showInlineAlert(alertBox, 'alert-danger', `Failed to update complaint ${complaintId}: ${errorMsg}`);
    }
}

function viewComplaintDetails(complaintId) {
    const complaints = DataStore.getComplaints();
    const c = complaints.find(item => item.id === complaintId);
    if (!c) return;

    const modalContent = document.getElementById('complaintDetailContent');
    modalContent.innerHTML = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
            <div><strong>Complaint ID:</strong> ${c.id}</div>
            <div><strong>Filed Date:</strong> ${c.date}</div>
            <div><strong>Complainant:</strong> ${escapeHtml(c.userName)}</div>
            <div><strong>Demand Area:</strong> ${escapeHtml(c.areaName || c.areaId)}</div>
            <div><strong>Category:</strong> ${c.type}</div>
            <div><strong>Current Status:</strong> <span class="badge ${getStatusBadgeClass(c.status)}">${c.status}</span></div>
        </div>
        <div style="background: #f8fbfe; border: 1px solid var(--card-border); padding: 14px; border-radius: 6px;">
            <strong style="color: var(--primary-dark);">Full Problem Description:</strong>
            <p style="margin-top: 8px; line-height: 1.5; color: var(--text-body);">${escapeHtml(c.description)}</p>
        </div>
    `;

    document.getElementById('complaintDetailModal').style.display = 'flex';
}

function closeComplaintModal() {
    document.getElementById('complaintDetailModal').style.display = 'none';
}

// =============================================================================
// SECTION 4: WATER NETWORK FLOW GRAPH (SVG + Graph Reachability)
// =============================================================================

function renderNetworkFlowGraph() {
    const svg = document.getElementById('networkSvg');
    if (!svg) return;

    const reservoirs = DataStore.getReservoirs();
    const areas = DataStore.getDemandAreas();
    const pipelines = DataStore.getPipelines();

    const reservoirPositions = {
        'RES-A': { x: 130, y: 130, name: 'Reservoir A' },
        'RES-B': { x: 130, y: 350, name: 'Reservoir B' }
    };

    const areaPositions = {};
    const totalAreas = areas.length;
    const startY = 70;
    const endY = 410;
    const stepY = totalAreas > 1 ? (endY - startY) / (totalAreas - 1) : 0;

    areas.forEach((area, index) => {
        areaPositions[area.id] = {
            x: 730,
            y: Math.round(startY + index * stepY),
            name: area.name,
            priority: area.priority,
            type: area.type
        };
    });

    let svgHtml = '';

    // Draw Pipelines
    pipelines.forEach(pipeline => {
        const fromPos = reservoirPositions[pipeline.from];
        const toPos = areaPositions[pipeline.to];

        if (!fromPos || !toPos) return;

        let strokeColor = '#2e7d32'; // Green
        let strokeDash = 'none';
        let strokeWidth = '4';

        if (pipeline.status === 'Failed') {
            strokeColor = '#c62828'; // Red
            strokeDash = '6,4';
            strokeWidth = '5';
        } else if (pipeline.status === 'Under Maintenance') {
            strokeColor = '#ef6c00'; // Orange
            strokeDash = '8,4';
            strokeWidth = '4';
        }

        const midX = (fromPos.x + toPos.x) / 2;
        const midY = (fromPos.y + toPos.y) / 2;

        svgHtml += `
            <g class="pipeline-group">
                <line x1="${fromPos.x + 30}" y1="${fromPos.y}" x2="${toPos.x - 30}" y2="${toPos.y}" 
                      stroke="${strokeColor}" stroke-width="${strokeWidth}" stroke-dasharray="${strokeDash}" />
                
                <rect x="${midX - 32}" y="${midY - 12}" width="64" height="22" rx="4" fill="#ffffff" stroke="${strokeColor}" stroke-width="1.5" />
                <text x="${midX}" y="${midY + 3}" font-size="11" font-weight="bold" fill="${strokeColor}" text-anchor="middle">
                    ${pipeline.id}
                </text>
            </g>
        `;
    });

    // Draw Reservoirs
    reservoirs.forEach(res => {
        const pos = reservoirPositions[res.id];
        if (!pos) return;

        const isActive = res.status === 'Active';
        const nodeColor = isActive ? '#0d47a1' : '#78909c';
        const fillColor = isActive ? '#e3f2fd' : '#eceff1';

        svgHtml += `
            <g class="reservoir-node" transform="translate(${pos.x}, ${pos.y})">
                <circle r="44" fill="${fillColor}" stroke="${nodeColor}" stroke-width="3" />
                <text y="-14" font-size="18" text-anchor="middle">🌊</text>
                <text y="4" font-size="12" font-weight="bold" fill="${nodeColor}" text-anchor="middle">${res.name.split(' (')[0]}</text>
                <text y="18" font-size="10" fill="#37474f" text-anchor="middle">${Number(res.available).toLocaleString()} L</text>
                <text y="30" font-size="9" font-weight="bold" fill="${isActive ? '#2e7d32' : '#c62828'}" text-anchor="middle">[${res.status}]</text>
            </g>
        `;
    });

    // Draw Demand Areas
    const disconnectedAreas = [];

    areas.forEach(area => {
        const pos = areaPositions[area.id];
        if (!pos) return;

        const isConnected = isAreaReachable(area.id, pipelines, reservoirs);
        if (!isConnected) {
            disconnectedAreas.push(area.name);
        }

        const borderColor = isConnected ? '#1976d2' : '#c62828';
        const nodeBg = isConnected ? '#ffffff' : '#ffebee';
        const strokeDash = isConnected ? 'none' : '5,3';
        const icon = area.type === 'Critical Institution' ? '🏥' : (area.name.includes('School') ? '🏫' : '🏘️');

        svgHtml += `
            <g class="area-node" transform="translate(${pos.x}, ${pos.y})">
                <rect x="-95" y="-26" width="190" height="52" rx="8" fill="${nodeBg}" stroke="${borderColor}" stroke-width="2.5" stroke-dasharray="${strokeDash}" />
                <text x="-80" y="5" font-size="18">${icon}</text>
                <text x="-56" y="-6" font-size="11.5" font-weight="bold" fill="#0d47a1">${escapeHtml(area.name.length > 20 ? area.name.substring(0, 18) + '...' : area.name)}</text>
                <text x="-56" y="9" font-size="10" fill="#546e7a">${area.priority} Priority</text>
                <text x="-56" y="21" font-size="9.5" font-weight="bold" fill="${isConnected ? '#2e7d32' : '#c62828'}">
                    ${isConnected ? '● Connected' : '⚠️ DISCONNECTED'}
                </text>
            </g>
        `;
    });

    svg.innerHTML = svgHtml;

    // Disconnected Notice Alert Box
    const noticeBox = document.getElementById('disconnectedNoticeBox');
    if (noticeBox) {
        if (disconnectedAreas.length > 0) {
            noticeBox.style.display = 'block';
            noticeBox.innerHTML = `
                <strong>⚠️ Network Disconnection Warning:</strong><br>
                The following demand area(s) have <strong>zero working pipelines</strong> connecting them to active reservoirs:
                <ul style="margin-left: 20px; margin-top: 4px;">
                    ${disconnectedAreas.map(a => `<li><strong>${a}</strong> &mdash; Pipeline failure active</li>`).join('')}
                </ul>
                <em>Tip: Mark the failed pipeline as Working or process it in the Maintenance Queue to restore water flow!</em>
            `;
        } else {
            noticeBox.style.display = 'none';
        }
    }
}

function renderPipelinesTable() {
    const pipelines = DataStore.getPipelines();
    const reservoirs = DataStore.getReservoirs();
    const areas = DataStore.getDemandAreas();
    const tbody = document.getElementById('pipelinesControlTbody');
    if (!tbody) return;

    tbody.innerHTML = pipelines.map(pipe => {
        const fromRes = reservoirs.find(r => r.id === pipe.from);
        const toArea = areas.find(a => a.id === pipe.to);
        const fromText = fromRes ? fromRes.name : pipe.from;
        const toText = toArea ? toArea.name : pipe.to;

        return `
            <tr>
                <td><strong>${pipe.id}</strong></td>
                <td>${pipe.name}</td>
                <td><code>${fromText}</code></td>
                <td><strong>${toText}</strong></td>
                <td>${Number(pipe.capacity).toLocaleString()} L</td>
                <td><span class="badge ${getStatusBadgeClass(pipe.status)}">${pipe.status}</span></td>
                <td>
                    <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                        <button class="btn btn-sm btn-success" onclick="setPipelineStatus('${pipe.id}', 'Working')">
                            🟢 Working
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="setPipelineStatus('${pipe.id}', 'Failed')">
                            🔴 Failed
                        </button>
                        <button class="btn btn-sm btn-warning" onclick="setPipelineStatus('${pipe.id}', 'Under Maintenance')">
                            🟠 Maintenance
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function setPipelineStatus(pipelineId, newStatus) {
    const alertBox = document.getElementById('networkAlert');
    // 1. Call Java Backend PUT /api/pipelines/status
    const apiResult = await BackendApi.setPipelineStatus(pipelineId, newStatus);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-info', `Pipeline ${pipelineId} status updated to "${newStatus}" in MySQL. Water flow graph updated!`);
        renderNetworkFlowGraph();
        renderPipelinesTable();
        refreshAdminDashboardStats();
        renderMaintenanceQueueTable();
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : 'Database update failed.';
        showInlineAlert(alertBox, 'alert-danger', `Failed to update pipeline ${pipelineId}: ${errorMsg}`);
    }
}

// =============================================================================
// SECTION 5: MAINTENANCE QUEUE (FIFO DATA STRUCTURE)
// =============================================================================

function generateNextMaintenanceId() {
    const queue = DataStore.getMaintenanceQueue();
    const mntIdField = document.getElementById('mntId');
    if (mntIdField) {
        mntIdField.value = `MNT-${301 + queue.length}`;
    }
}

function populateMntPipelineDropdown() {
    const pipelines = DataStore.getPipelines();
    const dropdown = document.getElementById('mntPipeline');
    if (!dropdown) return;

    dropdown.innerHTML = pipelines.map(p => {
        return `<option value="${p.id}">${p.id} &mdash; ${p.name} [${p.status}]</option>`;
    }).join('');

    autoFillMntAffectedArea();
}

function autoFillMntAffectedArea() {
    const pipelineId = document.getElementById('mntPipeline').value;
    const pipelines = DataStore.getPipelines();
    const areas = DataStore.getDemandAreas();
    const pipe = pipelines.find(p => p.id === pipelineId);
    const areaField = document.getElementById('mntArea');

    if (pipe && areaField) {
        const area = areas.find(a => a.id === pipe.to);
        areaField.value = area ? area.name : pipe.to;
    }
}

async function handleAddMaintenance(event) {
    event.preventDefault();

    const mntId = document.getElementById('mntId').value;
    const pipelineId = document.getElementById('mntPipeline').value;
    const affectedArea = document.getElementById('mntArea').value;
    const priority = document.getElementById('mntPriority').value;
    const problem = document.getElementById('mntProblem').value.trim();
    const alertBox = document.getElementById('maintenanceAlert');

    if (!pipelineId || !problem) {
        showInlineAlert(alertBox, 'alert-danger', 'Please provide problem description.');
        return;
    }

    const newJob = {
        id: mntId,
        pipelineId: pipelineId,
        affectedArea: affectedArea,
        problemDescription: problem,
        priority: priority,
        dateAdded: new Date().toISOString().split('T')[0],
        status: 'Queued'
    };

    // 1. Call Java Backend POST /api/maintenance
    const apiResult = await BackendApi.enqueueMaintenance(newJob);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-success', `Maintenance Task ${mntId} enqueued at the back of the FIFO queue!`);
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : ((apiResult && apiResult.message) ? apiResult.message : 'Database error: failed to enqueue maintenance task.');
        showInlineAlert(alertBox, 'alert-danger', `Error: ${errorMsg}`);
        return;
    }

    document.getElementById('mntProblem').value = '';
    generateNextMaintenanceId();
    renderMaintenanceQueueTable();
    renderNetworkFlowGraph();
    renderPipelinesTable();
}

/**
 * Process the next maintenance task at the FRONT of the FIFO queue (ArrayDeque in Java)
 */
async function processNextMaintenanceFIFO() {
    const alertBox = document.getElementById('maintenanceAlert');

    // 1. Call Java Backend POST /api/maintenance/process
    const apiResult = await BackendApi.processNextMaintenanceFIFO();

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        const msg = apiResult.message || 'Next maintenance task processed via FIFO queue in database.';
        showInlineAlert(alertBox, 'alert-success', msg);
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : ((apiResult && apiResult.message) ? apiResult.message : 'No maintenance tasks could be processed in database.');
        showInlineAlert(alertBox, 'alert-warning', errorMsg);
        return;
    }

    renderMaintenanceQueueTable();
    renderNetworkFlowGraph();
    renderPipelinesTable();
    refreshAdminDashboardStats();
}

async function markJobRepaired(jobId) {
    const alertBox = document.getElementById('maintenanceAlert');

    // 1. Call Java Backend PUT /api/maintenance/repair
    const apiResult = await BackendApi.repairMaintenanceJob(jobId);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-success', `Maintenance task ${jobId} finished! Pipeline restored to Working condition.`);
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : ((apiResult && apiResult.message) ? apiResult.message : 'Failed to mark maintenance task repaired.');
        showInlineAlert(alertBox, 'alert-danger', `Error: ${errorMsg}`);
        return;
    }

    renderMaintenanceQueueTable();
    renderNetworkFlowGraph();
    renderPipelinesTable();
    refreshAdminDashboardStats();
}

function renderMaintenanceQueueTable() {
    const queue = DataStore.getMaintenanceQueue();
    const tbody = document.getElementById('maintenanceQueueTbody');
    const badge = document.getElementById('mntQueueCountBadge');
    if (!tbody) return;

    const activeCount = queue.filter(j => j.status !== 'Completed').length;
    if (badge) badge.textContent = `${activeCount} Active / ${queue.length} Total Jobs`;

    if (queue.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted);">No maintenance tasks in queue.</td></tr>';
        return;
    }

    let activeIndex = 0;

    tbody.innerHTML = queue.map((job) => {
        let positionText = '';
        if (job.status !== 'Completed') {
            activeIndex++;
            positionText = activeIndex === 1 ? '<strong style="color: #c62828;">#1 (FRONT)</strong>' : `#${activeIndex}`;
        } else {
            positionText = '<span style="color: #78909c;">Finished</span>';
        }

        const problemText = job.problemDescription || job.problem || '';

        return `
            <tr>
                <td>${positionText}</td>
                <td><strong>${job.id}</strong></td>
                <td><code>${job.pipelineId}</code></td>
                <td>${escapeHtml(job.affectedArea)}</td>
                <td>${escapeHtml(problemText)}</td>
                <td><span class="badge ${getPriorityBadgeClass(job.priority)}">${job.priority}</span></td>
                <td>${job.dateAdded}</td>
                <td><span class="badge ${getStatusBadgeClass(job.status)}">${job.status}</span></td>
                <td>
                    ${job.status !== 'Completed' ? 
                        `<button class="btn btn-sm btn-success" onclick="markJobRepaired('${job.id}')">🛠️ Mark Repaired</button>` : 
                        '<span style="color: var(--status-green);">✓ Resolved</span>'}
                </td>
            </tr>
        `;
    }).join('');
}

// =============================================================================
// SECTION 6: WATER ALLOCATION (PRIORITY ALGORITHM & JAVA INTEGRATION)
// =============================================================================

const PRIORITY_WEIGHTS = {
    'Critical': 5,
    'High': 4,
    'Medium': 3,
    'Low': 2,
    'Very Low': 1
};

async function runWaterAllocationAlgorithm() {
    // 1. Run the PriorityQueue algorithm on Java Backend against MySQL
    const result = await BackendApi.runAllocation();
    if (result && result.records) {
        renderAllocationResults(result);
        await BackendApi.syncFromBackend();
        refreshAdminDashboardStats();
        renderReservoirCards();
        renderNetworkFlowGraph();
        return;
    }

    alert('Failed to execute water allocation algorithm against database backend.');
}

async function calculateAndRenderAllocations(shouldSaveAndNotify) {
    // Read-only preview from Java backend
    const preview = await BackendApi.getAllocationPreview();
    if (preview && preview.records) {
        renderAllocationResults(preview);
        return;
    }

    const areas = DataStore.getDemandAreas();
    const reservoirs = DataStore.getReservoirs();
    const pipelines = DataStore.getPipelines();
    const requests = DataStore.getWaterRequests();

    let activeReservoirs = reservoirs.filter(r => r.status === 'Active');
    let reservoirWaterMap = {};
    activeReservoirs.forEach(r => {
        reservoirWaterMap[r.id] = Number(r.available);
    });

    const totalSystemAvailable = Object.values(reservoirWaterMap).reduce((a, b) => a + b, 0);

    let areaDemandMap = {};
    requests.forEach((req, originalIndex) => {
        if (!areaDemandMap[req.areaId]) {
            areaDemandMap[req.areaId] = {
                areaId: req.areaId,
                areaName: req.areaName || req.areaId,
                requestedQuantity: 0,
                priority: req.priority,
                arrivalIndex: originalIndex
            };
        }
        areaDemandMap[req.areaId].requestedQuantity += Number(req.quantity);
        if (PRIORITY_WEIGHTS[req.priority] > PRIORITY_WEIGHTS[areaDemandMap[req.areaId].priority]) {
            areaDemandMap[req.areaId].priority = req.priority;
        }
    });

    areas.forEach((area, index) => {
        if (!areaDemandMap[area.id]) {
            areaDemandMap[area.id] = {
                areaId: area.id,
                areaName: area.name,
                requestedQuantity: area.maxRequirement,
                priority: area.priority,
                arrivalIndex: 900 + index
            };
        }
    });

    // PriorityQueue sort: Priority DESC, then FIFO arrival
    let sortedQueue = Object.values(areaDemandMap).sort((a, b) => {
        const weightDiff = PRIORITY_WEIGHTS[b.priority] - PRIORITY_WEIGHTS[a.priority];
        if (weightDiff !== 0) return weightDiff;
        return a.arrivalIndex - b.arrivalIndex;
    });

    let totalRequested = 0;
    let totalAllocated = 0;
    let disconnectedCount = 0;
    let unmetDemandTotal = 0;
    let allocationResults = [];

    sortedQueue.forEach(item => {
        totalRequested += item.requestedQuantity;

        const connectedActiveReservoirs = pipelines
            .filter(p => p.to === item.areaId && p.status === 'Working' && reservoirWaterMap[p.from] !== undefined)
            .map(p => p.from);

        const isConnected = connectedActiveReservoirs.length > 0;
        let allocated = 0;
        let status = 'Not Allocated';

        if (!isConnected) {
            allocated = 0;
            status = 'Disconnected';
            disconnectedCount++;
        } else {
            let needed = item.requestedQuantity;
            for (let resId of connectedActiveReservoirs) {
                if (reservoirWaterMap[resId] > 0) {
                    const canGive = Math.min(needed, reservoirWaterMap[resId]);
                    allocated += canGive;
                    reservoirWaterMap[resId] -= canGive;
                    needed -= canGive;
                }
                if (needed === 0) break;
            }

            if (allocated === item.requestedQuantity) status = 'Fully Allocated';
            else if (allocated > 0) status = 'Partially Allocated';
            else status = 'Not Allocated';
        }

        const unmet = Math.max(0, item.requestedQuantity - allocated);
        totalAllocated += allocated;
        unmetDemandTotal += unmet;

        allocationResults.push({
            areaName: item.areaName,
            areaId: item.areaId,
            priority: item.priority,
            requested: item.requestedQuantity,
            allocated: allocated,
            unmet: unmet,
            connectivity: isConnected ? 'Connected' : 'Disconnected',
            status: status
        });
    });

    const remainingWater = Math.max(0, totalSystemAvailable - totalAllocated);

    const summary = {
        totalAvailable: totalSystemAvailable,
        totalRequested: totalRequested,
        totalAllocated: totalAllocated,
        remainingWater: remainingWater,
        unmetDemand: unmetDemandTotal,
        records: allocationResults,
        alertType: (totalRequested > totalSystemAvailable) ? 'warning' : (disconnectedCount > 0 ? 'danger' : 'success'),
        alertMessage: (totalRequested > totalSystemAvailable)
            ? `⚠️ Water Deficit Notice: Total demand (${totalRequested.toLocaleString()} L) exceeds available active water (${totalSystemAvailable.toLocaleString()} L). Critical & High areas prioritized.`
            : (disconnectedCount > 0
                ? `🚫 Disconnected Areas Detected: ${disconnectedCount} demand area(s) could not receive water because feeder pipelines are Failed.`
                : `✅ Allocation Balanced: Sufficient water available. All reachable areas supplied according to priority.`)
    };

    renderAllocationResults(summary);
}

function renderAllocationResults(summary) {
    if (!summary) return;

    document.getElementById('allocTotalAvailable').textContent = `${Number(summary.totalAvailable).toLocaleString()} L`;
    document.getElementById('allocTotalRequested').textContent = `${Number(summary.totalRequested).toLocaleString()} L`;
    document.getElementById('allocTotalAllocated').textContent = `${Number(summary.totalAllocated).toLocaleString()} L`;
    document.getElementById('allocRemainingWater').textContent = `${Number(summary.remainingWater).toLocaleString()} L`;
    document.getElementById('allocUnmetDemand').textContent = `${Number(summary.unmetDemand).toLocaleString()} L`;

    const tbody = document.getElementById('allocationResultsTbody');
    if (tbody && summary.records) {
        tbody.innerHTML = summary.records.map(res => {
            return `
                <tr>
                    <td><strong>${escapeHtml(res.areaName)}</strong></td>
                    <td><span class="badge ${getPriorityBadgeClass(res.priority)}">${res.priority}</span></td>
                    <td>${Number(res.requested).toLocaleString()} L</td>
                    <td><strong style="color: ${res.allocated > 0 ? 'var(--status-green)' : 'inherit'};">${Number(res.allocated).toLocaleString()} L</strong></td>
                    <td style="color: ${res.unmet > 0 ? 'var(--status-red)' : 'inherit'};"><strong>${Number(res.unmet).toLocaleString()} L</strong></td>
                    <td>
                        <span class="badge ${res.connectivity === 'Connected' ? 'badge-working' : 'badge-disconnected'}">
                            ${res.connectivity}
                        </span>
                    </td>
                    <td><span class="badge ${getStatusBadgeClass(res.status)}">${res.status}</span></td>
                </tr>
            `;
        }).join('');
    }

    const alertBox = document.getElementById('allocationAlertBox');
    if (alertBox) {
        alertBox.style.display = 'block';
        alertBox.className = `alert alert-${summary.alertType || 'info'}`;
        alertBox.innerHTML = summary.alertMessage || 'Allocation updated.';
    }
}

// =============================================================================
// SECTION 7: RESERVOIR MANAGEMENT
// =============================================================================

function renderReservoirCards() {
    const reservoirs = DataStore.getReservoirs();
    const container = document.getElementById('reservoirCardsContainer');
    const totalWaterDisplay = document.getElementById('resTotalActiveWaterDisplay');

    let totalActive = 0;
    reservoirs.forEach(r => {
        if (r.status === 'Active') totalActive += Number(r.available);
    });

    if (totalWaterDisplay) {
        totalWaterDisplay.textContent = `${totalActive.toLocaleString()} Litres`;
    }

    if (!container) return;

    container.innerHTML = reservoirs.map(res => {
        const pct = Math.round((Number(res.available) / Number(res.capacity)) * 100);
        const isLow = pct < 25;
        const isActive = res.status === 'Active';

        return `
            <div class="card" style="margin-bottom: 0; border-top: 4px solid ${isActive ? 'var(--primary-blue)' : 'var(--status-red)'};">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <h3 style="color: var(--primary-dark); font-size: 1.15rem;">${res.name}</h3>
                    <span class="badge ${isActive ? 'badge-active' : 'badge-failed'}">${res.status}</span>
                </div>

                <div style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 12px;">
                    ${res.location || 'Municipal Sector'}
                </div>

                <!-- Water Gauge -->
                <div style="margin: 14px 0;">
                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: bold; margin-bottom: 4px;">
                        <span>Current Water Level: ${pct}%</span>
                        <span>${Number(res.available).toLocaleString()} / ${Number(res.capacity).toLocaleString()} L</span>
                    </div>
                    <div class="water-gauge-container">
                        <div class="water-gauge-fill" style="width: ${pct}%; background: ${isLow ? 'linear-gradient(90deg, #f44336, #e53935)' : 'linear-gradient(90deg, #42a5f5, #1976d2)'};"></div>
                    </div>
                </div>

                <div style="font-size: 0.85rem; color: var(--text-body); margin-top: 10px;">
                    <div><strong>Daily Supply Limit:</strong> ${Number(res.dailyLimit).toLocaleString()} L</div>
                    <div><strong>Remaining Capacity:</strong> ${(Number(res.capacity) - Number(res.available)).toLocaleString()} L</div>
                </div>

                ${isLow && isActive ? `
                    <div style="margin-top: 12px; font-size: 0.82rem; color: #c62828; background: #ffebee; padding: 6px 10px; border-radius: 4px;">
                        ⚠️ Warning: Low water level (&lt;25%). Switch demand to backup reservoir if necessary.
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}

function loadSelectedReservoirData() {
    const resId = document.getElementById('resSelect').value;
    const reservoirs = DataStore.getReservoirs();
    const res = reservoirs.find(r => r.id === resId);

    if (res) {
        document.getElementById('resCapacity').value = res.capacity;
        document.getElementById('resAvailable').value = res.available;
        document.getElementById('resDailyLimit').value = res.dailyLimit;
        document.getElementById('resStatus').value = res.status;
    }
}

async function handleUpdateReservoir(event) {
    event.preventDefault();

    const resId = document.getElementById('resSelect').value;
    const available = parseInt(document.getElementById('resAvailable').value, 10);
    const dailyLimit = parseInt(document.getElementById('resDailyLimit').value, 10);
    const status = document.getElementById('resStatus').value;
    const alertBox = document.getElementById('reservoirAlert');

    const reservoirs = DataStore.getReservoirs();
    const res = reservoirs.find(r => r.id === resId);

    if (!res) return;

    if (available < 0 || available > res.capacity) {
        showInlineAlert(alertBox, 'alert-danger', `Invalid Quantity: Available water must be between 0 and total capacity (${res.capacity.toLocaleString()} L).`);
        return;
    }

    if (dailyLimit < 0) {
        showInlineAlert(alertBox, 'alert-danger', 'Daily supply limit cannot be negative.');
        return;
    }

    // 1. Call Java Backend PUT /api/reservoirs
    const apiResult = await BackendApi.updateReservoir(resId, available, dailyLimit, status);

    if (apiResult && apiResult.success) {
        await BackendApi.syncFromBackend();
        showInlineAlert(alertBox, 'alert-success', `${res.name} parameters updated successfully in database!`);
    } else {
        const errorMsg = (apiResult && apiResult.error) ? apiResult.error : ((apiResult && apiResult.message) ? apiResult.message : 'Database update failed.');
        showInlineAlert(alertBox, 'alert-danger', `Error: ${errorMsg}`);
        return;
    }

    renderReservoirCards();
    refreshAdminDashboardStats();
    renderNetworkFlowGraph();
}

// =============================================================================
// GLOBAL UTILITIES & LOGOUT
// =============================================================================

function handleAdminLogout() {
    sessionStorage.removeItem('swd_admin_logged_in');
    sessionStorage.removeItem('swd_admin_name');
    window.location.href = 'index.html';
}

function showInlineAlert(element, alertClass, message) {
    if (!element) return;
    element.className = `alert ${alertClass}`;
    element.innerHTML = message;
    element.style.display = 'block';

    setTimeout(() => {
        element.style.display = 'none';
    }, 6000);
}

// Fallback shared utilities
if (typeof escapeHtml === 'undefined') {
    window.escapeHtml = function(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    };
    if (typeof global !== 'undefined') global.escapeHtml = window.escapeHtml;
}

if (typeof getPriorityBadgeClass === 'undefined') {
    window.getPriorityBadgeClass = function(priority) {
        switch (priority) {
            case 'Critical': return 'badge-critical';
            case 'High': return 'badge-high';
            case 'Medium': return 'badge-medium';
            case 'Low': return 'badge-low';
            case 'Very Low': return 'badge-very-low';
            default: return 'badge-medium';
        }
    };
    if (typeof global !== 'undefined') global.getPriorityBadgeClass = window.getPriorityBadgeClass;
}

if (typeof getStatusBadgeClass === 'undefined') {
    window.getStatusBadgeClass = function(status) {
        switch (status) {
            case 'Allocated':
            case 'Fully Allocated':
            case 'Resolved':
            case 'Working':
            case 'Active':
                return 'badge-allocated';
            case 'Partially Allocated':
            case 'In Progress':
            case 'Under Maintenance':
                return 'badge-partially-allocated';
            case 'Pending':
            case 'Queued':
                return 'badge-pending';
            case 'Failed':
            case 'Disconnected':
            case 'Not Allocated':
            case 'Open':
            case 'Inactive':
                return 'badge-failed';
            default:
                return 'badge-pending';
        }
    };
    if (typeof global !== 'undefined') global.getStatusBadgeClass = window.getStatusBadgeClass;
}
