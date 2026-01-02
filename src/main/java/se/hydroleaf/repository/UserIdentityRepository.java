package se.hydroleaf.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.OauthProvider;
import se.hydroleaf.model.UserIdentity;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderSubject(OauthProvider provider, String providerSubject);
}
