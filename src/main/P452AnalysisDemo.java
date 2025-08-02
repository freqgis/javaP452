package main;

/**
 * Demonstration program for ITU-R P.452 Analysis
 * 
 * This program shows how to use the analysis capabilities of the P452Analysis class
 * to analyze radio wave propagation according to ITU-R P.452-17.
 */
public class P452AnalysisDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ITU-R P.452 Analysis Demonstration ===\n");
        
        P452Analysis analyzer = new P452Analysis();
        
        // Example scenario: Radio link between two stations
        System.out.println("Scenario: 100 km radio link at 2 GHz with mixed terrain\n");
        
        // Input parameters
        double f = 2.0;           // Frequency (GHz)
        double p = 50.0;          // Time percentage (%)
        
        // Terrain profile (distance and height)
        double[] d = {0.0, 20.0, 40.0, 60.0, 80.0, 100.0};
        double[] h = {50.0, 120.0, 200.0, 180.0, 150.0, 80.0};
        int[] zone = {2, 2, 2, 2, 2, 2}; // All inland
        
        double htg = 30.0;        // Tx antenna height (m)
        double hrg = 25.0;        // Rx antenna height (m)
        double phi_path = 45.0;   // Path center latitude (deg)
        double Gt = 12.0;         // Tx antenna gain (dBi)
        double Gr = 10.0;         // Rx antenna gain (dBi)
        double pol = 1;           // Horizontal polarization
        double dct = 500.0;       // Distance to coast from Tx (km)
        double dcr = 500.0;       // Distance to coast from Rx (km)
        double DN = 40.0;         // Radio-refractivity lapse-rate (N-units/km)
        double N0 = 325.0;        // Sea-level surface refractivity (N-units)
        double press = 1013.25;   // Dry air pressure (hPa)
        double temp = 15.0;       // Air temperature (°C)
        double ha_t = 0.0;        // Clutter height at Tx (m)
        double ha_r = 0.0;        // Clutter height at Rx (m)
        double dk_t = 0.0;        // Clutter distance at Tx (km)
        double dk_r = 0.0;        // Clutter distance at Rx (km)
        
        // 1. Comprehensive transmission loss analysis
        System.out.println("1. COMPREHENSIVE TRANSMISSION LOSS ANALYSIS");
        System.out.println("=============================================");
        
        P452Analysis.P452AnalysisResult result = analyzer.analyzeTransmissionLoss(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        System.out.println(result.toString());
        
        // 2. Terrain analysis
        System.out.println("\n2. TERRAIN PROFILE ANALYSIS");
        System.out.println("============================");
        
        P452Analysis.TerrainAnalysis terrain = analyzer.analyzeTerrainProfile(d, h, htg, hrg);
        System.out.println(terrain.toString());
        
        // Display clearance profile
        System.out.println("Clearance Profile:");
        System.out.println("Distance (km) | Height (m) | Clearance (m)");
        System.out.println("-------------|------------|---------------");
        for (int i = 0; i < d.length; i++) {
            System.out.printf("%12.1f | %10.1f | %13.1f\n", 
                            d[i], h[i], terrain.clearanceProfile[i]);
        }
        
        // 3. Parameter sensitivity analysis
        System.out.println("\n3. PARAMETER SENSITIVITY ANALYSIS");
        System.out.println("==================================");
        
        P452Analysis.SensitivityAnalysis sensitivity = analyzer.analyzeSensitivity(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        System.out.println(sensitivity.toString());
        
        // 4. Frequency analysis across multiple frequencies
        System.out.println("\n4. FREQUENCY DEPENDENCE ANALYSIS");
        System.out.println("=================================");
        
        double[] frequencies = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 5.0, 10.0};
        System.out.println("Frequency (GHz) | Basic Loss (dB) | Dominant Mechanism");
        System.out.println("---------------|-----------------|-------------------");
        
        for (double freq : frequencies) {
            P452Analysis.P452AnalysisResult freqResult = analyzer.analyzeTransmissionLoss(
                freq, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
            );
            System.out.printf("%14.1f | %15.2f | %s\n", 
                            freq, freqResult.Lb_total, freqResult.dominantMechanism);
        }
        
        // 5. Time percentage analysis
        System.out.println("\n5. TIME PERCENTAGE ANALYSIS");
        System.out.println("============================");
        
        double[] timePercentages = {1.0, 5.0, 10.0, 20.0, 50.0};
        System.out.println("Time %% | Basic Loss (dB) | Dominant Mechanism");
        System.out.println("-------|-----------------|-------------------");
        
        for (double time : timePercentages) {
            P452Analysis.P452AnalysisResult timeResult = analyzer.analyzeTransmissionLoss(
                f, time, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
                dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
            );
            System.out.printf("%6.1f | %15.2f | %s\n", 
                            time, timeResult.Lb_total, timeResult.dominantMechanism);
        }
        
        // 6. Compare with original implementation for verification
        System.out.println("\n6. VERIFICATION AGAINST ORIGINAL IMPLEMENTATION");
        System.out.println("================================================");
        
        double originalLoss = analyzer.tl_p452(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false
        );
        
        System.out.printf("Original P452 implementation: %.3f dB\n", originalLoss);
        System.out.printf("Analysis implementation:      %.3f dB\n", result.Lb_total);
        System.out.printf("Difference:                   %.6f dB\n", 
                         Math.abs(originalLoss - result.Lb_total));
        
        if (Math.abs(originalLoss - result.Lb_total) < 0.001) {
            System.out.println("✓ Perfect agreement with original implementation");
        } else {
            System.out.println("⚠ Difference detected - requires investigation");
        }
        
        System.out.println("\n=== Analysis Complete ===");
    }
}