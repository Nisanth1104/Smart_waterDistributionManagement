/**
 * Smart Water Distribution Management System
 * File: js/data.js
 * Description: Data layer with Java Backend Integration (fetch) and LocalStorage fallback.
 * Designed for Java & Data Structures College Mini Project Phase 1 & Phase 2.
 */

// Key constants for localStorage
const STORAGE_KEYS = {
    RESERVOIRS: 'swd_reservoirs',
    DEMAND_AREAS: 'swd_demand_areas',
    PIPELINES: 'swd_pipelines',
    WATER_REQUESTS: 'swd_water_requests',
    COMPLAINTS: 'swd_complaints',
    MAINTENANCE_QUEUE: 'swd_maintenance_queue',
    ALLOCATIONS: 'swd_allocations'
};

// API Base URL (defaults to Java HttpServer on port 8080)
const API_BASE_URL = (typeof window !== 'undefined' && window.location.origin && window.location.origin.startsWith('http')) 
    ? (window.location.origin + '/api') 
    : 'http://localhost:8080/api';

// Initial default sample data for Reservoirs
const DEFAULT_RESERVOIRS = [
    {
        id: 'RES-A',
        name: 'Reservoir A (North Hill)',
        capacity: 100000,
        available: 40000,
        dailyLimit: 50000,
        status: 'Active',
        location: 'North Hill Sector'
    },
    {
        id: 'RES-B',
        name: 'Reservoir B (South Valley)',
        capacity: 80000,
        available: 25000,
        dailyLimit: 40000,
        status: 'Active',
        location: 'South Valley Sector'
    }
];

// Initial default sample data for Demand Areas
const DEFAULT_DEMAND_AREAS = [
    {
        id: 'AREA-01',
        name: 'Greenfield Colony (Area 1)',
        type: 'Residential',
        minRequirement: 15000,
        maxRequirement: 20000,
        priority: 'High',
        connectedReservoir: 'RES-A',
        description: 'Dense residential neighborhood with 800 households.'
    },
    {
        id: 'AREA-02',
        name: 'City Care Hospital (Area 2)',
        type: 'Critical Institution',
        minRequirement: 12000,
        maxRequirement: 15000,
        priority: 'Critical',
        connectedReservoir: 'RES-A',
        backupReservoir: 'RES-B',
        description: 'Multi-specialty 400-bed hospital with continuous ICU water requirement.'
    },
    {
        id: 'AREA-03',
        name: 'Sunrise Township (Area 3)',
        type: 'Residential',
        minRequirement: 14000,
        maxRequirement: 18000,
        priority: 'Medium',
        connectedReservoir: 'RES-B',
        description: 'Suburban apartment complexes and community center.'
    },
    {
        id: 'AREA-04',
        name: 'Apex Model School (Area 4)',
        type: 'Critical Institution',
        minRequirement: 8000,
        maxRequirement: 12000,
        priority: 'High',
        connectedReservoir: 'RES-A',
        description: 'Senior secondary campus with 2,500 students and staff.'
    },
    {
        id: 'AREA-05',
        name: 'Riverview Heights (Area 5)',
        type: 'Residential',
        minRequirement: 7000,
        maxRequirement: 10000,
        priority: 'Low',
        connectedReservoir: 'RES-B',
        description: 'Low-density gated community on the city perimeter.'
    }
];

// Initial default sample data for Pipelines (PL-04 is intentionally Failed)
const DEFAULT_PIPELINES = [
    {
        id: 'PL-01',
        name: 'North Main Trunk 1',
        from: 'RES-A',
        to: 'AREA-01',
        capacity: 25000,
        status: 'Working',
        lengthKm: 4.2
    },
    {
        id: 'PL-02',
        name: 'Hospital Express Line',
        from: 'RES-A',
        to: 'AREA-02',
        capacity: 20000,
        status: 'Working',
        lengthKm: 2.8
    },
    {
        id: 'PL-03',
        name: 'South Valley Line 1',
        from: 'RES-B',
        to: 'AREA-03',
        capacity: 25000,
        status: 'Working',
        lengthKm: 3.5
    },
    {
        id: 'PL-04',
        name: 'School Zone Feeder',
        from: 'RES-A',
        to: 'AREA-04',
        capacity: 15000,
        status: 'Failed',
        lengthKm: 5.1
    },
    {
        id: 'PL-05',
        name: 'Riverview Line',
        from: 'RES-B',
        to: 'AREA-05',
        capacity: 15000,
        status: 'Working',
        lengthKm: 6.0
    },
    {
        id: 'PL-06',
        name: 'Hospital South Backup Link',
        from: 'RES-B',
        to: 'AREA-02',
        capacity: 15000,
        status: 'Working',
        lengthKm: 4.0
    }
];

