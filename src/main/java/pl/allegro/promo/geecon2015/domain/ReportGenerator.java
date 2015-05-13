package pl.allegro.promo.geecon2015.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import pl.allegro.promo.geecon2015.domain.stats.FinancialStatisticsRepository;
import pl.allegro.promo.geecon2015.domain.transaction.TransactionRepository;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransaction;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransactions;
import pl.allegro.promo.geecon2015.domain.user.UserRepository;

@Component
public class ReportGenerator {

    private final FinancialStatisticsRepository financialStatisticsRepository;

    private final UserRepository userRepository;

    private final TransactionRepository transactionRepository;

    @Autowired
    public ReportGenerator(FinancialStatisticsRepository financialStatisticsRepository, UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public Report generate(ReportRequest request) {
        List<UUID> usersId = financialStatisticsRepository.listUsersWithMinimalIncome(request.getMinimalIncome(), request
                .getUsersToCheck())
                .getUserIds();

        Report report = new Report();
        for (UUID userId : usersId) {
            report.add(new ReportedUser(userId, getUserName(userId), getTotalAmountForUser(userId)));
        }
        return report;
    }

    private String getUserName(UUID userId) {
        try {
            return userRepository.detailsOf(userId).getName();
        } catch (HttpServerErrorException e) {
            return "<failed>";
        }
    }

    private BigDecimal getTotalAmountForUser(UUID userId) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        UserTransactions transactions;
        try {
            transactions = transactionRepository.transactionsOf(userId);
        } catch (HttpServerErrorException e) {
            return null;
        }
        return transactions.getTransactions().stream()
                .map(UserTransaction::getAmount)
                .reduce(BigDecimal.ZERO, (amount, item) -> amount = amount.add(item));
    }
}
