package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.response.TicketExportDto;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.entity.Ticket;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.TicketRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventReportService {

    private static final String REGULAR_FONT_PATH = "fonts/arial.ttf";
    private static final String BOLD_FONT_PATH = "fonts/arialbd.ttf";

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderTicketRepository orderTicketRepository;

    public List<TicketExportDto> getTicketStatsByEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        List<Ticket> tickets = ticketRepository.findByEvent(event);
        List<TicketExportDto> dtos = new ArrayList<>();
        for (Ticket ticket : tickets) {
            int total = ticket.getQuantityTotal();
            int sold = orderTicketRepository.sumQuantityByTicket(ticket);
            int remaining = Math.max(0, total - sold);
            BigDecimal revenue = ticket.getPrice().multiply(BigDecimal.valueOf(sold));
            dtos.add(new TicketExportDto(ticket.getTicketType(), total, sold, remaining, revenue));
        }
        return dtos;
    }

    public byte[] generateExcelReport(Integer eventId) {
        authorizeEventReportAccess(eventId);
        List<TicketExportDto> dtos = getTicketStatsByEvent(eventId);
        try {
            return exportTicketStatsToExcel(dtos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] generatePdfReport(Integer eventId) {
        authorizeEventReportAccess(eventId);
        List<TicketExportDto> dtos = getTicketStatsByEvent(eventId);
        try {
            return exportTicketStatsToPdf(dtos);
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    public byte[] generateBuyerReportExcel(Integer eventId) {
        Event event = authorizeEventReportAccess(eventId);
        List<Ticket> tickets = ticketRepository.findByEvent(event);
        List<OrderTicket> paidOrderTickets = orderTicketRepository.findAllByTicketEvent(event);

        Map<User, Map<String, Integer>> userTicketMap = new LinkedHashMap<>();
        Map<User, BigDecimal> userTotalMap = new LinkedHashMap<>();
        aggregateBuyers(paidOrderTickets, userTicketMap, userTotalMap);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Buyer List");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Khách hàng");

            List<String> ticketTypes = tickets.stream().map(Ticket::getTicketType).distinct().toList();
            int col = 2;
            for (String ticketType : ticketTypes) {
                header.createCell(col++).setCellValue(ticketType);
            }
            header.createCell(col).setCellValue("Tổng tiền");

            int rowNum = 1;
            int stt = 1;
            for (User buyer : userTicketMap.keySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(buyer.getFullName());

                Map<String, Integer> ticketMap = userTicketMap.get(buyer);
                int i = 2;
                for (String type : ticketTypes) {
                    row.createCell(i++).setCellValue(ticketMap.getOrDefault(type, 0));
                }
                row.createCell(i).setCellValue(userTotalMap.get(buyer).doubleValue());
            }

            for (int i = 0; i <= ticketTypes.size() + 2; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export buyer Excel report", e);
        }
    }

    public byte[] generateBuyerReportPdf(Integer eventId) {
        Event event = authorizeEventReportAccess(eventId);
        List<Ticket> tickets = ticketRepository.findByEvent(event);
        List<String> ticketTypes = tickets.stream().map(Ticket::getTicketType).distinct().toList();
        List<OrderTicket> paidOrderTickets = orderTicketRepository.findAllByTicketEvent(event);

        Map<User, Map<String, Integer>> userTicketMap = new LinkedHashMap<>();
        Map<User, BigDecimal> userTotalMap = new LinkedHashMap<>();
        aggregateBuyers(paidOrderTickets, userTicketMap, userTotalMap);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = loadVietnameseFont(16f, true);
            Font fontHeader = loadVietnameseFont(12f, true);
            Font fontBody = loadVietnameseFont(12f, false);

            Paragraph title = new Paragraph("Danh sách khách hàng", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            int columnCount = 2 + ticketTypes.size() + 1;
            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("STT", fontHeader)));
            table.addCell(new PdfPCell(new Phrase("Khách hàng", fontHeader)));
            for (String ticketType : ticketTypes) {
                table.addCell(new PdfPCell(new Phrase(ticketType, fontHeader)));
            }
            table.addCell(new PdfPCell(new Phrase("Tổng tiền", fontHeader)));
            table.setHeaderRows(1);

            int stt = 1;
            for (User buyer : userTicketMap.keySet()) {
                Map<String, Integer> ticketMap = userTicketMap.get(buyer);
                table.addCell(new PdfPCell(new Phrase(String.valueOf(stt++), fontBody)));
                table.addCell(new PdfPCell(new Phrase(buyer.getFullName(), fontBody)));
                for (String type : ticketTypes) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(ticketMap.getOrDefault(type, 0)), fontBody)));
                }
                String formattedTotal = userTotalMap.get(buyer).setScale(0, RoundingMode.HALF_UP).toPlainString();
                table.addCell(new PdfPCell(new Phrase(formattedTotal, fontBody)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate buyer PDF report", e);
        }
    }

    public byte[] exportTicketStatsToExcel(List<TicketExportDto> dtos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ticket Stats");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Ticket Type", "Total Quantity", "Sold Quantity", "Remaining Quantity", "Revenue"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (TicketExportDto dto : dtos) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getTicketType());
                row.createCell(1).setCellValue(dto.getTotalQuantity());
                row.createCell(2).setCellValue(dto.getSoldQuantity());
                row.createCell(3).setCellValue(dto.getRemainingQuantity());
                row.createCell(4).setCellValue(dto.getRevenue().doubleValue());
                totalRevenue = totalRevenue.add(dto.getRevenue());
            }

            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(3).setCellValue("Total Revenue");
            totalRow.createCell(4).setCellValue(totalRevenue.doubleValue());

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTicketStatsToPdf(List<TicketExportDto> dtos) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = loadVietnameseFont(16f, true);
            Font fontHeader = loadVietnameseFont(12f, true);
            Font fontBody = loadVietnameseFont(12f, false);

            Paragraph title = new Paragraph("Thống kê vé sự kiện", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 2, 2, 2, 2});

            Stream.of("Loại vé", "Tổng vé", "Đã bán", "Còn lại", "Doanh thu").forEach(h -> {
                PdfPCell header = new PdfPCell(new Phrase(h, fontHeader));
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(header);
            });

            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (TicketExportDto dto : dtos) {
                table.addCell(new PdfPCell(new Phrase(dto.getTicketType(), fontBody)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getTotalQuantity()), fontBody)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getSoldQuantity()), fontBody)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(dto.getRemainingQuantity()), fontBody)));
                table.addCell(new PdfPCell(new Phrase(dto.getRevenue().setScale(0, RoundingMode.HALF_UP).toPlainString(), fontBody)));
                totalRevenue = totalRevenue.add(dto.getRevenue());
            }

            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setColspan(3);
            empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);

            table.addCell(new PdfPCell(new Phrase("Tổng doanh thu", fontHeader)));
            table.addCell(new PdfPCell(new Phrase(totalRevenue.setScale(0, RoundingMode.HALF_UP).toPlainString(), fontHeader)));

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    public Font loadVietnameseFont(float size, boolean bold) throws IOException, DocumentException {
        String resourcePath = bold ? BOLD_FONT_PATH : REGULAR_FONT_PATH;
        ClassPathResource fontResource = new ClassPathResource(resourcePath);
        byte[] fontBytes = fontResource.getContentAsByteArray();
        BaseFont baseFont = BaseFont.createFont(
                bold ? "arialbd.ttf" : "arial.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED,
                true,
                fontBytes,
                null
        );
        return new Font(baseFont, size, Font.NORMAL);
    }

    private Event authorizeEventReportAccess(Integer eventId) {
        UserRole role = SecurityUtils.getCurrentUserRole();
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (role != UserRole.admin && role != UserRole.organizer) {
            throw new SecurityException("You do not have permission to generate reports.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (role != UserRole.admin && !event.getOrganizer().getId().equals(currentUserId)) {
            throw new SecurityException("You do not have permission to access reports for this event.");
        }
        return event;
    }

    private void aggregateBuyers(List<OrderTicket> orderTickets,
                                 Map<User, Map<String, Integer>> userTicketMap,
                                 Map<User, BigDecimal> userTotalMap) {
        for (OrderTicket ot : orderTickets) {
            User buyer = ot.getOrder().getUser();
            String ticketType = ot.getTicket().getTicketType();
            int quantity = ot.getQuantity();
            BigDecimal lineTotal = ot.getUnitPrice().multiply(BigDecimal.valueOf(quantity));

            userTicketMap.computeIfAbsent(buyer, k -> new HashMap<>())
                    .merge(ticketType, quantity, Integer::sum);
            userTotalMap.merge(buyer, lineTotal, BigDecimal::add);
        }
    }
}