// Initial default sample data for Water Demands
const DEFAULT_WATER_REQUESTS = [
    {
        id: 'REQ-101',
        userName: 'Hospital Superintendant',
        areaId: 'AREA-02',
        areaName: 'City Care Hospital (Area 2)',
        date: '2026-10-24',
        quantity: 15000,
        priority: 'Critical',
        notes: 'Critical surgical theatre and dialysis unit requirements',
        status: 'Pending'
    },
    {
        id: 'REQ-102',
        userName: 'RWA President - Greenfield',
        areaId: 'AREA-01',
        areaName: 'Greenfield Colony (Area 1)',
        date: '2026-10-24',
        quantity: 20000,
        priority: 'High',
        notes: 'Daily morning and evening domestic distribution',
        status: 'Pending'
    },
    {
        id: 'REQ-103',
        userName: 'School Principal',
        areaId: 'AREA-04',
        areaName: 'Apex Model School (Area 4)',
        date: '2026-10-25',
        quantity: 12000,
        priority: 'High',
        notes: 'Drinking water and cafeteria sanitation',
        status: 'Pending'
    },
    {
        id: 'REQ-104',
        userName: 'Sunrise Secretary',
        areaId: 'AREA-03',
        areaName: 'Sunrise Township (Area 3)',
        date: '2026-10-25',
        quantity: 18000,
        priority: 'Medium',
        notes: 'General household supply',
        status: 'Pending'
    },
    {
        id: 'REQ-105',
        userName: 'Riverview Resident',
        areaId: 'AREA-05',
        areaName: 'Riverview Heights (Area 5)',
        date: '2026-10-25',
        quantity: 10000,
        priority: 'Low',
        notes: 'Auxiliary garden and tank replenishment',
        status: 'Pending'
    }
];

// Initial default sample data for Complaints
const DEFAULT_COMPLAINTS = [
    {
        id: 'CMP-201',
        userName: 'Apex School Admin',
        areaId: 'AREA-04',
        areaName: 'Apex Model School (Area 4)',
        type: 'Water Not Supplied',
        description: 'Zero water flow in main inlet valve since 7:00 AM. Emergency supply needed.',
        date: '2026-10-25',
        status: 'Open'
    },
    {
        id: 'CMP-202',
        userName: 'Greenfield Resident',
        areaId: 'AREA-01',
        areaName: 'Greenfield Colony (Area 1)',
        type: 'Low Water Supply',
        description: 'Water pressure in 3rd floor apartments is inadequate.',
        date: '2026-10-24',
        status: 'In Progress'
    },
    {
        id: 'CMP-203',
        userName: 'Sunrise Township Secretary',
        areaId: 'AREA-03',
        areaName: 'Sunrise Township (Area 3)',
        type: 'Pipeline Leakage',
        description: 'Minor water seepage observed near valve junction B-3.',
        date: '2026-10-23',
        status: 'Resolved'
    }
];

// Initial default sample data for Maintenance Queue (FIFO)
const DEFAULT_MAINTENANCE_QUEUE = [
    {
        id: 'MNT-301',
        pipelineId: 'PL-04',
        affectedArea: 'Apex Model School (Area 4)',
        problem: 'Main line burst near Sector 4 underpass. Supply disconnected.',
        priority: 'Critical',
        dateAdded: '2026-10-25',
        status: 'Queued'
    },
    {
        id: 'MNT-302',
        pipelineId: 'PL-03',
        affectedArea: 'Sunrise Township (Area 3)',
        problem: 'Periodic flange vibration and joint wear inspection.',
        priority: 'Medium',
        dateAdded: '2026-10-25',
        status: 'Queued'
    }
];

/**
 * Storage Manager Helper Object
 * Provides get/set/reset methods for localStorage data
 */
