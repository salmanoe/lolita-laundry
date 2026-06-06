package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.in.GetBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.billing.domain.port.out.OrderInvoiceRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-side queries for billing history and PDF links.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class BillingQueryService implements GetBillingUseCase {

    private static final int PDF_URL_TTL_SECONDS = 900;   // 15 min — long enough to view, short enough not to leak

    private final MonthlyBillingRepository billingRepository;
    private final OrderInvoiceRepository invoiceRepository;
    private final BillingStoragePort storage;

    @Override
    public List<MonthlyBilling> listBillings(Long clientId, Integer year, Integer month) {
        return billingRepository.findAll(clientId, year, month);
    }

    @Override
    public MonthlyBilling getBilling(Long id) {
        return billingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Billing not found: " + id));
    }

    @Override
    public String getBillingPdfUrl(Long id) {
        return presign(getBilling(id).getPdfUrl(), "billing " + id);
    }

    @Override
    public OrderInvoice getInvoiceForOrder(Long orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("No invoice for order " + orderId));
    }

    @Override
    public String getInvoicePdfUrlForOrder(Long orderId) {
        return presign(getInvoiceForOrder(orderId).getPdfUrl(), "invoice of order " + orderId);
    }

    private String presign(String key, String what) {
        if (key == null || key.isBlank()) {
            throw new NotFoundException("No PDF available for " + what);
        }
        return storage.presignedUrl(key, PDF_URL_TTL_SECONDS);
    }
}