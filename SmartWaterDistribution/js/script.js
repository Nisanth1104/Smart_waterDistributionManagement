/**
 * Smart Water Distribution Management System
 * File: js/script.js
 * Description: Core utilities, session management, and common helper functions.
 */

// Global App Configuration
const APP_CONFIG = {
    appName: "Smart Water Distribution Management System",
    version: "2.0.0 (Java & DSA Edition)",
    course: "Java & Data Structures Mini Project"
};

/**
 * Format litres with commas and unit
 * e.g., 20000 -> "20,000 Litres"
 */
function formatWaterVolume(litres) {
    if (isNaN(litres)) return '0 Litres';
    return `${Number(litres).toLocaleString()} Litres`;
}

/**
 * Get current system date in YYYY-MM-DD
 */
function getTodayIsoDate() {
    return new Date().toISOString().split('T')[0];
}

/**
 * Escape string for safe HTML display
 */
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * Helper for Priority Badge CSS classes
 */
function getPriorityBadgeClass(priority) {
    switch (priority) {
        case 'Critical': return 'badge-critical';
        case 'High': return 'badge-high';
        case 'Medium': return 'badge-medium';
        case 'Low': return 'badge-low';
        case 'Very Low': return 'badge-very-low';
        default: return 'badge-medium';
    }
}

/**
 * Helper for Status Badge CSS classes
 */
function getStatusBadgeClass(status) {
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
}

// Console greeting for evaluation
console.log(`%c[${APP_CONFIG.appName}]`, "color: #1976d2; font-weight: bold; font-size: 14px;");
console.log(`Java & Data Structures Client Active. System ready.`);
