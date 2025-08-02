package main;

/**
 * ITU-R P.452 Analysis Extension
 * 
 * This class provides analysis capabilities for ITU-R P.452-17 recommendation
 * including loss component breakdown, path analysis, and parameter sensitivity.
 * 
 * Copyright (c) 2024, Extension Implementation
 * Based on the original P452 implementation by Ivica Stevanovic, OFCOM
 */
public class P452Analysis extends P452 {
    
    /**
     * Detailed result structure for transmission loss analysis
     */
    public static class P452AnalysisResult {
        // Basic transmission loss (total)
        public double Lb_total;
        
        // Loss components breakdown
        public double Lbfsg;        // Free-space + atmospheric gases loss
        public double Ldp;          // Diffraction loss for p% time
        public double Ld50;         // Diffraction loss for 50% time
        public double Lba;          // Anomalous propagation loss
        public double Lbs;          // Troposcatter loss
        public double Aht;          // Transmitter clutter loss
        public double Ahr;          // Receiver clutter loss
        
        // Path characteristics
        public double d_total;      // Total path distance (km)
        public double d_los;        // Line-of-sight distance (km)
        public double omega;        // Sea path fraction
        public double dtm;          // Longest continuous land section (km)
        public double dlm;          // Longest continuous inland section (km)
        public double b0;           // Time percentage for anomalous propagation
        
        // Effective heights and horizon distances
        public double hte;          // Effective transmitter height (m)
        public double hre;          // Effective receiver height (m)
        public double dlt;          // Transmitter horizon distance (km)
        public double dlr;          // Receiver horizon distance (km)
        public double theta_total;  // Total angular distance (mrad)
        
        // Path type and dominant mechanism
        public int pathtype;        // 1=LoS, 2=transhorizon
        public String dominantMechanism;  // Description of dominant propagation mechanism
        
        public P452AnalysisResult() {
            // Initialize all values to zero
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ITU-R P.452 Transmission Loss Analysis ===\n");
            sb.append(String.format("Total Basic Transmission Loss: %.2f dB\n", Lb_total));
            sb.append("\n--- Loss Components Breakdown ---\n");
            sb.append(String.format("Free-space + Atmospheric: %.2f dB\n", Lbfsg));
            sb.append(String.format("Diffraction (%% time): %.2f dB\n", Ldp));
            sb.append(String.format("Diffraction (50%% time): %.2f dB\n", Ld50));
            sb.append(String.format("Anomalous Propagation: %.2f dB\n", Lba));
            sb.append(String.format("Troposcatter: %.2f dB\n", Lbs));
            sb.append(String.format("Transmitter Clutter: %.2f dB\n", Aht));
            sb.append(String.format("Receiver Clutter: %.2f dB\n", Ahr));
            sb.append("\n--- Path Characteristics ---\n");
            sb.append(String.format("Total Distance: %.2f km\n", d_total));
            sb.append(String.format("LoS Distance: %.2f km\n", d_los));
            sb.append(String.format("Sea Path Fraction: %.3f\n", omega));
            sb.append(String.format("Longest Land Section: %.2f km\n", dtm));
            sb.append(String.format("Longest Inland Section: %.2f km\n", dlm));
            sb.append(String.format("Anomalous Propagation %%: %.2f%%\n", b0));
            sb.append(String.format("Path Type: %s\n", pathtype == 1 ? "Line-of-Sight" : "Trans-horizon"));
            sb.append(String.format("Dominant Mechanism: %s\n", dominantMechanism));
            return sb.toString();
        }
    }
    
