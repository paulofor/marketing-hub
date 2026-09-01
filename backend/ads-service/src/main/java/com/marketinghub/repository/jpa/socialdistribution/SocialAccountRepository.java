package com.marketinghub.repository.jpa.socialdistribution;

import com.marketinghub.socialdistribution.SocialAccount;
import com.marketinghub.socialdistribution.SocialAccountStatus;
import com.marketinghub.socialdistribution.SocialPlatform;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir contas sociais usadas na distribuição orgânica. */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
  /** Lista contas sociais por rede em ordem de cadastro mais recente. */
  List<SocialAccount> findByPlatformOrderByCreatedAtDesc(SocialPlatform platform);

  /** Conta canais orgânicos cuja conexão está pronta para uso. */
  long countByStatus(SocialAccountStatus status);
}
