package com.frontbackend.springboot.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.frontbackend.springboot.model.FileEntity;
import com.frontbackend.springboot.service.FileService;

@RestController
@RequestMapping("files")
public class FilesController {

    private Integer counter=1;

    private final FileService fileService;


    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public FilesController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            FileEntity fileEntity = fileService.save(file);

            return ResponseEntity.status(HttpStatus.OK)
                                 .body(String.format("File uploaded successfully: %s, uuid=%s", file.getOriginalFilename(), fileEntity.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(String.format("Could not upload the file: %s!", file.getOriginalFilename()));
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        Optional<FileEntity> fileEntityOptional = fileService.getFile(id);

        if (!fileEntityOptional.isPresent()) {
            return ResponseEntity.notFound()
                                 .build();
        }

        FileEntity fileEntity = fileEntityOptional.get();
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                             .contentType(MediaType.valueOf(fileEntity.getContentType()))
                             .body(fileEntity.getData());
    }

    @PostMapping("/addData")
    public ResponseEntity<String> addData(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            redisTemplate.opsForValue().set(String.valueOf("file"+counter),bytes);
            counter++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(null,HttpStatus.CREATED);
    }
    @GetMapping(value = "/fetchData",produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> fetchData(@RequestParam("id") Integer fileCounter) {
        byte[] fileByte = null;
        try {
            Object bytes = redisTemplate.opsForValue().get(String.valueOf("file" + fileCounter));
            fileByte   = (byte[]) bytes;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(fileByte,HttpStatus.CREATED);
    }

}
