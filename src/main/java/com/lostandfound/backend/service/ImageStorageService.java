package com.lostandfound.backend.service;

import com.lostandfound.backend.exception.BadRequestException;
import com.lostandfound.backend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Value("${storage.public-base-url}")
    private String publicBaseUrl;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo vazio");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Apenas JPEG, PNG or WEBP images are allowed");
        }

        try {
            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            String extension = extractExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;

            Path destination = directory.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return publicBaseUrl + "/" + fileName;
        } catch (IOException e) {
            log.error("Falha ao armazenar a imagem", e);
            throw new BadRequestException("Falha ao armazenar a imagem");
        }
    }

    public Resource load(String filename) {
        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = directory.resolve(filename).normalize();

            // impede path traversal (ex.: ../../etc/passwd)
            if (!file.startsWith(directory)) {
                throw new BadRequestException("Nome de arquivo inválido");
            }

            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Imagem não encontrada");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Imagem não encontrada");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
