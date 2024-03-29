package com.example.adapterdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionException;
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
import java.util.Objects;

public class Adapter {
    public interface VideoToAudioConverterAdapter {
        MultipartFile convertVideoToAudio(String inputVideoFilePath, String outputAudioFilePath) throws ConversionException;
    }

    @Component
    public static class FFmpegAdapter implements VideoToAudioConverterAdapter {

        @Override
        public MultipartFile convertVideoToAudio(String inputVideoFilePath, String outputAudioFilePath) throws ConversionException {
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

            return null;
        }
    }


    @Service
    public static class FileConversionService {

        @Autowired
        private VideoToAudioConverterAdapter converterAdapter;

        public String convertVideoToAudio(MultipartFile videoFile) throws IOException, ConversionException {
            int lastIndex = Objects.requireNonNull(videoFile.getOriginalFilename()).lastIndexOf('.');
            String customVideoDirectoryPath = "D:\\";
            String customVideoDirectoryName = "testConversion";
            String customVideoFileName = videoFile.getOriginalFilename().substring(0, lastIndex) + "Video.mp4";
            File customVideoPath = new File(customVideoDirectoryPath + customVideoDirectoryName);
            if (!customVideoPath.exists())
            {
                customVideoPath.mkdir();
            }
            File customVideoFile = new File(customVideoDirectoryPath+customVideoDirectoryName+customVideoFileName);
            videoFile.transferTo(customVideoFile);
            // Create a temporary output audio file
            String customAudioDirectoryPath = "D:\\testConversion\\";
            String customAudioFileName = videoFile.getOriginalFilename().substring(0, lastIndex) + "Audio.mp3";
            File customAudioFile = new File(customAudioDirectoryPath + customAudioFileName);

            // Convert video to audio using the adapter
            converterAdapter.convertVideoToAudio(customVideoFile.getAbsolutePath(), customAudioFile.getAbsolutePath());

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
                modelAndView.addObject("audioFilePath", audioFilePath);
            } catch (IOException | ConversionException e) {
                modelAndView.setViewName("error");
                modelAndView.addObject("error", "Error occurred: " + e.getMessage());
            }
            return modelAndView;
        }
    }
}
