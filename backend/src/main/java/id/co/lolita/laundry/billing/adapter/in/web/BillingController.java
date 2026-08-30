package id.co.lolita.laundry.billing.adapter.in.web;

import id.co.lolita.laundry.billing.adapter.in.web.dto.BillingPdfUrlResponse;
import id.co.lolita.laundry.billing.adapter.in.web.dto.GenerateBillingRequest;
import id.co.lolita.laundry.billing.adapter.in.web.dto.MonthlyBillingResponse;
import id.co.lolita.laundry.billing.adapter.in.web.dto.RegeneratePdfsResponse;
import id.co.lolita.laundry.billing.adapter.in.web.dto.ResyncBillingResponse;
import id.co.lolita.laundry.billing.adapter.in.web.dto.UpdateBillingStatusRequest;
import id.co.lolita.laundry.billing.domain.port.in.CreateOrderInvoiceUseCase;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase.GenerateCommand;
import id.co.lolita.laundry.billing.domain.port.in.GetBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.SyncOrderBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase.UpdateStatusCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Monthly billing operations for Lolita staff (OWNER / STAFF). Per-order invoices are served
 * by {@link OrderInvoiceController}.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
class BillingController {

    private final GenerateMonthlyBillingUseCase generateBilling;
    private final GetBillingUseCase billingQuery;
    private final UpdateBillingStatusUseCase updateStatus;
    private final CreateOrderInvoiceUseCase orderInvoices;
    private final SyncOrderBillingUseCase syncBilling;

    @GetMapping
    List<MonthlyBillingResponse> list(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return billingQuery.listBillings(clientId, year, month).stream()
                .map(MonthlyBillingResponse::from).toList();
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    List<MonthlyBillingResponse> generate(@Valid @RequestBody GenerateBillingRequest request) {
        var billings = generateBilling.generate(
                new GenerateCommand(request.clientId(), request.year(), request.month()));
        return billings.stream().map(MonthlyBillingResponse::from).toList();
    }

    @GetMapping("/{id}")
    MonthlyBillingResponse get(@PathVariable Long id) {
        return MonthlyBillingResponse.from(billingQuery.getBilling(id));
    }

    @GetMapping("/{id}/pdf")
    BillingPdfUrlResponse pdf(@PathVariable Long id,
                             @RequestParam(defaultValue = "false") boolean download) {
        // Lazily render the PDF if a past storage outage left it unattached, then pre-sign it.
        // download=true → attachment disposition (the "Unduh" action, reliable on mobile).
        generateBilling.ensurePdfForBilling(id);
        return new BillingPdfUrlResponse(billingQuery.getBillingPdfUrl(id, download));
    }

    @PatchMapping("/{id}/status")
    MonthlyBillingResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateBillingStatusRequest request) {
        return MonthlyBillingResponse.from(
                updateStatus.updateStatus(new UpdateStatusCommand(id, request.status())));
    }

    /**
     * Re-runs the order → billing sync for a client's orders from {@code from} onward (default:
     * the start of the previous month), rebuilding its open DRAFT billings from current state.
     *
     * <p>The reason this exists: the sync is event-driven, so changing a client's <strong>billing
     * cycle</strong> does not by itself move orders already sitting on a DRAFT. Running this after
     * the change re-homes them into the right period. Frozen (ISSUED/PAID) billings are untouched.
     */
    @PostMapping("/resync/{clientId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    ResyncBillingResponse resync(@PathVariable Long clientId,
                                 @RequestParam(required = false)
                                 @org.springframework.format.annotation.DateTimeFormat(iso =
                                         org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                 LocalDate from) {
        var since = from != null ? from : LocalDate.now().minusMonths(1).withDayOfMonth(1);
        return new ResyncBillingResponse(syncBilling.resyncClient(clientId, since));
    }

    /**
     * Bulk-refresh every billing and order-invoice PDF to the current template (layout-only —
     * no amounts change). SUPER_ADMIN only; intended for applying a finalized PDF design before go-live.
     */
    @PostMapping("/regenerate-all-pdfs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    RegeneratePdfsResponse regenerateAllPdfs() {
        int billings = generateBilling.regenerateAllPdfs();
        int invoices = orderInvoices.regenerateAllPdfs();
        return new RegeneratePdfsResponse(billings, invoices);
    }
}
