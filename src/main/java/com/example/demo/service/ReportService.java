package com.example.demo.service;

import com.example.demo.model.UserResponse;
import com.example.demo.repository.UserResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final UserResponseRepository repository;

    @Async
    public CompletableFuture<byte[]> generateReport() throws Exception {
        List<UserResponse> responses = repository.findAll();
        byte[] bytes = generateDocx(responses);
        return CompletableFuture.completedFuture(bytes);
    }

    private byte[] generateDocx(List<UserResponse> responses) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = title.createRun();
            run.setText("Результаты опроса");
            run.setBold(true);
            run.setFontSize(16);

            XWPFTable table = doc.createTable();

            // Заголовок
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("Имя");
            header.addNewTableCell().setText("Email");
            header.addNewTableCell().setText("Оценка");

            // Данные
            for (UserResponse r : responses) {
                XWPFTableRow row = table.createRow();
                row.getCell(0).setText(r.getName());
                row.getCell(1).setText(r.getEmail());
                row.getCell(2).setText(String.valueOf(r.getScore()));
            }

            doc.write(out);
            return out.toByteArray();
        }
    }
}
