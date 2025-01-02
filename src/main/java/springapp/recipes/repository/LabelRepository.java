package springapp.recipes.repository;

import org.springframework.data.repository.CrudRepository;
import springapp.recipes.model.Label;

public interface LabelRepository extends CrudRepository<Label, Integer> {
}
