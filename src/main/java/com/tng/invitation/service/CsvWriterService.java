package com.tng.invitation.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvException;
import com.tng.invitation.entity.InvitationsDTO;
import com.tng.invitation.utils.ByteArrayInOutStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
public class CsvWriterService {
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
}
