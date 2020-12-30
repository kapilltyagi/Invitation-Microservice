package com.tng.invitation.controller;

import com.tng.invitation.entity.AdminInvitations;
import com.tng.invitation.entity.InvitationsDTO;
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

    @PostMapping(value = "/batchUpload1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> batchUploadInvitationFromCsv1(@RequestPart("files") FilePart files) throws IOException {
        File convertFile = new File("C:\\workspaces_bini_1\\" + files.filename());
        convertFile.createNewFile();
        System.out.println(files);
        FileOutputStream fout = new FileOutputStream(convertFile);
        fout.write(files.filename().getBytes());
        fout.close();
        return new ResponseEntity<>("File Uploaded successfully", HttpStatus.OK);
    }

    // use single FilePart for single file upload
    @PostMapping(value = "/upload-filePart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public Mono<List<InvitationsDTO>> upload(@RequestPart("file") FilePart filePart) {
        return getLines(filePart).collectList();


    }


    public Flux<InvitationsDTO> getLines(FilePart filePart) {
        return filePart.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .map(this::processAndGetLinesAsList)

                .flatMapIterable(Function.identity());
    }

    private List<InvitationsDTO> processAndGetLinesAsList(String string) {
        List<InvitationsDTO> invitationsDTOList = new ArrayList<>();

        Supplier<Stream<String>> streamSupplier = string::lines;
        List<String> list = streamSupplier.get().collect(Collectors.toList());
        list.remove(0);
        for (String s : list) {
            String[] arr = s.split(",");
            InvitationsDTO invitationsDTO = new InvitationsDTO();
            invitationsDTO.setFirstName(arr[0]);
            invitationsDTO.setLastName(arr[1]);
            invitationsDTO.setCompanyName(arr[2]);
            invitationsDTO.setEmail(arr[3]);
            invitationsDTO.setCountryAbbr(arr[4]);
            invitationsDTO.setStateProvinceAbbr(arr[5]);
            invitationsDTO.setInvitedTo(arr[6]);
            invitationsDTO.setResult("Success");

            boolean firstNamePresent = invitationsValidationService.isFirstNamePresent(arr[0]);
            if (!firstNamePresent) {
                invitationsDTO.setResult("Error");
                invitationsDTO.setMessage("First Name is a required field");
            }
            boolean lastNamePresent = invitationsValidationService.isLastNamePresent(arr[1]);
            if (!lastNamePresent) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage("Last Name is a require field");
                }
                invitationsDTO.setResult("Error");

            }

            boolean emailPresent = invitationsValidationService.isEmailPresent(arr[3]);
            if (!emailPresent) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage("Email is a require field");
                }
                invitationsDTO.setResult("Error");

            }

            boolean isProvinceStateValid = invitationsValidationService.isProvinceStateValid(arr[5]);
            if (!isProvinceStateValid) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage("State should be a valid two letter code");
                }
                invitationsDTO.setResult("Error");

            }

            boolean isValidEmail = invitationsValidationService.isValidEmail(arr[3]);
            if (!isValidEmail) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage("Email is in incorrect format");
                }
                invitationsDTO.setResult("Error");

            }
            invitationsDTOList.add(invitationsDTO);

        }
        //Mono<ByteArrayInputStream> csvServiceOutput = csvWriterService.generateCsv(invitationsDTOList);
        invitationsDTOList.stream().forEach(System.out::println);
        //return streamSupplier.get().collect(Collectors.toList());
        return invitationsDTOList;
    }

}