var DataStore = {
    backendConnected: false,

    init: function() {
        if (!localStorage.getItem(STORAGE_KEYS.RESERVOIRS)) this.setReservoirs([]);
        if (!localStorage.getItem(STORAGE_KEYS.DEMAND_AREAS)) this.setDemandAreas([]);
        if (!localStorage.getItem(STORAGE_KEYS.PIPELINES)) this.setPipelines([]);
        if (!localStorage.getItem(STORAGE_KEYS.WATER_REQUESTS)) this.setWaterRequests([]);
        if (!localStorage.getItem(STORAGE_KEYS.COMPLAINTS)) this.setComplaints([]);
        if (!localStorage.getItem(STORAGE_KEYS.MAINTENANCE_QUEUE)) this.setMaintenanceQueue([]);
    },

    resetAll: function() {
        this.setReservoirs([]);
        this.setDemandAreas([]);
        this.setPipelines([]);
        this.setWaterRequests([]);
        this.setComplaints([]);
        this.setMaintenanceQueue([]);
        localStorage.removeItem(STORAGE_KEYS.ALLOCATIONS);
    },

    getReservoirs: function() {
        const data = localStorage.getItem(STORAGE_KEYS.RESERVOIRS);
        return data ? JSON.parse(data) : [];
    },
    setReservoirs: function(reservoirs) {
        localStorage.setItem(STORAGE_KEYS.RESERVOIRS, JSON.stringify(reservoirs || []));
    },

    getDemandAreas: function() {
        const data = localStorage.getItem(STORAGE_KEYS.DEMAND_AREAS);
        return data ? JSON.parse(data) : [];
    },
    setDemandAreas: function(areas) {
        localStorage.setItem(STORAGE_KEYS.DEMAND_AREAS, JSON.stringify(areas || []));
    },

    getPipelines: function() {
        const data = localStorage.getItem(STORAGE_KEYS.PIPELINES);
        return data ? JSON.parse(data) : [];
    },
    setPipelines: function(pipelines) {
        localStorage.setItem(STORAGE_KEYS.PIPELINES, JSON.stringify(pipelines || []));
    },

    getWaterRequests: function() {
        const data = localStorage.getItem(STORAGE_KEYS.WATER_REQUESTS);
        return data ? JSON.parse(data) : [];
    },
    setWaterRequests: function(requests) {
        localStorage.setItem(STORAGE_KEYS.WATER_REQUESTS, JSON.stringify(requests || []));
    },

    getComplaints: function() {
        const data = localStorage.getItem(STORAGE_KEYS.COMPLAINTS);
        return data ? JSON.parse(data) : [];
    },
    setComplaints: function(complaints) {
        localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(complaints || []));
    },

    getMaintenanceQueue: function() {
        const data = localStorage.getItem(STORAGE_KEYS.MAINTENANCE_QUEUE);
        return data ? JSON.parse(data) : [];
    },
    setMaintenanceQueue: function(queue) {
        localStorage.setItem(STORAGE_KEYS.MAINTENANCE_QUEUE, JSON.stringify(queue || []));
    },

    getAllocations: function() {
        const data = localStorage.getItem(STORAGE_KEYS.ALLOCATIONS);
        return data ? JSON.parse(data) : null;
    },
    setAllocations: function(allocations) {
        localStorage.setItem(STORAGE_KEYS.ALLOCATIONS, JSON.stringify(allocations));
    }
};

/**
 * Backend API Client
 * Uses fetch() to interact with the Java HTTP server (Phase 2 Backend).
 * Gracefully synchronizes with DataStore on success.
 */
