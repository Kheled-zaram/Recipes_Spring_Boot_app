package springapp.recipes.repository;

import org.springframework.data.repository.CrudRepository;
import springapp.recipes.model.Category;

public interface CategoryRepository extends CrudRepository<Category, Integer> {
}
