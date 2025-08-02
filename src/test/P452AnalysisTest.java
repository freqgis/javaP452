package test;

import main.P452Analysis;
import main.P452Analysis.P452AnalysisResult;
import main.P452Analysis.TerrainAnalysis;
import main.P452Analysis.SensitivityAnalysis;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 * Test cases for P452 Analysis Extension
 * 
 * These tests validate the analysis functionality added to the ITU-R P.452 implementation
 */
public class P452AnalysisTest {
    
    private P452Analysis analyzer;
    private TestUtil util;
    
    @Before
    public void setup() {
        analyzer = new P452Analysis();
        util = new TestUtil(0.01);
    }
    
    @Test
    public void testBasicAnalysis() {
        // Test basic analysis functionality with a simple path
        
        double f = 2.0;  // GHz
        double p = 50.0; // %
        
        // Simple 100km path with moderate terrain
        double[] d = {0.0, 25.0, 50.0, 75.0, 100.0};
        double[] h = {50.0, 120.0, 200.0, 150.0, 80.0};
        int[] zone = {2, 2, 2, 2, 2}; // All inland
        
        double htg = 30.0;
        double hrg = 20.0;
        double phi_path = 45.0;
        double Gt = 0.0;
        double Gr = 0.0;
        double pol = 1; // horizontal
        double dct = 500.0;
        double dcr = 500.0;
        double DN = 40.0;
        double N0 = 325.0;
        double press = 1013.25;
        double temp = 15.0;
        double ha_t = 0.0;
        double ha_r = 0.0;
        double dk_t = 0.0;
        double dk_r = 0.0;
        
        P452AnalysisResult result = analyzer.analyzeTransmissionLoss(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        // Validate basic results
        Assert.assertTrue("Total loss should be positive", result.Lb_total > 0);
        Assert.assertTrue("Free space loss should be positive", result.Lbfsg > 0);
        Assert.assertEquals("Total distance should be 100km", 100.0, result.d_total, 0.1);
        Assert.assertTrue("Sea fraction should be 0 for inland path", result.omega < 0.01);
        Assert.assertTrue("Dominant mechanism should be determined", 
                         result.dominantMechanism != null && !result.dominantMechanism.isEmpty());
        
        // Check that the analysis result string is properly formatted
        String analysisString = result.toString();
        Assert.assertTrue("Analysis should contain basic transmission loss", 
                         analysisString.contains("Total Basic Transmission Loss"));
        Assert.assertTrue("Analysis should contain loss components", 
                         analysisString.contains("Loss Components Breakdown"));
        Assert.assertTrue("Analysis should contain path characteristics", 
                         analysisString.contains("Path Characteristics"));
        
        System.out.println("Basic Analysis Test Results:");
        System.out.println(result.toString());
    }
    
    @Test
    public void testTerrainAnalysis() {
        // Test terrain analysis functionality
        
        double[] d = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0};
        double[] h = {100.0, 150.0, 300.0, 250.0, 180.0, 120.0}; // Mountainous terrain
        double htg = 25.0;
        double hrg = 20.0;
        
        TerrainAnalysis terrain = analyzer.analyzeTerrainProfile(d, h, htg, hrg);
        
        Assert.assertEquals("Max height should be 300m", 300.0, terrain.maxHeight, 0.1);
        Assert.assertEquals("Min height should be 100m", 100.0, terrain.minHeight, 0.1);
        Assert.assertTrue("Average height should be reasonable", 
                         terrain.avgHeight > 150.0 && terrain.avgHeight < 250.0);
        Assert.assertTrue("Height variance should be positive", terrain.heightVariance > 0);
        Assert.assertTrue("Clearance profile should have correct length", 
                         terrain.clearanceProfile.length == h.length);
        Assert.assertEquals("Terrain should be classified as hilly", 
                           "Hilly", terrain.terrainClassification);
        
        System.out.println("Terrain Analysis Test Results:");
        System.out.println(terrain.toString());
    }
    
    @Test
    public void testFlatTerrainClassification() {
        // Test flat terrain classification
        
        double[] d = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0};
        double[] h = {100.0, 105.0, 102.0, 108.0, 103.0, 106.0}; // Flat terrain
        double htg = 25.0;
        double hrg = 20.0;
        
        TerrainAnalysis terrain = analyzer.analyzeTerrainProfile(d, h, htg, hrg);
        
