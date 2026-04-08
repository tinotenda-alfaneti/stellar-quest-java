package stellarquest.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import stellarquest.web.dto.AccountMergeRequest;
import stellarquest.web.dto.CreateAccountRequest;
import stellarquest.web.dto.FundRequest;
import stellarquest.web.dto.ManageDataRequest;
import stellarquest.web.dto.OfferRequest;
import stellarquest.web.dto.PathPaymentRequest;
import stellarquest.web.dto.PaymentRequest;
import stellarquest.web.dto.TrustlineRequest;

@RestController
@RequestMapping("/api")
public final class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ok("Loaded app configuration.", transactionService.appConfig());
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Map<String, Object>> account(@PathVariable String accountId) throws IOException {
        return ok("Loaded account balances.", transactionService.loadAccount(accountId));
    }

    @PostMapping("/transactions/fund")
    public ResponseEntity<Map<String, Object>> fund(@RequestBody(required = false) FundRequest request) throws IOException {
        return ok("Friendbot request completed.", transactionService.fund(request));
    }

    @PostMapping("/transactions/create-account")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody(required = false) CreateAccountRequest request)
            throws IOException {
        return ok("Create account transaction submitted.", transactionService.createAccount(request));
    }

    @PostMapping("/transactions/payment")
    public ResponseEntity<Map<String, Object>> payment(@RequestBody(required = false) PaymentRequest request)
            throws IOException {
        return ok("Payment transaction submitted.", transactionService.payment(request));
    }

    @PostMapping("/transactions/manage-data")
    public ResponseEntity<Map<String, Object>> manageData(@RequestBody(required = false) ManageDataRequest request)
            throws IOException {
        return ok("Manage data transaction submitted.", transactionService.manageData(request));
    }

    @PostMapping("/transactions/account-merge")
    public ResponseEntity<Map<String, Object>> accountMerge(@RequestBody(required = false) AccountMergeRequest request)
            throws IOException {
        return ok("Account merge transaction submitted.", transactionService.accountMerge(request));
    }

    @PostMapping("/transactions/trustline")
    public ResponseEntity<Map<String, Object>> trustline(@RequestBody(required = false) TrustlineRequest request)
            throws IOException {
        return ok("Trustline transaction submitted.", transactionService.trustline(request));
    }

    @PostMapping("/transactions/offer")
    public ResponseEntity<Map<String, Object>> offer(@RequestBody(required = false) OfferRequest request)
            throws IOException {
        return ok("Offer transaction submitted.", transactionService.offer(request));
    }

    @PostMapping("/transactions/path-payment")
    public ResponseEntity<Map<String, Object>> pathPayment(@RequestBody(required = false) PathPaymentRequest request)
            throws IOException {
        return ok("Path payment transaction submitted.", transactionService.pathPayment(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIo(IOException ex) {
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> ok(String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}
