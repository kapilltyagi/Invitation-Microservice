package com.tng.invitation.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvException;
import com.tng.invitation.entity.AdminInvitations;
import com.tng.invitation.entity.InvitationsDTO;
import com.tng.invitation.repository.InvitationRepository;
import com.tng.invitation.repository.InvitationRepositoryInterface;
import com.tng.invitation.utils.AppConstants;
import com.tng.invitation.utils.ByteArrayInOutStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tng.invitation.utils.AppConstants.*;

@Service
public class CsvWriterService {

    @Autowired
    private InvitationsValidationService invitationsValidationService;

    @Autowired
    private InvitationRepositoryInterface invitationRepositoryInterface;

    @Autowired
    private InvitationRepository invitationRepository;

    public Mono<ByteArrayInputStream> generateCsv(List<InvitationsDTO> invitationDtoList) {
        String[] columns = {"First Name", "Last Name", "Company", "Email", "Country", "Province/State", "Result", "Message"};


        return Mono.fromCallable(() -> {
            try {
                ByteArrayInOutStream stream = new ByteArrayInOutStream();
                OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
                CSVWriter writer = new CSVWriter(streamWriter);

                ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
                mappingStrategy.setType(InvitationsDTO.class);
                mappingStrategy.setColumnMapping(columns);
                writer.writeNext(columns);

                StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                        .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .withMappingStrategy(mappingStrategy)
                        .withSeparator(',')
                        .build();

                beanToCsv.write(invitationDtoList);
                streamWriter.flush();
                return stream.getInputStream();
            } catch (CsvException | IOException e) {
                throw new RuntimeException(e);
            }

        }).subscribeOn(Schedulers.boundedElastic());

    }

    public String createCsvFromList(List<InvitationsDTO> invitationDtoList) {
        File file = null;
        try {
            file = new File("MassUploadResult"+System.currentTimeMillis()+".csv");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.append(FILE_HEADER);
            for (InvitationsDTO invitations : invitationDtoList) {
                fileWriter.append(NEW_LINE_SEPRATOR);

                fileWriter.append(invitations.getFirstName());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getLastName());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getCompanyName());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getEmail());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getCountryAbbr());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getStateProvinceAbbr());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getResult());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getMessage());
            }
            System.out.println("file.getAbsolutePath()="+file.getAbsolutePath());

            fileWriter.flush();
            fileWriter.close();


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "CSV Created at location: "+file.getAbsolutePath();
    }


    public Flux<AdminInvitations> getLines(FilePart filePart) {
        return filePart.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .map(this::processAndGetLinesAsList)

                .flatMapIterable(Function.identity())
                .filter(invitation -> invitation.getResult().equals("Success"))
                .map(data -> {
                    AdminInvitations invitations = new AdminInvitations();
                    invitations.setFirstName(data.getFirstName());
                    invitations.setLastName(data.getLastName());
                    invitations.setCompanyName(data.getCompanyName());
                    invitations.setEmail(data.getEmail());
                    invitations.setCountryAbbr(data.getCountryAbbr());
                    invitations.setStateProvinceAbbr(data.getStateProvinceAbbr());
                    invitations.setInvitedTo(data.getInvitedTo());
                    invitations.setInvitationStatus(AppConstants.INVITATION_STATUS);
                    return invitations;
                })
                .flatMap(data -> invitationRepositoryInterface.save(data));


    }

    public List<InvitationsDTO> processAndGetLinesAsList(String string) {
        List<InvitationsDTO> invitationsDTOList = new ArrayList<>();

        Supplier<Stream<String>> streamSupplier = string::lines;
        List<String> list = streamSupplier.get().collect(Collectors.toList());
        list.remove(0);
        for (String s : list) {
            String[] arr = s.split(COMMA_DELIMITER);
            InvitationsDTO invitationsDTO = new InvitationsDTO();
            invitationsDTO.setFirstName(arr[0]);
            invitationsDTO.setLastName(arr[1]);
            invitationsDTO.setCompanyName(arr[2]);
            invitationsDTO.setEmail(arr[3]);
            invitationsDTO.setCountryAbbr(arr[4]);
            invitationsDTO.setStateProvinceAbbr(arr[5]);
            invitationsDTO.setInvitedTo(arr[6]);
            invitationsDTO.setResult(SUCCESS);



            boolean firstNamePresent = invitationsValidationService.isFirstNamePresent(arr[0]);
            if (!firstNamePresent) {
                invitationsDTO.setResult(ERROR);
                invitationsDTO.setMessage(REQUIRED_FIRST_NAME);
            }
            boolean lastNamePresent = invitationsValidationService.isLastNamePresent(arr[1]);
            if (!lastNamePresent) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage(REQUIRED_LAST_NAME);
                }
                invitationsDTO.setResult(ERROR);

            }

            boolean emailPresent = invitationsValidationService.isEmailPresent(arr[3]);
            if (!emailPresent) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage(REQUIRED_EMAIL);
                }
                invitationsDTO.setResult(ERROR);

            }

            boolean isProvinceStateValid = invitationsValidationService.isProvinceStateValid(arr[5]);
            if (!isProvinceStateValid) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage(VALID_STATE_PROVINCE);
                }
                invitationsDTO.setResult(ERROR);

            }

            boolean isValidEmail = invitationsValidationService.isValidEmail(arr[3]);
            if (!isValidEmail) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage(EMAIL_INCORRECT_FORMAT);
                }
                invitationsDTO.setResult(ERROR);

            }
            String emailFromDB = invitationRepository.findByEmail(arr[3]);
            if (emailFromDB != null ) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage(EMAIL_ALREADY_EXISTS);
                }
                invitationsDTO.setResult(ERROR);

            }
            if(invitationsDTO.getMessage()==null){
                invitationsDTO.setMessage(NULL_DELIMITER);
            }
            invitationsDTOList.add(invitationsDTO);

        }
        //Mono<ByteArrayInputStream> csvServiceOutput = csvWriterService.generateCsv(invitationsDTOList);
        //invitationsDTOList.stream().forEach(System.out::println);
        String csvLocation = createCsvFromList(invitationsDTOList);
        //return streamSupplier.get().collect(Collectors.toList());
        //saving success records in DB

        /*Flux.fromIterable(invitationsDTOList).filter(invitation -> invitation.getResult().equals("Success"))
                .map(data -> {
                    AdminInvitations invitations = new AdminInvitations();
                    invitations.setFirstName(data.getFirstName());
                    invitations.setLastName(data.getLastName());
                    invitations.setCompanyName(data.getCompanyName());
                    invitations.setEmail(data.getEmail());
                    invitations.setCountryAbbr(data.getCountryAbbr());
                    invitations.setStateProvinceAbbr(data.getStateProvinceAbbr());
                    invitations.setInvitedTo(data.getInvitedTo());
                    return invitations;
                })
                .flatMap(data -> invitationRepositoryInterface.save(data));*/
        return invitationsDTOList;
    }

}
