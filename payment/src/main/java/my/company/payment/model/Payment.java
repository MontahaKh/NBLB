package my.company.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.company.payment.model.PaymentType;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Double amount;

    private String status;

    private Date date;

    private String username;

    @Enumerated(EnumType.STRING)
    private PaymentType type;  // CASH_ON_DELIVERY or CARD_STRIPE
}