    /**
     * Analyze transmission loss with detailed component breakdown
     * 
     * @param f         Frequency (GHz)
     * @param p         Time percentage (%)
     * @param d         Distance profile (km)
     * @param h         Height profile (m asl)
     * @param zone      Zone types
     * @param htg       Tx antenna height above ground (m)
     * @param hrg       Rx antenna height above ground (m)
     * @param phi_path  Path center latitude (deg)
     * @param Gt        Tx antenna gain (dBi)
     * @param Gr        Rx antenna gain (dBi)
     * @param pol       Polarization (1=horizontal, 2=vertical)
     * @param dct       Distance to coast from Tx (km)
     * @param dcr       Distance to coast from Rx (km)
     * @param DN        Average radio-refractivity lapse-rate (N-units/km)
     * @param N0        Sea-level surface refractivity (N-units)
     * @param press     Dry air pressure (hPa)
     * @param temp      Air temperature (°C)
     * @param ha_t      Clutter height at Tx (m)
     * @param ha_r      Clutter height at Rx (m)
     * @param dk_t      Clutter distance at Tx (km)
     * @param dk_r      Clutter distance at Rx (km)
     * @return Detailed analysis result
     */
    public P452AnalysisResult analyzeTransmissionLoss(double f, double p, double[] d, double[] h, 
                                                     int[] zone, double htg, double hrg, double phi_path, 
                                                     double Gt, double Gr, double pol, double dct, double dcr, 
                                                     double DN, double N0, double press, double temp, 
                                                     double ha_t, double ha_r, double dk_t, double dk_r) {
        
        P452AnalysisResult result = new P452AnalysisResult();
        
        // Calculate total transmission loss first
        result.Lb_total = tl_p452(f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                 dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        
        // Now extract components by replicating key parts of the calculation
        
        // Basic path analysis
        result.d_total = d[d.length - 1] - d[0];
        
        // Zone analysis
        int zone_r = 12;
        result.dtm = longest_cont_dist(d, zone, zone_r);
        zone_r = 2;
        result.dlm = longest_cont_dist(d, zone, zone_r);
        result.omega = path_fraction_sea(d, zone, 3);
        
        // Calculate b0
        result.b0 = beta0(phi_path, result.dtm, result.dlm);
        
        // Earth radius
        double[] aa = earth_rad_eff(DN);
        double ae = aa[0];
        
        // Clutter analysis
        double[] clut = closs_corr(f, d, h, zone, htg, hrg, ha_t, ha_r, dk_t, dk_r);
        int index1 = (int) clut[0];
        int index2 = (int) clut[1];
        double htg_corr = clut[2];
        double hrg_corr = clut[3];
        result.Aht = clut[4];
        result.Ahr = clut[5];
        
        // Extract corrected path
        int N = index2 - index1 + 1;
        double[] dc = new double[N];
        double[] hc = new double[N];
        int[] zonec = new int[N];
        
        for (int ii = index1; ii <= index2; ii++) {
            dc[ii - index1] = d[ii] - d[index1];
            hc[ii - index1] = h[ii];
            zonec[ii - index1] = zone[ii];
        }
        
        // Smooth earth heights
        double[] seh = smooth_earth_heights(dc, hc, htg_corr, hrg_corr, ae, f);
        result.hte = seh[4];
        result.hre = seh[5];
        result.dlt = seh[7];
        result.dlr = seh[8];
        result.theta_total = seh[11];
        result.pathtype = (int) seh[12];
        
        // Calculate LoS distance
        result.d_los = Math.sqrt(2 * ae) * (Math.sqrt(0.001 * result.hte) + Math.sqrt(0.001 * result.hre));
        
        // Calculate individual loss components
        double hts = hc[0] + htg_corr;
        double hrs = hc[hc.length - 1] + hrg_corr;
        
        // Free space + atmospheric loss
        double d3d = Math.sqrt(result.d_total * result.d_total + Math.pow((hts - hrs) / 1000.0, 2.0));
        double[] Lpl = pl_los(d3d, f, p, result.b0, result.omega, temp, press, result.dlt, result.dlr);
        result.Lbfsg = Lpl[0];
        
        // Diffraction losses
        double[] Ldl = dl_p(dc, hc, hts, hrs, seh[2], seh[3], f, result.omega, p, result.b0, DN, pol);
        result.Ldp = Ldl[0];
        result.Ld50 = Ldl[1];
        
        // Anomalous propagation loss
        result.Lba = tl_anomalous(result.d_total, result.dlt, result.dlr, dct, dcr, result.dlm, 
                                 hts, hrs, result.hte, result.hre, seh[6], seh[9], seh[10], 
                                 f, p, temp, press, result.omega, ae, result.b0);
        
        // Troposcatter loss
        result.Lbs = tl_tropo(result.d_total, result.theta_total, f, p, temp, press, N0, Gt, Gr);
        
        // Determine dominant mechanism
        result.dominantMechanism = determineDominantMechanism(result);
        
        return result;
    }
    
    /**
     * Determine the dominant propagation mechanism based on loss components
     */
    private String determineDominantMechanism(P452AnalysisResult result) {
        // Find which loss component dominates
        double maxLoss = Math.max(Math.max(result.Ldp, result.Lba), result.Lbs);
        
        if (result.d_total <= result.d_los) {
            return "Line-of-Sight";
        } else if (Math.abs(result.Ldp - maxLoss) < 0.1) {
            return "Diffraction";
        } else if (Math.abs(result.Lba - maxLoss) < 0.1) {
            return "Anomalous Propagation (Ducting/Layer Reflection)";
        } else if (Math.abs(result.Lbs - maxLoss) < 0.1) {
            return "Troposcatter";
        } else {
            return "Mixed Propagation";
        }
    }
    
    /**
     * Analyze path terrain characteristics
     */
    public static class TerrainAnalysis {
        public double maxHeight;
        public double minHeight;
        public double avgHeight;
        public double heightVariance;
        public double[] clearanceProfile;  // Clearance at each point
        public double minClearance;
        public String terrainClassification;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Terrain Analysis ===\n");
            sb.append(String.format("Max Height: %.1f m\n", maxHeight));
            sb.append(String.format("Min Height: %.1f m\n", minHeight));
            sb.append(String.format("Average Height: %.1f m\n", avgHeight));
            sb.append(String.format("Height Variance: %.1f m²\n", heightVariance));
            sb.append(String.format("Min Clearance: %.1f m\n", minClearance));
            sb.append(String.format("Terrain Classification: %s\n", terrainClassification));
            return sb.toString();
        }
    }
    
