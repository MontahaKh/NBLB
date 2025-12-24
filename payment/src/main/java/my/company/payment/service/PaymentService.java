package my.company.payment.service;



import my.company.payment.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentService {

    Payment createPayment(Payment payment);

    List<Payment> getPayments();

    Optional<Payment> getPaymentById(Long id);

    Payment updatePayment(Payment payment);

    void deletePayment(Long id);
}
