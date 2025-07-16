package com.screenleads.backend.app.application.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class FirebaseStorageService {

    public String upload(File file, String destination) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(destination, new FileInputStream(file), Files.probeContentType(file.toPath()));

        // Ya no se puede hacer blob.createAcl(...) si uniform bucket-level access está
        // activado.

        // Devuelve la URL pública si el bucket permite acceso público, o una URL
        // firmada si no.
        return String.format("https://storage.googleapis.com/%s/%s", bucket.getName(), blob.getName());
    }
}
