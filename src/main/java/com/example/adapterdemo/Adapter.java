package com.example.adapterdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

public class Adapter {
    public interface VideoToAudioConverterAdapter {
        void convertVideoToAudio(String inputVideoFilePath, String outputAudioFilePath) throws ConversionException;
    }

    @Component
    public static class FFmpegAdapter implements VideoToAudioConverterAdapter {

        @Override
        public void convertVideoToAudio(String inputVideoFilePath, String outputAudioFilePath) throws ConversionException {
            try {
                // Create FFmpeg process
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "ffmpeg", "-i", inputVideoFilePath, outputAudioFilePath);
                // Redirect error stream to output stream
                processBuilder.redirectErrorStream(true);

                // Start process
                Process process = processBuilder.start();

                // Wait for process to finish
                int exitCode = process.waitFor();

                // Check if process terminated successfully
                if (exitCode != 0) {
                    System.out.println(exitCode);
                } else {
                    System.out.println("Conversion completed successfully.");

                }
            } catch (IOException |InterruptedException e ) {
                throw new RuntimeException(e);
            }

        }
    }


    @Service
    public static class FileConversionService {

        @Autowired
        private VideoToAudioConverterAdapter converterAdapter;

        public String convertVideoToAudio(MultipartFile videoFile) throws IOException, ConversionException {
            String currentDirectory = System.getProperty("user.dir");
            int lastIndex = Objects.requireNonNull(videoFile.getOriginalFilename()).lastIndexOf('.');
            String customAudioDirectoryName = "\\src\\main\\resources\\music\\";
            String customAudioFileName = videoFile.getOriginalFilename().substring(0, lastIndex) + "Audio.mp3";
            File customAudioFile = new File(currentDirectory + customAudioDirectoryName+ customAudioFileName);

            if(videoFile.getOriginalFilename().contains("mp4")) {
                String customVideoFileName = videoFile.getOriginalFilename().substring(0, lastIndex) + "Video.mp4";
                File customVideoPath = new File(currentDirectory + customAudioDirectoryName);
                if (!customVideoPath.exists()) {
                    customVideoPath.mkdir();
                }
                File customVideoFile = new File(currentDirectory + customAudioDirectoryName + customVideoFileName);
                videoFile.transferTo(customVideoFile);
                converterAdapter.convertVideoToAudio(customVideoFile.getAbsolutePath(), customAudioFile.getAbsolutePath());
            }
            else{
                videoFile.transferTo(customAudioFile);
            }
            return customAudioFile.getAbsolutePath();
        }
    }
    @Controller
    public static class FileUploadController {

        @Autowired
        private FileConversionService conversionService;

        @GetMapping("/")
        public String Init()
        {
            return "HomePage";
        }
        @PostMapping("/upload")
        public ModelAndView uploadFile(@RequestParam("file") MultipartFile file) {
            ModelAndView modelAndView = new ModelAndView();
            try {
                String audioFilePath = conversionService.convertVideoToAudio(file);
                modelAndView.setViewName("HomePage");
                modelAndView.addObject("message", "Conversion successful");
                modelAndView.addObject("audioFilePath", URLEncoder.encode(audioFilePath, StandardCharsets.UTF_8));
            } catch (IOException | ConversionException e) {
                modelAndView.setViewName("error");
                modelAndView.addObject("error", "Error occurred: " + e.getMessage());
            }
            return modelAndView;
        }
        @GetMapping("/audio")
        public ResponseEntity<byte[]> getAudioFile(@RequestParam("audioFilePath") String audioFilePath) throws IOException {
            Path audioPath = Paths.get(audioFilePath);

            if (!Files.exists(audioPath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            byte[] audioBytes = Files.readAllBytes(audioPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", audioPath.getFileName().toString());

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
        }
    }
}
