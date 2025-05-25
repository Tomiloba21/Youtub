package dev.lobzter.webApp;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Controller
@CrossOrigin("*")
@RequestMapping("/youtube")
public class YTController {

    @Value("${youtube.download.directory}")
    private String downloadDirectory;

    private static final String BINARIES_DIR = "binaries/";
    private static final String YT_DLP_WINDOWS = "yt-dlp.exe";
    private static final String YT_DLP_LINUX = "yt-dlp";
    private static final String YT_DLP_MAC = "yt-dlp_macos";

    @GetMapping("/download")
    public ResponseEntity<String> downloadVideo(
            @RequestParam String url,
            @RequestParam String quality) {

        try {
            String outputPath = downloadDirectory + "/" + System.currentTimeMillis() + ".mp4";
            downloadYouTubeVideo(url, quality, outputPath);
            return ResponseEntity.ok("Video downloaded successfully: " + outputPath);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error downloading video: " + e.getMessage());
        }
    }

    private void downloadYouTubeVideo(String url, String quality, String outputPath) throws IOException, InterruptedException {
        // Create the output directory if it doesn't exist
        File outputDir = new File(outputPath).getParentFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Get the correct yt-dlp binary for the platform
        String ytDlpPath = getYtDlpBinaryPath();



        ProcessBuilder processBuilder = new ProcessBuilder(
                ytDlpPath,
                "-f", "bestvideo[height<=" + quality + "]+bestaudio/best[height<=" + quality + "]", // Download best video and audio
                "--merge-output-format", "mkv", // Merge into .mkv format
                "-o", outputPath + "/%(playlist_index)s - %(title)s.%(ext)s", // Save files with playlist index and title
                "--yes-playlist", // Ensure the entire playlist is downloaded
                "--no-mtime", // Disable modification time
                "--no-overwrites", // Prevent overwriting existing files
                url
        );

        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);

        // Start the process
        Process process = processBuilder.start();

        // Read the output of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line); // Log the output (optional)
        }

        // Wait for the process to complete
        int exitCode = process.waitFor();

        // Check if the process completed successfully
        if (exitCode != 0) {
            throw new RuntimeException("Failed to download video. Exit code: " + exitCode);
        }
    }

    private String getYtDlpBinaryPath() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String binaryName;

        if (os.contains("win")) {
            binaryName = YT_DLP_WINDOWS;
        } else if (os.contains("mac")) {
            binaryName = YT_DLP_MAC;
        } else {
            binaryName = YT_DLP_LINUX;
        }

        // Load binary from resources inside JAR
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(BINARIES_DIR + binaryName)) {
            if (inputStream == null) {
                throw new IOException("Binary not found in resources: " + BINARIES_DIR + binaryName);
            }

            // Copy to a temp file
            Path tempBinary = Files.createTempFile("yt-dlp-", binaryName);
            Files.copy(inputStream, tempBinary, StandardCopyOption.REPLACE_EXISTING);

            File binaryFile = tempBinary.toFile();
            binaryFile.setExecutable(true); // Set permission
            binaryFile.deleteOnExit();

            return binaryFile.getAbsolutePath();
        }
    }

}