var BackendApi = {
    isServerActive: false,

    // Test connectivity to the Java backend
    checkConnection: async function() {
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 1200);
            const response = await fetch(`${API_BASE_URL}/overview`, { signal: controller.signal });
            clearTimeout(timeoutId);
            this.isServerActive = response.ok;
            DataStore.backendConnected = response.ok;
            return response.ok;
        } catch (e) {
            this.isServerActive = false;
            DataStore.backendConnected = false;
            return false;
        }
    },

    // Sync all latest data from Java backend into frontend DataStore
    syncFromBackend: async function() {
        try {
            const [resRes, areaRes, pipeRes, reqRes, cmpRes, mntRes] = await Promise.all([
                fetch(`${API_BASE_URL}/reservoirs`).then(r => r.ok ? r.json() : null),
                fetch(`${API_BASE_URL}/areas`).then(r => r.ok ? r.json() : null),
                fetch(`${API_BASE_URL}/pipelines`).then(r => r.ok ? r.json() : null),
                fetch(`${API_BASE_URL}/requests`).then(r => r.ok ? r.json() : null),
                fetch(`${API_BASE_URL}/complaints`).then(r => r.ok ? r.json() : null),
                fetch(`${API_BASE_URL}/maintenance`).then(r => r.ok ? r.json() : null)
            ]);

            if (resRes) DataStore.setReservoirs(resRes);
            if (areaRes) DataStore.setDemandAreas(areaRes);
            if (pipeRes) DataStore.setPipelines(pipeRes);
            if (reqRes) DataStore.setWaterRequests(reqRes);
            if (cmpRes) DataStore.setComplaints(cmpRes);
            if (mntRes) DataStore.setMaintenanceQueue(mntRes);
            return true;
        } catch (e) {
            console.warn("Backend sync failed, using local cache:", e);
            return false;
        }
    },

    // Login authentication via backend
    login: async function(username, password, role) {
        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, role })
            });
            return await response.json();
        } catch (e) {
            // Fallback to offline check
            if (role === 'admin' && username === 'admin' && password === 'admin123') {
                return { success: true, username: 'admin', displayName: 'Chief Water Engineer (Admin)', role: 'admin' };
            }
            if (role === 'user' && username === 'user' && password === 'user123') {
                return { success: true, username: 'user', displayName: 'Resident User (Greenfield)', role: 'user' };
            }
            return { success: false, error: 'Invalid credentials!' };
        }
    },

    // Submit Water Demand Request
    addWaterRequest: async function(requestObj) {
        try {
            const response = await fetch(`${API_BASE_URL}/requests`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestObj)
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Delete Water Demand Request
    deleteWaterRequest: async function(id) {
        try {
            const response = await fetch(`${API_BASE_URL}/requests?id=${encodeURIComponent(id)}`, {
                method: 'DELETE'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Submit Complaint
    addComplaint: async function(complaintObj) {
        try {
            const response = await fetch(`${API_BASE_URL}/complaints`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(complaintObj)
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Update Complaint Status
    updateComplaintStatus: async function(id, status) {
        try {
            const response = await fetch(`${API_BASE_URL}/complaints/status`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, status })
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Add Demand Area
    addDemandArea: async function(areaObj) {
        try {
            const response = await fetch(`${API_BASE_URL}/areas`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(areaObj)
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Update Demand Area
    updateDemandArea: async function(areaObj) {
        try {
            const response = await fetch(`${API_BASE_URL}/areas`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(areaObj)
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Delete Demand Area
    deleteDemandArea: async function(areaId) {
        try {
            const response = await fetch(`${API_BASE_URL}/areas?id=${encodeURIComponent(areaId)}`, {
                method: 'DELETE'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Update Pipeline Status
    setPipelineStatus: async function(id, status) {
        try {
            const response = await fetch(`${API_BASE_URL}/pipelines/status`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, status })
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Maintenance Queue: Add Job
    enqueueMaintenance: async function(jobObj) {
        try {
            const response = await fetch(`${API_BASE_URL}/maintenance`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(jobObj)
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Maintenance Queue: Process Next FIFO
    processNextMaintenanceFIFO: async function() {
        try {
            const response = await fetch(`${API_BASE_URL}/maintenance/process`, {
                method: 'POST'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Maintenance Queue: Repair Job
    repairMaintenanceJob: async function(id) {
        try {
            const response = await fetch(`${API_BASE_URL}/maintenance/repair`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id })
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Update Reservoir
    updateReservoir: async function(id, available, dailyLimit, status) {
        try {
            const response = await fetch(`${API_BASE_URL}/reservoirs`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, available, dailyLimit, status })
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Run Water Allocation Algorithm (PriorityQueue in Java)
    runAllocation: async function() {
        try {
            const response = await fetch(`${API_BASE_URL}/allocation/run`, {
                method: 'POST'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Get Allocation Preview
    getAllocationPreview: async function() {
        try {
            const response = await fetch(`${API_BASE_URL}/allocation`, {
                method: 'GET'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Network Reachability (Graph BFS in Java)
    getNetworkConnectivity: async function() {
        try {
            const response = await fetch(`${API_BASE_URL}/network/connectivity`, {
                method: 'GET'
            });
            return await response.json();
        } catch (e) {
            return null;
        }
    },

    // Reset All Data
    resetAllData: async function() {
        try {
            await fetch(`${API_BASE_URL}/reset`, { method: 'POST' });
        } catch (e) {
        }
    }
};

// Expose globally
if (typeof window !== 'undefined') {
    window.DataStore = DataStore;
    window.BackendApi = BackendApi;
}
if (typeof global !== 'undefined') {
    global.DataStore = DataStore;
    global.BackendApi = BackendApi;
}

// Auto initialize on script load
DataStore.init();
