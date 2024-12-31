package springapp.recipes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface RecipeRepository extends CrudRepository<Recipe, Long>, PagingAndSortingRepository<Recipe, Long> {

    Recipe findByIdAndOwner(Long id, String owner);

    boolean existsByIdAndOwner(Long id, String owner);

    Page<Recipe> findByOwner(String owner, PageRequest pageRequest);
}
