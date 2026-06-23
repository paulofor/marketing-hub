import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentLandingAnalyticsSection {
  sectionId: string;
  visibleMs: number;
  events: number;
}

export interface ExperimentLandingAnalyticsDevice {
  deviceType: "mobile" | "desktop" | "tablet" | string;
  label: string;
  sessions: number;
  percentage: number;
}

export interface ExperimentLandingAnalyticsOperatingSystem {
  operatingSystem: "ios" | "android" | "other" | string;
  label: string;
  sessions: number;
  percentage: number;
}

export interface ExperimentLandingAnalyticsScreenSize {
  screenSize: string;
  label: string;
  width?: number | null;
  height?: number | null;
  sessions: number;
  percentage: number;
}

export interface ExperimentLandingAnalyticsSession {
  sessionId: string;
  eventCount: number;
  pageViews: number;
  sectionViewEvents: number;
  totalVisibleMs: number;
  firstEventAt?: string | null;
  lastEventAt?: string | null;
  lastPageUrl?: string | null;
  lastUserAgent?: string | null;
  deviceType?: string | null;
  deviceLabel?: string | null;
  operatingSystem?: string | null;
  operatingSystemLabel?: string | null;
  screenWidth?: number | null;
  screenHeight?: number | null;
  screenSizeLabel?: string | null;
  topSections: ExperimentLandingAnalyticsSection[];
}

export interface ExperimentLandingAnalyticsVisitor {
  visitorId: string;
  totalSessions: number;
  validPageViews: number;
  firstAccessAt?: string | null;
  lastAccessAt?: string | null;
  intervalSeconds: number;
  distinctPages: number;
  lastUserAgent?: string | null;
  deviceType?: string | null;
  deviceLabel?: string | null;
  recurrent: boolean;
}

export interface ExperimentLandingAnalyticsVisitors {
  probableVisitors: number;
  recurrentVisitors: number;
  singleVisitVisitors: number;
  visitors: ExperimentLandingAnalyticsVisitor[];
}

export interface ExperimentLandingAnalyticsLoadMetrics {
  events: number;
  averageLoadDurationMs: number;
  p95LoadDurationMs: number;
  averageDomContentLoadedMs: number;
  averageFirstContentfulPaintMs: number;
  totalResourceErrors: number;
  sessionsWithoutSectionEvents: number;
  initialEngagementRate: number;
  inAppBrowserSessions: number;
  inAppBrowserPercentage: number;
  diagnosisCode: string;
  diagnosisLabel: string;
  diagnosisSeverity: "success" | "warning" | "danger" | "info" | string;
  diagnosisSummary: string;
  recommendation: string;
}

export interface ExperimentLandingAnalytics {
  totalEvents: number;
  totalSessions: number;
  pageViews: number;
  sectionViewEvents: number;
  totalVisibleMs: number;
  averageVisibleMsPerSession: number;
  lastEventAt?: string | null;
  deviceBreakdown: ExperimentLandingAnalyticsDevice[];
  mobileOperatingSystemBreakdown: ExperimentLandingAnalyticsOperatingSystem[];
  screenSizeBreakdown: ExperimentLandingAnalyticsScreenSize[];
  loadMetrics?: ExperimentLandingAnalyticsLoadMetrics | null;
  visitors?: ExperimentLandingAnalyticsVisitors | null;
  sessions: ExperimentLandingAnalyticsSession[];
}

export function useExperimentLandingAnalytics(experimentId?: string) {
  return useQuery<ExperimentLandingAnalytics>({
    queryKey: ["experiment", experimentId, "landing-analytics"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentLandingAnalytics>(
        `/api/experiments/${experimentId}/funnel/analytics`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
