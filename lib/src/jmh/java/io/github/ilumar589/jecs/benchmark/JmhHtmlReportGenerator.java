package io.github.ilumar589.jecs.benchmark;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Generates an HTML report with charts from JMH JSON results.
 * Uses Chart.js for visualization (loaded from CDN).
 */
public class JmhHtmlReportGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: JmhHtmlReportGenerator <input.json> <output.html>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        String jsonContent = Files.readString(Path.of(inputPath));
        List<BenchmarkResult> results = parseResults(jsonContent);
        String html = generateHtml(results);
        
        Files.createDirectories(Path.of(outputPath).getParent());
        Files.writeString(Path.of(outputPath), html);
        
        System.out.println("HTML report generated: " + outputPath);
    }

    static List<BenchmarkResult> parseResults(String json) {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // Split by top-level benchmark objects (each starts with { and has "benchmark" field)
        // Use a simpler approach - find each benchmark entry
        Pattern benchmarkNamePattern = Pattern.compile("\"benchmark\"\\s*:\\s*\"([^\"]+)\"");
        Pattern modePattern = Pattern.compile("\"mode\"\\s*:\\s*\"([^\"]+)\"");
        Pattern scorePattern = Pattern.compile("\"primaryMetric\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*([\\d.E+-]+)");
        Pattern scoreErrorPattern = Pattern.compile("\"primaryMetric\"\\s*:\\s*\\{[^}]*\"scoreError\"\\s*:\\s*\"?([\\d.E+-]+|NaN)\"?");
        Pattern scoreUnitPattern = Pattern.compile("\"primaryMetric\"\\s*:\\s*\\{[^}]*\"scoreUnit\"\\s*:\\s*\"([^\"]+)\"");
        Pattern entityCountPattern = Pattern.compile("\"params\"\\s*:\\s*\\{[^}]*\"entityCount\"\\s*:\\s*\"(\\d+)\"");
        
        // Split the JSON array into individual benchmark objects
        // Each benchmark object starts after [ or }, followed by {
        String[] parts = json.split("(?<=\\[|\\},)\\s*(?=\\{)");
        
        for (String part : parts) {
            Matcher nameMatcher = benchmarkNamePattern.matcher(part);
            if (!nameMatcher.find()) continue;
            
            String benchmark = nameMatcher.group(1);
            
            Matcher modeMatcher = modePattern.matcher(part);
            String mode = modeMatcher.find() ? modeMatcher.group(1) : "unknown";
            
            Matcher scoreMatcher = scorePattern.matcher(part);
            double score = scoreMatcher.find() ? Double.parseDouble(scoreMatcher.group(1)) : 0.0;
            
            Matcher errorMatcher = scoreErrorPattern.matcher(part);
            double error = 0.0;
            if (errorMatcher.find()) {
                String errorStr = errorMatcher.group(1);
                error = "NaN".equals(errorStr) ? 0.0 : Double.parseDouble(errorStr);
            }
            
            Matcher unitMatcher = scoreUnitPattern.matcher(part);
            String unit = unitMatcher.find() ? unitMatcher.group(1) : "us/op";
            
            Matcher entityMatcher = entityCountPattern.matcher(part);
            String entityCount = entityMatcher.find() ? entityMatcher.group(1) : "N/A";
            
            results.add(new BenchmarkResult(benchmark, mode, score, error, unit, entityCount));
        }
        
        return results;
    }

    static String generateHtml(List<BenchmarkResult> results) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>JEcs Systems API - Performance Benchmark Results</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        max-width: 1400px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    h1 {
                        color: #333;
                        text-align: center;
                        margin-bottom: 10px;
                    }
                    .subtitle {
                        text-align: center;
                        color: #666;
                        margin-bottom: 30px;
                    }
                    .chart-container {
                        background: white;
                        border-radius: 8px;
                        padding: 20px;
                        margin-bottom: 30px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .chart-title {
                        font-size: 1.2em;
                        font-weight: bold;
                        margin-bottom: 15px;
                        color: #444;
                    }
                    canvas {
                        max-height: 400px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                        background: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    th, td {
                        padding: 12px 15px;
                        text-align: left;
                        border-bottom: 1px solid #eee;
                    }
                    th {
                        background-color: #4a90d9;
                        color: white;
                        font-weight: 600;
                    }
                    tr:hover {
                        background-color: #f8f9fa;
                    }
                    .score {
                        font-weight: bold;
                        color: #2d6da3;
                    }
                    .error {
                        color: #888;
                        font-size: 0.9em;
                    }
                    .summary {
                        background: white;
                        border-radius: 8px;
                        padding: 20px;
                        margin-bottom: 30px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .summary h2 {
                        margin-top: 0;
                        color: #444;
                    }
                    .summary ul {
                        line-height: 1.8;
                    }
                </style>
            </head>
            <body>
                <h1>🚀 JEcs Systems API - Performance Benchmarks</h1>
                <p class="subtitle">Generated with JMH (Java Microbenchmark Harness)</p>
                
                <div class="summary">
                    <h2>📊 Benchmark Summary</h2>
                    <ul>
                        <li><strong>Total benchmarks:</strong> """ + results.size() + """
                        </li>
                        <li><strong>Benchmark categories:</strong> System Execution, Component Access, Scheduler Building</li>
                        <li><strong>Virtual Threads:</strong> Enabled by default for parallel execution</li>
                    </ul>
                </div>
            """);

        // Group results by benchmark class
        Map<String, List<BenchmarkResult>> grouped = new LinkedHashMap<>();
        for (BenchmarkResult r : results) {
            String className = r.benchmark.substring(r.benchmark.lastIndexOf('.') + 1);
            className = className.substring(0, className.indexOf('_') > 0 ? className.indexOf('_') : className.length());
            grouped.computeIfAbsent(getGroupName(r.benchmark), k -> new ArrayList<>()).add(r);
        }

        int chartId = 0;
        for (Map.Entry<String, List<BenchmarkResult>> entry : grouped.entrySet()) {
            String groupName = entry.getKey();
            List<BenchmarkResult> groupResults = entry.getValue();
            
            html.append("""
                <div class="chart-container">
                    <div class="chart-title">%s</div>
                    <canvas id="chart%d"></canvas>
                </div>
                """.formatted(groupName, chartId));
            
            chartId++;
        }

        // Results table
        html.append("""
            <h2>📋 Detailed Results</h2>
            <table>
                <thead>
                    <tr>
                        <th>Benchmark</th>
                        <th>Parameters</th>
                        <th>Score</th>
                        <th>Error (±)</th>
                        <th>Unit</th>
                    </tr>
                </thead>
                <tbody>
            """);

        for (BenchmarkResult r : results) {
            String shortName = r.benchmark.substring(r.benchmark.lastIndexOf('.') + 1);
            html.append("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td class="score">%.3f</td>
                    <td class="error">%.3f</td>
                    <td>%s</td>
                </tr>
                """.formatted(shortName, r.entityCount, r.score, r.error, r.unit));
        }

        html.append("""
                </tbody>
            </table>
            
            <script>
            """);

        // Generate Chart.js code
        chartId = 0;
        for (Map.Entry<String, List<BenchmarkResult>> entry : grouped.entrySet()) {
            List<BenchmarkResult> groupResults = entry.getValue();
            
            StringBuilder labels = new StringBuilder("[");
            StringBuilder scores = new StringBuilder("[");
            StringBuilder errors = new StringBuilder("[");
            StringBuilder colors = new StringBuilder("[");
            
            String[] chartColors = {
                "'rgba(74, 144, 217, 0.8)'",
                "'rgba(40, 167, 69, 0.8)'",
                "'rgba(255, 193, 7, 0.8)'",
                "'rgba(220, 53, 69, 0.8)'",
                "'rgba(111, 66, 193, 0.8)'",
                "'rgba(23, 162, 184, 0.8)'"
            };
            
            int colorIdx = 0;
            for (int i = 0; i < groupResults.size(); i++) {
                BenchmarkResult r = groupResults.get(i);
                String label = r.benchmark.substring(r.benchmark.lastIndexOf('.') + 1);
                if (!r.entityCount.equals("N/A")) {
                    label += " (" + r.entityCount + " entities)";
                }
                
                if (i > 0) {
                    labels.append(",");
                    scores.append(",");
                    errors.append(",");
                    colors.append(",");
                }
                labels.append("'").append(label).append("'");
                scores.append(r.score);
                errors.append(r.error);
                colors.append(chartColors[colorIdx % chartColors.length]);
                colorIdx++;
            }
            
            labels.append("]");
            scores.append("]");
            errors.append("]");
            colors.append("]");
            
            String unit = groupResults.isEmpty() ? "" : groupResults.get(0).unit;
            
            html.append("""
                new Chart(document.getElementById('chart%d'), {
                    type: 'bar',
                    data: {
                        labels: %s,
                        datasets: [{
                            label: 'Score (%s)',
                            data: %s,
                            backgroundColor: %s,
                            borderColor: %s,
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: {
                                display: true,
                                position: 'top'
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: '%s (lower is better)'
                                }
                            }
                        }
                    }
                });
                """.formatted(chartId, labels, unit, scores, colors, colors, unit));
            
            chartId++;
        }

        html.append("""
            </script>
            
            <div style="text-align: center; margin-top: 40px; color: #888;">
                <p>Generated by JEcs Benchmark Suite</p>
            </div>
            </body>
            </html>
            """);

        return html.toString();
    }

    static String getGroupName(String benchmark) {
        if (benchmark.contains("SystemBenchmark")) {
            return "System Execution Performance";
        } else if (benchmark.contains("ComponentAccessBenchmark")) {
            return "Component Access & Conflict Detection";
        } else if (benchmark.contains("SchedulerBenchmark")) {
            return "Scheduler Building Performance";
        }
        return "Other Benchmarks";
    }

    record BenchmarkResult(
        String benchmark,
        String mode,
        double score,
        double error,
        String unit,
        String entityCount
    ) {}
}
