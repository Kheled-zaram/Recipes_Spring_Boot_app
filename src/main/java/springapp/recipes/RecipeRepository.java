package springapp.recipes;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface RecipeRepository extends CrudRepository<Recipe, Long>, PagingAndSortingRepository<Recipe, Long> {
}