    /**
     * Analyze terrain profile characteristics
     */
    public TerrainAnalysis analyzeTerrainProfile(double[] d, double[] h, double htg, double hrg) {
        TerrainAnalysis analysis = new TerrainAnalysis();
        
        int n = h.length;
        double hts = h[0] + htg;
        double hrs = h[n-1] + hrg;
        double dtot = d[n-1] - d[0];
        
        // Basic height statistics
        analysis.maxHeight = h[0];
        analysis.minHeight = h[0];
        double sumHeight = 0;
        
        for (int i = 0; i < n; i++) {
            analysis.maxHeight = Math.max(analysis.maxHeight, h[i]);
            analysis.minHeight = Math.min(analysis.minHeight, h[i]);
            sumHeight += h[i];
        }
        
        analysis.avgHeight = sumHeight / n;
        
        // Height variance
        double sumSquaredDiff = 0;
        for (int i = 0; i < n; i++) {
            double diff = h[i] - analysis.avgHeight;
            sumSquaredDiff += diff * diff;
        }
        analysis.heightVariance = sumSquaredDiff / (n - 1);
        
        // Clearance profile calculation
        analysis.clearanceProfile = new double[n];
        analysis.minClearance = Double.MAX_VALUE;
        
        for (int i = 0; i < n; i++) {
            // Calculate straight-line height at this point
            double straightLineHeight = hts + (hrs - hts) * d[i] / dtot;
            analysis.clearanceProfile[i] = straightLineHeight - h[i];
            analysis.minClearance = Math.min(analysis.minClearance, analysis.clearanceProfile[i]);
        }
        
        // Terrain classification
        double heightRange = analysis.maxHeight - analysis.minHeight;
        if (heightRange < 50) {
            analysis.terrainClassification = "Flat";
        } else if (heightRange < 200) {
            analysis.terrainClassification = "Rolling";
        } else if (heightRange < 500) {
            analysis.terrainClassification = "Hilly";
        } else {
            analysis.terrainClassification = "Mountainous";
        }
        
        return analysis;
    }
    