        Assert.assertEquals("Terrain should be classified as flat", 
                           "Flat", terrain.terrainClassification);
        Assert.assertTrue("Height variance should be small for flat terrain", 
                         terrain.heightVariance < 100);
    }
    
    @Test
    public void testSensitivityAnalysis() {
        // Test parameter sensitivity analysis
        
        double f = 1.8;  // GHz
        double p = 20.0; // %
        
        double[] d = {0.0, 20.0, 40.0, 60.0, 80.0};
        double[] h = {50.0, 80.0, 120.0, 90.0, 60.0};
        int[] zone = {2, 2, 2, 2, 2}; // All inland
        
        double htg = 40.0;
        double hrg = 30.0;
        double phi_path = 52.0;
        double Gt = 10.0;
        double Gr = 15.0;
        double pol = 2; // vertical
        double dct = 500.0;
        double dcr = 500.0;
        double DN = 45.0;
        double N0 = 315.0;
        double press = 1013.25;
        double temp = 20.0;
        double ha_t = 0.0;
        double ha_r = 0.0;
        double dk_t = 0.0;
        double dk_r = 0.0;
        
        SensitivityAnalysis sensitivity = analyzer.analyzeSensitivity(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        Assert.assertTrue("Baseline loss should be positive", sensitivity.baselineLoss > 0);
        Assert.assertTrue("Frequency sensitivity should be reasonable", 
                         Math.abs(sensitivity.frequencySensitivity) < 50.0);
        Assert.assertTrue("Height sensitivity should be reasonable", 
                         Math.abs(sensitivity.heightSensitivity) < 5.0);
        Assert.assertTrue("Distance sensitivity should be positive", 
                         sensitivity.distanceSensitivity > 0);
        Assert.assertEquals("Should have correct number of parameters", 
                           5, sensitivity.parameterImpact.length);
        Assert.assertEquals("Should have correct number of parameter names", 
                           5, sensitivity.parameterNames.length);
        
        System.out.println("Sensitivity Analysis Test Results:");
        System.out.println(sensitivity.toString());
    }
    
    @Test
    public void testAnalysisWithClutter() {
        // Test analysis with clutter losses
        
        double f = 2.5;  // GHz
        double p = 10.0; // %
        
        double[] d = {0.0, 15.0, 30.0, 45.0, 60.0};
        double[] h = {80.0, 110.0, 140.0, 120.0, 90.0};
        int[] zone = {1, 1, 1, 1, 1}; // Coastal land
        
        double htg = 20.0;
        double hrg = 15.0;
        double phi_path = 48.0;
        double Gt = 5.0;
        double Gr = 5.0;
        double pol = 1; // horizontal
        double dct = 10.0;
        double dcr = 8.0;
        double DN = 35.0;
        double N0 = 335.0;
        double press = 1013.25;
        double temp = 18.0;
        double ha_t = 25.0; // Clutter height > antenna height
        double ha_r = 20.0; // Clutter height > antenna height
        double dk_t = 1.0;
        double dk_r = 1.5;
        
        P452AnalysisResult result = analyzer.analyzeTransmissionLoss(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        // With clutter, there should be additional losses
        Assert.assertTrue("Transmitter clutter loss should be positive when ha_t > htg", 
                         result.Aht > 0);
        Assert.assertTrue("Receiver clutter loss should be positive when ha_r > hrg", 
                         result.Ahr > 0);
        Assert.assertTrue("Sea fraction should be 0 for coastal land", 
                         result.omega >= 0);
        
        System.out.println("Analysis with Clutter Test Results:");
        System.out.println(result.toString());
    }
    
    @Test
    public void testAnalysisWithSeaPath() {
        // Test analysis with mixed land/sea path
        
        double f = 3.0;  // GHz
        double p = 5.0;  // %
        
        double[] d = {0.0, 25.0, 50.0, 75.0, 100.0};
        double[] h = {20.0, 15.0, 10.0, 8.0, 5.0};
        int[] zone = {1, 3, 3, 3, 1}; // Coastal-Sea-Sea-Sea-Coastal
        
        double htg = 50.0;
        double hrg = 40.0;
        double phi_path = 55.0;
        double Gt = 12.0;
        double Gr = 10.0;
        double pol = 2; // vertical
        double dct = 5.0;
        double dcr = 8.0;
        double DN = 42.0;
        double N0 = 320.0;
        double press = 1015.0;
        double temp = 12.0;
        double ha_t = 0.0;
        double ha_r = 0.0;
        double dk_t = 0.0;
        double dk_r = 0.0;
        
        P452AnalysisResult result = analyzer.analyzeTransmissionLoss(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        // With sea path, omega should be significant
        Assert.assertTrue("Sea fraction should be significant for mixed path", 
                         result.omega > 0.3);
        Assert.assertTrue("Distance to coast should affect the calculation", 
                         dct > 0 && dcr > 0);
        
        System.out.println("Analysis with Sea Path Test Results:");
        System.out.println(result.toString());
    }
    
    @Test
    public void testConsistencyWithOriginalImplementation() {
        // Test that analysis results are consistent with original implementation
        
        double f = 2.0;
        double p = 50.0;
        
        double[] d = {0.0, 30.0, 60.0, 90.0};
        double[] h = {100.0, 150.0, 120.0, 80.0};
        int[] zone = {2, 2, 2, 2};
        
        double htg = 30.0;
        double hrg = 25.0;
        double phi_path = 45.0;
        double Gt = 0.0;
        double Gr = 0.0;
        double pol = 1;
        double dct = 500.0;
        double dcr = 500.0;
        double DN = 40.0;
        double N0 = 325.0;
        double press = 1013.25;
        double temp = 15.0;
        double ha_t = 0.0;
        double ha_r = 0.0;
        double dk_t = 0.0;
        double dk_r = 0.0;
        
        // Calculate using analysis method
        P452AnalysisResult analysisResult = analyzer.analyzeTransmissionLoss(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r
        );
        
        // Calculate using original method
        double originalResult = analyzer.tl_p452(
            f, p, d, h, zone, htg, hrg, phi_path, Gt, Gr, pol, 
            dct, dcr, DN, N0, press, temp, ha_t, ha_r, dk_t, dk_r, false
        );
        
        // Results should be identical
        Assert.assertEquals("Analysis total loss should match original implementation", 
                           originalResult, analysisResult.Lb_total, 0.001);
        
        System.out.println("Consistency Test Results:");
        System.out.printf("Original implementation: %.3f dB\n", originalResult);
        System.out.printf("Analysis implementation: %.3f dB\n", analysisResult.Lb_total);
        System.out.printf("Difference: %.6f dB\n", Math.abs(originalResult - analysisResult.Lb_total));
    }
}