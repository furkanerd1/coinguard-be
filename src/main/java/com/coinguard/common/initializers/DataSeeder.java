package com.coinguard.common.initializers;

import com.coinguard.user.entity.User;
import com.coinguard.user.enums.UserRole;
import com.coinguard.user.repository.UserRepository;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import com.coinguard.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (userRepository.count() > 0) {
            log.info("Veritabanı zaten dolu, seeder çalışmadı.");
            return;
        }

        log.info("🌱 Data Seeding Başlıyor...");


        User ali = User.builder()
                .username("alizengin")
                .email("ali@coinguard.com")
                .fullName("Ali Veli")
                .password("pass123")
                .phoneNumber("5551112233")
                .role(UserRole.USER)
                .isEmailVerified(true)
                .isActive(true)
                .build();

        userRepository.save(ali);
        walletService.createWalletForUser(ali);

        Wallet aliWallet = walletRepository.findByUserId(ali.getId()).orElseThrow();
        aliWallet.setBalance(new BigDecimal("50000.00"));
        walletRepository.save(aliWallet);


        User ayse = User.builder()
                .username("ayseyilmaz")
                .email("ayse@coinguard.com")
                .fullName("Ayşe Yılmaz")
                .password("pass123")
                .phoneNumber("5554445566")
                .role(UserRole.USER)
                .isEmailVerified(true)
                .isActive(true)
                .build();

        userRepository.save(ayse);
        walletService.createWalletForUser(ayse);

        Wallet ayseWallet = walletRepository.findByUserId(ayse.getId()).orElseThrow();
        ayseWallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.save(ayseWallet);

        log.info(" Data Seeding Tamamlandı!");
        log.info("------------------------------------------------");
        log.info("Ali ID (Sender): {}", ali.getId());
        log.info(" Ayşe ID (Receiver): {}", ayse.getId());
        log.info(" Şifreler: pass123");
        log.info("------------------------------------------------");
    }
}