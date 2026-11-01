package com.smartwater.server;

import com.smartwater.model.*;
import java.util.*;

/**
 * SimpleJson Class
 * Zero-dependency JSON serializer and parser tailored for the Smart Water System.
 * Eliminates the need for external libraries (Jackson/Gson/Maven/Gradle) so that
 * the project compiles and runs out-of-the-box on any standard Java JDK.
 */
public class SimpleJson {

    // =========================================================================
    // SERIALIZERS (Java Objects -> JSON String)
    // =========================================================================

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String reservoirToJson(Reservoir r) {
        if (r == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"capacity\":%d,\"available\":%d,\"dailyLimit\":%d,\"status\":\"%s\",\"location\":\"%s\"}",
                escape(r.getId()), escape(r.getName()), r.getCapacity(), r.getAvailable(), r.getDailyLimit(), escape(r.getStatus()), escape(r.getLocation()));
    }

    public static String reservoirsToJson(Collection<Reservoir> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (Reservoir r : list) {
            if (i++ > 0) sb.append(",");
            sb.append(reservoirToJson(r));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String demandAreaToJson(DemandArea a) {
        if (a == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"minRequirement\":%d,\"maxRequirement\":%d,\"priority\":\"%s\",\"connectedReservoir\":\"%s\",\"backupReservoir\":\"%s\",\"description\":\"%s\"}",
                escape(a.getId()), escape(a.getName()), escape(a.getAreaType()), a.getMinRequirement(), a.getMaxRequirement(),
                escape(a.getEffectivePriority().getDisplayName()), escape(a.getConnectedReservoir()),
                escape(a.getBackupReservoir() != null ? a.getBackupReservoir() : ""), escape(a.getDescription()));
    }

    public static String demandAreasToJson(Collection<DemandArea> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (DemandArea a : list) {
            if (i++ > 0) sb.append(",");
            sb.append(demandAreaToJson(a));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String pipelineToJson(Pipeline p) {
        if (p == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"capacity\":%d,\"status\":\"%s\",\"lengthKm\":%.1f}",
                escape(p.getId()), escape(p.getName()), escape(p.getFromReservoirId()), escape(p.getToAreaId()),
                p.getCapacity(), escape(p.getStatus().getDisplayName()), p.getLengthKm());
    }

    public static String pipelinesToJson(Collection<Pipeline> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (Pipeline p : list) {
            if (i++ > 0) sb.append(",");
            sb.append(pipelineToJson(p));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String waterRequestToJson(WaterRequest req) {
        if (req == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"userName\":\"%s\",\"areaId\":\"%s\",\"areaName\":\"%s\",\"quantity\":%d,\"priority\":\"%s\",\"notes\":\"%s\",\"date\":\"%s\",\"status\":\"%s\",\"arrivalOrder\":%d}",
                escape(req.getId()), escape(req.getUserName()), escape(req.getAreaId()), escape(req.getAreaName()),
                req.getQuantity(), escape(req.getPriority().getDisplayName()), escape(req.getNotes()),
                escape(req.getDate()), escape(req.getStatus()), req.getArrivalOrder());
    }

    public static String waterRequestsToJson(Collection<WaterRequest> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (WaterRequest r : list) {
            if (i++ > 0) sb.append(",");
            sb.append(waterRequestToJson(r));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String complaintToJson(Complaint c) {
        if (c == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"userName\":\"%s\",\"areaId\":\"%s\",\"areaName\":\"%s\",\"type\":\"%s\",\"description\":\"%s\",\"date\":\"%s\",\"status\":\"%s\"}",
                escape(c.getId()), escape(c.getUserName()), escape(c.getAreaId()), escape(c.getAreaName()),
                escape(c.getType()), escape(c.getDescription()), escape(c.getDate()), escape(c.getStatus()));
    }

    public static String complaintsToJson(Collection<Complaint> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (Complaint c : list) {
            if (i++ > 0) sb.append(",");
            sb.append(complaintToJson(c));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String maintenanceRequestToJson(MaintenanceRequest m) {
        if (m == null) return "{}";
        return String.format(Locale.US,
                "{\"id\":\"%s\",\"pipelineId\":\"%s\",\"affectedArea\":\"%s\",\"problemDescription\":\"%s\",\"priority\":\"%s\",\"dateAdded\":\"%s\",\"status\":\"%s\"}",
                escape(m.getId()), escape(m.getPipelineId()), escape(m.getAffectedArea()), escape(m.getProblemDescription()),
                escape(m.getPriority()), escape(m.getDateAdded()), escape(m.getStatus()));
    }

    public static String maintenanceQueueToJson(Collection<MaintenanceRequest> list) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (MaintenanceRequest m : list) {
            if (i++ > 0) sb.append(",");
            sb.append(maintenanceRequestToJson(m));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String allocationRecordToJson(AllocationRecord rec) {
        if (rec == null) return "{}";
        return String.format(Locale.US,
                "{\"areaId\":\"%s\",\"areaName\":\"%s\",\"priority\":\"%s\",\"requested\":%d,\"allocated\":%d,\"unmet\":%d,\"connectivity\":\"%s\",\"status\":\"%s\",\"suppliedBy\":\"%s\"}",
                escape(rec.getAreaId()), escape(rec.getAreaName()), escape(rec.getPriority()),
                rec.getRequested(), rec.getAllocated(), rec.getUnmet(), escape(rec.getConnectivity()),
                escape(rec.getStatus()), escape(rec.getSuppliedBy()));
    }

    public static String allocationSummaryToJson(AllocationSummary s) {
        if (s == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(String.format(Locale.US, "\"totalAvailable\":%d,\"totalRequested\":%d,\"totalAllocated\":%d,\"remainingWater\":%d,\"unmetDemand\":%d,",
                s.getTotalAvailable(), s.getTotalRequested(), s.getTotalAllocated(), s.getRemainingWater(), s.getUnmetDemand()));
        sb.append(String.format("\"alertMessage\":\"%s\",\"alertType\":\"%s\",\"records\":[",
                escape(s.getAlertMessage()), escape(s.getAlertType())));

        for (int i = 0; i < s.getRecords().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(allocationRecordToJson(s.getRecords().get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String successResponse(String message) {
        return String.format("{\"success\":true,\"message\":\"%s\"}", escape(message));
    }

    public static String errorResponse(String error) {
        return String.format("{\"success\":false,\"error\":\"%s\"}", escape(error));
    }

    // =========================================================================
    // PARSER (JSON String -> Simple Key-Value Map)
    // =========================================================================

    /**
     * Parses a flat or semi-flat JSON object string into a Key-Value Map<String, String>.
     */
    public static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        boolean inQuotes = false;
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean parsingKey = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (c == '\\' && i + 1 < trimmed.length()) {
                if (parsingKey) currentKey.append(trimmed.charAt(i + 1));
                else currentValue.append(trimmed.charAt(i + 1));
                i++;
                continue;
            }

            if (c == '\"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes) {
                if (c == ':') {
                    parsingKey = false;
                    continue;
                }
                if (c == ',') {
                    String key = currentKey.toString().trim();
                    String val = currentValue.toString().trim();
                    if (!key.isEmpty()) {
                        map.put(key, val);
                    }
                    currentKey.setLength(0);
                    currentValue.setLength(0);
                    parsingKey = true;
                    continue;
                }
            }

            if (parsingKey) {
                currentKey.append(c);
            } else {
                currentValue.append(c);
            }
        }

        String key = currentKey.toString().trim();
        String val = currentValue.toString().trim();
        if (!key.isEmpty()) {
            map.put(key, val);
        }

        return map;
    }

    public static String getString(Map<String, String> map, String key, String defaultVal) {
        String v = map.get(key);
        return v != null ? v : defaultVal;
    }

    public static int getInt(Map<String, String> map, String key, int defaultVal) {
        String v = map.get(key);
        if (v == null) return defaultVal;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
