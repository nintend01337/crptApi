package com.net.nintend01337.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptAPI {
    private final ScheduledExecutorService scheduler;
    private final Semaphore semaphore;
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final ObjectMapper objectMapper;

    public CrptAPI(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit, true);
        this.scheduler = Executors.newScheduledThreadPool(1);

        this.objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        long period = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException {
        semaphore.acquire();
        try {
            String documentJson = objectMapper.writeValueAsString(document);
            sendDocumentToApi(documentJson, signature);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private void sendDocumentToApi(String documentJson, String signature) throws Exception {
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        HttpURLConnection connection = getUrlConnection(documentJson, signature, apiUrl);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            System.out.println("Документ успешно отправлен API.");
        } else {
            System.out.println("Не удалось отправить документ. Код ошибки: " + responseCode);
        }
    }

    private static HttpURLConnection getUrlConnection(String documentJson, String signature, String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + signature);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = documentJson.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private void shutdown() {
        scheduler.shutdown();
        try {
            if (scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CrptAPI api = new CrptAPI(TimeUnit.SECONDS, 5);

        Document doc = new Document();
        doc.setParticipantInn("123123123554");
        doc.setDocId("docID");
        doc.setDocStatus("status");
        doc.setDocType("doc_type");
        doc.setImportRequest(true);
        doc.setOwnerInn("981737129398");
        doc.setProductionDate("2024-02-23");
        doc.setProductionType("ProductionType");
        doc.setRegDate("2024-02-23");
        doc.setRegNumber("81763263");

        Document.Product product = new Document.Product();
        product.setCertificateDocument("Certificate");
        product.setCertificateDocumentDate("2024-02-23");
        product.setCertificateDocumentNumber("CertificateDocumentNumber");
        product.setOwnerInn("981737129398");
        product.setProducerInn("8971892739217");
        product.setProductionDate("2024-02-23");
        product.setTnvedCode("TnvedCode");
        product.setUitCode("31231");
        product.setUituCode("1231");

        doc.setProducts(new Document.Product[]{product});
        String signature = "Подпись";

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    api.createDocument(doc, signature);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        Thread.sleep(6000);
        api.shutdown();
    }
}


class Document {
    private String participantInn;
    private String docId;
    private String docStatus;
    private String docType;
    private boolean importRequest;
    private String ownerInn;
    private String producerInn;
    private String productionDate;
    private String productionType;
    private Product[] products;
    private String regDate;
    private String regNumber;

    public String participantInn() {
        return participantInn;
    }

    public Document setParticipantInn(String participantInn) {
        this.participantInn = participantInn;
        return this;
    }

    public String docId() {
        return docId;
    }

    public Document setDocId(String docId) {
        this.docId = docId;
        return this;
    }

    public String docStatus() {
        return docStatus;
    }

    public Document setDocStatus(String docStatus) {
        this.docStatus = docStatus;
        return this;
    }

    public String docType() {
        return docType;
    }

    public Document setDocType(String docType) {
        this.docType = docType;
        return this;
    }

    public boolean importRequest() {
        return importRequest;
    }

    public Document setImportRequest(boolean importRequest) {
        this.importRequest = importRequest;
        return this;
    }

    public String ownerInn() {
        return ownerInn;
    }

    public Document setOwnerInn(String ownerInn) {
        this.ownerInn = ownerInn;
        return this;
    }

    public String producerInn() {
        return producerInn;
    }

    public Document setProducerInn(String producerInn) {
        this.producerInn = producerInn;
        return this;
    }

    public String productionDate() {
        return productionDate;
    }

    public Document setProductionDate(String productionDate) {
        this.productionDate = productionDate;
        return this;
    }

    public String productionType() {
        return productionType;
    }

    public Document setProductionType(String productionType) {
        this.productionType = productionType;
        return this;
    }

    public Product[] products() {
        return products;
    }

    public Document setProducts(Product[] products) {
        this.products = products;
        return this;
    }

    public String regDate() {
        return regDate;
    }

    public Document setRegDate(String regDate) {
        this.regDate = regDate;
        return this;
    }

    public String regNumber() {
        return regNumber;
    }

    public Document setRegNumber(String regNumber) {
        this.regNumber = regNumber;
        return this;
    }

    static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public String certificateDocument() {
            return certificateDocument;
        }

        public Product setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
            return this;
        }

        public String certificateDocumentDate() {
            return certificateDocumentDate;
        }

        public Product setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
            return this;
        }

        public String certificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public Product setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
            return this;
        }

        public String ownerInn() {
            return ownerInn;
        }

        public Product setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
            return this;
        }

        public String producerInn() {
            return producerInn;
        }

        public Product setProducerInn(String producerInn) {
            this.producerInn = producerInn;
            return this;
        }

        public String productionDate() {
            return productionDate;
        }

        public Product setProductionDate(String productionDate) {
            this.productionDate = productionDate;
            return this;
        }

        public String tnvedCode() {
            return tnvedCode;
        }

        public Product setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
            return this;
        }

        public String uitCode() {
            return uitCode;
        }

        public Product setUitCode(String uitCode) {
            this.uitCode = uitCode;
            return this;
        }

        public String uituCode() {
            return uituCode;
        }

        public Product setUituCode(String uituCode) {
            this.uituCode = uituCode;
            return this;
        }
    }
}