    /**
     * Parameter sensitivity analysis result
     */
    public static class SensitivityAnalysis {
        public double baselineLoss;
        public double frequencySensitivity;    // dB per GHz
        public double heightSensitivity;       // dB per meter
        public double distanceSensitivity;     // dB per km
        public double[] parameterImpact;       // Impact of each major parameter
        public String[] parameterNames;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Parameter Sensitivity Analysis ===\n");
            sb.append(String.format("Baseline Loss: %.2f dB\n", baselineLoss));
            sb.append(String.format("Frequency Sensitivity: %.3f dB/GHz\n", frequencySensitivity));
            sb.append(String.format("Height Sensitivity: %.4f dB/m\n", heightSensitivity));
            sb.append(String.format("Distance Sensitivity: %.4f dB/km\n", distanceSensitivity));
            sb.append("\n--- Parameter Impact Ranking ---\n");
            for (int i = 0; i < parameterNames.length; i++) {
                sb.append(String.format("%s: %.3f dB change\n", parameterNames[i], parameterImpact[i]));
            }
            return sb.toString();
        }
    }
    
    /**
     * Perform parameter sensitivity analysis
     */
    public SensitivityAnalysis analyzeSensitivity(double f, double p, double[] d, double[] h, 
                                                 int[] zone, double htg, double hrg, double phi_path, 
                                                 double Gt, double Gr, double pol, double dct, double dcr, 
                                                 double DN, double N0, double press, double temp, 
                                                 double ha_t, double ha_r, double dk_t, double dk_r) {
        
        SensitivityAnalysis analysis = new SensitivityAnalysis();
        
        // Calculate baseline
        analysis.baselineLoss = tl_p452(f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                       dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        
        // Frequency sensitivity (±0.1 GHz)
        double f_high = tl_p452(f + 0.1, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                               dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        double f_low = tl_p452(f - 0.1, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                              dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        analysis.frequencySensitivity = (f_high - f_low) / 0.2;
        
        // Height sensitivity (±1m for transmitter)
        double h_high = tl_p452(f, p, d, h, zone, htg + 1, hrg, phi_path, Gt, Gr, pol, 
                               dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        double h_low = tl_p452(f, p, d, h, zone, htg - 1, hrg, phi_path, Gt, Gr, pol, 
                              dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        analysis.heightSensitivity = (h_high - h_low) / 2.0;
        
        // Distance sensitivity (scale distance by ±1%)
        double[] d_high = new double[d.length];
        double[] d_low = new double[d.length];
        double scale_factor = 0.01;
        for (int i = 0; i < d.length; i++) {
            d_high[i] = d[i] * (1 + scale_factor);
            d_low[i] = d[i] * (1 - scale_factor);
        }
        double dist_high = tl_p452(f, p, d_high, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                  dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        double dist_low = tl_p452(f, p, d_low, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                 dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        double distance_change = (d[d.length-1] - d[0]) * scale_factor * 2;
        analysis.distanceSensitivity = (dist_high - dist_low) / distance_change;
        
        // Parameter impact analysis
        analysis.parameterNames = new String[]{"Frequency", "Tx Height", "Rx Height", "Temperature", "Pressure"};
        analysis.parameterImpact = new double[5];
        
        analysis.parameterImpact[0] = Math.abs(f_high - analysis.baselineLoss);
        analysis.parameterImpact[1] = Math.abs(h_high - analysis.baselineLoss);
        
        double rx_high = tl_p452(f, p, d, h, zone, htg, hrg + 1, phi_path, Gt, Gr, pol, 
                                dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false);
        analysis.parameterImpact[2] = Math.abs(rx_high - analysis.baselineLoss);
        
        double temp_high = tl_p452(f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                  dct, dcr, DN, N0, press, temp + 5, ha_t, ha_r, dk_t, dk_r, false);
        analysis.parameterImpact[3] = Math.abs(temp_high - analysis.baselineLoss);
        
        double press_high = tl_p452(f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                                   dct, dcr, DN, N0, press + 10, temp, ha_t, ha_r, dk_t, dk_r, false);
        analysis.parameterImpact[4] = Math.abs(press_high - analysis.baselineLoss);
        
        return analysis;
    }
}