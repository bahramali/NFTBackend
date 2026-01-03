package se.hydroleaf.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
            select distinct refreshToken
            from RefreshToken refreshToken
            join fetch refreshToken.user user
            left join fetch user.permissions
            where refreshToken.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashWithUserAndPermissions(@Param("tokenHash") String tokenHash);
}
