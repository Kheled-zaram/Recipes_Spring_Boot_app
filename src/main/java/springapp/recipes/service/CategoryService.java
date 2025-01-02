package springapp.recipes.service;

import org.springframework.stereotype.Service;
import springapp.recipes.model.Category;
import springapp.recipes.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createIfNotExists(Category category) {
        if (category.getId() == null || !categoryRepository.existsById(category.getId())) {
            category.setId(null);
            return categoryRepository.save(category);
        }
        return category;
    }
}
