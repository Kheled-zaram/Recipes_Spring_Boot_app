package springapp.recipes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import springapp.recipes.model.Label;
import springapp.recipes.model.Recipe;
import springapp.recipes.repository.RecipeRepository;
import springapp.recipes.service.CategoryService;
import springapp.recipes.service.LabelService;

import java.net.URI;
import java.security.Principal;
import java.util.Date;

@RestController
@RequestMapping("/recipes")
public class RecipesController {

    private final RecipeRepository recipeRepository;

    private final CategoryService categoryService;

    private final LabelService labelService;

    public RecipesController(RecipeRepository recipeRepository, CategoryService categoryService, LabelService labelService) {
        this.recipeRepository = recipeRepository;
        this.categoryService = categoryService;
        this.labelService = labelService;
    }

    @GetMapping
    public ResponseEntity<Iterable<Recipe>> findAllRecipes(Pageable pageable, Principal principal) {
        Page<Recipe> page = recipeRepository.findByOwner(
                principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "lastUpdate"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    public ResponseEntity<Void> saveRecipe(@RequestBody Recipe recipe, UriComponentsBuilder ucb, Principal principal) {
        recipe.setOwner(principal.getName());
        recipe.setLastUpdate(new Date());

        recipe.setCategory(categoryService.createIfNotExists(recipe.getCategory()));
        recipe.setLabels(labelService.createIfNotExist(recipe.getLabels()));

        Recipe savedRecipe = recipeRepository.save(recipe);
        URI locationOfNewCashCard = ucb
                .path("recipes/{id}")
                .buildAndExpand(savedRecipe.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> findRecipe(@PathVariable Long id, Principal principal) {
        Recipe recipe = recipeRepository.findByIdAndOwner(id, principal.getName());

        if (recipe != null)
            return ResponseEntity.ok(recipe);

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, Principal principal) {

        if (!recipeRepository.existsByIdAndOwner(id, principal.getName()))
            return ResponseEntity.notFound().build();

        recipeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, Principal principal) {
        recipe.setOwner(principal.getName());
        recipe.setLastUpdate(new Date());

        if (!recipeRepository.existsByIdAndOwner(id, principal.getName()))
            return ResponseEntity.notFound().build();

        recipe.setCategory(categoryService.createIfNotExists(recipe.getCategory()));
        recipe.setLabels(labelService.createIfNotExist(recipe.getLabels()));

        recipeRepository.save(recipe);
        return ResponseEntity.ok(recipe);
    }

    @GetMapping("/{id}/labels")
    public ResponseEntity<Iterable<Label>> findLabels(@PathVariable Long id, Principal principal) {
        Recipe recipe = recipeRepository.findByIdAndOwner(id, principal.getName());

        if (recipe == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(recipe.getLabels());
    }

}

