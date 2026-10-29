package com.ares.transport.service;

import com.ares.transport.model.Delivery;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/** Builds a single A6 shipping label for an existing delivery. */
@Service
public class DeliveryLabelService {
    private static final float MARGIN = 18f;
    private final BaseFont regular;
    private final BaseFont bold;

    public DeliveryLabelService() throws IOException, DocumentException {
        regular = loadFont("fonts/DejaVuSans.ttf");
        bold = loadFont("fonts/DejaVuSans-Bold.ttf");
    }

    public byte[] render(Delivery delivery) throws IOException, DocumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Rectangle page = PageSize.A6;
        Document document = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        document.addTitle("Shipping label " + delivery.getTrackingNumber());
        document.open();

        Font heading = new Font(bold, 17, Font.BOLD, new Color(15, 23, 42));
        Paragraph shop = new Paragraph("ARES STORE", heading);
        shop.setAlignment(Element.ALIGN_CENTER);
        document.add(shop);
        Paragraph carrier = new Paragraph(delivery.getCarrier(), new Font(bold, 11));
        carrier.setAlignment(Element.ALIGN_CENTER);
        document.add(carrier);
        document.add(new Paragraph(" "));

        PdfContentByte canvas = writer.getDirectContent();
        Barcode128 barcode = new Barcode128();
        barcode.setCode(delivery.getTrackingNumber());
        barcode.setFont(null);
        barcode.setBarHeight(42f);
        barcode.setX(1.1f);
        com.lowagie.text.Image image = barcode.createImageWithBarcode(canvas, Color.BLACK, Color.BLACK);
        float availableWidth = page.getWidth() - 2 * MARGIN;
        image.scalePercent(Math.min(100f, availableWidth * 100f / image.getWidth()));
        image.setAlignment(Element.ALIGN_CENTER);
        document.add(image);
        Paragraph tracking = new Paragraph(delivery.getTrackingNumber(), new Font(bold, 11));
        tracking.setAlignment(Element.ALIGN_CENTER);
        document.add(tracking);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("NGƯỜI NHẬN", new Font(bold, 9)));
        document.add(new Paragraph(delivery.getRecipientName(), new Font(bold, 14)));
        document.add(new Paragraph(maskPhone(delivery.getPhone()), new Font(regular, 11)));
        document.add(new Paragraph(delivery.getAddress() + ", " + delivery.getCity(), new Font(regular, 10)));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("MÃ ĐƠN: " + delivery.getOrderCode(), new Font(bold, 10)));
        document.add(new Paragraph("Ngày tạo: " + delivery.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), new Font(regular, 9)));
        document.close();
        return output.toByteArray();
    }

    private static BaseFont loadFont(String resource) throws IOException, DocumentException {
        try (var input = DeliveryLabelService.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing PDF font resource: " + resource);
            byte[] bytes = input.readAllBytes();
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        }
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone == null ? "" : phone;
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }
}
