package nz.clemwhite.spring_social_cloud.security.data;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AuthorityEntityRespository extends CrudRepository<AuthorityEntity, Long> {


    Optional<AuthorityEntity> findByName(String authority);
}
