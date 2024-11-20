package nz.clemwhite.spring_social_cloud.security.data;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRespository extends CrudRepository<UserEntity, String> {



}
