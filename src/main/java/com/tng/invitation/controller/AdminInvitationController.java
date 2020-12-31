package com.tng.invitation.controller;

import com.tng.invitation.entity.AdminInvitations;
import com.tng.invitation.entity.InvitationsDTO;
import com.tng.invitation.repository.InvitationRepository;
import com.tng.invitation.repository.InvitationRepositoryInterface;
import com.tng.invitation.service.CsvWriterService;
import com.tng.invitation.service.InvitationsValidationService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Slf4j
@Valid
@RequestMapping("/admin/invitation")
public class AdminInvitationController {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private CsvWriterService csvWriterService;
    @Autowired
    private InvitationRepositoryInterface invitationRepositoryInterface;

    @Autowired
    private InvitationsValidationService invitationsValidationService;



    @GetMapping()
    public Flux<AdminInvitations> getAllVendorInvitation() {
        log.info("getAllVendorInvitation() started");
        return invitationRepositoryInterface.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<AdminInvitations>> getVendorInvitationById(@PathVariable("id") UUID id) {
        return invitationRepositoryInterface.findById(id)
                .map(vendorInvitation -> new ResponseEntity<>(vendorInvitation, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AdminInvitations> createVendorInvitation(@RequestBody AdminInvitations vendorInvitation) {
        return invitationRepositoryInterface.save(vendorInvitation);
    }

    @PutMapping("/{invitationId}")
    public Mono<ResponseEntity<AdminInvitations>> updateVendorInvitation(@PathVariable UUID invitationId, @RequestBody AdminInvitations vendorInvitation) {
        return invitationRepositoryInterface.findById(invitationId)
                .map(data -> {
                    //System.out.println(Long.valueOf(data.getCreatedAt().toString()).longValue());
                    new ModelMapper().map(vendorInvitation, data);
                    data.setInvitationId(invitationId);
                    data.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    return data;
                })
                .flatMap(data -> invitationRepositoryInterface.save(data))
                .map(data -> new ResponseEntity<>(data, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteVendorInvitation(@PathVariable UUID id) {
        return invitationRepositoryInterface.deleteById(id);
    }

    @PostMapping(value = "/batchUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> batchUploadInvitationFromCsv(@RequestPart("files") FilePart files) throws IOException {
        File convertFile = new File(files.filename());
        convertFile.createNewFile();
        System.out.println(files);
        FileOutputStream fout = new FileOutputStream(convertFile);
        fout.write(files.filename().getBytes());
        fout.close();
        return new ResponseEntity<>("File Uploaded successfully", HttpStatus.OK);
    }

    // use single FilePart for single file upload
    @PostMapping(value = "/uploadCsv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public Mono<List<AdminInvitations>> upload(@RequestPart("file") FilePart filePart) {
        return csvWriterService.getLines(filePart).collectList();


    }


}
