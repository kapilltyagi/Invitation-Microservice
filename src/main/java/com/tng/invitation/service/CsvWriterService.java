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
import com.tng.invitation.utils.ByteArrayInOutStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CsvWriterService {

    @Autowired
    private InvitationsValidationService invitationsValidationService;

    @Autowired
    private InvitationRepositoryInterface invitationRepositoryInterface;

    @Autowired
    private InvitationRepository invitationRepository;

    public Mono<ByteArrayInputStream> generateCsv(List<InvitationsDTO> invitationDtoList) {
        String[] columns = {"firstName", "lastName", "companyName", "email", "countryAbbr", "stateProvinceAbbr", "invitedTo", "result", "message"};


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
        String FILE_HEADER = "firstName,lastName,companyName,email,countryAbbr,stateProvinceAbbr,invitedTo,result,message";
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPRATOR = "\n";
        try {
            FileWriter fileWriter = new FileWriter("MassUploadResult.csv");
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

                fileWriter.append(invitations.getInvitedTo());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getResult());
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append(invitations.getMessage());
            }
            fileWriter.flush();
            fileWriter.close();


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "CSV Successfully Written";
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
            String emailFromDB = invitationRepository.findByEmail(arr[3]);
            if (emailFromDB != null ) {
                if (invitationsDTO.getMessage() == null) {
                    invitationsDTO.setMessage("Email already exist");
                }
                invitationsDTO.setResult("Error");

            }
            if(invitationsDTO.getMessage()==null){
                invitationsDTO.setMessage("");
            }
            invitationsDTOList.add(invitationsDTO);

        }
        //Mono<ByteArrayInputStream> csvServiceOutput = csvWriterService.generateCsv(invitationsDTOList);
        //invitationsDTOList.stream().forEach(System.out::println);
        createCsvFromList(invitationsDTOList);
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
