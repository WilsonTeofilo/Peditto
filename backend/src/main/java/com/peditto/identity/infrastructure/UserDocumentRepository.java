package com.peditto.identity.infrastructure;
import com.peditto.identity.domain.UserDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserDocumentRepository extends JpaRepository<UserDocument, UUID> {
    boolean existsByDocumentNormalized(String documentNormalized);
    java.util.List<UserDocument> findAllByUser_Id(UUID userId);
    @org.springframework.data.jpa.repository.Query("select d from UserDocument d join fetch d.user where d.documentNormalized=:document")
    java.util.Optional<UserDocument> findWithUserByDocument(@org.springframework.data.repository.query.Param("document") String document);
}
